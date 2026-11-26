package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Base64
import com.example.audio.VideoAudioExtractor
import com.example.data.model.AppConfig
import com.example.data.model.AppUpdateConfig
import com.example.data.model.DeveloperButton
import com.example.data.model.DeveloperConfig
import com.example.data.model.KeyGateConfig
import com.example.data.model.KeySocialLink
import com.example.data.model.SoundItem
import com.example.data.model.SoundVisibility
import com.example.data.model.UploadSoundDraft
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class PendingUserUpload(
  val localId: String,
  val uid: String,
  val title: String,
  val category: String,
  val emoji: String,
  val localAudioPath: String,
  val audioFileName: String,
  val directAudioUrl: String,
  val useDirectUrl: Boolean,
  val localThumbPath: String,
  val defaultVolume: Int,
  val addToFavorites: Boolean,
  val visibility: String,
  val createdAt: Long,
) {
  fun toSoundItem(): SoundItem =
    SoundItem(
      id = localId,
      title = title,
      category = category,
      audioUrl = if (useDirectUrl && directAudioUrl.isNotBlank()) directAudioUrl else localAudioPath,
      thumbnailUrl = if (localThumbPath.isNotBlank()) Uri.fromFile(File(localThumbPath)).toString() else "",
      emoji = emoji,
      defaultVolume = defaultVolume,
      likes = 0L,
      createdBy = uid,
      createdAt = createdAt,
      visibility = visibility,
      isDownloaded = true,
    )

  fun toDraft(): UploadSoundDraft =
    UploadSoundDraft(
      title = title,
      category = category,
      emoji = emoji,
      audioUri = if (!useDirectUrl && localAudioPath.isNotBlank()) Uri.fromFile(File(localAudioPath)) else null,
      audioFileName = audioFileName,
      directAudioUrl = directAudioUrl,
      useDirectUrl = useDirectUrl,
      thumbnailUri = if (localThumbPath.isNotBlank()) Uri.fromFile(File(localThumbPath)) else null,
      defaultVolume = defaultVolume,
      addToFavorites = addToFavorites,
      visibility = if (visibility == "private") SoundVisibility.PRIVATE else SoundVisibility.PUBLIC,
    )
}

object OfflineSoundStore {

  private const val DIR_NAME = "offline_sounds"
  private const val INDEX_FILE = "offline_downloads.json"
  private const val FAVS_FILE = "offline_favs.json"
  private const val PUBLIC_CACHE_FILE = "cached_public_sounds.json"
  private const val CATEGORIES_CACHE_FILE = "cached_categories.json"
  private const val PENDING_UPLOADS_FILE = "pending_uploads.json"
  private const val KEY_GATE_CACHE_FILE = "cached_key_gate.json"
  private const val DEVELOPER_CACHE_FILE = "cached_developer.json"
  private const val APP_CONFIG_CACHE_FILE = "cached_app_config.json"
  private const val APP_UPDATE_CACHE_FILE = "cached_app_update.json"

