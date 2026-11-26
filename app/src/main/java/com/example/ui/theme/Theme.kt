package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.model.AppThemeStyle

val LocalZonexPalette = staticCompositionLocalOf { PureBlackPalette }

@Composable
fun ZonexSoundTheme(
  themeStyle: AppThemeStyle = AppThemeStyle.PURE_BLACK,
  content: @Composable () -> Unit,
) {
  val palette = paletteForTheme(themeStyle)
  val colorScheme =
    darkColorScheme(
      primary = palette.goldPrimary,
      onPrimary = palette.onGold,
      primaryContainer = palette.surfaceHighlight,
      onPrimaryContainer = palette.goldBright,
      secondary = palette.accentSecondary,
      onSecondary = palette.onGold,
      secondaryContainer = palette.surfaceElevated,
      onSecondaryContainer = palette.textPrimary,
      tertiary = palette.info,
      background = palette.background,
      onBackground = palette.textPrimary,
      surface = palette.surface,
      onSurface = palette.textPrimary,
      surfaceVariant = palette.surfaceElevated,
      onSurfaceVariant = palette.textSecondary,
      outline = palette.border,
      error = palette.danger,
    )

  CompositionLocalProvider(LocalZonexPalette provides palette) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = ZonexTypography,
      content = content,
    )
  }
}
