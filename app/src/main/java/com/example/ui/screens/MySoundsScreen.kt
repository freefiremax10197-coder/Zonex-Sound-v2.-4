package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PlaybackState
import com.example.data.model.SoundItem
import com.example.ui.components.ClayButton
import com.example.ui.components.ClayCard
import com.example.ui.components.IdleEqualizerStrip
import com.example.ui.components.PlayingEqualizerStrip
import com.example.ui.components.claySurface
import com.example.ui.theme.LocalZonexPalette

private enum class MySoundsSection {
  PRIVATE,
  PUBLIC,
  DOWNLOAD,
}

@Composable
fun MySoundsScreen(
  downloadedSounds: List<SoundItem>,
  userPublicUploads: List<SoundItem>,
  privateSounds: List<SoundItem>,
  favoriteIds: Set<String>,
  downloadedIds: Set<String>,
  playback: PlaybackState,
  animationsEnabled: Boolean,
  isBanglaUi: Boolean,
  onPlaySound: (SoundItem) -> Unit,
  onStopAll: () -> Unit,
  onToggleFavorite: (SoundItem) -> Unit,
  onSaveSound: (SoundItem) -> Unit,
  onRemoveDownload: (String) -> Unit,
  onDeleteUploaded: (SoundItem) -> Unit,
  onOpenUploadModal: () -> Unit,
  onOpenGameMode: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalZonexPalette.current
  var selectedSection by remember { mutableStateOf(MySoundsSection.PRIVATE) }

  val activeSounds =
    remember(selectedSection, privateSounds, userPublicUploads, downloadedSounds) {
      when (selectedSection) {
        MySoundsSection.PRIVATE -> privateSounds
        MySoundsSection.PUBLIC -> userPublicUploads
        MySoundsSection.DOWNLOAD -> downloadedSounds
      }
    }

  Column(modifier = modifier.fillMaxSize().background(palette.background)) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (isBanglaUi) "মাই সাউন্ডস" else "My Sounds",
            style = MaterialTheme.typography.headlineSmall,
            color = palette.textPrimary,
          )
          Text(
            text =
              if (isBanglaUi)
                "${privateSounds.size} প্রাইভেট • ${userPublicUploads.size} পাবলিক • ${downloadedSounds.size} অফলাইন ডাউনলোড"
              else
                "${privateSounds.size} private • ${userPublicUploads.size} public • ${downloadedSounds.size} offline downloads",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Top 2 buttons: GAME MODE + ADD SOUND
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ClayButton(
          onClick = onOpenGameMode,
          containerColor = palette.surfaceElevated,
          contentColor = palette.goldBright,
          borderColor = palette.goldPrimary,
          cornerRadius = 16.dp,
          modifier = Modifier.weight(1f).height(46.dp).testTag("my_sounds_game_mode_btn"),
        ) {
          Icon(
            imageVector = Icons.Filled.SportsEsports,
            contentDescription = "Game Mode",
            tint = palette.goldPrimary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isBanglaUi) "গেম মোড" else "GAME MODE",
            style = MaterialTheme.typography.labelLarge,
            color = palette.goldBright,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
          )
        }

        ClayButton(
          onClick = onOpenUploadModal,
          containerColor = palette.goldPrimary,
          contentColor = palette.onGold,
          cornerRadius = 16.dp,
          modifier = Modifier.weight(1f).height(46.dp).testTag("my_sounds_upload_btn"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isBanglaUi) "সাউন্ড যোগ" else "ADD SOUND",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 3 Section Tabs: Private | Public | Download
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SectionTabChip(
          label =
            if (isBanglaUi) "প্রাইভেট (${privateSounds.size})"
            else "Private (${privateSounds.size})",
          icon = Icons.Filled.Lock,
          selected = selectedSection == MySoundsSection.PRIVATE,
          onClick = { selectedSection = MySoundsSection.PRIVATE },
          modifier = Modifier.weight(1f).testTag("tab_my_private"),
        )
        SectionTabChip(
          label =
            if (isBanglaUi) "পাবলিক (${userPublicUploads.size})"
            else "Public (${userPublicUploads.size})",
          icon = Icons.Filled.Public,
          selected = selectedSection == MySoundsSection.PUBLIC,
          onClick = { selectedSection = MySoundsSection.PUBLIC },
          modifier = Modifier.weight(1f).testTag("tab_my_public"),
        )
        SectionTabChip(
          label =
            if (isBanglaUi) "ডাউনলোড (${downloadedSounds.size})"
            else "Download (${downloadedSounds.size})",
          icon = Icons.Filled.OfflinePin,
          selected = selectedSection == MySoundsSection.DOWNLOAD,
          onClick = { selectedSection = MySoundsSection.DOWNLOAD },
          modifier = Modifier.weight(1f).testTag("tab_my_download"),
        )
      }
    }

    if (activeSounds.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Box(
            modifier = Modifier.size(68.dp).clip(CircleShape).background(palette.surfaceElevated),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector =
                when (selectedSection) {
                  MySoundsSection.PRIVATE -> Icons.Filled.Lock
                  MySoundsSection.PUBLIC -> Icons.Filled.Public
                  MySoundsSection.DOWNLOAD -> Icons.Filled.FolderSpecial
                },
              contentDescription = null,
              tint = palette.goldPrimary,
              modifier = Modifier.size(32.dp),
            )
          }
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text =
              when (selectedSection) {
                MySoundsSection.PRIVATE ->
                  if (isBanglaUi) "কোনো প্রাইভেট সাউন্ড নেই" else "No Private Sounds Yet"
                MySoundsSection.PUBLIC ->
                  if (isBanglaUi) "কোনো পাবলিক আপলোড নেই" else "No Public Uploads Yet"
                MySoundsSection.DOWNLOAD ->
                  if (isBanglaUi) "কোনো ডাউনলোড করা সাউন্ড নেই" else "No Offline Downloads Yet"
              },
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text =
              when (selectedSection) {
                MySoundsSection.PRIVATE ->
                  if (isBanglaUi) "+ ADD SOUND থেকে প্রাইভেট অডিও যোগ করুন।"
                  else "Tap + ADD SOUND to save private clips for yourself."
                MySoundsSection.PUBLIC ->
                  if (isBanglaUi) "+ ADD SOUND থেকে সবার জন্য পাবলিক অডিও শেয়ার করুন।"
                  else "Tap + ADD SOUND and choose Public to share with everyone."
                MySoundsSection.DOWNLOAD ->
                  if (isBanglaUi) "হোম পেজ থেকে ⬇ ডাউনলোড করলে অফলাইনেও এখান থেকে ও ফ্লোটিং উইন্ডোতে চলবে!"
                  else "Tap ⬇ on any sound to download it here — plays 100% offline & in Floating Window!"
              },
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )
          if (selectedSection != MySoundsSection.DOWNLOAD) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = onOpenUploadModal,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = palette.goldPrimary,
                  contentColor = palette.onGold,
                ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(46.dp).testTag("my_sounds_empty_upload_btn"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (isBanglaUi) "সাউন্ড যোগ করুন" else "ADD SOUND", style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }
    } else {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 600.dp) 3 else 2
        LazyVerticalGrid(
          columns = GridCells.Fixed(columns),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 100.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(activeSounds, key = { "${selectedSection.name}_${it.id}" }) { sound ->
            val isCurrent = playback.currentSound?.id == sound.id
            val isDownloaded = selectedSection == MySoundsSection.DOWNLOAD || downloadedIds.contains(sound.id)
            val badgeLabel =
              when (selectedSection) {
                MySoundsSection.PRIVATE -> if (isBanglaUi) "প্রাইভেট" else "Private"
                MySoundsSection.PUBLIC -> if (isBanglaUi) "পাবলিক" else "Public"
                MySoundsSection.DOWNLOAD -> if (isBanglaUi) "অফলাইন" else "Offline"
              }
            MySoundSquareCard(
              sound = sound,
              badgeLabel = badgeLabel,
              isOfflineSaved = isDownloaded,
              showDownloadButton = selectedSection != MySoundsSection.DOWNLOAD,
              isPlaying = isCurrent && playback.isPlaying,
              isBuffering = isCurrent && playback.isBuffering,
              isFavorite = favoriteIds.contains(sound.id),
              animationsEnabled = animationsEnabled,
              onPlayClick = {
                if (isCurrent && playback.isPlaying) onStopAll() else onPlaySound(sound)
              },
              onToggleFavorite = { onToggleFavorite(sound) },
              onDownloadClick = { onSaveSound(sound) },
              onDeleteClick = {
                if (selectedSection == MySoundsSection.DOWNLOAD) onRemoveDownload(sound.id)
                else onDeleteUploaded(sound)
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SectionTabChip(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalZonexPalette.current
  Box(
    modifier =
      modifier
        .height(40.dp)
        .claySurface(
          baseColor = if (selected) palette.goldPrimary else palette.surface,
          cornerRadius = 14.dp,
          elevation = if (selected) 8.dp else 5.dp,
          accentBorderColor = if (selected) palette.goldPrimary else palette.border,
        )
        .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) palette.onGold else palette.goldPrimary,
        modifier = Modifier.size(14.dp),
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) palette.onGold else palette.textPrimary,
        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun MySoundSquareCard(
  sound: SoundItem,
  badgeLabel: String,
  isOfflineSaved: Boolean,
  showDownloadButton: Boolean,
  isPlaying: Boolean,
  isBuffering: Boolean,
  isFavorite: Boolean,
  animationsEnabled: Boolean,
  onPlayClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  onDownloadClick: () -> Unit,
  onDeleteClick: () -> Unit,
) {
  val palette = LocalZonexPalette.current
  ClayCard(
    modifier =
      Modifier.fillMaxWidth()
        .height(174.dp)
        .testTag("my_sound_card_${sound.id}"),
    baseColor = if (isPlaying) palette.surfaceHighlight else palette.surface,
    cornerRadius = 22.dp,
    elevation = if (isPlaying) 14.dp else 9.dp,
    borderColor = if (isPlaying) palette.goldPrimary else palette.border.copy(alpha = 0.65f),
    borderWidth = if (isPlaying) 1.8.dp else 1.dp,
    onClick = onPlayClick,
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
          modifier = Modifier.fillMaxSize().background(palette.goldPrimary.copy(alpha = 0.12f))
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
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          // Top-left status badge (Private / Public / Offline)
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(9.dp))
                .background(
                  if (isOfflineSaved) Color(0xFF065F46).copy(alpha = 0.85f)
                  else Color.Black.copy(alpha = 0.62f)
                )
                .border(
                  1.dp,
                  if (isOfflineSaved) palette.success else palette.goldPrimary.copy(alpha = 0.55f),
                  RoundedCornerShape(9.dp),
                )
                .padding(horizontal = 7.dp, vertical = 3.dp)
          ) {
            Text(
              text = badgeLabel,
              style = MaterialTheme.typography.labelSmall,
              color = if (isOfflineSaved) Color(0xFFA7F3D0) else palette.goldBright,
              fontWeight = FontWeight.Bold,
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            if (showDownloadButton) {
              Box(
                modifier =
                  Modifier.size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.60f))
                    .border(
                      1.dp,
                      if (isOfflineSaved) palette.success else Color.White.copy(alpha = 0.25f),
                      CircleShape,
                    )
                    .clickable(onClick = onDownloadClick)
                    .testTag("my_dl_btn_${sound.id}"),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector =
                    if (isOfflineSaved) Icons.Filled.BookmarkAdded else Icons.Filled.Download,
                  contentDescription = "Save Offline",
                  tint = if (isOfflineSaved) palette.success else Color.White,
                  modifier = Modifier.size(14.dp),
                )
              }
            }

            Box(
              modifier =
                Modifier.size(28.dp)
                  .clip(CircleShape)
                  .background(Color.Black.copy(alpha = 0.60f))
                  .border(
                    1.dp,
                    if (isFavorite) palette.goldPrimary else Color.White.copy(alpha = 0.25f),
                    CircleShape,
                  )
                  .clickable(onClick = onToggleFavorite)
                  .testTag("my_fav_btn_${sound.id}"),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "Pin to Floating Window",
                tint = if (isFavorite) palette.goldPrimary else Color.White,
                modifier = Modifier.size(15.dp),
              )
            }

            Box(
              modifier =
                Modifier.size(28.dp)
                  .clip(CircleShape)
                  .background(Color.Black.copy(alpha = 0.60f))
                  .border(1.dp, palette.danger.copy(alpha = 0.7f), CircleShape)
                  .clickable(onClick = onDeleteClick)
                  .testTag("my_del_btn_${sound.id}"),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "Delete",
                tint = Color(0xFFFF6B8B),
                modifier = Modifier.size(15.dp),
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
          Text(
            text = sound.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isPlaying) palette.goldBright else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

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
