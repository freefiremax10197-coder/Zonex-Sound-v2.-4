package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.UserSettingsState
import com.example.ui.components.ClayCard
import com.example.ui.components.ClayPill
import com.example.ui.components.claySurface
import com.example.ui.theme.LocalZonexPalette
import kotlin.math.roundToInt

@Composable
fun MixerScreen(
  settings: UserSettingsState,
  activeOutputSummary: String,
  isMicActive: Boolean,
  onMasterVolumeChange: (Float) -> Unit,
  onSfxVolumeChange: (Float) -> Unit,
  onMediaVolumeChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalZonexPalette.current
  val context = LocalContext.current
  val isBangla = settings.isBanglaUi
  val hasMicPerm =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
      PackageManager.PERMISSION_GRANTED

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(palette.background)
        .verticalScroll(rememberScrollState())
        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      ClayPill(
        baseColor = palette.goldPrimary.copy(alpha = 0.18f),
        borderColor = palette.goldPrimary.copy(alpha = 0.45f),
        cornerRadius = 14.dp,
        elevation = 6.dp,
        modifier = Modifier.size(42.dp),
      ) {
        Icon(Icons.Filled.Tune, contentDescription = null, tint = palette.goldPrimary)
      }
      Column {
        Text(
          text = if (isBangla) "অডিও চ্যানেল ও মিক্সার" else "Audio Channels & Mixer",
          style = MaterialTheme.typography.headlineSmall,
          color = palette.textPrimary,
        )
        Text(
          text = activeOutputSummary,
          style = MaterialTheme.typography.bodySmall,
          color = palette.textSecondary,
        )
      }
    }

    MixerChannelCard(
      icon = Icons.Filled.VolumeUp,
      title = if (isBangla) "মাস্টার আউটপুট ভলিউম" else "Master Output Volume",
      subtitle = if (isBangla) "পুরো সাউন্ডবোর্ডের মূল ভলিউম কন্ট্রোল" else "Global soundboard engine output ceiling",
      value = settings.masterVolume,
      onValueChange = onMasterVolumeChange,
      testTag = "mixer_master_slider",
    )

    MixerChannelCard(
      icon = Icons.Filled.Equalizer,
      title = if (isBangla) "সাউন্ডবোর্ড SFX চ্যানেল" else "Soundboard SFX Channel",
      subtitle = if (isBangla) "প্রতিটি সাউন্ড ইফেক্টের পাওয়ার নিয়ন্ত্রণ করে" else "Multiplies individual sound trigger loudness",
      value = settings.sfxChannelVolume,
      onValueChange = onSfxVolumeChange,
      testTag = "mixer_sfx_slider",
    )

    MixerChannelCard(
      icon = Icons.Filled.MusicNote,
      title = if (isBangla) "মিডিয়া প্লেব্যাক চ্যানেল" else "Media Playback Channel",
      subtitle = if (isBangla) "সিস্টেম STREAM_MUSIC ভলিউম অ্যাডজাস্ট করে" else "Adjusts Android system STREAM_MUSIC volume",
      value = settings.mediaChannelVolume,
      onValueChange = onMediaVolumeChange,
      testTag = "mixer_media_slider",
    )

    // Microphone Input Stream status card
    ClayCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 20.dp,
      elevation = 9.dp,
      baseColor = palette.surface,
      borderColor = palette.border,
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.size(44.dp)
              .claySurface(
                baseColor =
                  if (isMicActive || hasMicPerm) palette.success.copy(alpha = 0.18f)
                  else palette.surfaceElevated,
                cornerRadius = 22.dp,
                elevation = 5.dp,
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = "Microphone Stream",
            tint = if (isMicActive || hasMicPerm) palette.success else palette.goldPrimary,
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (isBangla) "মাইক্রোফোন ইনপুট স্ট্রিম" else "Microphone Input Stream",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
          )
          Text(
            text =
              when {
                isMicActive -> if (isBangla) "লাইভ মনিটর চালু আছে" else "LIVE MONITOR STREAMING"
                hasMicPerm -> if (isBangla) "অনুমোদিত • ক্যাপচারের জন্য প্রস্তুত" else "AUTHORIZED • READY FOR CAPTURE"
                else -> if (isBangla) "বন্ধ • এখনো পারমিশন দেওয়া হয়নি" else "IDLE • PERMISSION NOT YET REQUESTED"
              },
            style = MaterialTheme.typography.labelMedium,
            color = if (isMicActive || hasMicPerm) palette.success else palette.textSecondary,
          )
        }
        Icon(
          imageVector = Icons.Filled.GraphicEq,
          contentDescription = null,
          tint = if (isMicActive) palette.success else palette.border,
        )
      }
    }

    // Transparent Audio Routing Notice
    ClayCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 20.dp,
      elevation = 7.dp,
      baseColor = palette.surface,
      borderColor = palette.goldPrimary.copy(alpha = 0.5f),
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Icon(
          imageVector = Icons.Filled.Info,
          contentDescription = null,
          tint = palette.goldPrimary,
          modifier = Modifier.size(20.dp),
        )
        Column {
          Text(
            text = if (isBangla) "অডিও রাউটিং তথ্য" else "AUDIO ROUTING NOTICE",
            style = MaterialTheme.typography.labelLarge,
            color = palette.goldPrimary,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text =
              if (isBangla) {
                "সাউন্ডবোর্ড ইফেক্ট লো-লেটেন্সি গেম অডিও স্ট্রিমের মাধ্যমে স্পিকার, ব্লুটুথ বা হেডসেটে বাজে। সরাসরি গেমের মাইক্রোফোনে ভয়েস পাঠাতে Shizuku বা Root মোড কানেক্ট করুন।"
              } else {
                "Soundboard effects play via Android's low-latency GAME / SONIFICATION audio stream to your active speaker, Bluetooth, or wired headset. Standard Android security prevents third-party apps from injecting internal audio directly into another app's locked VoIP mic buffer without external loopback or Shizuku/Root injection."
              },
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )
        }
      }
    }
  }
}

@Composable
private fun MixerChannelCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  value: Float,
  onValueChange: (Float) -> Unit,
  testTag: String,
) {
  val palette = LocalZonexPalette.current
  val pct = (value * 100f).roundToInt()

  ClayCard(
    modifier = Modifier.fillMaxWidth(),
    cornerRadius = 20.dp,
    elevation = 9.dp,
    baseColor = palette.surface,
    borderColor = palette.border,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(icon, contentDescription = null, tint = palette.goldPrimary, modifier = Modifier.size(20.dp))
          Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
          }
        }
        ClayPill(
          baseColor = palette.goldPrimary.copy(alpha = 0.18f),
          borderColor = palette.goldPrimary.copy(alpha = 0.45f),
          cornerRadius = 10.dp,
          elevation = 4.dp,
        ) {
          Text(
            text = "$pct%",
            style = MaterialTheme.typography.labelLarge,
            color = palette.goldPrimary,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Slider(
        value = value,
        onValueChange = onValueChange,
        colors =
          SliderDefaults.colors(
            thumbColor = palette.goldPrimary,
            activeTrackColor = palette.goldPrimary,
            inactiveTrackColor = palette.border,
          ),
        modifier = Modifier.testTag(testTag),
      )
    }
  }
}
