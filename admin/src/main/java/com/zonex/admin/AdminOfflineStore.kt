package com.zonex.admin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class AdminPendingOp(
  val opId: String,
  val type: String,
  val payloadJson: String,
  val createdAt: Long,
) {
  companion object {
    const val TYPE_SAVE_SOUND = "SAVE_SOUND"
    const val TYPE_DELETE_SOUND = "DELETE_SOUND"
    const val TYPE_DELETE_USER_SOUND = "DELETE_USER_SOUND"
    const val TYPE_SAVE_CATEGORIES = "SAVE_CATEGORIES"
    const val TYPE_SAVE_NOTICE = "SAVE_NOTICE"
    const val TYPE_SAVE_MAINTENANCE = "SAVE_MAINTENANCE"
    const val TYPE_SAVE_APP_UPDATE = "SAVE_APP_UPDATE"
    const val TYPE_SAVE_TTS_VOICE = "SAVE_TTS_VOICE"
    const val TYPE_DELETE_TTS_VOICE = "DELETE_TTS_VOICE"
    const val TYPE_SAVE_APP_BRANDING = "SAVE_APP_BRANDING"
    const val TYPE_SAVE_DEV_CONFIG = "SAVE_DEV_CONFIG"
    const val TYPE_SAVE_KEY_GATE = "SAVE_KEY_GATE"
    const val TYPE_UPSERT_KEY = "UPSERT_KEY"
    const val TYPE_TOGGLE_REVOKE_KEY = "TOGGLE_REVOKE_KEY"
    const val TYPE_RESET_KEY = "RESET_KEY"
    const val TYPE_DELETE_KEY = "DELETE_KEY"
  }
}

object AdminOfflineStore {

  private const val DIR_NAME = "admin_offline"
  private const val MEDIA_DIR = "media"
  private const val SOUNDS_FILE = "cached_sounds.json"
  private const val CATEGORIES_FILE = "cached_categories.json"
  private const val NOTICE_FILE = "cached_notice.json"
  private const val APP_CONFIG_FILE = "cached_app_config.json"
  private const val DEV_CONFIG_FILE = "cached_dev_config.json"
  private const val UPDATE_CONFIG_FILE = "cached_update_config.json"
  private const val MAINT_CONFIG_FILE = "cached_maintenance.json"
  private const val TTS_VOICES_FILE = "cached_tts_voices.json"
  private const val KEY_GATE_FILE = "cached_key_gate.json"
  private const val ACCESS_KEYS_FILE = "cached_access_keys.json"
  private const val USERS_FILE = "cached_users.json"
  private const val PENDING_OPS_FILE = "pending_ops.json"

  private val _cachedSounds = MutableStateFlow<List<AdminSound>>(emptyList())
  val cachedSounds: StateFlow<List<AdminSound>> = _cachedSounds.asStateFlow()

  private val _cachedCategories = MutableStateFlow(listOf("Funny", "Reaction", "Meme"))
  val cachedCategories: StateFlow<List<String>> = _cachedCategories.asStateFlow()

  private val _cachedNotice = MutableStateFlow(AdminNotice())
  val cachedNotice: StateFlow<AdminNotice> = _cachedNotice.asStateFlow()

  private val _cachedAppConfig = MutableStateFlow(AdminAppConfig())
  val cachedAppConfig: StateFlow<AdminAppConfig> = _cachedAppConfig.asStateFlow()

  private val _cachedDevConfig = MutableStateFlow(AdminDeveloperConfig())
  val cachedDevConfig: StateFlow<AdminDeveloperConfig> = _cachedDevConfig.asStateFlow()

  private val _cachedUpdateConfig = MutableStateFlow(AdminAppUpdateConfig())
  val cachedUpdateConfig: StateFlow<AdminAppUpdateConfig> = _cachedUpdateConfig.asStateFlow()

