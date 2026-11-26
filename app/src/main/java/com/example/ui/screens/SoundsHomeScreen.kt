package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.CartesiaVoiceOption
import com.example.data.model.AccessKeyState
import com.example.data.model.AdminNotice
import com.example.data.model.PlaybackState
import com.example.data.model.SoundItem
import com.example.shizuku.ShizukuRootState
import com.example.ui.components.ClayButton
import com.example.ui.components.ClayCard
import com.example.ui.components.SoundGridCard
import com.example.ui.components.TextToVoiceCard
import com.example.ui.components.ZonexBrandTitle
import com.example.ui.components.claySurface
import com.example.ui.theme.LocalZonexPalette

@Composable
fun SoundsHomeScreen(
  sounds: List<SoundItem>,
  categories: List<String>,
  selectedCategory: String,
  searchQuery: String,
  favoriteIds: Set<String>,
  likedIds: Set<String>,
  downloadedIds: Set<String>,
  playback: PlaybackState,
  animationsEnabled: Boolean,
  activeOutputSummary: String,
  adminNotice: AdminNotice,
  dismissedBannerText: String,
  isBanglaUi: Boolean,
  isFloatingOpen: Boolean = false,
  shizukuState: ShizukuRootState,
  ttsVoices: List<CartesiaVoiceOption>,
  selectedVoiceId: String,
  savedKeyCount: Int,
  ttsPitch: Float,
  ttsSpeechRate: Float,
  ttsInputText: String,
  isSynthesizingTts: Boolean,
  isSavingTtsCard: Boolean,
  ttsError: String?,
  canSaveLastTtsOutput: Boolean,
  accessKeyState: AccessKeyState,
  onSearchQueryChange: (String) -> Unit,
  onSelectCategory: (String) -> Unit,
  onPlaySound: (SoundItem) -> Unit,
  onToggleFavorite: (SoundItem) -> Unit,
  onToggleLike: (SoundItem) -> Unit,
  onSaveSound: (SoundItem) -> Unit,
  onStopAll: () -> Unit,
  onMaxVolume: () -> Unit,
  onToggleLanguage: () -> Unit,
  onOpenShizukuDialog: () -> Unit,
  onExtendKeyClick: () -> Unit,
  onRedeemKey: (String) -> Unit,
  onFloatClick: () -> Unit,
  onOpenAudioLab: () -> Unit,
  onDismissBanner: () -> Unit,
  onTtsInputChange: (String) -> Unit,
  onOpenVoiceChooser: () -> Unit,
  onTtsPitchChange: (Float) -> Unit,
  onTtsRateChange: (Float) -> Unit,
  onOpenCartesiaKeyDialog: () -> Unit,
  onRefreshCartesiaVoices: () -> Unit,
  onSendPlayTts: () -> Unit,
  onSaveTtsCard: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalZonexPalette.current
  var showTtsStudio by remember { mutableStateOf(false) }
  var showKeyCard by remember { mutableStateOf(false) }
  var redeemInput by remember { mutableStateOf("") }

  val showBanner =
    adminNotice.bannerActive &&
      adminNotice.bannerText.isNotBlank() &&
      adminNotice.bannerText != dismissedBannerText

  Box(modifier = modifier.fillMaxSize().background(palette.background)) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Header
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            ZonexBrandTitle()
            Text(
              text =
                if (isBanglaUi) "ট্যাপ করলে বাজবে, আবার ট্যাপ করলে থামবে"
                else "Tap to Play • Tap again to Stop",
              style = MaterialTheme.typography.bodySmall,
              color = palette.textSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            // বাং / EN Pill Toggle
            Box(
              modifier =
                Modifier.height(36.dp)
                  .claySurface(
                    baseColor = palette.surfaceElevated,
                    cornerRadius = 13.dp,
                    elevation = 6.dp,
                    accentBorderColor = palette.border,
                  )
                  .clickable(onClick = onToggleLanguage)
                  .testTag("lang_toggle_btn"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = if (isBanglaUi) "বাং" else "EN",
                  style = MaterialTheme.typography.labelMedium,
                  color = palette.goldBright,
                  fontWeight = FontWeight.ExtraBold,
                )
              }
            }

            // Shizuku / Root Button
            Box(
              modifier =
                Modifier.height(36.dp)
                  .claySurface(
                    baseColor =
                      if (shizukuState.isInjectionActive) palette.success.copy(alpha = 0.24f)
                      else palette.surfaceElevated,
                    cornerRadius = 13.dp,
                    elevation = 6.dp,
                    accentBorderColor =
                      if (shizukuState.isInjectionActive) palette.success
                      else if (shizukuState.hasShizukuPermission) palette.info
                      else palette.border,
                  )
                  .clickable(onClick = onOpenShizukuDialog)
                  .testTag("header_shizuku_btn"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Security,
                  contentDescription = "Shizuku / Root",
                  tint =
                    if (shizukuState.isInjectionActive) palette.success
                    else palette.goldPrimary,
                  modifier = Modifier.size(15.dp),
                )
                Text(
                  text = if (shizukuState.isInjectionActive) "MIC ON" else "SHIZUKU",
                  style = MaterialTheme.typography.labelSmall,
                  color =
                    if (shizukuState.isInjectionActive) palette.success
                    else palette.textPrimary,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Gaming Action Row: OVERLAY / FLOAT | VOL MAX | STOP ALL
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          HeaderActionPill(
            label =
              if (isFloatingOpen) (if (isBanglaUi) "ওভারলে অন" else "FLOAT ON")
              else (if (isBanglaUi) "ওভারলে" else "FLOAT"),
            icon = Icons.Filled.Layers,
            containerColor =
              if (isFloatingOpen) palette.success.copy(alpha = 0.22f)
              else palette.surfaceElevated,
            contentColor = if (isFloatingOpen) palette.success else palette.goldPrimary,
            borderColor =
              if (isFloatingOpen) palette.success
              else palette.goldPrimary.copy(alpha = 0.65f),
            onClick = onFloatClick,
            modifier = Modifier.weight(1f).testTag("header_float_btn"),
          )
          HeaderActionPill(
            label = if (isBanglaUi) "ভলিউম ম্যাক্স" else "VOL MAX",
            icon = Icons.Filled.VolumeUp,
            containerColor = palette.goldPrimary.copy(alpha = 0.16f),
            contentColor = palette.goldBright,
            borderColor = palette.goldPrimary.copy(alpha = 0.65f),
            onClick = onMaxVolume,
            modifier = Modifier.weight(1f).testTag("header_volmax_btn"),
          )
          HeaderActionPill(
            label = if (isBanglaUi) "সব থামাও" else "STOP",
            icon = Icons.Filled.Stop,
            containerColor = palette.danger,
            contentColor = Color.White,
            borderColor = palette.danger,
            onClick = onStopAll,
            modifier = Modifier.weight(0.95f).testTag("header_stop_btn"),
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Device Audio Compatibility & Audio Lab bar
        ClayCard(
          modifier = Modifier.fillMaxWidth().testTag("device_audio_lab_row"),
          baseColor = palette.surface,
          cornerRadius = 16.dp,
          elevation = 7.dp,
          borderColor = palette.border,
          onClick = onOpenAudioLab,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = palette.success,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = activeOutputSummary,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = if (isBanglaUi) "অডিও ল্যাব" else "Audio Lab",
                style = MaterialTheme.typography.labelSmall,
                color = palette.goldPrimary,
                fontWeight = FontWeight.Bold,
              )
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open Audio Lab",
                tint = palette.goldPrimary,
                modifier = Modifier.size(13.dp),
              )
            }
          }
        }
      }

      // Dismissible ADMIN NOTICE Banner
      AnimatedVisibility(visible = showBanner) {
        ClayCard(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
          baseColor = palette.surfaceElevated,
          cornerRadius = 18.dp,
          elevation = 8.dp,
          borderColor = palette.goldPrimary.copy(alpha = 0.7f),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.Campaign,
              contentDescription = "Admin Notice",
              tint = palette.goldPrimary,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "ADMIN NOTICE",
                style = MaterialTheme.typography.labelSmall,
                color = palette.goldPrimary,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(
                text = adminNotice.bannerText,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textPrimary,
              )
            }
            IconButton(onClick = onDismissBanner, modifier = Modifier.size(32.dp)) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Dismiss admin notice",
                tint = palette.textSecondary,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }

      // API Key Section ABOVE Search Bar
      if (accessKeyState.keyCode.isNotBlank()) {
        val masked =
          if (accessKeyState.keyCode.length > 15) {
            accessKeyState.keyCode.take(10) + "••••" + accessKeyState.keyCode.takeLast(4)
          } else {
            accessKeyState.keyCode
          }
        ClayCard(
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 4.dp)
              .testTag("home_api_key_card"),
          baseColor = palette.surface,
          cornerRadius = 18.dp,
          elevation = 8.dp,
          borderColor = palette.goldPrimary.copy(alpha = 0.55f),
          onClick = { showKeyCard = !showKeyCard },
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(Icons.Filled.Key, contentDescription = null, tint = palette.goldPrimary, modifier = Modifier.size(16.dp))
                Text(
                  text = masked,
                  fontFamily = FontFamily.Monospace,
                  style = MaterialTheme.typography.labelLarge,
                  color = palette.goldBright,
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text =
                    if (isBanglaUi) "⏳ বাকি ${accessKeyState.remainingText}"
                    else "⏳ ${accessKeyState.remainingText} left",
                  style = MaterialTheme.typography.labelMedium,
                  color = palette.success,
                  fontWeight = FontWeight.Bold,
                )
                if (accessKeyState.expiryDateText.isNotBlank()) {
                  Text(
                    text =
                      if (isBanglaUi) "শেষ ডেট: ${accessKeyState.expiryDateText}"
                      else "Expires: ${accessKeyState.expiryDateText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                  )
                }
              }
            }
            AnimatedVisibility(visible = showKeyCard) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = onExtendKeyClick,
                  colors = ButtonDefaults.buttonColors(containerColor = palette.goldPrimary, contentColor = palette.onGold),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.fillMaxWidth().height(42.dp),
                ) {
                  Text(
                    if (isBanglaUi) "⏳ মেয়াদ বাড়ান (Get a Key)" else "⏳ Extend Key (Get a Key)",
                    fontWeight = FontWeight.Bold,
                  )
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  OutlinedTextField(
                    value = redeemInput,
                    onValueChange = { redeemInput = it.uppercase() },
                    placeholder = {
                      Text(
                        if (isBanglaUi) "নতুন Key এখানে বসাও" else "ZONEX-ZIXU-XXXXX-XXXXX",
                        color = palette.textSecondary,
                      )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                  )
                  Button(
                    onClick = {
                      onRedeemKey(redeemInput)
                      redeemInput = ""
                    },
                    enabled = redeemInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.success, contentColor = palette.background),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(50.dp),
                  ) {
                    Text(if (isBanglaUi) "রিডিম" else "Redeem", fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }

      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
          Text(
            if (isBanglaUi) "ভয়েস বা সাউন্ড খুঁজো..." else "Search sounds, memes, effects...",
            color = palette.textSecondary,
          )
        },
        leadingIcon = {
          Icon(Icons.Filled.Search, contentDescription = null, tint = palette.goldPrimary)
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChange("") }) {
              Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = palette.textSecondary)
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.goldPrimary,
            unfocusedBorderColor = palette.border,
            focusedContainerColor = palette.surface,
            unfocusedContainerColor = palette.surface,
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary,
            cursorColor = palette.goldPrimary,
          ),
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("sounds_search_input"),
      )

      // Horizontal Category Chips
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(categories, key = { it }) { cat ->
          val selected = cat.equals(selectedCategory, ignoreCase = true)
          FilterChip(
            selected = selected,
            onClick = { onSelectCategory(cat) },
            label = {
              Text(
                text = cat,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              )
            },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = palette.goldPrimary,
                selectedLabelColor = palette.onGold,
                containerColor = palette.surface,
                labelColor = palette.textSecondary,
              ),
            border =
              FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = palette.border,
                selectedBorderColor = palette.goldPrimary,
              ),
          )
        }
      }

      // Live Sound Grid + Collapsible Text -> Voice Studio header
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 600.dp) 3 else 2
        LazyVerticalGrid(
          columns = GridCells.Fixed(columns),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 100.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          item(span = { GridItemSpan(maxLineSpan) }, key = "tts_header_card") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              ClayCard(
                modifier = Modifier.fillMaxWidth().testTag("toggle_tts_studio_btn"),
                baseColor = palette.surface,
                cornerRadius = 18.dp,
                elevation = 8.dp,
                borderColor = palette.goldPrimary.copy(alpha = 0.5f),
                onClick = { showTtsStudio = !showTtsStudio },
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.RecordVoiceOver,
                      contentDescription = null,
                      tint = palette.goldPrimary,
                      modifier = Modifier.size(18.dp),
                    )
                    Text(
                      text =
                        if (isBanglaUi) "🗣️ টেক্সট → ভয়েস (Cartesia / ফ্রি TTS)"
                        else "🗣️ Text → Voice (Cartesia AI / Free TTS)",
                      style = MaterialTheme.typography.titleSmall,
                      color = palette.goldBright,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                  Icon(
                    imageVector =
                      if (showTtsStudio) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = "Expand Text to Voice",
                    tint = palette.goldPrimary,
                  )
                }
              }

              AnimatedVisibility(visible = showTtsStudio) {
                TextToVoiceCard(
                  isBangla = isBanglaUi,
                  voices = ttsVoices,
                  selectedVoiceId = selectedVoiceId,
                  savedKeyCount = savedKeyCount,
                  ttsPitch = ttsPitch,
                  ttsSpeechRate = ttsSpeechRate,
                  inputText = ttsInputText,
                  isSynthesizing = isSynthesizingTts,
                  isSavingCard = isSavingTtsCard,
                  ttsError = ttsError,
                  canSaveLastOutput = canSaveLastTtsOutput,
                  onInputTextChange = onTtsInputChange,
                  onOpenVoiceChooser = onOpenVoiceChooser,
                  onPitchChange = onTtsPitchChange,
                  onRateChange = onTtsRateChange,
                  onOpenCartesiaKeyDialog = onOpenCartesiaKeyDialog,
                  onRefreshCartesiaVoices = onRefreshCartesiaVoices,
                  onSendPlay = onSendPlayTts,
                  onSaveToCard = onSaveTtsCard,
                )
              }
            }
          }

          if (sounds.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "empty_grid") {
              Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                contentAlignment = Alignment.Center,
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Box(
                    modifier =
                      Modifier.size(64.dp)
                        .clip(CircleShape)
                        .background(palette.surfaceElevated),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Filled.MusicNote,
                      contentDescription = null,
                      tint = palette.goldPrimary,
                      modifier = Modifier.size(30.dp),
                    )
                  }
                  Spacer(modifier = Modifier.height(12.dp))
                  Text(
                    text =
                      if (searchQuery.isNotBlank()) "No sounds match \"$searchQuery\""
                      else if (isBanglaUi) "সাউন্ড লোড হচ্ছে..." else "Syncing live sound library...",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text =
                      if (selectedCategory.equals("Trending", ignoreCase = true)) {
                        if (isBanglaUi) "সাউন্ডে ❤️ লাভ রিয়েক্ট দিলে ট্রেন্ডিং-এ সবার উপরে আসবে"
                        else "Tap ❤️ on any sound to rank it in Trending!"
                      } else {
                        if (isBanglaUi) "নিজের অডিও যোগ করতে My Sounds ট্যাবে যাও"
                        else "Go to My Sounds tab to add your own audio clips."
                      },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                  )
                }
              }
            }
          } else {
            items(sounds, key = { it.id }) { sound ->
              val isCurrent = playback.currentSound?.id == sound.id
              SoundGridCard(
                sound = sound,
                isPlaying = isCurrent && playback.isPlaying,
                isBuffering = isCurrent && playback.isBuffering,
                isFavorite = favoriteIds.contains(sound.id),
                isLiked = likedIds.contains(sound.id),
                isDownloaded = downloadedIds.contains(sound.id),
                animationsEnabled = animationsEnabled,
                onPlayClick = { onPlaySound(sound) },
                onToggleFavorite = { onToggleFavorite(sound) },
                onToggleLike = { onToggleLike(sound) },
                onDownloadClick = { onSaveSound(sound) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HeaderActionPill(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  containerColor: Color,
  contentColor: Color,
  borderColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ClayButton(
    onClick = onClick,
    containerColor = containerColor,
    contentColor = contentColor,
    borderColor = borderColor,
    cornerRadius = 14.dp,
    elevation = 7.dp,
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    modifier = modifier.height(38.dp),
  ) {
    Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(14.dp))
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = contentColor,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
