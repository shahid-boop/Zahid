package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.i18n.AcademyStrings
import com.example.model.AccessEvaluator
import com.example.model.AppSettings
import com.example.model.EffectiveAccessState
import com.example.model.PaymentRequest
import com.example.model.SubscriptionPlan
import com.example.model.UserProfile
import com.example.ui.components.PaymentCard
import com.example.ui.components.SubscriptionCard
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.NavyTint
import com.example.ui.theme.OrangeTint
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WhatsAppGreen

@Composable
fun SubscriptionAndPaymentScreen(
  profile: UserProfile?,
  appSettings: AppSettings,
  plans: List<SubscriptionPlan>,
  userPayments: List<PaymentRequest>,
  isSubmitting: Boolean,
  strings: AcademyStrings,
  onActivateTrial: () -> Unit,
  onSubmitPayment: (SubscriptionPlan, String, String, Uri?) -> Unit,
  onOpenWhatsApp: (String) -> Unit = {},
) {
  var selectedPlanId by remember(plans) { mutableStateOf(plans.firstOrNull()?.id ?: "") }
  var selectedProvider by remember { mutableStateOf("JAZZCASH") }
  var txReference by remember { mutableStateOf("") }
  var proofUri by remember { mutableStateOf<Uri?>(null) }

  val photoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) proofUri = uri
  }

  val selectedPlan = plans.firstOrNull { it.id == selectedPlanId } ?: plans.firstOrNull()
  val accessState = AccessEvaluator.effectiveAccessState(profile)
  val trialDaysLeft = AccessEvaluator.trialDaysRemaining(profile)

  Box(modifier = Modifier.fillMaxSize().testTag("subscription_screen"), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 800.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Current Status Card
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = PrimaryNavy),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Filled.Verified, contentDescription = null, tint = AccentOrange)
              Text(
                text = when (accessState) {
                  EffectiveAccessState.PRO_ACTIVE -> strings.proActiveBadge
                  EffectiveAccessState.TRIAL_ACTIVE -> "${strings.trialActiveBadge} ($trialDaysLeft days remaining)"
                  EffectiveAccessState.TRIAL_EXPIRING_SOON -> "${strings.trialExpiringBadge} ($trialDaysLeft days left)"
                  EffectiveAccessState.TRIAL_EXPIRED -> strings.trialExpiredBadge
                  EffectiveAccessState.SUSPENDED -> strings.accountSuspendedBanner
                  EffectiveAccessState.NOT_STARTED -> strings.trialSummaryTitle
                },
                style = MaterialTheme.typography.titleMedium,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
              )
            }
            profile?.subscriptionExpiresAt?.let { exp ->
              if (accessState == EffectiveAccessState.PRO_ACTIVE) {
                Text(
                  text = "Valid until: ${formatTimestamp(exp)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = PureWhite.copy(alpha = 0.85f),
                )
              }
            }
            if (accessState == EffectiveAccessState.NOT_STARTED && appSettings.trialEnabled) {
              Button(
                onClick = onActivateTrial,
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, contentColor = PureWhite),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("sub_activate_trial_btn"),
              ) {
                Text(strings.startTrialBtn)
              }
            }
          }
        }
      }

      item {
        Text(
          text = strings.subscriptionPlansTitle,
          style = MaterialTheme.typography.titleLarge,
          color = PrimaryNavy,
          fontWeight = FontWeight.Bold,
        )
      }

      items(plans, key = { it.id }) { plan ->
        SubscriptionCard(
          plan = plan,
          isSelected = plan.id == selectedPlan?.id,
          onSelect = { selectedPlanId = plan.id },
        )
      }

      // Payment Form
      if (selectedPlan != null && profile != null) {
        item {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = PrimaryNavy)
                Text(strings.paymentMethodTitle, style = MaterialTheme.typography.titleMedium, color = PrimaryNavy)
              }

              Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                  selected = selectedProvider == "JAZZCASH",
                  onClick = { selectedProvider = "JAZZCASH" },
                  label = { Text(strings.payWithJazzCash) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryNavy, selectedLabelColor = PureWhite),
                  modifier = Modifier.weight(1f).testTag("provider_jazzcash"),
                )
                FilterChip(
                  selected = selectedProvider == "EASYPAISA",
                  onClick = { selectedProvider = "EASYPAISA" },
                  label = { Text(strings.payWithEasypaisa) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryNavy, selectedLabelColor = PureWhite),
                  modifier = Modifier.weight(1f).testTag("provider_easypaisa"),
                )
              }

              val accName = if (selectedProvider == "JAZZCASH") appSettings.jazzCashAccountName else appSettings.easypaisaAccountName
              val accNum = if (selectedProvider == "JAZZCASH") appSettings.jazzCashAccountNumber else appSettings.easypaisaAccountNumber
              val instructions = if (selectedProvider == "JAZZCASH") appSettings.jazzCashInstructions else appSettings.easypaisaInstructions

              Surface(color = OrangeTint, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text("Account Title: $accName", style = MaterialTheme.typography.titleSmall, color = PrimaryNavy)
                  Text("Account / Mobile Number: $accNum", style = MaterialTheme.typography.titleSmall, color = AccentOrange, fontWeight = FontWeight.Bold)
                  Text("Amount: PKR ${selectedPlan.pricePkr}", style = MaterialTheme.typography.labelLarge, color = PrimaryNavy)
                  Spacer(Modifier.height(2.dp))
                  Text(instructions, style = MaterialTheme.typography.bodySmall)
                }
              }

              OutlinedTextField(
                value = txReference,
                onValueChange = { txReference = it },
                label = { Text(strings.txReferenceLabel) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("tx_reference_input"),
              )

              OutlinedButton(
                onClick = {
                  photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("attach_proof_btn"),
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
                  onSubmitPayment(selectedPlan, selectedProvider, txReference, proofUri)
                  if (txReference.trim().length >= 4) {
                    txReference = ""
                    proofUri = null
                  }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, contentColor = PureWhite),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("submit_payment_btn"),
              ) {
                if (isSubmitting) {
                  CircularProgressIndicator(color = PureWhite, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                  Text(strings.submitPaymentBtn, fontWeight = FontWeight.Bold)
                }
              }

              Button(
                onClick = {
                  onOpenWhatsApp("Assalam-o-Alaikum, I want to send my $selectedProvider payment screenshot for Zahid Architecture Academy ${selectedPlan.title} (PKR ${selectedPlan.pricePkr}).")
                },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = PureWhite),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sub_whatsapp_receipt_btn"),
              ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.sendScreenshotWhatsapp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      if (userPayments.isNotEmpty()) {
        item {
          Text(
            text = strings.paymentHistoryTitle,
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryNavy,
            fontWeight = FontWeight.Bold,
          )
        }
        items(userPayments, key = { it.id }) { payment ->
          PaymentCard(payment = payment)
        }
      }
    }
  }
}
