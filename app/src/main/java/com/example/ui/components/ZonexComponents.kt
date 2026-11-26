package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.data.model.AdminNotice
import com.example.data.model.AppUpdateConfig
import com.example.data.model.MaintenanceConfig
import com.example.data.model.PlaybackState
import com.example.data.model.SoundItem
import com.example.ui.theme.LocalZonexPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

fun parseHexColorSafe(hex: String?, fallback: Color): Color {
  if (hex.isNullOrBlank()) return fallback
  return runCatching {
    val clean = if (hex.startsWith("#")) hex else "#$hex"
    Color(android.graphics.Color.parseColor(clean))
  }.getOrDefault(fallback)
}

@Composable
fun ZonexBrandTitle(
  modifier: Modifier = Modifier,
  large: Boolean = false,
) {
  val palette = LocalZonexPalette.current
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Text(
      text = "Zonex Sound",
      style =
        if (large) MaterialTheme.typography.headlineMedium
        else MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = palette.textPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Box(
      modifier =
        Modifier.claySurface(
          baseColor = palette.goldPrimary,
          cornerRadius = 9.dp,
          elevation = 4.dp,
          highlightAlpha = 0.35f,
        )
          .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
      Text(
        text = "PRO",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        color = palette.onGold,
        letterSpacing = 0.8.sp,
      )
    }
  }
}

