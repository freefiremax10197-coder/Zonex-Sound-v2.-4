package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.data.model.AppThemeStyle

@Immutable
data class ZonexPalette(
  val background: Color,
  val surface: Color,
  val surfaceElevated: Color,
  val surfaceHighlight: Color,
  val clayHighlight: Color,
  val clayShadow: Color,
  val border: Color,
  val borderAccent: Color,
  val goldPrimary: Color,
  val goldBright: Color,
  val accentSecondary: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val danger: Color,
  val success: Color,
  val info: Color,
  val onGold: Color,
)

val PureBlackPalette =
  ZonexPalette(
    background = Color(0xFF131622),
    surface = Color(0xFF1E2336),
    surfaceElevated = Color(0xFF262D44),
    surfaceHighlight = Color(0xFF303956),
    clayHighlight = Color(0x36FFFFFF),
    clayShadow = Color(0xC805070E),
    border = Color(0xFF343C58),
    borderAccent = Color(0x99F6B930),
    goldPrimary = Color(0xFFF6B930),
    goldBright = Color(0xFFFFDE7A),
    accentSecondary = Color(0xFFF59E0B),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFFA8B3CF),
    danger = Color(0xFFFF5A6E),
    success = Color(0xFF2BD978),
    info = Color(0xFF42C6FF),
    onGold = Color(0xFF100D04),
  )

val GoldenRoboticsPalette =
  ZonexPalette(
    background = Color(0xFF17120C),
    surface = Color(0xFF251D14),
    surfaceElevated = Color(0xFF31261A),
    surfaceHighlight = Color(0xFF3E3021),
    clayHighlight = Color(0x34FFE8BA),
    clayShadow = Color(0xD0080502),
    border = Color(0xFF483825),
    borderAccent = Color(0xAAFFB300),
    goldPrimary = Color(0xFFFFB81C),
    goldBright = Color(0xFFFFE28A),
    accentSecondary = Color(0xFFFF7A45),
    textPrimary = Color(0xFFFFF8E8),
    textSecondary = Color(0xFFCDBB97),
    danger = Color(0xFFFF6B7A),
    success = Color(0xFF4ADE80),
    info = Color(0xFFFBBF24),
    onGold = Color(0xFF160E02),
  )

val CyberTitaniumPalette =
  ZonexPalette(
    background = Color(0xFF101826),
    surface = Color(0xFF1A263A),
    surfaceElevated = Color(0xFF23324C),
    surfaceHighlight = Color(0xFF2D4060),
    clayHighlight = Color(0x38D8F0FF),
    clayShadow = Color(0xC8050A14),
    border = Color(0xFF334768),
    borderAccent = Color(0x9938BDF8),
    goldPrimary = Color(0xFFF6B930),
    goldBright = Color(0xFFFFDF7E),
    accentSecondary = Color(0xFF38BDF8),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFFA2B5CD),
    danger = Color(0xFFFB7185),
    success = Color(0xFF34D399),
    info = Color(0xFF38BDF8),
    onGold = Color(0xFF0B1120),
  )

fun paletteForTheme(style: AppThemeStyle): ZonexPalette =
  when (style) {
    AppThemeStyle.PURE_BLACK -> PureBlackPalette
    AppThemeStyle.GOLDEN_ROBOTICS -> GoldenRoboticsPalette
    AppThemeStyle.CYBER_TITANIUM -> CyberTitaniumPalette
  }