  private val httpClient =
    OkHttpClient.Builder()
      .connectTimeout(25, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .followRedirects(true)
      .build()

  private val _offlineDownloads = MutableStateFlow<List<SoundItem>>(emptyList())
  val offlineDownloads: StateFlow<List<SoundItem>> = _offlineDownloads.asStateFlow()

  private val _offlineFavoriteIds = MutableStateFlow<Set<String>>(emptySet())
  val offlineFavoriteIds: StateFlow<Set<String>> = _offlineFavoriteIds.asStateFlow()

  private val _cachedPublicSounds = MutableStateFlow<List<SoundItem>>(emptyList())
  val cachedPublicSounds: StateFlow<List<SoundItem>> = _cachedPublicSounds.asStateFlow()

  private val _cachedPrivateSounds = MutableStateFlow<List<SoundItem>>(emptyList())
  val cachedPrivateSounds: StateFlow<List<SoundItem>> = _cachedPrivateSounds.asStateFlow()

  private val _cachedCategories = MutableStateFlow<List<String>>(listOf("Funny", "Reaction", "Meme"))
  val cachedCategories: StateFlow<List<String>> = _cachedCategories.asStateFlow()

  private val _pendingUploads = MutableStateFlow<List<PendingUserUpload>>(emptyList())
  val pendingUploads: StateFlow<List<PendingUserUpload>> = _pendingUploads.asStateFlow()

  private val _cachedKeyGateConfig = MutableStateFlow(KeyGateConfig())
  val cachedKeyGateConfig: StateFlow<KeyGateConfig> = _cachedKeyGateConfig.asStateFlow()

  private val _cachedDeveloperConfig = MutableStateFlow(DeveloperConfig())
  val cachedDeveloperConfig: StateFlow<DeveloperConfig> = _cachedDeveloperConfig.asStateFlow()

  private val _cachedAppConfig = MutableStateFlow(AppConfig())
  val cachedAppConfig: StateFlow<AppConfig> = _cachedAppConfig.asStateFlow()

  private val _cachedAppUpdateConfig = MutableStateFlow(AppUpdateConfig())
  val cachedAppUpdateConfig: StateFlow<AppUpdateConfig> = _cachedAppUpdateConfig.asStateFlow()

  @Volatile
  private var initialized = false

  fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    val hasTransport =
      caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    return hasTransport && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  fun ensureLoaded(context: Context) {
    if (initialized) return
    synchronized(this) {
      if (initialized) return
      val appCtx = context.applicationContext
      _offlineDownloads.value = readSoundsFile(File(baseDir(appCtx), INDEX_FILE), forceDownloaded = true)
      _offlineFavoriteIds.value = readFavsFile(appCtx)
      _cachedPublicSounds.value = readSoundsFile(File(baseDir(appCtx), PUBLIC_CACHE_FILE), forceDownloaded = false)
      val savedCats = readStringListFile(File(baseDir(appCtx), CATEGORIES_CACHE_FILE))
      if (savedCats.isNotEmpty()) {
        _cachedCategories.value = savedCats
      }
      _pendingUploads.value = readPendingUploadsFile(appCtx)
      _cachedKeyGateConfig.value = readKeyGateFile(File(baseDir(appCtx), KEY_GATE_CACHE_FILE))
      _cachedDeveloperConfig.value = readDeveloperFile(File(baseDir(appCtx), DEVELOPER_CACHE_FILE))
      _cachedAppConfig.value = readAppConfigFile(File(baseDir(appCtx), APP_CONFIG_CACHE_FILE))
      _cachedAppUpdateConfig.value = readAppUpdateFile(File(baseDir(appCtx), APP_UPDATE_CACHE_FILE))
      initialized = true
    }
  }

  fun loadCachedPrivateForUser(context: Context, uid: String) {
    if (uid.isBlank()) {
      _cachedPrivateSounds.value = emptyList()
      return
    }
    ensureLoaded(context)
    val file = File(baseDir(context), "cached_private_${sanitizeId(uid)}.json")
    _cachedPrivateSounds.value = readSoundsFile(file, forceDownloaded = false)
  }

  suspend fun saveCachedPublicSounds(context: Context, items: List<SoundItem>) =
    withContext(Dispatchers.IO) {
      val appCtx = context.applicationContext
      ensureLoaded(appCtx)
      _cachedPublicSounds.value = items
      writeSoundsFile(File(baseDir(appCtx), PUBLIC_CACHE_FILE), items)
    }

  suspend fun saveCachedPrivateSounds(context: Context, uid: String, items: List<SoundItem>) =
    withContext(Dispatchers.IO) {
      if (uid.isBlank()) return@withContext
      val appCtx = context.applicationContext
      ensureLoaded(appCtx)
      _cachedPrivateSounds.value = items
      writeSoundsFile(File(baseDir(appCtx), "cached_private_${sanitizeId(uid)}.json"), items)
    }

  suspend fun saveCachedCategories(context: Context, categories: List<String>) =
    withContext(Dispatchers.IO) {
      val appCtx = context.applicationContext
      ensureLoaded(appCtx)
      _cachedCategories.value = categories
      writeStringListFile(File(baseDir(appCtx), CATEGORIES_CACHE_FILE), categories)
    }

  suspend fun saveCachedKeyGateConfig(context: Context, config: KeyGateConfig) =
    withContext(Dispatchers.IO) {
      val appCtx = context.applicationContext
      ensureLoaded(appCtx)
      _cachedKeyGateConfig.value = config
      writeKeyGateFile(File(baseDir(appCtx), KEY_GATE_CACHE_FILE), config)
    }

  suspend fun saveCachedDeveloperConfig(context: Context, config: DeveloperConfig) =
    withContext(Dispatchers.IO) {
      val appCtx = context.applicationContext
      ensureLoaded(appCtx)
      _cachedDeveloperConfig.value = config
      writeDeveloperFile(File(baseDir(appCtx), DEVELOPER_CACHE_FILE), config)
    }

  suspend fun saveCachedAppConfig(context: Context, config: AppConfig) =
    withContext(Dispatchers.IO) {
      val appCtx = context.applicationContext
      ensureLoaded(appCtx)
      _cachedAppConfig.value = config
      writeAppConfigFile(File(baseDir(appCtx), APP_CONFIG_CACHE_FILE), config)
    }

  suspend fun saveCachedAppUpdateConfig(context: Context, config: AppUpdateConfig) =
    withContext(Dispatchers.IO) {
      val appCtx = context.applicationContext
      ensureLoaded(appCtx)
      _cachedAppUpdateConfig.value = config
      writeAppUpdateFile(File(baseDir(appCtx), APP_UPDATE_CACHE_FILE), config)
    }

  suspend fun enqueuePendingUpload(
    context: Context,
    uid: String,
    draft: UploadSoundDraft,
  ): Result<PendingUserUpload> =
    withContext(Dispatchers.IO) {
      runCatching {
        val appCtx = context.applicationContext
        ensureLoaded(appCtx)
        val localId = "offline_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val dir = File(baseDir(appCtx), "pending_media").apply { mkdirs() }

        var localAudioPath = ""
        var audioName = draft.audioFileName
        if (!draft.useDirectUrl) {
          val rawUri = draft.audioUri ?: error("No audio selected.")
          val (extractedUri, extName) =
            VideoAudioExtractor.extractAudioIfVideo(appCtx, rawUri).getOrThrow()
          val cleanName =
            (if (extName.startsWith("extracted_")) extName else draft.audioFileName.ifBlank { "audio.mp3" })
              .replace(Regex("[^a-zA-Z0-9._-]"), "_")
          audioName = cleanName
          val ext = cleanName.substringAfterLast('.', "mp3").ifBlank { "mp3" }
          val audioDest = File(dir, "${localId}.$ext")
          val extractedPath = extractedUri.path
          val bytes =
            if (extractedUri.scheme == "file" && extractedPath != null) {
              File(extractedPath).readBytes()
            } else {
              appCtx.contentResolver.openInputStream(extractedUri)?.use { it.readBytes() }
                ?: error("Cannot read selected audio file.")
            }
          audioDest.writeBytes(bytes)
          localAudioPath = audioDest.absolutePath

          // Also mirror to offline audio cache so playback & Floating Window find it by localId
          removeLocalAudioFiles(appCtx, localId)
          val cachedPlayTarget = File(baseDir(appCtx), "${sanitizeId(localId)}.$ext")
          audioDest.copyTo(cachedPlayTarget, overwrite = true)
        }

        var localThumbPath = ""
        if (draft.thumbnailUri != null) {
          val thumbPath = draft.thumbnailUri.path
          val thumbRaw =
            if (draft.thumbnailUri.scheme == "file" && thumbPath != null) {
              File(thumbPath).readBytes()
            } else {
              appCtx.contentResolver.openInputStream(draft.thumbnailUri)?.use { it.readBytes() }
            }
          if (thumbRaw != null && thumbRaw.isNotEmpty()) {
            val decoded = BitmapFactory.decodeByteArray(thumbRaw, 0, thumbRaw.size)
            val thumbDest = File(dir, "${localId}_thumb.jpg")
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
              thumbDest.writeBytes(out.toByteArray())
            } else {
              thumbDest.writeBytes(thumbRaw)
            }
            localThumbPath = thumbDest.absolutePath
          }
        }

        val item =
          PendingUserUpload(
            localId = localId,
            uid = uid,
            title = draft.title.trim(),
            category = draft.category,
            emoji = draft.emoji,
            localAudioPath = localAudioPath,
            audioFileName = audioName.ifBlank { "audio.mp3" },
            directAudioUrl = draft.directAudioUrl.trim(),
            useDirectUrl = draft.useDirectUrl,
            localThumbPath = localThumbPath,
            defaultVolume = draft.defaultVolume.coerceIn(0, 100),
            addToFavorites = draft.addToFavorites,
            visibility = draft.visibility.value,
            createdAt = System.currentTimeMillis(),
          )

        val next = listOf(item) + _pendingUploads.value
        _pendingUploads.value = next
        writePendingUploadsFile(appCtx, next)

        val soundItem = item.toSoundItem()
        upsertIndexEntry(appCtx, soundItem)
        if (draft.addToFavorites) {
          setFavoriteLocal(appCtx, localId, true)
        }
        item
      }
    }

