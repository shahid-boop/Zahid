package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.AcademyStrings
import com.example.model.LocalDownloadedFile
import com.example.model.ResourceCategory
import com.example.model.ResourceItem
import com.example.ui.components.AcademySearchBar
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.formatBytes
import com.example.ui.components.formatDateMs
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedBg
import com.example.ui.theme.NavyTint
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg

@Composable
fun MyDownloadsScreen(
  localDownloads: List<LocalDownloadedFile>,
  catalogResources: List<ResourceItem>,
  customFolderUri: String?,
  strings: AcademyStrings,
  verifyExists: (LocalDownloadedFile) -> Boolean,
  onSelectCustomFolder: (Uri?) -> Unit,
  onViewLocalFile: (LocalDownloadedFile) -> Unit,
  onOpenExternal: (LocalDownloadedFile) -> Unit,
  onShareFile: (LocalDownloadedFile) -> Unit,
  onReDownload: (ResourceItem) -> Unit,
  onDeleteLocal: (LocalDownloadedFile) -> Unit,
) {
  val context = LocalContext.current
  var query by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("ALL") }
  var pendingDelete by remember { mutableStateOf<LocalDownloadedFile?>(null) }

  val folderPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
  ) { uri ->
    if (uri != null) {
      try {
        context.contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
      } catch (_: Exception) {
      }
      onSelectCustomFolder(uri)
    }
  }

  val filtered = remember(localDownloads, query, selectedCategory) {
    val q = query.trim().lowercase()
    localDownloads.filter { item ->
      val matchCat = selectedCategory == "ALL" || item.categoryId == selectedCategory
      val matchQ = q.isEmpty() ||
        item.title.lowercase().contains(q) ||
        item.fileName.lowercase().contains(q) ||
        item.fileType.lowercase().contains(q)
      matchCat && matchQ
    }
  }

  pendingDelete?.let { target ->
    ConfirmationDialog(
      title = strings.deleteLocalConfirmTitle,
      message = strings.deleteLocalConfirmBody,
      confirmLabel = strings.confirmBtn,
      cancelLabel = strings.cancelBtn,
      onConfirm = {
        onDeleteLocal(target)
        pendingDelete = null
      },
      onDismiss = { pendingDelete = null },
    )
  }

  Box(modifier = Modifier.fillMaxSize().testTag("downloads_screen"), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Text(
          text = strings.myDownloadsTitle,
          style = MaterialTheme.typography.headlineSmall,
          color = PrimaryNavy,
          fontWeight = FontWeight.Bold,
        )
      }

      // Folder Configuration Card
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, BorderSubtle),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = PrimaryNavy)
              Text(strings.downloadFolderLabel, style = MaterialTheme.typography.titleSmall, color = PrimaryNavy)
            }
            Text(
              text = if (customFolderUri.isNullOrBlank()) {
                "Internal Storage / Download / Zahid Architecture Academy"
              } else {
                "Custom Folder: $customFolderUri"
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
              OutlinedButton(
                onClick = { folderPicker.launch(null) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).testTag("choose_folder_btn"),
              ) {
                Text(strings.chooseFolderBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
              if (!customFolderUri.isNullOrBlank()) {
                TextButton(
                  onClick = { onSelectCustomFolder(null) },
                  modifier = Modifier.testTag("reset_folder_btn"),
                ) {
                  Text(strings.resetDefaultFolderBtn)
                }
              }
            }
          }
        }
      }

      item {
        AcademySearchBar(
          query = query,
          hint = strings.searchHint,
          onQueryChange = { query = it },
        )
      }

      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            FilterChip(
              selected = selectedCategory == "ALL",
              onClick = { selectedCategory = "ALL" },
              label = { Text(strings.allFilter) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryNavy, selectedLabelColor = PureWhite),
            )
          }
          items(ResourceCategory.entries) { cat ->
            FilterChip(
              selected = selectedCategory == cat.id,
              onClick = { selectedCategory = cat.id },
              label = { Text(cat.localizedLabel(strings)) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryNavy, selectedLabelColor = PureWhite),
            )
          }
        }
      }

      if (filtered.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Filled.DownloadDone, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(44.dp))
              Text(strings.noResultsTitle, style = MaterialTheme.typography.titleMedium)
              Text(
                text = "Files downloaded from the Library appear here with offline access and native file management.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(filtered, key = { it.resourceId }) { item ->
          val exists = verifyExists(item)
          val catalogMatch = catalogResources.firstOrNull { it.id == item.resourceId }
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth().testTag("download_item_${item.resourceId}"),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                  Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                  Text(
                    text = "${item.fileName} • ${formatBytes(item.fileSizeBytes)} • ${formatDateMs(item.downloadedAtMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Surface(
                  color = if (exists) SuccessGreenBg else ErrorRedBg,
                  shape = RoundedCornerShape(50),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(
                      imageVector = if (exists) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                      contentDescription = null,
                      tint = if (exists) SuccessGreen else ErrorRed,
                      modifier = Modifier.size(14.dp),
                    )
                    Text(
                      text = if (exists) strings.fileVerified else strings.fileMissing,
                      style = MaterialTheme.typography.labelSmall,
                      color = if (exists) SuccessGreen else ErrorRed,
                    )
                  }
                }
              }

              Text(
                text = item.displayLocation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                if (exists) {
                  OutlinedButton(
                    onClick = { onViewLocalFile(item) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.viewBtn, style = MaterialTheme.typography.labelMedium)
                  }
                  Button(
                    onClick = { onOpenExternal(item) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy, contentColor = PureWhite),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.openFileBtn, style = MaterialTheme.typography.labelMedium)
                  }
                  IconButton(onClick = { onShareFile(item) }) {
                    Icon(Icons.Filled.Share, contentDescription = strings.shareBtn, tint = PrimaryNavy)
                  }
                } else if (catalogMatch != null) {
                  Button(
                    onClick = { onReDownload(catalogMatch) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, contentColor = PureWhite),
                    shape = RoundedCornerShape(10.dp),
                  ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.reDownloadBtn)
                  }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { pendingDelete = item }, modifier = Modifier.testTag("delete_download_${item.resourceId}")) {
                  Icon(Icons.Filled.DeleteOutline, contentDescription = strings.deleteLocalBtn, tint = ErrorRed)
                }
              }
            }
          }
        }
      }
    }
  }
}
