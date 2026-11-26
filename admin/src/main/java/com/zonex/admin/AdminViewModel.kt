package com.zonex.admin

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AdminViewModel(application: Application) : AndroidViewModel(application) {

  private val appContext = application.applicationContext
  private val repo = AdminRepository()
  private var previewPlayer: MediaPlayer? = null
  private val syncMutex = Mutex()
  private var networkCallback: ConnectivityManager.NetworkCallback? = null

  init {
    AdminOfflineStore.ensureLoaded(appContext)
    registerNetworkObserver()
  }

  private val _isOnline = MutableStateFlow(AdminOfflineStore.isNetworkAvailable(appContext))
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  private val _isSyncing = MutableStateFlow(false)
  val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

  val pendingSyncCount: StateFlow<Int> =
    AdminOfflineStore.pendingOps
      .map { it.size }
      .stateIn(viewModelScope, SharingStarted.Eagerly, AdminOfflineStore.pendingOps.value.size)

  // Optimistic in-memory overrides for instant <10ms UI updates even while Firebase writes are in-flight
  private val _optimisticUpsertedSounds = MutableStateFlow<Map<String, AdminSound>>(emptyMap())
  private val _optimisticDeletedSoundIds = MutableStateFlow<Set<String>>(emptySet())
  private val _optimisticCategories = MutableStateFlow<List<String>?>(null)
  private val _optimisticNotice = MutableStateFlow<AdminNotice?>(null)
  private val _optimisticMaintenance = MutableStateFlow<AdminMaintenanceConfig?>(null)
  private val _optimisticAppUpdate = MutableStateFlow<AdminAppUpdateConfig?>(null)
  private val _optimisticAppConfig = MutableStateFlow<AdminAppConfig?>(null)
  private val _optimisticDevConfig = MutableStateFlow<AdminDeveloperConfig?>(null)
  private val _optimisticUpsertedVoices = MutableStateFlow<Map<String, AdminTtsVoice>>(emptyMap())
  private val _optimisticDeletedVoiceIds = MutableStateFlow<Set<String>>(emptySet())
  private val _optimisticKeyGate = MutableStateFlow<AdminKeyGateConfig?>(null)
  private val _optimisticUpsertedKeys = MutableStateFlow<Map<String, AdminAccessKey>>(emptyMap())
  private val _optimisticDeletedKeyCodes = MutableStateFlow<Set<String>>(emptySet())
  private val _optimisticDeletedUserSoundKeys = MutableStateFlow<Set<String>>(emptySet())

  private val _remoteSounds = MutableStateFlow<List<AdminSound>?>(null)
  private val _remoteNotice = MutableStateFlow<AdminNotice?>(null)
  private val _remoteAppConfig = MutableStateFlow<AdminAppConfig?>(null)
  private val _remoteDevConfig = MutableStateFlow<AdminDeveloperConfig?>(null)
  private val _remoteUpdateConfig = MutableStateFlow<AdminAppUpdateConfig?>(null)
  private val _remoteMaintConfig = MutableStateFlow<AdminMaintenanceConfig?>(null)
  private val _remoteVoices = MutableStateFlow<List<AdminTtsVoice>?>(null)
  private val _remoteCategories = MutableStateFlow<List<String>?>(null)
  private val _remoteUsers = MutableStateFlow<List<AdminUserRecord>?>(null)
  private val _remoteKeyGate = MutableStateFlow<AdminKeyGateConfig?>(null)
  private val _remoteKeys = MutableStateFlow<List<AdminAccessKey>?>(null)

  val authEmail: StateFlow<String?> =
    repo.authEmailFlow.stateIn(viewModelScope, SharingStarted.Eagerly, repo.currentUid())

  init {
    viewModelScope.launch {
      repo.observePublicSounds()
        .onEach { list -> AdminOfflineStore.saveSoundsCache(appContext, list) }
        .collect { _remoteSounds.value = it }
    }
    viewModelScope.launch {
      repo.observeNotice()
        .onEach { AdminOfflineStore.saveNoticeCache(appContext, it) }
        .collect { _remoteNotice.value = it }
    }
    viewModelScope.launch {
      repo.observeAppConfig()
        .onEach { AdminOfflineStore.saveAppConfigCache(appContext, it) }
        .collect { _remoteAppConfig.value = it }
    }
    viewModelScope.launch {
      repo.observeDeveloperConfig()
        .onEach { AdminOfflineStore.saveDevConfigCache(appContext, it) }
        .collect { _remoteDevConfig.value = it }
    }
    viewModelScope.launch {
      repo.observeAppUpdateConfig()
        .onEach { AdminOfflineStore.saveUpdateConfigCache(appContext, it) }
        .collect { _remoteUpdateConfig.value = it }
    }
    viewModelScope.launch {
      repo.observeMaintenanceConfig()
        .onEach { AdminOfflineStore.saveMaintConfigCache(appContext, it) }
        .collect { _remoteMaintConfig.value = it }
    }
    viewModelScope.launch {
      repo.observeTtsVoices()
        .onEach { AdminOfflineStore.saveTtsVoicesCache(appContext, it) }
        .collect { _remoteVoices.value = it }
    }
    viewModelScope.launch {
      repo.observeCategories()
        .onEach { AdminOfflineStore.saveCategoriesCache(appContext, it) }
        .collect { _remoteCategories.value = it }
    }
    viewModelScope.launch {
      repo.observeUsers()
        .onEach { AdminOfflineStore.saveUsersCache(appContext, it) }
        .collect { _remoteUsers.value = it }
    }
    viewModelScope.launch {
      repo.observeKeyGateConfig()
        .onEach { AdminOfflineStore.saveKeyGateCache(appContext, it) }
        .collect { _remoteKeyGate.value = it }
    }
    viewModelScope.launch {
      repo.observeAccessKeys()
        .onEach { AdminOfflineStore.saveAccessKeysCache(appContext, it) }
        .collect { _remoteKeys.value = it }
    }
    if (AdminOfflineStore.isNetworkAvailable(appContext)) {
      syncPendingOperations()
    }
  }

  val publicSounds: StateFlow<List<AdminSound>> =
    combine(
      _remoteSounds,
      AdminOfflineStore.cachedSounds,
      AdminOfflineStore.pendingOps,
      _optimisticUpsertedSounds,
      _optimisticDeletedSoundIds,
    ) { remote, cached, ops, optUpserts, optDeletes ->
      val base = (remote ?: cached).associateBy { it.id }.toMutableMap()
      // Apply persisted offline sound ops
      ops.forEach { op ->
        when (op.type) {
          AdminPendingOp.TYPE_SAVE_SOUND -> {
            runCatching {
              val o = JSONObject(op.payloadJson)
              val id = o.getString("soundId")
              val existing = base[id]
              val localAudio = o.optString("localAudioPath", "")
              val localThumb = o.optString("localThumbPath", "")
              val directUrl = o.optString("directAudioUrl", "")
              val useDirect = o.optBoolean("useDirectUrl", false)
              val existingAudio = o.optString("existingAudioUrl", "")
              val existingThumb = o.optString("existingThumbnailUrl", "")
              val audioStr =
                when {
                  useDirect && directUrl.isNotBlank() -> directUrl
                  localAudio.isNotBlank() && File(localAudio).exists() -> localAudio
                  else -> existingAudio
                }
              val thumbStr =
                when {
                  localThumb.isNotBlank() && File(localThumb).exists() ->
                    Uri.fromFile(File(localThumb)).toString()
                  else -> existingThumb
                }
              base[id] =
                AdminSound(
                  id = id,
                  title = o.optString("title", "Sound"),
                  category = o.optString("category", "Trending"),
                  audioUrl = audioStr,
                  thumbnailUrl = thumbStr,
                  emoji = o.optString("emoji", "🔥"),
                  defaultVolume = o.optInt("defaultVolume", 100),
                  likes = existing?.likes ?: o.optLong("likes", 0L),
                  createdBy = existing?.createdBy?.ifBlank { o.optString("uid", "admin") }
                    ?: o.optString("uid", "admin"),
                  createdAt = existing?.createdAt ?: op.createdAt,
                )
            }
          }
          AdminPendingOp.TYPE_DELETE_SOUND -> {
            runCatching {
              val id = JSONObject(op.payloadJson).getString("soundId")
              base.remove(id)
            }
          }
        }
      }
      optUpserts.forEach { (id, snd) -> base[id] = snd }
      optDeletes.forEach { id -> base.remove(id) }
      base.values.sortedByDescending { it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AdminOfflineStore.cachedSounds.value)

  val managedCategories: StateFlow<List<String>> =
    combine(
      _remoteCategories,
      AdminOfflineStore.cachedCategories,
      AdminOfflineStore.pendingOps,
      _optimisticCategories,
    ) { remote, cached, ops, optCats ->
      if (optCats != null) return@combine optCats
      val pendingCatOp = ops.lastOrNull { it.type == AdminPendingOp.TYPE_SAVE_CATEGORIES }
      if (pendingCatOp != null) {
        val parsed =
          runCatching {
            val arr = JSONObject(pendingCatOp.payloadJson).getJSONArray("categories")
            List(arr.length()) { i -> arr.getString(i) }
          }.getOrNull()
        if (parsed != null) return@combine parsed
      }
      remote ?: cached
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AdminOfflineStore.cachedCategories.value)

  val noticeConfig: StateFlow<AdminNotice> =
    combine(
      _remoteNotice,
      AdminOfflineStore.cachedNotice,
      AdminOfflineStore.pendingOps,
      _optimisticNotice,
    ) { remote, cached, ops, opt ->
      if (opt != null) return@combine opt
      val pending = ops.lastOrNull { it.type == AdminPendingOp.TYPE_SAVE_NOTICE }
      if (pending != null) {
        runCatching {
          val o = JSONObject(pending.payloadJson)
          return@combine AdminNotice(
            bannerActive = o.optBoolean("bannerActive", false),
            bannerText = o.optString("bannerText", ""),
            popupActive = o.optBoolean("popupActive", false),
            popupTitle = o.optString("popupTitle", ""),
            popupMessage = o.optString("popupMessage", ""),
          )
        }
      }
      remote ?: cached
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AdminOfflineStore.cachedNotice.value)

  val appConfig: StateFlow<AdminAppConfig> =
    combine(
      _remoteAppConfig,
      AdminOfflineStore.cachedAppConfig,
      AdminOfflineStore.pendingOps,
      _optimisticAppConfig,
    ) { remote, cached, ops, opt ->
      if (opt != null) return@combine opt
      val pending = ops.lastOrNull { it.type == AdminPendingOp.TYPE_SAVE_APP_BRANDING }
      if (pending != null) {
        runCatching {
          val o = JSONObject(pending.payloadJson)
          val localImg = o.optString("localImagePath", "")
          val resolved =
            if (localImg.isNotBlank() && File(localImg).exists()) Uri.fromFile(File(localImg)).toString()
            else o.optString("currentUrl", "")
          return@combine AdminAppConfig(
            logoUrl = resolved,
            floatingBrandName = o.optString("brandName", "ZONEX").ifBlank { "ZONEX" },
          )
        }
      }
      remote ?: cached
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AdminOfflineStore.cachedAppConfig.value)

  val developerConfig: StateFlow<AdminDeveloperConfig> =
    combine(
      _remoteDevConfig,
      AdminOfflineStore.cachedDevConfig,
      AdminOfflineStore.pendingOps,
      _optimisticDevConfig,
    ) { remote, cached, ops, opt ->
      if (opt != null) return@combine opt
      val pending = ops.lastOrNull { it.type == AdminPendingOp.TYPE_SAVE_DEV_CONFIG }
      if (pending != null) {
        runCatching {
          val o = JSONObject(pending.payloadJson)
          val localImg = o.optString("localImagePath", "")
          val logo =
            if (localImg.isNotBlank() && File(localImg).exists()) Uri.fromFile(File(localImg)).toString()
            else o.optString("logoUrl", "")
          val btnsArr = o.optJSONArray("buttons") ?: JSONArray()
          val btns = List(btnsArr.length()) { idx ->
            val b = btnsArr.getJSONObject(idx)
            AdminDevButton(
              label = b.optString("label", ""),
              url = b.optString("url", ""),
              color = b.optString("color", "#F5B82E"),
            )
          }
          return@combine AdminDeveloperConfig(
            name = o.optString("name", "Zonex Official"),
            logoUrl = logo,
            bio = o.optString("bio", ""),
            buttons = btns,
          )
        }
      }
      remote ?: cached
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AdminOfflineStore.cachedDevConfig.value)

  val appUpdateConfig: StateFlow<AdminAppUpdateConfig> =
    combine(
      _remoteUpdateConfig,
      AdminOfflineStore.cachedUpdateConfig,
      AdminOfflineStore.pendingOps,
      _optimisticAppUpdate,
    ) { remote, cached, ops, opt ->
      if (opt != null) return@combine opt
      val pending = ops.lastOrNull { it.type == AdminPendingOp.TYPE_SAVE_APP_UPDATE }
      if (pending != null) {
        runCatching {
          val o = JSONObject(pending.payloadJson)
          return@combine AdminAppUpdateConfig(
            enabled = o.optBoolean("enabled", false),
            latestVersionName = o.optString("latestVersionName", "2.2"),
            latestVersionCode = o.optInt("latestVersionCode", 22),
            forceUpdate = o.optBoolean("forceUpdate", false),
            title = o.optString("title", "নতুন আপডেট এসেছে!"),
            message = o.optString("message", ""),
            downloadUrl = o.optString("downloadUrl", ""),
          )
        }
      }
      remote ?: cached
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AdminOfflineStore.cachedUpdateConfig.value)

  val maintenanceConfig: StateFlow<AdminMaintenanceConfig> =
    combine(
      _remoteMaintConfig,
      AdminOfflineStore.cachedMaintConfig,
      AdminOfflineStore.pendingOps,
      _optimisticMaintenance,
    ) { remote, cached, ops, opt ->
      if (opt != null) return@combine opt
      val pending = ops.lastOrNull { it.type == AdminPendingOp.TYPE_SAVE_MAINTENANCE }
      if (pending != null) {
        runCatching {
          val o = JSONObject(pending.payloadJson)
          return@combine AdminMaintenanceConfig(
            enabled = o.optBoolean("enabled", false),
            title = o.optString("title", "কাজ চলতেছে"),
            message = o.optString("message", ""),
          )
        }
      }
      remote ?: cached
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AdminOfflineStore.cachedMaintConfig.value)

  val ttsVoices: StateFlow<List<AdminTtsVoice>> =
    combine(
      _remoteVoices,
      AdminOfflineStore.cachedTtsVoices,
      AdminOfflineStore.pendingOps,
      _optimisticUpsertedVoices,
      _optimisticDeletedVoiceIds,
    ) { remote, cached, ops, optUp, optDel ->
      val map = (remote ?: cached).associateBy { it.id }.toMutableMap()
      ops.forEach { op ->
        when (op.type) {
          AdminPendingOp.TYPE_SAVE_TTS_VOICE -> {
            runCatching {
              val o = JSONObject(op.payloadJson)
              val id = o.getString("id")
              val localAvatar = o.optString("localAvatarPath", "")
              val avatar =
                if (localAvatar.isNotBlank() && File(localAvatar).exists()) {
                  Uri.fromFile(File(localAvatar)).toString()
                } else {
                  o.optString("avatarUrl", "")
                }
              map[id] =
                AdminTtsVoice(
                  id = id,
                  name = o.optString("name", ""),
                  cartesiaVoiceId = o.optString("cartesiaVoiceId", ""),
                  language = o.optString("language", "Multi"),
                  avatarUrl = avatar,
                  badgeColorHex = o.optString("badgeColorHex", "#16A34A"),
                  sortOrder = o.optInt("sortOrder", 0),
                )
            }
          }
          AdminPendingOp.TYPE_DELETE_TTS_VOICE -> {
            runCatching { map.remove(JSONObject(op.payloadJson).getString("id")) }
          }
        }
      }
      optUp.forEach { (k, v) -> map[k] = v }
      optDel.forEach { k -> map.remove(k) }
      map.values.sortedBy { it.sortOrder }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AdminOfflineStore.cachedTtsVoices.value)

  val keyGateConfig: StateFlow<AdminKeyGateConfig> =
    combine(
      _remoteKeyGate,
      AdminOfflineStore.cachedKeyGate,
      AdminOfflineStore.pendingOps,
      _optimisticKeyGate,
    ) { remote, cached, ops, opt ->
      if (opt != null) return@combine opt
      val pending = ops.lastOrNull { it.type == AdminPendingOp.TYPE_SAVE_KEY_GATE }
      if (pending != null) {
        runCatching {
          val o = JSONObject(pending.payloadJson)
          val arr = o.optJSONArray("socialLinks") ?: JSONArray()
          val links = List(arr.length()) { i ->
            val s = arr.getJSONObject(i)
            AdminKeySocialLink(
              platform = s.optString("platform", "Telegram"),
              label = s.optString("label", ""),
              url = s.optString("url", ""),
              color = s.optString("color", "#0EA5E9"),
            )
          }
          return@combine AdminKeyGateConfig(
            requireKeyAfterLogin = o.optBoolean("requireKeyAfterLogin", true),
            getKeyUrl = o.optString("getKeyUrl", ""),
            socialLinks = links,
          )
        }
      }
      remote ?: cached
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AdminOfflineStore.cachedKeyGate.value)

  val accessKeys: StateFlow<List<AdminAccessKey>> =
    combine(
      _remoteKeys,
      AdminOfflineStore.cachedAccessKeys,
      AdminOfflineStore.pendingOps,
      _optimisticUpsertedKeys,
      _optimisticDeletedKeyCodes,
    ) { remote, cached, ops, optUp, optDel ->
      val map = (remote ?: cached).associateBy { it.code }.toMutableMap()
      ops.forEach { op ->
        when (op.type) {
          AdminPendingOp.TYPE_UPSERT_KEY -> {
            runCatching {
              val o = JSONObject(op.payloadJson)
              val code = o.getString("code")
              map[code] =
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
            }
          }
          AdminPendingOp.TYPE_TOGGLE_REVOKE_KEY -> {
            runCatching {
              val o = JSONObject(op.payloadJson)
              val code = o.getString("code")
              val rev = o.getBoolean("revoked")
              map[code]?.let { map[code] = it.copy(isRevoked = rev) }
            }
          }
          AdminPendingOp.TYPE_RESET_KEY -> {
            runCatching {
              val o = JSONObject(op.payloadJson)
              val code = o.getString("code")
              val now = o.getLong("now")
              val exp = o.getLong("expiresAt")
              map[code]?.let {
                map[code] =
                  it.copy(
                    createdAt = now,
                    activatedAt = now,
                    expiresAt = exp,
                    boundUid = "",
                    boundDeviceId = "",
                    isRevoked = false,
                  )
              }
            }
          }
          AdminPendingOp.TYPE_DELETE_KEY -> {
            runCatching { map.remove(JSONObject(op.payloadJson).getString("code")) }
          }
        }
      }
      optUp.forEach { (k, v) -> map[k] = v }
      optDel.forEach { code -> map.remove(code) }
      map.values.sortedByDescending { it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AdminOfflineStore.cachedAccessKeys.value)

  val usersList: StateFlow<List<AdminUserRecord>> =
    combine(
      _remoteUsers,
      AdminOfflineStore.cachedUsers,
      publicSounds,
      authEmail,
      combine(AdminOfflineStore.pendingOps, _optimisticDeletedUserSoundKeys) { ops, optDel -> ops to optDel },
    ) { remoteUsers, cachedUsers, pubSounds, _, (ops, optDel) ->
      val rawUsers = remoteUsers ?: cachedUsers
      val deletedUserKeys =
        buildSet {
          addAll(optDel)
          ops.filter { it.type == AdminPendingOp.TYPE_DELETE_USER_SOUND }.forEach { op ->
            runCatching {
              val o = JSONObject(op.payloadJson)
              add("${o.getString("uid")}::${o.getString("soundId")}")
            }
          }
        }
      val currentAdminUid = repo.currentUid().orEmpty()
      val publicByCreator =
        pubSounds
          .filter { it.createdBy.isNotBlank() }
          .groupBy { it.createdBy }

      val userMap = LinkedHashMap<String, AdminUserRecord>()
      rawUsers.forEach { u ->
        val filteredPriv =
          u.uploadedSounds.filterNot { deletedUserKeys.contains("${u.uid}::${it.id}") }
        val pubUploads =
          publicByCreator[u.uid].orEmpty().map { s ->
            AdminUserSoundItem(
              id = s.id,
              title = s.title,
              category = s.category,
              audioUrl = s.audioUrl,
              isPublic = true,
              createdAt = s.createdAt,
            )
          }
        val combined = (filteredPriv + pubUploads).sortedByDescending { it.createdAt }
        userMap[u.uid] =
          u.copy(
            privateCount = filteredPriv.size,
            publicUploadCount = pubUploads.size,
            isAdminAccount = u.uid == currentAdminUid,
            uploadedSounds = combined,
          )
      }

      publicByCreator.forEach { (creatorUid, list) ->
        if (!userMap.containsKey(creatorUid)) {
          val pubItems =
            list.map { s ->
              AdminUserSoundItem(
                id = s.id,
                title = s.title,
                category = s.category,
                audioUrl = s.audioUrl,
                isPublic = true,
                createdAt = s.createdAt,
              )
            }.sortedByDescending { it.createdAt }
          userMap[creatorUid] =
            AdminUserRecord(
              uid = creatorUid,
              publicUploadCount = pubItems.size,
              isAdminAccount = creatorUid == currentAdminUid,
              uploadedSounds = pubItems,
            )
        }
      }

      userMap.values.sortedWith(
        compareByDescending<AdminUserRecord> { it.totalAddedCount }
          .thenBy { it.isAdminAccount }
      )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AdminOfflineStore.cachedUsers.value)

  private val _selectedTab = MutableStateFlow(AdminTab.SOUNDS)
  val selectedTab: StateFlow<AdminTab> = _selectedTab.asStateFlow()

  private val _isLoggingIn = MutableStateFlow(false)
  val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

  private val _loginError = MutableStateFlow<String?>(null)
  val loginError: StateFlow<String?> = _loginError.asStateFlow()

  private val _playingSoundId = MutableStateFlow<String?>(null)
  val playingSoundId: StateFlow<String?> = _playingSoundId.asStateFlow()

  private val _soundEditorDraft = MutableStateFlow<AdminSoundDraft?>(null)
  val soundEditorDraft: StateFlow<AdminSoundDraft?> = _soundEditorDraft.asStateFlow()

  private val _isSavingSound = MutableStateFlow(false)
  val isSavingSound: StateFlow<Boolean> = _isSavingSound.asStateFlow()

  private val _saveProgress = MutableStateFlow(0f)
  val saveProgress: StateFlow<Float> = _saveProgress.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private fun registerNetworkObserver() {
    val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
    val cb =
      object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          _isOnline.value = true
          syncPendingOperations()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
          val ok =
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
              networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
          _isOnline.value = ok
          if (ok && AdminOfflineStore.pendingOps.value.isNotEmpty()) {
            syncPendingOperations()
          }
        }

        override fun onLost(network: Network) {
          _isOnline.value = AdminOfflineStore.isNetworkAvailable(appContext)
        }
      }
    networkCallback = cb
    runCatching {
      val req =
        NetworkRequest.Builder()
          .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
          .build()
      cm.registerNetworkCallback(req, cb)
    }
  }

  fun selectTab(tab: AdminTab) {
    _selectedTab.value = tab
  }

  fun signIn(email: String, password: String) {
    if (email.isBlank() || password.isBlank()) {
      _loginError.value = "Enter admin email and password."
      return
    }
    viewModelScope.launch {
      _isLoggingIn.value = true
      _loginError.value = null
      repo.signIn(email, password)
        .onSuccess {
          if (AdminOfflineStore.isNetworkAvailable(appContext)) {
            syncPendingOperations()
          }
        }
        .onFailure { _loginError.value = it.localizedMessage ?: "Login failed" }
      _isLoggingIn.value = false
    }
  }

  fun signOut() {
    stopPreview()
    repo.signOut()
  }

  fun togglePlayPreview(sound: AdminSound) {
    previewAudio(sound.id, sound.audioUrl)
  }

  fun togglePlayUserSound(item: AdminUserSoundItem) {
    previewAudio("user_${item.id}", item.audioUrl)
  }

  private fun previewAudio(playKey: String, audioUrl: String) {
    if (_playingSoundId.value == playKey) {
      stopPreview()
      return
    }
    stopPreview()
    if (audioUrl.isBlank()) {
      _statusMessage.value = "Sound URL is empty"
      return
    }
    viewModelScope.launch {
      runCatching {
        val dataSource =
          when {
            audioUrl.startsWith("data:", ignoreCase = true) -> {
              withContext(Dispatchers.IO) {
                val b64 = audioUrl.substringAfter("base64,", "")
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val ext = if (audioUrl.contains("mp4", ignoreCase = true)) "m4a" else "mp3"
                val outFile = File(appContext.cacheDir, "admin_preview_${playKey.hashCode()}.$ext")
                outFile.writeBytes(bytes)
                outFile.absolutePath
              }
            }
            audioUrl.startsWith("file://", ignoreCase = true) -> {
              Uri.parse(audioUrl).path ?: audioUrl
            }
            else -> audioUrl
          }
        previewPlayer =
          MediaPlayer().apply {
            setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            )
            setDataSource(dataSource)
            setOnPreparedListener {
              it.start()
              _playingSoundId.value = playKey
            }
            setOnCompletionListener { _playingSoundId.value = null }
            setOnErrorListener { _, _, _ ->
              _playingSoundId.value = null
              _statusMessage.value = "Failed to play preview"
              true
            }
            prepareAsync()
          }
      }.onFailure { _statusMessage.value = it.localizedMessage }
    }
  }

  fun stopPreview() {
    runCatching {
      previewPlayer?.stop()
      previewPlayer?.release()
    }
    previewPlayer = null
    _playingSoundId.value = null
  }

  fun addCategory(rawName: String) {
    val clean = rawName.trim()
    if (clean.isBlank()) return
    if (clean.equals("All", ignoreCase = true) || clean.equals("Trending", ignoreCase = true)) {
      _statusMessage.value = "'All' and 'Trending' are fixed categories"
      return
    }
    val current = managedCategories.value
    if (current.any { it.equals(clean, ignoreCase = true) }) {
      _statusMessage.value = "Category \"$clean\" already exists"
      return
    }
    val updated = current + clean
    saveCategoriesOptimistic(updated, "Added category \"$clean\"")
  }

  fun removeCategory(cat: String) {
    if (cat.equals("All", ignoreCase = true) || cat.equals("Trending", ignoreCase = true)) return
    val updated = managedCategories.value.filterNot { it.equals(cat, ignoreCase = true) }
    saveCategoriesOptimistic(updated, "Removed category \"$cat\"")
  }

  private fun saveCategoriesOptimistic(updated: List<String>, successMsg: String) {
    _optimisticCategories.value = updated
    _statusMessage.value = successMsg
    viewModelScope.launch {
      AdminOfflineStore.saveCategoriesCache(appContext, updated)
      if (!AdminOfflineStore.isNetworkAvailable(appContext)) {
        enqueueCategoriesOp(updated)
        _optimisticCategories.value = null
        _statusMessage.value = "$successMsg (অফলাইন কিউতে সেভ)"
        return@launch
      }
      repo.saveCategories(updated)
        .onFailure {
          enqueueCategoriesOp(updated)
          _statusMessage.value = "$successMsg (অফলাইনে সেভ, নেট এলে সিঙ্ক)"
        }
      _optimisticCategories.value = null
    }
  }

  private suspend fun enqueueCategoriesOp(categories: List<String>) {
    val json = JSONObject().apply {
      put("categories", JSONArray(categories))
    }
    AdminOfflineStore.enqueueOp(
      appContext,
      AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_SAVE_CATEGORIES, json.toString(), System.currentTimeMillis()),
      replacePredicate = { it.type == AdminPendingOp.TYPE_SAVE_CATEGORIES },
    )
  }

  fun openNewSoundDialog() {
    val defaultCat = managedCategories.value.firstOrNull() ?: "General"
    _soundEditorDraft.value = AdminSoundDraft(category = defaultCat)
  }

  fun openEditSoundDialog(sound: AdminSound) {
    _soundEditorDraft.value =
      AdminSoundDraft(
        editingId = sound.id,
        title = sound.title,
        category = sound.category,
        emoji = sound.emoji,
        defaultVolume = sound.defaultVolume,
        useDirectUrl = true,
        directAudioUrl = sound.audioUrl,
        existingAudioUrl = sound.audioUrl,
        existingThumbnailUrl = sound.thumbnailUrl,
      )
  }

  fun updateDraft(transform: (AdminSoundDraft) -> AdminSoundDraft) {
    _soundEditorDraft.update { it?.let(transform) }
  }

  fun closeSoundDialog() {
    if (!_isSavingSound.value) _soundEditorDraft.value = null
  }

  fun saveSoundDraft() {
    val draft = _soundEditorDraft.value ?: return
    if (draft.title.isBlank()) {
      _statusMessage.value = "Enter sound title"
      return
    }
    val isNew = draft.editingId == null
    val soundId = draft.editingId ?: repo.newPublicSoundId()
    val existing = publicSounds.value.firstOrNull { it.id == soundId }
    val uid = repo.currentUid() ?: "admin"
    val now = existing?.createdAt ?: System.currentTimeMillis()

    viewModelScope.launch {
      _isSavingSound.value = true
      _saveProgress.value = 0.1f

      // Close editor cleanly or keep brief spinner while copying local media
      val hasInternet = AdminOfflineStore.isNetworkAvailable(appContext)
      if (!hasInternet) {
        val result =
          runCatching {
            val localAudioPath =
              if (!draft.useDirectUrl && draft.audioUri != null) {
                AdminOfflineStore.copyMediaFile(
                  appContext,
                  draft.audioFileName.ifBlank { "${soundId}_audio.mp3" },
                  draft.audioUri,
                )
              } else {
                ""
              }
            val localThumbPath =
              if (draft.thumbnailUri != null) {
                AdminOfflineStore.copyCompressedImage(appContext, "${soundId}_thumb", draft.thumbnailUri)
              } else {
                ""
              }
            if (localAudioPath.isBlank() && !(draft.useDirectUrl && draft.directAudioUrl.isNotBlank()) && draft.existingAudioUrl.isBlank()) {
              error("Select an audio/video file or paste a direct audio URL.")
            }
            val payload =
              JSONObject().apply {
                put("soundId", soundId)
                put("isNewSound", isNew)
                put("uid", uid)
                put("title", draft.title.trim())
                put("category", draft.category)
                put("emoji", draft.emoji)
                put("defaultVolume", draft.defaultVolume.coerceIn(0, 100))
                put("useDirectUrl", draft.useDirectUrl)
                put("directAudioUrl", draft.directAudioUrl.trim())
                put("existingAudioUrl", draft.existingAudioUrl)
                put("existingThumbnailUrl", draft.existingThumbnailUrl)
                put("localAudioPath", localAudioPath)
                put("audioFileName", draft.audioFileName.ifBlank { File(localAudioPath).name.ifBlank { "audio.mp3" } })
                put("localThumbPath", localThumbPath)
                put("likes", existing?.likes ?: 0L)
              }
            AdminOfflineStore.enqueueOp(
              appContext,
              AdminPendingOp(
                opId = UUID.randomUUID().toString(),
                type = AdminPendingOp.TYPE_SAVE_SOUND,
                payloadJson = payload.toString(),
                createdAt = now,
              ),
              replacePredicate = {
                (it.type == AdminPendingOp.TYPE_SAVE_SOUND || it.type == AdminPendingOp.TYPE_DELETE_SOUND) &&
                  runCatching { JSONObject(it.payloadJson).optString("soundId") == soundId }.getOrDefault(false)
              },
            )
          }
        _isSavingSound.value = false
        result
          .onSuccess {
            _soundEditorDraft.value = null
            _statusMessage.value = "অফলাইনে সেভ হয়েছে! নেট এলে অটো আপলোড হবে।"
          }
          .onFailure { _statusMessage.value = it.localizedMessage ?: "Save failed" }
        return@launch
      }

      // Online: Optimistic item visible right away, then upload in Firebase
      val optimisticThumb =
        draft.thumbnailUri?.toString()?.takeIf { it.isNotBlank() } ?: draft.existingThumbnailUrl
      val optimisticAudio =
        when {
          draft.useDirectUrl && draft.directAudioUrl.isNotBlank() -> draft.directAudioUrl.trim()
          draft.audioUri != null -> draft.audioUri.toString()
          else -> draft.existingAudioUrl
        }
      val optimisticItem =
        AdminSound(
          id = soundId,
          title = draft.title.trim(),
          category = draft.category,
          audioUrl = optimisticAudio,
          thumbnailUrl = optimisticThumb,
          emoji = draft.emoji,
          defaultVolume = draft.defaultVolume.coerceIn(0, 100),
          likes = existing?.likes ?: 0L,
          createdBy = existing?.createdBy?.ifBlank { uid } ?: uid,
          createdAt = now,
        )
      _optimisticDeletedSoundIds.update { it - soundId }
      _optimisticUpsertedSounds.update { it + (soundId to optimisticItem) }
      _soundEditorDraft.value = null
      _isSavingSound.value = false

      repo.savePublicSound(
        context = appContext,
        draft = draft,
        forcedId = soundId,
        isNewSound = isNew,
      ) { _saveProgress.value = it }
        .onSuccess {
          AdminOfflineStore.removePendingOpsMatching(appContext) {
            it.type == AdminPendingOp.TYPE_SAVE_SOUND &&
              runCatching { JSONObject(it.payloadJson).optString("soundId") == soundId }.getOrDefault(false)
          }
          _optimisticUpsertedSounds.update { it - soundId }
          _statusMessage.value = "Public sound saved to Firebase!"
        }
        .onFailure { err ->
          // Fallback to offline queue if upload interrupted due to network drop
          if (!AdminOfflineStore.isNetworkAvailable(appContext)) {
            val fallbackLocalAudio =
              runCatching {
                if (!draft.useDirectUrl && draft.audioUri != null) {
                  AdminOfflineStore.copyMediaFile(
                    appContext,
                    draft.audioFileName.ifBlank { "${soundId}_audio.mp3" },
                    draft.audioUri,
                  )
                } else ""
              }.getOrDefault("")
            val fallbackThumb =
              runCatching {
                if (draft.thumbnailUri != null) {
                  AdminOfflineStore.copyCompressedImage(appContext, "${soundId}_thumb", draft.thumbnailUri)
                } else ""
              }.getOrDefault("")
            val payload =
              JSONObject().apply {
                put("soundId", soundId)
                put("isNewSound", isNew)
                put("uid", uid)
                put("title", draft.title.trim())
                put("category", draft.category)
                put("emoji", draft.emoji)
                put("defaultVolume", draft.defaultVolume.coerceIn(0, 100))
                put("useDirectUrl", draft.useDirectUrl)
                put("directAudioUrl", draft.directAudioUrl.trim())
                put("existingAudioUrl", draft.existingAudioUrl)
                put("existingThumbnailUrl", draft.existingThumbnailUrl)
                put("localAudioPath", fallbackLocalAudio)
                put("audioFileName", draft.audioFileName.ifBlank { File(fallbackLocalAudio).name.ifBlank { "audio.mp3" } })
                put("localThumbPath", fallbackThumb)
                put("likes", existing?.likes ?: 0L)
              }
            AdminOfflineStore.enqueueOp(
              appContext,
              AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_SAVE_SOUND, payload.toString(), now),
            )
            _optimisticUpsertedSounds.update { it - soundId }
            _statusMessage.value = "নেট বিচ্ছিন্ন! অফলাইনে সেভ রয়েছে, নেট এলে সিঙ্ক হবে।"
          } else {
            _optimisticUpsertedSounds.update { it - soundId }
            _statusMessage.value = err.localizedMessage ?: "Failed to save sound"
          }
        }
    }
  }

  fun deletePublicSound(sound: AdminSound) {
    if (_playingSoundId.value == sound.id) stopPreview()
    // Instant removal from UI
    _optimisticUpsertedSounds.update { it - sound.id }
    _optimisticDeletedSoundIds.update { it + sound.id }
    _statusMessage.value = "Deleted \"${sound.title}\""

    viewModelScope.launch {
      // Remove any pending SAVE_SOUND for this sound first
      val hadUnsyncedSave =
        AdminOfflineStore.pendingOps.value.any {
          it.type == AdminPendingOp.TYPE_SAVE_SOUND &&
            runCatching { JSONObject(it.payloadJson).optString("soundId") == sound.id }.getOrDefault(false)
        }
      AdminOfflineStore.removePendingOpsMatching(appContext) {
        it.type == AdminPendingOp.TYPE_SAVE_SOUND &&
          runCatching { JSONObject(it.payloadJson).optString("soundId") == sound.id }.getOrDefault(false)
      }
      if (hadUnsyncedSave) {
        _optimisticDeletedSoundIds.update { it - sound.id }
        return@launch
      }

      if (!AdminOfflineStore.isNetworkAvailable(appContext)) {
        enqueueDeleteSoundOp(sound.id)
        _optimisticDeletedSoundIds.update { it - sound.id }
        _statusMessage.value = "\"${sound.title}\" অফলাইন ডিলিট কিউতে যুক্ত হয়েছে"
        return@launch
      }

      repo.deletePublicSound(sound.id)
        .onFailure {
          enqueueDeleteSoundOp(sound.id)
          _statusMessage.value = "অফলাইন ডিলিট কিউতে রাখা হয়েছে"
        }
      _optimisticDeletedSoundIds.update { it - sound.id }
    }
  }

  private suspend fun enqueueDeleteSoundOp(soundId: String) {
    val json = JSONObject().apply { put("soundId", soundId) }
    AdminOfflineStore.enqueueOp(
      appContext,
      AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_DELETE_SOUND, json.toString(), System.currentTimeMillis()),
      replacePredicate = {
        it.type == AdminPendingOp.TYPE_DELETE_SOUND &&
          runCatching { JSONObject(it.payloadJson).optString("soundId") == soundId }.getOrDefault(false)
      },
    )
  }

  fun deleteUserSound(uid: String, item: AdminUserSoundItem) {
    val key = "$uid::${item.id}"
    _optimisticDeletedUserSoundKeys.update { it + key }
    if (item.isPublic) _optimisticDeletedSoundIds.update { it + item.id }
    _statusMessage.value = "Deleted \"${item.title}\""

    viewModelScope.launch {
      if (!AdminOfflineStore.isNetworkAvailable(appContext)) {
        val json = JSONObject().apply {
          put("uid", uid)
          put("soundId", item.id)
          put("isPublic", item.isPublic)
        }
        AdminOfflineStore.enqueueOp(
          appContext,
          AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_DELETE_USER_SOUND, json.toString(), System.currentTimeMillis()),
        )
        _optimisticDeletedUserSoundKeys.update { it - key }
        _optimisticDeletedSoundIds.update { it - item.id }
        return@launch
      }

      val result =
        if (item.isPublic) repo.deletePublicSound(item.id)
        else repo.deleteUserPrivateSound(uid, item.id)
      result.onFailure {
        val json = JSONObject().apply {
          put("uid", uid)
          put("soundId", item.id)
          put("isPublic", item.isPublic)
        }
        AdminOfflineStore.enqueueOp(
          appContext,
          AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_DELETE_USER_SOUND, json.toString(), System.currentTimeMillis()),
        )
      }
      _optimisticDeletedUserSoundKeys.update { it - key }
      _optimisticDeletedSoundIds.update { it - item.id }
    }
  }

  fun saveNotice(notice: AdminNotice) {
    _optimisticNotice.value = notice
    _statusMessage.value = "Notice updated live!"
    viewModelScope.launch {
      AdminOfflineStore.saveNoticeCache(appContext, notice)
      if (!AdminOfflineStore.isNetworkAvailable(appContext)) {
        enqueueNoticeOp(notice)
        _optimisticNotice.value = null
        _statusMessage.value = "অফলাইনে নোটিশ সেভ হয়েছে, নেট এলে সিঙ্ক হবে।"
        return@launch
      }
      repo.saveNotice(notice)
        .onFailure {
          enqueueNoticeOp(notice)
          _statusMessage.value = "অফলাইনে নোটিশ সেভ হয়েছে।"
        }
      _optimisticNotice.value = null
    }
  }

  private suspend fun enqueueNoticeOp(notice: AdminNotice) {
    val json = JSONObject().apply {
      put("bannerActive", notice.bannerActive)
      put("bannerText", notice.bannerText)
      put("popupActive", notice.popupActive)
      put("popupTitle", notice.popupTitle)
      put("popupMessage", notice.popupMessage)
    }
    AdminOfflineStore.enqueueOp(
      appContext,
      AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_SAVE_NOTICE, json.toString(), System.currentTimeMillis()),
      replacePredicate = { it.type == AdminPendingOp.TYPE_SAVE_NOTICE },
    )
  }

  fun saveMaintenanceConfig(config: AdminMaintenanceConfig) {
    _optimisticMaintenance.value = config
    _statusMessage.value =
      if (config.enabled) "Maintenance mode ENABLED for User App!"
      else "Maintenance mode DISABLED!"
    viewModelScope.launch {
      AdminOfflineStore.saveMaintConfigCache(appContext, config)
      if (!AdminOfflineStore.isNetworkAvailable(appContext)) {
        enqueueMaintenanceOp(config)
        _optimisticMaintenance.value = null
        _statusMessage.value = "অফলাইনে Maintenance সেভ হয়েছে।"
        return@launch
      }
      repo.saveMaintenanceConfig(config)
        .onFailure { enqueueMaintenanceOp(config) }
      _optimisticMaintenance.value = null
    }
  }

  private suspend fun enqueueMaintenanceOp(config: AdminMaintenanceConfig) {
    val json = JSONObject().apply {
      put("enabled", config.enabled)
      put("title", config.title)
      put("message", config.message)
    }
    AdminOfflineStore.enqueueOp(
      appContext,
      AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_SAVE_MAINTENANCE, json.toString(), System.currentTimeMillis()),
      replacePredicate = { it.type == AdminPendingOp.TYPE_SAVE_MAINTENANCE },
    )
  }

  fun saveAppUpdateConfig(config: AdminAppUpdateConfig) {
    _optimisticAppUpdate.value = config
    _statusMessage.value = "App Update popup config saved live!"
    viewModelScope.launch {
      AdminOfflineStore.saveUpdateConfigCache(appContext, config)
      if (!AdminOfflineStore.isNetworkAvailable(appContext)) {
        enqueueUpdateOp(config)
        _optimisticAppUpdate.value = null
        _statusMessage.value = "অফলাইনে App Update সেভ হয়েছে।"
        return@launch
      }
      repo.saveAppUpdateConfig(config)
        .onFailure { enqueueUpdateOp(config) }
      _optimisticAppUpdate.value = null
    }
  }

  private suspend fun enqueueUpdateOp(config: AdminAppUpdateConfig) {
    val json = JSONObject().apply {
      put("enabled", config.enabled)
      put("latestVersionName", config.latestVersionName)
      put("latestVersionCode", config.latestVersionCode)
      put("targetVersionName", config.targetVersionName)
      put("forceUpdate", config.forceUpdate)
      put("title", config.title)
      put("message", config.message)
      put("downloadUrl", config.downloadUrl)
    }
    AdminOfflineStore.enqueueOp(
      appContext,
      AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_SAVE_APP_UPDATE, json.toString(), System.currentTimeMillis()),
      replacePredicate = { it.type == AdminPendingOp.TYPE_SAVE_APP_UPDATE },
    )
  }

  fun saveTtsVoice(voice: AdminTtsVoice, pickedAvatarUri: Uri?) {
    if (voice.name.isBlank() || voice.cartesiaVoiceId.isBlank()) {
      _statusMessage.value = "Enter character name and Cartesia Voice ID"
      return
    }
    val resolvedId = voice.id.trim().ifBlank { "v_${System.currentTimeMillis()}" }
    val normalized = voice.copy(
      id = resolvedId,
      avatarUrl = pickedAvatarUri?.toString() ?: voice.avatarUrl,
    )
    _optimisticDeletedVoiceIds.update { it - resolvedId }
    _optimisticUpsertedVoices.update { it + (resolvedId to normalized) }
    _statusMessage.value = "Saved voice \"${voice.name}\"!"

    viewModelScope.launch {
      if (!AdminOfflineStore.isNetworkAvailable(appContext)) {
        enqueueTtsVoiceOp(voice.copy(id = resolvedId), pickedAvatarUri)
        _optimisticUpsertedVoices.update { it - resolvedId }
        _statusMessage.value = "অফলাইনে ভয়েস সেভ হয়েছে।"
        return@launch
      }
      repo.saveTtsVoice(appContext, voice.copy(id = resolvedId), pickedAvatarUri)
        .onFailure { enqueueTtsVoiceOp(voice.copy(id = resolvedId), pickedAvatarUri) }
      _optimisticUpsertedVoices.update { it - resolvedId }
    }
  }

  private suspend fun enqueueTtsVoiceOp(voice: AdminTtsVoice, pickedAvatarUri: Uri?) {
    val localAvatar =
      if (pickedAvatarUri != null) {
        runCatching { AdminOfflineStore.copyCompressedImage(appContext, "tts_${voice.id}", pickedAvatarUri) }.getOrDefault("")
      } else ""
    val json = JSONObject().apply {
      put("id", voice.id)
      put("name", voice.name)
      put("cartesiaVoiceId", voice.cartesiaVoiceId)
      put("language", voice.language)
      put("avatarUrl", voice.avatarUrl)
      put("localAvatarPath", localAvatar)
      put("badgeColorHex", voice.badgeColorHex)
      put("sortOrder", voice.sortOrder)
    }
    AdminOfflineStore.enqueueOp(
      appContext,
      AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_SAVE_TTS_VOICE, json.toString(), System.currentTimeMillis()),
      replacePredicate = {
        (it.type == AdminPendingOp.TYPE_SAVE_TTS_VOICE || it.type == AdminPendingOp.TYPE_DELETE_TTS_VOICE) &&
          runCatching { JSONObject(it.payloadJson).optString("id") == voice.id }.getOrDefault(false)
      },
    )
  }

  fun deleteTtsVoice(voice: AdminTtsVoice) {
    _optimisticUpsertedVoices.update { it - voice.id }
    _optimisticDeletedVoiceIds.update { it + voice.id }
    _statusMessage.value = "Deleted voice \"${voice.name}\""
    viewModelScope.launch {
      AdminOfflineStore.removePendingOpsMatching(appContext) {
        it.type == AdminPendingOp.TYPE_SAVE_TTS_VOICE &&
          runCatching { JSONObject(it.payloadJson).optString("id") == voice.id }.getOrDefault(false)
      }
      if (!AdminOfflineStore.isNetworkAvailable(appContext)) {
        val json = JSONObject().apply { put("id", voice.id) }
        AdminOfflineStore.enqueueOp(
          appContext,
          AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_DELETE_TTS_VOICE, json.toString(), System.currentTimeMillis()),
        )
        _optimisticDeletedVoiceIds.update { it - voice.id }
        return@launch
      }
      repo.deleteTtsVoice(voice.id)
        .onFailure {
          val json = JSONObject().apply { put("id", voice.id) }
          AdminOfflineStore.enqueueOp(
            appContext,
            AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_DELETE_TTS_VOICE, json.toString(), System.currentTimeMillis()),
          )
        }
      _optimisticDeletedVoiceIds.update { it - voice.id }
    }
  }

  fun saveAppBranding(brandName: String, currentUrl: String, pickedImageUri: Uri?) {
    val opt = AdminAppConfig(
      logoUrl = pickedImageUri?.toString() ?: currentUrl,
      floatingBrandName = brandName.ifBlank { "ZONEX" },
    )
    _optimisticAppConfig.value = opt
    _statusMessage.value = "Floating window logo & brand updated!"

    viewModelScope.launch {
      if (!AdminOfflineStore.isNetworkAvailable(appContext)) {
        enqueueAppBrandingOp(brandName, currentUrl, pickedImageUri)
        _optimisticAppConfig.value = null
        _statusMessage.value = "অফলাইনে Branding সেভ হয়েছে।"
        return@launch
      }
      val resolvedUrl =
        if (pickedImageUri != null) {
          repo.uploadBrandingImage(appContext, "app_logo", pickedImageUri).getOrElse {
            enqueueAppBrandingOp(brandName, currentUrl, pickedImageUri)
            _optimisticAppConfig.value = null
            _statusMessage.value = "অফলাইনে Branding কিউতে সেভ হয়েছে।"
            return@launch
          }
        } else {
          currentUrl
        }
      repo.saveAppConfig(AdminAppConfig(logoUrl = resolvedUrl, floatingBrandName = brandName))
        .onFailure { enqueueAppBrandingOp(brandName, resolvedUrl, null) }
      _optimisticAppConfig.value = null
    }
  }

  private suspend fun enqueueAppBrandingOp(brandName: String, currentUrl: String, pickedUri: Uri?) {
    val localPath =
      if (pickedUri != null) runCatching { AdminOfflineStore.copyCompressedImage(appContext, "app_logo", pickedUri) }.getOrDefault("")
      else ""
    val json = JSONObject().apply {
      put("brandName", brandName)
      put("currentUrl", currentUrl)
      put("localImagePath", localPath)
    }
    AdminOfflineStore.enqueueOp(
      appContext,
      AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_SAVE_APP_BRANDING, json.toString(), System.currentTimeMillis()),
      replacePredicate = { it.type == AdminPendingOp.TYPE_SAVE_APP_BRANDING },
    )
  }

  fun saveDeveloperConfig(dev: AdminDeveloperConfig, pickedLogoUri: Uri?) {
    val opt = dev.copy(logoUrl = pickedLogoUri?.toString() ?: dev.logoUrl)
    _optimisticDevConfig.value = opt
    _statusMessage.value = "Developer profile & buttons updated!"

    viewModelScope.launch {
      if (!AdminOfflineStore.isNetworkAvailable(appContext)) {
        enqueueDevConfigOp(dev, pickedLogoUri)
        _optimisticDevConfig.value = null
        _statusMessage.value = "অফলাইনে Dev Info সেভ হয়েছে।"
        return@launch
      }
      val resolvedUrl =
        if (pickedLogoUri != null) {
          repo.uploadBrandingImage(appContext, "dev_logo", pickedLogoUri).getOrElse {
            enqueueDevConfigOp(dev, pickedLogoUri)
            _optimisticDevConfig.value = null
            _statusMessage.value = "অফলাইনে Dev Info কিউতে সেভ হয়েছে।"
            return@launch
          }
        } else {
          dev.logoUrl
        }
      repo.saveDeveloperConfig(dev.copy(logoUrl = resolvedUrl))
        .onFailure { enqueueDevConfigOp(dev.copy(logoUrl = resolvedUrl), null) }
      _optimisticDevConfig.value = null
    }
  }

  private suspend fun enqueueDevConfigOp(dev: AdminDeveloperConfig, pickedLogoUri: Uri?) {
    val localPath =
      if (pickedLogoUri != null) runCatching { AdminOfflineStore.copyCompressedImage(appContext, "dev_logo", pickedLogoUri) }.getOrDefault("")
      else ""
    val btns = JSONArray()
    dev.buttons.forEach { b ->
      btns.put(JSONObject().apply {
        put("label", b.label)
        put("url", b.url)
        put("color", b.color)
      })
    }
    val json = JSONObject().apply {
      put("name", dev.name)
      put("logoUrl", dev.logoUrl)
      put("localImagePath", localPath)
      put("bio", dev.bio)
      put("buttons", btns)
    }
    AdminOfflineStore.enqueueOp(
      appContext,
      AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_SAVE_DEV_CONFIG, json.toString(), System.currentTimeMillis()),
      replacePredicate = { it.type == AdminPendingOp.TYPE_SAVE_DEV_CONFIG },
    )
  }

  fun saveKeyGateConfig(config: AdminKeyGateConfig) {
    _optimisticKeyGate.value = config
    _statusMessage.value = "Key Gate & Get Key URL saved!"
    viewModelScope.launch {
      AdminOfflineStore.saveKeyGateCache(appContext, config)
      if (!AdminOfflineStore.isNetworkAvailable(appContext)) {
        enqueueKeyGateOp(config)
        _optimisticKeyGate.value = null
        _statusMessage.value = "অফলাইনে Key Gate সেভ হয়েছে।"
        return@launch
      }
      repo.saveKeyGateConfig(config)
        .onFailure { enqueueKeyGateOp(config) }
      _optimisticKeyGate.value = null
    }
  }

  private suspend fun enqueueKeyGateOp(config: AdminKeyGateConfig) {
    val arr = JSONArray()
    config.socialLinks.forEach { s ->
      arr.put(JSONObject().apply {
        put("platform", s.platform)
        put("label", s.label)
        put("url", s.url)
        put("color", s.color)
      })
    }
    val json = JSONObject().apply {
      put("requireKeyAfterLogin", config.requireKeyAfterLogin)
      put("getKeyUrl", config.getKeyUrl)
      put("socialLinks", arr)
    }
    AdminOfflineStore.enqueueOp(
      appContext,
      AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_SAVE_KEY_GATE, json.toString(), System.currentTimeMillis()),
      replacePredicate = { it.type == AdminPendingOp.TYPE_SAVE_KEY_GATE },
    )
  }

  fun generateKeys(count: Int, durationHours: Int) {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    fun seg(): String = (1..5).map { chars.random() }.joinToString("")
    val safeHours = durationHours.coerceAtLeast(1)
    val now = System.currentTimeMillis()
    val expiresAt = now + safeHours * 3600_000L
    val createdKeys = List(count.coerceIn(1, 20)) {
      val code = "ZONEX-ZIXU-${seg()}-${seg()}"
      AdminAccessKey(
        code = code,
        durationHours = safeHours,
        createdAt = now,
        activatedAt = now,
        expiresAt = expiresAt,
        boundUid = "",
        boundDeviceId = "",
        isRevoked = false,
      )
    }

    _optimisticUpsertedKeys.update { current ->
      current + createdKeys.associateBy { it.code }
    }
    _statusMessage.value =
      if (createdKeys.size == 1) "Generated key: ${createdKeys.first().code}"
      else "Generated ${createdKeys.size} keys (ZONEX-ZIXU-...)"

    viewModelScope.launch {
      if (!AdminOfflineStore.isNetworkAvailable(appContext)) {
        createdKeys.forEach { enqueueUpsertKeyOp(it) }
        _optimisticUpsertedKeys.update { it - createdKeys.map { k -> k.code }.toSet() }
        _statusMessage.value = "অফলাইনে Key তৈরি হয়েছে, নেট এলে সিঙ্ক হবে।"
        return@launch
      }
      var anyFailed = false
      createdKeys.forEach { k ->
        repo.upsertAccessKey(k).onFailure {
          anyFailed = true
          enqueueUpsertKeyOp(k)
        }
      }
      _optimisticUpsertedKeys.update { it - createdKeys.map { k -> k.code }.toSet() }
      if (anyFailed) _statusMessage.value = "কিছু Key অফলাইন কিউতে আছে।"
    }
  }

  private suspend fun enqueueUpsertKeyOp(k: AdminAccessKey) {
    val json = JSONObject().apply {
      put("code", k.code)
      put("durationHours", k.durationHours)
      put("createdAt", k.createdAt)
      put("activatedAt", k.activatedAt)
      put("expiresAt", k.expiresAt)
      put("boundUid", k.boundUid)
      put("boundDeviceId", k.boundDeviceId)
      put("isRevoked", k.isRevoked)
    }
    AdminOfflineStore.enqueueOp(
      appContext,
      AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_UPSERT_KEY, json.toString(), k.createdAt),
    )
  }

  fun toggleRevokeKey(key: AdminAccessKey) {
    val newRevoked = !key.isRevoked
    _optimisticUpsertedKeys.update { it + (key.code to key.copy(isRevoked = newRevoked)) }
    _statusMessage.value = if (key.isRevoked) "Key restored" else "Key revoked"
    viewModelScope.launch {
      if (!AdminOfflineStore.isNetworkAvailable(appContext)) {
        enqueueRevokeKeyOp(key.code, newRevoked)
        _optimisticUpsertedKeys.update { it - key.code }
        return@launch
      }
      repo.toggleRevokeKey(key.code, newRevoked)
        .onFailure { enqueueRevokeKeyOp(key.code, newRevoked) }
      _optimisticUpsertedKeys.update { it - key.code }
    }
  }

  private suspend fun enqueueRevokeKeyOp(code: String, revoked: Boolean) {
    val json = JSONObject().apply {
      put("code", code)
      put("revoked", revoked)
    }
    AdminOfflineStore.enqueueOp(
      appContext,
      AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_TOGGLE_REVOKE_KEY, json.toString(), System.currentTimeMillis()),
    )
  }

  fun resetKeyBinding(key: AdminAccessKey) {
    val now = System.currentTimeMillis()
    val exp = now + key.durationHours.coerceAtLeast(1) * 3600_000L
    _optimisticUpsertedKeys.update {
      it + (key.code to key.copy(createdAt = now, activatedAt = now, expiresAt = exp, boundUid = "", boundDeviceId = "", isRevoked = false))
    }
    _statusMessage.value = "Key binding & ${key.durationHours}h timer reset"
    viewModelScope.launch {
      if (!AdminOfflineStore.isNetworkAvailable(appContext)) {
        enqueueResetKeyOp(key.code, now, exp)
        _optimisticUpsertedKeys.update { it - key.code }
        return@launch
      }
      repo.resetKeyBindingWithTimestamps(key.code, now, exp)
        .onFailure { enqueueResetKeyOp(key.code, now, exp) }
      _optimisticUpsertedKeys.update { it - key.code }
    }
  }

  private suspend fun enqueueResetKeyOp(code: String, now: Long, expiresAt: Long) {
    val json = JSONObject().apply {
      put("code", code)
      put("now", now)
      put("expiresAt", expiresAt)
    }
    AdminOfflineStore.enqueueOp(
      appContext,
      AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_RESET_KEY, json.toString(), now),
    )
  }

  fun deleteAccessKey(key: AdminAccessKey) {
    _optimisticUpsertedKeys.update { it - key.code }
    _optimisticDeletedKeyCodes.update { it + key.code }
    _statusMessage.value = "Deleted key ${key.code}"
    viewModelScope.launch {
      val wasUnsyncedCreate =
        AdminOfflineStore.pendingOps.value.any {
          it.type == AdminPendingOp.TYPE_UPSERT_KEY &&
            runCatching { JSONObject(it.payloadJson).optString("code") == key.code }.getOrDefault(false)
        }
      AdminOfflineStore.removePendingOpsMatching(appContext) {
        (it.type == AdminPendingOp.TYPE_UPSERT_KEY ||
          it.type == AdminPendingOp.TYPE_TOGGLE_REVOKE_KEY ||
          it.type == AdminPendingOp.TYPE_RESET_KEY) &&
          runCatching { JSONObject(it.payloadJson).optString("code") == key.code }.getOrDefault(false)
      }
      if (wasUnsyncedCreate) {
        _optimisticDeletedKeyCodes.update { it - key.code }
        return@launch
      }
      if (!AdminOfflineStore.isNetworkAvailable(appContext)) {
        enqueueDeleteKeyOp(key.code)
        _optimisticDeletedKeyCodes.update { it - key.code }
        return@launch
      }
      repo.deleteAccessKey(key.code)
        .onFailure { enqueueDeleteKeyOp(key.code) }
      _optimisticDeletedKeyCodes.update { it - key.code }
    }
  }

  private suspend fun enqueueDeleteKeyOp(code: String) {
    val json = JSONObject().apply { put("code", code) }
    AdminOfflineStore.enqueueOp(
      appContext,
      AdminPendingOp(UUID.randomUUID().toString(), AdminPendingOp.TYPE_DELETE_KEY, json.toString(), System.currentTimeMillis()),
    )
  }

  fun syncPendingOperations() {
    if (repo.currentUid() == null) return
    if (!AdminOfflineStore.isNetworkAvailable(appContext)) return
    val ops = AdminOfflineStore.pendingOps.value
    if (ops.isEmpty()) return

    viewModelScope.launch {
      syncMutex.withLock {
        val currentOps = AdminOfflineStore.pendingOps.value
        if (currentOps.isEmpty() || !AdminOfflineStore.isNetworkAvailable(appContext)) return@withLock
        _isSyncing.value = true
        var syncedCount = 0
        for (op in currentOps) {
          if (!AdminOfflineStore.isNetworkAvailable(appContext)) break
          val result = executePendingOp(op)
          if (result.isSuccess) {
            AdminOfflineStore.removePendingOp(appContext, op.opId)
            syncedCount++
          } else {
            // Stop syncing further ordered ops on connectivity failure
            if (!AdminOfflineStore.isNetworkAvailable(appContext)) break
          }
        }
        _isSyncing.value = false
        if (syncedCount > 0) {
          _statusMessage.value = "অফলাইন $syncedCount টি কাজ Firebase-এ সিঙ্ক হয়েছে!"
        }
      }
    }
  }

  private suspend fun executePendingOp(op: AdminPendingOp): Result<Unit> {
    val o = JSONObject(op.payloadJson)
    return when (op.type) {
      AdminPendingOp.TYPE_SAVE_SOUND -> {
        val soundId = o.getString("soundId")
        val isNew = o.optBoolean("isNewSound", true)
        val localAudio = o.optString("localAudioPath", "")
        val localThumb = o.optString("localThumbPath", "")
        val draft =
          AdminSoundDraft(
            editingId = soundId,
            title = o.optString("title", "Sound"),
            category = o.optString("category", "Trending"),
            emoji = o.optString("emoji", "🔥"),
            defaultVolume = o.optInt("defaultVolume", 100),
            useDirectUrl = o.optBoolean("useDirectUrl", false),
            directAudioUrl = o.optString("directAudioUrl", ""),
            existingAudioUrl = o.optString("existingAudioUrl", ""),
            existingThumbnailUrl = o.optString("existingThumbnailUrl", ""),
            audioUri = if (localAudio.isNotBlank() && File(localAudio).exists()) Uri.fromFile(File(localAudio)) else null,
            audioFileName = o.optString("audioFileName", "audio.mp3"),
            thumbnailUri = if (localThumb.isNotBlank() && File(localThumb).exists()) Uri.fromFile(File(localThumb)) else null,
          )
        repo.savePublicSound(appContext, draft, forcedId = soundId, isNewSound = isNew) {}
      }
      AdminPendingOp.TYPE_DELETE_SOUND -> repo.deletePublicSound(o.getString("soundId"))
      AdminPendingOp.TYPE_DELETE_USER_SOUND -> {
        val uid = o.getString("uid")
        val soundId = o.getString("soundId")
        if (o.optBoolean("isPublic", false)) repo.deletePublicSound(soundId)
        else repo.deleteUserPrivateSound(uid, soundId)
      }
      AdminPendingOp.TYPE_SAVE_CATEGORIES -> {
        val arr = o.getJSONArray("categories")
        repo.saveCategories(List(arr.length()) { i -> arr.getString(i) })
      }
      AdminPendingOp.TYPE_SAVE_NOTICE -> {
        repo.saveNotice(
          AdminNotice(
            bannerActive = o.optBoolean("bannerActive", false),
            bannerText = o.optString("bannerText", ""),
            popupActive = o.optBoolean("popupActive", false),
            popupTitle = o.optString("popupTitle", ""),
            popupMessage = o.optString("popupMessage", ""),
          )
        )
      }
      AdminPendingOp.TYPE_SAVE_MAINTENANCE -> {
        repo.saveMaintenanceConfig(
          AdminMaintenanceConfig(
            enabled = o.optBoolean("enabled", false),
            title = o.optString("title", "কাজ চলতেছে"),
            message = o.optString("message", ""),
          )
        )
      }
      AdminPendingOp.TYPE_SAVE_APP_UPDATE -> {
        repo.saveAppUpdateConfig(
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
        )
      }
      AdminPendingOp.TYPE_SAVE_TTS_VOICE -> {
        val localAvatar = o.optString("localAvatarPath", "")
        val uri = if (localAvatar.isNotBlank() && File(localAvatar).exists()) Uri.fromFile(File(localAvatar)) else null
        repo.saveTtsVoice(
          appContext,
          AdminTtsVoice(
            id = o.getString("id"),
            name = o.optString("name", ""),
            cartesiaVoiceId = o.optString("cartesiaVoiceId", ""),
            language = o.optString("language", "Multi"),
            avatarUrl = o.optString("avatarUrl", ""),
            badgeColorHex = o.optString("badgeColorHex", "#16A34A"),
            sortOrder = o.optInt("sortOrder", 0),
          ),
          uri,
        )
      }
      AdminPendingOp.TYPE_DELETE_TTS_VOICE -> repo.deleteTtsVoice(o.getString("id"))
      AdminPendingOp.TYPE_SAVE_APP_BRANDING -> {
        val localImg = o.optString("localImagePath", "")
        val url =
          if (localImg.isNotBlank() && File(localImg).exists()) {
            repo.uploadBrandingImage(appContext, "app_logo", Uri.fromFile(File(localImg))).getOrElse {
              return Result.failure(it)
            }
          } else {
            o.optString("currentUrl", "")
          }
        repo.saveAppConfig(AdminAppConfig(logoUrl = url, floatingBrandName = o.optString("brandName", "ZONEX")))
      }
      AdminPendingOp.TYPE_SAVE_DEV_CONFIG -> {
        val localImg = o.optString("localImagePath", "")
        val url =
          if (localImg.isNotBlank() && File(localImg).exists()) {
            repo.uploadBrandingImage(appContext, "dev_logo", Uri.fromFile(File(localImg))).getOrElse {
              return Result.failure(it)
            }
          } else {
            o.optString("logoUrl", "")
          }
        val arr = o.optJSONArray("buttons") ?: JSONArray()
        val btns = List(arr.length()) { idx ->
          val b = arr.getJSONObject(idx)
          AdminDevButton(b.optString("label", ""), b.optString("url", ""), b.optString("color", "#F5B82E"))
        }
        repo.saveDeveloperConfig(
          AdminDeveloperConfig(
            name = o.optString("name", "Zonex Official"),
            logoUrl = url,
            bio = o.optString("bio", ""),
            buttons = btns,
          )
        )
      }
      AdminPendingOp.TYPE_SAVE_KEY_GATE -> {
        val arr = o.optJSONArray("socialLinks") ?: JSONArray()
        val links = List(arr.length()) { i ->
          val s = arr.getJSONObject(i)
          AdminKeySocialLink(
            platform = s.optString("platform", "Telegram"),
            label = s.optString("label", ""),
            url = s.optString("url", ""),
            color = s.optString("color", "#0EA5E9"),
          )
        }
        repo.saveKeyGateConfig(
          AdminKeyGateConfig(
            requireKeyAfterLogin = o.optBoolean("requireKeyAfterLogin", true),
            getKeyUrl = o.optString("getKeyUrl", ""),
            socialLinks = links,
          )
        )
      }
      AdminPendingOp.TYPE_UPSERT_KEY -> {
        repo.upsertAccessKey(
          AdminAccessKey(
            code = o.getString("code"),
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
      AdminPendingOp.TYPE_TOGGLE_REVOKE_KEY -> {
        repo.toggleRevokeKey(o.getString("code"), o.getBoolean("revoked"))
      }
      AdminPendingOp.TYPE_RESET_KEY -> {
        repo.resetKeyBindingWithTimestamps(o.getString("code"), o.getLong("now"), o.getLong("expiresAt"))
      }
      AdminPendingOp.TYPE_DELETE_KEY -> repo.deleteAccessKey(o.getString("code"))
      else -> Result.success(Unit)
    }
  }

  fun clearMessage() {
    _statusMessage.value = null
  }

  override fun onCleared() {
    stopPreview()
    networkCallback?.let { cb ->
      val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      runCatching { cm?.unregisterNetworkCallback(cb) }
    }
    super.onCleared()
  }
}
