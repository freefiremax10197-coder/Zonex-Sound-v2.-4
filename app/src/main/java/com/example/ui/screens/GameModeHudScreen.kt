package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlaybackState
import com.example.data.model.SoundItem
import com.example.ui.components.ClayButton
import com.example.ui.components.ClayCard
import com.example.ui.components.ClayPill
import com.example.ui.components.PlayingEqualizerStrip
import com.example.ui.theme.LocalZonexPalette

@Composable
fun GameModeHudScreen(
  favoriteSounds: List<SoundItem>,
  playback: PlaybackState,
  isMicOn: Boolean,
  isBangla: Boolean = true,
  onToggleMic: () -> Unit,
  onPlaySound: (SoundItem) -> Unit,
  onStopAll: () -> Unit,
  onExit: () -> Unit,
) {
  val palette = LocalZonexPalette.current

  BackHandler(onBack = onExit)

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(palette.background)
        .safeDrawingPadding()
        .padding(16.dp)
        .testTag("game_mode_hud"),
  ) {
    // HUD Top Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          Icons.Filled.SportsEsports,
          contentDescription = null,
          tint = palette.goldPrimary,
          modifier = Modifier.size(26.dp),
        )
        Text(
          text = if (isBangla) "গেম মোড" else "GAME MODE",
          style = MaterialTheme.typography.headlineSmall,
          color = palette.goldBright,
          fontWeight = FontWeight.ExtraBold,
        )
      }

      ClayPill(
        onClick = onExit,
        modifier = Modifier.testTag("game_mode_exit_btn"),
        cornerRadius = 14.dp,
        baseColor = palette.surfaceElevated,
        borderColor = palette.border,
      ) {
        Text(
          text = if (isBangla) "× বের হও" else "× EXIT",
          style = MaterialTheme.typography.labelLarge,
          color = palette.textPrimary,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Status Badges: LOW LATENCY | FF MIC: OFF/ON
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      ClayPill(
        cornerRadius = 14.dp,
        baseColor = palette.surface,
        borderColor = palette.success.copy(alpha = 0.6f),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            Icons.Filled.Bolt,
            contentDescription = null,
            tint = palette.success,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = if (isBangla) "লো লেটেন্সি" else "LOW LATENCY",
            style = MaterialTheme.typography.labelLarge,
            color = palette.success,
          )
        }
      }

      ClayPill(
        onClick = onToggleMic,
        modifier = Modifier.testTag("game_mode_ff_mic_btn"),
        cornerRadius = 14.dp,
        baseColor = if (isMicOn) palette.surfaceHighlight else palette.surfaceElevated,
        borderColor = if (isMicOn) palette.goldPrimary else palette.border,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = if (isMicOn) Icons.Filled.Mic else Icons.Filled.MicOff,
            contentDescription = "FF Mic Status",
            tint = if (isMicOn) palette.goldPrimary else palette.textSecondary,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text =
              if (isMicOn) (if (isBangla) "FF মাইক: চালু" else "FF MIC: ON")
              else (if (isBangla) "FF মাইক: বন্ধ" else "FF MIC: OFF"),
            style = MaterialTheme.typography.labelLarge,
            color = if (isMicOn) palette.goldBright else palette.textSecondary,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = if (isBangla) "প্রিয় ট্যাকটিক্যাল সাউন্ড" else "FAVORITE TACTICAL SOUNDS",
      style = MaterialTheme.typography.labelMedium,
      color = palette.textSecondary,
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Favorite Sounds Grid
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      if (favoriteSounds.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            text =
              if (isBangla) "হোমে সাউন্ডে ★ ট্যাপ করলে দ্রুত খেলার জন্য এখানে দেখাবে।"
              else "Star sounds on the Home screen to pin quick tactical triggers here.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
          )
        }
      } else {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          val cols = if (maxWidth >= 600.dp) 4 else 2
          LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(favoriteSounds, key = { it.id }) { sound ->
              val active = playback.currentSound?.id == sound.id && playback.isPlaying
              ClayCard(
                onClick = { onPlaySound(sound) },
                modifier = Modifier.fillMaxWidth().height(92.dp),
                cornerRadius = 18.dp,
                elevation = if (active) 12.dp else 8.dp,
                baseColor = if (active) palette.surfaceHighlight else palette.surface,
                borderColor = if (active) palette.goldPrimary else palette.border,
                borderWidth = if (active) 1.5.dp else 1.dp,
              ) {
                Column(
                  modifier = Modifier.fillMaxSize().padding(12.dp),
                  verticalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(sound.emoji, fontSize = 24.sp)
                    if (active) {
                      PlayingEqualizerStrip(modifier = Modifier.width(42.dp))
                    }
                  }
                  Text(
                    text = sound.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (active) palette.goldBright else palette.textPrimary,
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

    Spacer(modifier = Modifier.height(12.dp))

    ClayButton(
      onClick = onStopAll,
      containerColor = palette.danger,
      contentColor = Color.White,
      cornerRadius = 18.dp,
      elevation = 10.dp,
      modifier = Modifier.fillMaxWidth().height(54.dp).testTag("game_mode_stop_all_btn"),
    ) {
      Icon(Icons.Filled.Stop, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = if (isBangla) "সব সাউন্ড বন্ধ করো" else "STOP ALL SOUNDS",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
      )
    }
  }
}
