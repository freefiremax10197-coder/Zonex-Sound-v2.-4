package com.example.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.Process
import com.example.IShizukuAudioService
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

data class ShizukuRootState(
  val isShizukuInstalled: Boolean = false,
  val isShizukuRunning: Boolean = false,
  val hasShizukuPermission: Boolean = false,
  val isRootAvailable: Boolean = false,
  val isServiceBound: Boolean = false,
  val isInjectionActive: Boolean = false,
  val shellIdentity: String = "",
  val statusMessage: String = "Shizuku / Root চেক করতে নিচের বাটনে ট্যাপ করুন",
  val diagnosticReport: String = "",
)

object ShizukuRootManager {

  private val SHIZUKU_PACKAGES = listOf("moe.shizuku.privileged.api", "moe.shizuku.manager")
  private const val REQ_CODE_SHIZUKU = 9001
  private val PKG_REGEX = Regex("^[a-zA-Z0-9_.]{3,128}$")

  private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val _state = MutableStateFlow(ShizukuRootState())
  val state: StateFlow<ShizukuRootState> = _state.asStateFlow()

  @Volatile
  private var userService: IShizukuAudioService? = null
  @Volatile
  private var isBindingInProgress = false
  private var listenersRegistered = false
  private var appContextRef: Context? = null

  private fun buildUserServiceArgs(ctx: Context): Shizuku.UserServiceArgs =
    Shizuku.UserServiceArgs(ComponentName(ctx.applicationContext, ShizukuAudioUserService::class.java))
      .daemon(false)
      .processNameSuffix("zonex_audio")
      .debuggable(false)
      .version(3)

  private val serviceConnection =
    object : ServiceConnection {
      override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        isBindingInProgress = false
        val stub = IShizukuAudioService.Stub.asInterface(service)
        userService = stub
        ioScope.launch {
          val idText = runCatching { stub?.getShellIdentity().orEmpty() }.getOrDefault("")
          val displayId = idText.ifBlank { "uid=2000(shell)" }
          _state.update {
            it.copy(
              isShizukuRunning = true,
              hasShizukuPermission = true,
              isServiceBound = true,
              shellIdentity = displayId,
              statusMessage = "Shizuku ✔ রেডি\n$displayId",
            )
          }
        }
      }

