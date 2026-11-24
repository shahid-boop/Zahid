package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.i18n.AcademyStrings
import com.example.model.AcademyNotification
import com.example.model.AccessEvaluator
import com.example.model.AppSettings
import com.example.model.CourseItem
import com.example.model.CourseProgress
import com.example.model.DownloadState
import com.example.model.EffectiveAccessState
import com.example.model.LocalDownloadedFile
import com.example.model.PaymentRequest
import com.example.model.ResourceCategory
import com.example.model.ResourceItem
import com.example.model.SubscriptionPlan
import com.example.model.UserProfile
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DeepBlueprintNavy
import com.example.ui.theme.EasyPaisaGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedBg
import com.example.ui.theme.JazzCashRed
import com.example.ui.theme.NavyTint
import com.example.ui.theme.OrangeTint
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryNavy
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenBg
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg
import com.example.ui.theme.WhatsAppGreen
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
  title: String,
  unreadNotifications: Int,
  photoUrl: String?,
  userInitial: String,
  onMenuClick: () -> Unit,
  onNotificationsClick: () -> Unit,
  onProfileClick: () -> Unit,
) {
  TopAppBar(
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = PrimaryNavy,
      titleContentColor = PureWhite,
      navigationIconContentColor = PureWhite,
      actionIconContentColor = PureWhite,
    ),
    navigationIcon = {
      IconButton(
        onClick = onMenuClick,
        modifier = Modifier.testTag("header_menu_button").minimumInteractiveComponentSize(),
      ) {
        Icon(Icons.Filled.Menu, contentDescription = "Menu")
      }
    },
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    },
    actions = {
      IconButton(
        onClick = onNotificationsClick,
        modifier = Modifier.testTag("header_notifications_button").minimumInteractiveComponentSize(),
      ) {
        BadgedBox(
          badge = {
            if (unreadNotifications > 0) {
              Badge(containerColor = AccentOrange, contentColor = PureWhite) {
                Text(unreadNotifications.coerceAtMost(99).toString())
              }
            }
          }
        ) {
          Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
        }
      }
      Spacer(Modifier.width(4.dp))
      Box(
        modifier = Modifier
          .padding(end = 12.dp)
          .size(36.dp)
          .clip(CircleShape)
          .background(AccentOrange)
          .clickable(onClick = onProfileClick)
          .testTag("header_avatar_button"),
        contentAlignment = Alignment.Center,
      ) {
        if (!photoUrl.isNullOrBlank()) {
          AsyncImage(
            model = photoUrl,
            contentDescription = "Profile",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
        } else {
          Text(
            text = userInitial.uppercase(),
            color = PureWhite,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    },
  )
}

enum class MainTab(val route: String) {
  HOME("home"),
  LIBRARY("library"),
  COURSES("courses"),
  DOWNLOADS("downloads"),
  PROFILE("profile"),
}

@Composable
fun AcademyBottomNavigation(
  currentRoute: String,
  strings: AcademyStrings,
  onSelectTab: (MainTab) -> Unit,
) {
  NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 4.dp,
    windowInsets = WindowInsets.navigationBars,
  ) {
    val items = listOf(
      Triple(MainTab.HOME, strings.navHome, Pair(Icons.Filled.Home, Icons.Outlined.Home)),
      Triple(MainTab.LIBRARY, strings.navLibrary, Pair(Icons.AutoMirrored.Filled.LibraryBooks, Icons.AutoMirrored.Outlined.LibraryBooks)),
      Triple(MainTab.COURSES, strings.navCourses, Pair(Icons.Filled.School, Icons.Outlined.School)),
      Triple(MainTab.DOWNLOADS, strings.navDownloads, Pair(Icons.Filled.DownloadDone, Icons.Filled.Download)),
      Triple(MainTab.PROFILE, strings.navProfile, Pair(Icons.Filled.Person, Icons.Outlined.Person)),
    )
    items.forEach { (tab, label, icons) ->
      val selected = currentRoute == tab.route
      NavigationBarItem(
        selected = selected,
        onClick = { onSelectTab(tab) },
        modifier = Modifier.testTag("bottom_nav_${tab.route}"),
        icon = {
          Icon(
            imageVector = if (selected) icons.first else icons.second,
            contentDescription = label,
          )
        },
        label = {
          Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = PrimaryNavy,
          selectedTextColor = PrimaryNavy,
          indicatorColor = NavyTint,
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
      )
    }
  }
}

fun openWhatsAppSupport(
  context: Context,
  whatsappNumber: String,
  message: String,
) {
  val rawDigits = whatsappNumber.filter { it.isDigit() }
  val digitsOnly = when {
    rawDigits.isBlank() -> "923467803931"
    rawDigits.startsWith("0") && rawDigits.length == 11 -> "92" + rawDigits.drop(1)
    else -> rawDigits
  }
  val encodedMsg = Uri.encode(message.ifBlank { "Hello Zahid Architecture Academy, I need assistance with my subscription." })
  val url = "https://wa.me/$digitsOnly?text=$encodedMsg"
  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }
  try {
    context.startActivity(intent)
  } catch (_: Exception) {
  }
}

@Composable
fun WhatsAppFloatingButton(
  whatsappNumber: String,
  defaultMessage: String,
  contentDescription: String,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  FloatingActionButton(
    onClick = {
      openWhatsAppSupport(context, whatsappNumber, defaultMessage)
    },
    shape = CircleShape,
    containerColor = WhatsAppGreen,
    contentColor = PureWhite,
    modifier = modifier
      .size(56.dp)
      .testTag("whatsapp_support_fab"),
  ) {
    // Recognizable official WhatsApp chat bubble + phone handset icon (no number displayed anywhere)
    Canvas(modifier = Modifier.size(28.dp)) {
      val w = size.width
      val h = size.height
      drawCircle(
        color = Color.White,
        radius = w * 0.42f,
        center = Offset(w * 0.52f, h * 0.48f),
        style = Stroke(width = w * 0.09f),
      )
      val tail = Path().apply {
        moveTo(w * 0.22f, h * 0.72f)
        lineTo(w * 0.12f, h * 0.90f)
        lineTo(w * 0.34f, h * 0.82f)
      }
      drawPath(tail, color = Color.White, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
      val phone = Path().apply {
        moveTo(w * 0.38f, h * 0.35f)
        quadraticTo(w * 0.44f, h * 0.58f, w * 0.66f, h * 0.61f)
      }
      drawPath(phone, color = Color.White, style = Stroke(width = w * 0.11f, cap = StrokeCap.Round))
    }
  }
}

@Composable
fun TrialBanner(
  profile: UserProfile?,
  strings: AcademyStrings,
  onStartTrial: () -> Unit,
  onUpgradeClick: () -> Unit,
  onQuickJazzCash: (() -> Unit)? = null,
  onQuickEasypaisa: (() -> Unit)? = null,
  onWhatsAppClick: (() -> Unit)? = null,
) {
  val state = AccessEvaluator.effectiveAccessState(profile)
  val daysLeft = AccessEvaluator.trialDaysRemaining(profile)

  Card(
    shape = RoundedCornerShape(22.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
    modifier = Modifier.fillMaxWidth().testTag("trial_banner_card"),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.linearGradient(
            colors = listOf(DeepBlueprintNavy, PrimaryNavy, SecondaryNavy)
          )
        )
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_academy_banner_1795445785907),
        contentDescription = null,
        alpha = 0.20f,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize(),
      )
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Surface(
            color = if (state == EffectiveAccessState.PRO_ACTIVE) SuccessGreen else AccentOrange,
            shape = RoundedCornerShape(50),
          ) {
            Text(
              text = when (state) {
                EffectiveAccessState.PRO_ACTIVE -> strings.proActiveBadge
                EffectiveAccessState.TRIAL_ACTIVE -> "${strings.trialActiveBadge} • ${daysLeft}d"
                EffectiveAccessState.TRIAL_EXPIRING_SOON -> "${strings.trialExpiringBadge} • ${daysLeft}d"
                EffectiveAccessState.TRIAL_EXPIRED -> strings.trialExpiredBadge
                else -> strings.freeTrialBannerTitle
              },
              color = PureWhite,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
          }
          Icon(
            imageVector = Icons.Filled.Architecture,
            contentDescription = null,
            tint = PureWhite.copy(alpha = 0.85f),
            modifier = Modifier.size(26.dp),
          )
        }

        Text(
          text = strings.freeTrialBannerTitle,
          style = MaterialTheme.typography.headlineSmall,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = if (state == EffectiveAccessState.PRO_ACTIVE) {
            strings.subscribedUnlimitedLabel
          } else {
            "${strings.trialQuotaOnePerCategory} • ${strings.freeTrialBannerSubtitle}"
          },
          style = MaterialTheme.typography.bodySmall,
          color = PureWhite.copy(alpha = 0.9f),
        )

        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          if (state == EffectiveAccessState.NOT_STARTED) {
            Button(
              onClick = onStartTrial,
              colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, contentColor = PureWhite),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).testTag("activate_trial_button"),
            ) {
              Text(strings.startTrialBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
          OutlinedButton(
            onClick = onUpgradeClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, PureWhite.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag("banner_subscribe_button"),
          ) {
            Text(strings.subscribeNowBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }

        if (onQuickJazzCash != null && onQuickEasypaisa != null) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Button(
              onClick = onQuickJazzCash,
              colors = ButtonDefaults.buttonColors(containerColor = JazzCashRed, contentColor = PureWhite),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.weight(1f).testTag("quick_jazzcash_btn"),
            ) {
              Text(strings.payWithJazzCash, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Button(
              onClick = onQuickEasypaisa,
              colors = ButtonDefaults.buttonColors(containerColor = EasyPaisaGreen, contentColor = PureWhite),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.weight(1f).testTag("quick_easypaisa_btn"),
            ) {
              Text(strings.payWithEasypaisa, fontWeight = FontWeight.Bold, maxLines = 1)
            }
          }
        }

        if (onWhatsAppClick != null) {
          Button(
            onClick = onWhatsAppClick,
            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = PureWhite),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("banner_whatsapp_btn"),
          ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(strings.whatsappContactBtn, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun QuickPaymentDialog(
  initialProvider: String,
  appSettings: AppSettings,
  plans: List<SubscriptionPlan>,
  isSubmitting: Boolean,
  strings: AcademyStrings,
  onDismiss: () -> Unit,
  onSubmitPayment: (SubscriptionPlan, String, String, Uri?) -> Unit,
  onOpenWhatsApp: (String) -> Unit,
) {
  var provider by remember(initialProvider) { mutableStateOf(initialProvider) }
  var selectedPlanId by remember(plans) { mutableStateOf(plans.firstOrNull()?.id ?: "") }
  var txReference by remember { mutableStateOf("") }
  var proofUri by remember { mutableStateOf<Uri?>(null) }

  val plan = plans.firstOrNull { it.id == selectedPlanId } ?: plans.firstOrNull()
  val photoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri -> if (uri != null) proofUri = uri }

  val accTitle = if (provider == "JAZZCASH") appSettings.jazzCashAccountName else appSettings.easypaisaAccountName
  val accNumber = if (provider == "JAZZCASH") appSettings.jazzCashAccountNumber else appSettings.easypaisaAccountNumber
  val instructions = if (provider == "JAZZCASH") appSettings.jazzCashInstructions else appSettings.easypaisaInstructions

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "${if (provider == "JAZZCASH") strings.payWithJazzCash else strings.payWithEasypaisa} • ${strings.subscriptionPlansTitle}",
        style = MaterialTheme.typography.titleMedium,
        color = PrimaryNavy,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          FilterChip(
            selected = provider == "JAZZCASH",
            onClick = { provider = "JAZZCASH" },
            label = { Text(strings.payWithJazzCash) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = JazzCashRed, selectedLabelColor = PureWhite),
            modifier = Modifier.weight(1f),
          )
          FilterChip(
            selected = provider == "EASYPAISA",
            onClick = { provider = "EASYPAISA" },
            label = { Text(strings.payWithEasypaisa) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EasyPaisaGreen, selectedLabelColor = PureWhite),
            modifier = Modifier.weight(1f),
          )
        }

        if (plans.isNotEmpty()) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            plans.take(2).forEach { p ->
              FilterChip(
                selected = p.id == plan?.id,
                onClick = { selectedPlanId = p.id },
                label = { Text("${p.title} (PKR ${p.pricePkr})", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryNavy, selectedLabelColor = PureWhite),
                modifier = Modifier.weight(1f),
              )
            }
          }
        }

        Surface(color = OrangeTint, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Account Title: $accTitle", style = MaterialTheme.typography.labelLarge, color = PrimaryNavy)
            Text("Account / Mobile Number: $accNumber", style = MaterialTheme.typography.titleSmall, color = AccentOrange, fontWeight = FontWeight.Bold)
            plan?.let {
              Text("Amount: PKR ${it.pricePkr} (${it.durationDays} days)", style = MaterialTheme.typography.labelMedium, color = PrimaryNavy)
            }
            Text(instructions, style = MaterialTheme.typography.bodySmall)
          }
        }

        OutlinedTextField(
          value = txReference,
          onValueChange = { txReference = it },
          label = { Text(strings.txReferenceLabel) },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().testTag("quick_pay_tid_input"),
        )

        OutlinedButton(
          onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(
            imageVector = if (proofUri != null) Icons.Filled.CheckCircle else Icons.Filled.Image,
            contentDescription = null,
            tint = if (proofUri != null) SuccessGreen else PrimaryNavy,
          )
          Spacer(Modifier.width(8.dp))
          Text(if (proofUri != null) strings.proofAttached else strings.attachProofBtn)
        }

        Button(
          onClick = {
            val planText = plan?.let { "${it.title} (PKR ${it.pricePkr})" } ?: "Monthly Pro (PKR 999)"
            onOpenWhatsApp("Assalam-o-Alaikum, I want to send my $provider payment screenshot for Zahid Architecture Academy $planText.")
          },
          colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = PureWhite),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().testTag("quick_pay_whatsapp_btn"),
        ) {
          Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Text(strings.sendScreenshotWhatsapp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (plan != null) {
            onSubmitPayment(plan, provider, txReference, proofUri)
            if (txReference.trim().length >= 4) {
              onDismiss()
            }
          }
        },
        enabled = !isSubmitting && plan != null,
        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, contentColor = PureWhite),
        modifier = Modifier.testTag("quick_pay_submit_btn"),
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(color = PureWhite, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
          Text(strings.submitPaymentBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancelBtn) }
    },
  )
}

