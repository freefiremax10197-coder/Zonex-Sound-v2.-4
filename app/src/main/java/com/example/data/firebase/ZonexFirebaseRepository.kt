package com.example.data.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.audio.CartesiaVoiceOption
import com.example.audio.VideoAudioExtractor
import com.example.data.model.AdminNotice
import com.example.data.model.AppConfig
import com.example.data.model.AppUpdateConfig
import com.example.data.model.DeveloperButton
import com.example.data.model.DeveloperConfig
import com.example.data.model.KeyGateConfig
import com.example.data.model.KeySocialLink
import com.example.data.model.MaintenanceConfig
import com.example.data.model.SoundItem
import com.example.data.model.SoundVisibility
import com.example.data.model.UploadSoundDraft
import com.example.data.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface KeyLiveCheck {
  data class Valid(val expiresAt: Long) : KeyLiveCheck
  data object Deleted : KeyLiveCheck
  data object Revoked : KeyLiveCheck
  data object Expired : KeyLiveCheck
  data object WrongBinding : KeyLiveCheck
}

sealed interface KeyValidationResult {
  data class Valid(val code: String, val expiresAt: Long) : KeyValidationResult
  data object Invalid : KeyValidationResult
  data object NetworkError : KeyValidationResult
}

class ZonexFirebaseRepository {

