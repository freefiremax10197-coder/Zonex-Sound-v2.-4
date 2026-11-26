package com.zonex.admin

import android.app.Application
import android.util.Base64
import coil.Coil
import coil.ImageLoader
import coil.map.Mapper
import coil.request.Options
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class AdminApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    AdminSecurityGuard.verifyIntegrity(this)
    if (FirebaseApp.getApps(this).isEmpty()) {
      val options =
        FirebaseOptions.Builder()
          .setApiKey(AdminSecurityGuard.firebaseApiKey)
          .setApplicationId(AdminSecurityGuard.firebaseAppId)
          .setProjectId(AdminSecurityGuard.firebaseProjectId)
          .setDatabaseUrl(AdminSecurityGuard.firebaseDbUrl)
          .setStorageBucket(AdminSecurityGuard.firebaseStorageBucket)
          .setGcmSenderId(AdminSecurityGuard.firebaseSenderId)
          .build()
      FirebaseApp.initializeApp(this, options)
    }

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
}