@Composable
fun RequestCustomMapDialog(
  strings: AcademyStrings,
  onDismiss: () -> Unit,
  onSendWhatsAppBrief: (String) -> Unit,
) {
  val plotSizes = listOf("3 Marla", "5 Marla", "10 Marla", "1 Kanal")
  val facings = listOf("North", "South", "East", "West")
  val floorsList = listOf("Single Unit", "Double Unit", "Basement + G + 1")

  var selectedSize by remember { mutableStateOf("5 Marla") }
  var selectedFacing by remember { mutableStateOf("North") }
  var selectedFloors by remember { mutableStateOf("Double Unit") }
  var isCornerPlot by remember { mutableStateOf(false) }
  var notes by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = strings.customMapTitle,
        style = MaterialTheme.typography.titleMedium,
        color = PrimaryNavy,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = strings.customMapDesc,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("Plot Size:", style = MaterialTheme.typography.labelLarge, color = PrimaryNavy)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
          plotSizes.take(2).forEach { size ->
            FilterChip(
              selected = selectedSize == size,
              onClick = { selectedSize = size },
              label = { Text(size) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryNavy, selectedLabelColor = PureWhite),
              modifier = Modifier.weight(1f),
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
          plotSizes.drop(2).forEach { size ->
            FilterChip(
              selected = selectedSize == size,
              onClick = { selectedSize = size },
              label = { Text(size) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryNavy, selectedLabelColor = PureWhite),
              modifier = Modifier.weight(1f),
            )
          }
        }

        Text("Facing Direction:", style = MaterialTheme.typography.labelLarge, color = PrimaryNavy)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
          facings.forEach { dir ->
            FilterChip(
              selected = selectedFacing == dir,
              onClick = { selectedFacing = dir },
              label = { Text(dir, maxLines = 1) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentOrange, selectedLabelColor = PureWhite),
              modifier = Modifier.weight(1f),
            )
          }
        }

        Text("Storeys / Unit Type:", style = MaterialTheme.typography.labelLarge, color = PrimaryNavy)
        floorsList.forEach { fl ->
          FilterChip(
            selected = selectedFloors == fl,
            onClick = { selectedFloors = fl },
            label = { Text(fl) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryNavy, selectedLabelColor = PureWhite),
            modifier = Modifier.fillMaxWidth(),
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Corner Plot", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = isCornerPlot, onCheckedChange = { isCornerPlot = it })
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Rooms / Elevation Requirements (Optional)") },
          minLines = 2,
          maxLines = 4,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().testTag("custom_map_notes_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val cornerLabel = if (isCornerPlot) "Yes (Corner Plot)" else "Standard"
          val msg = buildString {
            append("Assalam-o-Alaikum, I want to request a custom House Map (Naqsha) from Zahid Architecture Academy:\n")
            append("• Plot Size: $selectedSize\n")
            append("• Facing: $selectedFacing\n")
            append("• Configuration: $selectedFloors\n")
            append("• Corner Plot: $cornerLabel")
            if (notes.isNotBlank()) {
              append("\n• Notes: ${notes.trim()}")
            }
          }
          onSendWhatsAppBrief(msg)
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = PureWhite),
        modifier = Modifier.testTag("custom_map_whatsapp_send_btn"),
      ) {
        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(strings.sendCustomMapWhatsapp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancelBtn) }
    },
  )
}

