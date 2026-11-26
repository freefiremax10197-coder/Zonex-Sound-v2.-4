package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Base64
import coil.Coil
import coil.ImageLoader
import coil.map.Mapper
import coil.request.Options
import com.example.security.ZonexSecurityGuard
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class ZonexApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    ZonexSecurityGuard.verifyIntegrity(this)
    initFirebase()
    initCoil()
    createNotificationChannels()
  }

  private fun initFirebase() {
    if (FirebaseApp.getApps(this).isEmpty()) {
      val options =
        FirebaseOptions.Builder()
          .setApiKey(ZonexSecurityGuard.firebaseApiKey)
          .setApplicationId(ZonexSecurityGuard.firebaseAppId)
          .setProjectId(ZonexSecurityGuard.firebaseProjectId)
          .setDatabaseUrl(ZonexSecurityGuard.firebaseDbUrl)
          .setStorageBucket(ZonexSecurityGuard.firebaseStorageBucket)
          .setGcmSenderId(ZonexSecurityGuard.firebaseSenderId)
          .build()
      FirebaseApp.initializeApp(this, options)
    }
  }

  private fun initCoil() {
    Coil.setImageLoader(
      ImageLoader.Builder(this)
        .components {
          add(
            Mapper<String, ByteArray> { data: String, _: Options ->
              if (!data.startsWith("data:image/", ignoreCase = true)) null
              else {
                val b64 = data.substringAfter("base64,", "")
                if (b64.isBlank()) null
                else runCatching { Base64.decode(b64, Base64.DEFAULT) }.getOrNull()
              }
            }
          )
        }
        .build()
    )
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(NotificationManager::class.java) ?: return
      val playbackChannel =
        NotificationChannel(
          CHANNEL_PLAYBACK,
          "Zonex Sound Controls",
          NotificationManager.IMPORTANCE_LOW,
        ).apply {
          description = "Quick notification bar controls and floating overlay status"
          setShowBadge(false)
        }
      manager.createNotificationChannel(playbackChannel)
    }
  }

  companion object {
    const val CHANNEL_PLAYBACK = "zonex_playback_controls"
  }
}
