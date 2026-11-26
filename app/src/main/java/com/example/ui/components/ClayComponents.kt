package com.example.ui.components

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalZonexPalette

/**
 * Applies a true 3D matte Claymorphism look:
 * 1) Soft outer shadow beneath the puffy element
 * 2) Subtle diagonal top-left-to-bottom-right gradient fill
 * 3) Translucent top-left specular inner highlight rim + subtle bottom-right bevel
 */
@Composable
fun Modifier.claySurface(
  baseColor: Color = LocalZonexPalette.current.surface,
  cornerRadius: Dp = 20.dp,
  elevation: Dp = 10.dp,
  accentBorderColor: Color? = null,
  borderWidth: Dp = 1.dp,
  highlightAlpha: Float = 0.22f,
): Modifier {
  val palette = LocalZonexPalette.current
  val shape = RoundedCornerShape(cornerRadius)
  val topTint = lerp(baseColor, Color.White, 0.10f)
  val bottomTint = lerp(baseColor, Color.Black, 0.14f)
  val rimTopColor = palette.clayHighlight.copy(alpha = (palette.clayHighlight.alpha * (highlightAlpha / 0.22f)).coerceIn(0.08f, 0.45f))
  val rimBottomColor = palette.clayShadow.copy(alpha = 0.48f)

  return this
    .shadow(
      elevation = elevation,
      shape = shape,
      clip = false,
      ambientColor = palette.clayShadow,
      spotColor = palette.clayShadow,
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
      // Top-left inner puffy clay specular highlight
      drawRoundRect(
        brush =
          Brush.linearGradient(
            colors = listOf(rimTopColor, Color.Transparent, rimBottomColor),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
          ),
        cornerRadius = CornerRadius(r, r),
        style = Stroke(width = strokePx),
      )
    }
}

@Composable
fun ClayCard(
  modifier: Modifier = Modifier,
  baseColor: Color = LocalZonexPalette.current.surface,
  cornerRadius: Dp = 22.dp,
  elevation: Dp = 10.dp,
  borderColor: Color? = LocalZonexPalette.current.border.copy(alpha = 0.7f),
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
      label = "clay_card_press",
    )

  Column(
    modifier =
      modifier
        .scale(scale)
        .claySurface(
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
fun ClayButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  containerColor: Color = LocalZonexPalette.current.goldPrimary,
  contentColor: Color = LocalZonexPalette.current.onGold,
  borderColor: Color? = null,
  cornerRadius: Dp = 18.dp,
  elevation: Dp = 9.dp,
  contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
  content: @Composable RowScope.() -> Unit,
) {
  val palette = LocalZonexPalette.current
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (enabled && pressed) 0.95f else 1f,
      animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
      label = "clay_btn_press",
    )
  val effectiveBg = if (enabled) containerColor else palette.surfaceElevated.copy(alpha = 0.65f)
  val effectiveContent = if (enabled) contentColor else palette.textSecondary.copy(alpha = 0.6f)

  Row(
    modifier =
      modifier
        .heightIn(min = 44.dp)
        .scale(scale)
        .claySurface(
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
fun ClayPill(
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  baseColor: Color = LocalZonexPalette.current.surfaceElevated,
  borderColor: Color? = LocalZonexPalette.current.border,
  cornerRadius: Dp = 16.dp,
  elevation: Dp = 6.dp,
  content: @Composable BoxScopeWrapper.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (onClick != null && pressed) 0.95f else 1f,
      animationSpec = spring(dampingRatio = 0.55f, stiffness = 550f),
      label = "clay_pill_press",
    )

  Box(
    modifier =
      modifier
        .scale(scale)
        .claySurface(
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
  ) {
    BoxScopeWrapper(this).content()
  }
}

class BoxScopeWrapper(private val scope: androidx.compose.foundation.layout.BoxScope) {
  fun Modifier.align(alignment: Alignment) = with(scope) { this@align.align(alignment) }
}
