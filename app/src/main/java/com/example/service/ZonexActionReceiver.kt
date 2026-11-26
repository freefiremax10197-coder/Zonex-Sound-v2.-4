package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.audio.ZonexAudioEngine

class ZonexActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    when (intent?.action) {
      ACTION_STOP_ALL -> ZonexAudioEngine.stopAll()
      ACTION_CLOSE_OVERLAY -> context.stopService(Intent(context, FloatingSoundboardService::class.java))
    }
  }

  companion object {
    const val ACTION_STOP_ALL = "com.example.ACTION_STOP_ALL"
    const val ACTION_CLOSE_OVERLAY = "com.example.ACTION_CLOSE_OVERLAY"
  }
}
