package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.AcademyStrings
import com.example.model.AcademyStarterCatalog
import com.example.model.AccessEvaluator
import com.example.model.FavoriteItem
import com.example.model.LocalDownloadedFile
import com.example.model.ResourceCategory
import com.example.model.ResourceItem
import com.example.model.UserProfile
import com.example.ui.components.CategoryCard
import com.example.ui.components.ResourceCard
import com.example.ui.components.TrialBanner
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
fun HomeScreen(
  profile: UserProfile?,
  resources: List<ResourceItem>,
  favorites: List<FavoriteItem>,
  localDownloads: List<LocalDownloadedFile>,
  strings: AcademyStrings,
  onStartTrial: () -> Unit,
  onUpgradeClick: () -> Unit,
  onCategoryClick: (ResourceCategory) -> Unit,
  onSeeAllLibrary: () -> Unit,
  onResourceClick: (ResourceItem) -> Unit,
  onViewResource: (ResourceItem) -> Unit,
  onDownloadResource: (ResourceItem) -> Unit,
  onToggleFavorite: (ResourceItem) -> Unit,
  onOpenTools: () -> Unit = {},
  onOpenDownloads: () -> Unit = {},
  trialCategoryDownloads: Map<String, Int> = emptyMap(),
  onQuickJazzCash: (() -> Unit)? = null,
  onQuickEasypaisa: (() -> Unit)? = null,
  onWhatsAppClick: (() -> Unit)? = null,
  onPlotSizeClick: (String) -> Unit = {},
  onRequestCustomMap: () -> Unit = {},
) {
  val homeCategories = remember { ResourceCategory.homeCategories() }
  val categoryCounts = remember(resources) {
    homeCategories.associateWith { cat -> resources.count { it.categoryId == cat.id } }
  }
  val plotSizes = AcademyStarterCatalog.plotSizes
  var selectedPlotSize by remember { mutableStateOf("5 Marla") }
  var showAllPlotMaps by remember { mutableStateOf(false) }

  val plotSizeCounts = remember(resources) {
    plotSizes.associateWith { size -> resources.count { it.plotSize == size } }
  }
  val selectedPlotMaps = remember(resources, selectedPlotSize) {
    resources.filter { it.plotSize == selectedPlotSize }
  }
  val displayedPlotMaps = remember(selectedPlotMaps, showAllPlotMaps) {
    if (showAllPlotMaps) selectedPlotMaps else selectedPlotMaps.take(3)
  }

  val recentResources = remember(resources) { resources.take(6) }
  val favIds = remember(favorites) { favorites.map { it.resourceId }.toSet() }
  val bookmarkedResources = remember(favorites, resources) {
    val byId = resources.associateBy { it.id }
    favorites.mapNotNull { byId[it.resourceId] }
  }
  val downloadedIds = remember(localDownloads) { localDownloads.map { it.resourceId }.toSet() }
  val plotRemaining = AccessEvaluator.remainingTrialDownloadsInCategory(
    profile = profile,
    quotaKey = "Maps - $selectedPlotSize",
    downloadedCategoryCounts = trialCategoryDownloads,
  )

  Box(
    modifier = Modifier.fillMaxSize().testTag("home_screen"),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 840.dp).fillMaxSize().testTag("home_lazy_column"),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      if (profile?.accountStatus == "SUSPENDED") {
        item {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = ErrorRedBg),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Filled.Warning, contentDescription = null, tint = ErrorRed)
              Text(
                text = strings.accountSuspendedBanner,
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }
      }

      item {
        TrialBanner(
          profile = profile,
          strings = strings,
          onStartTrial = onStartTrial,
          onUpgradeClick = onUpgradeClick,
          onQuickJazzCash = onQuickJazzCash,
          onQuickEasypaisa = onQuickEasypaisa,
          onWhatsAppClick = onWhatsAppClick,
        )
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Card(
            onClick = onOpenTools,
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.weight(1f).testTag("home_quick_tools_card"),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Surface(color = OrangeTint, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Filled.Calculate, contentDescription = strings.navTools, tint = AccentOrange)
                }
              }
              Column {
                Text(strings.navTools, style = MaterialTheme.typography.titleSmall, color = PrimaryNavy, fontWeight = FontWeight.Bold)
                Text("Plot, BOQ & Stairs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
          Card(
            onClick = onOpenDownloads,
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.weight(1f).testTag("home_quick_downloads_card"),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Surface(color = NavyTint, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Filled.DownloadDone, contentDescription = strings.navDownloads, tint = PrimaryNavy)
                }
              }
              Column {
                Text(strings.navDownloads, style = MaterialTheme.typography.titleSmall, color = PrimaryNavy, fontWeight = FontWeight.Bold)
                Text("${localDownloads.size} offline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }

      if (bookmarkedResources.isNotEmpty()) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Filled.Bookmark, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(20.dp))
              Text(
                text = strings.savedBookmarksTitle,
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryNavy,
                fontWeight = FontWeight.Bold,
              )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              items(bookmarkedResources, key = { "fav_${it.id}" }) { favRes ->
                Card(
                  onClick = { onResourceClick(favRes) },
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  border = BorderStroke(1.dp, BorderSubtle),
                  modifier = Modifier.width(210.dp).testTag("home_bookmark_card_${favRes.id}"),
                ) {
                  Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = favRes.fileType,
                      style = MaterialTheme.typography.labelSmall,
                      color = AccentOrange,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = favRes.title,
                      style = MaterialTheme.typography.titleSmall,
                      color = PrimaryNavy,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = favRes.fileSizeLabel,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Maps by Plot Size (3 Marla, 5 Marla, 10 Marla, 1 Kanal — 20 designs each)
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Filled.Map, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(22.dp))
              Text(
                text = strings.mapsByPlotSizeTitle,
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryNavy,
                fontWeight = FontWeight.Bold,
              )
            }
            TextButton(onClick = { onPlotSizeClick(selectedPlotSize) }) {
              Text(strings.seeAll, color = AccentOrange)
            }
          }

          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(plotSizes) { size ->
              val count = plotSizeCounts[size] ?: 20
              FilterChip(
                selected = selectedPlotSize == size,
                onClick = {
                  selectedPlotSize = size
                  showAllPlotMaps = false
                },
                label = { Text("$size ($count)") },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = PrimaryNavy,
                  selectedLabelColor = PureWhite,
                ),
                modifier = Modifier.testTag("plot_size_chip_${size.replace(" ", "_")}"),
              )
            }
          }

          Surface(
            color = if (plotRemaining == null || plotRemaining > 0) SuccessGreenBg else OrangeTint,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = when (plotRemaining) {
                null -> "Maps - $selectedPlotSize: Unlimited Pro Access (${selectedPlotMaps.size} designs)"
                1 -> "Maps - $selectedPlotSize: 1 free trial download available"
                else -> "Maps - $selectedPlotSize: Trial download used • Subscribe to unlock all ${selectedPlotMaps.size}"
              },
              style = MaterialTheme.typography.labelMedium,
              color = if (plotRemaining == null || plotRemaining > 0) SuccessGreen else AccentOrange,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            )
          }
        }
      }

      items(displayedPlotMaps, key = { "plot_${it.id}" }) { resource ->
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

      if (selectedPlotMaps.size > 3) {
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
              onClick = { showAllPlotMaps = !showAllPlotMaps },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).testTag("toggle_all_plot_maps_btn"),
            ) {
              Text(if (showAllPlotMaps) strings.showLessBtn else strings.showAllDesignsBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
              onClick = onRequestCustomMap,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).testTag("home_custom_map_btn"),
            ) {
              Icon(Icons.Filled.Architecture, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(strings.requestCustomMapBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = strings.categoriesTitle,
            style = MaterialTheme.typography.titleLarge,
            color = PrimaryNavy,
            fontWeight = FontWeight.Bold,
          )
          TextButton(onClick = onSeeAllLibrary, modifier = Modifier.testTag("home_see_all_categories")) {
            Text(strings.seeAll, color = AccentOrange)
          }
        }
      }

      item {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
          val cols = if (maxWidth >= 600.dp) 3 else 2
          val rows = homeCategories.chunked(cols)
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { rowItems ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                rowItems.forEach { cat ->
                  CategoryCard(
                    category = cat,
                    count = categoryCounts[cat] ?: 0,
                    strings = strings,
                    onClick = { onCategoryClick(cat) },
                    modifier = Modifier.weight(1f),
                  )
                }
                repeat(cols - rowItems.size) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = strings.recentLessonsTitle,
            style = MaterialTheme.typography.titleLarge,
            color = PrimaryNavy,
            fontWeight = FontWeight.Bold,
          )
          TextButton(onClick = onSeeAllLibrary, modifier = Modifier.testTag("home_see_all_recent")) {
            Text(strings.seeAll, color = AccentOrange)
          }
        }
      }

      items(recentResources, key = { it.id }) { resource ->
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
    }
  }
}