  suspend fun completePendingUpload(
    context: Context,
    pending: PendingUserUpload,
    uploaded: SoundItem,
  ) =
    withContext(Dispatchers.IO) {
      val appCtx = context.applicationContext
      ensureLoaded(appCtx)

      // Migrate offline playable audio from localId -> uploaded.id
      val oldAudioFile = findLocalAudioFile(appCtx, pending.localId)
      if (oldAudioFile != null && oldAudioFile.exists()) {
        val ext = oldAudioFile.extension.ifBlank { "mp3" }
        val newTarget = File(baseDir(appCtx), "${sanitizeId(uploaded.id)}.$ext")
        oldAudioFile.copyTo(newTarget, overwrite = true)
      }
      removeSound(appCtx, pending.localId)
      upsertIndexEntry(appCtx, uploaded.copy(isDownloaded = true))

      if (pending.addToFavorites || _offlineFavoriteIds.value.contains(pending.localId)) {
        val nextFavs = (_offlineFavoriteIds.value - pending.localId) + uploaded.id
        _offlineFavoriteIds.value = nextFavs
        writeFavsFile(appCtx, nextFavs)
      }

      if (pending.localAudioPath.isNotBlank()) runCatching { File(pending.localAudioPath).delete() }
      if (pending.localThumbPath.isNotBlank()) runCatching { File(pending.localThumbPath).delete() }

      val nextPending = _pendingUploads.value.filterNot { it.localId == pending.localId }
      _pendingUploads.value = nextPending
      writePendingUploadsFile(appCtx, nextPending)
    }