      override fun onServiceDisconnected(name: ComponentName?) {
        isBindingInProgress = false
        userService = null
        _state.update { it.copy(isServiceBound = false) }
      }
    }

  private val binderReceivedListener =
    Shizuku.OnBinderReceivedListener {
      refreshShizukuStatus()
    }

  private val binderDeadListener =
    Shizuku.OnBinderDeadListener {
      isBindingInProgress = false
      userService = null
      _state.update {
        it.copy(
          isShizukuRunning = false,
          hasShizukuPermission = false,
          isServiceBound = false,
          shellIdentity = "",
          statusMessage = "Shizuku সার্ভিস সংযোগ বিচ্ছিন্ন হয়েছে। Shizuku অ্যাপে Start দিন।",
        )
      }
    }

  private val permissionResultListener =
    Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
      if (requestCode == REQ_CODE_SHIZUKU) {
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        _state.update {
          it.copy(
            isShizukuRunning = runCatching { Shizuku.pingBinder() }.getOrDefault(it.isShizukuRunning),
            hasShizukuPermission = granted,
            statusMessage =
              if (granted) "Shizuku ✔ রেডি — শেল সার্ভিস কানেক্ট হচ্ছে..."
              else "Shizuku পারমিশন Allow করা হয়নি। দয়া করে Allow দিন।",
          )
        }
        if (granted) {
          bindShizukuService()
        }
      }
    }

  fun init(context: Context) {
    appContextRef = context.applicationContext
    if (!listenersRegistered) {
      listenersRegistered = true
      runCatching {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
      }
    }
    refreshShizukuStatus()
  }

  fun findInstalledShizukuPackage(context: Context): String? {
    return SHIZUKU_PACKAGES.firstOrNull { pkg ->
      runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          context.packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
        } else {
          @Suppress("DEPRECATION")
          context.packageManager.getPackageInfo(pkg, 0)
        }
        true
      }.getOrDefault(false)
    }
  }

  fun refreshShizukuStatus() {
    val ctx = appContextRef ?: return
    val installed = findInstalledShizukuPackage(ctx) != null
    if (installed && !runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
      runCatching { ShizukuProvider.requestBinderForNonProviderProcess(ctx) }
    }

    val running = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
    val granted =
      if (running) {
        runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }
          .getOrDefault(false)
      } else {
        false
      }

    _state.update { cur ->
      val readyDetail =
        if (cur.shellIdentity.isNotBlank()) "Shizuku ✔ রেডি\n${cur.shellIdentity}"
        else "Shizuku ✔ রেডি"
      cur.copy(
        isShizukuInstalled = installed,
        isShizukuRunning = running,
        hasShizukuPermission = granted,
        shellIdentity = if (granted) cur.shellIdentity else "",
        statusMessage =
          when {
            cur.isInjectionActive -> cur.statusMessage
            !installed -> "Shizuku অ্যাপ ইন্সটল করা নেই। নিচের বাটন থেকে ইন্সটল করুন।"
            !running -> "Shizuku ইন্সটল আছে কিন্তু চালু নয়। Shizuku অ্যাপে Start দিন।"
            granted -> readyDetail
            else -> "Shizuku চালু আছে। পারমিশন দিতে বাটনে ট্যাপ করে Allow দিন।"
          },
      )
    }

    if (granted && userService == null) {
      bindShizukuService()
    } else if (granted && userService != null && _state.value.shellIdentity.isBlank()) {
      ioScope.launch {
        val idText = runCatching { userService?.getShellIdentity().orEmpty() }.getOrDefault("")
        if (idText.isNotBlank()) {
          _state.update {
            it.copy(
              isServiceBound = true,
              shellIdentity = idText,
              statusMessage = if (it.isInjectionActive) it.statusMessage else "Shizuku ✔ রেডি\n$idText",
            )
          }
        }
      }
    }
  }

  fun requestShizukuPermission() {
    val ctx = appContextRef ?: return
    ioScope.launch {
      val installed = findInstalledShizukuPackage(ctx) != null
      if (!installed) {
        _state.update {
          it.copy(
            isShizukuInstalled = false,
            isShizukuRunning = false,
            hasShizukuPermission = false,
            statusMessage = "Shizuku অ্যাপ ইন্সটল করা নেই। 'Shizuku ইন্সটল করো' বাটনে ট্যাপ করুন।",
          )
        }
        return@launch
      }

      if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
        runCatching { ShizukuProvider.requestBinderForNonProviderProcess(ctx) }
        delay(280L)
      }

      val running = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
      if (!running) {
        _state.update {
          it.copy(
            isShizukuInstalled = true,
            isShizukuRunning = false,
            hasShizukuPermission = false,
            statusMessage = "Shizuku সার্ভিস চালু নেই! Shizuku অ্যাপ খুলে Wireless Debugging দিয়ে Start দিন।",
          )
        }
        return@launch
      }

      val alreadyGranted =
        runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }
          .getOrDefault(false)
      if (alreadyGranted) {
        _state.update {
          it.copy(
            isShizukuInstalled = true,
            isShizukuRunning = true,
            hasShizukuPermission = true,
            statusMessage =
              if (it.shellIdentity.isNotBlank()) "Shizuku ✔ রেডি\n${it.shellIdentity}"
              else "Shizuku ✔ রেডি",
          )
        }
        bindShizukuService()
        return@launch
      }

      withContext(Dispatchers.Main) {
        runCatching { Shizuku.requestPermission(REQ_CODE_SHIZUKU) }
          .onFailure { err ->
            _state.update {
              it.copy(statusMessage = "Shizuku রিকোয়েস্ট ব্যর্থ: ${err.localizedMessage}")
            }
          }
      }
    }
  }

  private fun bindShizukuService() {
    val ctx = appContextRef ?: return
    if (isBindingInProgress) return
    if (userService != null) return
    isBindingInProgress = true
    runCatching {
      Shizuku.bindUserService(buildUserServiceArgs(ctx), serviceConnection)
    }.onFailure {
      isBindingInProgress = false
    }
  }

  private fun hasSuBinaryHint(): Boolean {
    val staticPaths =
      arrayOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/vendor/bin/su",
        "/debug_ramdisk/su",
        "/data/adb/ksu/bin/su",
        "/data/adb/ap/bin/su",
      )
    if (staticPaths.any { runCatching { File(it).exists() }.getOrDefault(false) }) {
      return true
    }
    val envPath = System.getenv("PATH").orEmpty()
    return envPath.split(":").any { dir ->
      dir.isNotBlank() && runCatching { File(dir, "su").exists() }.getOrDefault(false)
    }
  }

  private fun hasExternalHeadset(audioManager: AudioManager?): Boolean {
    val outputs = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: return false
    return outputs.any { dev ->
      dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
        dev.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
        dev.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
        dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
        dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
        (Build.VERSION.SDK_INT >= 31 && dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
    }
  }

  private fun runSuScript(script: String): Pair<Boolean, String> {
    return runCatching {
      val proc = ProcessBuilder("su").redirectErrorStream(true).start()
      proc.outputStream.bufferedWriter().use { writer ->
        writer.write(script)
        writer.newLine()
        writer.write("exit")
        writer.newLine()
        writer.flush()
      }
      val out = BufferedReader(InputStreamReader(proc.inputStream)).readText().trim()
      val code = proc.waitFor()
      (code == 0) to out
    }.getOrDefault(false to "")
  }

  suspend fun checkRootAccess(): Boolean =
    withContext(Dispatchers.IO) {
      val (ok, out) = runSuScript("id")
      val rooted = ok && out.contains("uid=0")
      _state.update {
        it.copy(
          isRootAvailable = rooted,
          statusMessage =
            if (rooted) "Root ✔ অ্যাক্সেস পাওয়া গেছে (uid=0)!"
            else "ডিভাইসে Root অ্যাক্সেস নেই (Shizuku ব্যবহার করুন)।",
        )
      }
      rooted
    }

  suspend fun restoreSavedMicTransferIfReady(context: Context) {
    withContext(Dispatchers.IO) {
      refreshShizukuStatus()
      val hasShizuku = _state.value.hasShizukuPermission
      val hasRoot = if (hasSuBinaryHint()) checkRootAccess() else false
      if (hasShizuku || hasRoot) {
        enableMicTransferInternal(context, precheckedRoot = hasRoot)
      }
    }
  }

  suspend fun enableMicTransfer(context: Context): String =
    enableMicTransferInternal(context, precheckedRoot = null)

  private suspend fun enableMicTransferInternal(
    context: Context,
    precheckedRoot: Boolean?,
  ): String =
    withContext(Dispatchers.IO) {
      val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
      val headsetConnected = hasExternalHeadset(audioManager)
      val forceSpeaker = !headsetConnected

      // Do NOT call audioManager.mode = MODE_IN_COMMUNICATION!
      // Calling MODE_IN_COMMUNICATION steals VoIP session ownership from Free Fire
      // and causes Free Fire's mic to stutter or cut off completely.
      audioManager?.let { am ->
        runCatching {
          val maxMusic = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
          val curMusic = am.getStreamVolume(AudioManager.STREAM_MUSIC)
          if (curMusic < (maxMusic * 0.85f).toInt()) {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
          }
        }
      }

      val pkg = context.packageName.takeIf { PKG_REGEX.matches(it) } ?: "com.zonexsound.app"
      val myPid = Process.myPid()
      val rooted = precheckedRoot ?: checkRootAccess()
      val hasShizuku = _state.value.hasShizukuPermission

      val backends = mutableListOf<String>()

      if (rooted) {
        val forceMode = if (forceSpeaker) "SPEAKER" else "NONE"
        val suScript =
          """
          id
          cmd deviceidle whitelist +$pkg 2>/dev/null || true
          appops set $pkg RUN_IN_BACKGROUND allow 2>/dev/null || true
          appops set $pkg RUN_ANY_IN_BACKGROUND allow 2>/dev/null || true
          appops set $pkg PLAY_AUDIO allow 2>/dev/null || true
          appops set $pkg RECORD_AUDIO allow 2>/dev/null || true
          appops set $pkg TAKE_AUDIO_FOCUS ignore 2>/dev/null || true
          pm grant $pkg android.permission.MODIFY_AUDIO_SETTINGS 2>/dev/null || true
          cmd audio set-force-use COMMUNICATION $forceMode 2>/dev/null || true
          renice -n -10 $myPid 2>/dev/null || true
          """.trimIndent()
        val (suOk, _) = runSuScript(suScript)
        if (suOk) {
          backends.add("Root (su) HD Audio + Priority")
        }
      }

      if (hasShizuku) {
        if (userService == null) {
          bindShizukuService()
          var waitedMs = 0L
          while (userService == null && waitedMs < 900L) {
            delay(150L)
            waitedMs += 150L
          }
        }
        val shizukuOut =
          runCatching {
            userService?.enableOptimizedMicRouting(pkg, myPid, forceSpeaker)
          }.getOrNull().orEmpty()
        if (shizukuOut.isNotBlank()) {
          backends.add("Shizuku ($shizukuOut)")
        } else {
          backends.add("Shizuku Authorized")
        }
      }

      val routeNote =
        if (headsetConnected) "Headset Safe Mode"
        else "Clean 48kHz Speaker-to-Mic"
      val backendDetail =
        if (backends.isNotEmpty()) {
          "${backends.joinToString(" + ")} • $routeNote"
        } else {
          "Standard HD Media Routing • $routeNote"
        }

      _state.update {
        it.copy(
          isInjectionActive = true,
          statusMessage = backendDetail,
        )
      }
      backendDetail
    }

  suspend fun disableMicTransfer(context: Context) =
    withContext(Dispatchers.IO) {
      val pkg = context.packageName.takeIf { PKG_REGEX.matches(it) } ?: "com.zonexsound.app"
      if (_state.value.isRootAvailable) {
        runSuScript(
          """
          cmd audio set-force-use COMMUNICATION NONE 2>/dev/null || true
          appops set $pkg TAKE_AUDIO_FOCUS allow 2>/dev/null || true
          """.trimIndent()
        )
      }
      runCatching { userService?.disableOptimizedMicRouting(pkg) }
      _state.update {
        it.copy(
          isInjectionActive = false,
          statusMessage = "Mic Transfer বন্ধ করা হয়েছে। স্বাভাবিক অডিও চালু।",
        )
      }
    }

  suspend fun generateDiagnosticReport(context: Context): String =
    withContext(Dispatchers.IO) {
      refreshShizukuStatus()
      val rooted = checkRootAccess()
      val shizukuDiag =
        if (_state.value.hasShizukuPermission && userService != null) {
          runCatching { userService?.runAudioDiagnostic() }.getOrNull().orEmpty()
        } else {
          "Shizuku UserService not bound (grant Shizuku permission first)."
        }

      val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
      val mode = am?.mode ?: -1
      val report = buildString {
        appendLine("=== ZONEX AUDIO & PRIVILEGE REPORT ===")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})")
        appendLine("Root (su): ${if (rooted) "AVAILABLE" else "NOT AVAILABLE"}")
        appendLine("Shizuku Installed: ${_state.value.isShizukuInstalled}")
        appendLine("Shizuku Running: ${_state.value.isShizukuRunning}")
        appendLine("Shizuku Permission: ${_state.value.hasShizukuPermission}")
        appendLine("Headset Connected: ${hasExternalHeadset(am)}")
        appendLine("AudioManager Mode: $mode")
        appendLine("--- Privileged Probe ---")
        appendLine(shizukuDiag)
        appendLine("--- Audio Routing ---")
        appendLine(
          "HD Media 48kHz stream + Limiter + Focus Protection (no VoIP mode hijack). Keep phone speaker unobstructed for Free Fire mic, or use V8 Sound Card for wired loopback."
        )
      }
      _state.update { it.copy(diagnosticReport = report) }
      report
    }
}
