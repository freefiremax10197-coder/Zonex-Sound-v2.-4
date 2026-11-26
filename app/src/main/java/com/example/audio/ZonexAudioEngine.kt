package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.Process
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import androidx.core.content.ContextCompat
import com.example.data.model.AudioDeviceDisplay
import com.example.data.model.CompatibilityCheckItem
import com.example.data.model.GameMixingStatus
import com.example.data.model.PlaybackState
import com.example.data.model.SoundItem
import java.io.File
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ZonexAudioEngine {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private var mediaPlayer: MediaPlayer? = null
  private var loudnessEnhancer: LoudnessEnhancer? = null
  private var dynamicsProcessing: DynamicsProcessing? = null
  private var playGeneration: Long = 0L
  private var progressJob: Job? = null
  private var testToneJob: Job? = null
  private var micTestJob: Job? = null

  private var masterVolume: Float = 0.9f
  private var sfxVolume: Float = 1.0f
  private var voiceBoostEnabled: Boolean = false
  private var micTransferActive: Boolean = false

  private val _playbackState = MutableStateFlow(PlaybackState())
  val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

  private val _isTestingSpeaker = MutableStateFlow(false)
  val isTestingSpeaker: StateFlow<Boolean> = _isTestingSpeaker.asStateFlow()

  private val _isTestingMic = MutableStateFlow(false)
  val isTestingMic: StateFlow<Boolean> = _isTestingMic.asStateFlow()

  private val _micLevelNormalized = MutableStateFlow(0f)
  val micLevelNormalized: StateFlow<Float> = _micLevelNormalized.asStateFlow()

  private val _micDbLevel = MutableStateFlow(-60f)
  val micDbLevel: StateFlow<Float> = _micDbLevel.asStateFlow()

  fun updateMixerVolumes(
    master: Float,
    sfx: Float,
    voiceBoost: Boolean,
    micTransfer: Boolean = micTransferActive,
  ) {
    masterVolume = master.coerceIn(0f, 1f)
    sfxVolume = sfx.coerceIn(0f, 1f)
    voiceBoostEnabled = voiceBoost
    micTransferActive = micTransfer
    applyLiveVolume()
  }

  private fun currentLoudnessGainMb(): Int =
    when {
      micTransferActive && voiceBoostEnabled -> 350 // +3.5 dB clean boost with limiter
      micTransferActive -> 200                      // +2.0 dB clean headroom boost
      voiceBoostEnabled -> 300                      // +3.0 dB Free Fire boost
      else -> 0
    }

  private fun applyLiveVolume() {
    val player = mediaPlayer ?: return
    val soundVol = (_playbackState.value.currentSound?.defaultVolume ?: 100) / 100f
    val effective = (masterVolume * sfxVolume * soundVol).coerceIn(0f, 1f)
    runCatching { player.setVolume(effective, effective) }
    val gainMb = currentLoudnessGainMb()
    runCatching {
      loudnessEnhancer?.enabled = gainMb > 0
      loudnessEnhancer?.setTargetGain(gainMb)
    }
  }

  private fun attachCleanAudioEffects(audioSessionId: Int, gainMb: Int) {
    runCatching {
      loudnessEnhancer?.release()
      loudnessEnhancer =
        LoudnessEnhancer(audioSessionId).apply {
          setTargetGain(gainMb)
          enabled = gainMb > 0
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      runCatching {
        dynamicsProcessing?.release()
        dynamicsProcessing = null
        if (gainMb > 0 || micTransferActive) {
          val dpConfig =
            DynamicsProcessing.Config.Builder(
              DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
              2,
              false,
              0,
              false,
              0,
              false,
              0,
              true,
            ).build()
          val dp = DynamicsProcessing(0, audioSessionId, dpConfig)
          val limiter =
            DynamicsProcessing.Limiter(
              true,
              true,
              0,
              1.0f,
              50.0f,
              10.0f,
              -1.5f,
              0.0f,
            )
          dp.setLimiterAllChannelsTo(limiter)
          dp.enabled = true
          dynamicsProcessing = dp
        }
      }
    }
  }

  fun toggleOrPlaySound(context: Context, sound: SoundItem, triggerHaptic: Boolean = false) {
    val pb = _playbackState.value
    if (pb.currentSound?.id == sound.id && (pb.isPlaying || pb.isBuffering)) {
      if (triggerHaptic) performHapticTap(context)
      stopAll()
    } else {
      playSound(context, sound, triggerHaptic)
    }
  }

  fun playSound(context: Context, sound: SoundItem, triggerHaptic: Boolean = false) {
    if (sound.audioUrl.isBlank()) {
      _playbackState.update { it.copy(errorMessage = "Audio URL is missing for ${sound.title}") }
      return
    }
    if (triggerHaptic) {
      performHapticTap(context)
    }
    val gen = ++playGeneration
    releasePlayer()
    _playbackState.value =
      PlaybackState(
        currentSound = sound,
        isPlaying = false,
        isBuffering = true,
        positionMs = 0L,
        durationMs = 0L,
        errorMessage = null,
      )

    scope.launch {
      try {
        val dataSource =
          withContext(Dispatchers.IO) {
            runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO) }
            val offlineFile = com.example.data.local.OfflineSoundStore.findLocalAudioFile(context, sound.id)
            when {
              offlineFile != null && offlineFile.exists() && offlineFile.length() > 0L -> {
                offlineFile.absolutePath
              }
              sound.audioUrl.startsWith("data:", ignoreCase = true) -> {
                val b64 = sound.audioUrl.substringAfter("base64,", "")
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val ext = if (sound.audioUrl.contains("mp4", ignoreCase = true)) "m4a" else "mp3"
                val cached = File(context.cacheDir, "zonex_audio_${sound.id.hashCode()}.$ext")
                cached.writeBytes(bytes)
                cached.absolutePath
              }
              else -> sound.audioUrl
            }
          }

        if (gen != playGeneration) return@launch

        // Use USAGE_MEDIA + CONTENT_TYPE_MUSIC when micTransferActive is enabled.
        // USAGE_VOICE_COMMUNICATION routes through Android's narrowband VoIP / AEC echo-reference path,
        // which causes Free Fire's echo canceller to chop, distort ("ghyas ghyas"), or mute the sound!
        val usage =
          if (micTransferActive) AudioAttributes.USAGE_MEDIA
          else AudioAttributes.USAGE_GAME
        val contentType =
          if (micTransferActive) AudioAttributes.CONTENT_TYPE_MUSIC
          else AudioAttributes.CONTENT_TYPE_SONIFICATION

        val player =
          MediaPlayer().apply {
            setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(contentType)
                .build()
            )
            setDataSource(dataSource)
            setOnPreparedListener { mp ->
              if (gen != playGeneration || mediaPlayer !== mp) {
                runCatching { mp.release() }
                return@setOnPreparedListener
              }
              val soundVol = (sound.defaultVolume / 100f).coerceIn(0f, 1f)
              val effective = (masterVolume * sfxVolume * soundVol).coerceIn(0f, 1f)
              mp.setVolume(effective, effective)
              attachCleanAudioEffects(mp.audioSessionId, currentLoudnessGainMb())
              mp.start()
              _playbackState.update {
                it.copy(
                  isPlaying = true,
                  isBuffering = false,
                  durationMs = mp.duration.toLong().coerceAtLeast(0L),
                )
              }
              startProgressTicker()
            }
            setOnCompletionListener { mp ->
              if (gen != playGeneration || mediaPlayer !== mp) return@setOnCompletionListener
              progressJob?.cancel()
              _playbackState.update {
                it.copy(isPlaying = false, isBuffering = false, positionMs = 0L)
              }
            }
            setOnErrorListener { mp, what, extra ->
              if (gen != playGeneration || mediaPlayer !== mp) return@setOnErrorListener true
              progressJob?.cancel()
              _playbackState.update {
                it.copy(
                  isPlaying = false,
                  isBuffering = false,
                  errorMessage = "Playback failed (code $what/$extra). Check network or audio URL.",
                )
              }
              true
            }
            prepareAsync()
          }
        mediaPlayer = player
      } catch (e: Exception) {
        if (gen == playGeneration) {
          _playbackState.update {
            it.copy(
              isPlaying = false,
              isBuffering = false,
              errorMessage = e.localizedMessage ?: "Unable to play sound",
            )
          }
        }
      }
    }
  }

  fun togglePlayPause() {
    val player = mediaPlayer ?: return
    runCatching {
      if (player.isPlaying) {
        player.pause()
        progressJob?.cancel()
        _playbackState.update { it.copy(isPlaying = false) }
      } else {
        player.start()
        _playbackState.update { it.copy(isPlaying = true) }
        startProgressTicker()
      }
    }
  }

  fun stopAll() {
    playGeneration++
    releasePlayer()
    _playbackState.value = PlaybackState()
  }

  fun clearError() {
    _playbackState.update { it.copy(errorMessage = null) }
  }

  private fun startProgressTicker() {
    progressJob?.cancel()
    progressJob =
      scope.launch {
        while (isActive) {
          val player = mediaPlayer
          if (player != null && runCatching { player.isPlaying }.getOrDefault(false)) {
            val pos = runCatching { player.currentPosition.toLong() }.getOrDefault(0L)
            val dur = runCatching { player.duration.toLong() }.getOrDefault(0L)
            _playbackState.update { it.copy(positionMs = pos, durationMs = dur) }
          }
          delay(150L)
        }
      }
  }

  private fun releasePlayer() {
    progressJob?.cancel()
    progressJob = null
    runCatching { dynamicsProcessing?.release() }
    dynamicsProcessing = null
    runCatching { loudnessEnhancer?.release() }
    loudnessEnhancer = null
    runCatching {
      mediaPlayer?.stop()
      mediaPlayer?.release()
    }
    mediaPlayer = null
  }

  fun playSpeakerTestTone() {
    if (_isTestingSpeaker.value) return
    testToneJob?.cancel()
    testToneJob =
      scope.launch(Dispatchers.IO) {
        _isTestingSpeaker.value = true
        try {
          val sampleRate = 44100
          val durationSec = 1.2
          val totalSamples = (sampleRate * durationSec).toInt()
          val buffer = ShortArray(totalSamples)
          val gain = (0.35 * masterVolume.coerceAtLeast(0.25f))

          for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            // Two-step golden confirmation chime: 523.25 Hz (C5) -> 783.99 Hz (G5)
            val freq = if (t < 0.45) 523.25 else 783.99
            val env =
              when {
                t < 0.03 -> t / 0.03
                t > durationSec - 0.15 -> ((durationSec - t) / 0.15).coerceAtLeast(0.0)
                else -> 1.0
              }
            val wave =
              sin(2.0 * PI * freq * t) * 0.75 + sin(2.0 * PI * (freq * 2.0) * t) * 0.25
            buffer[i] = (wave * env * gain * Short.MAX_VALUE).toInt().toShort()
          }

          val minBufBytes =
            AudioTrack.getMinBufferSize(
              sampleRate,
              AudioFormat.CHANNEL_OUT_MONO,
              AudioFormat.ENCODING_PCM_16BIT,
            )
          val track =
            AudioTrack.Builder()
              .setAudioAttributes(
                AudioAttributes.Builder()
                  .setUsage(AudioAttributes.USAGE_MEDIA)
                  .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                  .build()
              )
              .setAudioFormat(
                AudioFormat.Builder()
                  .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                  .setSampleRate(sampleRate)
                  .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                  .build()
              )
              .setBufferSizeInBytes(maxOf(minBufBytes, buffer.size * 2))
              .setTransferMode(AudioTrack.MODE_STATIC)
              .build()

          track.write(buffer, 0, buffer.size)
          track.play()
          delay((durationSec * 1000).toLong() + 100L)
          runCatching {
            track.stop()
            track.release()
          }
        } finally {
          _isTestingSpeaker.value = false
        }
      }
  }

  @SuppressLint("MissingPermission")
  fun startMicTest(context: Context) {
    if (_isTestingMic.value) return
    val granted =
      ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    if (!granted) return

    micTestJob?.cancel()
    micTestJob =
      scope.launch(Dispatchers.IO) {
        _isTestingMic.value = true
        val sampleRate = 16000
        val minBuf =
          AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
          ).coerceAtLeast(2048)

        var recorder: AudioRecord? = null
        try {
          recorder =
            AudioRecord(
              MediaRecorder.AudioSource.MIC,
              sampleRate,
              AudioFormat.CHANNEL_IN_MONO,
              AudioFormat.ENCODING_PCM_16BIT,
              minBuf,
            )
          if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            return@launch
          }
          recorder.startRecording()
          val samples = ShortArray(minBuf / 2)
          while (isActive) {
            val read = recorder.read(samples, 0, samples.size)
            if (read > 0) {
              var sumSquares = 0.0
              for (i in 0 until read) {
                val s = samples[i].toDouble()
                sumSquares += s * s
              }
              val rms = sqrt(sumSquares / read)
              val db = if (rms > 1.0) (20.0 * log10(rms / 32767.0)).toFloat() else -60f
              val norm = ((db + 55f) / 55f).coerceIn(0f, 1f)
              _micDbLevel.value = db.coerceIn(-60f, 0f)
              _micLevelNormalized.value = norm
            }
            delay(50L)
          }
        } finally {
          runCatching {
            recorder?.stop()
            recorder?.release()
          }
          _micLevelNormalized.value = 0f
          _micDbLevel.value = -60f
          _isTestingMic.value = false
        }
      }
  }

  fun stopMicTest() {
    micTestJob?.cancel()
    micTestJob = null
    _isTestingMic.value = false
    _micLevelNormalized.value = 0f
    _micDbLevel.value = -60f
  }

  fun setMediaChannelVolume(context: Context, normalized: Float) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val target = (normalized.coerceIn(0f, 1f) * max).toInt().coerceIn(0, max)
    runCatching { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0) }
  }

  fun maxMediaVolume(context: Context) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    runCatching { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max, 0) }
    masterVolume = 1.0f
    sfxVolume = 1.0f
    applyLiveVolume()
  }

  fun getActiveOutputSummary(context: Context): String {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
      ?: return "Device Audio Ready"
    val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    val bt =
      outputs.firstOrNull {
        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
          it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
          (Build.VERSION.SDK_INT >= 31 && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
      }
    val wired =
      outputs.firstOrNull {
        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
          it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
          it.type == AudioDeviceInfo.TYPE_USB_HEADSET
      }
    val speaker = outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
    val primary = bt ?: wired ?: speaker ?: outputs.firstOrNull()
    val rate = primary?.sampleRates?.firstOrNull()?.let { "${it / 1000}kHz" } ?: "48kHz"
    val label =
      when {
        bt != null -> "Bluetooth (${bt.productName})"
        wired != null -> "Wired Aux (${wired.productName})"
        speaker != null -> "Phone Speaker (${Build.MODEL})"
        else -> Build.MODEL
      }
    return "$label • $rate"
  }

  fun inspectAudioDevices(context: Context): List<AudioDeviceDisplay> {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
      ?: return emptyList()
    return audioManager.getDevices(AudioManager.GET_DEVICES_ALL).map { dev ->
      val rates =
        if (dev.sampleRates.isNotEmpty()) {
          dev.sampleRates.joinToString(", ") { "${it / 1000}kHz" }
        } else {
          "Auto"
        }
      AudioDeviceDisplay(
        id = dev.id,
        name = dev.productName?.toString()?.ifBlank { "System Audio Device" } ?: "Audio Device",
        typeLabel = mapDeviceTypeName(dev.type),
        direction = if (dev.isSink && dev.isSource) "Input / Output" else if (dev.isSink) "Output" else "Input",
        sampleRates = rates,
        isWireless =
          dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= 31 && dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET),
      )
    }
  }

  fun inspectCompatibilityChecks(context: Context): List<CompatibilityCheckItem> {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    val outputs = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
    val inputs = audioManager?.getDevices(AudioManager.GET_DEVICES_INPUTS).orEmpty()
    val pm = context.packageManager

    val hasSpeaker = outputs.any { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
    val hasMicHardware = pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE) || inputs.isNotEmpty()
    val hasMicPermission =
      ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    val btDevice =
      outputs.firstOrNull {
        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
          it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
          (Build.VERSION.SDK_INT >= 31 && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
      }
    val wiredDevice =
      outputs.firstOrNull {
        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
          it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
          it.type == AudioDeviceInfo.TYPE_USB_HEADSET
      }

    return listOf(
      CompatibilityCheckItem(
        id = "speaker",
        title = "Phone Speaker",
        statusText = if (hasSpeaker) "DETECTED" else "UNAVAILABLE",
        isAvailable = hasSpeaker,
        detail = if (hasSpeaker) "Built-in speaker output active" else "No built-in speaker found",
      ),
      CompatibilityCheckItem(
        id = "mic",
        title = "Microphone",
        statusText =
          when {
            !hasMicHardware -> "NO HARDWARE"
            hasMicPermission -> "READY"
            else -> "PERMISSION NEEDED"
          },
        isAvailable = hasMicHardware,
        detail =
          if (hasMicPermission) "${inputs.size} input route(s) • Mic access granted"
          else "Hardware present • Tap TEST MIC to authorize",
      ),
      CompatibilityCheckItem(
        id = "bluetooth",
        title = "Bluetooth Audio",
        statusText = if (btDevice != null) "CONNECTED" else "NOT CONNECTED",
        isAvailable = btDevice != null,
        detail = btDevice?.productName?.toString() ?: "No active A2DP / SCO headset",
      ),
      CompatibilityCheckItem(
        id = "wired",
        title = "Wired Headset / Aux",
        statusText = if (wiredDevice != null) "CONNECTED" else "NOT PLUGGED",
        isAvailable = wiredDevice != null,
        detail = wiredDevice?.productName?.toString() ?: "No 3.5mm or USB-C headset detected",
      ),
    )
  }

  fun inspectGameMixingStatus(context: Context): GameMixingStatus {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    val pm = context.packageManager
    val modeLabel =
      when (audioManager?.mode) {
        AudioManager.MODE_IN_COMMUNICATION -> "IN COMMUNICATION (VoIP)"
        AudioManager.MODE_IN_CALL -> "IN CALL"
        AudioManager.MODE_RINGTONE -> "RINGTONE"
        else -> "NORMAL MEDIA"
      }
    val lowLatency = pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY)
    val proAudio = pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO)
    @Suppress("DEPRECATION")
    val speakerOn = audioManager?.isSpeakerphoneOn == true
    @Suppress("DEPRECATION")
    val scoOn = audioManager?.isBluetoothScoOn == true
    return GameMixingStatus(
      audioModeLabel = modeLabel,
      lowLatencySupported = lowLatency,
      proAudioSupported = proAudio,
      activeOutputLabel = getActiveOutputSummary(context),
      speakerphoneOn = speakerOn,
      bluetoothScoOn = scoOn,
    )
  }

  private fun mapDeviceTypeName(type: Int): String {
    return when (type) {
      AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in Speaker"
      AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
      AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Built-in Mic"
      AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
      AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
      AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
      AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
      AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
      AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Audio"
      AudioDeviceInfo.TYPE_TELEPHONY -> "Telephony"
      else -> "Audio Endpoint (#$type)"
    }
  }

  fun performHapticTap(context: Context) {
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator?.vibrate(
          VibrationEffect.createOneShot(28L, VibrationEffect.DEFAULT_AMPLITUDE)
        )
      } else {
        @Suppress("DEPRECATION")
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          v?.vibrate(VibrationEffect.createOneShot(28L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
          @Suppress("DEPRECATION")
          v?.vibrate(28L)
        }
      }
    }
  }
}
