package com.example.data.model

import android.net.Uri

enum class AppThemeStyle(val displayName: String, val subtitle: String) {
  PURE_BLACK("Clay Obsidian", "Sculpted Matte Slate & Gold"),
  GOLDEN_ROBOTICS("Clay Amber", "Warm Matte Bronze & Cyber Gold"),
  CYBER_TITANIUM("Clay Titanium", "Tactile Cyber Slate & Electric Gold"),
}

enum class SoundVisibility(val value: String) {
  PUBLIC("public"),
  PRIVATE("private"),
}

enum class MainTab(val route: String, val label: String) {
  SOUNDS("sounds", "Sounds"),
  MY_SOUNDS("my_sounds", "My Sounds"),
  MIXER("mixer", "Mixer"),
  AUDIO_LAB("audio_lab", "Audio Lab"),
  SETTINGS("settings", "Settings");

  fun localizedLabel(isBangla: Boolean): String =
    if (!isBangla) label
    else when (this) {
      SOUNDS -> "সাউন্ড"
      MY_SOUNDS -> "মাই সাউন্ড"
      MIXER -> "মিক্সার"
      AUDIO_LAB -> "অডিও ল্যাব"
      SETTINGS -> "সেটিংস"
    }
}

data class SoundItem(
  val id: String = "",
  val title: String = "",
  val category: String = "Trending",
  val audioUrl: String = "",
  val thumbnailUrl: String = "",
  val emoji: String = "🔊",
  val defaultVolume: Int = 100,
  val likes: Long = 0L,
  val createdBy: String = "",
  val createdAt: Long = 0L,
  val visibility: String = "public",
  val isDownloaded: Boolean = false,
)

data class AdminNotice(
  val bannerActive: Boolean = false,
  val bannerText: String = "",
  val popupActive: Boolean = false,
  val popupTitle: String = "",
  val popupMessage: String = "",
)

data class DeveloperButton(
  val label: String = "",
  val url: String = "",
  val color: String = "#F5B82E",
)

data class DeveloperConfig(
  val name: String = "Zonex Official",
  val logoUrl: String = "",
  val bio: String = "Pro gaming soundboard & low-latency audio utility built for creators and gamers.",
  val buttons: List<DeveloperButton> = emptyList(),
)

data class AppConfig(
  val logoUrl: String = "",
  val floatingBrandName: String = "ZONEX",
)

data class AppUpdateConfig(
  val enabled: Boolean = false,
  val latestVersionName: String = "2.4",
  val latestVersionCode: Int = 24,
  val targetVersionName: String = "",
  val forceUpdate: Boolean = false,
  val title: String = "নতুন আপডেট এসেছে!",
  val message: String = "",
  val downloadUrl: String = "",
)

data class MaintenanceConfig(
  val enabled: Boolean = false,
  val title: String = "কাজ চলতেছে",
  val message: String = "অ্যাপের উন্নয়ন ও সার্ভার মেইনটেন্যান্সের কাজ চলছে। কিছুক্ষণ পর আবার চেষ্টা করুন।",
)

data class UserProfile(
  val uid: String = "",
  val displayName: String = "",
  val email: String = "",
)

data class UserSettingsState(
  val themeStyle: AppThemeStyle = AppThemeStyle.PURE_BLACK,
  val keepLoggedIn: Boolean = true,
  val masterVolume: Float = 0.9f,
  val sfxChannelVolume: Float = 1.0f,
  val mediaChannelVolume: Float = 0.85f,
  val freeFireVoiceBoost: Boolean = false,
  val floatingBubbleEnabled: Boolean = false,
  val quickNotificationControl: Boolean = false,
  val hapticFeedback: Boolean = true,
  val soundAnimations: Boolean = true,
  val dismissedBannerText: String = "",
  val cartesiaApiKey: String = "",
  val cartesiaApiKeys: List<String> = emptyList(),
  val activeCartesiaKeyIndex: Int = 0,
  val selectedCartesiaVoiceId: String = "char_omor_on_fire",
  val customTtsVoicesJson: String = "",
  val ttsVoiceOrderIds: List<String> = emptyList(),
  val isBanglaUi: Boolean = true,
  val micInjectionEnabled: Boolean = false,
)

data class CompatibilityCheckItem(
  val id: String,
  val title: String,
  val statusText: String,
  val isAvailable: Boolean,
  val detail: String,
)

data class AudioDeviceDisplay(
  val id: Int,
  val name: String,
  val typeLabel: String,
  val direction: String,
  val sampleRates: String,
  val isWireless: Boolean,
)

data class GameMixingStatus(
  val audioModeLabel: String,
  val lowLatencySupported: Boolean,
  val proAudioSupported: Boolean,
  val activeOutputLabel: String,
  val speakerphoneOn: Boolean,
  val bluetoothScoOn: Boolean,
)

data class PlaybackState(
  val currentSound: SoundItem? = null,
  val isPlaying: Boolean = false,
  val isBuffering: Boolean = false,
  val positionMs: Long = 0L,
  val durationMs: Long = 0L,
  val errorMessage: String? = null,
)

data class UploadSoundDraft(
  val title: String = "",
  val category: String = "Trending",
  val emoji: String = "🔥",
  val audioUri: Uri? = null,
  val audioFileName: String = "",
  val directAudioUrl: String = "",
  val useDirectUrl: Boolean = false,
  val thumbnailUri: Uri? = null,
  val defaultVolume: Int = 90,
  val addToFavorites: Boolean = true,
  val visibility: SoundVisibility = SoundVisibility.PUBLIC,
)

data class KeySocialLink(
  val platform: String = "Telegram",
  val label: String = "",
  val url: String = "",
  val color: String = "#0EA5E9",
)

data class KeyGateConfig(
  val requireKeyAfterLogin: Boolean = true,
  val getKeyUrl: String = "",
  val socialLinks: List<KeySocialLink> = emptyList(),
)

data class AccessKeyState(
  val isChecking: Boolean = false,
  val isActivated: Boolean = false,
  val keyCode: String = "",
  val expiresAt: Long = 0L,
  val remainingText: String = "",
  val expiryDateText: String = "",
  val isActivating: Boolean = false,
  val errorMessage: String? = null,
)