  suspend fun removePendingUpload(context: Context, localId: String) =
    withContext(Dispatchers.IO) {
      val appCtx = context.applicationContext
      ensureLoaded(appCtx)
      val existing = _pendingUploads.value.firstOrNull { it.localId == localId }
      if (existing != null) {
        if (existing.localAudioPath.isNotBlank()) runCatching { File(existing.localAudioPath).delete() }
        if (existing.localThumbPath.isNotBlank()) runCatching { File(existing.localThumbPath).delete() }
      }
      val next = _pendingUploads.value.filterNot { it.localId == localId }
      _pendingUploads.value = next
      writePendingUploadsFile(appCtx, next)
      removeSound(appCtx, localId)
    }

  private fun baseDir(context: Context): File =
    File(context.applicationContext.filesDir, DIR_NAME).apply { mkdirs() }

  private fun sanitizeId(id: String): String =
    id.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "sound_${id.hashCode()}" }

  fun findLocalAudioFile(context: Context, soundId: String): File? {
    if (soundId.isBlank()) return null
    val dir = baseDir(context)
    val prefix = "${sanitizeId(soundId)}."
    val match =
      dir.listFiles()?.firstOrNull { file ->
        file.isFile && file.name.startsWith(prefix) && file.length() > 0L
      }
    return match
  }

  fun hasLocalAudio(context: Context, soundId: String): Boolean =
    findLocalAudioFile(context, soundId) != null

  suspend fun cacheSoundFromSource(context: Context, sound: SoundItem): Result<SoundItem> =
    withContext(Dispatchers.IO) {
      runCatching {
        val appCtx = context.applicationContext
        ensureLoaded(appCtx)
        val existing = findLocalAudioFile(appCtx, sound.id)
        if (existing != null && existing.length() > 0L) {
          val updated = sound.copy(isDownloaded = true)
          upsertIndexEntry(appCtx, updated)
          return@runCatching updated
        }

        val src = sound.audioUrl.trim()
        if (src.isBlank()) error("Audio URL is empty.")

        val (bytes, ext) =
          when {
            src.startsWith("data:", ignoreCase = true) -> {
              val b64 = src.substringAfter("base64,", "")
              val decoded = Base64.decode(b64, Base64.DEFAULT)
              val extension = guessExtensionFromMimeOrUrl(src.substringBefore(";"))
              decoded to extension
            }
            src.startsWith("http://", ignoreCase = true) || src.startsWith("https://", ignoreCase = true) -> {
              val req = Request.Builder().url(src).get().build()
              httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                  error("Download failed (HTTP ${resp.code})")
                }
                val bodyBytes = resp.body?.bytes() ?: error("Empty audio response")
                if (bodyBytes.isEmpty()) error("Downloaded file is empty")
                val contentType = resp.header("Content-Type").orEmpty()
                val extension = guessExtensionFromMimeOrUrl("$contentType $src")
                bodyBytes to extension
              }
            }
            src.startsWith("file://", ignoreCase = true) || src.startsWith("/") -> {
              val path = if (src.startsWith("file://")) Uri.parse(src).path.orEmpty() else src
              val file = File(path)
              if (!file.exists() || file.length() == 0L) error("Local audio file missing")
              val extension = file.extension.ifBlank { "mp3" }
              file.readBytes() to extension
            }
            src.startsWith("content://", ignoreCase = true) -> {
              val data =
                appCtx.contentResolver.openInputStream(Uri.parse(src))?.use { it.readBytes() }
                  ?: error("Cannot read audio content URI")
              data to "mp3"
            }
            else -> error("Unsupported audio source")
          }

        removeLocalAudioFiles(appCtx, sound.id)
        val target = File(baseDir(appCtx), "${sanitizeId(sound.id)}.$ext")
        target.writeBytes(bytes)

        val savedItem = sound.copy(isDownloaded = true)
        upsertIndexEntry(appCtx, savedItem)
        savedItem
      }
    }

  suspend fun cacheLocalFileForSound(context: Context, sound: SoundItem, sourceFile: File): Result<SoundItem> =
    withContext(Dispatchers.IO) {
      runCatching {
        val appCtx = context.applicationContext
        ensureLoaded(appCtx)
        if (!sourceFile.exists() || sourceFile.length() == 0L) {
          error("Audio file not found")
        }
        val ext = sourceFile.extension.ifBlank { "wav" }
        removeLocalAudioFiles(appCtx, sound.id)
        val target = File(baseDir(appCtx), "${sanitizeId(sound.id)}.$ext")
        sourceFile.copyTo(target, overwrite = true)
        val updated = sound.copy(isDownloaded = true)
        upsertIndexEntry(appCtx, updated)
        updated
      }
    }

  suspend fun upsertMetadataOnlyIfCached(context: Context, sound: SoundItem) =
    withContext(Dispatchers.IO) {
      val appCtx = context.applicationContext
      ensureLoaded(appCtx)
      upsertIndexEntry(appCtx, sound.copy(isDownloaded = true))
    }

  suspend fun removeSound(context: Context, soundId: String) =
    withContext(Dispatchers.IO) {
      val appCtx = context.applicationContext
      ensureLoaded(appCtx)
      removeLocalAudioFiles(appCtx, soundId)
      val next = _offlineDownloads.value.filterNot { it.id == soundId }
      _offlineDownloads.value = next
      writeSoundsFile(File(baseDir(appCtx), INDEX_FILE), next)
      if (_offlineFavoriteIds.value.contains(soundId)) {
        val nextFavs = _offlineFavoriteIds.value - soundId
        _offlineFavoriteIds.value = nextFavs
        writeFavsFile(appCtx, nextFavs)
      }
    }

  suspend fun setFavoriteLocal(context: Context, soundId: String, favorite: Boolean) =
    withContext(Dispatchers.IO) {
      val appCtx = context.applicationContext
      ensureLoaded(appCtx)
      val next =
        if (favorite) _offlineFavoriteIds.value + soundId
        else _offlineFavoriteIds.value - soundId
      _offlineFavoriteIds.value = next
      writeFavsFile(appCtx, next)
    }

  suspend fun replaceRemoteFavorites(context: Context, remoteFavIds: Set<String>) =
    withContext(Dispatchers.IO) {
      val appCtx = context.applicationContext
      ensureLoaded(appCtx)
      val pendingFavIds =
        _pendingUploads.value.filter { it.addToFavorites }.map { it.localId }.toSet()
      val merged = remoteFavIds + pendingFavIds
      if (merged != _offlineFavoriteIds.value) {
        _offlineFavoriteIds.value = merged
        writeFavsFile(appCtx, merged)
      }
    }

  private fun removeLocalAudioFiles(context: Context, soundId: String) {
    val prefix = "${sanitizeId(soundId)}."
    baseDir(context).listFiles()?.forEach { file ->
      if (file.isFile && file.name.startsWith(prefix)) {
        runCatching { file.delete() }
      }
    }
  }

  private fun guessExtensionFromMimeOrUrl(hint: String): String {
    val h = hint.lowercase()
    return when {
      h.contains("wav") -> "wav"
      h.contains("m4a") || h.contains("mp4") || h.contains("aac") -> "m4a"
      h.contains("ogg") || h.contains("opus") -> "ogg"
      else -> "mp3"
    }
  }

  private fun upsertIndexEntry(context: Context, item: SoundItem) {
    val current = _offlineDownloads.value.filterNot { it.id == item.id }
    val next = listOf(item) + current
    _offlineDownloads.value = next
    writeSoundsFile(File(baseDir(context), INDEX_FILE), next)
  }

  private fun readSoundsFile(file: File, forceDownloaded: Boolean): List<SoundItem> {
    if (!file.exists()) return emptyList()
    return runCatching {
      val arr = JSONArray(file.readText())
      buildList {
        for (i in 0 until arr.length()) {
          val o = arr.getJSONObject(i)
          val id = o.optString("id")
          if (id.isNotBlank()) {
            add(
              SoundItem(
                id = id,
                title = o.optString("title", "Untitled"),
                category = o.optString("category", "Saved"),
                audioUrl = o.optString("audioUrl", ""),
                thumbnailUrl = o.optString("thumbnailUrl", ""),
                emoji = o.optString("emoji", "🔊"),
                defaultVolume = o.optInt("defaultVolume", 100),
                likes = o.optLong("likes", 0L),
                createdBy = o.optString("createdBy", ""),
                createdAt = o.optLong("createdAt", 0L),
                visibility = o.optString("visibility", "public"),
                isDownloaded = if (forceDownloaded) true else o.optBoolean("isDownloaded", false),
              )
            )
          }
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun writeSoundsFile(file: File, items: List<SoundItem>) {
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
            put("visibility", s.visibility)
            put("isDownloaded", s.isDownloaded)
          }
        )
      }
      file.writeText(arr.toString())
    }
  }

  private fun readStringListFile(file: File): List<String> {
    if (!file.exists()) return emptyList()
    return runCatching {
      val arr = JSONArray(file.readText())
      buildList {
        for (i in 0 until arr.length()) {
          val s = arr.optString(i).trim()
          if (s.isNotBlank()) add(s)
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun writeStringListFile(file: File, items: List<String>) {
    runCatching {
      val arr = JSONArray()
      items.forEach { arr.put(it) }
      file.writeText(arr.toString())
    }
  }

  private fun readPendingUploadsFile(context: Context): List<PendingUserUpload> {
    val file = File(baseDir(context), PENDING_UPLOADS_FILE)
    if (!file.exists()) return emptyList()
    return runCatching {
      val arr = JSONArray(file.readText())
      buildList {
        for (i in 0 until arr.length()) {
          val o = arr.getJSONObject(i)
          val id = o.optString("localId")
          if (id.isNotBlank()) {
            add(
              PendingUserUpload(
                localId = id,
                uid = o.optString("uid", ""),
                title = o.optString("title", "Sound"),
                category = o.optString("category", "Trending"),
                emoji = o.optString("emoji", "🔥"),
                localAudioPath = o.optString("localAudioPath", ""),
                audioFileName = o.optString("audioFileName", "audio.mp3"),
                directAudioUrl = o.optString("directAudioUrl", ""),
                useDirectUrl = o.optBoolean("useDirectUrl", false),
                localThumbPath = o.optString("localThumbPath", ""),
                defaultVolume = o.optInt("defaultVolume", 90),
                addToFavorites = o.optBoolean("addToFavorites", true),
                visibility = o.optString("visibility", "public"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
              )
            )
          }
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun writePendingUploadsFile(context: Context, items: List<PendingUserUpload>) {
    runCatching {
      val arr = JSONArray()
      items.forEach { p ->
        arr.put(
          JSONObject().apply {
            put("localId", p.localId)
            put("uid", p.uid)
            put("title", p.title)
            put("category", p.category)
            put("emoji", p.emoji)
            put("localAudioPath", p.localAudioPath)
            put("audioFileName", p.audioFileName)
            put("directAudioUrl", p.directAudioUrl)
            put("useDirectUrl", p.useDirectUrl)
            put("localThumbPath", p.localThumbPath)
            put("defaultVolume", p.defaultVolume)
            put("addToFavorites", p.addToFavorites)
            put("visibility", p.visibility)
            put("createdAt", p.createdAt)
          }
        )
      }
      File(baseDir(context), PENDING_UPLOADS_FILE).writeText(arr.toString())
    }
  }

  private fun readFavsFile(context: Context): Set<String> {
    val file = File(baseDir(context), FAVS_FILE)
    if (!file.exists()) return emptySet()
    return runCatching {
      val arr = JSONArray(file.readText())
      buildSet {
        for (i in 0 until arr.length()) {
          val v = arr.optString(i)
          if (v.isNotBlank()) add(v)
        }
      }
    }.getOrDefault(emptySet())
  }

  private fun writeFavsFile(context: Context, ids: Set<String>) {
    runCatching {
      val arr = JSONArray()
      ids.forEach { arr.put(it) }
      File(baseDir(context), FAVS_FILE).writeText(arr.toString())
    }
  }

  private fun readKeyGateFile(file: File): KeyGateConfig {
    if (!file.exists()) return KeyGateConfig()
    return runCatching {
      val o = JSONObject(file.readText())
      val arr = o.optJSONArray("socialLinks") ?: JSONArray()
      val links = buildList {
        for (i in 0 until arr.length()) {
          val l = arr.getJSONObject(i)
          val url = l.optString("url", "")
          if (url.isNotBlank()) {
            add(
              KeySocialLink(
                platform = l.optString("platform", "Telegram"),
                label = l.optString("label", ""),
                url = url,
                color = l.optString("color", "#0EA5E9"),
              )
            )
          }
        }
      }
      KeyGateConfig(
        requireKeyAfterLogin = o.optBoolean("requireKeyAfterLogin", true),
        getKeyUrl = o.optString("getKeyUrl", ""),
        socialLinks = links,
      )
    }.getOrDefault(KeyGateConfig())
  }

  private fun writeKeyGateFile(file: File, cfg: KeyGateConfig) {
    runCatching {
      val arr = JSONArray()
      cfg.socialLinks.forEach { l ->
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
          put("requireKeyAfterLogin", cfg.requireKeyAfterLogin)
          put("getKeyUrl", cfg.getKeyUrl)
          put("socialLinks", arr)
        }.toString()
      )
    }
  }

  private fun readDeveloperFile(file: File): DeveloperConfig {
    if (!file.exists()) return DeveloperConfig()
    return runCatching {
      val o = JSONObject(file.readText())
      val arr = o.optJSONArray("buttons") ?: JSONArray()
      val btns = buildList {
        for (i in 0 until arr.length()) {
          val b = arr.getJSONObject(i)
          val label = b.optString("label", "")
          val url = b.optString("url", "")
          if (label.isNotBlank() && url.isNotBlank()) {
            add(DeveloperButton(label = label, url = url, color = b.optString("color", "#F5B82E")))
          }
        }
      }
      DeveloperConfig(
        name = o.optString("name", "Zonex Official").ifBlank { "Zonex Official" },
        logoUrl = o.optString("logoUrl", ""),
        bio = o.optString("bio", ""),
        buttons = btns,
      )
    }.getOrDefault(DeveloperConfig())
  }

  private fun writeDeveloperFile(file: File, cfg: DeveloperConfig) {
    runCatching {
      val arr = JSONArray()
      cfg.buttons.forEach { b ->
        arr.put(
          JSONObject().apply {
            put("label", b.label)
            put("url", b.url)
            put("color", b.color)
          }
        )
      }
      file.writeText(
        JSONObject().apply {
          put("name", cfg.name)
          put("logoUrl", cfg.logoUrl)
          put("bio", cfg.bio)
          put("buttons", arr)
        }.toString()
      )
    }
  }

  private fun readAppConfigFile(file: File): AppConfig {
    if (!file.exists()) return AppConfig()
    return runCatching {
      val o = JSONObject(file.readText())
      AppConfig(
        logoUrl = o.optString("logoUrl", ""),
        floatingBrandName = o.optString("floatingBrandName", "ZONEX").ifBlank { "ZONEX" },
      )
    }.getOrDefault(AppConfig())
  }

  private fun writeAppConfigFile(file: File, cfg: AppConfig) {
    runCatching {
      file.writeText(
        JSONObject().apply {
          put("logoUrl", cfg.logoUrl)
          put("floatingBrandName", cfg.floatingBrandName)
        }.toString()
      )
    }
  }

  private fun readAppUpdateFile(file: File): AppUpdateConfig {
    if (!file.exists()) return AppUpdateConfig()
    return runCatching {
      val o = JSONObject(file.readText())
      AppUpdateConfig(
        enabled = o.optBoolean("enabled", false),
        latestVersionName = o.optString("latestVersionName", "2.4"),
        latestVersionCode = o.optInt("latestVersionCode", 24),
        targetVersionName = o.optString("targetVersionName", ""),
        forceUpdate = o.optBoolean("forceUpdate", false),
        title = o.optString("title", "নতুন আপডেট এসেছে!"),
        message = o.optString("message", ""),
        downloadUrl = o.optString("downloadUrl", ""),
      )
    }.getOrDefault(AppUpdateConfig())
  }

  private fun writeAppUpdateFile(file: File, cfg: AppUpdateConfig) {
    runCatching {
      file.writeText(
        JSONObject().apply {
          put("enabled", cfg.enabled)
          put("latestVersionName", cfg.latestVersionName)
          put("latestVersionCode", cfg.latestVersionCode)
          put("targetVersionName", cfg.targetVersionName)
          put("forceUpdate", cfg.forceUpdate)
          put("title", cfg.title)
          put("message", cfg.message)
          put("downloadUrl", cfg.downloadUrl)
        }.toString()
      )
    }
  }
}
