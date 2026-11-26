package com.example.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Debug
import android.os.Process
import com.example.BuildConfig
import java.io.File
import java.security.MessageDigest

object ZonexSecurityGuard {

  private const val PKG_TOKEN = "paduseo.op.noxnzmc"
  private const val FB_KEY_TOKEN = "8UaeHdaBIyI-RlsfBSzAHHH6h6Jaw4lLJzV0yaI"
  private const val FB_DB_TOKEN = "mcoeaei.drtufdeivxnz/stho.isbrfbt-lae-co-eo/:pt"
  private const val FB_PROJ_TOKEN = "eivxnzco-eo"
  private const val FB_BUCKET_TOKEN = "paeaossbrfeivxnzp.grteaei.co-eo"
  private const val FB_SENDER_TOKEN = "384452941506"
  private const val FB_APP_TOKEN = "4525f8ff9b4:e:9415061170edaa3671bw384452:"
  private const val CARTESIA_TTS_TOKEN = "stbsti.iercia/sthey/t/aasta.p/:pt"
  private const val CARTESIA_VOICES_TOKEN = "sco/aasta.p/:pteivi.iercia/sth"

  internal fun reveal(token: String): String {
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
  val cartesiaTtsUrl: String get() = reveal(CARTESIA_TTS_TOKEN)
  val cartesiaVoicesUrl: String get() = reveal(CARTESIA_VOICES_TOKEN)

  fun compareVersionNames(first: String, second: String): Int {
    val aClean = first.trim().removePrefix("v").removePrefix("V")
    val bClean = second.trim().removePrefix("v").removePrefix("V")
    val aParts = aClean.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
    val bParts = bClean.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
    val maxLen = maxOf(aParts.size, bParts.size)
    for (i in 0 until maxLen) {
      val av = aParts.getOrElse(i) { 0 }
      val bv = bParts.getOrElse(i) { 0 }
      if (av != bv) return av.compareTo(bv)
    }
    return 0
  }

  fun isVersionBelowLatest(
    installedVersionName: String,
    installedVersionCode: Int,
    latestVersionName: String,
    latestVersionCode: Int,
  ): Boolean {
    val latestNameClean = latestVersionName.trim().removePrefix("v").removePrefix("V")
    if (latestNameClean.isNotBlank()) {
      val cmp = compareVersionNames(installedVersionName, latestNameClean)
      if (cmp < 0) return true
      if (cmp > 0) return false
      // Same versionName string — also honor higher latestVersionCode only when explicitly set
      return latestVersionCode > 0 && installedVersionCode < latestVersionCode
    }
    return latestVersionCode > 0 && installedVersionCode < latestVersionCode
  }

  fun shouldShowAppUpdate(
    installedVersionName: String,
    installedVersionCode: Int,
    latestVersionName: String,
    latestVersionCode: Int,
    targetVersionName: String,
  ): Boolean {
    val targetClean = targetVersionName.trim().removePrefix("v").removePrefix("V")
    if (targetClean.isNotBlank()) {
      // Admin specifies max version (e.g. "2.1") -> 2.1, 2.0, 1.9, 1.8... all receive the update notice
      return compareVersionNames(installedVersionName, targetClean) <= 0
    }
    return isVersionBelowLatest(
      installedVersionName = installedVersionName,
      installedVersionCode = installedVersionCode,
      latestVersionName = latestVersionName,
      latestVersionCode = latestVersionCode,
    )
  }

  @Volatile
  private var integrityVerified = false

  fun verifyIntegrity(context: Context) {
    if (context.packageName != expectedPackageName) {
      throw SecurityException("Package signature validation failed")
    }
    if (!BuildConfig.DEBUG) {
      val appDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
      if (appDebuggable || Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
        Process.killProcess(Process.myPid())
        return
      }
      if (hasHookingFrameworkArtifacts()) {
        throw SecurityException("Runtime integrity check failed")
      }
    }
    integrityVerified = true
  }

  private fun hasHookingFrameworkArtifacts(): Boolean {
    val hookedClasses =
      arrayOf(
        "de.robv.android.xposed.XposedBridge",
        "de.robv.android.xposed.XposedHelpers",
        "com.saurik.substrate.MS",
      )
    for (cls in hookedClasses) {
      val found = runCatching { Class.forName(cls, false, ZonexSecurityGuard::class.java.classLoader) }.isSuccess
      if (found) return true
    }
    val suspiciousMaps =
      runCatching {
        val mapsFile = File("/proc/self/maps")
        if (!mapsFile.exists()) false
        else {
          mapsFile.bufferedReader().useLines { lines ->
            lines.any { line ->
              val lower = line.lowercase()
              lower.contains("frida-agent") || lower.contains("frida-gadget")
            }
          }
        }
      }.getOrDefault(false)
    return suspiciousMaps
  }

  @Suppress("DEPRECATION")
  fun getAppSignatureSha256(context: Context): String {
    return runCatching {
      val pm = context.packageManager
      val pkgInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
          pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        }
      val sigBytes =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          pkgInfo.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        } else {
          pkgInfo.signatures?.firstOrNull()?.toByteArray()
        } ?: return@runCatching ""
      val digest = MessageDigest.getInstance("SHA-256").digest(sigBytes)
      digest.joinToString(":") { "%02X".format(it) }
    }.getOrDefault("")
  }

  fun sanitizeExternalUrl(rawUrl: String?): String? {
    val trimmed = rawUrl?.trim().orEmpty()
    if (trimmed.isBlank()) return null
    val lower = trimmed.lowercase()
    if (
      lower.startsWith("javascript:") ||
      lower.startsWith("vbscript:") ||
      lower.startsWith("data:") ||
      lower.startsWith("file:") ||
      lower.startsWith("content:") ||
      lower.startsWith("intent:")
    ) {
      return null
    }
    val candidate =
      if (lower.startsWith("https://") || lower.startsWith("http://")) trimmed
      else "https://$trimmed"
    val uri = runCatching { Uri.parse(candidate) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase().orEmpty()
    val host = uri.host.orEmpty()
    if ((scheme != "https" && scheme != "http") || host.isBlank()) return null
    return uri.toString()
  }
}
