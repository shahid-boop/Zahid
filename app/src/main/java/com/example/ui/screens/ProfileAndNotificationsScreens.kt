package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.example.i18n.AcademyStrings
import com.example.i18n.AppLanguage
import com.example.model.AcademyNotification
import com.example.model.AccessEvaluator
import com.example.model.EffectiveAccessState
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.NotificationItemRow
import com.example.ui.components.UserProfileCard
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedBg
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.PureWhite

@Composable
fun NotificationsScreen(
  notifications: List<AcademyNotification>,
  strings: AcademyStrings,
  onMarkRead: (String) -> Unit,
  onMarkAllRead: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize().testTag("notifications_screen"), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 800.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = strings.notificationsTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = PrimaryNavy,
            fontWeight = FontWeight.Bold,
          )
          if (notifications.any { !it.read }) {
            TextButton(onClick = onMarkAllRead, modifier = Modifier.testTag("mark_all_read_btn")) {
              Text(strings.markAllReadBtn, color = AccentOrange)
            }
          }
        }
      }

      if (notifications.isEmpty()) {
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
              Icon(Icons.Filled.NotificationsNone, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(44.dp))
              Text(strings.notificationsTitle, style = MaterialTheme.typography.titleMedium)
              Text(
                text = "Academy updates, trial reminders, and payment status alerts appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(notifications, key = { it.id }) { notif ->
          NotificationItemRow(
            item = notif,
            onClick = { if (!notif.read) onMarkRead(notif.id) },
          )
        }
      }
    }
  }
}

@Composable
fun ProfileScreen(
  profile: UserProfile?,
  fallbackEmail: String,
  selectedLanguage: AppLanguage,
  strings: AcademyStrings,
  onUpdateName: (String) -> Unit,
  onSelectLanguage: (AppLanguage) -> Unit,
  onOpenDownloads: () -> Unit,
  onOpenTools: () -> Unit = {},
  onRequestCustomMap: () -> Unit = {},
  onOpenSubscription: () -> Unit,
  onOpenAdmin: () -> Unit,
  onSignOut: () -> Unit,
) {
  var showEditDialog by remember { mutableStateOf(false) }
  var editNameText by remember(profile?.fullName) { mutableStateOf(profile?.fullName ?: "") }
  val isStaff = UserRole.fromString(profile?.role).isStaff()
  val accessState = AccessEvaluator.effectiveAccessState(profile)
  val trialDaysLeft = AccessEvaluator.trialDaysRemaining(profile)

  if (showEditDialog) {
    AlertDialog(
      onDismissRequest = { showEditDialog = false },
      title = { Text(strings.editNameBtn) },
      text = {
        OutlinedTextField(
          value = editNameText,
          onValueChange = { editNameText = it },
          singleLine = true,
          label = { Text("Full Name") },
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (editNameText.isNotBlank()) {
              onUpdateName(editNameText)
            }
            showEditDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy, contentColor = PureWhite),
        ) {
          Text(strings.saveChangesBtn)
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditDialog = false }) { Text(strings.cancelBtn) }
      },
    )
  }

  Box(modifier = Modifier.fillMaxSize().testTag("profile_screen"), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      UserProfileCard(profile = profile, fallbackEmail = fallbackEmail)

      if (profile?.accountStatus == "SUSPENDED") {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = ErrorRedBg),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = ErrorRed)
            Text(strings.accountSuspendedBanner, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
          }
        }
      }

      OutlinedButton(
        onClick = {
          editNameText = profile?.fullName ?: ""
          showEditDialog = true
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("edit_name_btn"),
      ) {
        Icon(Icons.Filled.Edit, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(strings.editNameBtn)
      }

      // Subscription Summary Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = when (accessState) {
              EffectiveAccessState.PRO_ACTIVE -> strings.proActiveBadge
              EffectiveAccessState.TRIAL_ACTIVE -> "${strings.trialActiveBadge} • $trialDaysLeft d"
              EffectiveAccessState.TRIAL_EXPIRING_SOON -> "${strings.trialExpiringBadge} • $trialDaysLeft d"
              EffectiveAccessState.TRIAL_EXPIRED -> strings.trialExpiredBadge
              else -> strings.trialSummaryTitle
            },
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryNavy,
            fontWeight = FontWeight.Bold,
          )
          Button(
            onClick = onOpenSubscription,
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, contentColor = PureWhite),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("profile_subscription_btn"),
          ) {
            Icon(Icons.Filled.Subscriptions, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.navSubscription)
          }
        }
      }

      // Language Switcher
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Language, contentDescription = null, tint = PrimaryNavy)
            Text(strings.languageLabel, style = MaterialTheme.typography.titleSmall, color = PrimaryNavy)
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AppLanguage.entries.forEach { lang ->
              FilterChip(
                selected = selectedLanguage == lang,
                onClick = { onSelectLanguage(lang) },
                label = { Text(lang.nativeLabel) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = PrimaryNavy,
                  selectedLabelColor = PureWhite,
                ),
                modifier = Modifier.weight(1f).testTag("profile_lang_${lang.code}"),
              )
            }
          }
        }
      }

      // Quick Actions
      OutlinedButton(
        onClick = onOpenTools,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_tools_btn"),
      ) {
        Icon(Icons.Filled.Calculate, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(strings.navTools)
      }

      OutlinedButton(
        onClick = onRequestCustomMap,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_custom_map_btn"),
      ) {
        Icon(Icons.Filled.Architecture, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(strings.requestCustomMapBtn)
      }

      OutlinedButton(
        onClick = onOpenDownloads,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_downloads_btn"),
      ) {
        Icon(Icons.Filled.DownloadDone, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(strings.myDownloadsTitle)
      }

      if (isStaff) {
        Button(
          onClick = onOpenAdmin,
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy, contentColor = PureWhite),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_admin_btn"),
        ) {
          Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = AccentOrange)
          Spacer(Modifier.width(8.dp))
          Text(strings.navAdmin)
        }
      }

      OutlinedButton(
        onClick = onSignOut,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("logout_button"),
      ) {
        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(strings.logoutBtn)
      }
    }
  }
}
