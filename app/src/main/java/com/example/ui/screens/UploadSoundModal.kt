package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.SoundVisibility
import com.example.data.model.UploadSoundDraft
import com.example.ui.components.ClayButton
import com.example.ui.components.ClayCard
import com.example.ui.components.claySurface
import com.example.ui.theme.LocalZonexPalette
import com.example.ui.viewmodel.UploadModalState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadSoundModal(
  uploadState: UploadModalState,
  availableCategories: List<String>,
  isBangla: Boolean = true,
  onDismiss: () -> Unit,
  onSubmit: (UploadSoundDraft) -> Unit,
) {
  val palette = LocalZonexPalette.current
  val context = LocalContext.current
  val categoryChoices =
    remember(availableCategories) {
      availableCategories
        .filter { !it.equals("All", ignoreCase = true) && !it.equals("Trending", ignoreCase = true) }
        .ifEmpty { listOf("General") }
    }
  var draft by remember {
    mutableStateOf(UploadSoundDraft(category = categoryChoices.firstOrNull() ?: "General"))
  }

  val audioPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
      if (uri != null) {
        val displayName =
          runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
              val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
              if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
            }
          }.getOrNull() ?: "selected_audio.mp3"
        draft =
          draft.copy(
            audioUri = uri,
            audioFileName = displayName,
            useDirectUrl = false,
            title = draft.title.ifBlank { displayName.substringBeforeLast(".") },
          )
      }
    }

  val thumbnailPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
      if (uri != null) {
        draft = draft.copy(thumbnailUri = uri)
      }
    }

  val emojiChoices = listOf("🔥", "😂", "💀", "⚡", "🎮", "🔊", "👑", "💥", "😱", "🎯", "🤡", "🎵")

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .safeDrawingPadding()
          .imePadding()
          .padding(horizontal = 16.dp, vertical = 14.dp),
      contentAlignment = Alignment.Center,
    ) {
      ClayCard(
        modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
        cornerRadius = 24.dp,
        elevation = 16.dp,
        baseColor = palette.surfaceElevated,
        borderColor = palette.goldPrimary.copy(alpha = 0.7f),
        borderWidth = 1.5.dp,
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = if (isBangla) "নতুন সাউন্ড যোগ করুন" else "Add New Sound",
              style = MaterialTheme.typography.headlineSmall,
              color = palette.goldBright,
            )
            IconButton(onClick = onDismiss, enabled = !uploadState.isUploading) {
              Icon(Icons.Filled.Close, contentDescription = "Close", tint = palette.textSecondary)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Column(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())
          ) {

        // Audio Source Mode Toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ClayButton(
            onClick = { audioPickerLauncher.launch(arrayOf("audio/*", "video/*")) },
            enabled = !uploadState.isUploading,
            containerColor = palette.surface,
            contentColor = palette.goldPrimary,
            borderColor = palette.goldPrimary.copy(alpha = 0.75f),
            cornerRadius = 14.dp,
            elevation = 6.dp,
            modifier = Modifier.weight(1f).height(48.dp).testTag("select_audio_file_btn"),
          ) {
            Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              if (isBangla) "অডিও / ভিডিও বাছুন" else "SELECT AUDIO / VIDEO",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
            )
          }
          FilterChip(
            selected = draft.useDirectUrl,
            onClick = { draft = draft.copy(useDirectUrl = !draft.useDirectUrl) },
            label = { Text(if (isBangla) "সরাসরি লিংক" else "Direct URL") },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = palette.goldPrimary,
                selectedLabelColor = palette.onGold,
              ),
          )
        }

        if (!draft.useDirectUrl && draft.audioFileName.isNotBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text =
              if (isBangla) "নির্বাচিত: ${draft.audioFileName} (ভিডিও হলে অডিও আলাদা হবে)"
              else "Selected: ${draft.audioFileName} (videos auto-extract to audio)",
            style = MaterialTheme.typography.bodySmall,
            color = palette.success,
          )
        }

        val fieldColors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.goldPrimary,
            unfocusedBorderColor = palette.border,
            focusedLabelColor = palette.goldPrimary,
            unfocusedLabelColor = palette.textSecondary,
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary,
            cursorColor = palette.goldPrimary,
          )

        if (draft.useDirectUrl) {
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = draft.directAudioUrl,
            onValueChange = { draft = draft.copy(directAudioUrl = it) },
            label = { Text("Direct Audio URL (https://...)") },
            singleLine = true,
            colors = fieldColors,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Custom Thumbnail Picker (Android Photo Picker - zero permissions)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (draft.thumbnailUri != null) {
            Box(
              modifier =
                Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(palette.surface)
            ) {
              AsyncImage(
                model = draft.thumbnailUri,
                contentDescription = "Selected Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
            }
          }
          OutlinedButton(
            onClick = {
              thumbnailPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            enabled = !uploadState.isUploading,
            border = BorderStroke(1.dp, palette.border),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.textPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
          ) {
            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              if (draft.thumbnailUri == null) (if (isBangla) "+ কভার ছবি যোগ করুন" else "+ ADD THUMBNAIL IMAGE")
              else (if (isBangla) "কভার ছবি পরিবর্তন" else "CHANGE THUMBNAIL"),
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = draft.title,
          onValueChange = { draft = draft.copy(title = it) },
          label = { Text(if (isBangla) "সাউন্ডের নাম" else "Sound Name") },
          singleLine = true,
          colors = fieldColors,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("upload_sound_name_input"),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(if (isBangla) "ক্যাটাগরি" else "Category", style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          categoryChoices.forEach { cat ->
            val selected = draft.category == cat
            FilterChip(
              selected = selected,
              onClick = { draft = draft.copy(category = cat) },
              label = { Text(cat) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = palette.goldPrimary,
                  selectedLabelColor = palette.onGold,
                ),
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          if (isBangla) "বিকল্প ইমোজি আইকন" else "Fallback Icon Emoji",
          style = MaterialTheme.typography.labelLarge,
          color = palette.textSecondary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          emojiChoices.forEach { em ->
            val selected = draft.emoji == em
            Box(
              modifier =
                Modifier.size(40.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(
                    if (selected) palette.goldPrimary.copy(alpha = 0.25f) else palette.surface
                  )
                  .clickable { draft = draft.copy(emoji = em) },
              contentAlignment = Alignment.Center,
            ) {
              Text(em, fontSize = 20.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            if (isBangla) "প্রাথমিক ভলিউম" else "Initial Volume",
            style = MaterialTheme.typography.labelLarge,
            color = palette.textSecondary,
          )
          Text(
            "${draft.defaultVolume}%",
            style = MaterialTheme.typography.labelLarge,
            color = palette.goldPrimary,
          )
        }
        Slider(
          value = draft.defaultVolume / 100f,
          onValueChange = { draft = draft.copy(defaultVolume = (it * 100f).roundToInt()) },
          colors =
            SliderDefaults.colors(
              thumbColor = palette.goldPrimary,
              activeTrackColor = palette.goldPrimary,
              inactiveTrackColor = palette.border,
            ),
        )

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable { draft = draft.copy(addToFavorites = !draft.addToFavorites) }
              .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Checkbox(
            checked = draft.addToFavorites,
            onCheckedChange = { draft = draft.copy(addToFavorites = it) },
            colors =
              CheckboxDefaults.colors(
                checkedColor = palette.goldPrimary,
                checkmarkColor = palette.onGold,
              ),
          )
          Text(
            if (isBangla) "ফেভারিট / ওভারলেতে যোগ করুন" else "Add to Favorites",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textPrimary,
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          if (isBangla) "প্রকাশের ধরন" else "Visibility",
          style = MaterialTheme.typography.labelLarge,
          color = palette.textSecondary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          VisibilityOptionCard(
            selected = draft.visibility == SoundVisibility.PUBLIC,
            title = if (isBangla) "পাবলিক" else "Public",
            subtitle = if (isBangla) "সব ইউজার দেখতে পাবে" else "Shared in public library",
            icon = Icons.Filled.Public,
            onClick = { draft = draft.copy(visibility = SoundVisibility.PUBLIC) },
            modifier = Modifier.weight(1f),
          )
          VisibilityOptionCard(
            selected = draft.visibility == SoundVisibility.PRIVATE,
            title = if (isBangla) "প্রাইভেট" else "Private",
            subtitle = if (isBangla) "শুধুমাত্র আপনি দেখতে পাবেন" else "Only visible to you",
            icon = Icons.Filled.Lock,
            onClick = { draft = draft.copy(visibility = SoundVisibility.PRIVATE) },
            modifier = Modifier.weight(1f),
          )
        }

        if (!uploadState.errorMessage.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(10.dp))
          val isPermError = uploadState.errorMessage.contains("Permission denied", ignoreCase = true)
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = palette.danger.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.5f)),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
              Text(
                text = uploadState.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = palette.danger,
              )
              if (isPermError) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Firebase Console → Realtime Database Rules থেকে অনুমোদন না থাকলে Public/Private সেভ করা যায় না।",
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.textSecondary,
                )
                if (draft.visibility == SoundVisibility.PUBLIC) {
                  Spacer(modifier = Modifier.height(6.dp))
                  OutlinedButton(
                    onClick = {
                      val privateDraft = draft.copy(visibility = SoundVisibility.PRIVATE)
                      draft = privateDraft
                      onSubmit(privateDraft)
                    },
                    border = BorderStroke(1.dp, palette.goldPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.goldPrimary),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Text("Switch to Private & Retry")
                  }
                }
              }
            }
          }
        }

        if (uploadState.isUploading) {
          Spacer(modifier = Modifier.height(12.dp))
          LinearProgressIndicator(
            progress = { uploadState.progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = palette.goldPrimary,
            trackColor = palette.border,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Uploading ${(uploadState.progress * 100).roundToInt()}%...",
            style = MaterialTheme.typography.labelSmall,
            color = palette.goldPrimary,
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ClayButton(
          onClick = { onSubmit(draft) },
          enabled = !uploadState.isUploading,
          containerColor = palette.goldPrimary,
          contentColor = palette.onGold,
          cornerRadius = 16.dp,
          elevation = 10.dp,
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("upload_submit_btn"),
        ) {
          Text(
            text =
              if (uploadState.isUploading) (if (isBangla) "আপলোড হচ্ছে..." else "UPLOADING...")
              else (if (isBangla) "সাউন্ড আপলোড করুন" else "UPLOAD SOUND"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
          )
        }
          }
        }
      }
    }
  }
}

@Composable
private fun VisibilityOptionCard(
  selected: Boolean,
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalZonexPalette.current
  ClayCard(
    onClick = onClick,
    modifier = modifier,
    cornerRadius = 16.dp,
    elevation = if (selected) 8.dp else 5.dp,
    baseColor = if (selected) palette.surfaceHighlight else palette.surface,
    borderColor = if (selected) palette.goldPrimary else palette.border,
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          icon,
          contentDescription = null,
          tint = if (selected) palette.goldPrimary else palette.textSecondary,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          color = if (selected) palette.goldBright else palette.textPrimary,
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = palette.textSecondary,
      )
    }
  }
}
