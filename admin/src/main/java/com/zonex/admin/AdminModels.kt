package com.zonex.admin

import android.net.Uri

enum class AdminTab(val label: String) {
  SOUNDS("Sounds"),
  KEYS("Keys"),
  NOTICE("Notice"),
  BRANDING("Branding"),
  USERS("Users"),
}

data class AdminSound(
  val id: String = "",
  val title: String = "",
  val category: String = "Trending",
  val audioUrl: String = "",
  val thumbnailUrl: String = "",
  val emoji: String = "🔥",
  val defaultVolume: Int = 100,
  val likes: Long = 0L,
  val createdBy: String = "",
  val createdAt: Long = 0L,
)

data class AdminSoundDraft(
  val editingId: String? = null,
  val title: String = "",
  val category: String = "Trending",
  val emoji: String = "🔥",
  val defaultVolume: Int = 100,
  val useDirectUrl: Boolean = false,
  val directAudioUrl: String = "",
  val existingAudioUrl: String = "",
  val existingThumbnailUrl: String = "",
  val audioUri: Uri? = null,
  val audioFileName: String = "",
  val thumbnailUri: Uri? = null,
)

data class AdminNotice(
  val bannerActive: Boolean = false,
  val bannerText: String = "",
  val popupActive: Boolean = false,
  val popupTitle: String = "",
  val popupMessage: String = "",
)

data class AdminDevButton(
  val label: String = "",
  val url: String = "",
  val color: String = "#F5B82E",
)

data class AdminDeveloperConfig(
  val name: String = "Zonex Official",
  val logoUrl: String = "",
  val bio: String = "",
  val buttons: List<AdminDevButton> = emptyList(),
)

data class AdminAppConfig(
  val logoUrl: String = "",
  val floatingBrandName: String = "ZONEX",
)

data class AdminUserSoundItem(
  val id: String = "",
  val title: String = "",
  val category: String = "",
  val audioUrl: String = "",
  val isPublic: Boolean = false,
  val createdAt: Long = 0L,
)

data class AdminUserRecord(
  val uid: String = "",
  val displayName: String = "",
  val email: String = "",
  val privateCount: Int = 0,
  val publicUploadCount: Int = 0,
  val downloadCount: Int = 0,
  val favoriteCount: Int = 0,
  val hasSeenOnboarding: Boolean = false,
  val isAdminAccount: Boolean = false,
  val uploadedSounds: List<AdminUserSoundItem> = emptyList(),
) {
  val totalAddedCount: Int
    get() = privateCount + publicUploadCount
}

data class AdminKeySocialLink(
  val platform: String = "Telegram",
  val label: String = "",
  val url: String = "",
  val color: String = "#0EA5E9",
)

data class AdminKeyGateConfig(
  val requireKeyAfterLogin: Boolean = true,
  val getKeyUrl: String = "",
  val socialLinks: List<AdminKeySocialLink> = emptyList(),
)

data class AdminAccessKey(
  val code: String = "",
  val durationHours: Int = 24,
  val createdAt: Long = 0L,
  val activatedAt: Long = 0L,
  val expiresAt: Long = 0L,
  val boundUid: String = "",
  val boundDeviceId: String = "",
  val isRevoked: Boolean = false,
)

data class AdminAppUpdateConfig(
  val enabled: Boolean = false,
  val latestVersionName: String = "2.4",
  val latestVersionCode: Int = 24,
  val targetVersionName: String = "",
  val forceUpdate: Boolean = false,
  val title: String = "নতুন আপডেট এসেছে!",
  val message: String = "",
  val downloadUrl: String = "",
)

data class AdminMaintenanceConfig(
  val enabled: Boolean = false,
  val title: String = "কাজ চলতেছে",
  val message: String = "অ্যাপের উন্নয়ন ও সার্ভার মেইনটেন্যান্সের কাজ চলছে। কিছুক্ষণ পর আবার চেষ্টা করুন।",
)

data class AdminTtsVoice(
  val id: String = "",
  val name: String = "",
  val cartesiaVoiceId: String = "",
  val language: String = "Bangla / Multi",
  val avatarUrl: String = "",
  val badgeColorHex: String = "#16A34A",
  val sortOrder: Int = 0,
)