  private val app: FirebaseApp by lazy { FirebaseApp.getInstance() }
  private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance(app) }
  private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance(app) }
  private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance(app) }

  fun currentUserProfile(): UserProfile? {
    val user = auth.currentUser ?: return null
    return UserProfile(
      uid = user.uid,
      displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Gamer",
      email = user.email ?: "",
    )
  }

  val authStateFlow: Flow<UserProfile?> = callbackFlow {
    val listener =
      FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        trySend(
          user?.let {
            UserProfile(
              uid = it.uid,
              displayName = it.displayName ?: it.email?.substringBefore("@") ?: "Gamer",
              email = it.email ?: "",
            )
          }
        )
      }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  suspend fun signIn(email: String, password: String): Result<UserProfile> {
    return runCatching {
      val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
      val user = result.user ?: error("Authentication returned empty user.")
      val profile =
        UserProfile(
          uid = user.uid,
          displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Gamer",
          email = user.email ?: email.trim(),
        )
      runCatching {
        database.reference
          .child("users")
          .child(profile.uid)
          .updateChildren(mapOf("displayName" to profile.displayName, "email" to profile.email))
          .await()
      }
      profile
    }
  }

  suspend fun signUp(fullName: String, email: String, password: String): Result<UserProfile> {
    return runCatching {
      val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
      val user = result.user ?: error("Registration returned empty user.")
      val profileUpdate =
        UserProfileChangeRequest.Builder().setDisplayName(fullName.trim()).build()
      user.updateProfile(profileUpdate).await()
      val profile =
        UserProfile(
          uid = user.uid,
          displayName = fullName.trim().ifEmpty { email.trim().substringBefore("@") },
          email = user.email ?: email.trim(),
        )
      runCatching {
        database.reference
          .child("users")
          .child(profile.uid)
          .updateChildren(mapOf("displayName" to profile.displayName, "email" to profile.email))
          .await()
      }
      profile
    }
  }

  fun signOut() {
    auth.signOut()
  }

  suspend fun hasSeenOnboardingRemote(uid: String): Boolean {
    return runCatching {
      val snapshot = database.reference.child("users").child(uid).child("hasSeenOnboarding").get().await()
      snapshot.getValue(Boolean::class.java) == true
    }.getOrDefault(false)
  }

  suspend fun markOnboardingSeen(uid: String) {
    runCatching {
      database.reference.child("users").child(uid).child("hasSeenOnboarding").setValue(true).await()
    }
  }

  fun observePublicSounds(): Flow<List<SoundItem>> =
    callbackFlow {
      val ref = database.reference.child("sounds").child("public")
      val listener =
        object : ValueEventListener {
          override fun onDataChange(snapshot: DataSnapshot) {
            val list =
              snapshot.children
                .mapNotNull { parseSoundSnapshot(it, defaultVisibility = "public") }
                .sortedByDescending { it.createdAt }
            trySend(list)
          }

          override fun onCancelled(error: DatabaseError) {
            close(error.toException())
          }
        }
      ref.addValueEventListener(listener)
      val initialFetch =
        launch {
          runCatching {
            auth.currentUser?.getIdToken(false)?.await()
            val snap = ref.get().await()
            val list =
              snap.children
                .mapNotNull { parseSoundSnapshot(it, defaultVisibility = "public") }
                .sortedByDescending { it.createdAt }
            trySend(list)
          }
        }
      awaitClose {
        initialFetch.cancel()
        ref.removeEventListener(listener)
      }
    }.retryWhen { _, attempt ->
      delay(minOf(2_000L * (attempt + 1), 10_000L))
      true
    }

  fun observePrivateSounds(uid: String): Flow<List<SoundItem>> =
    callbackFlow {
      val ref = database.reference.child("users").child(uid).child("privateSounds")
      val listener =
        object : ValueEventListener {
          override fun onDataChange(snapshot: DataSnapshot) {
            val list =
              snapshot.children
                .mapNotNull { parseSoundSnapshot(it, defaultVisibility = "private", ownerUid = uid) }
                .sortedByDescending { it.createdAt }
            trySend(list)
          }

          override fun onCancelled(error: DatabaseError) {
            close(error.toException())
          }
        }
      ref.addValueEventListener(listener)
      awaitClose { ref.removeEventListener(listener) }
    }.retryWhen { _, attempt ->
      delay(minOf(2_000L * (attempt + 1), 10_000L))
      true
    }

  fun observeDownloads(uid: String): Flow<Map<String, SoundItem?>> =
    callbackFlow {
      val ref = database.reference.child("users").child(uid).child("downloads")
      val listener =
        object : ValueEventListener {
          override fun onDataChange(snapshot: DataSnapshot) {
            val map = mutableMapOf<String, SoundItem?>()
            for (child in snapshot.children) {
              val key = child.key ?: continue
              val parsed = parseSoundSnapshot(child, defaultVisibility = "public", isDownloaded = true)
              map[key] = parsed
            }
            trySend(map)
          }

          override fun onCancelled(error: DatabaseError) {
            close(error.toException())
          }
        }
      ref.addValueEventListener(listener)
      awaitClose { ref.removeEventListener(listener) }
    }.retryWhen { _, attempt ->
      delay(minOf(2_000L * (attempt + 1), 10_000L))
      true
    }

  fun observeFavorites(uid: String): Flow<Set<String>> =
    callbackFlow {
      val ref = database.reference.child("users").child(uid).child("favorites")
      val listener =
        object : ValueEventListener {
          override fun onDataChange(snapshot: DataSnapshot) {
            val ids =
              snapshot.children
                .mapNotNull { child ->
                  val isFav = child.getValue(Boolean::class.java) == true
                  if (isFav) child.key else null
                }
                .toSet()
            trySend(ids)
          }

          override fun onCancelled(error: DatabaseError) {
            close(error.toException())
          }
        }
      ref.addValueEventListener(listener)
      awaitClose { ref.removeEventListener(listener) }
    }.retryWhen { _, attempt ->
      delay(minOf(2_000L * (attempt + 1), 10_000L))
      true
    }

  fun observeNotice(): Flow<AdminNotice> =
    callbackFlow {
      val ref = database.reference.child("config").child("notice")
      val listener =
        object : ValueEventListener {
          override fun onDataChange(snapshot: DataSnapshot) {
            val notice =
              AdminNotice(
                bannerActive = snapshot.child("bannerActive").getValue(Boolean::class.java) == true,
                bannerText = snapshot.child("bannerText").getValue(String::class.java) ?: "",
                popupActive = snapshot.child("popupActive").getValue(Boolean::class.java) == true,
                popupTitle = snapshot.child("popupTitle").getValue(String::class.java) ?: "",
                popupMessage = snapshot.child("popupMessage").getValue(String::class.java) ?: "",
              )
            trySend(notice)
          }

          override fun onCancelled(error: DatabaseError) {
            close(error.toException())
          }
        }
      ref.addValueEventListener(listener)
      awaitClose { ref.removeEventListener(listener) }
    }.retryWhen { _, attempt ->
      delay(minOf(2_000L * (attempt + 1), 10_000L))
      true
    }

  private fun parseDeveloperSnapshot(snapshot: DataSnapshot): DeveloperConfig {
    val buttons =
      snapshot.child("buttons").children.mapNotNull { btnSnap ->
        val label = btnSnap.child("label").getValue(String::class.java).orEmpty()
        val url = btnSnap.child("url").getValue(String::class.java).orEmpty()
        val color = btnSnap.child("color").getValue(String::class.java) ?: "#F5B82E"
        if (label.isNotBlank() && url.isNotBlank()) {
          DeveloperButton(label = label, url = url, color = color)
        } else {
          null
        }
      }
    val name = snapshot.child("name").getValue(String::class.java)
      ?.takeIf { it.isNotBlank() } ?: "Zonex Official"
    val logoUrl = snapshot.child("logoUrl").getValue(String::class.java).orEmpty()
    val bio = snapshot.child("bio").getValue(String::class.java)
      ?.takeIf { it.isNotBlank() }
      ?: "Pro gaming soundboard & low-latency audio utility built for creators and gamers."
    return DeveloperConfig(
      name = name,
      logoUrl = logoUrl,
      bio = bio,
      buttons = buttons,
    )
  }

  fun observeDeveloper(): Flow<DeveloperConfig> =
    callbackFlow {
      val ref = database.reference.child("config").child("developer")
      val listener =
        object : ValueEventListener {
          override fun onDataChange(snapshot: DataSnapshot) {
            trySend(parseDeveloperSnapshot(snapshot))
          }

          override fun onCancelled(error: DatabaseError) {
            close(error.toException())
          }
        }
      ref.addValueEventListener(listener)
      val initialFetch = launch {
        runCatching {
          val snap = ref.get().await()
          trySend(parseDeveloperSnapshot(snap))
        }
      }
      awaitClose {
        initialFetch.cancel()
        ref.removeEventListener(listener)
      }
    }.retryWhen { _, attempt ->
      delay(minOf(2_000L * (attempt + 1), 10_000L))
      true
    }

  fun observeAppConfig(): Flow<AppConfig> =
    callbackFlow {
      val ref = database.reference.child("config").child("app")
      val listener =
        object : ValueEventListener {
          override fun onDataChange(snapshot: DataSnapshot) {
            val logoUrl = snapshot.child("logoUrl").getValue(String::class.java).orEmpty()
            val brand =
              snapshot.child("floatingBrandName").getValue(String::class.java)
                ?.takeIf { it.isNotBlank() } ?: "ZONEX"
            trySend(AppConfig(logoUrl = logoUrl, floatingBrandName = brand))
          }

          override fun onCancelled(error: DatabaseError) {
            close(error.toException())
          }
        }
      ref.addValueEventListener(listener)
      awaitClose { ref.removeEventListener(listener) }
    }.retryWhen { _, attempt ->
      delay(minOf(2_000L * (attempt + 1), 10_000L))
      true
    }

  fun observeAppUpdateConfig(): Flow<AppUpdateConfig> =
    callbackFlow {
      val ref = database.reference.child("config").child("appUpdate")
      val listener =
        object : ValueEventListener {
          override fun onDataChange(snapshot: DataSnapshot) {
            val enabled = snapshot.child("enabled").getValue(Boolean::class.java) == true
            val latestName =
              snapshot.child("latestVersionName").getValue(String::class.java).orEmpty().trim()
            val latestCode =
              (snapshot.child("latestVersionCode").value as? Number)?.toInt() ?: 0
            val targetName =
              snapshot.child("targetVersionName").getValue(String::class.java).orEmpty().trim()
            val force = snapshot.child("forceUpdate").getValue(Boolean::class.java) == true
            val title =
              snapshot.child("title").getValue(String::class.java)
                ?.takeIf { it.isNotBlank() } ?: "নতুন আপডেট এসেছে!"
            val message = snapshot.child("message").getValue(String::class.java).orEmpty()
            val url = snapshot.child("downloadUrl").getValue(String::class.java).orEmpty().trim()
            trySend(
              AppUpdateConfig(
                enabled = enabled,
                latestVersionName = latestName,
                latestVersionCode = latestCode,
                targetVersionName = targetName,
                forceUpdate = force,
                title = title,
                message = message,
                downloadUrl = url,
              )
            )
          }

          override fun onCancelled(error: DatabaseError) {
            close(error.toException())
          }
        }
      ref.addValueEventListener(listener)
      awaitClose { ref.removeEventListener(listener) }
    }.retryWhen { _, attempt ->
      delay(minOf(2_000L * (attempt + 1), 10_000L))
      true
    }

  fun observeMaintenanceConfig(): Flow<MaintenanceConfig> =
    callbackFlow {
      val ref = database.reference.child("config").child("maintenance")
      val listener =
        object : ValueEventListener {
          override fun onDataChange(snapshot: DataSnapshot) {
            val enabled = snapshot.child("enabled").getValue(Boolean::class.java) == true
            val title =
              snapshot.child("title").getValue(String::class.java)
                ?.takeIf { it.isNotBlank() } ?: "কাজ চলতেছে"
            val message =
              snapshot.child("message").getValue(String::class.java)
                ?.takeIf { it.isNotBlank() }
                ?: "অ্যাপের উন্নয়ন ও সার্ভার মেইনটেন্যান্সের কাজ চলছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"
            trySend(MaintenanceConfig(enabled = enabled, title = title, message = message))
          }

          override fun onCancelled(error: DatabaseError) {
            close(error.toException())
          }
        }
      ref.addValueEventListener(listener)
      awaitClose { ref.removeEventListener(listener) }
    }.retryWhen { _, attempt ->
      delay(minOf(2_000L * (attempt + 1), 10_000L))
      true
    }

  fun observeRemoteTtsVoices(): Flow<List<CartesiaVoiceOption>> =
    callbackFlow {
      val ref = database.reference.child("config").child("ttsVoices")
      val listener =
        object : ValueEventListener {
          override fun onDataChange(snapshot: DataSnapshot) {
            trySend(parseRemoteVoicesSnapshot(snapshot))
          }

          override fun onCancelled(error: DatabaseError) {
            close(error.toException())
          }
        }
      ref.addValueEventListener(listener)
      awaitClose { ref.removeEventListener(listener) }
    }.retryWhen { _, attempt ->
      delay(minOf(2_000L * (attempt + 1), 10_000L))
      true
    }

  suspend fun fetchRemoteTtsVoicesOnce(): List<CartesiaVoiceOption> =
    runCatching {
      val snap = database.reference.child("config").child("ttsVoices").get().await()
      parseRemoteVoicesSnapshot(snap)
    }.getOrDefault(emptyList())

  private fun parseRemoteVoicesSnapshot(snapshot: DataSnapshot): List<CartesiaVoiceOption> {
    if (!snapshot.exists()) return emptyList()
    return snapshot.children.mapNotNull { child ->
      val key = child.key ?: return@mapNotNull null
      val name = child.child("name").getValue(String::class.java)?.trim().orEmpty()
      val voiceId =
        child.child("cartesiaVoiceId").getValue(String::class.java)?.trim()
          ?.ifBlank { child.child("voiceId").getValue(String::class.java)?.trim() }
          .orEmpty()
      if (name.isBlank() || voiceId.isBlank()) return@mapNotNull null
      val language =
        child.child("language").getValue(String::class.java)?.trim()?.ifBlank { "Multi" } ?: "Multi"
      val avatarUrl = child.child("avatarUrl").getValue(String::class.java)?.trim().orEmpty()
      val badgeColorHex =
        child.child("badgeColorHex").getValue(String::class.java)?.trim()?.ifBlank { "#16A34A" }
          ?: "#16A34A"
      val sortOrder = (child.child("sortOrder").value as? Number)?.toInt() ?: Int.MAX_VALUE
      sortOrder to
        CartesiaVoiceOption(
          id = child.child("id").getValue(String::class.java)?.trim()?.ifBlank { key } ?: key,
          name = name,
          cartesiaVoiceId = voiceId,
          language = language,
          avatarUrl = avatarUrl,
          badgeColorHex = badgeColorHex,
        )
    }.sortedBy { it.first }.map { it.second }
  }

  private fun parseKeyGateSnapshot(snapshot: DataSnapshot): KeyGateConfig {
    val required = snapshot.child("requireKeyAfterLogin").getValue(Boolean::class.java) ?: true
    val url = snapshot.child("getKeyUrl").getValue(String::class.java).orEmpty()
    val socials =
      snapshot.child("socialLinks").children.mapNotNull { s ->
        val platform = s.child("platform").getValue(String::class.java).orEmpty().ifBlank { "Telegram" }
        val label = s.child("label").getValue(String::class.java).orEmpty()
        val link = s.child("url").getValue(String::class.java).orEmpty()
        val color = s.child("color").getValue(String::class.java).orEmpty().ifBlank { "#0EA5E9" }
        if (link.isNotBlank()) KeySocialLink(platform, label.ifBlank { platform }, link, color) else null
      }
    return KeyGateConfig(requireKeyAfterLogin = required, getKeyUrl = url, socialLinks = socials)
  }

  fun observeKeyGateConfig(): Flow<KeyGateConfig> = callbackFlow {
    val ref = database.reference.child("config").child("keys")
    val listener =
      object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          trySend(parseKeyGateSnapshot(snapshot))
        }

        override fun onCancelled(error: DatabaseError) {
          close(error.toException())
        }
      }
    ref.addValueEventListener(listener)
    val initialFetch = launch {
      runCatching {
        val snap = ref.get().await()
        trySend(parseKeyGateSnapshot(snap))
      }
    }
    awaitClose {
      initialFetch.cancel()
      ref.removeEventListener(listener)
    }
  }.retryWhen { _, attempt ->
    delay(minOf(2_000L * (attempt + 1), 10_000L))
    true
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

        override fun onCancelled(error: DatabaseError) {
          close(error.toException())
        }
      }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }.retryWhen { _, attempt ->
    delay(minOf(2_000L * (attempt + 1), 10_000L))
    true
  }

  suspend fun verifyAndActivateAccessKey(
    uid: String,
    deviceId: String,
    rawCode: String,
  ): Result<Pair<String, Long>> =
    runCatching {
      auth.currentUser?.getIdToken(false)?.await()
      val code = rawCode.trim().uppercase()
      val pattern = Regex("^ZONEX-ZIXU-[A-Z0-9]{5}-[A-Z0-9]{5}$")
      if (!pattern.matches(code)) {
        error("ফরম্যাট সঠিক নয়! Key এমন হবে: ZONEX-ZIXU-XXXXX-XXXXX")
      }

      val keyRef = database.reference.child("accessKeys").child(code)
      val snap = keyRef.get().await()
      if (!snap.exists()) {
        error("এই Key-টি সঠিক নয় বা সার্ভারে পাওয়া যায়নি। 'Get a Key' থেকে নতুন Key নিন।")
      }
      if (snap.child("isRevoked").getValue(Boolean::class.java) == true) {
        error("এই Key-টি অ্যাডমিন কর্তৃক বাতিল (Revoked) করা হয়েছে।")
      }

      val boundUid = snap.child("boundUid").getValue(String::class.java).orEmpty()
      val boundDevice = snap.child("boundDeviceId").getValue(String::class.java).orEmpty()
      if (boundUid.isNotBlank() && boundUid != uid) {
        error("এই Key-টি ইতিমধ্যে অন্য একজন ইউজার ব্যবহার করছে! ১টি Key শুধু ১ জন ইউজার ব্যবহার করতে পারবে।")
      }
      if (boundDevice.isNotBlank() && boundDevice != deviceId) {
        error("এই Key-টি অন্য একটি ডিভাইসে লক করা আছে। ১টি Key শুধু ১টি ডিভাইসে চলবে।")
      }

      val now = System.currentTimeMillis()
      val createdAt = (snap.child("createdAt").value as? Number)?.toLong() ?: 0L
      val activatedAt = (snap.child("activatedAt").value as? Number)?.toLong() ?: 0L
      val existingExpires = (snap.child("expiresAt").value as? Number)?.toLong() ?: 0L
      val durationHours = (snap.child("durationHours").value as? Number)?.toInt()?.coerceAtLeast(1) ?: 24

      // Key timer starts immediately from creation time!
      val finalExpiresAt =
        when {
          existingExpires > 0L -> existingExpires
          activatedAt > 0L -> activatedAt + durationHours * 3600_000L
          createdAt > 0L -> createdAt + durationHours * 3600_000L
          else -> now + durationHours * 3600_000L
        }

      if (now > finalExpiresAt) {
        error("এই Key-টির মেয়াদ শেষ হয়ে গেছে। নতুন Key নিন।")
      }

      val updates =
        mapOf(
          "activatedAt" to (if (activatedAt > 0L) activatedAt else if (createdAt > 0L) createdAt else now),
          "expiresAt" to finalExpiresAt,
          "boundUid" to uid,
          "boundDeviceId" to deviceId,
        )
      keyRef.updateChildren(updates).await()

      database.reference
        .child("users")
        .child(uid)
        .child("activeKey")
        .setValue(mapOf("code" to code, "expiresAt" to finalExpiresAt, "boundDeviceId" to deviceId))
        .await()

      code to finalExpiresAt
    }

  suspend fun validateExistingUserKey(
    uid: String,
    deviceId: String,
    cachedCode: String,
  ): Pair<String, Long>? {
    return when (val res = validateExistingUserKeyDetailed(uid, deviceId, cachedCode)) {
      is KeyValidationResult.Valid -> res.code to res.expiresAt
      else -> null
    }
  }

  suspend fun validateExistingUserKeyDetailed(
    uid: String,
    deviceId: String,
    cachedCode: String,
  ): KeyValidationResult {
    return try {
      val trimmedCached = cachedCode.trim().uppercase()
      val code =
        if (trimmedCached.isNotBlank()) {
          trimmedCached
        } else {
          val userKeySnap = database.reference.child("users").child(uid).child("activeKey").get().await()
          userKeySnap.child("code").getValue(String::class.java).orEmpty().trim().uppercase()
        }
      if (code.isBlank()) return KeyValidationResult.Invalid

      val snap = database.reference.child("accessKeys").child(code).get().await()
      if (!snap.exists()) return KeyValidationResult.Invalid
      if (snap.child("isRevoked").getValue(Boolean::class.java) == true) {
        return KeyValidationResult.Invalid
      }

      val boundUid = snap.child("boundUid").getValue(String::class.java).orEmpty()
      val boundDevice = snap.child("boundDeviceId").getValue(String::class.java).orEmpty()
      if (boundUid.isNotBlank() && boundUid != uid) return KeyValidationResult.Invalid
      if (boundDevice.isNotBlank() && boundDevice != deviceId) return KeyValidationResult.Invalid

      val createdAt = (snap.child("createdAt").value as? Number)?.toLong() ?: 0L
      val durationHours =
        (snap.child("durationHours").value as? Number)?.toInt()?.coerceAtLeast(1) ?: 24
      val rawExpiresAt = (snap.child("expiresAt").value as? Number)?.toLong() ?: 0L
      val expiresAt =
        if (rawExpiresAt > 0L) rawExpiresAt
        else if (createdAt > 0L) createdAt + durationHours * 3600_000L
        else 0L
      if (expiresAt <= 0L || System.currentTimeMillis() > expiresAt) {
        return KeyValidationResult.Invalid
      }
      KeyValidationResult.Valid(code, expiresAt)
    } catch (_: Exception) {
      KeyValidationResult.NetworkError
    }
  }

  fun observeAccessKeyLive(
    keyCode: String,
    uid: String,
    deviceId: String,
  ): Flow<KeyLiveCheck> = callbackFlow {
    val cleanCode = keyCode.trim().uppercase()
    if (cleanCode.isBlank()) {
      close()
      return@callbackFlow
    }
    val ref = database.reference.child("accessKeys").child(cleanCode)
    val listener =
      object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
          if (!snapshot.exists()) {
            trySend(KeyLiveCheck.Deleted)
            return
          }
          if (snapshot.child("isRevoked").getValue(Boolean::class.java) == true) {
            trySend(KeyLiveCheck.Revoked)
            return
          }
          val boundUid = snapshot.child("boundUid").getValue(String::class.java).orEmpty()
          val boundDevice = snapshot.child("boundDeviceId").getValue(String::class.java).orEmpty()
          if ((boundUid.isNotBlank() && boundUid != uid) ||
            (boundDevice.isNotBlank() && boundDevice != deviceId)
          ) {
            trySend(KeyLiveCheck.WrongBinding)
            return
          }
          val createdAt = (snapshot.child("createdAt").value as? Number)?.toLong() ?: 0L
          val durationHours =
            (snapshot.child("durationHours").value as? Number)?.toInt()?.coerceAtLeast(1) ?: 24
          val rawExpiresAt = (snapshot.child("expiresAt").value as? Number)?.toLong() ?: 0L
          val expiresAt =
            if (rawExpiresAt > 0L) rawExpiresAt
            else if (createdAt > 0L) createdAt + durationHours * 3600_000L
            else 0L
          if (expiresAt <= 0L || System.currentTimeMillis() > expiresAt) {
            trySend(KeyLiveCheck.Expired)
            return
          }
          trySend(KeyLiveCheck.Valid(expiresAt))
        }

        override fun onCancelled(error: DatabaseError) {}
      }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

  suspend fun toggleFavorite(uid: String, soundId: String, favorite: Boolean) {
    val ref = database.reference.child("users").child(uid).child("favorites").child(soundId)
    if (favorite) {
      ref.setValue(true).await()
    } else {
      ref.removeValue().await()
    }
  }

  suspend fun incrementPublicLike(soundId: String, delta: Long) {
    val ref = database.reference.child("sounds").child("public").child(soundId).child("likes")
    ref.runTransaction(
      object : Transaction.Handler {
        override fun doTransaction(currentData: MutableData): Transaction.Result {
          val current = (currentData.value as? Number)?.toLong() ?: 0L
          currentData.value = (current + delta).coerceAtLeast(0L)
          return Transaction.success(currentData)
        }

        override fun onComplete(
          error: DatabaseError?,
          committed: Boolean,
          currentData: DataSnapshot?,
        ) {}
      }
    )
  }

  suspend fun saveToDownloads(uid: String, sound: SoundItem) {
    val payload =
      mapOf(
        "id" to sound.id,
        "title" to sound.title,
        "category" to sound.category,
        "audioUrl" to sound.audioUrl,
        "thumbnailUrl" to sound.thumbnailUrl,
        "emoji" to sound.emoji,
        "defaultVolume" to sound.defaultVolume,
        "createdBy" to sound.createdBy,
        "createdAt" to System.currentTimeMillis(),
      )
    database.reference.child("users").child(uid).child("downloads").child(sound.id).setValue(payload).await()
  }

  suspend fun removeFromDownloads(uid: String, soundId: String) {
    database.reference.child("users").child(uid).child("downloads").child(soundId).removeValue().await()
  }

  suspend fun deleteUploadedSound(uid: String, sound: SoundItem) {
    if (sound.visibility == "private") {
      database.reference.child("users").child(uid).child("privateSounds").child(sound.id).removeValue().await()
    } else if (sound.createdBy == uid) {
      database.reference.child("sounds").child("public").child(sound.id).removeValue().await()
    }
  }

  suspend fun uploadSound(
    context: Context,
    uid: String,
    draft: UploadSoundDraft,
    onProgress: (Float) -> Unit,
  ): Result<SoundItem> {
    return runCatching {
      // Ensure the Firebase Auth token is attached before RTDB/Storage writes
      auth.currentUser?.getIdToken(false)?.await()

      val targetRef =
        if (draft.visibility == SoundVisibility.PRIVATE) {
          database.reference.child("users").child(uid).child("privateSounds").push()
        } else {
          database.reference.child("sounds").child("public").push()
        }
      val soundId = targetRef.key ?: System.currentTimeMillis().toString()

      onProgress(0.12f)
      val resolvedAudioUrl =
        if (draft.useDirectUrl) {
          draft.directAudioUrl.trim()
        } else {
          val rawUri = draft.audioUri ?: error("Please select an audio or video file, or enter a direct audio URL.")
          val (finalUri, extractedName) =
            VideoAudioExtractor.extractAudioIfVideo(context, rawUri).getOrThrow()
          val candidateName =
            if (extractedName.startsWith("extracted_")) extractedName
            else draft.audioFileName.ifBlank { "audio_${soundId}.mp3" }
          val safeName = candidateName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
          val audioBytes = readUriBytes(context, finalUri)
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

      onProgress(0.75f)
      val resolvedThumbnailUrl =
        if (draft.thumbnailUri != null) {
          uploadThumbnail(context, uid, soundId, draft.thumbnailUri)
        } else {
          ""
        }

      onProgress(0.90f)
      val now = System.currentTimeMillis()
      val visibilityStr = draft.visibility.value
      val payload =
        buildMap<String, Any> {
          put("id", soundId)
          put("title", draft.title.trim())
          put("category", draft.category)
          put("audioUrl", resolvedAudioUrl)
          put("thumbnailUrl", resolvedThumbnailUrl)
          put("emoji", draft.emoji)
          put("defaultVolume", draft.defaultVolume.coerceIn(0, 100))
          put("createdAt", now)
          if (draft.visibility == SoundVisibility.PRIVATE) {
            put("visibility", "private")
          } else {
            put("likes", 0L)
            put("createdBy", uid)
          }
        }

      val rtdbPath =
        if (draft.visibility == SoundVisibility.PRIVATE) "users/$uid/privateSounds" else "sounds/public"
      try {
        targetRef.setValue(payload).await()
      } catch (e: Exception) {
        val msg = e.localizedMessage ?: "Permission denied"
        if (msg.contains("Permission denied", ignoreCase = true)) {
          throw IllegalStateException(
            "Firebase Database Permission denied on '$rtdbPath'. Update RTDB Rules in Firebase Console for zonex-voice.",
            e,
          )
        }
        throw e
      }

      if (draft.addToFavorites) {
        runCatching { toggleFavorite(uid, soundId, true) }
      }

      auth.currentUser?.let { firebaseUser ->
        val name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Gamer"
        val mail = firebaseUser.email.orEmpty()
        runCatching {
          database.reference
            .child("users")
            .child(uid)
            .updateChildren(mapOf("displayName" to name, "email" to mail))
        }
      }

      onProgress(1f)
      SoundItem(
        id = soundId,
        title = draft.title.trim(),
        category = draft.category,
        audioUrl = resolvedAudioUrl,
        thumbnailUrl = resolvedThumbnailUrl,
        emoji = draft.emoji,
        defaultVolume = draft.defaultVolume.coerceIn(0, 100),
        likes = 0L,
        createdBy = uid,
        createdAt = now,
        visibility = visibilityStr,
      )
    }
  }

  private suspend fun uploadThumbnail(context: Context, uid: String, soundId: String, uri: Uri): String {
    val bytes = compressImageToJpegBytes(context, uri)
    return uploadMediaWithFallback(
      storagePath = "thumbnails/$uid/$soundId.jpg",
      bytes = bytes,
      contentType = "image/jpeg",
      isImage = true,
    )
  }

  private suspend fun readUriBytes(context: Context, uri: Uri): ByteArray =
    withContext(Dispatchers.IO) {
      val filePath = uri.path
      if (uri.scheme == "file" && filePath != null) {
        File(filePath).readBytes()
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
    val maxBytes = if (isImage) 1_500_000 else 5 * 1024 * 1024
    if (bytes.size > maxBytes) {
      throw IllegalStateException(
        "File size exceeds 5 MB limit without Firebase Storage. Trim clip or use Direct URL.",
        lastError,
      )
    }
    onProgress(1f)
    return "data:$contentType;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
  }

  private fun parseSoundSnapshot(
    snapshot: DataSnapshot,
    defaultVisibility: String,
    ownerUid: String = "",
    isDownloaded: Boolean = false,
  ): SoundItem? {
    val key = snapshot.key ?: return null
    val audioUrl = snapshot.child("audioUrl").getValue(String::class.java).orEmpty()
    val title = snapshot.child("title").getValue(String::class.java).orEmpty()
    if (title.isBlank() && audioUrl.isBlank()) return null

    val volRaw = snapshot.child("defaultVolume").value
    val defaultVolume =
      when (volRaw) {
        is Double -> if (volRaw > 0.0 && volRaw <= 1.0) (volRaw * 100.0).roundToInt() else volRaw.roundToInt()
        is Float -> if (volRaw > 0f && volRaw <= 1f) (volRaw * 100f).roundToInt() else volRaw.roundToInt()
        is Number -> volRaw.toInt()
        is String -> volRaw.toDoubleOrNull()?.let {
          if (it > 0.0 && it <= 1.0) (it * 100.0).roundToInt() else it.roundToInt()
        } ?: 100
        else -> 100
      }.coerceIn(0, 100)

    val likes = (snapshot.child("likes").value as? Number)?.toLong() ?: 0L
    val createdAt = (snapshot.child("createdAt").value as? Number)?.toLong() ?: 0L
    val visibility = snapshot.child("visibility").getValue(String::class.java) ?: defaultVisibility
    val createdBy = snapshot.child("createdBy").getValue(String::class.java) ?: ownerUid

    return SoundItem(
      id = snapshot.child("id").getValue(String::class.java)?.ifBlank { key } ?: key,
      title = title.ifBlank { "Untitled Sound" },
      category = snapshot.child("category").getValue(String::class.java)?.ifBlank { "Trending" }
        ?: "Trending",
      audioUrl = audioUrl,
      thumbnailUrl = snapshot.child("thumbnailUrl").getValue(String::class.java).orEmpty(),
      emoji = snapshot.child("emoji").getValue(String::class.java)?.ifBlank { "🔊" } ?: "🔊",
      defaultVolume = defaultVolume,
      likes = likes,
      createdBy = createdBy,
      createdAt = createdAt,
      visibility = visibility,
      isDownloaded = isDownloaded,
    )
  }
}
