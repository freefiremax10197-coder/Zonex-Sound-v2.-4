package com.example.shizuku

import android.os.Process
import androidx.annotation.Keep
import com.example.IShizukuAudioService
import java.io.BufferedReader
import java.io.InputStreamReader

class ShizukuAudioUserService @Keep constructor() : IShizukuAudioService.Stub() {

  override fun getShellIdentity(): String {
    val uid = Process.myUid()
    return execCmd("id").ifBlank { "uid=$uid(shell)" }
  }

  override fun runAudioDiagnostic(): String {
    val uid = Process.myUid()
    val selinux = execCmd("getenforce").ifBlank { "Unknown" }
    val idOut = execCmd("id").ifBlank { "uid=$uid" }
    val sndInfo = execCmd("ls /dev/snd").ifBlank { "restricted" }
    val tinymixPath = execCmd("which tinymix").ifBlank { "not present" }
    val audioDump = execCmd("dumpsys audio | head -n 12").ifBlank { "unavailable" }
    return buildString {
      appendLine("Process Identity: $idOut")
      appendLine("Privilege Level: ${if (uid == 0) "ROOT (UID 0)" else "ADB SHELL (UID $uid)"}")
      appendLine("SELinux Mode: $selinux")
      appendLine("tinymix binary: $tinymixPath")
      appendLine("/dev/snd nodes: ${sndInfo.replace("\n", ", ")}")
      appendLine("AudioService Probe:\n$audioDump")
    }.trim()
  }

  override fun enableMicTransferRouting(): String {
    return enableOptimizedMicRouting("com.zonexsound.app", 0, true)
  }

  override fun disableMicTransferRouting(): String {
    return disableOptimizedMicRouting("com.zonexsound.app")
  }

  override fun enableOptimizedMicRouting(
    packageName: String?,
    appPid: Int,
    forceSpeaker: Boolean,
  ): String {
    val pkg = sanitizePackageName(packageName) ?: return "INVALID_PACKAGE"
    val uid = Process.myUid()
    val forceMode = if (forceSpeaker) "SPEAKER" else "NONE"
    val cmds = buildList {
      add("cmd deviceidle whitelist +$pkg 2>/dev/null || true")
      add("appops set $pkg RUN_IN_BACKGROUND allow 2>/dev/null || true")
      add("appops set $pkg RUN_ANY_IN_BACKGROUND allow 2>/dev/null || true")
      add("appops set $pkg PLAY_AUDIO allow 2>/dev/null || true")
      add("appops set $pkg RECORD_AUDIO allow 2>/dev/null || true")
      add("appops set $pkg TAKE_AUDIO_FOCUS ignore 2>/dev/null || true")
      add("pm grant $pkg android.permission.MODIFY_AUDIO_SETTINGS 2>/dev/null || true")
      add("cmd audio set-force-use COMMUNICATION $forceMode 2>/dev/null || true")
      if (uid == 0 && appPid > 0) {
        add("renice -n -10 $appPid 2>/dev/null || true")
      }
    }
    execCmd(cmds.joinToString("; "))
    return if (uid == 0) {
      "ROOT_SHIZUKU_HD_ROUTING ($forceMode)"
    } else {
      "SHIZUKU_HD_ROUTING ($forceMode)"
    }
  }

  override fun disableOptimizedMicRouting(packageName: String?): String {
    val pkg = sanitizePackageName(packageName)
    val cmds = buildList {
      add("cmd audio set-force-use COMMUNICATION NONE 2>/dev/null || true")
      if (pkg != null) {
        add("appops set $pkg TAKE_AUDIO_FOCUS allow 2>/dev/null || true")
      }
    }
    execCmd(cmds.joinToString("; "))
    return "RESTORED_NORMAL_ROUTING"
  }

  override fun destroy() {
    System.exit(0)
  }

  private fun sanitizePackageName(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isBlank()) return null
    return if (PKG_REGEX.matches(trimmed)) trimmed else null
  }

  private fun execCmd(cmd: String): String {
    return runCatching {
      val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
      val out = BufferedReader(InputStreamReader(proc.inputStream)).readText().trim()
      val err = BufferedReader(InputStreamReader(proc.errorStream)).readText().trim()
      proc.waitFor()
      out.ifBlank { err }
    }.getOrDefault("")
  }

  companion object {
    private val PKG_REGEX = Regex("^[a-zA-Z0-9_.]{3,128}$")
  }
}
