package com.example.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.DownloadState
import com.example.model.LocalDownloadedFile
import com.example.model.ResourceCategory
import com.example.model.ResourceFileType
import com.example.model.ResourceItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class AcademyFileDownloadManager(
  private val context: Context,
  private val prefs: LocalPreferencesManager,
) {
  private val client = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .build()

  private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
  val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

  @Volatile
  private var activeCall: Call? = null
  @Volatile
  private var activeJob: Job? = null

  fun resetState() {
    _downloadState.value = DownloadState.Idle
  }

  fun cancelActiveDownload() {
    activeCall?.cancel()
    activeCall = null
    activeJob?.cancel()
    activeJob = null
    _downloadState.value = DownloadState.Idle
  }

  suspend fun downloadResource(
    resource: ResourceItem,
    customTreeUriString: String?,
  ): Result<LocalDownloadedFile> = withContext(Dispatchers.IO) {
    coroutineScope {
      activeJob = coroutineContext[Job]
      val category = ResourceCategory.fromId(resource.categoryId)
      val fileType = ResourceFileType.fromString(resource.fileType)
      val safeFileName = sanitizeFileName(resource.fileName.ifBlank { "${resource.id}.${fileType.ext}" })

      _downloadState.value = DownloadState.InProgress(
        resourceId = resource.id,
        fileName = safeFileName,
        totalBytes = resource.fileSizeBytes.coerceAtLeast(1L),
        downloadedBytes = 0L,
        percent = 0,
      )

      try {
        val resultFile = if (!customTreeUriString.isNullOrBlank()) {
          saveToCustomSafFolder(
            treeUri = Uri.parse(customTreeUriString),
            category = category,
            fileName = safeFileName,
            mimeType = fileType.mimeType,
            resource = resource,
          )
        } else {
          saveToDefaultDownloadsFolder(
            category = category,
            fileName = safeFileName,
            mimeType = fileType.mimeType,
            resource = resource,
          )
        }

        prefs.upsertLocalDownload(resultFile)
        _downloadState.value = DownloadState.Completed(resultFile)
        Result.success(resultFile)
      } catch (e: CancellationException) {
        _downloadState.value = DownloadState.Idle
        throw e
      } catch (e: Exception) {
        val message = e.localizedMessage ?: "Download failed"
        _downloadState.value = DownloadState.Failed(resource.id, message)
        Result.failure(e)
      } finally {
        activeCall = null
        activeJob = null
      }
    }
  }

  private suspend fun saveToDefaultDownloadsFolder(
    category: ResourceCategory,
    fileName: String,
    mimeType: String,
    resource: ResourceItem,
  ): LocalDownloadedFile {
    val relativeSubdir = "${Environment.DIRECTORY_DOWNLOADS}/Zahid Architecture Academy/${category.folderName}"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val resolver = context.contentResolver
      val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
      val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, relativeSubdir)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
      }
      val uri = try {
        resolver.insert(collection, values)
      } catch (_: Exception) {
        null
      }
      if (uri != null) {
        try {
          val stream = resolver.openOutputStream(uri)
          if (stream != null) {
            val writtenBytes = stream.use { output ->
              streamSourceToOutput(resource, output, fileName)
            }
            val doneValues = ContentValues().apply {
              put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(uri, doneValues, null, null)

            return LocalDownloadedFile(
              resourceId = resource.id,
              title = resource.title,
              categoryId = resource.categoryId,
              fileType = resource.fileType,
              fileName = fileName,
              fileSizeBytes = writtenBytes,
              savedPathOrUri = uri.toString(),
              displayLocation = "Internal Storage / Download / Zahid Architecture Academy / ${category.folderName} / $fileName",
              isTreeUri = true,
              downloadedAtMs = System.currentTimeMillis(),
            )
          } else {
            resolver.delete(uri, null, null)
          }
        } catch (e: CancellationException) {
          try {
            resolver.delete(uri, null, null)
          } catch (_: Exception) {
          }
          throw e
        } catch (_: Exception) {
          try {
            resolver.delete(uri, null, null)
          } catch (_: Exception) {
          }
          // Fall through to app-scoped Downloads directory if MediaStore stream is unavailable
        }
      }
    }

    // Fallback for API 24-28 or Robolectric test environment
    val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
    val targetDir = File(baseDir, "Zahid Architecture Academy/${category.folderName}")
    if (!targetDir.exists()) targetDir.mkdirs()
    val destFile = File(targetDir, fileName)
    val tempFile = File(targetDir, "$fileName.part")
    val writtenBytes = try {
      FileOutputStream(tempFile).use { output ->
        streamSourceToOutput(resource, output, fileName)
      }
    } catch (e: Exception) {
      tempFile.delete()
      throw e
    }
    if (destFile.exists()) destFile.delete()
    if (!tempFile.renameTo(destFile)) {
      tempFile.copyTo(destFile, overwrite = true)
      tempFile.delete()
    }

    return LocalDownloadedFile(
      resourceId = resource.id,
      title = resource.title,
      categoryId = resource.categoryId,
      fileType = resource.fileType,
      fileName = fileName,
      fileSizeBytes = writtenBytes,
      savedPathOrUri = destFile.absolutePath,
      displayLocation = destFile.absolutePath,
      isTreeUri = false,
      downloadedAtMs = System.currentTimeMillis(),
    )
  }

  private suspend fun saveToCustomSafFolder(
    treeUri: Uri,
    category: ResourceCategory,
    fileName: String,
    mimeType: String,
    resource: ResourceItem,
  ): LocalDownloadedFile {
    val resolver = context.contentResolver
    val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
    val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)

    val academyFolderUri = findOrCreateChildFolder(treeUri, rootDocUri, "Zahid Architecture Academy")
      ?: rootDocUri
    val categoryFolderUri = findOrCreateChildFolder(treeUri, academyFolderUri, category.folderName)
      ?: academyFolderUri

    val docUri = DocumentsContract.createDocument(resolver, categoryFolderUri, mimeType, fileName)
      ?: throw IllegalStateException("Could not create file in selected folder")

    val writtenBytes = try {
      resolver.openOutputStream(docUri)?.use { output ->
        streamSourceToOutput(resource, output, fileName)
      } ?: throw IllegalStateException("Unable to write to selected folder")
    } catch (e: Exception) {
      try {
        DocumentsContract.deleteDocument(resolver, docUri)
      } catch (_: Exception) {
      }
      throw e
    }

    return LocalDownloadedFile(
      resourceId = resource.id,
      title = resource.title,
      categoryId = resource.categoryId,
      fileType = resource.fileType,
      fileName = fileName,
      fileSizeBytes = writtenBytes,
      savedPathOrUri = docUri.toString(),
      displayLocation = "Custom Folder / Zahid Architecture Academy / ${category.folderName} / $fileName",
      isTreeUri = true,
      downloadedAtMs = System.currentTimeMillis(),
    )
  }

  private fun findOrCreateChildFolder(treeUri: Uri, parentDocUri: Uri, folderName: String): Uri? {
    val resolver = context.contentResolver
    val parentDocId = DocumentsContract.getDocumentId(parentDocUri)
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
    resolver.query(
      childrenUri,
      arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
      ),
      null,
      null,
      null,
    )?.use { cursor ->
      val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
      val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
      val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
      while (cursor.moveToNext()) {
        val name = cursor.getString(nameCol)
        val mime = cursor.getString(mimeCol)
        if (name == folderName && mime == DocumentsContract.Document.MIME_TYPE_DIR) {
          val childId = cursor.getString(idCol)
          return DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
        }
      }
    }
    return try {
      DocumentsContract.createDocument(
        resolver,
        parentDocUri,
        DocumentsContract.Document.MIME_TYPE_DIR,
        folderName,
      )
    } catch (_: Exception) {
      null
    }
  }

  private suspend fun streamSourceToOutput(
    resource: ResourceItem,
    output: OutputStream,
    fileName: String,
  ): Long = coroutineScope {
    val url = resource.fileUrl.trim()
    if (url.startsWith("asset://")) {
      val assetPath = url.removePrefix("asset://")
      context.assets.open(assetPath).use { input ->
        val total = resource.fileSizeBytes.coerceAtLeast(input.available().toLong()).coerceAtLeast(1L)
        copyWithProgress(input, output, total, resource.id, fileName)
      }
    } else {
      val request = Request.Builder().url(url).build()
      val call = client.newCall(request)
      activeCall = call
      val response = call.execute()
      if (!response.isSuccessful) {
        response.close()
        throw IllegalStateException("HTTP ${response.code}: Unable to download file")
      }
      val body = response.body ?: throw IllegalStateException("Empty server response")
      val total = body.contentLength().takeIf { it > 0L } ?: resource.fileSizeBytes.coerceAtLeast(1L)
      body.byteStream().use { input ->
        copyWithProgress(input, output, total, resource.id, fileName)
      }
    }
  }

  private suspend fun copyWithProgress(
    input: InputStream,
    output: OutputStream,
    totalBytes: Long,
    resourceId: String,
    fileName: String,
  ): Long = coroutineScope {
    val buffer = ByteArray(8192)
    var downloaded = 0L
    var lastReportedPct = -1
    while (isActive) {
      val read = input.read(buffer)
      if (read == -1) break
      output.write(buffer, 0, read)
      downloaded += read
      val pct = ((downloaded * 100L) / totalBytes.coerceAtLeast(downloaded)).toInt().coerceIn(0, 100)
      if (pct != lastReportedPct) {
        lastReportedPct = pct
        _downloadState.value = DownloadState.InProgress(
          resourceId = resourceId,
          fileName = fileName,
          totalBytes = totalBytes.coerceAtLeast(downloaded),
          downloadedBytes = downloaded,
          percent = pct,
        )
      }
    }
    if (!isActive) throw CancellationException("Download cancelled")
    output.flush()
    downloaded
  }

  suspend fun resolvePreviewFile(
    url: String,
    fallbackName: String,
    localRecord: LocalDownloadedFile? = null,
  ): Result<File> = withContext(Dispatchers.IO) {
    try {
      // Check if already downloaded locally
      if (localRecord != null && verifyFileExists(localRecord)) {
        if (!localRecord.isTreeUri) {
          val file = File(localRecord.savedPathOrUri)
          if (file.exists()) return@withContext Result.success(file)
        } else {
          val cacheCopy = File(context.cacheDir, "preview_${sanitizeFileName(localRecord.fileName)}")
          context.contentResolver.openInputStream(Uri.parse(localRecord.savedPathOrUri))?.use { input ->
            FileOutputStream(cacheCopy).use { out -> input.copyTo(out) }
          }
          if (cacheCopy.exists() && cacheCopy.length() > 0L) {
            return@withContext Result.success(cacheCopy)
          }
        }
      }

      val cleanName = sanitizeFileName(fallbackName)
      val cacheFile = File(context.cacheDir, "preview_$cleanName")
      if (url.startsWith("asset://")) {
        val assetPath = url.removePrefix("asset://")
        context.assets.open(assetPath).use { input ->
          FileOutputStream(cacheFile).use { out -> input.copyTo(out) }
        }
        Result.success(cacheFile)
      } else {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
          response.close()
          return@withContext Result.failure(IllegalStateException("HTTP ${response.code}"))
        }
        val body = response.body ?: return@withContext Result.failure(IllegalStateException("Empty body"))
        body.byteStream().use { input ->
          FileOutputStream(cacheFile).use { out -> input.copyTo(out) }
        }
        Result.success(cacheFile)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun verifyFileExists(localFile: LocalDownloadedFile): Boolean {
    return try {
      if (localFile.isTreeUri) {
        val uri = Uri.parse(localFile.savedPathOrUri)
        (context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false) ||
          (context.contentResolver.openInputStream(uri)?.use { true } ?: false)
      } else {
        val f = File(localFile.savedPathOrUri)
        f.exists() && f.length() > 0L
      }
    } catch (_: Exception) {
      false
    }
  }

  suspend fun deleteLocalCopy(localFile: LocalDownloadedFile): Boolean = withContext(Dispatchers.IO) {
    val deleted = try {
      if (localFile.isTreeUri) {
        val uri = Uri.parse(localFile.savedPathOrUri)
        val rows = try {
          context.contentResolver.delete(uri, null, null)
        } catch (_: Exception) {
          if (DocumentsContract.isDocumentUri(context, uri)) {
            if (DocumentsContract.deleteDocument(context.contentResolver, uri)) 1 else 0
          } else 0
        }
        rows >= 0
      } else {
        val f = File(localFile.savedPathOrUri)
        !f.exists() || f.delete()
      }
    } catch (_: Exception) {
      false
    }
    prefs.removeLocalDownload(localFile.resourceId)
    deleted
  }

  fun openFileExternally(localFile: LocalDownloadedFile): Boolean {
    val mime = ResourceFileType.fromString(localFile.fileType).mimeType
    val uri = resolveShareableUri(localFile) ?: return false
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, mime)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
      context.startActivity(Intent.createChooser(intent, localFile.fileName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      true
    } catch (_: Exception) {
      false
    }
  }

  fun shareFile(localFile: LocalDownloadedFile): Boolean {
    val mime = ResourceFileType.fromString(localFile.fileType).mimeType
    val uri = resolveShareableUri(localFile) ?: return false
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = mime
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, localFile.title)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
      context.startActivity(Intent.createChooser(intent, localFile.title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      true
    } catch (_: Exception) {
      false
    }
  }

  private fun resolveShareableUri(localFile: LocalDownloadedFile): Uri? {
    return try {
      if (localFile.isTreeUri) {
        Uri.parse(localFile.savedPathOrUri)
      } else {
        val file = File(localFile.savedPathOrUri)
        if (!file.exists()) return null
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
      }
    } catch (_: Exception) {
      null
    }
  }

  private fun sanitizeFileName(raw: String): String =
    raw.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_").trim().ifBlank { "academy_resource.bin" }
}