  private val _cachedMaintConfig = MutableStateFlow(AdminMaintenanceConfig())
  val cachedMaintConfig: StateFlow<AdminMaintenanceConfig> = _cachedMaintConfig.asStateFlow()

  private val _cachedTtsVoices = MutableStateFlow<List<AdminTtsVoice>>(emptyList())
  val cachedTtsVoices: StateFlow<List<AdminTtsVoice>> = _cachedTtsVoices.asStateFlow()

  private val _cachedKeyGate = MutableStateFlow(AdminKeyGateConfig())
  val cachedKeyGate: StateFlow<AdminKeyGateConfig> = _cachedKeyGate.asStateFlow()

  private val _cachedAccessKeys = MutableStateFlow<List<AdminAccessKey>>(emptyList())
  val cachedAccessKeys: StateFlow<List<AdminAccessKey>> = _cachedAccessKeys.asStateFlow()

  private val _cachedUsers = MutableStateFlow<List<AdminUserRecord>>(emptyList())
  val cachedUsers: StateFlow<List<AdminUserRecord>> = _cachedUsers.asStateFlow()

  private val _pendingOps = MutableStateFlow<List<AdminPendingOp>>(emptyList())
  val pendingOps: StateFlow<List<AdminPendingOp>> = _pendingOps.asStateFlow()

  @Volatile
  private var initialized = false

  fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }

  private fun baseDir(context: Context): File =
    File(context.applicationContext.filesDir, DIR_NAME).apply { mkdirs() }

  private fun mediaDir(context: Context): File =
    File(baseDir(context), MEDIA_DIR).apply { mkdirs() }

  fun ensureLoaded(context: Context) {
    if (initialized) return
    synchronized(this) {
      if (initialized) return
      val appCtx = context.applicationContext
      _cachedSounds.value = readSounds(File(baseDir(appCtx), SOUNDS_FILE))
      val cats = readStrings(File(baseDir(appCtx), CATEGORIES_FILE))
      if (cats.isNotEmpty()) _cachedCategories.value = cats
      _cachedNotice.value = readNotice(File(baseDir(appCtx), NOTICE_FILE))
      _cachedAppConfig.value = readAppConfig(File(baseDir(appCtx), APP_CONFIG_FILE))
      _cachedDevConfig.value = readDevConfig(File(baseDir(appCtx), DEV_CONFIG_FILE))
      _cachedUpdateConfig.value = readUpdateConfig(File(baseDir(appCtx), UPDATE_CONFIG_FILE))
      _cachedMaintConfig.value = readMaintConfig(File(baseDir(appCtx), MAINT_CONFIG_FILE))
      _cachedTtsVoices.value = readTtsVoices(File(baseDir(appCtx), TTS_VOICES_FILE))
      _cachedKeyGate.value = readKeyGate(File(baseDir(appCtx), KEY_GATE_FILE))
      _cachedAccessKeys.value = readAccessKeys(File(baseDir(appCtx), ACCESS_KEYS_FILE))
      _cachedUsers.value = readUsers(File(baseDir(appCtx), USERS_FILE))
      _pendingOps.value = readPendingOps(File(baseDir(appCtx), PENDING_OPS_FILE))
      initialized = true
    }
  }

  suspend fun saveSoundsCache(context: Context, list: List<AdminSound>) = withContext(Dispatchers.IO) {
    val appCtx = context.applicationContext
    ensureLoaded(appCtx)
    _cachedSounds.value = list
    writeSounds(File(baseDir(appCtx), SOUNDS_FILE), list)
  }

  suspend fun saveCategoriesCache(context: Context, list: List<String>) = withContext(Dispatchers.IO) {
    val appCtx = context.applicationContext
    ensureLoaded(appCtx)
    _cachedCategories.value = list
    writeStrings(File(baseDir(appCtx), CATEGORIES_FILE), list)
  }

  suspend fun saveNoticeCache(context: Context, item: AdminNotice) = withContext(Dispatchers.IO) {
    val appCtx = context.applicationContext
    ensureLoaded(appCtx)
    _cachedNotice.value = item
    writeNotice(File(baseDir(appCtx), NOTICE_FILE), item)
  }

  suspend fun saveAppConfigCache(context: Context, item: AdminAppConfig) = withContext(Dispatchers.IO) {
    val appCtx = context.applicationContext
    ensureLoaded(appCtx)
    _cachedAppConfig.value = item
    writeAppConfig(File(baseDir(appCtx), APP_CONFIG_FILE), item)
  }

  suspend fun saveDevConfigCache(context: Context, item: AdminDeveloperConfig) = withContext(Dispatchers.IO) {
    val appCtx = context.applicationContext
    ensureLoaded(appCtx)
    _cachedDevConfig.value = item
    writeDevConfig(File(baseDir(appCtx), DEV_CONFIG_FILE), item)
  }

  suspend fun saveUpdateConfigCache(context: Context, item: AdminAppUpdateConfig) = withContext(Dispatchers.IO) {
    val appCtx = context.applicationContext
    ensureLoaded(appCtx)
    _cachedUpdateConfig.value = item
    writeUpdateConfig(File(baseDir(appCtx), UPDATE_CONFIG_FILE), item)
  }

  suspend fun saveMaintConfigCache(context: Context, item: AdminMaintenanceConfig) = withContext(Dispatchers.IO) {
    val appCtx = context.applicationContext
    ensureLoaded(appCtx)
    _cachedMaintConfig.value = item
    writeMaintConfig(File(baseDir(appCtx), MAINT_CONFIG_FILE), item)
  }

  suspend fun saveTtsVoicesCache(context: Context, list: List<AdminTtsVoice>) = withContext(Dispatchers.IO) {
    val appCtx = context.applicationContext
    ensureLoaded(appCtx)
    _cachedTtsVoices.value = list
    writeTtsVoices(File(baseDir(appCtx), TTS_VOICES_FILE), list)
  }

  suspend fun saveKeyGateCache(context: Context, item: AdminKeyGateConfig) = withContext(Dispatchers.IO) {
    val appCtx = context.applicationContext
    ensureLoaded(appCtx)
    _cachedKeyGate.value = item
    writeKeyGate(File(baseDir(appCtx), KEY_GATE_FILE), item)
  }

  suspend fun saveAccessKeysCache(context: Context, list: List<AdminAccessKey>) = withContext(Dispatchers.IO) {
    val appCtx = context.applicationContext
    ensureLoaded(appCtx)
    _cachedAccessKeys.value = list
    writeAccessKeys(File(baseDir(appCtx), ACCESS_KEYS_FILE), list)
  }

  suspend fun saveUsersCache(context: Context, list: List<AdminUserRecord>) = withContext(Dispatchers.IO) {
    val appCtx = context.applicationContext
    ensureLoaded(appCtx)
    _cachedUsers.value = list
    writeUsers(File(baseDir(appCtx), USERS_FILE), list)
  }

  suspend fun copyMediaFile(context: Context, targetName: String, sourceUri: Uri): String =
    withContext(Dispatchers.IO) {
      val appCtx = context.applicationContext
      val (audioUri, resolvedName) = extractAudioIfVideo(appCtx, sourceUri, targetName)
      val ext = resolvedName.substringAfterLast('.', "mp3").ifBlank { "mp3" }
      val dest = File(mediaDir(appCtx), "${targetName}_${System.currentTimeMillis()}.$ext")
      val bytes =
        if (audioUri.scheme == "file" && audioUri.path != null) {
          File(audioUri.path!!).readBytes()
        } else {
          appCtx.contentResolver.openInputStream(audioUri)?.use { it.readBytes() }
            ?: error("Could not read selected file.")
        }
      dest.writeBytes(bytes)
      dest.absolutePath
    }

  suspend fun copyCompressedImage(context: Context, targetName: String, sourceUri: Uri): String =
    withContext(Dispatchers.IO) {
      val appCtx = context.applicationContext
      val raw =
        if (sourceUri.scheme == "file" && sourceUri.path != null) {
          File(sourceUri.path!!).readBytes()
        } else {
          appCtx.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
            ?: error("Could not read image.")
        }
      val dest = File(mediaDir(appCtx), "${targetName}_${System.currentTimeMillis()}.jpg")
      val decoded = BitmapFactory.decodeByteArray(raw, 0, raw.size)
      if (decoded != null) {
        val maxDim = 512
        val largest = max(decoded.width, decoded.height)
        val scaled =
          if (largest > maxDim) {
            val ratio = maxDim.toFloat() / largest.toFloat()
            Bitmap.createScaledBitmap(
              decoded,
              (decoded.width * ratio).roundToInt().coerceAtLeast(1),
              (decoded.height * ratio).roundToInt().coerceAtLeast(1),
              true,
            )
          } else {
            decoded
          }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
        if (scaled !== decoded) decoded.recycle()
        scaled.recycle()
        dest.writeBytes(out.toByteArray())
      } else {
        dest.writeBytes(raw)
      }
      dest.absolutePath
    }

  private fun extractAudioIfVideo(context: Context, sourceUri: Uri, baseName: String): Pair<Uri, String> {
    val mime = context.contentResolver.getType(sourceUri).orEmpty()
    if (!mime.startsWith("video/")) {
      return sourceUri to baseName
    }
    val outFile = File(mediaDir(context), "extracted_${System.currentTimeMillis()}.m4a")
    val extractor = MediaExtractor()
    extractor.setDataSource(context, sourceUri, null)
    var audioTrack = -1
    var format: MediaFormat? = null
    for (i in 0 until extractor.trackCount) {
      val f = extractor.getTrackFormat(i)
      if (f.getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/")) {
        audioTrack = i
        format = f
        break
      }
    }
    if (audioTrack < 0 || format == null) {
      extractor.release()
      error("Selected video has no audio track.")
    }
    extractor.selectTrack(audioTrack)
    val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    val muxTrack = muxer.addTrack(format)
    muxer.start()
    val buf = ByteBuffer.allocate(256 * 1024)
    val info = android.media.MediaCodec.BufferInfo()
    while (true) {
      val size = extractor.readSampleData(buf, 0)
      if (size < 0) break
      info.offset = 0
      info.size = size
      info.presentationTimeUs = extractor.sampleTime
      @Suppress("WrongConstant")
      info.flags = extractor.sampleFlags
      muxer.writeSampleData(muxTrack, buf, info)
      extractor.advance()
    }
    muxer.stop()
    muxer.release()
    extractor.release()
    return Uri.fromFile(outFile) to outFile.name
  }

  suspend fun enqueueOp(context: Context, op: AdminPendingOp, replacePredicate: ((AdminPendingOp) -> Boolean)? = null) =
    withContext(Dispatchers.IO) {
      val appCtx = context.applicationContext
      ensureLoaded(appCtx)
      val filtered =
        if (replacePredicate != null) _pendingOps.value.filterNot(replacePredicate)
        else _pendingOps.value
      val next = filtered + op
      _pendingOps.value = next
      writePendingOps(File(baseDir(appCtx), PENDING_OPS_FILE), next)
    }

  suspend fun removePendingOp(context: Context, opId: String) = withContext(Dispatchers.IO) {
    val appCtx = context.applicationContext
    ensureLoaded(appCtx)
    val removed = _pendingOps.value.firstOrNull { it.opId == opId }
    if (removed != null) {
      cleanupOpMedia(removed)
    }
    val next = _pendingOps.value.filterNot { it.opId == opId }
    _pendingOps.value = next
    writePendingOps(File(baseDir(appCtx), PENDING_OPS_FILE), next)
  }

  suspend fun removePendingOpsMatching(context: Context, predicate: (AdminPendingOp) -> Boolean) =
    withContext(Dispatchers.IO) {
      val appCtx = context.applicationContext
      ensureLoaded(appCtx)
      _pendingOps.value.filter(predicate).forEach { cleanupOpMedia(it) }
      val next = _pendingOps.value.filterNot(predicate)
      _pendingOps.value = next
      writePendingOps(File(baseDir(appCtx), PENDING_OPS_FILE), next)
    }

  private fun cleanupOpMedia(op: AdminPendingOp) {
    runCatching {
      val o = JSONObject(op.payloadJson)
      listOf("localAudioPath", "localThumbPath", "localAvatarPath", "localImagePath").forEach { key ->
        val p = o.optString(key, "")
        if (p.isNotBlank()) File(p).delete()
      }
    }
  }

  // --- JSON helpers ---

  private fun readSounds(file: File): List<AdminSound> {
    if (!file.exists()) return emptyList()
    return runCatching {
      val arr = JSONArray(file.readText())
      buildList {
        for (i in 0 until arr.length()) {
          val o = arr.getJSONObject(i)
          val id = o.optString("id")
          if (id.isNotBlank()) {
            add(
              AdminSound(
                id = id,
                title = o.optString("title", "Untitled"),
                category = o.optString("category", "Trending"),
                audioUrl = o.optString("audioUrl", ""),
                thumbnailUrl = o.optString("thumbnailUrl", ""),
                emoji = o.optString("emoji", "🔥"),
                defaultVolume = o.optInt("defaultVolume", 100),
                likes = o.optLong("likes", 0L),
                createdBy = o.optString("createdBy", ""),
                createdAt = o.optLong("createdAt", 0L),
              )
            )
          }
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun writeSounds(file: File, items: List<AdminSound>) {
    runCatching {
      val arr = JSONArray()
      items.forEach { s ->
        arr.put(
          JSONObject().apply {
            put("id", s.id)
            put("title", s.title)
            put("category", s.category)
            put("audioUrl", s.audioUrl)
            put("thumbnailUrl", s.thumbnailUrl)
            put("emoji", s.emoji)
            put("defaultVolume", s.defaultVolume)
            put("likes", s.likes)
            put("createdBy", s.createdBy)
            put("createdAt", s.createdAt)
          }
        )
      }
      file.writeText(arr.toString())
    }
  }

  private fun readStrings(file: File): List<String> {
    if (!file.exists()) return emptyList()
    return runCatching {
      val arr = JSONArray(file.readText())
      buildList {
        for (i in 0 until arr.length()) {
          val v = arr.optString(i).trim()
          if (v.isNotBlank()) add(v)
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun writeStrings(file: File, items: List<String>) {
    runCatching {
      val arr = JSONArray()
      items.forEach { arr.put(it) }
      file.writeText(arr.toString())
    }
  }

  private fun readNotice(file: File): AdminNotice {
    if (!file.exists()) return AdminNotice()
    return runCatching {
      val o = JSONObject(file.readText())
      AdminNotice(
        bannerActive = o.optBoolean("bannerActive", false),
        bannerText = o.optString("bannerText", ""),
        popupActive = o.optBoolean("popupActive", false),
        popupTitle = o.optString("popupTitle", ""),
        popupMessage = o.optString("popupMessage", ""),
      )
    }.getOrDefault(AdminNotice())
  }

  private fun writeNotice(file: File, item: AdminNotice) {
    runCatching {
      file.writeText(
        JSONObject().apply {
          put("bannerActive", item.bannerActive)
          put("bannerText", item.bannerText)
          put("popupActive", item.popupActive)
          put("popupTitle", item.popupTitle)
          put("popupMessage", item.popupMessage)
        }.toString()
      )
    }
  }

  private fun readAppConfig(file: File): AdminAppConfig {
    if (!file.exists()) return AdminAppConfig()
    return runCatching {
      val o = JSONObject(file.readText())
      AdminAppConfig(
        logoUrl = o.optString("logoUrl", ""),
        floatingBrandName = o.optString("floatingBrandName", "ZONEX").ifBlank { "ZONEX" },
      )
    }.getOrDefault(AdminAppConfig())
  }

  private fun writeAppConfig(file: File, item: AdminAppConfig) {
    runCatching {
      file.writeText(
        JSONObject().apply {
          put("logoUrl", item.logoUrl)
          put("floatingBrandName", item.floatingBrandName)
        }.toString()
      )
    }
  }

  private fun readDevConfig(file: File): AdminDeveloperConfig {
    if (!file.exists()) return AdminDeveloperConfig()
    return runCatching {
      val o = JSONObject(file.readText())
      val btnArr = o.optJSONArray("buttons") ?: JSONArray()
      val buttons = buildList {
        for (i in 0 until btnArr.length()) {
          val b = btnArr.getJSONObject(i)
          add(
            AdminDevButton(
              label = b.optString("label", ""),
              url = b.optString("url", ""),
              color = b.optString("color", "#F5B82E"),
            )
          )
        }
      }
      AdminDeveloperConfig(
        name = o.optString("name", "Zonex Official"),
        logoUrl = o.optString("logoUrl", ""),
        bio = o.optString("bio", ""),
        buttons = buttons,
      )
    }.getOrDefault(AdminDeveloperConfig())
  }

  private fun writeDevConfig(file: File, item: AdminDeveloperConfig) {
    runCatching {
      val btnArr = JSONArray()
      item.buttons.forEach { b ->
        btnArr.put(
          JSONObject().apply {
            put("label", b.label)
            put("url", b.url)
            put("color", b.color)
          }
        )
      }
      file.writeText(
        JSONObject().apply {
          put("name", item.name)
          put("logoUrl", item.logoUrl)
          put("bio", item.bio)
          put("buttons", btnArr)
        }.toString()
      )
    }
  }

  private fun readUpdateConfig(file: File): AdminAppUpdateConfig {
    if (!file.exists()) return AdminAppUpdateConfig()
    return runCatching {
      val o = JSONObject(file.readText())
      AdminAppUpdateConfig(
        enabled = o.optBoolean("enabled", false),
        latestVersionName = o.optString("latestVersionName", "2.4"),
        latestVersionCode = o.optInt("latestVersionCode", 24),
        targetVersionName = o.optString("targetVersionName", ""),
        forceUpdate = o.optBoolean("forceUpdate", false),
        title = o.optString("title", "নতুন আপডেট এসেছে!"),
        message = o.optString("message", ""),
        downloadUrl = o.optString("downloadUrl", ""),
      )
    }.getOrDefault(AdminAppUpdateConfig())
  }

  private fun writeUpdateConfig(file: File, item: AdminAppUpdateConfig) {
    runCatching {
      file.writeText(
        JSONObject().apply {
          put("enabled", item.enabled)
          put("latestVersionName", item.latestVersionName)
          put("latestVersionCode", item.latestVersionCode)
          put("targetVersionName", item.targetVersionName)
          put("forceUpdate", item.forceUpdate)
          put("title", item.title)
          put("message", item.message)
          put("downloadUrl", item.downloadUrl)
        }.toString()
      )
    }
  }

  private fun readMaintConfig(file: File): AdminMaintenanceConfig {
    if (!file.exists()) return AdminMaintenanceConfig()
    return runCatching {
      val o = JSONObject(file.readText())
      AdminMaintenanceConfig(
        enabled = o.optBoolean("enabled", false),
        title = o.optString("title", "কাজ চলতেছে"),
        message = o.optString("message", "অ্যাপের উন্নয়ন ও সার্ভার মেইনটেন্যান্সের কাজ চলছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"),
      )
    }.getOrDefault(AdminMaintenanceConfig())
  }

  private fun writeMaintConfig(file: File, item: AdminMaintenanceConfig) {
    runCatching {
      file.writeText(
        JSONObject().apply {
          put("enabled", item.enabled)
          put("title", item.title)
          put("message", item.message)
        }.toString()
      )
    }
  }

  private fun readTtsVoices(file: File): List<AdminTtsVoice> {
    if (!file.exists()) return emptyList()
    return runCatching {
      val arr = JSONArray(file.readText())
      buildList {
        for (i in 0 until arr.length()) {
          val o = arr.getJSONObject(i)
          add(
            AdminTtsVoice(
              id = o.optString("id", ""),
              name = o.optString("name", ""),
              cartesiaVoiceId = o.optString("cartesiaVoiceId", ""),
              language = o.optString("language", "Multi"),
              avatarUrl = o.optString("avatarUrl", ""),
              badgeColorHex = o.optString("badgeColorHex", "#16A34A"),
              sortOrder = o.optInt("sortOrder", 0),
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun writeTtsVoices(file: File, items: List<AdminTtsVoice>) {
    runCatching {
      val arr = JSONArray()
      items.forEach { v ->
        arr.put(
          JSONObject().apply {
            put("id", v.id)
            put("name", v.name)
            put("cartesiaVoiceId", v.cartesiaVoiceId)
            put("language", v.language)
            put("avatarUrl", v.avatarUrl)
            put("badgeColorHex", v.badgeColorHex)
            put("sortOrder", v.sortOrder)
          }
        )
      }
      file.writeText(arr.toString())
    }
  }

  private fun readKeyGate(file: File): AdminKeyGateConfig {
    if (!file.exists()) return AdminKeyGateConfig()
    return runCatching {
      val o = JSONObject(file.readText())
      val linksArr = o.optJSONArray("socialLinks") ?: JSONArray()
      val links = buildList {
        for (i in 0 until linksArr.length()) {
          val l = linksArr.getJSONObject(i)
          add(
            AdminKeySocialLink(
              platform = l.optString("platform", "Telegram"),
              label = l.optString("label", ""),
              url = l.optString("url", ""),
              color = l.optString("color", "#0EA5E9"),
            )
          )
        }
      }
      AdminKeyGateConfig(
        requireKeyAfterLogin = o.optBoolean("requireKeyAfterLogin", true),
        getKeyUrl = o.optString("getKeyUrl", ""),
        socialLinks = links,
      )
    }.getOrDefault(AdminKeyGateConfig())
  }

  private fun writeKeyGate(file: File, item: AdminKeyGateConfig) {
    runCatching {
      val arr = JSONArray()
      item.socialLinks.forEach { l ->
        arr.put(
          JSONObject().apply {
            put("platform", l.platform)
            put("label", l.label)
            put("url", l.url)
            put("color", l.color)
          }
        )
      }
      file.writeText(
        JSONObject().apply {
          put("requireKeyAfterLogin", item.requireKeyAfterLogin)
          put("getKeyUrl", item.getKeyUrl)
          put("socialLinks", arr)
        }.toString()
      )
    }
  }

  private fun readAccessKeys(file: File): List<AdminAccessKey> {
    if (!file.exists()) return emptyList()
    return runCatching {
      val arr = JSONArray(file.readText())
      buildList {
        for (i in 0 until arr.length()) {
          val o = arr.getJSONObject(i)
          val code = o.optString("code")
          if (code.isNotBlank()) {
            add(
              AdminAccessKey(
                code = code,
                durationHours = o.optInt("durationHours", 24),
                createdAt = o.optLong("createdAt", 0L),
                activatedAt = o.optLong("activatedAt", 0L),
                expiresAt = o.optLong("expiresAt", 0L),
                boundUid = o.optString("boundUid", ""),
                boundDeviceId = o.optString("boundDeviceId", ""),
                isRevoked = o.optBoolean("isRevoked", false),
              )
            )
          }
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun writeAccessKeys(file: File, items: List<AdminAccessKey>) {
    runCatching {
      val arr = JSONArray()
      items.forEach { k ->
        arr.put(
          JSONObject().apply {
            put("code", k.code)
            put("durationHours", k.durationHours)
            put("createdAt", k.createdAt)
            put("activatedAt", k.activatedAt)
            put("expiresAt", k.expiresAt)
            put("boundUid", k.boundUid)
            put("boundDeviceId", k.boundDeviceId)
            put("isRevoked", k.isRevoked)
          }
        )
      }
      file.writeText(arr.toString())
    }
  }

  private fun readUsers(file: File): List<AdminUserRecord> {
    if (!file.exists()) return emptyList()
    return runCatching {
      val arr = JSONArray(file.readText())
      buildList {
        for (i in 0 until arr.length()) {
          val o = arr.getJSONObject(i)
          val uid = o.optString("uid")
          if (uid.isNotBlank()) {
            val sArr = o.optJSONArray("uploadedSounds") ?: JSONArray()
            val sounds = buildList {
              for (j in 0 until sArr.length()) {
                val s = sArr.getJSONObject(j)
                add(
                  AdminUserSoundItem(
                    id = s.optString("id", ""),
                    title = s.optString("title", "Sound"),
                    category = s.optString("category", ""),
                    audioUrl = s.optString("audioUrl", ""),
                    isPublic = s.optBoolean("isPublic", false),
                    createdAt = s.optLong("createdAt", 0L),
                  )
                )
              }
            }
            add(
              AdminUserRecord(
                uid = uid,
                displayName = o.optString("displayName", ""),
                email = o.optString("email", ""),
                privateCount = o.optInt("privateCount", 0),
                publicUploadCount = o.optInt("publicUploadCount", 0),
                downloadCount = o.optInt("downloadCount", 0),
                favoriteCount = o.optInt("favoriteCount", 0),
                hasSeenOnboarding = o.optBoolean("hasSeenOnboarding", false),
                isAdminAccount = o.optBoolean("isAdminAccount", false),
                uploadedSounds = sounds,
              )
            )
          }
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun writeUsers(file: File, items: List<AdminUserRecord>) {
    runCatching {
      val arr = JSONArray()
      items.forEach { u ->
        val sArr = JSONArray()
        u.uploadedSounds.forEach { s ->
          sArr.put(
            JSONObject().apply {
              put("id", s.id)
              put("title", s.title)
              put("category", s.category)
              put("audioUrl", s.audioUrl)
              put("isPublic", s.isPublic)
              put("createdAt", s.createdAt)
            }
          )
        }
        arr.put(
          JSONObject().apply {
            put("uid", u.uid)
            put("displayName", u.displayName)
            put("email", u.email)
            put("privateCount", u.privateCount)
            put("publicUploadCount", u.publicUploadCount)
            put("downloadCount", u.downloadCount)
            put("favoriteCount", u.favoriteCount)
            put("hasSeenOnboarding", u.hasSeenOnboarding)
            put("isAdminAccount", u.isAdminAccount)
            put("uploadedSounds", sArr)
          }
        )
      }
      file.writeText(arr.toString())
    }
  }

  private fun readPendingOps(file: File): List<AdminPendingOp> {
    if (!file.exists()) return emptyList()
    return runCatching {
      val arr = JSONArray(file.readText())
      buildList {
        for (i in 0 until arr.length()) {
          val o = arr.getJSONObject(i)
          val opId = o.optString("opId")
          val type = o.optString("type")
          if (opId.isNotBlank() && type.isNotBlank()) {
            add(
              AdminPendingOp(
                opId = opId,
                type = type,
                payloadJson = o.optString("payloadJson", "{}"),
                createdAt = o.optLong("createdAt", 0L),
              )
            )
          }
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun writePendingOps(file: File, items: List<AdminPendingOp>) {
    runCatching {
      val arr = JSONArray()
      items.forEach { op ->
        arr.put(
          JSONObject().apply {
            put("opId", op.opId)
            put("type", op.type)
            put("payloadJson", op.payloadJson)
            put("createdAt", op.createdAt)
          }
        )
      }
      file.writeText(arr.toString())
    }
  }
}
