package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SearchOff
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.i18n.AcademyStrings
import com.example.model.AcademyStarterCatalog
import com.example.model.AccessEvaluator
import com.example.model.FavoriteItem
import com.example.model.LocalDownloadedFile
import com.example.model.ResourceCategory
import com.example.model.ResourceItem
import com.example.model.UserProfile
import com.example.ui.components.AcademySearchBar
import com.example.ui.components.FilterPanel
import com.example.ui.components.ResourceCard
import com.example.ui.components.categoryIcon
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedBg
import com.example.ui.theme.NavyTint
import com.example.ui.theme.OrangeTint
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg

@Composable
fun LibraryScreen(
  initialCategoryId: String?,
  profile: UserProfile?,
  resources: List<ResourceItem>,
  favorites: List<FavoriteItem>,
  localDownloads: List<LocalDownloadedFile>,
  strings: AcademyStrings,
  onResourceClick: (ResourceItem) -> Unit,
  onViewResource: (ResourceItem) -> Unit,
  onDownloadResource: (ResourceItem) -> Unit,
  onToggleFavorite: (ResourceItem) -> Unit,
  initialPlotSize: String? = null,
  trialCategoryDownloads: Map<String, Int> = emptyMap(),
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember(initialCategoryId) { mutableStateOf(initialCategoryId ?: "ALL") }
  var selectedPlotSize by remember(initialPlotSize) { mutableStateOf(initialPlotSize ?: "ALL") }
  var selectedAccess by remember { mutableStateOf("ALL") }
  var selectedSort by remember { mutableStateOf("NEWEST") }
  var onlyFavorites by remember { mutableStateOf(false) }
  var visibleCount by remember { mutableIntStateOf(20) }

  val favIds = remember(favorites) { favorites.map { it.resourceId }.toSet() }
  val downloadedIds = remember(localDownloads) { localDownloads.map { it.resourceId }.toSet() }

  val filtered = remember(resources, searchQuery, selectedCategory, selectedPlotSize, selectedAccess, selectedSort, onlyFavorites, favIds) {
    val q = searchQuery.trim().lowercase()
    val base = resources.filter { item ->
      val matchCat = selectedCategory == "ALL" || item.categoryId == selectedCategory
      val matchPlot = selectedPlotSize == "ALL" || item.plotSize == selectedPlotSize
      val matchAccess = when (selectedAccess) {
        "FREE" -> !item.premium
        "PREMIUM" -> item.premium
        else -> true
      }
      val matchFav = !onlyFavorites || item.id in favIds
      val matchQuery = q.isEmpty() ||
        item.title.lowercase().contains(q) ||
        item.description.lowercase().contains(q) ||
        item.fileType.lowercase().contains(q) ||
        item.fileName.lowercase().contains(q)
      matchCat && matchPlot && matchAccess && matchFav && matchQuery
    }
    when (selectedSort) {
      "TITLE" -> base.sortedBy { it.title.lowercase() }
      "SIZE" -> base.sortedByDescending { it.fileSizeBytes }
      else -> base
    }
  }

  val paged = remember(filtered, visibleCount) { filtered.take(visibleCount) }

  Box(
    modifier = Modifier.fillMaxSize().testTag("library_screen"),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        AcademySearchBar(
          query = searchQuery,
          hint = strings.searchHint,
          onQueryChange = {
            searchQuery = it
            visibleCount = 20
          },
        )
      }

      item {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          item {
            FilterChip(
              selected = selectedCategory == "ALL" && !onlyFavorites,
              onClick = {
                selectedCategory = "ALL"
                onlyFavorites = false
              },
              label = { Text(strings.allFilter) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PrimaryNavy,
                selectedLabelColor = PureWhite,
              ),
              modifier = Modifier.testTag("lib_cat_ALL"),
            )
          }
          item {
            FilterChip(
              selected = onlyFavorites,
              onClick = { onlyFavorites = !onlyFavorites },
              label = { Text(strings.favoriteBtn) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AccentOrange,
                selectedLabelColor = PureWhite,
              ),
              modifier = Modifier.testTag("lib_cat_FAV"),
            )
          }
          items(ResourceCategory.entries) { cat ->
            FilterChip(
              selected = selectedCategory == cat.id,
              onClick = {
                selectedCategory = cat.id
                onlyFavorites = false
              },
              label = { Text(cat.localizedLabel(strings)) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PrimaryNavy,
                selectedLabelColor = PureWhite,
              ),
              modifier = Modifier.testTag("lib_cat_${cat.id}"),
            )
          }
        }
      }

      item {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          item {
            FilterChip(
              selected = selectedPlotSize == "ALL",
              onClick = { selectedPlotSize = "ALL" },
              label = { Text("All Plot Sizes") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AccentOrange,
                selectedLabelColor = PureWhite,
              ),
            )
          }
          items(AcademyStarterCatalog.plotSizes) { size ->
            FilterChip(
              selected = selectedPlotSize == size,
              onClick = {
                selectedPlotSize = if (selectedPlotSize == size) "ALL" else size
              },
              label = { Text("$size Maps") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AccentOrange,
                selectedLabelColor = PureWhite,
              ),
            )
          }
        }
      }

      item {
        FilterPanel(
          selectedAccess = selectedAccess,
          selectedSort = selectedSort,
          strings = strings,
          onAccessChange = { selectedAccess = it },
          onSortChange = { selectedSort = it },
        )
      }

      if (paged.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Filled.SearchOff, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(44.dp))
              Text(strings.noResultsTitle, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
              Text(
                strings.noResultsDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
      } else {
        items(paged, key = { it.id }) { resource ->
          ResourceCard(
            resource = resource,
            strings = strings,
            hasAccess = AccessEvaluator.canAccessResource(profile, resource),
            isFavorited = resource.id in favIds,
            isDownloaded = resource.id in downloadedIds,
            onCardClick = { onResourceClick(resource) },
            onViewClick = { onViewResource(resource) },
            onDownloadClick = { onDownloadResource(resource) },
            onToggleFavorite = { onToggleFavorite(resource) },
            canDownload = AccessEvaluator.canDownloadInCategory(profile, resource, trialCategoryDownloads, downloadedIds),
          )
        }

        if (filtered.size > paged.size) {
          item {
            OutlinedButton(
              onClick = { visibleCount += 20 },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("load_more_resources_btn"),
            ) {
              Text("Load More (${filtered.size - paged.size} remaining)")
            }
          }
        }
      }
    }
  }
}

