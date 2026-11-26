package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.audio.CartesiaVoiceOption
import com.example.ui.theme.LocalZonexPalette
import kotlin.math.roundToInt

@Composable
fun TextToVoiceCard(
  isBangla: Boolean,
  voices: List<CartesiaVoiceOption>,
  selectedVoiceId: String,
  savedKeyCount: Int,
  ttsPitch: Float,
  ttsSpeechRate: Float,
  inputText: String,
  isSynthesizing: Boolean,
  isSavingCard: Boolean,
  ttsError: String?,
  canSaveLastOutput: Boolean,
  onInputTextChange: (String) -> Unit,
  onOpenVoiceChooser: () -> Unit,
  onPitchChange: (Float) -> Unit,
  onRateChange: (Float) -> Unit,
  onOpenCartesiaKeyDialog: () -> Unit,
  onRefreshCartesiaVoices: () -> Unit,
  onSendPlay: () -> Unit,
  onSaveToCard: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalZonexPalette.current
  val selectedVoice = voices.firstOrNull { it.id == selectedVoiceId } ?: voices.firstOrNull()
  val hasCartesiaKey = savedKeyCount > 0

  ClayCard(
    modifier = modifier.fillMaxWidth(),
    baseColor = palette.surface,
    cornerRadius = 22.dp,
    elevation = 10.dp,
    borderColor = palette.goldPrimary.copy(alpha = 0.5f),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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
            imageVector = Icons.Filled.RecordVoiceOver,
            contentDescription = null,
            tint = palette.goldPrimary,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = if (isBangla) "টেক্সট → ভয়েস স্টুডিও" else "Text → Voice Studio",
            style = MaterialTheme.typography.titleMedium,
            color = palette.goldBright,
            fontWeight = FontWeight.Bold,
          )
        }
        IconButton(
          onClick = onRefreshCartesiaVoices,
          modifier = Modifier.size(32.dp).testTag("tts_sync_voices_btn"),
        ) {
          Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "Sync Cartesia Voices",
            tint = palette.goldPrimary,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      // Voice Selector Pill (opens Claymorphism Voice Picker Modal matching Screenshots 2 & 3)
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .claySurface(
              baseColor = palette.surfaceElevated,
              cornerRadius = 16.dp,
              elevation = 7.dp,
              accentBorderColor = palette.goldPrimary.copy(alpha = 0.55f),
            )
            .clickable(onClick = onOpenVoiceChooser)
            .testTag("tts_voice_selector"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (selectedVoice != null) {
            VoiceAvatarBadge(voice = selectedVoice, size = 34.dp)
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text =
                selectedVoice?.name
                  ?: if (isBangla) "ভয়েস বাছাই করো" else "Choose Voice",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = palette.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text =
                if (isBangla) "ট্যাপ করে ভয়েস বাছাই ও অর্ডার সাজাও (${voices.size} টি)"
                else "Tap to choose character voice & reorder (${voices.size})",
              style = MaterialTheme.typography.labelSmall,
              color = palette.goldPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose Voice", tint = palette.goldPrimary)
        }
      }

      // Pitch & Speed Sliders for Offline Voice Modulator
      if (selectedVoice?.isLocalPreset == true || selectedVoice?.id?.startsWith("DEVICE_") == true) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = palette.surfaceElevated.copy(alpha = 0.6f),
          border = BorderStroke(1.dp, palette.border),
        ) {
          Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = if (isBangla) "ভয়েস পিচ (Pitch)" else "Voice Pitch",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
              )
              Text(
                text = "${(ttsPitch * 100).roundToInt() / 100.0}x",
                style = MaterialTheme.typography.labelSmall,
                color = palette.goldPrimary,
                fontWeight = FontWeight.Bold,
              )
            }
            Slider(
              value = ttsPitch,
              onValueChange = onPitchChange,
              valueRange = 0.5f..1.7f,
              colors =
                SliderDefaults.colors(
                  thumbColor = palette.goldPrimary,
                  activeTrackColor = palette.goldPrimary,
                ),
              modifier = Modifier.height(28.dp),
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = if (isBangla) "স্পিড (Speed)" else "Voice Speed",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
              )
              Text(
                text = "${(ttsSpeechRate * 100).roundToInt() / 100.0}x",
                style = MaterialTheme.typography.labelSmall,
                color = palette.goldPrimary,
                fontWeight = FontWeight.Bold,
              )
            }
            Slider(
              value = ttsSpeechRate,
              onValueChange = onRateChange,
              valueRange = 0.6f..1.6f,
              colors =
                SliderDefaults.colors(
                  thumbColor = palette.goldPrimary,
                  activeTrackColor = palette.goldPrimary,
                ),
              modifier = Modifier.height(28.dp),
            )
          }
        }
      }

      // Cartesia Multi-Key Rotation CTA + Character Credit Meter
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .claySurface(
              baseColor = palette.surfaceElevated,
              cornerRadius = 15.dp,
              elevation = 6.dp,
              accentBorderColor = palette.goldPrimary.copy(alpha = 0.5f),
            )
            .clickable(onClick = onOpenCartesiaKeyDialog)
            .testTag("tts_cartesia_key_btn"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              Icons.Filled.Key,
              contentDescription = null,
              tint = palette.goldPrimary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text =
                if (hasCartesiaKey) {
                  if (isBangla) "🔑 আমার Cartesia Key ($savedKeyCount টি • অটো-রোটেশন)"
                  else "🔑 My Cartesia Keys ($savedKeyCount saved • Auto-rotate)"
                } else {
                  if (isBangla) "🔑 আমার Cartesia Key যোগ করো (অথবা ফ্রি প্রিসেট)"
                  else "🔑 Add My Cartesia Keys (or use Free presets)"
                },
              style = MaterialTheme.typography.labelLarge,
              color = palette.goldBright,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = palette.background.copy(alpha = 0.8f),
          ) {
            Text(
              text =
                if (isBangla) "${inputText.length} অক্ষর"
                else "${inputText.length} chars",
              style = MaterialTheme.typography.labelSmall,
              color = palette.info,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
      }

      // Input box
      OutlinedTextField(
        value = inputText,
        onValueChange = onInputTextChange,
        placeholder = {
          Text(
            if (isBangla) "এখানে লিখো, সেন্ড করলে ভয়েস হয়ে বাজবে..."
            else "Type text here, tap Send to speak as voice...",
            color = palette.textSecondary,
          )
        },
        minLines = 2,
        maxLines = 3,
        shape = RoundedCornerShape(12.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.goldPrimary,
            unfocusedBorderColor = palette.border,
            focusedContainerColor = palette.background,
            unfocusedContainerColor = palette.background,
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary,
            cursorColor = palette.goldPrimary,
          ),
        modifier = Modifier.fillMaxWidth().testTag("tts_text_input"),
      )

      if (!ttsError.isNullOrBlank()) {
        Text(
          text = ttsError,
          style = MaterialTheme.typography.bodySmall,
          color = palette.danger,
        )
      }

      // Send & Save to Card buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ClayButton(
          onClick = onSendPlay,
          enabled = !isSynthesizing && inputText.isNotBlank(),
          containerColor = palette.success,
          contentColor = palette.onGold,
          cornerRadius = 16.dp,
          modifier = Modifier.weight(1f).height(48.dp).testTag("tts_send_btn"),
        ) {
          if (isSynthesizing) {
            CircularProgressIndicator(
              color = palette.onGold,
              strokeWidth = 2.dp,
              modifier = Modifier.size(18.dp),
            )
          } else {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isBangla) "সেন্ড" else "Send",
              fontWeight = FontWeight.ExtraBold,
            )
          }
        }

        ClayButton(
          onClick = onSaveToCard,
          enabled = canSaveLastOutput && !isSavingCard && !isSynthesizing,
          containerColor = palette.surfaceElevated,
          contentColor = palette.goldBright,
          borderColor = palette.goldPrimary.copy(alpha = 0.7f),
          cornerRadius = 16.dp,
          modifier = Modifier.weight(1f).height(48.dp).testTag("tts_save_card_btn"),
        ) {
          if (isSavingCard) {
            CircularProgressIndicator(
              color = palette.goldPrimary,
              strokeWidth = 2.dp,
              modifier = Modifier.size(18.dp),
            )
          } else {
            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isBangla) "কার্ডে সেভ" else "Save Card",
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

@Composable
fun VoiceAvatarBadge(
  voice: CartesiaVoiceOption,
  size: Dp = 36.dp,
) {
  val palette = LocalZonexPalette.current
  val accent = parseHexColorSafe(voice.badgeColorHex, palette.goldPrimary)
  Box(
    modifier =
      Modifier.size(size)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(listOf(accent, palette.surfaceElevated))
        )
        .border(1.4.dp, Color.White.copy(alpha = 0.35f), CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    if (voice.avatarUrl.isNotBlank()) {
      AsyncImage(
        model = voice.avatarUrl,
        contentDescription = voice.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      val initials =
        voice.name
          .trim()
          .split(" ")
          .filter { it.isNotBlank() }
          .take(2)
          .joinToString("") { it.take(1) }
          .ifBlank { "🎙" }
          .uppercase()
      Text(
        text = initials,
        color = Color.White,
        fontWeight = FontWeight.ExtraBold,
        fontSize = if (size >= 34.dp) 12.sp else 10.sp,
      )
    }
  }
}

@Composable
fun VoiceSelectorDialog(
  voices: List<CartesiaVoiceOption>,
  selectedVoiceId: String,
  isRefreshing: Boolean,
  isBangla: Boolean,
  onSelectVoice: (String) -> Unit,
  onMoveVoiceOrder: (voiceId: String, moveUp: Boolean) -> Unit,
  onDeleteCustomVoice: (String) -> Unit,
  onOpenCustomVoiceDialog: () -> Unit,
  onRefreshVoices: () -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalZonexPalette.current

  Dialog(onDismissRequest = onDismiss) {
    ClayCard(
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = 430.dp)
          .heightIn(max = 600.dp)
          .testTag("voice_chooser_dialog"),
      baseColor = palette.surfaceElevated,
      cornerRadius = 26.dp,
      elevation = 16.dp,
      borderColor = palette.goldPrimary.copy(alpha = 0.7f),
      borderWidth = 1.4.dp,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = if (isBangla) "🎭 ভয়েস বাছাই করো" else "🎭 Choose Voice",
          style = MaterialTheme.typography.titleLarge,
          color = palette.textPrimary,
          fontWeight = FontWeight.ExtraBold,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )

        LazyColumn(
          modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          itemsIndexed(voices, key = { _, v -> v.id }) { idx, voice ->
            val selected = voice.id == selectedVoiceId
            val rowBaseColor =
              if (selected) Color(0xFF15803D) else palette.surface
            val rowBorderColor =
              if (selected) Color(0xFF4ADE80) else palette.border.copy(alpha = 0.75f)

            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .claySurface(
                    baseColor = rowBaseColor,
                    cornerRadius = 18.dp,
                    elevation = if (selected) 10.dp else 6.dp,
                    accentBorderColor = rowBorderColor,
                  )
                  .clickable {
                    onSelectVoice(voice.id)
                    onDismiss()
                  }
                  .testTag("voice_option_${voice.id}"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                VoiceAvatarBadge(voice = voice, size = 38.dp)

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = voice.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) Color.White else palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = voice.language,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color.White.copy(alpha = 0.85f) else palette.textSecondary,
                    maxLines = 1,
                  )
                }

                if (voice.isCustom) {
                  IconButton(
                    onClick = { onDeleteCustomVoice(voice.id) },
                    modifier = Modifier.size(30.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.DeleteOutline,
                      contentDescription = "Delete custom voice",
                      tint = if (selected) Color.White else palette.danger,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }

                // Reorder arrows ▲ / ▼
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  IconButton(
                    onClick = { onMoveVoiceOrder(voice.id, true) },
                    enabled = idx > 0,
                    modifier = Modifier.size(28.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.KeyboardArrowUp,
                      contentDescription = "Move Up",
                      tint =
                        if (idx == 0) Color.Gray.copy(alpha = 0.35f)
                        else if (selected) Color.White else palette.textSecondary,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                  IconButton(
                    onClick = { onMoveVoiceOrder(voice.id, false) },
                    enabled = idx < voices.lastIndex,
                    modifier = Modifier.size(28.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.KeyboardArrowDown,
                      contentDescription = "Move Down",
                      tint =
                        if (idx == voices.lastIndex) Color.Gray.copy(alpha = 0.35f)
                        else if (selected) Color.White else palette.textSecondary,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }

                if (selected) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFFBBF7D0),
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
            }
          }
        }

        // Bottom actions matching Screenshots 2 & 3
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ClayButton(
            onClick = {
              onDismiss()
              onOpenCustomVoiceDialog()
            },
            containerColor = Color(0xFF4C1D95),
            contentColor = Color.White,
            borderColor = Color(0xFFA78BFA).copy(alpha = 0.6f),
            cornerRadius = 15.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.weight(1.1f).height(46.dp).testTag("add_custom_voice_btn"),
          ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = palette.goldPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = if (isBangla) "নিজের ভয়েস ID যোগ" else "Add Voice ID",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          ClayButton(
            onClick = onRefreshVoices,
            enabled = !isRefreshing,
            containerColor = palette.surface,
            contentColor = palette.goldBright,
            borderColor = palette.goldPrimary.copy(alpha = 0.55f),
            cornerRadius = 15.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.weight(0.95f).height(46.dp).testTag("refresh_voice_list_btn"),
          ) {
            if (isRefreshing) {
              CircularProgressIndicator(
                color = palette.goldPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp),
              )
            } else {
              Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = if (isBangla) "তালিকা রিফ্রেশ" else "Refresh List",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        ClayButton(
          onClick = onDismiss,
          containerColor = palette.surface,
          contentColor = palette.textPrimary,
          borderColor = palette.border,
          cornerRadius = 15.dp,
          modifier = Modifier.fillMaxWidth().height(44.dp),
        ) {
          Text(
            text = if (isBangla) "বন্ধ করো" else "Close",
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
fun CustomVoiceIdDialog(
  isBangla: Boolean,
  onAdd: (name: String, cartesiaVoiceId: String, avatarUrl: String) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalZonexPalette.current
  var name by remember { mutableStateOf("") }
  var voiceId by remember { mutableStateOf("") }
  var avatarUrl by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    ClayCard(
      modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
      baseColor = palette.surfaceElevated,
      cornerRadius = 26.dp,
      elevation = 16.dp,
      borderColor = palette.goldPrimary,
      borderWidth = 1.4.dp,
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = if (isBangla) "⭐ নিজের ভয়েস ID যোগ করো" else "⭐ Add Custom Cartesia Voice ID",
          style = MaterialTheme.typography.titleMedium,
          color = palette.goldBright,
          fontWeight = FontWeight.ExtraBold,
        )
        Text(
          text =
            if (isBangla) "Cartesia ড্যাশবোর্ডে তৈরি করা নিজের বা ক্লোন করা ভয়েসের Name ও Voice ID বসাও।"
            else "Enter a label and the UUID of your cloned or custom Cartesia Voice ID.",
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary,
        )
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(if (isBangla) "ভয়েসের নাম (যেমন: My Clone)" else "Voice Name") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = voiceId,
          onValueChange = { voiceId = it },
          label = { Text("Cartesia Voice ID (UUID)") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = avatarUrl,
          onValueChange = { avatarUrl = it },
          label = { Text(if (isBangla) "ছবির লিংক (ঐচ্ছিক)" else "Avatar Photo URL (optional)") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text(if (isBangla) "বাতিল" else "Cancel", color = palette.textSecondary)
          }
          Button(
            onClick = { onAdd(name, voiceId, avatarUrl) },
            enabled = voiceId.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = palette.goldPrimary, contentColor = palette.onGold),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1.2f).height(46.dp),
          ) {
            Text(if (isBangla) "যোগ করো" else "Add Voice", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun CartesiaApiKeyDialog(
  savedKeys: List<String>,
  activeKeyIndex: Int,
  isBangla: Boolean,
  onAddKey: (String) -> Unit,
  onRemoveKey: (Int) -> Unit,
  onSetActiveKey: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalZonexPalette.current
  var newKeyInput by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    ClayCard(
      modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).testTag("cartesia_key_dialog"),
      baseColor = palette.surfaceElevated,
      cornerRadius = 26.dp,
      elevation = 16.dp,
      borderColor = palette.goldPrimary.copy(alpha = 0.7f),
      borderWidth = 1.4.dp,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = if (isBangla) "🔑 আমার Cartesia Key" else "🔑 My Cartesia Key",
          style = MaterialTheme.typography.titleLarge,
          color = palette.textPrimary,
          fontWeight = FontWeight.ExtraBold,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )

        Text(
          text =
            if (isBangla)
              "প্রতিটি key আলাদা অ্যাকাউন্ট: ফ্রিতে ২০,০০০ ক্রেডিট (১ অক্ষর = ১ ক্রেডিট)। একটা শেষ হলে অটো পরের key চলবে। Key শুধু তোমার ফোনে থাকে।\nফ্রি key নিতে: play.cartesia.ai → API Keys"
            else
              "Each key gives 20,000 free credits (1 character = 1 credit). Auto-rotates to the next key when exhausted. Keys stay only on your device.\nGet free keys: play.cartesia.ai → API Keys",
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary,
        )

        if (savedKeys.isEmpty()) {
          Text(
            text =
              if (isBangla) "এখনো কোনো key নেই। নিচে পেস্ট করে যোগ করো।"
              else "No keys saved yet. Paste one below to add.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            itemsIndexed(savedKeys) { idx, key ->
              val isActive = idx == activeKeyIndex.coerceIn(0, savedKeys.lastIndex)
              val masked =
                if (key.length > 12) "${key.take(8)}••••${key.takeLast(4)}" else key
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .claySurface(
                      baseColor = if (isActive) palette.success.copy(alpha = 0.22f) else palette.surface,
                      cornerRadius = 14.dp,
                      elevation = 4.dp,
                      accentBorderColor = if (isActive) palette.success else palette.border,
                    )
                    .clickable { onSetActiveKey(idx) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "#${idx + 1}  $masked",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textPrimary,
                  )
                  Text(
                    text =
                      if (isActive) (if (isBangla) "সক্রিয় Key" else "Active Key")
                      else (if (isBangla) "স্ট্যান্ডবাই (ট্যাপ করে সক্রিয় করুন)" else "Standby (tap to activate)"),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) palette.success else palette.textSecondary,
                  )
                }
                IconButton(onClick = { onRemoveKey(idx) }, modifier = Modifier.size(30.dp)) {
                  Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Remove Key",
                    tint = palette.danger,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }
        }

        OutlinedTextField(
          value = newKeyInput,
          onValueChange = { newKeyInput = it },
          placeholder = {
            Text(
              if (isBangla) "এখানে Cartesia API key পেস্ট করো"
              else "Paste Cartesia API key here (sk_car_...)",
              color = palette.textSecondary,
            )
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = palette.goldPrimary,
              unfocusedBorderColor = palette.border,
              focusedContainerColor = palette.background,
              unfocusedContainerColor = palette.background,
              focusedTextColor = palette.textPrimary,
              unfocusedTextColor = palette.textPrimary,
            ),
          modifier = Modifier.fillMaxWidth().testTag("cartesia_key_input"),
        )

        ClayButton(
          onClick = {
            if (newKeyInput.isNotBlank()) {
              onAddKey(newKeyInput)
              newKeyInput = ""
            }
          },
          enabled = newKeyInput.isNotBlank(),
          containerColor = Color(0xFF10B981),
          contentColor = Color(0xFF06281E),
          cornerRadius = 15.dp,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("cartesia_add_key_btn"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isBangla) "Key যোগ করো" else "Add Key",
            fontWeight = FontWeight.ExtraBold,
          )
        }

        TextButton(
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth().testTag("cartesia_close_key_btn"),
        ) {
          Text(
            text = if (isBangla) "বন্ধ করো" else "Close",
            color = palette.textSecondary,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}
