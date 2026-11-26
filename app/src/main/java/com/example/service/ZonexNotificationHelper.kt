package com.example.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ZonexApplication

object ZonexNotificationHelper {

  const val NOTIFICATION_ID_QUICK = 1001
  const val NOTIFICATION_ID_OVERLAY = 1002

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun buildControlNotification(
    context: Context,
    title: String,
    subtitle: String,
  ): Notification {
    val openIntent =
      PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val stopIntent =
      PendingIntent.getBroadcast(
        context,
        1,
        Intent(context, ZonexActionReceiver::class.java).apply {
          action = ZonexActionReceiver.ACTION_STOP_ALL
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    return NotificationCompat.Builder(context, ZonexApplication.CHANNEL_PLAYBACK)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(subtitle)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setContentIntent(openIntent)
      .addAction(android.R.drawable.ic_media_pause, "STOP ALL", stopIntent)
      .build()
  }

  fun showOrUpdateQuickControl(context: Context, enabled: Boolean, currentTitle: String?) {
    if (!enabled) {
      NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_QUICK)
      return
    }
    if (!hasNotificationPermission(context)) return
    val title = "Zonex Sound"
    val sub =
      if (!currentTitle.isNullOrBlank()) "Playing: $currentTitle • Tap STOP ALL to silence"
      else "Quick Soundboard Control Active"
    runCatching {
      NotificationManagerCompat.from(context)
        .notify(NOTIFICATION_ID_QUICK, buildControlNotification(context, title, sub))
    }
  }
}
