package com.zonex.admin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Base64
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AdminRepository {

  private val app: FirebaseApp by lazy { FirebaseApp.getInstance() }
  private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance(app) }
  private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance(app) }
  private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance(app) }

  val authEmailFlow: Flow<String?> = callbackFlow {
    val listener =
      FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser?.email ?: firebaseAuth.currentUser?.uid)
      }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  fun currentUid(): String? = auth.currentUser?.uid

  suspend fun signIn(email: String, password: String): Result<String> =
    runCatching {
      val res = auth.signInWithEmailAndPassword(email.trim(), password).await()
      res.user?.email ?: res.user?.uid ?: error("Authentication failed")
    }

  fun signOut() {
    auth.signOut()
  }

  fun observePublicSounds(): Flow<List<AdminSound>> = callbackFlow {
    val ref = database.reference.child("sounds").child("public")
    val listener =
      object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val list =
            snapshot.children
              .mapNotNull { parseSound(it) }
              .sortedByDescending { it.createdAt }
          trySend(list)
        }

        override fun onCancelled(error: DatabaseError) {}
      }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  fun observeNotice(): Flow<AdminNotice> = callbackFlow {
    val ref = database.reference.child("config").child("notice")
    val listener =
      object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          trySend(
            AdminNotice(
              bannerActive = snapshot.child("bannerActive").getValue(Boolean::class.java) == true,
              bannerText = snapshot.child("bannerText").getValue(String::class.java).orEmpty(),
              popupActive = snapshot.child("popupActive").getValue(Boolean::class.java) == true,
              popupTitle = snapshot.child("popupTitle").getValue(String::class.java).orEmpty(),
              popupMessage = snapshot.child("popupMessage").getValue(String::class.java).orEmpty(),
            )
          )
        }

        override fun onCancelled(error: DatabaseError) {}
      }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  fun observeAppConfig(): Flow<AdminAppConfig> = callbackFlow {
    val ref = database.reference.child("config").child("app")
    val listener =
      object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          trySend(
            AdminAppConfig(
              logoUrl = snapshot.child("logoUrl").getValue(String::class.java).orEmpty(),
              floatingBrandName =
                snapshot.child("floatingBrandName").getValue(String::class.java)
                  ?.ifBlank { "ZONEX" } ?: "ZONEX",
            )
          )
        }

        override fun onCancelled(error: DatabaseError) {}
      }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  fun observeDeveloperConfig(): Flow<AdminDeveloperConfig> = callbackFlow {
    val ref = database.reference.child("config").child("developer")
    val listener =
      object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val buttons =
            snapshot.child("buttons").children.mapNotNull { b ->
              val label = b.child("label").getValue(String::class.java).orEmpty()
              val url = b.child("url").getValue(String::class.java).orEmpty()
              val color = b.child("color").getValue(String::class.java) ?: "#F5B82E"
              if (label.isNotBlank() && url.isNotBlank()) AdminDevButton(label, url, color) else null
            }
          trySend(
            AdminDeveloperConfig(
              name = snapshot.child("name").getValue(String::class.java) ?: "Zonex Official",
              logoUrl = snapshot.child("logoUrl").getValue(String::class.java).orEmpty(),
              bio = snapshot.child("bio").getValue(String::class.java).orEmpty(),
              buttons = buttons,
            )
          )
        }

        override fun onCancelled(error: DatabaseError) {}
      }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  fun observeCategories(): Flow<List<String>> = callbackFlow {
    val ref = database.reference.child("config").child("categories")
    val listener =
      object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val defaults = listOf("Funny", "Reaction", "Meme")
          if (!snapshot.exists()) {
            trySend(defaults)
            return
          }
          val initialized = snapshot.child("initialized").getValue(Boolean::class.java) == true
          val itemsSnap = if (snapshot.hasChild("items")) snapshot.child("items") else snapshot
          val parsed =
            itemsSnap.children
              .mapNotNull { child ->
                if (child.key == "initialized") null
                else child.getValue(String::class.java)?.trim()?.takeIf {
                  it.isNotBlank() &&
                    !it.equals("All", ignoreCase = true) &&
                    !it.equals("Trending", ignoreCase = true)
                }
              }
              .distinct()
          trySend(if (initialized) parsed else parsed.ifEmpty { defaults })
        }

        override fun onCancelled(error: DatabaseError) {}
      }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  suspend fun saveCategories(categories: List<String>): Result<Unit> =
    runCatching {
      val clean =
        categories
          .map { it.trim() }
          .filter {
            it.isNotBlank() &&
              !it.equals("All", ignoreCase = true) &&
              !it.equals("Trending", ignoreCase = true)
          }
          .distinctBy { it.lowercase() }
      database.reference
        .child("config")
        .child("categories")
        .setValue(mapOf("initialized" to true, "items" to clean))
        .await()
    }

  fun observeUsers(): Flow<List<AdminUserRecord>> = callbackFlow {
    val ref = database.reference.child("users")
    val listener =
      object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val list =
            snapshot.children.mapNotNull { u ->
              val uid = u.key ?: return@mapNotNull null
              val privItems =
                u.child("privateSounds").children.mapNotNull { p ->
                  val id = p.key ?: return@mapNotNull null
                  val title = p.child("title").getValue(String::class.java)?.ifBlank { "Sound" } ?: "Sound"
                  val cat = p.child("category").getValue(String::class.java).orEmpty()
                  val audioUrl = p.child("audioUrl").getValue(String::class.java).orEmpty()
                  val createdAt = (p.child("createdAt").value as? Number)?.toLong() ?: 0L
                  AdminUserSoundItem(
                    id = id,
                    title = title,
                    category = cat,
                    audioUrl = audioUrl,
                    isPublic = false,
                    createdAt = createdAt,
                  )
                }
              AdminUserRecord(
                uid = uid,
                displayName = u.child("displayName").getValue(String::class.java).orEmpty(),
                email = u.child("email").getValue(String::class.java).orEmpty(),
                privateCount = privItems.size,
                downloadCount = u.child("downloads").childrenCount.toInt(),
                favoriteCount = u.child("favorites").childrenCount.toInt(),
                hasSeenOnboarding = u.child("hasSeenOnboarding").getValue(Boolean::class.java) == true,
                uploadedSounds = privItems,
              )
            }
          trySend(list)
        }

        override fun onCancelled(error: DatabaseError) {}
      }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  fun newPublicSoundId(): String =
    database.reference.child("sounds").child("public").push().key
      ?: "pub_${System.currentTimeMillis()}_${(1000..9999).random()}"

  suspend fun savePublicSound(
    context: Context,
    draft: AdminSoundDraft,
    forcedId: String? = null,
    isNewSound: Boolean = draft.editingId == null,
    onProgress: (Float) -> Unit,
  ): Result<Unit> =
    runCatching {
      val uid = auth.currentUser?.uid ?: "admin"
      auth.currentUser?.getIdToken(false)?.await()
      val soundId =
        forcedId?.takeIf { it.isNotBlank() }
          ?: draft.editingId
          ?: newPublicSoundId()

      onProgress(0.15f)
      val finalAudioUrl =
        when {
          draft.useDirectUrl && draft.directAudioUrl.isNotBlank() -> draft.directAudioUrl.trim()
          draft.audioUri != null -> {
            val (uploadUri, name) = extractAudioIfVideo(context, draft.audioUri)
            val safeName =
              (if (name.startsWith("extracted_")) name else draft.audioFileName.ifBlank { "audio.mp3" })
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val audioBytes = readUriBytes(context, uploadUri)
            val mime =
              when {
                safeName.endsWith(".m4a", ignoreCase = true) ||
                  safeName.endsWith(".mp4", ignoreCase = true) -> "audio/mp4"
                safeName.endsWith(".wav", ignoreCase = true) -> "audio/wav"
                safeName.endsWith(".ogg", ignoreCase = true) -> "audio/ogg"
                else -> "audio/mpeg"
              }
            uploadMediaWithFallback(
              storagePath = "sounds/$uid/${soundId}_$safeName",
              bytes = audioBytes,
              contentType = mime,
              isImage = false,
            ) { fraction ->
              onProgress(0.15f + fraction * 0.55f)
            }
          }
          draft.existingAudioUrl.isNotBlank() -> draft.existingAudioUrl
          else -> error("Select an audio/video file or paste a direct audio URL.")
        }

      onProgress(0.75f)
      val finalThumbUrl =
        if (draft.thumbnailUri != null) {
          val imgBytes = compressImageToJpegBytes(context, draft.thumbnailUri)
          uploadMediaWithFallback(
            storagePath = "thumbnails/$uid/${soundId}.jpg",
            bytes = imgBytes,
            contentType = "image/jpeg",
            isImage = true,
          )
        } else {
          draft.existingThumbnailUrl
        }

      onProgress(0.9f)
      val payload =
        buildMap<String, Any> {
          put("id", soundId)
          put("title", draft.title.trim())
          put("category", draft.category)
          put("audioUrl", finalAudioUrl)
          put("thumbnailUrl", finalThumbUrl)
          put("emoji", draft.emoji)
          put("defaultVolume", draft.defaultVolume.coerceIn(0, 100))
          if (isNewSound) {
            put("likes", 0L)
            put("createdBy", uid)
            put("createdAt", System.currentTimeMillis())
          }
        }
      database.reference.child("sounds").child("public").child(soundId).updateChildren(payload).await()
      onProgress(1f)
    }

  suspend fun deletePublicSound(soundId: String): Result<Unit> =
    runCatching {
      database.reference.child("sounds").child("public").child(soundId).removeValue().await()
    }

  suspend fun deleteUserPrivateSound(uid: String, soundId: String): Result<Unit> =
    runCatching {
      database.reference.child("users").child(uid).child("privateSounds").child(soundId).removeValue().await()
    }

  suspend fun saveNotice(notice: AdminNotice): Result<Unit> =
    runCatching {
      val map =
        mapOf(
          "bannerActive" to notice.bannerActive,
          "bannerText" to notice.bannerText.trim(),
          "popupActive" to notice.popupActive,
          "popupTitle" to notice.popupTitle.trim(),
          "popupMessage" to notice.popupMessage.trim(),
        )
      database.reference.child("config").child("notice").setValue(map).await()
    }

  suspend fun uploadBrandingImage(context: Context, name: String, uri: Uri): Result<String> =
    runCatching {
      val uid = auth.currentUser?.uid ?: "admin"
      val bytes = compressImageToJpegBytes(context, uri)
      uploadMediaWithFallback(
        storagePath = "thumbnails/$uid/${name}_${System.currentTimeMillis()}.jpg",
        bytes = bytes,
        contentType = "image/jpeg",
        isImage = true,
      )
    }

  private suspend fun readUriBytes(context: Context, uri: Uri): ByteArray =
    withContext(Dispatchers.IO) {
      if (uri.scheme == "file" && uri.path != null) {
        File(uri.path!!).readBytes()
      } else {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
          ?: error("Could not read selected file.")
      }
    }

  private suspend fun compressImageToJpegBytes(context: Context, uri: Uri): ByteArray =
    withContext(Dispatchers.IO) {
      val rawBytes = readUriBytes(context, uri)
      val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return@withContext rawBytes
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
      out.toByteArray()
    }

  private suspend fun uploadMediaWithFallback(
    storagePath: String,
    bytes: ByteArray,
    contentType: String,
    isImage: Boolean,
    onProgress: (Float) -> Unit = {},
  ): String {
    val metadata = StorageMetadata.Builder().setContentType(contentType).build()
    val buckets = listOf(storage, FirebaseStorage.getInstance(app, "gs://zonex-voice.appspot.com"))
    var lastError: Exception? = null
    for (bucketStorage in buckets) {
      try {
        val ref = bucketStorage.reference.child(storagePath)
        val task = ref.putBytes(bytes, metadata)
        task.addOnProgressListener { snap ->
          if (snap.totalByteCount > 0) {
            onProgress(snap.bytesTransferred.toFloat() / snap.totalByteCount.toFloat())
          }
        }
        task.await()
        return ref.downloadUrl.await().toString()
      } catch (e: Exception) {
        lastError = e
      }
    }
    // Firebase Storage bucket unavailable (e.g. Spark plan / not enabled): store cleanly in RTDB as Base64 data URI
    val maxBytes = if (isImage) 1_500_000 else 5 * 1024 * 1024
    if (bytes.size > maxBytes) {
      throw IllegalStateException(
        "File is ${(bytes.size / (1024f * 1024f)).roundToInt()} MB (max 5 MB without Firebase Storage). Use a smaller clip or Direct URL.",
        lastError,
      )
    }
    onProgress(1f)
    return "data:$contentType;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
  }

  suspend fun saveAppConfig(config: AdminAppConfig): Result<Unit> =
    runCatching {
      val map =
        mapOf(
          "logoUrl" to config.logoUrl.trim(),
          "floatingBrandName" to config.floatingBrandName.trim().ifBlank { "ZONEX" },
        )
      database.reference.child("config").child("app").setValue(map).await()
    }

  suspend fun saveDeveloperConfig(config: AdminDeveloperConfig): Result<Unit> =
    runCatching {
      val btnList =
        config.buttons.map {
          mapOf("label" to it.label.trim(), "url" to it.url.trim(), "color" to it.color.trim())
        }
      val map =
        mapOf(
          "name" to config.name.trim(),
          "logoUrl" to config.logoUrl.trim(),
          "bio" to config.bio.trim(),
          "buttons" to btnList,
        )
      database.reference.child("config").child("developer").setValue(map).await()
    }

  fun observeAppUpdateConfig(): Flow<AdminAppUpdateConfig> = callbackFlow {
    val ref = database.reference.child("config").child("appUpdate")
    val listener =
      object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          trySend(
            AdminAppUpdateConfig(
              enabled = snapshot.child("enabled").getValue(Boolean::class.java) == true,
              latestVersionName =
                snapshot.child("latestVersionName").getValue(String::class.java)
                  ?.ifBlank { "2.4" } ?: "2.4",
              latestVersionCode =
                (snapshot.child("latestVersionCode").value as? Number)?.toInt() ?: 24,
              targetVersionName =
                snapshot.child("targetVersionName").getValue(String::class.java).orEmpty().trim(),
              forceUpdate = snapshot.child("forceUpdate").getValue(Boolean::class.java) == true,
              title =
                snapshot.child("title").getValue(String::class.java)
                  ?.ifBlank { "নতুন আপডেট এসেছে!" } ?: "নতুন আপডেট এসেছে!",
              message = snapshot.child("message").getValue(String::class.java).orEmpty(),
              downloadUrl = snapshot.child("downloadUrl").getValue(String::class.java).orEmpty(),
            )
          )
        }

        override fun onCancelled(error: DatabaseError) {}
      }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  suspend fun saveAppUpdateConfig(config: AdminAppUpdateConfig): Result<Unit> =
    runCatching {
      val map =
        mapOf(
          "enabled" to config.enabled,
          "latestVersionName" to config.latestVersionName.trim().ifBlank { "2.4" },
          "latestVersionCode" to config.latestVersionCode.coerceAtLeast(1),
          "targetVersionName" to config.targetVersionName.trim(),
          "forceUpdate" to config.forceUpdate,
          "title" to config.title.trim().ifBlank { "নতুন আপডেট এসেছে!" },
          "message" to config.message.trim(),
          "downloadUrl" to config.downloadUrl.trim(),
        )
      database.reference.child("config").child("appUpdate").setValue(map).await()
    }

  fun observeTtsVoices(): Flow<List<AdminTtsVoice>> = callbackFlow {
    val ref = database.reference.child("config").child("ttsVoices")
    val listener =
      object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val list =
            snapshot.children.mapNotNull { c ->
              val key = c.key ?: return@mapNotNull null
              val name = c.child("name").getValue(String::class.java)?.trim().orEmpty()
              val voiceId = c.child("cartesiaVoiceId").getValue(String::class.java)?.trim().orEmpty()
              if (name.isBlank() || voiceId.isBlank()) return@mapNotNull null
              AdminTtsVoice(
                id = c.child("id").getValue(String::class.java)?.ifBlank { key } ?: key,
                name = name,
                cartesiaVoiceId = voiceId,
                language = c.child("language").getValue(String::class.java)?.ifBlank { "Multi" } ?: "Multi",
                avatarUrl = c.child("avatarUrl").getValue(String::class.java).orEmpty(),
                badgeColorHex =
                  c.child("badgeColorHex").getValue(String::class.java)?.ifBlank { "#16A34A" }
                    ?: "#16A34A",
                sortOrder = (c.child("sortOrder").value as? Number)?.toInt() ?: 0,
              )
            }.sortedBy { it.sortOrder }
          trySend(list)
        }

        override fun onCancelled(error: DatabaseError) {}
      }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  suspend fun saveTtsVoice(
    context: Context,
    voice: AdminTtsVoice,
    pickedAvatarUri: Uri?,
  ): Result<Unit> =
    runCatching {
      val targetId =
        voice.id.trim().ifBlank {
          database.reference.child("config").child("ttsVoices").push().key
            ?: "v_${System.currentTimeMillis()}"
        }
      val resolvedAvatar =
        if (pickedAvatarUri != null) {
          val uid = auth.currentUser?.uid ?: "admin"
          val bytes = compressImageToJpegBytes(context, pickedAvatarUri)
          uploadMediaWithFallback(
            storagePath = "thumbnails/$uid/tts_${targetId}.jpg",
            bytes = bytes,
            contentType = "image/jpeg",
            isImage = true,
          )
        } else {
          voice.avatarUrl.trim()
        }
      val map =
        mapOf(
          "id" to targetId,
          "name" to voice.name.trim(),
          "cartesiaVoiceId" to voice.cartesiaVoiceId.trim(),
          "language" to voice.language.trim().ifBlank { "Multi" },
          "avatarUrl" to resolvedAvatar,
          "badgeColorHex" to voice.badgeColorHex.trim().ifBlank { "#16A34A" },
          "sortOrder" to voice.sortOrder,
        )
      database.reference.child("config").child("ttsVoices").child(targetId).setValue(map).await()
    }

  suspend fun deleteTtsVoice(voiceId: String): Result<Unit> =
    runCatching {
      database.reference.child("config").child("ttsVoices").child(voiceId).removeValue().await()
    }

  fun observeMaintenanceConfig(): Flow<AdminMaintenanceConfig> = callbackFlow {
    val ref = database.reference.child("config").child("maintenance")
    val listener =
      object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          trySend(
            AdminMaintenanceConfig(
              enabled = snapshot.child("enabled").getValue(Boolean::class.java) == true,
              title =
                snapshot.child("title").getValue(String::class.java)?.ifBlank { "কাজ চলতেছে" }
                  ?: "কাজ চলতেছে",
              message =
                snapshot.child("message").getValue(String::class.java)
                  ?.ifBlank {
                    "অ্যাপের উন্নয়ন ও সার্ভার মেইনটেন্যান্সের কাজ চলছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"
                  }
                  ?: "অ্যাপের উন্নয়ন ও সার্ভার মেইনটেন্যান্সের কাজ চলছে। কিছুক্ষণ পর আবার চেষ্টা করুন।",
            )
          )
        }

        override fun onCancelled(error: DatabaseError) {}
      }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  suspend fun saveMaintenanceConfig(config: AdminMaintenanceConfig): Result<Unit> =
    runCatching {
      database.reference
        .child("config")
        .child("maintenance")
        .setValue(
          mapOf(
            "enabled" to config.enabled,
            "title" to config.title.trim().ifBlank { "কাজ চলতেছে" },
            "message" to config.message.trim(),
          )
        )
        .await()
    }

  fun observeKeyGateConfig(): Flow<AdminKeyGateConfig> = callbackFlow {
    val ref = database.reference.child("config").child("keys")
    val listener =
      object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val required = snapshot.child("requireKeyAfterLogin").getValue(Boolean::class.java) ?: true
          val url = snapshot.child("getKeyUrl").getValue(String::class.java).orEmpty()
          val socials =
            snapshot.child("socialLinks").children.mapNotNull { s ->
              val platform = s.child("platform").getValue(String::class.java).orEmpty().ifBlank { "Telegram" }
              val label = s.child("label").getValue(String::class.java).orEmpty()
              val link = s.child("url").getValue(String::class.java).orEmpty()
              val color = s.child("color").getValue(String::class.java).orEmpty().ifBlank { "#0EA5E9" }
              if (link.isNotBlank()) AdminKeySocialLink(platform, label.ifBlank { platform }, link, color) else null
            }
          trySend(AdminKeyGateConfig(requireKeyAfterLogin = required, getKeyUrl = url, socialLinks = socials))
        }

        override fun onCancelled(error: DatabaseError) {}
      }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  fun observeAccessKeys(): Flow<List<AdminAccessKey>> = callbackFlow {
    val ref = database.reference.child("accessKeys")
    val listener =
      object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          val list =
            snapshot.children
              .mapNotNull { snap ->
                val code = snap.key ?: return@mapNotNull null
                AdminAccessKey(
                  code = code,
                  durationHours = (snap.child("durationHours").value as? Number)?.toInt() ?: 24,
                  createdAt = (snap.child("createdAt").value as? Number)?.toLong() ?: 0L,
                  activatedAt = (snap.child("activatedAt").value as? Number)?.toLong() ?: 0L,
                  expiresAt = (snap.child("expiresAt").value as? Number)?.toLong() ?: 0L,
                  boundUid = snap.child("boundUid").getValue(String::class.java).orEmpty(),
                  boundDeviceId = snap.child("boundDeviceId").getValue(String::class.java).orEmpty(),
                  isRevoked = snap.child("isRevoked").getValue(Boolean::class.java) == true,
                )
              }
              .sortedByDescending { it.createdAt }
          trySend(list)
        }

        override fun onCancelled(error: DatabaseError) {}
      }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  suspend fun saveKeyGateConfig(config: AdminKeyGateConfig): Result<Unit> =
    runCatching {
      val socials =
        config.socialLinks.filter { it.url.isNotBlank() }.map {
          mapOf(
            "platform" to it.platform.trim().ifBlank { "Link" },
            "label" to it.label.trim().ifBlank { it.platform.trim().ifBlank { "Link" } },
            "url" to it.url.trim(),
            "color" to it.color.trim().ifBlank { "#0EA5E9" },
          )
        }
      database.reference
        .child("config")
        .child("keys")
        .setValue(
          mapOf(
            "requireKeyAfterLogin" to config.requireKeyAfterLogin,
            "getKeyUrl" to config.getKeyUrl.trim(),
            "socialLinks" to socials,
          )
        )
        .await()
    }

  suspend fun generateAccessKeys(count: Int, durationHours: Int): Result<List<String>> =
    runCatching {
      val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
      fun seg(): String = (1..5).map { chars.random() }.joinToString("")
      val created = mutableListOf<String>()
      val safeHours = durationHours.coerceAtLeast(1)
      val now = System.currentTimeMillis()
      val expiresAt = now + safeHours * 3600_000L
      repeat(count.coerceIn(1, 20)) {
        val code = "ZONEX-ZIXU-${seg()}-${seg()}"
        val payload =
          mapOf(
            "code" to code,
            "durationHours" to safeHours,
            "createdAt" to now,
            "activatedAt" to now,
            "expiresAt" to expiresAt,
            "boundUid" to "",
            "boundDeviceId" to "",
            "isRevoked" to false,
          )
        database.reference.child("accessKeys").child(code).setValue(payload).await()
        created.add(code)
      }
      created
    }

  suspend fun upsertAccessKey(key: AdminAccessKey): Result<Unit> =
    runCatching {
      val payload =
        mapOf(
          "code" to key.code,
          "durationHours" to key.durationHours,
          "createdAt" to key.createdAt,
          "activatedAt" to key.activatedAt,
          "expiresAt" to key.expiresAt,
          "boundUid" to key.boundUid,
          "boundDeviceId" to key.boundDeviceId,
          "isRevoked" to key.isRevoked,
        )
      database.reference.child("accessKeys").child(key.code).setValue(payload).await()
    }

  suspend fun toggleRevokeKey(code: String, revoked: Boolean): Result<Unit> =
    runCatching {
      database.reference.child("accessKeys").child(code).child("isRevoked").setValue(revoked).await()
    }

  suspend fun resetKeyBinding(code: String): Result<Unit> =
    runCatching {
      val snap = database.reference.child("accessKeys").child(code).get().await()
      val durationHours = (snap.child("durationHours").value as? Number)?.toInt()?.coerceAtLeast(1) ?: 24
      val now = System.currentTimeMillis()
      val exp = now + durationHours * 3600_000L
      resetKeyBindingWithTimestamps(code, now, exp).getOrThrow()
    }

  suspend fun resetKeyBindingWithTimestamps(code: String, now: Long, expiresAt: Long): Result<Unit> =
    runCatching {
      database.reference
        .child("accessKeys")
        .child(code)
        .updateChildren(
          mapOf(
            "createdAt" to now,
            "activatedAt" to now,
            "expiresAt" to expiresAt,
            "boundUid" to "",
            "boundDeviceId" to "",
            "isRevoked" to false,
          )
        )
        .await()
    }

  suspend fun deleteAccessKey(code: String): Result<Unit> =
    runCatching {
      database.reference.child("accessKeys").child(code).removeValue().await()
    }

  private suspend fun extractAudioIfVideo(context: Context, sourceUri: Uri): Pair<Uri, String> =
    withContext(Dispatchers.IO) {
      val mime = context.contentResolver.getType(sourceUri).orEmpty()
      if (!mime.startsWith("video/")) {
        return@withContext sourceUri to "audio.mp3"
      }
      val outDir = File(context.cacheDir, "admin_extracted").apply { mkdirs() }
      val outFile = File(outDir, "extracted_${System.currentTimeMillis()}.m4a")
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
      Uri.fromFile(outFile) to outFile.name
    }

  private fun parseSound(snapshot: DataSnapshot): AdminSound? {
    val key = snapshot.key ?: return null
    val title = snapshot.child("title").getValue(String::class.java).orEmpty()
    val audioUrl = snapshot.child("audioUrl").getValue(String::class.java).orEmpty()
    if (title.isBlank() && audioUrl.isBlank()) return null
    val volRaw = snapshot.child("defaultVolume").value
    val vol =
      when (volRaw) {
        is Double -> if (volRaw <= 1.0) (volRaw * 100.0).roundToInt() else volRaw.roundToInt()
        is Number -> volRaw.toInt()
        else -> 100
      }.coerceIn(0, 100)
    return AdminSound(
      id = snapshot.child("id").getValue(String::class.java)?.ifBlank { key } ?: key,
      title = title.ifBlank { "Untitled" },
      category = snapshot.child("category").getValue(String::class.java)?.ifBlank { "Trending" }
        ?: "Trending",
      audioUrl = audioUrl,
      thumbnailUrl = snapshot.child("thumbnailUrl").getValue(String::class.java).orEmpty(),
      emoji = snapshot.child("emoji").getValue(String::class.java)?.ifBlank { "🔥" } ?: "🔥",
      defaultVolume = vol,
      likes = (snapshot.child("likes").value as? Number)?.toLong() ?: 0L,
      createdBy = snapshot.child("createdBy").getValue(String::class.java).orEmpty(),
      createdAt = (snapshot.child("createdAt").value as? Number)?.toLong() ?: 0L,
    )
  }
}
