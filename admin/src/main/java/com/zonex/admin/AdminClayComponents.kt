package com.zonex.admin

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val AdminClayBg = Color(0xFF131622)
val AdminClaySurface = Color(0xFF1E2336)
val AdminClaySurfaceElevated = Color(0xFF262D44)
val AdminClayBorder = Color(0xFF343C58)
val AdminClayHighlight = Color(0x34FFFFFF)
val AdminClayShadow = Color(0xC805070E)

@Composable
fun Modifier.adminClaySurface(
  baseColor: Color = AdminClaySurface,
  cornerRadius: Dp = 20.dp,
  elevation: Dp = 10.dp,
  accentBorderColor: Color? = AdminClayBorder.copy(alpha = 0.7f),
  borderWidth: Dp = 1.dp,
  highlightAlpha: Float = 0.24f,
): Modifier {
  val shape = RoundedCornerShape(cornerRadius)
  val topTint = lerp(baseColor, Color.White, 0.11f)
  val bottomTint = lerp(baseColor, Color.Black, 0.15f)
  val rimTop = Color.White.copy(alpha = highlightAlpha.coerceIn(0.08f, 0.42f))
  val rimBottom = AdminClayShadow.copy(alpha = 0.50f)

  return this
    .shadow(
      elevation = elevation,
      shape = shape,
      clip = false,
      ambientColor = AdminClayShadow,
      spotColor = AdminClayShadow,
    )
    .clip(shape)
    .background(
      brush =
        Brush.linearGradient(
          colors = listOf(topTint, baseColor, bottomTint),
          start = Offset(0f, 0f),
          end = Offset(600f, 600f),
        )
    )
    .then(
      if (accentBorderColor != null) Modifier.border(BorderStroke(borderWidth, accentBorderColor), shape)
      else Modifier
    )
    .drawWithContent {
      drawContent()
      val strokePx = 1.6.dp.toPx()
      val r = cornerRadius.toPx()
      drawRoundRect(
        brush =
          Brush.linearGradient(
            colors = listOf(rimTop, Color.Transparent, rimBottom),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
          ),
        cornerRadius = CornerRadius(r, r),
        style = Stroke(width = strokePx),
      )
    }
}

@Composable
fun AdminClayCard(
  modifier: Modifier = Modifier,
  baseColor: Color = AdminClaySurface,
  cornerRadius: Dp = 22.dp,
  elevation: Dp = 10.dp,
  borderColor: Color? = AdminClayBorder.copy(alpha = 0.7f),
  borderWidth: Dp = 1.dp,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (onClick != null && pressed) 0.97f else 1f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
      label = "admin_clay_card_press",
    )

  Column(
    modifier =
      modifier
        .scale(scale)
        .adminClaySurface(
          baseColor = baseColor,
          cornerRadius = cornerRadius,
          elevation = if (pressed) (elevation * 0.55f) else elevation,
          accentBorderColor = borderColor,
          borderWidth = borderWidth,
        )
        .then(
          if (onClick != null) {
            Modifier.clickable(
              interactionSource = interactionSource,
              indication = null,
              onClick = onClick,
            )
          } else Modifier
        ),
    content = content,
  )
}

@Composable
fun AdminClayButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  containerColor: Color = Color(0xFFF5B82E),
  contentColor: Color = Color.Black,
  borderColor: Color? = null,
  cornerRadius: Dp = 16.dp,
  elevation: Dp = 8.dp,
  contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
  content: @Composable RowScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (enabled && pressed) 0.95f else 1f,
      animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
      label = "admin_clay_btn_press",
    )
  val effectiveBg = if (enabled) containerColor else AdminClaySurfaceElevated.copy(alpha = 0.65f)
  val effectiveContent = if (enabled) contentColor else Color(0xFF9CA3AF).copy(alpha = 0.6f)

  Row(
    modifier =
      modifier
        .heightIn(min = 44.dp)
        .scale(scale)
        .adminClaySurface(
          baseColor = effectiveBg,
          cornerRadius = cornerRadius,
          elevation = if (!enabled) 2.dp else if (pressed) 4.dp else elevation,
          accentBorderColor = borderColor,
          highlightAlpha = 0.30f,
        )
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          enabled = enabled,
          onClick = onClick,
        )
        .padding(contentPadding),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    CompositionLocalProvider(LocalContentColor provides effectiveContent) {
      ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(color = effectiveContent)) {
        content()
      }
    }
  }
}

@Composable
fun AdminClayPill(
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  baseColor: Color = AdminClaySurfaceElevated,
  borderColor: Color? = AdminClayBorder,
  cornerRadius: Dp = 14.dp,
  elevation: Dp = 5.dp,
  content: @Composable BoxScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (onClick != null && pressed) 0.95f else 1f,
      animationSpec = spring(dampingRatio = 0.55f, stiffness = 550f),
      label = "admin_clay_pill_press",
    )

  Box(
    modifier =
      modifier
        .scale(scale)
        .adminClaySurface(
          baseColor = baseColor,
          cornerRadius = cornerRadius,
          elevation = elevation,
          accentBorderColor = borderColor,
        )
        .then(
          if (onClick != null) {
            Modifier.clickable(
              interactionSource = interactionSource,
              indication = null,
              onClick = onClick,
            )
          } else Modifier
        ),
    contentAlignment = Alignment.Center,
    content = content,
  )
}
