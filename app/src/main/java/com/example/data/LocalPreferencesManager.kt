package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.i18n.AppLanguage
import com.example.model.LocalDownloadedFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.academyDataStore: DataStore<Preferences> by preferencesDataStore(name = "zahid_academy_prefs")

class LocalPreferencesManager(private val context: Context) {
  private val languageKey = stringPreferencesKey("preferred_language")
  private val customFolderUriKey = stringPreferencesKey("custom_download_folder_uri")
  private val downloadsJsonKey = stringPreferencesKey("local_downloads_json")
  private val trialCategoryCountsKey = stringPreferencesKey("trial_category_downloads_json")

  val languageFlow: Flow<AppLanguage> = context.academyDataStore.data.map { prefs ->
    AppLanguage.fromCode(prefs[languageKey])
  }

  val customFolderUriFlow: Flow<String?> = prefsFlowString(customFolderUriKey)

  private fun prefsFlowString(key: Preferences.Key<String>): Flow<String?> =
    context.academyDataStore.data.map { prefs -> prefs[key]?.takeIf { it.isNotBlank() } }

  val localDownloadsFlow: Flow<List<LocalDownloadedFile>> = context.academyDataStore.data.map { prefs ->
    parseDownloads(prefs[downloadsJsonKey])
  }

  val trialCategoryDownloadsFlow: Flow<Map<String, Int>> = context.academyDataStore.data.map { prefs ->
    parseCategoryCounts(prefs[trialCategoryCountsKey])
  }

  suspend fun setLanguage(language: AppLanguage) {
    context.academyDataStore.edit { prefs ->
      prefs[languageKey] = language.code
    }
  }

  suspend fun setCustomFolderUri(uriString: String?) {
    context.academyDataStore.edit { prefs ->
      if (uriString.isNullOrBlank()) {
        prefs.remove(customFolderUriKey)
      } else {
        prefs[customFolderUriKey] = uriString
      }
    }
  }

  suspend fun upsertLocalDownload(file: LocalDownloadedFile) {
    context.academyDataStore.edit { prefs ->
      val current = parseDownloads(prefs[downloadsJsonKey]).toMutableList()
      current.removeAll { it.resourceId == file.resourceId || it.fileName == file.fileName }
      current.add(0, file)
      prefs[downloadsJsonKey] = serializeDownloads(current)
    }
  }

  suspend fun incrementTrialCategoryDownload(quotaKey: String) {
    context.academyDataStore.edit { prefs ->
      val counts = parseCategoryCounts(prefs[trialCategoryCountsKey]).toMutableMap()
      counts[quotaKey] = (counts[quotaKey] ?: 0) + 1
      val obj = JSONObject()
      counts.forEach { (k, v) -> obj.put(k, v) }
      prefs[trialCategoryCountsKey] = obj.toString()
    }
  }

  suspend fun removeLocalDownload(resourceId: String) {
    context.academyDataStore.edit { prefs ->
      val current = parseDownloads(prefs[downloadsJsonKey]).filterNot { it.resourceId == resourceId }
      prefs[downloadsJsonKey] = serializeDownloads(current)
    }
  }

  private fun parseCategoryCounts(raw: String?): Map<String, Int> {
    if (raw.isNullOrBlank()) return emptyMap()
    return try {
      val obj = JSONObject(raw)
      buildMap {
        val keys = obj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          put(k, obj.optInt(k, 0))
        }
      }
    } catch (_: Exception) {
      emptyMap()
    }
  }

  private fun parseDownloads(raw: String?): List<LocalDownloadedFile> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          add(
            LocalDownloadedFile(
              resourceId = obj.optString("resourceId"),
              title = obj.optString("title"),
              categoryId = obj.optString("categoryId"),
              fileType = obj.optString("fileType"),
              fileName = obj.optString("fileName"),
              fileSizeBytes = obj.optLong("fileSizeBytes"),
              savedPathOrUri = obj.optString("savedPathOrUri"),
              displayLocation = obj.optString("displayLocation"),
              isTreeUri = obj.optBoolean("isTreeUri", false),
              downloadedAtMs = obj.optLong("downloadedAtMs"),
            )
          )
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun serializeDownloads(items: List<LocalDownloadedFile>): String {
    val arr = JSONArray()
    items.forEach { item ->
      arr.put(
        JSONObject().apply {
          put("resourceId", item.resourceId)
          put("title", item.title)
          put("categoryId", item.categoryId)
          put("fileType", item.fileType)
          put("fileName", item.fileName)
          put("fileSizeBytes", item.fileSizeBytes)
          put("savedPathOrUri", item.savedPathOrUri)
          put("displayLocation", item.displayLocation)
          put("isTreeUri", item.isTreeUri)
          put("downloadedAtMs", item.downloadedAtMs)
        }
      )
    }
    return arr.toString()
  }
}