fun categoryIcon(category: ResourceCategory): ImageVector = when (category) {
  ResourceCategory.PDF_LESSONS -> Icons.Filled.PictureAsPdf
  ResourceCategory.VIDEOS -> Icons.Filled.PlayCircle
  ResourceCategory.PICTURES -> Icons.Filled.Image
  ResourceCategory.NAQSHAY -> Icons.Filled.Map
  ResourceCategory.MAX_3D -> Icons.Filled.ViewInAr
  ResourceCategory.DWG -> Icons.Filled.Architecture
  ResourceCategory.DXF -> Icons.Filled.Layers
  ResourceCategory.LECTURES -> Icons.Filled.School
  ResourceCategory.DRAWINGS -> Icons.Filled.Architecture
  ResourceCategory.SKETCHUP -> Icons.Filled.ViewInAr
}

@Composable
fun CategoryCard(
  category: ResourceCategory,
  count: Int,
  strings: AcademyStrings,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier.testTag("category_card_${category.id}"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NavyTint),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = categoryIcon(category),
            contentDescription = category.localizedLabel(strings),
            tint = PrimaryNavy,
            modifier = Modifier.size(22.dp),
          )
        }
        Surface(
          color = OrangeTint,
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = AccentOrange,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }
      Text(
        text = category.localizedLabel(strings),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun ResourceCard(
  resource: ResourceItem,
  strings: AcademyStrings,
  hasAccess: Boolean,
  isFavorited: Boolean,
  isDownloaded: Boolean,
  onCardClick: () -> Unit,
  onViewClick: () -> Unit,
  onDownloadClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  canDownload: Boolean = hasAccess,
) {
  val category = ResourceCategory.fromId(resource.categoryId)
  val dateStr = formatTimestamp(resource.updatedAt ?: resource.createdAt)

  Card(
    onClick = onCardClick,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth().testTag("resource_card_${resource.id}"),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (resource.premium) OrangeTint else NavyTint),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = categoryIcon(category),
            contentDescription = resource.fileType,
            tint = if (resource.premium) AccentOrange else PrimaryNavy,
          )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            text = resource.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(color = NavyTint, shape = RoundedCornerShape(6.dp)) {
              Text(
                text = resource.plotSize ?: resource.fileType,
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryNavy,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
            Text(
              text = resource.fileSizeLabel,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (dateStr.isNotEmpty()) {
              Text(
                text = "• $dateStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        IconButton(
          onClick = onToggleFavorite,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("fav_btn_${resource.id}"),
        ) {
          Icon(
            imageVector = if (isFavorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            contentDescription = strings.favoriteBtn,
            tint = if (isFavorited) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (isDownloaded) {
          Surface(color = SuccessGreenBg, shape = RoundedCornerShape(8.dp)) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
              Text("Saved", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
            }
          }
        } else if ((resource.premium && !hasAccess) || !canDownload) {
          Surface(color = ErrorRedBg, shape = RoundedCornerShape(8.dp)) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(Icons.Filled.Lock, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp))
              Text(
                text = if (hasAccess && !canDownload) "Quota Used" else "Pro",
                style = MaterialTheme.typography.labelSmall,
                color = ErrorRed,
              )
            }
          }
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
          onClick = onViewClick,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("view_btn_${resource.id}"),
        ) {
          Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(5.dp))
          Text(strings.viewBtn, style = MaterialTheme.typography.labelMedium)
        }

        Button(
          onClick = onDownloadClick,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (canDownload) PrimaryNavy else AccentOrange,
            contentColor = PureWhite,
          ),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("download_btn_${resource.id}"),
        ) {
          Icon(
            imageVector = if (canDownload) Icons.Filled.Download else Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.width(5.dp))
          Text(strings.downloadBtn, style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}

@Composable
fun CourseCard(
  course: CourseItem,
  progress: CourseProgress?,
  strings: AcademyStrings,
  onClick: () -> Unit,
) {
  val thumbRes = when {
    course.id.contains("3dmax", true) || course.categoryId == "MAX_3D" -> R.drawable.img_course_3dmax_1795445786941
    course.id.contains("naq", true) || course.categoryId == "NAQSHAY" -> R.drawable.img_course_naqshay_1795445785729
    else -> R.drawable.img_course_autocad_1795445786167
  }
  val pct = progress?.progressPercent?.toInt() ?: 0

  Card(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth().testTag("course_card_${course.id}"),
  ) {
    Column {
      Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        if (!course.thumbnailUrl.isNullOrBlank()) {
          AsyncImage(
            model = course.thumbnailUrl,
            contentDescription = course.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Image(
            painter = painterResource(id = thumbRes),
            contentDescription = course.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        }
        Surface(
          color = if (course.premium) AccentOrange else SuccessGreen,
          shape = RoundedCornerShape(50),
          modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        ) {
          Text(
            text = if (course.premium) strings.premiumFilter else strings.freeFilter,
            color = PureWhite,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = course.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${course.instructorName} • ${course.totalLessons} ${strings.lessonsLabel} • ${course.durationLabel}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (pct > 0) {
          Spacer(Modifier.height(4.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            LinearProgressIndicator(
              progress = { pct / 100f },
              color = AccentOrange,
              trackColor = NavyTint,
              modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(4.dp)),
            )
            Text(
              text = "$pct%",
              style = MaterialTheme.typography.labelSmall,
              color = PrimaryNavy,
            )
          }
        }
      }
    }
  }
}

@Composable
fun SubscriptionCard(
  plan: SubscriptionPlan,
  isSelected: Boolean,
  onSelect: () -> Unit,
) {
  Card(
    onClick = onSelect,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) NavyTint else MaterialTheme.colorScheme.surface,
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = if (isSelected) 2.dp else 1.dp,
      color = if (isSelected) PrimaryNavy else BorderSubtle,
    ),
    modifier = Modifier.fillMaxWidth().testTag("plan_card_${plan.id}"),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(plan.title, style = MaterialTheme.typography.titleMedium, color = PrimaryNavy)
          plan.badgeText?.takeIf { it.isNotBlank() }?.let { badge ->
            Surface(color = AccentOrange, shape = RoundedCornerShape(50)) {
              Text(
                text = badge,
                color = PureWhite,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
          }
        }
        Text(plan.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Spacer(Modifier.width(12.dp))
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "PKR ${plan.pricePkr}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = PrimaryNavy,
        )
        Text(
          text = "${plan.durationDays} days",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun PaymentCard(payment: PaymentRequest) {
  val (bg, fg) = when (payment.status) {
    "APPROVED" -> SuccessGreenBg to SuccessGreen
    "REJECTED" -> ErrorRedBg to ErrorRed
    else -> WarningAmberBg to WarningAmber
  }
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(20.dp))
          Text(
            text = "${payment.planTitle} • ${payment.provider}",
            style = MaterialTheme.typography.titleSmall,
          )
        }
        Surface(color = bg, shape = RoundedCornerShape(50)) {
          Text(
            text = payment.status,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
          )
        }
      }
      Text(
        text = "PKR ${payment.amountPkr} • Ref: ${payment.transactionReference}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      payment.verificationNote?.takeIf { it.isNotBlank() }?.let { note ->
        Text(
          text = "Note: $note",
          style = MaterialTheme.typography.bodySmall,
          color = fg,
        )
      }
    }
  }
}

@Composable
fun AcademySearchBar(
  query: String,
  hint: String,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    placeholder = { Text(hint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = PrimaryNavy) },
    trailingIcon = {
      if (query.isNotEmpty()) {
        IconButton(onClick = { onQueryChange("") }) {
          Icon(Icons.Filled.Close, contentDescription = "Clear")
        }
      }
    },
    singleLine = true,
    shape = RoundedCornerShape(14.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = PrimaryNavy,
      unfocusedBorderColor = BorderSubtle,
      focusedContainerColor = MaterialTheme.colorScheme.surface,
      unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    ),
    modifier = modifier.fillMaxWidth().testTag("search_input"),
  )
}

@Composable
fun FilterPanel(
  selectedAccess: String,
  selectedSort: String,
  strings: AcademyStrings,
  onAccessChange: (String) -> Unit,
  onSortChange: (String) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(Icons.Filled.FilterList, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(18.dp))
    listOf("ALL" to strings.allFilter, "FREE" to strings.freeFilter, "PREMIUM" to strings.premiumFilter).forEach { (key, label) ->
      FilterChip(
        selected = selectedAccess == key,
        onClick = { onAccessChange(key) },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = PrimaryNavy,
          selectedLabelColor = PureWhite,
        ),
      )
    }
    Spacer(Modifier.weight(1f))
    val nextSort = when (selectedSort) {
      "NEWEST" -> "TITLE"
      "TITLE" -> "SIZE"
      else -> "NEWEST"
    }
    val sortLabel = when (selectedSort) {
      "TITLE" -> strings.sortTitle
      "SIZE" -> strings.sortSize
      else -> strings.sortNewest
    }
    FilterChip(
      selected = true,
      onClick = { onSortChange(nextSort) },
      label = { Text(sortLabel, style = MaterialTheme.typography.labelMedium) },
      colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = OrangeTint,
        selectedLabelColor = AccentOrange,
      ),
    )
  }
}

