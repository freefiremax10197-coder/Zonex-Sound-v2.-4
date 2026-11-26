package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.shizuku.ShizukuRootState
import com.example.ui.components.ClayButton
import com.example.ui.components.ClayCard
import com.example.ui.components.ClayPill
import com.example.ui.components.claySurface
import com.example.ui.theme.LocalZonexPalette
import com.example.ui.viewmodel.AudioLabState
import kotlin.math.roundToInt

@Composable
fun AudioLabScreen(
  labState: AudioLabState,
  shizukuState: ShizukuRootState,
  isTestingSpeaker: Boolean,
  isTestingMic: Boolean,
  micLevelNormalized: Float,
  micDbLevel: Float,
  isBangla: Boolean = true,
  isFloatingOpen: Boolean = false,
  onRefreshDiagnostics: () -> Unit,
  onTestSpeaker: () -> Unit,
  onStartMicTest: () -> Unit,
  onStopMicTest: () -> Unit,
  onLaunchFloatingWindow: () -> Unit,
  onOpenShizukuDialog: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalZonexPalette.current
  val context = LocalContext.current
  var micDeniedMessage by remember { mutableStateOf<String?>(null) }

  val micPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        micDeniedMessage = null
        onStartMicTest()
      } else {
        micDeniedMessage =
          if (isBangla) {
            "মাইক্রোফোন পারমিশন দেওয়া হয়নি। লাইভ মাইক টেস্ট করতে অ্যাপ সেটিংস থেকে RECORD_AUDIO পারমিশন দিন।"
          } else {
            "Microphone permission denied. Grant RECORD_AUDIO in App Settings to test live mic input."
          }
      }
      onRefreshDiagnostics()
    }

  DisposableEffect(Unit) {
    onDispose {
      onStopMicTest()
    }
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(palette.background)
        .verticalScroll(rememberScrollState())
        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ClayPill(
          baseColor = palette.goldPrimary.copy(alpha = 0.18f),
          borderColor = palette.goldPrimary.copy(alpha = 0.45f),
          cornerRadius = 14.dp,
          elevation = 6.dp,
          modifier = Modifier.size(42.dp),
        ) {
          Icon(Icons.Filled.Science, contentDescription = null, tint = palette.goldPrimary)
        }
        Column {
          Text(
            text = if (isBangla) "অডিও ল্যাব ও ডায়াগনস্টিকস" else "Audio Lab & Diagnostics",
            style = MaterialTheme.typography.headlineSmall,
            color = palette.textPrimary,
          )
          Text(
            text =
              if (isBangla) "রিয়েল Android অডিও ও হার্ডওয়্যার টেস্ট"
              else "Real Android AudioManager & PCM Diagnostics",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )
        }
      }
      ClayPill(
        onClick = onRefreshDiagnostics,
        baseColor = palette.surfaceElevated,
        borderColor = palette.border,
        cornerRadius = 14.dp,
        modifier = Modifier.size(42.dp),
      ) {
        Icon(Icons.Filled.Refresh, contentDescription = "Refresh hardware", tint = palette.goldPrimary)
      }
    }

    // Live Speaker & Mic Interactive Diagnostics Card
    ClayCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 22.dp,
      elevation = 10.dp,
      baseColor = palette.surface,
      borderColor = palette.goldPrimary.copy(alpha = 0.55f),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = if (isBangla) "লাইভ হার্ডওয়্যার টেস্ট" else "LIVE HARDWARE TESTS",
          style = MaterialTheme.typography.labelLarge,
          color = palette.goldPrimary,
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          ClayButton(
            onClick = onTestSpeaker,
            containerColor = palette.goldPrimary,
            contentColor = palette.onGold,
            cornerRadius = 14.dp,
            modifier = Modifier.weight(1f).height(48.dp).testTag("test_speaker_btn"),
          ) {
            Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              if (isTestingSpeaker) (if (isBangla) "বাজছে..." else "PLAYING...")
              else (if (isBangla) "স্পিকার টেস্ট" else "TEST SPEAKER"),
              fontWeight = FontWeight.ExtraBold,
            )
          }

          ClayButton(
            onClick = {
              if (isTestingMic) {
                onStopMicTest()
              } else {
                val granted =
                  ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) {
                  micDeniedMessage = null
                  onStartMicTest()
                } else {
                  micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
              }
            },
            containerColor = palette.surfaceElevated,
            contentColor = if (isTestingMic) palette.danger else palette.goldPrimary,
            borderColor = if (isTestingMic) palette.danger else palette.goldPrimary.copy(alpha = 0.7f),
            cornerRadius = 14.dp,
            modifier = Modifier.weight(1f).height(48.dp).testTag("test_mic_btn"),
          ) {
            Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              if (isTestingMic) (if (isBangla) "মাইক থামাও" else "STOP MIC")
              else (if (isBangla) "মাইক টেস্ট" else "TEST MIC"),
              fontWeight = FontWeight.Bold,
            )
          }
        }

        if (!micDeniedMessage.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = micDeniedMessage ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = palette.danger,
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        val animatedLevel by animateFloatAsState(targetValue = micLevelNormalized, label = "mic_lvl")
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text =
              if (isTestingMic) (if (isBangla) "লাইভ মাইক ইনপুট লেভেল" else "Live Microphone Input Level")
              else (if (isBangla) "মাইক ইনপুট দেখতে 'মাইক টেস্ট' ট্যাপ করুন" else "Tap TEST MIC to measure real audio input"),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )
          Text(
            text = "${micDbLevel.roundToInt()} dBFS",
            style = MaterialTheme.typography.labelMedium,
            color = if (isTestingMic) palette.success else palette.textSecondary,
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
          progress = { animatedLevel },
          modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
          color =
            when {
              animatedLevel > 0.8f -> palette.danger
              animatedLevel > 0.45f -> palette.goldPrimary
              else -> palette.success
            },
          trackColor = palette.surfaceElevated,
        )
      }
    }

    // Hardware Compatibility Checks
    Text(
      text = if (isBangla) "হার্ডওয়্যার কমপ্যাটিবিলিটি" else "HARDWARE COMPATIBILITY",
      style = MaterialTheme.typography.labelLarge,
      color = palette.goldPrimary,
    )
    labState.compatibilityChecks.forEach { item ->
      val icon =
        when (item.id) {
          "speaker" -> Icons.Filled.Speaker
          "mic" -> Icons.Filled.Mic
          "bluetooth" -> Icons.Filled.BluetoothAudio
          else -> Icons.Filled.Headset
        }
      ClayCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        elevation = 8.dp,
        baseColor = palette.surface,
        borderColor = palette.border,
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.size(40.dp)
                .claySurface(
                  baseColor =
                    if (item.isAvailable) palette.success.copy(alpha = 0.16f)
                    else palette.surfaceElevated,
                  cornerRadius = 20.dp,
                  elevation = 4.dp,
                ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              icon,
              contentDescription = null,
              tint = if (item.isAvailable) palette.success else palette.textSecondary,
              modifier = Modifier.size(20.dp),
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Text(item.detail, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
          }
          ClayPill(
            baseColor =
              if (item.isAvailable) palette.success.copy(alpha = 0.18f)
              else palette.surfaceElevated,
            borderColor = if (item.isAvailable) palette.success.copy(alpha = 0.5f) else palette.border,
            cornerRadius = 10.dp,
            elevation = 3.dp,
          ) {
            Text(
              text = item.statusText,
              style = MaterialTheme.typography.labelSmall,
              color = if (item.isAvailable) palette.success else palette.textSecondary,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
      }
    }

    // Game Voice-Chat Mixing Status Card
    labState.gameMixingStatus?.let { gm ->
      ClayCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        elevation = 8.dp,
        baseColor = palette.surface,
        borderColor = palette.border,
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Filled.SportsEsports, contentDescription = null, tint = palette.goldPrimary)
            Text(
              text = if (isBangla) "গেম ভয়েস-চ্যাট মিক্সিং স্ট্যাটাস" else "Game Voice-Chat Mixing Status",
              style = MaterialTheme.typography.titleMedium,
              color = palette.textPrimary,
            )
          }
          StatusLineRow(if (isBangla) "অডিও মোড" else "Audio Mode", gm.audioModeLabel)
          StatusLineRow(if (isBangla) "সক্রিয় আউটপুট" else "Active Output", gm.activeOutputLabel)
          StatusLineRow(
            if (isBangla) "লো-লেটেন্সি হার্ডওয়্যার" else "Low-Latency Hardware",
            if (gm.lowLatencySupported) "SUPPORTED" else "STANDARD",
            valueColor = if (gm.lowLatencySupported) palette.success else palette.textSecondary,
          )
          StatusLineRow(
            if (isBangla) "প্রো অডিও ফ্ল্যাগ" else "Pro Audio Flag",
            if (gm.proAudioSupported) "SUPPORTED" else "STANDARD",
            valueColor = if (gm.proAudioSupported) palette.success else palette.textSecondary,
          )
        }
      }
    }

    // Floating Window Support Card
    ClayCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 20.dp,
      elevation = 8.dp,
      baseColor = palette.surface,
      borderColor = if (isFloatingOpen) palette.success else palette.border,
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          Icons.Filled.Layers,
          contentDescription = null,
          tint = if (isFloatingOpen || labState.hasOverlayPermission) palette.success else palette.goldPrimary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            if (isBangla) "ফ্লোটিং উইন্ডো সাপোর্ট" else "Floating Window Support",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
          )
          Text(
            when {
              isFloatingOpen ->
                if (isBangla) "ফ্লোটিং উইন্ডো এখন চালু আছে" else "Floating window is currently open"
              labState.hasOverlayPermission ->
                if (isBangla) "সিস্টেম ওভারলে পারমিশন দেওয়া আছে" else "System overlay permission granted"
              else ->
                if (isBangla) "চালু করতে Shizuku বা Root দরকার" else "Requires Shizuku or Root access to float"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (isFloatingOpen) palette.success else palette.textSecondary,
          )
        }
        ClayButton(
          onClick = onLaunchFloatingWindow,
          containerColor = if (isFloatingOpen) palette.danger.copy(alpha = 0.16f) else palette.surfaceElevated,
          contentColor = if (isFloatingOpen) palette.danger else palette.goldPrimary,
          borderColor = if (isFloatingOpen) palette.danger else palette.goldPrimary.copy(alpha = 0.7f),
          cornerRadius = 12.dp,
          elevation = 5.dp,
        ) {
          Text(
            if (isFloatingOpen) (if (isBangla) "বন্ধ করো" else "CLOSE")
            else (if (isBangla) "চালু করো" else "FLOAT"),
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    // Shizuku / Root Mic Transfer Card
    ClayCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 20.dp,
      elevation = 8.dp,
      baseColor = palette.surface,
      borderColor = if (shizukuState.isInjectionActive) palette.success else palette.goldPrimary.copy(alpha = 0.55f),
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          Icons.Filled.Security,
          contentDescription = null,
          tint = if (shizukuState.isInjectionActive) palette.success else palette.goldPrimary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            if (isBangla) "Shizuku / Root মাইক ট্রান্সফার" else "Shizuku / Root Mic Transfer",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
          )
          Text(
            shizukuState.statusMessage,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )
        }
        ClayButton(
          onClick = onOpenShizukuDialog,
          containerColor = palette.surfaceElevated,
          contentColor = palette.goldPrimary,
          borderColor = palette.goldPrimary.copy(alpha = 0.7f),
          cornerRadius = 12.dp,
          elevation = 5.dp,
        ) {
          Text(if (isBangla) "খুলুন" else "OPEN", fontWeight = FontWeight.Bold)
        }
      }
    }

    // Real Detected Audio Devices List
    Text(
      text =
        if (isBangla) "শনাক্তকৃত অডিও ডিভাইস (${labState.detectedDevices.size})"
        else "DETECTED AUDIO DEVICES (${labState.detectedDevices.size})",
      style = MaterialTheme.typography.labelLarge,
      color = palette.goldPrimary,
    )
    labState.detectedDevices.forEach { dev ->
      ClayCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        elevation = 6.dp,
        baseColor = palette.surface,
        borderColor = palette.border,
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${dev.name} (${dev.typeLabel})",
              style = MaterialTheme.typography.titleSmall,
              color = palette.textPrimary,
            )
            Text(
              text = "${dev.direction} • Sample Rates: ${dev.sampleRates}",
              style = MaterialTheme.typography.bodySmall,
              color = palette.textSecondary,
            )
          }
          Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = if (dev.isWireless) palette.info else palette.success,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun StatusLineRow(
  label: String,
  value: String,
  valueColor: Color = LocalZonexPalette.current.textPrimary,
) {
  val palette = LocalZonexPalette.current
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
    Text(value, style = MaterialTheme.typography.labelMedium, color = valueColor)
  }
}