@Composable
fun SoundGridCard(
  sound: SoundItem,
  isPlaying: Boolean,
  isBuffering: Boolean,
  isFavorite: Boolean,
  isLiked: Boolean,
  isDownloaded: Boolean,
  animationsEnabled: Boolean,
  onPlayClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  onToggleLike: () -> Unit,
  onDownloadClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalZonexPalette.current

  ClayCard(
    modifier =
      modifier
        .fillMaxWidth()
        .height(174.dp)
        .testTag("sound_card_${sound.id}"),
    baseColor = if (isPlaying) palette.surfaceHighlight else palette.surface,
    cornerRadius = 22.dp,
    elevation = if (isPlaying) 14.dp else 9.dp,
    borderColor = if (isPlaying) palette.goldPrimary else palette.border.copy(alpha = 0.65f),
    borderWidth = if (isPlaying) 1.8.dp else 1.dp,
    onClick = onPlayClick,
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Full-bleed cover artwork or gradient emoji backdrop
      if (sound.thumbnailUrl.isNotBlank()) {
        AsyncImage(
          model = sound.thumbnailUrl,
          contentDescription = sound.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.radialGradient(
                  colors =
                    if (isPlaying)
                      listOf(
                        palette.goldPrimary.copy(alpha = 0.34f),
                        palette.surfaceElevated,
                        palette.surface,
                      )
                    else
                      listOf(
                        palette.surfaceHighlight,
                        palette.surfaceElevated,
                        palette.surface,
                      )
                )
              ),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = sound.emoji,
            fontSize = 46.sp,
            modifier = Modifier.padding(bottom = 18.dp),
          )
        }
      }

      // Contrast gradient scrim so top badges & bottom text stay sharp over any photo
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.verticalGradient(
                0.0f to Color.Black.copy(alpha = 0.56f),
                0.40f to Color.Black.copy(alpha = 0.14f),
                0.68f to Color.Black.copy(alpha = 0.52f),
                1.0f to Color.Black.copy(alpha = 0.90f),
              )
            )
      )

      if (isPlaying) {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(palette.goldPrimary.copy(alpha = 0.12f))
        )
      }

      if (isBuffering) {
        Box(
          modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(
            modifier = Modifier.size(26.dp),
            strokeWidth = 2.5.dp,
            color = palette.goldPrimary,
          )
        }
      }

      Column(
        modifier = Modifier.fillMaxSize().padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Top row: ★ Favorite + ⬇ Download (left), ❤️ Like counter (right)
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Box(
              modifier =
                Modifier.size(30.dp)
                  .clip(CircleShape)
                  .background(Color.Black.copy(alpha = 0.58f))
                  .border(
                    1.dp,
                    if (isFavorite) palette.goldPrimary else Color.White.copy(alpha = 0.25f),
                    CircleShape,
                  )
                  .clickable(onClick = onToggleFavorite)
                  .testTag("fav_btn_${sound.id}"),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription =
                  if (isFavorite) "Remove from Floating Window" else "Pin to Floating Window",
                tint = if (isFavorite) palette.goldPrimary else Color.White,
                modifier = Modifier.size(16.dp),
              )
            }

            Box(
              modifier =
                Modifier.size(30.dp)
                  .clip(CircleShape)
                  .background(Color.Black.copy(alpha = 0.58f))
                  .border(
                    1.dp,
                    if (isDownloaded) palette.success else Color.White.copy(alpha = 0.25f),
                    CircleShape,
                  )
                  .clickable(onClick = onDownloadClick)
                  .testTag("download_btn_${sound.id}"),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector =
                  if (isDownloaded) Icons.Filled.BookmarkAdded else Icons.Filled.Download,
                contentDescription =
                  if (isDownloaded) "Downloaded for Offline" else "Download for Offline",
                tint = if (isDownloaded) palette.success else Color.White,
                modifier = Modifier.size(15.dp),
              )
            }
          }

          var reactBurst by remember { mutableIntStateOf(0) }
          val heartScale = remember { Animatable(1f) }
          LaunchedEffect(reactBurst) {
            if (reactBurst > 0) {
              heartScale.snapTo(1.42f)
              heartScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f))
            }
          }
          val activeLoved = isLiked || sound.likes > 0L

          Row(
            modifier =
              Modifier.clip(RoundedCornerShape(10.dp))
                .background(
                  if (activeLoved) Color(0xFF9F1239).copy(alpha = 0.78f)
                  else Color.Black.copy(alpha = 0.58f)
                )
                .border(
                  1.dp,
                  if (activeLoved) palette.danger.copy(alpha = 0.8f)
                  else Color.White.copy(alpha = 0.25f),
                  RoundedCornerShape(10.dp),
                )
                .clickable {
                  reactBurst++
                  onToggleLike()
                }
                .padding(horizontal = 7.dp, vertical = 4.dp)
                .testTag("like_btn_${sound.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = if (activeLoved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
              contentDescription = "Send Love React",
              tint = if (activeLoved) Color(0xFFFF6B8B) else Color.White,
              modifier = Modifier.size(13.dp).scale(heartScale.value),
            )
            Text(
              text = sound.likes.toString(),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White,
            )
          }
        }

        // Bottom overlay: Title + Play icon, Category badge + Equalizer
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = sound.title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = if (isPlaying) palette.goldBright else Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Box(
              modifier =
                Modifier.size(26.dp)
                  .clip(CircleShape)
                  .background(
                    if (isPlaying) palette.goldPrimary else Color.Black.copy(alpha = 0.65f)
                  )
                  .border(1.dp, palette.goldPrimary.copy(alpha = 0.8f), CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Stop" else "Play",
                tint = if (isPlaying) palette.onGold else palette.goldBright,
                modifier = Modifier.size(15.dp),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                  .background(Color.Black.copy(alpha = 0.56f))
                  .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = sound.category,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Box(modifier = Modifier.weight(1f)) {
              if (isPlaying && animationsEnabled) {
                PlayingEqualizerStrip(modifier = Modifier.height(10.dp))
              } else {
                IdleEqualizerStrip(
                  isHighlighted = isPlaying,
                  modifier = Modifier.height(10.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun IdleEqualizerStrip(
  isHighlighted: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val palette = LocalZonexPalette.current
  val barColor =
    if (isHighlighted) palette.goldPrimary
    else palette.border.copy(alpha = 0.65f)
  Row(
    modifier = modifier.fillMaxWidth().height(12.dp),
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    val heights = if (isHighlighted) listOf(10, 7, 12, 8, 11, 6) else listOf(4, 6, 4, 7, 5, 4)
    heights.forEach { h ->
      Box(
        modifier =
          Modifier.weight(1f)
            .height(h.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(barColor)
      )
    }
  }
}

@Composable
fun PlayingEqualizerStrip(modifier: Modifier = Modifier) {
  val palette = LocalZonexPalette.current
  val transition = rememberInfiniteTransition(label = "eq_strip")
  val b1 by
    transition.animateFloat(
      initialValue = 0.25f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(280), RepeatMode.Reverse),
      label = "b1",
    )
  val b2 by
    transition.animateFloat(
      initialValue = 0.85f,
      targetValue = 0.2f,
      animationSpec = infiniteRepeatable(tween(340), RepeatMode.Reverse),
      label = "b2",
    )
  val b3 by
    transition.animateFloat(
      initialValue = 0.35f,
      targetValue = 0.95f,
      animationSpec = infiniteRepeatable(tween(240), RepeatMode.Reverse),
      label = "b3",
    )

  Row(
    modifier = modifier.fillMaxWidth().height(12.dp),
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    listOf(b1, b2, b3, b2, b1, b3).forEach { frac ->
      Box(
        modifier =
          Modifier.weight(1f)
            .height((4 + 8 * frac).dp)
            .clip(RoundedCornerShape(2.dp))
            .background(palette.goldPrimary)
      )
    }
  }
}

@Composable
fun CurrentlyPlayingBar(
  playback: PlaybackState,
  onTogglePlayPause: () -> Unit,
  onStop: () -> Unit,
  modifier: Modifier = Modifier,
  isBangla: Boolean = true,
) {
  val palette = LocalZonexPalette.current
  val sound = playback.currentSound ?: return

  AnimatedVisibility(visible = true, modifier = modifier) {
    ClayCard(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
      baseColor = palette.surfaceElevated,
      cornerRadius = 22.dp,
      elevation = 12.dp,
      borderColor = palette.goldPrimary.copy(alpha = 0.7f),
      borderWidth = 1.2.dp,
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.size(38.dp)
                .clip(CircleShape)
                .background(palette.goldPrimary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.GraphicEq,
              contentDescription = "Playing",
              tint = palette.goldPrimary,
              modifier = Modifier.size(20.dp),
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (isBangla) "এখন বাজছে" else "CURRENTLY PLAYING",
              style = MaterialTheme.typography.labelSmall,
              color = palette.goldPrimary,
              letterSpacing = 0.9.sp,
            )
            Text(
              text = "${sound.emoji} ${sound.title}",
              style = MaterialTheme.typography.titleSmall,
              color = palette.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          IconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier.size(44.dp).testTag("currently_playing_pause_btn"),
          ) {
            Icon(
              imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
              contentDescription = if (playback.isPlaying) "Pause" else "Resume",
              tint = palette.goldBright,
            )
          }
          IconButton(
            onClick = onStop,
            modifier = Modifier.size(44.dp).testTag("currently_playing_stop_btn"),
          ) {
            Icon(
              imageVector = Icons.Filled.Stop,
              contentDescription = "Stop playback",
              tint = palette.danger,
            )
          }
        }
        val progress =
          if (playback.durationMs > 0) {
            (playback.positionMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f)
          } else {
            0f
          }
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier.fillMaxWidth().height(3.dp),
          color = palette.goldPrimary,
          trackColor = palette.border,
        )
      }
    }
  }
}

@Composable
fun FloatingWindowOverlayWidget(
  favorites: List<SoundItem>,
  appLogoUrl: String,
  brandTitle: String,
  isMinimized: Boolean,
  playingSoundId: String?,
  isBangla: Boolean = true,
  onPlaySound: (SoundItem) -> Unit,
  onStopAll: () -> Unit,
  onToggleMinimize: () -> Unit,
  onClose: () -> Unit,
) {
  val palette = LocalZonexPalette.current
  var offsetX by remember { mutableFloatStateOf(20f) }
  var offsetY by remember { mutableFloatStateOf(140f) }
  val pulseTransition = rememberInfiniteTransition(label = "floating_playing_pulse")
  val pulseScale by
    pulseTransition.animateFloat(
      initialValue = 1f,
      targetValue = 1.05f,
      animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
      label = "pulse_scale",
    )
  val glowAlpha by
    pulseTransition.animateFloat(
      initialValue = 0.55f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
      label = "glow_alpha",
    )
  val particlePhase by
    pulseTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
      label = "particle_phase",
    )

  if (isMinimized) {
    // ONLY circular floating logo bubble when first launched or minimized
    Surface(
      modifier =
        Modifier.offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
          .size(62.dp)
          .scale(if (playingSoundId != null) pulseScale else 1f)
          .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
              change.consume()
              offsetX += dragAmount.x
              offsetY += dragAmount.y
            }
          }
          .clip(CircleShape)
          .clickable(onClick = onToggleMinimize)
          .testTag("floating_circle_bubble"),
      shape = CircleShape,
      color = palette.background.copy(alpha = 0.96f),
      border =
        BorderStroke(
          width = if (playingSoundId != null) 2.5.dp else 1.8.dp,
          color =
            if (playingSoundId != null) palette.goldBright.copy(alpha = glowAlpha)
            else palette.goldPrimary,
        ),
      shadowElevation = 14.dp,
    ) {
      Box(modifier = Modifier.fillMaxSize().padding(3.dp).clip(CircleShape)) {
        if (appLogoUrl.isNotBlank()) {
          AsyncImage(
            model = appLogoUrl,
            contentDescription = "Open Floating Window",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          androidx.compose.foundation.Image(
            painter = painterResource(id = com.example.R.drawable.ic_floating_logo),
            contentDescription = "Open Floating Window",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
    return
  }

  ClayCard(
    modifier =
      Modifier.offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
        .width(240.dp)
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            offsetX += dragAmount.x
            offsetY += dragAmount.y
          }
        }
        .testTag("floating_window_widget"),
    baseColor = palette.surface.copy(alpha = 0.96f),
    cornerRadius = 18.dp,
    elevation = 16.dp,
    borderColor = palette.goldPrimary.copy(alpha = 0.85f),
    borderWidth = 1.4.dp,
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      // Animated glowing particle aura background
      Canvas(modifier = Modifier.matchParentSize()) {
        val w = size.width
        val h = size.height
        val angle = particlePhase * 2f * PI.toFloat()
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(palette.goldPrimary.copy(alpha = 0.14f), Color.Transparent),
              center = Offset(w * (0.28f + 0.08f * cos(angle)), h * 0.25f),
              radius = w * 0.55f,
            ),
          center = Offset(w * (0.28f + 0.08f * cos(angle)), h * 0.25f),
          radius = w * 0.55f,
        )
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(palette.info.copy(alpha = 0.12f), Color.Transparent),
              center = Offset(w * (0.72f + 0.07f * sin(angle)), h * 0.78f),
              radius = w * 0.50f,
            ),
          center = Offset(w * (0.72f + 0.07f * sin(angle)), h * 0.78f),
          radius = w * 0.50f,
        )
        val dotSeeds = listOf(0.14f, 0.31f, 0.52f, 0.73f, 0.88f, 0.22f, 0.63f, 0.81f)
        dotSeeds.forEachIndexed { idx, xSeed ->
          val drift = (particlePhase + idx * 0.14f) % 1f
          val px = (xSeed + 0.05f * sin(angle + idx)) * w
          val py = (1f - drift) * h
          val alpha = sin(drift * PI.toFloat()).coerceIn(0f, 1f) * 0.5f
          val color =
            if (idx % 3 == 0) palette.goldBright
            else if (idx % 3 == 1) palette.info
            else palette.goldPrimary
          drawCircle(
            color = color.copy(alpha = alpha),
            radius = if (idx % 2 == 0) 3.4f else 2.4f,
            center = Offset(px, py),
          )
        }
      }

      Column(modifier = Modifier.padding(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.size(28.dp)
              .clip(CircleShape)
              .background(palette.surfaceElevated)
              .border(1.dp, palette.goldPrimary, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          if (appLogoUrl.isNotBlank()) {
            AsyncImage(
              model = appLogoUrl,
              contentDescription = "App Logo",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            androidx.compose.foundation.Image(
              painter = painterResource(id = com.example.R.drawable.ic_floating_logo),
              contentDescription = "App Logo",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = brandTitle.ifBlank { "ZONEX" },
            style = MaterialTheme.typography.labelMedium,
            color = palette.goldPrimary,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
          )
          Text(
            text = "GOLD",
            style = MaterialTheme.typography.labelMedium,
            color = palette.goldBright,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
          )
        }
        Row(
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE11D48))
                .clickable(onClick = onStopAll)
                .padding(horizontal = 9.dp, vertical = 5.dp)
                .testTag("floating_stop_btn")
          ) {
            Text(
              text = if (isBangla) "■ বন্ধ" else "■ STOP",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              fontWeight = FontWeight.Bold,
            )
          }
          Box(
            modifier =
              Modifier.size(24.dp)
                .clip(CircleShape)
                .background(palette.surfaceElevated)
                .border(1.dp, palette.goldPrimary.copy(alpha = 0.5f), CircleShape)
                .clickable(onClick = onToggleMinimize)
                .testTag("floating_min_btn"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "—",
              style = MaterialTheme.typography.labelMedium,
              color = palette.goldPrimary,
            )
          }
          Box(
            modifier =
              Modifier.size(24.dp)
                .clip(CircleShape)
                .background(palette.surfaceElevated)
                .border(1.dp, palette.goldPrimary.copy(alpha = 0.5f), CircleShape)
                .clickable(onClick = onClose)
                .testTag("floating_close_btn"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "×",
              style = MaterialTheme.typography.titleSmall,
              color = palette.goldPrimary,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      if (favorites.isEmpty()) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(palette.surface)
              .padding(10.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text =
              if (isBangla) "হোমে ★ ট্যাপ করে প্রিয় সাউন্ড পিন করো"
              else "Star sounds to pin favorites here",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )
        }
      } else {
        val gridHeight = if (favorites.size <= 2) 102.dp else if (favorites.size <= 4) 204.dp else 260.dp
        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          modifier = Modifier.fillMaxWidth().height(gridHeight),
          contentPadding = PaddingValues(2.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          items(favorites.take(16), key = { it.id }) { sound ->
            val active = sound.id == playingSoundId
            Surface(
              modifier =
                Modifier.fillMaxWidth()
                  .height(94.dp)
                  .scale(if (active) pulseScale else 1f)
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { onPlaySound(sound) },
              shape = RoundedCornerShape(10.dp),
              color = if (active) palette.goldPrimary.copy(alpha = 0.2f) else palette.surfaceElevated,
              border =
                BorderStroke(
                  width = if (active) 2.2.dp else 1.dp,
                  color =
                    if (active) palette.goldBright.copy(alpha = glowAlpha)
                    else palette.goldPrimary.copy(alpha = 0.55f),
                ),
            ) {
              Box(modifier = Modifier.fillMaxSize()) {
                if (sound.thumbnailUrl.isNotBlank()) {
                  AsyncImage(
                    model = sound.thumbnailUrl,
                    contentDescription = sound.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                  )
                } else {
                  Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(text = sound.emoji, fontSize = 30.sp)
                  }
                }
                if (active) {
                  Box(
                    modifier =
                      Modifier.fillMaxSize().background(palette.goldPrimary.copy(alpha = 0.16f))
                  )
                }
                Box(
                  modifier =
                    Modifier.padding(5.dp)
                      .size(18.dp)
                      .clip(CircleShape)
                      .background(Color.Black.copy(alpha = 0.68f))
                      .align(Alignment.TopStart),
                  contentAlignment = Alignment.Center,
                ) {
                  Text("★", fontSize = 9.sp, color = palette.goldPrimary)
                }
                if (active) {
                  Box(
                    modifier =
                      Modifier.padding(5.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.goldPrimary)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                        .align(Alignment.TopEnd),
                  ) {
                    Text(
                      text = "LIVE",
                      fontSize = 8.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = Color.Black,
                    )
                  }
                }
                Column(
                  modifier =
                    Modifier.fillMaxWidth()
                      .align(Alignment.BottomCenter)
                      .background(Color.Black.copy(alpha = if (active) 0.88f else 0.78f))
                      .padding(horizontal = 6.dp, vertical = 5.dp),
                ) {
                  if (active) {
                    PlayingEqualizerStrip(modifier = Modifier.height(10.dp).padding(bottom = 3.dp))
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = if (active) "■ " else "▶ ",
                      fontSize = 9.sp,
                      color = if (active) palette.goldBright else palette.goldPrimary,
                    )
                    Text(
                      text = sound.title,
                      style = MaterialTheme.typography.labelSmall,
                      color = if (active) palette.goldBright else Color.White,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
    }
  }
}

@Composable
fun OverlayPermissionDialog(
  isBangla: Boolean = true,
  onDismiss: () -> Unit,
  onContinueInAppOnly: () -> Unit,
) {
  val palette = LocalZonexPalette.current
  val context = LocalContext.current
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = palette.surfaceElevated,
    titleContentColor = palette.textPrimary,
    textContentColor = palette.textSecondary,
    title = {
      Text(
        text = if (isBangla) "ফ্লোটিং ওভারলে পারমিশন" else "Floating Overlay Permission",
        style = MaterialTheme.typography.titleLarge,
        color = palette.goldPrimary,
      )
    },
    text = {
      Text(
        text =
          if (isBangla) {
            "গেমের উপরে (Free Fire, PUBG, BGMI) Zonex Gold ফ্লোটিং উইন্ডো ভাসাতে \"Display over other apps\" পারমিশন দিন। চাইলে ইন-অ্যাপ উইজেটও ব্যবহার করতে পারেন।"
          } else {
            "To float Zonex Gold over games (Free Fire, PUBG, BGMI), allow \"Display over other apps\". You can also use the in-app draggable widget immediately."
          },
        style = MaterialTheme.typography.bodyMedium,
      )
    },
    confirmButton = {
      Button(
        onClick = {
          onDismiss()
          val intent =
            Intent(
              Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
              Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          runCatching { context.startActivity(intent) }
        },
        colors = ButtonDefaults.buttonColors(containerColor = palette.goldPrimary, contentColor = palette.onGold),
      ) {
        Text(if (isBangla) "পারমিশন দিন" else "GRANT OVERLAY")
      }
    },
    dismissButton = {
      TextButton(onClick = onContinueInAppOnly) {
        Text(if (isBangla) "ইন-অ্যাপ উইজেট" else "USE IN-APP WIDGET", color = palette.textPrimary)
      }
    },
  )
}

@Composable
fun NoticePopupModal(
  notice: AdminNotice,
  onDismiss: () -> Unit,
) {
  val palette = LocalZonexPalette.current
  Dialog(onDismissRequest = onDismiss) {
    ClayCard(
      modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
      baseColor = palette.surfaceElevated,
      cornerRadius = 26.dp,
      elevation = 16.dp,
      borderColor = palette.goldPrimary,
      borderWidth = 1.4.dp,
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "জরুরি নোটিশ / Notice",
            style = MaterialTheme.typography.titleMedium,
            color = palette.goldPrimary,
            fontWeight = FontWeight.Bold,
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Close notice", tint = palette.textSecondary)
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (notice.popupTitle.isNotBlank()) {
          Text(
            text = notice.popupTitle,
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
          )
          Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
          text = notice.popupMessage.ifBlank { notice.bannerText },
          style = MaterialTheme.typography.bodyLarge,
          color = palette.textSecondary,
        )
        Spacer(modifier = Modifier.height(18.dp))
        ClayButton(
          onClick = onDismiss,
          containerColor = palette.goldPrimary,
          contentColor = palette.onGold,
          cornerRadius = 16.dp,
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
          Text("OK / বুঝেছি", fontWeight = FontWeight.ExtraBold)
        }
      }
    }
  }
}

@Composable
fun AppUpdatePopupModal(
  config: AppUpdateConfig,
  isBangla: Boolean,
  onUpdateClick: () -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalZonexPalette.current
  Dialog(onDismissRequest = { if (!config.forceUpdate) onDismiss() }) {
    ClayCard(
      modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).testTag("app_update_modal"),
      baseColor = palette.surfaceElevated,
      cornerRadius = 26.dp,
      elevation = 16.dp,
      borderColor = palette.goldPrimary,
      borderWidth = 1.6.dp,
    ) {
      Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = config.title.ifBlank {
              if (isBangla) "🚀 নতুন আপডেট এসেছে!" else "🚀 New App Update Available!"
            },
            style = MaterialTheme.typography.titleLarge,
            color = palette.goldBright,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f),
          )
          if (!config.forceUpdate) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
              Icon(Icons.Filled.Close, contentDescription = "Close update dialog", tint = palette.textSecondary)
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = palette.background.copy(alpha = 0.7f),
          border = BorderStroke(1.dp, palette.goldPrimary.copy(alpha = 0.45f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (isBangla) "আপনার ভার্সন" else "Your Version",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
              )
              Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelLarge,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = if (isBangla) "বর্তমান ভার্সন" else "Current Version",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
              )
              Text(
                text = "v${config.latestVersionName.ifBlank { BuildConfig.VERSION_NAME }}",
                style = MaterialTheme.typography.labelLarge,
                color = palette.success,
                fontWeight = FontWeight.ExtraBold,
              )
            }
          }
        }

        if (config.message.isNotBlank()) {
          Text(
            text = config.message,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textPrimary,
          )
        } else {
          Text(
            text =
              if (isBangla) "নতুন ফিচার ও বাগ ফিক্স পেতে এখনই নতুন ভার্সন ডাউনলোড করুন।"
              else "Download the latest version now for new features, speed & security fixes.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        ClayButton(
          onClick = onUpdateClick,
          containerColor = palette.goldPrimary,
          contentColor = palette.onGold,
          cornerRadius = 16.dp,
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("app_update_download_btn"),
        ) {
          Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isBangla) "এখনই আপডেট করো" else "Update Now",
            fontWeight = FontWeight.ExtraBold,
          )
        }

        if (!config.forceUpdate) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().testTag("app_update_later_btn"),
          ) {
            Text(
              text = if (isBangla) "পরে আপডেট করব" else "Later",
              color = palette.textSecondary,
            )
          }
        }
      }
    }
  }
}

