package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val SpaceGroteskFontFamily = FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))

val PlusJakartaSansFontFamily = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))

val ZonexTypography =
  Typography(
    displaySmall =
      TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.25).sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
      ),
    titleSmall =
      TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
      ),
  )
