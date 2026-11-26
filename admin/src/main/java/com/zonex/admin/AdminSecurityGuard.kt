package com.zonex.admin

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Debug
import android.os.Process
import java.io.File

object AdminSecurityGuard {

  private const val PKG_TOKEN = "panmano.op.iddusmc"
  private const val FB_KEY_TOKEN = "8UaeHdaBIyI-RlsfBSzAHHH6h6Jaw4lLJzV0yaI"
  private const val FB_DB_TOKEN = "mcoeaei.drtufdeivxnz/stho.isbrfbt-lae-co-eo/:pt"
  private const val FB_PROJ_TOKEN = "eivxnzco-eo"
  private const val FB_BUCKET_TOKEN = "paeaossbrfeivxnzp.grteaei.co-eo"
  private const val FB_SENDER_TOKEN = "384452941506"
  private const val FB_APP_TOKEN = "4525f8ff9b4:e:9415061170edaa3671bw384452:"

  private fun reveal(token: String): String {
    if (token.isEmpty()) return ""
    val half = (token.length + 1) / 2
    val evens = token.substring(0, half)
    val odds = token.substring(half)
    val sb = StringBuilder(token.length)
    for (i in 0 until half) {
      sb.append(evens[i])
      if (i < odds.length) sb.append(odds[i])
    }
    return sb.reverse().toString()
  }

  val expectedPackageName: String get() = reveal(PKG_TOKEN)
  val firebaseApiKey: String get() = reveal(FB_KEY_TOKEN)
  val firebaseDbUrl: String get() = reveal(FB_DB_TOKEN)
  val firebaseProjectId: String get() = reveal(FB_PROJ_TOKEN)
  val firebaseStorageBucket: String get() = reveal(FB_BUCKET_TOKEN)
  val firebaseSenderId: String get() = reveal(FB_SENDER_TOKEN)
  val firebaseAppId: String get() = reveal(FB_APP_TOKEN)

  fun verifyIntegrity(context: Context) {
    if (context.packageName != expectedPackageName) {
      throw SecurityException("Admin package tampering detected")
    }
    if (!BuildConfig.DEBUG) {
      val appDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
      if (appDebuggable || Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
        Process.killProcess(Process.myPid())
        return
      }
      val hasFrida =
        runCatching {
          val mapsFile = File("/proc/self/maps")
          mapsFile.exists() && mapsFile.bufferedReader().useLines { lines ->
            lines.any {
              val l = it.lowercase()
              l.contains("frida-agent") || l.contains("frida-gadget")
            }
          }
        }.getOrDefault(false)
      if (hasFrida) {
        throw SecurityException("Admin runtime integrity violation")
      }
    }
  }

  fun sanitizeExternalUrl(rawUrl: String?): String? {
    val trimmed = rawUrl?.trim().orEmpty()
    if (trimmed.isBlank()) return null
    val lower = trimmed.lowercase()
    if (
      lower.startsWith("javascript:") ||
      lower.startsWith("vbscript:") ||
      lower.startsWith("data:") ||
      lower.startsWith("file:")
    ) {
      return null
    }
    val candidate =
      if (lower.startsWith("https://") || lower.startsWith("http://")) trimmed
      else "https://$trimmed"
    val uri = runCatching { Uri.parse(candidate) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase().orEmpty()
    return if ((scheme == "https" || scheme == "http") && !uri.host.isNullOrBlank()) uri.toString() else null
  }
}