@Composable
fun MaintenanceBlockingModal(
  config: MaintenanceConfig,
  isBangla: Boolean = true,
) {
  val palette = LocalZonexPalette.current
  val pulseTransition = rememberInfiniteTransition(label = "maintenance_pulse")
  val pulseScale by
    pulseTransition.animateFloat(
      initialValue = 0.94f,
      targetValue = 1.06f,
      animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
      label = "maintenance_scale",
    )

  Dialog(
    onDismissRequest = {},
    properties =
      DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false,
      ),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(palette.background.copy(alpha = 0.94f))
          .padding(24.dp)
          .testTag("maintenance_blocking_modal"),
      contentAlignment = Alignment.Center,
    ) {
      ClayCard(
        modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
        baseColor = palette.surfaceElevated,
        cornerRadius = 28.dp,
        elevation = 18.dp,
        borderColor = palette.goldPrimary,
        borderWidth = 1.6.dp,
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Box(
            modifier =
              Modifier.size(72.dp)
                .scale(pulseScale)
                .claySurface(
                  baseColor = palette.goldPrimary.copy(alpha = 0.18f),
                  cornerRadius = 36.dp,
                  elevation = 8.dp,
                  accentBorderColor = palette.goldPrimary.copy(alpha = 0.7f),
                ),
            contentAlignment = Alignment.Center,
          ) {
            Text(text = "🛠️", fontSize = 34.sp)
          }

          Text(
            text =
              config.title.ifBlank {
                if (isBangla) "কাজ চলতেছে" else "Under Maintenance"
              },
            style = MaterialTheme.typography.headlineSmall,
            color = palette.goldBright,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
          )

          Text(
            text =
              config.message.ifBlank {
                if (isBangla) {
                  "অ্যাপের উন্নয়ন ও সার্ভার মেইনটেন্যান্সের কাজ চলছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"
                } else {
                  "We are upgrading the servers and soundboard features. Please check back shortly."
                }
              },
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
          )

          LinearProgressIndicator(
            modifier =
              Modifier.fillMaxWidth()
                .padding(top = 6.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = palette.goldPrimary,
            trackColor = palette.surface,
          )

          Text(
            text = if (isBangla) "অপেক্ষা করার জন্য ধন্যবাদ" else "Thank you for your patience",
            style = MaterialTheme.typography.labelMedium,
            color = palette.goldPrimary,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