@Composable
fun NotificationItemRow(
  item: AcademyNotification,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (item.read) MaterialTheme.colorScheme.surface else NavyTint.copy(alpha = 0.55f),
    ),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Box(
        modifier = Modifier
          .padding(top = 4.dp)
          .size(10.dp)
          .clip(CircleShape)
          .background(if (item.read) BorderSubtle else AccentOrange)
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = item.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = if (item.read) FontWeight.Medium else FontWeight.Bold,
        )
        Text(text = item.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
          text = formatTimestamp(item.createdAt),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun UserProfileCard(
  profile: UserProfile?,
  fallbackEmail: String,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = PrimaryNavy),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(18.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier = Modifier.size(56.dp).clip(CircleShape).background(AccentOrange),
        contentAlignment = Alignment.Center,
      ) {
        if (!profile?.photoUrl.isNullOrBlank()) {
          AsyncImage(
            model = profile.photoUrl,
            contentDescription = "Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Text(
            text = (profile?.fullName?.firstOrNull() ?: 'Z').uppercase(),
            color = PureWhite,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
          )
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          text = profile?.fullName?.ifBlank { "Academy Member" } ?: "Academy Member",
          style = MaterialTheme.typography.titleMedium,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = profile?.email?.ifBlank { fallbackEmail } ?: fallbackEmail,
          style = MaterialTheme.typography.bodySmall,
          color = PureWhite.copy(alpha = 0.85f),
        )
        Surface(color = PureWhite.copy(alpha = 0.16f), shape = RoundedCornerShape(50)) {
          Text(
            text = profile?.role ?: "USER",
            color = PureWhite,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          )
        }
      }
    }
  }
}

