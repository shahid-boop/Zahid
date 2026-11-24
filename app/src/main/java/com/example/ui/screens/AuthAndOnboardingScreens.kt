package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayLesson
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.i18n.AcademyStrings
import com.example.i18n.AppLanguage
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedBg
import com.example.ui.theme.NavyTint
import com.example.ui.theme.OrangeTint
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryNavy

@Composable
fun SplashScreen(strings: AcademyStrings) {
  val scale = remember { Animatable(0.84f) }
  val alpha = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    scale.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
    alpha.animateTo(1f, tween(500))
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Brush.verticalGradient(listOf(PrimaryNavy, SecondaryNavy)))
      .systemBarsPadding()
      .testTag("splash_screen"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier
        .padding(32.dp)
        .scale(scale.value)
        .alpha(alpha.value.coerceAtLeast(0.75f)),
    ) {
      Surface(
        shape = RoundedCornerShape(28.dp),
        color = PureWhite,
        shadowElevation = 10.dp,
        modifier = Modifier.size(104.dp),
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_launcher_art_1795445786223),
          contentDescription = strings.appName,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
      Text(
        text = strings.appName,
        style = MaterialTheme.typography.headlineMedium,
        color = PureWhite,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )
      Text(
        text = strings.tagline,
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite.copy(alpha = 0.85f),
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(8.dp))
      CircularProgressIndicator(
        color = AccentOrange,
        trackColor = PureWhite.copy(alpha = 0.2f),
        modifier = Modifier.size(32.dp),
      )
    }
  }
}

@Composable
fun OnboardingAndAuthScreen(
  strings: AcademyStrings,
  selectedLanguage: AppLanguage,
  isSigningIn: Boolean,
  authError: String?,
  onSelectLanguage: (AppLanguage) -> Unit,
  onGoogleSignInClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .systemBarsPadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 580.dp)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Language switcher
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AppLanguage.entries.forEach { lang ->
          FilterChip(
            selected = selectedLanguage == lang,
            onClick = { onSelectLanguage(lang) },
            label = { Text(lang.nativeLabel, style = MaterialTheme.typography.labelMedium) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = PrimaryNavy,
              selectedLabelColor = PureWhite,
            ),
            modifier = Modifier.padding(start = 6.dp).testTag("onboarding_lang_${lang.code}"),
          )
        }
      }

      // Hero Banner Card
      Card(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(PrimaryNavy, SecondaryNavy)))
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_academy_banner_1795445785907),
            contentDescription = strings.appName,
            contentScale = ContentScale.Crop,
            alpha = 0.25f,
            modifier = Modifier.matchParentSize(),
          )
          Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(PureWhite),
              ) {
                Image(
                  painter = painterResource(id = R.drawable.ic_launcher_art_1795445786223),
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              }
              Column {
                Surface(color = AccentOrange, shape = RoundedCornerShape(50)) {
                  Text(
                    text = strings.trialSummaryTitle,
                    color = PureWhite,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }
              }
            }
            Text(
              text = strings.onboardingTitle,
              style = MaterialTheme.typography.headlineSmall,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = strings.onboardingSubtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = PureWhite.copy(alpha = 0.9f),
            )
          }
        }
      }

      // Benefits
      BenefitCard(icon = Icons.Filled.CloudDownload, text = strings.benefit1)
      BenefitCard(icon = Icons.Filled.PlayLesson, text = strings.benefit2)
      BenefitCard(icon = Icons.Filled.Verified, text = strings.benefit3)

      // Trial Summary Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OrangeTint),
        border = BorderStroke(1.dp, AccentOrange.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(AccentOrange),
            contentAlignment = Alignment.Center,
          ) {
            Icon(Icons.Filled.Architecture, contentDescription = null, tint = PureWhite)
          }
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = strings.trialSummaryTitle,
              style = MaterialTheme.typography.titleMedium,
              color = PrimaryNavy,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = strings.trialSummaryDesc,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      if (!authError.isNullOrBlank()) {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = ErrorRedBg),
          modifier = Modifier.fillMaxWidth().testTag("auth_error_banner"),
        ) {
          Text(
            text = authError,
            color = ErrorRed,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      Spacer(Modifier.height(4.dp))

      Button(
        onClick = onGoogleSignInClick,
        enabled = !isSigningIn,
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy, contentColor = PureWhite),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("google_sign_in_button"),
      ) {
        if (isSigningIn) {
          CircularProgressIndicator(
            color = PureWhite,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(20.dp),
          )
          Spacer(Modifier.width(10.dp))
          Text(strings.signingIn, style = MaterialTheme.typography.titleSmall)
        } else {
          Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AccentOrange)
          Spacer(Modifier.width(10.dp))
          Text(strings.getStartedGoogle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
private fun BenefitCard(icon: ImageVector, text: String) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, BorderSubtle),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(NavyTint),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(20.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f),
      )
    }
  }
}
