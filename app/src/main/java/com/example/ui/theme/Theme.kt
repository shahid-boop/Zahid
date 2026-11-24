package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val AcademyLightColorScheme = lightColorScheme(
  primary = PrimaryNavy,
  onPrimary = PureWhite,
  primaryContainer = NavyTint,
  onPrimaryContainer = PrimaryNavy,
  secondary = AccentOrange,
  onSecondary = PureWhite,
  secondaryContainer = OrangeTint,
  onSecondaryContainer = AccentOrange,
  tertiary = SecondaryNavy,
  onTertiary = PureWhite,
  background = BackgroundLight,
  onBackground = TextPrimary,
  surface = PureWhite,
  onSurface = TextPrimary,
  surfaceVariant = ColorTokens.SurfaceVariantLight,
  onSurfaceVariant = TextSecondary,
  outline = BorderSubtle,
  outlineVariant = BorderSubtle,
  error = ErrorRed,
  onError = PureWhite,
)

private val AcademyDarkColorScheme = darkColorScheme(
  primary = ColorTokens.NavyLightForDark,
  onPrimary = DarkBackground,
  primaryContainer = SecondaryNavy,
  onPrimaryContainer = PureWhite,
  secondary = AccentOrange,
  onSecondary = PureWhite,
  secondaryContainer = ColorTokens.OrangeContainerDark,
  onSecondaryContainer = OrangeTint,
  tertiary = PrimaryNavy,
  onTertiary = PureWhite,
  background = DarkBackground,
  onBackground = DarkTextPrimary,
  surface = DarkSurface,
  onSurface = DarkTextPrimary,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkTextSecondary,
  outline = DarkBorder,
  outlineVariant = DarkBorder,
  error = ErrorRed,
  onError = PureWhite,
)

private object ColorTokens {
  val SurfaceVariantLight = androidx.compose.ui.graphics.Color(0xFFEDF1F7)
  val NavyLightForDark = androidx.compose.ui.graphics.Color(0xFF7CB2FF)
  val OrangeContainerDark = androidx.compose.ui.graphics.Color(0xFF5C2B00)
}

val AcademyShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
)

@Composable
fun ZahidAcademyTheme(
  isRtl: Boolean = false,
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) AcademyDarkColorScheme else AcademyLightColorScheme
  val direction = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

  CompositionLocalProvider(LocalLayoutDirection provides direction) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = academyTypography(isRtl),
      shapes = AcademyShapes,
      content = content,
    )
  }
}