@Composable
fun ResourceDetailScreen(
  resource: ResourceItem?,
  profile: UserProfile?,
  isFavorited: Boolean,
  localFile: LocalDownloadedFile?,
  strings: AcademyStrings,
  onBack: () -> Unit,
  onViewClick: (ResourceItem) -> Unit,
  onDownloadClick: (ResourceItem) -> Unit,
  onOpenExternal: (LocalDownloadedFile) -> Unit,
  onToggleFavorite: (ResourceItem) -> Unit,
  onUpgradeClick: () -> Unit,
) {
  if (resource == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(strings.noResultsTitle, style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = onBack) { Text("Back") }
      }
    }
    return
  }

  val category = ResourceCategory.fromId(resource.categoryId)
  val hasAccess = AccessEvaluator.canAccessResource(profile, resource)
  val dateStr = formatTimestamp(resource.updatedAt ?: resource.createdAt)

  Box(modifier = Modifier.fillMaxSize().testTag("resource_detail_screen"), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_btn")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryNavy)
        }
        Text(
          text = category.localizedLabel(strings),
          style = MaterialTheme.typography.titleMedium,
          color = PrimaryNavy,
          modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onToggleFavorite(resource) }, modifier = Modifier.testTag("detail_fav_btn")) {
          Icon(
            imageVector = if (isFavorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            contentDescription = strings.favoriteBtn,
            tint = if (isFavorited) AccentOrange else PrimaryNavy,
          )
        }
      }

      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
              modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (resource.premium) OrangeTint else NavyTint),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = categoryIcon(category),
                contentDescription = resource.fileType,
                tint = if (resource.premium) AccentOrange else PrimaryNavy,
                modifier = Modifier.size(30.dp),
              )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(resource.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = NavyTint, shape = RoundedCornerShape(6.dp)) {
                  Text(
                    resource.fileType,
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryNavy,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  )
                }
                Surface(
                  color = if (!resource.premium) SuccessGreenBg else if (hasAccess) NavyTint else ErrorRedBg,
                  shape = RoundedCornerShape(6.dp),
                ) {
                  Text(
                    text = if (!resource.premium) strings.freeFilter else if (hasAccess) strings.proActiveBadge else strings.premiumFilter,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (!resource.premium) SuccessGreen else if (hasAccess) PrimaryNavy else ErrorRed,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  )
                }
              }
            }
          }

          Text(
            text = resource.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("File: ${resource.fileName}", style = MaterialTheme.typography.bodySmall)
              Text("Size: ${resource.fileSizeLabel}", style = MaterialTheme.typography.bodySmall)
              if (dateStr.isNotEmpty()) {
                Text("Updated: $dateStr", style = MaterialTheme.typography.bodySmall)
              }
              if (localFile != null) {
                Text("Saved: ${localFile.displayLocation}", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
              }
            }
          }
        }
      }

      if (!hasAccess) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = OrangeTint),
          border = BorderStroke(1.dp, AccentOrange.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Filled.Lock, contentDescription = null, tint = AccentOrange)
              Text(strings.premiumLockedTitle, style = MaterialTheme.typography.titleMedium, color = PrimaryNavy)
            }
            Text(strings.premiumLockedDesc, style = MaterialTheme.typography.bodySmall)
            Button(
              onClick = onUpgradeClick,
              colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, contentColor = PureWhite),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("detail_unlock_btn"),
            ) {
              Text(strings.unlockAccessBtn)
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = { onViewClick(resource) },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).height(50.dp).testTag("detail_view_btn"),
        ) {
          Icon(Icons.Filled.Visibility, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(strings.viewBtn, fontWeight = FontWeight.Bold)
        }
        Button(
          onClick = { onDownloadClick(resource) },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (hasAccess) PrimaryNavy else AccentOrange,
            contentColor = PureWhite,
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).height(50.dp).testTag("detail_download_btn"),
        ) {
          Icon(if (hasAccess) Icons.Filled.Download else Icons.Filled.Lock, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(strings.downloadBtn, fontWeight = FontWeight.Bold)
        }
      }

      if (localFile != null) {
        OutlinedButton(
          onClick = { onOpenExternal(localFile) },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("detail_open_external_btn"),
        ) {
          Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(strings.openFileBtn)
        }
      }
    }
  }
}