@Composable
fun AdminStatCard(
  label: String,
  value: String,
  accent: Color = PrimaryNavy,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(text = value, style = MaterialTheme.typography.headlineSmall, color = accent, fontWeight = FontWeight.Bold)
      Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
  }
}

@Composable
fun DownloadProgressBanner(
  state: DownloadState,
  strings: AcademyStrings,
  onCancel: () -> Unit,
  onOpenFile: (LocalDownloadedFile) -> Unit,
  onDismiss: () -> Unit,
) {
  AnimatedVisibility(visible = state !is DownloadState.Idle) {
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryNavy.copy(alpha = 0.35f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).testTag("download_progress_banner"),
    ) {
      when (state) {
        is DownloadState.InProgress -> {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
              Text(
                text = "${strings.downloading}: ${state.fileName}",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = "${state.percent}%",
                style = MaterialTheme.typography.labelSmall,
                color = AccentOrange,
                fontWeight = FontWeight.Bold,
              )
              IconButton(onClick = onCancel, modifier = Modifier.size(32.dp).testTag("cancel_download_btn")) {
                Icon(Icons.Filled.Cancel, contentDescription = strings.cancelBtn, tint = ErrorRed)
              }
            }
            LinearProgressIndicator(
              progress = { state.percent / 100f },
              color = AccentOrange,
              trackColor = NavyTint,
              modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
            )
          }
        }
        is DownloadState.Completed -> {
          Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen)
            Column(modifier = Modifier.weight(1f)) {
              Text(state.localFile.fileName, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text(state.localFile.displayLocation, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = { onOpenFile(state.localFile) }, modifier = Modifier.testTag("open_downloaded_btn")) {
              Text(strings.openFileBtn)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Filled.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
            }
          }
        }
        is DownloadState.Failed -> {
          Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = ErrorRed)
            Text(state.message, style = MaterialTheme.typography.bodySmall, color = ErrorRed, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Filled.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
            }
          }
        }
        DownloadState.Idle -> Unit
      }
    }
  }
}

@Composable
fun ConfirmationDialog(
  title: String,
  message: String,
  confirmLabel: String,
  cancelLabel: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, style = MaterialTheme.typography.titleMedium) },
    text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = PureWhite),
      ) {
        Text(confirmLabel)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(cancelLabel)
      }
    },
  )
}

fun formatTimestamp(ts: Timestamp?): String {
  val date = ts?.toDate() ?: return ""
  return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
}

fun formatDateMs(ms: Long): String {
  return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ms))
}

fun formatBytes(bytes: Long): String {
  if (bytes < 1024L) return "$bytes B"
  val kb = bytes / 1024.0
  if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
  val mb = kb / 1024.0
  return String.format(Locale.US, "%.2f MB", mb)
}
