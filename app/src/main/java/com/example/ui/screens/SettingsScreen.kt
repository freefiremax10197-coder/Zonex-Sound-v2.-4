package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AppThemeStyle
import com.example.data.model.DeveloperConfig
import com.example.data.model.UserProfile
import com.example.data.model.UserSettingsState
import com.example.security.ZonexSecurityGuard
import com.example.service.ZonexNotificationHelper
import com.example.ui.components.ClayButton
import com.example.ui.components.ClayCard
import com.example.ui.components.claySurface
import com.example.ui.components.parseHexColorSafe
import com.example.ui.theme.LocalZonexPalette
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  user: UserProfile?,
  settings: UserSettingsState,
  developer: DeveloperConfig,
  onLogout: () -> Unit,
  onSelectTheme: (AppThemeStyle) -> Unit,
  onMasterVolumeChange: (Float) -> Unit,
  onToggleFreeFireBoost: (Boolean) -> Unit,
  onToggleFloatingBubble: (Boolean) -> Unit,
  onToggleQuickNotification: (Boolean) -> Unit,
  onToggleHapticFeedback: (Boolean) -> Unit,
  onToggleSoundAnimations: (Boolean) -> Unit,
  onOpenAudioLab: () -> Unit,
  onOpenShizukuDialog: () -> Unit,
  onRestoreDefaultLibrary: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalZonexPalette.current
  val context = LocalContext.current
  val isBangla = settings.isBanglaUi

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      onToggleQuickNotification(granted)
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
    Text(
      text = if (isBangla) "সেটিংস" else "Settings",
      style = MaterialTheme.typography.headlineSmall,
      color = palette.textPrimary,
    )

    // User Profile & Logout Card
    ClayCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 22.dp,
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
            Modifier.size(48.dp)
              .claySurface(
                baseColor = palette.goldPrimary.copy(alpha = 0.2f),
                cornerRadius = 24.dp,
                elevation = 5.dp,
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(Icons.Filled.Person, contentDescription = null, tint = palette.goldPrimary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = user?.displayName?.ifBlank { "Zonex User" } ?: "Zonex User",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
          )
          Text(
            text = user?.email.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
          )
        }
        ClayButton(
          onClick = onLogout,
          containerColor = palette.surfaceElevated,
          contentColor = palette.danger,
          borderColor = palette.danger.copy(alpha = 0.7f),
          cornerRadius = 14.dp,
          elevation = 5.dp,
          modifier = Modifier.testTag("settings_logout_btn"),
        ) {
          Icon(
            Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = "Logout",
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (isBangla) "লগআউট" else "Logout", fontWeight = FontWeight.Bold)
        }
      }
    }

    // App Background Theme Selector
    Text(
      text = if (isBangla) "অ্যাপ ব্যাকগ্রাউন্ড থিম" else "APP BACKGROUND THEME",
      style = MaterialTheme.typography.labelLarge,
      color = palette.goldPrimary,
    )
    ClayCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 22.dp,
      elevation = 9.dp,
      baseColor = palette.surface,
      borderColor = palette.border,
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppThemeStyle.entries.forEach { style ->
          val selected = settings.themeStyle == style
          val themeTitle =
            if (!isBangla) style.displayName
            else when (style) {
              AppThemeStyle.PURE_BLACK -> "ক্লে অবসিডিয়ান (ডার্ক গোল্ড)"
              AppThemeStyle.GOLDEN_ROBOTICS -> "ক্লে অ্যাম্বার (সোনালী)"
              AppThemeStyle.CYBER_TITANIUM -> "ক্লে টাইটানিয়াম (সাইবার)"
            }
          val themeSubtitle =
            if (!isBangla) style.subtitle
            else when (style) {
              AppThemeStyle.PURE_BLACK -> "গাঢ় ম্যাট স্লেট ও গোল্ড ফিনিশ"
              AppThemeStyle.GOLDEN_ROBOTICS -> "উষ্ণ ব্রোঞ্জ ও সাইবার গোল্ড"
              AppThemeStyle.CYBER_TITANIUM -> "মসৃণ সাইবার স্লেট ও ইলেকট্রিক গোল্ড"
            }
          ClayCard(
            onClick = { onSelectTheme(style) },
            modifier = Modifier.fillMaxWidth().testTag("theme_option_${style.name}"),
            cornerRadius = 16.dp,
            elevation = if (selected) 7.dp else 4.dp,
            baseColor = if (selected) palette.surfaceHighlight else palette.surfaceElevated,
            borderColor = if (selected) palette.goldPrimary else palette.border,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  Icons.Filled.Palette,
                  contentDescription = null,
                  tint = if (selected) palette.goldPrimary else palette.textSecondary,
                  modifier = Modifier.size(18.dp),
                )
                Column {
                  Text(
                    themeTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) palette.goldBright else palette.textPrimary,
                  )
                  Text(
                    themeSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                  )
                }
              }
              if (selected) {
                Icon(
                  Icons.Filled.CheckCircle,
                  contentDescription = "Selected",
                  tint = palette.goldPrimary,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }
        }
      }
    }

    // Master Soundboard Volume Slider
    ClayCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 22.dp,
      elevation = 9.dp,
      baseColor = palette.surface,
      borderColor = palette.border,
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = palette.goldPrimary)
            Text(
              if (isBangla) "মাস্টার সাউন্ডবোর্ড ভলিউম" else "Master Soundboard Volume",
              style = MaterialTheme.typography.titleMedium,
              color = palette.textPrimary,
            )
          }
          Text(
            "${(settings.masterVolume * 100f).roundToInt()}%",
            style = MaterialTheme.typography.labelLarge,
            color = palette.goldPrimary,
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Slider(
          value = settings.masterVolume,
          onValueChange = onMasterVolumeChange,
          colors =
            SliderDefaults.colors(
              thumbColor = palette.goldPrimary,
              activeTrackColor = palette.goldPrimary,
              inactiveTrackColor = palette.border,
            ),
        )
      }
    }

    // Gaming & Controls Toggles
    Text(
      text = if (isBangla) "গেমিং ও কন্ট্রোলস" else "GAMING & CONTROLS",
      style = MaterialTheme.typography.labelLarge,
      color = palette.goldPrimary,
    )
    ClayCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 22.dp,
      elevation = 9.dp,
      baseColor = palette.surface,
      borderColor = palette.border,
    ) {
      Column(modifier = Modifier.padding(vertical = 6.dp)) {
        SettingsSwitchRow(
          title = if (isBangla) "Free Fire ভয়েস ও স্পিকার বুস্ট" else "Free Fire Voice & Speaker Boost",
          subtitle =
            if (isBangla) "পরিষ্কার ইন-গেম সাউন্ডের জন্য +6dB লাউডনেস বুস্ট দেয়"
            else "Applies +6dB loudness boost for clear in-game playback",
          checked = settings.freeFireVoiceBoost,
          onCheckedChange = onToggleFreeFireBoost,
        )
        SettingsSwitchRow(
          title = if (isBangla) "ফ্লোটিং সাউন্ডবোর্ড বাবল" else "Floating Soundboard Bubble",
          subtitle =
            if (isBangla) "Shizuku বা Root কানেক্ট থাকলে ভাসমান বাবল চালু রাখে"
            else "Keep draggable ZONEX GOLD widget active (requires Shizuku or Root)",
          checked = settings.floatingBubbleEnabled,
          onCheckedChange = onToggleFloatingBubble,
        )
        SettingsSwitchRow(
          title = if (isBangla) "দ্রুত নোটিফিকেশন বার কন্ট্রোল" else "Quick Notification Bar Control",
          subtitle =
            if (isBangla) "নোটিফিকেশন বারে STOP ALL কন্ট্রোল দেখাবে"
            else "Persistent STOP ALL control in notification shade",
          checked = settings.quickNotificationControl,
          onCheckedChange = { wantEnable ->
            if (wantEnable &&
              Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
              !ZonexNotificationHelper.hasNotificationPermission(context)
            ) {
              notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              onToggleQuickNotification(wantEnable)
            }
          },
        )
        SettingsSwitchRow(
          title = if (isBangla) "হ্যাপটিক ভাইব্রেশন ফিডব্যাক" else "Haptic Vibration Feedback",
          subtitle =
            if (isBangla) "সাউন্ড বাটনে ট্যাপ করলে হালকা ভাইব্রেশন হবে"
            else "Tactile pulse on soundboard triggers",
          checked = settings.hapticFeedback,
          onCheckedChange = onToggleHapticFeedback,
        )
        SettingsSwitchRow(
          title = if (isBangla) "সাউন্ড বাটন অ্যানিমেশন" else "Sound Button Animations",
          subtitle =
            if (isBangla) "চলমান সাউন্ড কার্ডে ইকুয়ালাইজার ও অ্যানিমেশন দেখাবে"
            else "Spring scale & equalizer bars on active pads",
          checked = settings.soundAnimations,
          onCheckedChange = onToggleSoundAnimations,
        )
      }
    }

    // Shortcuts
    Text(
      text = if (isBangla) "ইউটিলিটি ও শর্টকাট" else "UTILITIES & SHORTCUTS",
      style = MaterialTheme.typography.labelLarge,
      color = palette.goldPrimary,
    )
    ClayCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 22.dp,
      elevation = 9.dp,
      baseColor = palette.surface,
      borderColor = palette.border,
    ) {
      Column {
        ShortcutRow(
          icon = Icons.Filled.Security,
          title = if (isBangla) "Shizuku / Root মাইক ট্রান্সফার অপশন" else "Shizuku / Root Mic Transfer Options",
          onClick = onOpenShizukuDialog,
        )
        ShortcutRow(
          icon = Icons.Filled.Science,
          title = if (isBangla) "অডিও কমপ্যাটিবিলিটি ও হার্ডওয়্যার ল্যাব" else "Audio Compatibility & Hardware Lab",
          onClick = onOpenAudioLab,
        )
        ShortcutRow(
          icon = Icons.Filled.RestartAlt,
          title = if (isBangla) "ডিফল্ট সাউন্ড লাইব্রেরি রিস্টোর করুন" else "Restore Default Sound Library",
          onClick = onRestoreDefaultLibrary,
        )
      }
    }

    // Bottom ABOUT & DEVELOPER section synced live from Firebase config/developer
    Text(
      text = if (isBangla) "ডেভেলপার ও অ্যাপ তথ্য" else "ABOUT & DEVELOPER",
      style = MaterialTheme.typography.labelLarge,
      color = palette.goldPrimary,
    )
    ClayCard(
      modifier = Modifier.fillMaxWidth().testTag("about_developer_card"),
      cornerRadius = 22.dp,
      elevation = 10.dp,
      baseColor = palette.surface,
      borderColor = palette.goldPrimary.copy(alpha = 0.55f),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Box(
            modifier =
              Modifier.size(54.dp)
                .claySurface(baseColor = palette.surfaceElevated, cornerRadius = 27.dp, elevation = 6.dp),
            contentAlignment = Alignment.Center,
          ) {
            if (developer.logoUrl.isNotBlank()) {
              AsyncImage(
                model = developer.logoUrl,
                contentDescription = developer.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
              )
            } else {
              Icon(Icons.Filled.Person, contentDescription = null, tint = palette.goldPrimary)
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = developer.name,
              style = MaterialTheme.typography.titleLarge,
              color = palette.goldBright,
            )
            Text(
              text = "Zonex Sound • v${com.example.BuildConfig.VERSION_NAME} • Live Admin Profile",
              style = MaterialTheme.typography.labelSmall,
              color = palette.textSecondary,
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = developer.bio,
          style = MaterialTheme.typography.bodyMedium,
          color = palette.textSecondary,
        )

        if (developer.buttons.isNotEmpty()) {
          Spacer(modifier = Modifier.height(14.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            developer.buttons.forEach { btn ->
              val btnColor = parseHexColorSafe(btn.color, palette.goldPrimary)
              ClayButton(
                onClick = {
                  val safeUri = ZonexSecurityGuard.sanitizeExternalUrl(btn.url) ?: return@ClayButton
                  runCatching {
                    context.startActivity(
                      Intent(Intent.ACTION_VIEW, Uri.parse(safeUri))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                  }
                },
                containerColor = btnColor,
                contentColor = Color.White,
                cornerRadius = 14.dp,
                elevation = 6.dp,
              ) {
                Text(btn.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  val palette = LocalZonexPalette.current
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable { onCheckedChange(!checked) }
        .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors =
        SwitchDefaults.colors(
          checkedThumbColor = palette.onGold,
          checkedTrackColor = palette.goldPrimary,
          uncheckedThumbColor = palette.textSecondary,
          uncheckedTrackColor = palette.surfaceElevated,
        ),
    )
  }
}

@Composable
private fun ShortcutRow(
  icon: ImageVector,
  title: String,
  onClick: () -> Unit,
) {
  val palette = LocalZonexPalette.current
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(icon, contentDescription = null, tint = palette.goldPrimary, modifier = Modifier.size(20.dp))
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      color = palette.textPrimary,
      modifier = Modifier.weight(1f),
    )
    Icon(
      Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = palette.textSecondary,
    )
  }
}
