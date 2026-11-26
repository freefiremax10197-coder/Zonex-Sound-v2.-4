package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.audio.CartesiaTtsClient
import com.example.audio.CartesiaVoiceOption
import com.example.audio.ZonexAudioEngine
import com.example.data.firebase.KeyLiveCheck
import com.example.data.firebase.KeyValidationResult
import com.example.data.firebase.ZonexFirebaseRepository
import com.example.data.local.OfflineSoundStore
import com.example.data.model.AccessKeyState
import com.example.data.model.AdminNotice
import com.example.data.model.AppConfig
import com.example.data.model.AppThemeStyle
import com.example.data.model.AppUpdateConfig
import com.example.data.model.AudioDeviceDisplay
import com.example.data.model.CompatibilityCheckItem
import com.example.data.model.DeveloperConfig
import com.example.data.model.GameMixingStatus
import com.example.data.model.KeyGateConfig
import com.example.data.model.MainTab
import com.example.data.model.MaintenanceConfig
import com.example.data.model.PlaybackState
import com.example.data.model.SoundItem
import com.example.data.model.SoundVisibility
import com.example.data.model.UploadSoundDraft
import com.example.data.model.UserProfile
import com.example.data.model.UserSettingsState
import com.example.data.preferences.ZonexPreferences
import com.example.security.ZonexSecurityGuard
import com.example.service.FloatingOverlayBus
import com.example.service.FloatingSoundboardService
import com.example.service.ZonexNotificationHelper
import com.example.shizuku.ShizukuRootManager
import com.example.shizuku.ShizukuRootState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthFormState(
  val isSignUpTab: Boolean = false,
  val fullName: String = "",
  val email: String = "",
  val password: String = "",
  val confirmPassword: String = "",
  val keepLoggedIn: Boolean = true,
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
)

data class OnboardingSequenceState(
  val isActive: Boolean = false,
  val showWelcomeOnTop: Boolean = false,
)

data class UploadModalState(
  val isOpen: Boolean = false,
  val isUploading: Boolean = false,
  val progress: Float = 0f,
  val errorMessage: String? = null,
)

data class AudioLabState(
  val activeOutputSummary: String = "Checking audio output...",
  val compatibilityChecks: List<CompatibilityCheckItem> = emptyList(),
  val detectedDevices: List<AudioDeviceDisplay> = emptyList(),
  val gameMixingStatus: GameMixingStatus? = null,
  val hasOverlayPermission: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ZonexViewModel(application: Application) : AndroidViewModel(application) {

  private val appContext = application.applicationContext
  private val repo = ZonexFirebaseRepository()
  private val prefs = ZonexPreferences(appContext)

  private val _isAuthChecking = MutableStateFlow(true)
  val isAuthChecking: StateFlow<Boolean> = _isAuthChecking.asStateFlow()

  val currentUser: StateFlow<UserProfile?> =
    repo.authStateFlow.stateIn(viewModelScope, SharingStarted.Eagerly, repo.currentUserProfile())

  val settings: StateFlow<UserSettingsState> =
    prefs.settingsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, UserSettingsState())

  private val _authForm = MutableStateFlow(AuthFormState())
  val authForm: StateFlow<AuthFormState> = _authForm.asStateFlow()

  private val _onboardingState = MutableStateFlow(OnboardingSequenceState())
  val onboardingState: StateFlow<OnboardingSequenceState> = _onboardingState.asStateFlow()

  private val _selectedTab = MutableStateFlow(MainTab.SOUNDS)
  val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedCategory = MutableStateFlow("All")
  val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

  private val _isGameModeOpen = MutableStateFlow(false)
  val isGameModeOpen: StateFlow<Boolean> = _isGameModeOpen.asStateFlow()

  private val _isGameModeMicOn = MutableStateFlow(false)
  val isGameModeMicOn: StateFlow<Boolean> = _isGameModeMicOn.asStateFlow()

  private val _isInAppFloatingOpen = MutableStateFlow(false)
  val isInAppFloatingOpen: StateFlow<Boolean> = _isInAppFloatingOpen.asStateFlow()

  private val _isInAppFloatingMinimized = MutableStateFlow(false)
  val isInAppFloatingMinimized: StateFlow<Boolean> = _isInAppFloatingMinimized.asStateFlow()

  private val _showOverlayPermissionDialog = MutableStateFlow(false)
  val showOverlayPermissionDialog: StateFlow<Boolean> = _showOverlayPermissionDialog.asStateFlow()

  private val _isNoticePopupDismissed = MutableStateFlow(false)
  val isNoticePopupDismissed: StateFlow<Boolean> = _isNoticePopupDismissed.asStateFlow()

  private val _uploadState = MutableStateFlow(UploadModalState())
  val uploadState: StateFlow<UploadModalState> = _uploadState.asStateFlow()

  private val _likedSoundIds = MutableStateFlow<Set<String>>(emptySet())
  val likedSoundIds: StateFlow<Set<String>> = _likedSoundIds.asStateFlow()

  private val _statusBannerMessage = MutableStateFlow<String?>(null)
  val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

  private val _audioLabState = MutableStateFlow(AudioLabState())
  val audioLabState: StateFlow<AudioLabState> = _audioLabState.asStateFlow()

  private val _showShizukuDialog = MutableStateFlow(false)
  val showShizukuDialog: StateFlow<Boolean> = _showShizukuDialog.asStateFlow()

  val shizukuState: StateFlow<ShizukuRootState> = ShizukuRootManager.state

  private val _ttsInputText = MutableStateFlow("")
  val ttsInputText: StateFlow<String> = _ttsInputText.asStateFlow()

  private val _ttsPitch = MutableStateFlow(1.0f)
  val ttsPitch: StateFlow<Float> = _ttsPitch.asStateFlow()

  private val _ttsSpeechRate = MutableStateFlow(1.0f)
  val ttsSpeechRate: StateFlow<Float> = _ttsSpeechRate.asStateFlow()

  private val _accountCartesiaVoices = MutableStateFlow<List<CartesiaVoiceOption>>(emptyList())
  private val _manualRemoteTtsVoices = MutableStateFlow<List<CartesiaVoiceOption>?>(null)

  private val remoteTtsVoices: StateFlow<List<CartesiaVoiceOption>> =
    repo.observeRemoteTtsVoices().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val _isRefreshingVoices = MutableStateFlow(false)
  val isRefreshingVoices: StateFlow<Boolean> = _isRefreshingVoices.asStateFlow()

  private val _isSynthesizingTts = MutableStateFlow(false)
  val isSynthesizingTts: StateFlow<Boolean> = _isSynthesizingTts.asStateFlow()

  private val _isSavingTtsCard = MutableStateFlow(false)
  val isSavingTtsCard: StateFlow<Boolean> = _isSavingTtsCard.asStateFlow()

  private val _ttsError = MutableStateFlow<String?>(null)
  val ttsError: StateFlow<String?> = _ttsError.asStateFlow()

  private val _lastSynthesizedTtsFile = MutableStateFlow<File?>(null)
  val lastSynthesizedTtsFile: StateFlow<File?> = _lastSynthesizedTtsFile.asStateFlow()

  private var lastSynthesizedTtsTitle: String = "Voice Note"

  private val _showCartesiaKeyDialog = MutableStateFlow(false)
  val showCartesiaKeyDialog: StateFlow<Boolean> = _showCartesiaKeyDialog.asStateFlow()

  private val _showVoiceChooserDialog = MutableStateFlow(false)
  val showVoiceChooserDialog: StateFlow<Boolean> = _showVoiceChooserDialog.asStateFlow()

  private val _showCustomVoiceDialog = MutableStateFlow(false)
  val showCustomVoiceDialog: StateFlow<Boolean> = _showCustomVoiceDialog.asStateFlow()

  val playbackState: StateFlow<PlaybackState> = ZonexAudioEngine.playbackState
  val isTestingSpeaker: StateFlow<Boolean> = ZonexAudioEngine.isTestingSpeaker
  val isTestingMic: StateFlow<Boolean> = ZonexAudioEngine.isTestingMic
  val micLevelNormalized: StateFlow<Float> = ZonexAudioEngine.micLevelNormalized
  val micDbLevel: StateFlow<Float> = ZonexAudioEngine.micDbLevel

  private val _accessKeyState = MutableStateFlow(AccessKeyState(isChecking = false))
  val accessKeyState: StateFlow<AccessKeyState> = _accessKeyState.asStateFlow()

  private val remotePublicSounds: StateFlow<List<SoundItem>?> =
    combine(
      currentUser.map { it?.uid }.distinctUntilChanged(),
      accessKeyState.map { it.isActivated }.distinctUntilChanged(),
    ) { uid, isActivated ->
      uid to isActivated
    }
      .flatMapLatest { (uid, _) ->
        if (uid == null) flowOf(null)
        else repo.observePublicSounds().map<List<SoundItem>, List<SoundItem>?> { it }
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val publicSounds: StateFlow<List<SoundItem>> =
    combine(
      remotePublicSounds,
      OfflineSoundStore.cachedPublicSounds,
      OfflineSoundStore.pendingUploads,
    ) { remote, cached, pending ->
      val base =
        when {
          remote == null -> cached
          remote.isEmpty() && cached.isNotEmpty() && !OfflineSoundStore.isNetworkAvailable(appContext) -> cached
          else -> remote
        }
      val pendingPublic =
        pending.filter { it.visibility == "public" }.map { it.toSoundItem() }
      val merged = LinkedHashMap<String, SoundItem>()
      pendingPublic.forEach { merged[it.id] = it }
      base.forEach { merged.putIfAbsent(it.id, it) }
      merged.values.sortedByDescending { it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val adminNotice: StateFlow<AdminNotice> =
    repo.observeNotice().stateIn(viewModelScope, SharingStarted.Eagerly, AdminNotice())

  private val remoteDeveloperConfig: StateFlow<DeveloperConfig?> =
    repo.observeDeveloper()
      .map<DeveloperConfig, DeveloperConfig?> { it }
      .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val developerConfig: StateFlow<DeveloperConfig> =
    combine(remoteDeveloperConfig, OfflineSoundStore.cachedDeveloperConfig) { remote, cached ->
      remote ?: cached
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DeveloperConfig())

  private val remoteAppConfig: StateFlow<AppConfig?> =
    repo.observeAppConfig()
      .map<AppConfig, AppConfig?> { it }
      .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val appConfig: StateFlow<AppConfig> =
    combine(remoteAppConfig, OfflineSoundStore.cachedAppConfig) { remote, cached ->
      remote ?: cached
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppConfig())

  private val remoteAppUpdateConfig: StateFlow<AppUpdateConfig?> =
    repo.observeAppUpdateConfig()
      .map<AppUpdateConfig, AppUpdateConfig?> { it }
      .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val appUpdateConfig: StateFlow<AppUpdateConfig> =
    combine(remoteAppUpdateConfig, OfflineSoundStore.cachedAppUpdateConfig) { remote, cached ->
      remote ?: cached
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUpdateConfig())

  val maintenanceConfig: StateFlow<MaintenanceConfig> =
    repo.observeMaintenanceConfig().stateIn(viewModelScope, SharingStarted.Eagerly, MaintenanceConfig())

  private val _isUpdatePopupDismissed = MutableStateFlow(false)

  val shouldShowUpdatePopup: StateFlow<Boolean> =
    combine(appUpdateConfig, _isUpdatePopupDismissed) { cfg, dismissed ->
      if (!cfg.enabled) return@combine false
      val needsUpdate =
        ZonexSecurityGuard.shouldShowAppUpdate(
          installedVersionName = BuildConfig.VERSION_NAME,
          installedVersionCode = BuildConfig.VERSION_CODE,
          latestVersionName = cfg.latestVersionName,
          latestVersionCode = cfg.latestVersionCode,
          targetVersionName = cfg.targetVersionName,
        )
      needsUpdate && (cfg.forceUpdate || !dismissed)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

  val cartesiaVoices: StateFlow<List<CartesiaVoiceOption>> =
    combine(
      remoteTtsVoices,
      _manualRemoteTtsVoices,
      _accountCartesiaVoices,
      settings,
    ) { liveRemote, manualRemote, accountVoices, s ->
      val effectiveRemote = manualRemote ?: liveRemote
      val customVoices = CartesiaTtsClient.decodeCustomVoices(s.customTtsVoicesJson)
      val defaultsMap = CartesiaTtsClient.defaultVoices.associateBy { it.id }

      // Merge remote updates into matching presets (so Admin can attach avatars or override voice IDs)
      val mergedDefaults =
        CartesiaTtsClient.defaultVoices.map { preset ->
          val override = effectiveRemote.firstOrNull { it.id == preset.id || it.name.equals(preset.name, ignoreCase = true) }
          if (override != null) {
            preset.copy(
              cartesiaVoiceId = override.cartesiaVoiceId.ifBlank { preset.cartesiaVoiceId },
              avatarUrl = override.avatarUrl.ifBlank { preset.avatarUrl },
              language = override.language.ifBlank { preset.language },
              badgeColorHex = override.badgeColorHex.ifBlank { preset.badgeColorHex },
            )
          } else {
            preset
          }
        }
      val extraRemote =
        effectiveRemote.filter { rem ->
          !defaultsMap.containsKey(rem.id) &&
            CartesiaTtsClient.defaultVoices.none { it.name.equals(rem.name, ignoreCase = true) }
        }
      val combined =
        (customVoices + extraRemote + mergedDefaults + accountVoices)
          .distinctBy { it.id }

      val order = s.ttsVoiceOrderIds
      if (order.isEmpty()) {
        combined
      } else {
        val rank = order.withIndex().associate { (idx, id) -> id to idx }
        combined.sortedWith(
          compareBy<CartesiaVoiceOption> { rank[it.id] ?: Int.MAX_VALUE }
        )
      }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CartesiaTtsClient.defaultVoices)

  private val remoteKeyGateConfig: StateFlow<KeyGateConfig?> =
    repo.observeKeyGateConfig()
      .map<KeyGateConfig, KeyGateConfig?> { it }
      .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val keyGateConfig: StateFlow<KeyGateConfig> =
    combine(remoteKeyGateConfig, OfflineSoundStore.cachedKeyGateConfig) { remote, cached ->
      remote ?: cached
    }.stateIn(viewModelScope, SharingStarted.Eagerly, KeyGateConfig())

  private val remotePrivateSounds: StateFlow<List<SoundItem>?> =
    currentUser
      .map { it?.uid }
      .distinctUntilChanged()
      .flatMapLatest { uid ->
        if (uid == null) flowOf(null)
        else repo.observePrivateSounds(uid).map<List<SoundItem>, List<SoundItem>?> { it }
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val privateSounds: StateFlow<List<SoundItem>> =
    combine(
      currentUser,
      remotePrivateSounds,
      OfflineSoundStore.cachedPrivateSounds,
      OfflineSoundStore.pendingUploads,
    ) { user, remote, cached, pending ->
      val uid = user?.uid.orEmpty()
      val base =
        when {
          remote == null -> cached
          remote.isEmpty() && cached.isNotEmpty() && !OfflineSoundStore.isNetworkAvailable(appContext) -> cached
          else -> remote
        }
      val pendingPriv =
        pending.filter { it.uid == uid && it.visibility == "private" }.map { it.toSoundItem() }
      val merged = LinkedHashMap<String, SoundItem>()
      pendingPriv.forEach { merged[it.id] = it }
      base.forEach { merged.putIfAbsent(it.id, it) }
      merged.values.sortedByDescending { it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val rawDownloads: StateFlow<Map<String, SoundItem?>> =
    currentUser
      .flatMapLatest { user ->
        if (user == null) flowOf(emptyMap()) else repo.observeDownloads(user.uid)
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

  private val remoteFavoriteIds: StateFlow<Set<String>?> =
    currentUser
      .flatMapLatest { user ->
        if (user == null) flowOf(null) else repo.observeFavorites(user.uid).map<Set<String>, Set<String>?> { it }
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val favoriteIds: StateFlow<Set<String>> = OfflineSoundStore.offlineFavoriteIds

  private val remoteCategories: StateFlow<List<String>> =
    currentUser
      .map { it?.uid }
      .distinctUntilChanged()
      .flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repo.observeCategories()
      }
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList(),
      )

  val managedCategories: StateFlow<List<String>> =
    combine(remoteCategories, OfflineSoundStore.cachedCategories) { remote, cached ->
      if (remote.isNotEmpty()) remote else cached
    }.stateIn(
      viewModelScope,
      SharingStarted.Eagerly,
      listOf("Funny", "Reaction", "Meme"),
    )

  val categories: StateFlow<List<String>> =
    managedCategories
      .map { managed ->
        (listOf("All", "Trending") + managed)
          .distinctBy { it.lowercase() }
      }
      .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        listOf("All", "Trending", "Funny", "Reaction", "Meme"),
      )

  val filteredPublicSounds: StateFlow<List<SoundItem>> =
    combine(publicSounds, searchQuery, selectedCategory) { sounds, query, category ->
      val q = query.trim().lowercase()
      val queryMatches =
        sounds.filter { item ->
          q.isEmpty() ||
            item.title.lowercase().contains(q) ||
            item.category.lowercase().contains(q)
        }
      when {
        category.equals("All", ignoreCase = true) -> {
          queryMatches.sortedByDescending { it.createdAt }
        }
        category.equals("Trending", ignoreCase = true) -> {
          val loved = queryMatches.filter { it.likes > 0L }
          if (loved.isNotEmpty()) {
            loved.sortedWith(
              compareByDescending<SoundItem> { it.likes }
                .thenByDescending { it.createdAt }
            )
          } else {
            queryMatches
              .filter { it.category.equals("Trending", ignoreCase = true) }
              .sortedByDescending { it.createdAt }
          }
        }
        else -> {
          queryMatches
            .filter { it.category.equals(category, ignoreCase = true) }
            .sortedByDescending { it.createdAt }
        }
      }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val downloadedSounds: StateFlow<List<SoundItem>> =
    combine(OfflineSoundStore.offlineDownloads, rawDownloads, publicSounds) {
      localList,
      downloadsMap,
      publicList ->
      val publicById = publicList.associateBy { it.id }
      val merged = LinkedHashMap<String, SoundItem>()
      localList.forEach { merged[it.id] = it.copy(isDownloaded = true) }
      downloadsMap.entries.forEach { (id, saved) ->
        val resolved =
          if (saved != null && saved.audioUrl.isNotBlank()) saved.copy(isDownloaded = true)
          else publicById[id]?.copy(isDownloaded = true)
        if (resolved != null && !merged.containsKey(id)) {
          merged[id] = resolved
        }
      }
      merged.values.sortedByDescending { it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val userPublicUploads: StateFlow<List<SoundItem>> =
    combine(publicSounds, currentUser) { sounds, user ->
      if (user == null) emptyList() else sounds.filter { it.createdBy == user.uid }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val favoriteSounds: StateFlow<List<SoundItem>> =
    combine(publicSounds, privateSounds, downloadedSounds, favoriteIds) {
      pub,
      priv,
      dl,
      favIds ->
      val merged = LinkedHashMap<String, SoundItem>()
      // Prefer downloadedSounds first so offline playback metadata is guaranteed present
      dl.forEach { merged[it.id] = it }
      priv.forEach { merged.putIfAbsent(it.id, it) }
      pub.forEach { merged.putIfAbsent(it.id, it) }
      favIds.mapNotNull { merged[it] }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private var connectivityCallback: ConnectivityManager.NetworkCallback? = null
  @Volatile
  private var isSyncingPendingUploads = false

  init {
    OfflineSoundStore.ensureLoaded(appContext)
    viewModelScope.launch {
      val keep = prefs.getKeepLoggedIn()
      _authForm.update { it.copy(keepLoggedIn = keep) }
      if (!keep && repo.currentUserProfile() != null) {
        repo.signOut()
      }
      _isAuthChecking.value = false
    }

    // Persist remote catalogs locally whenever online snapshots arrive
    viewModelScope.launch {
      remotePublicSounds.collect { list ->
        if (list != null && (list.isNotEmpty() || OfflineSoundStore.isNetworkAvailable(appContext))) {
          OfflineSoundStore.saveCachedPublicSounds(appContext, list)
        }
      }
    }
    viewModelScope.launch {
      remoteCategories.collect { cats ->
        if (cats.isNotEmpty()) {
          OfflineSoundStore.saveCachedCategories(appContext, cats)
        }
      }
    }
    viewModelScope.launch {
      combine(currentUser, remotePrivateSounds) { user, priv -> user to priv }
        .collect { (user, priv) ->
          if (user != null && priv != null && (priv.isNotEmpty() || OfflineSoundStore.isNetworkAvailable(appContext))) {
            OfflineSoundStore.saveCachedPrivateSounds(appContext, user.uid, priv)
          }
        }
    }

    viewModelScope.launch {
      remoteFavoriteIds.collect { remote ->
        if (remote != null) {
          OfflineSoundStore.replaceRemoteFavorites(appContext, remote)
        }
      }
    }
    viewModelScope.launch {
      remoteKeyGateConfig.collect { remote ->
        if (remote != null) OfflineSoundStore.saveCachedKeyGateConfig(appContext, remote)
      }
    }
    viewModelScope.launch {
      remoteDeveloperConfig.collect { remote ->
        if (remote != null) OfflineSoundStore.saveCachedDeveloperConfig(appContext, remote)
      }
    }
    viewModelScope.launch {
      remoteAppConfig.collect { remote ->
        if (remote != null) OfflineSoundStore.saveCachedAppConfig(appContext, remote)
      }
    }
    viewModelScope.launch {
      remoteAppUpdateConfig.collect { remote ->
        if (remote != null) OfflineSoundStore.saveCachedAppUpdateConfig(appContext, remote)
      }
    }

    ShizukuRootManager.init(appContext)
    viewModelScope.launch {
      if (prefs.getMicInjectionEnabled()) {
        ShizukuRootManager.restoreSavedMicTransferIfReady(appContext)
      }
    }

    // Cold-start Key Gate: prompt Key screen on every launch with valid saved Key pre-filled by default
    viewModelScope.launch {
      currentUser.collect { user ->
        if (user == null) {
          OfflineSoundStore.loadCachedPrivateForUser(appContext, "")
          _accessKeyState.value = AccessKeyState(isChecking = false, isActivated = false)
        } else {
          OfflineSoundStore.loadCachedPrivateForUser(appContext, user.uid)
          prepareKeyGateForUser(user.uid)
          syncPendingUploadsIfOnline()
        }
      }
    }

    // Real-time kickout if Admin deletes, revokes, or expires the active key
    viewModelScope.launch {
      combine(
        currentUser,
        accessKeyState.map { (if (it.isActivated) it.keyCode else "") to it.isActivated },
      ) { user, (activeCode, isAct) ->
        Triple(user?.uid.orEmpty(), activeCode, isAct)
      }
        .flatMapLatest { (uid, code, isAct) ->
          if (uid.isBlank() || code.isBlank() || !isAct) {
            flowOf<KeyLiveCheck?>(null)
          } else {
            repo.observeAccessKeyLive(code, uid, getDeviceFingerprint())
          }
        }
        .collect { check ->
          when (check) {
            is KeyLiveCheck.Deleted ->
              kickoutToKeyScreen("এই Key-টি অ্যাডমিন ডিলিট করেছেন। নতুন Key দিয়ে লগইন করুন।")
            is KeyLiveCheck.Revoked ->
              kickoutToKeyScreen("এই Key-টি অ্যাডমিন বাতিল (Revoked) করেছেন।")
            is KeyLiveCheck.WrongBinding ->
              kickoutToKeyScreen("এই Key-টি অন্য ডিভাইস বা অ্যাকাউন্টে পরিবর্তন করা হয়েছে।")
            is KeyLiveCheck.Expired ->
              kickoutToKeyScreen("আপনার ২৪ ঘণ্টার Key মেয়াদ শেষ হয়েছে। নতুন Key নিন।")
            is KeyLiveCheck.Valid -> {
              val now = System.currentTimeMillis()
              _accessKeyState.update {
                if (!it.isActivated) it
                else it.copy(
                  expiresAt = check.expiresAt,
                  remainingText = formatRemaining(check.expiresAt - now),
                  expiryDateText = formatExpiryDate(check.expiresAt),
                )
              }
            }
            null -> Unit
          }
        }
    }

    registerNetworkSyncCallback()

    viewModelScope.launch {
      while (true) {
        kotlinx.coroutines.delay(30_000L)
        val cur = _accessKeyState.value
        if (cur.isActivated && cur.expiresAt > 0L) {
          val now = System.currentTimeMillis()
          if (now > cur.expiresAt) {
            _accessKeyState.update {
              it.copy(
                isActivated = false,
                remainingText = "",
                errorMessage = "আপনার ২৪ ঘণ্টার Key মেয়াদ শেষ হয়েছে। নতুন Key নিন।",
              )
            }
          } else {
            _accessKeyState.update {
              it.copy(
                remainingText = formatRemaining(cur.expiresAt - now),
                expiryDateText = formatExpiryDate(cur.expiresAt),
              )
            }
          }
        }
      }
    }

    viewModelScope.launch {
      combine(settings, shizukuState) { s, shizuku -> s to shizuku }.collect { (s, shizuku) ->
        ZonexAudioEngine.updateMixerVolumes(
          master = s.masterVolume,
          sfx = s.sfxChannelVolume,
          voiceBoost = s.freeFireVoiceBoost,
          micTransfer = s.micInjectionEnabled || shizuku.isInjectionActive,
        )
        ZonexNotificationHelper.showOrUpdateQuickControl(
          context = appContext,
          enabled = s.quickNotificationControl && currentUser.value != null,
          currentTitle = playbackState.value.currentSound?.title,
        )
      }
    }

    viewModelScope.launch {
      combine(playbackState, settings, currentUser) { pb, s, user ->
        Triple(pb, s, user)
      }.collect { (pb, s, user) ->
        ZonexNotificationHelper.showOrUpdateQuickControl(
          context = appContext,
          enabled = s.quickNotificationControl && user != null,
          currentTitle = if (pb.isPlaying) pb.currentSound?.title else null,
        )
      }
    }

    viewModelScope.launch {
      favoriteSounds.collect { favs ->
        FloatingOverlayBus.updateFavorites(favs)
        if (FloatingOverlayBus.isSystemOverlayRunning.value && Settings.canDrawOverlays(appContext)) {
          val intent = Intent(appContext, FloatingSoundboardService::class.java)
          runCatching { appContext.startService(intent) }
        }
      }
    }

    viewModelScope.launch {
      combine(appConfig, developerConfig) { app, dev ->
        app.logoUrl.ifBlank { dev.logoUrl } to app.floatingBrandName
      }.collect { (logoUrl, brand) ->
        FloatingOverlayBus.updateBranding(logoUrl, brand)
        if (FloatingOverlayBus.isSystemOverlayRunning.value && Settings.canDrawOverlays(appContext)) {
          val intent = Intent(appContext, FloatingSoundboardService::class.java)
          runCatching { appContext.startService(intent) }
        }
      }
    }

    viewModelScope.launch {
      FloatingOverlayBus.userClosedEvents.collect {
        _isInAppFloatingOpen.value = false
        prefs.setFloatingBubbleEnabled(false)
      }
    }

    refreshHardwareDiagnostics()
  }

  // --- Auth Actions ---

  fun selectAuthTab(isSignUp: Boolean) {
    _authForm.update { it.copy(isSignUpTab = isSignUp, errorMessage = null) }
  }

  fun updateFullName(value: String) = _authForm.update { it.copy(fullName = value, errorMessage = null) }
  fun updateEmail(value: String) = _authForm.update { it.copy(email = value, errorMessage = null) }
  fun updatePassword(value: String) = _authForm.update { it.copy(password = value, errorMessage = null) }
  fun updateConfirmPassword(value: String) =
    _authForm.update { it.copy(confirmPassword = value, errorMessage = null) }

  fun updateKeepLoggedIn(keep: Boolean) {
    _authForm.update { it.copy(keepLoggedIn = keep) }
    viewModelScope.launch { prefs.setKeepLoggedIn(keep) }
  }

  fun submitLogin() {
    val form = _authForm.value
    if (form.email.isBlank() || form.password.isBlank()) {
      _authForm.update { it.copy(errorMessage = "Please enter both email and password.") }
      return
    }
    viewModelScope.launch {
      _authForm.update { it.copy(isLoading = true, errorMessage = null) }
      prefs.setKeepLoggedIn(form.keepLoggedIn)
      val res = repo.signIn(form.email, form.password)
      res.onSuccess { user ->
        _authForm.update { it.copy(isLoading = false, password = "", confirmPassword = "") }
        checkAndTriggerStrictOneTimeOnboarding(user.uid)
      }.onFailure { err ->
        _authForm.update {
          it.copy(
            isLoading = false,
            errorMessage = err.localizedMessage ?: "Login failed. Verify your credentials.",
          )
        }
      }
    }
  }

  fun submitSignUp() {
    val form = _authForm.value
    if (form.fullName.isBlank()) {
      _authForm.update { it.copy(errorMessage = "Please enter your full name.") }
      return
    }
    if (form.email.isBlank() || !form.email.contains("@")) {
      _authForm.update { it.copy(errorMessage = "Please enter a valid email address.") }
      return
    }
    if (form.password.length < 6) {
      _authForm.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
      return
    }
    if (form.password != form.confirmPassword) {
      _authForm.update { it.copy(errorMessage = "Passwords do not match.") }
      return
    }
    viewModelScope.launch {
      _authForm.update { it.copy(isLoading = true, errorMessage = null) }
      prefs.setKeepLoggedIn(form.keepLoggedIn)
      val res = repo.signUp(form.fullName, form.email, form.password)
      res.onSuccess { user ->
        _authForm.update { it.copy(isLoading = false, password = "", confirmPassword = "") }
        checkAndTriggerStrictOneTimeOnboarding(user.uid)
      }.onFailure { err ->
        _authForm.update {
          it.copy(
            isLoading = false,
            errorMessage = err.localizedMessage ?: "Registration failed. Try again.",
          )
        }
      }
    }
  }

  private suspend fun checkAndTriggerStrictOneTimeOnboarding(uid: String) {
    val localSeen = prefs.getLocalOnboardingSeen(uid)
    if (localSeen) return
    val remoteSeen = repo.hasSeenOnboardingRemote(uid)
    if (remoteSeen) {
      prefs.setLocalOnboardingSeen(uid, true)
      return
    }
    // Show layered strict one-time post-login popups:
    // Welcome/Creator popup on top, Feature Overview directly behind it.
    _onboardingState.value = OnboardingSequenceState(isActive = true, showWelcomeOnTop = true)
    // Mark seen immediately in RTDB + DataStore so normal app opens never repeat it
    prefs.setLocalOnboardingSeen(uid, true)
    repo.markOnboardingSeen(uid)
  }

  fun dismissWelcomePopupLayer() {
    _onboardingState.update { it.copy(showWelcomeOnTop = false) }
  }

  fun finishOnboarding(goToAudioLab: Boolean) {
    _onboardingState.value = OnboardingSequenceState(isActive = false, showWelcomeOnTop = false)
    if (goToAudioLab) {
      _selectedTab.value = MainTab.AUDIO_LAB
      refreshHardwareDiagnostics()
    } else {
      _selectedTab.value = MainTab.SOUNDS
    }
  }

  fun logout() {
    ZonexAudioEngine.stopAll()
    ZonexAudioEngine.stopMicTest()
    closeFloatingWindow()
    _isGameModeOpen.value = false
    ZonexNotificationHelper.showOrUpdateQuickControl(appContext, false, null)
    repo.signOut()
  }

  // --- Navigation & UI Actions ---

  fun selectTab(tab: MainTab) {
    _selectedTab.value = tab
    if (tab == MainTab.AUDIO_LAB || tab == MainTab.SOUNDS) {
      refreshHardwareDiagnostics()
    }
  }

  fun updateSearchQuery(q: String) {
    _searchQuery.value = q
  }

  fun selectCategory(cat: String) {
    _selectedCategory.value = cat
  }

  // --- Playback Actions ---

  fun playSound(sound: SoundItem) {
    ZonexAudioEngine.toggleOrPlaySound(
      context = appContext,
      sound = sound,
      triggerHaptic = settings.value.hapticFeedback,
    )
  }

  fun togglePlayPause() {
    ZonexAudioEngine.togglePlayPause()
  }

  fun stopAllSounds() {
    if (settings.value.hapticFeedback) {
      ZonexAudioEngine.performHapticTap(appContext)
    }
    ZonexAudioEngine.stopAll()
  }

  fun clearPlaybackError() = ZonexAudioEngine.clearError()

  // --- Sound Library Mutations ---

  fun toggleFavorite(sound: SoundItem) {
    val uid = currentUser.value?.uid ?: return
    val isCurrentlyFav = favoriteIds.value.contains(sound.id)
    if (!isCurrentlyFav) {
      val hasOfflineDownload =
        downloadedSounds.value.any { it.id == sound.id } ||
          OfflineSoundStore.hasLocalAudio(appContext, sound.id)
      if (!hasOfflineDownload) {
        showMessage(
          if (settings.value.isBanglaUi)
            "ফ্লোটিং উইন্ডোতে (★) এড করতে হলে আগে ডাউনলোড (⬇) করুন — তাহলে অফলাইনেও কাজ করবে!"
          else "Download (⬇) this sound first to pin it to Floating Window (works offline too)!"
        )
        return
      }
    }
    if (settings.value.hapticFeedback) {
      ZonexAudioEngine.performHapticTap(appContext)
    }
    val nextFav = !isCurrentlyFav
    viewModelScope.launch {
      if (nextFav && !OfflineSoundStore.hasLocalAudio(appContext, sound.id)) {
        OfflineSoundStore.cacheSoundFromSource(appContext, sound)
      }
      OfflineSoundStore.setFavoriteLocal(appContext, sound.id, nextFav)
      runCatching { repo.toggleFavorite(uid, sound.id, nextFav) }
    }
  }

  fun toggleLike(sound: SoundItem) {
    if (sound.visibility == "private") return
    _likedSoundIds.update { it + sound.id }
    if (settings.value.hapticFeedback) {
      ZonexAudioEngine.performHapticTap(appContext)
    }
    viewModelScope.launch {
      runCatching { repo.incrementPublicLike(sound.id, 1L) }
    }
  }

  fun saveToMySounds(sound: SoundItem) {
    val uid = currentUser.value?.uid ?: return
    if (OfflineSoundStore.hasLocalAudio(appContext, sound.id) &&
      downloadedSounds.value.any { it.id == sound.id }
    ) {
      showMessage(
        if (settings.value.isBanglaUi)
          "\"${sound.title}\" আগেই অফলাইনে ডাউনলোড করা আছে! ★ দিয়ে ফ্লোটিং উইন্ডোতে পিন করুন।"
        else "\"${sound.title}\" is already saved offline! Tap ★ to pin it to Floating Window."
      )
      return
    }
    viewModelScope.launch {
      showMessage(
        if (settings.value.isBanglaUi) "অফলাইনের জন্য \"${sound.title}\" ডাউনলোড হচ্ছে..."
        else "Downloading \"${sound.title}\" for offline use..."
      )
      val cacheResult = OfflineSoundStore.cacheSoundFromSource(appContext, sound)
      cacheResult
        .onSuccess { cached ->
          runCatching { repo.saveToDownloads(uid, cached) }
          showMessage(
            if (settings.value.isBanglaUi)
              "\"${sound.title}\" অফলাইন ও My Sounds-এ ডাউনলোড হয়েছে! এখন ★ দিয়ে ফ্লোটিং উইন্ডোতে নিতে পারবেন।"
            else "Downloaded \"${sound.title}\" for offline & Floating Window! Tap ★ to pin."
          )
        }
        .onFailure { err ->
          showMessage("Could not download offline: ${err.localizedMessage}")
        }
    }
  }

  fun removeFromDownloads(soundId: String) {
    val uid = currentUser.value?.uid ?: return
    viewModelScope.launch {
      OfflineSoundStore.removeSound(appContext, soundId)
      runCatching { repo.removeFromDownloads(uid, soundId) }
      runCatching { repo.toggleFavorite(uid, soundId, false) }
      showMessage(
        if (settings.value.isBanglaUi) "অফলাইন সেভ থেকে সরানো হয়েছে"
        else "Removed from offline saved sounds"
      )
    }
  }

  fun deleteUploadedSound(sound: SoundItem) {
    val uid = currentUser.value?.uid ?: return
    viewModelScope.launch {
      if (sound.id.startsWith("offline_")) {
        OfflineSoundStore.removePendingUpload(appContext, sound.id)
        showMessage("Deleted offline pending sound \"${sound.title}\"")
        return@launch
      }
      OfflineSoundStore.removeSound(appContext, sound.id)
      runCatching { repo.deleteUploadedSound(uid, sound) }
        .onSuccess { showMessage("Deleted \"${sound.title}\"") }
        .onFailure { showMessage("Delete failed: ${it.localizedMessage}") }
    }
  }

  // --- Upload Modal ---

  fun openUploadModal() {
    _uploadState.value = UploadModalState(isOpen = true)
  }

  fun closeUploadModal() {
    if (!_uploadState.value.isUploading) {
      _uploadState.value = UploadModalState(isOpen = false)
    }
  }

  fun submitSoundUpload(draft: UploadSoundDraft) {
    val uid = currentUser.value?.uid ?: return
    if (draft.title.isBlank()) {
      _uploadState.update { it.copy(errorMessage = "Please enter a sound name.") }
      return
    }
    if (draft.useDirectUrl && draft.directAudioUrl.isBlank()) {
      _uploadState.update { it.copy(errorMessage = "Please enter a valid audio URL.") }
      return
    }
    if (!draft.useDirectUrl && draft.audioUri == null) {
      _uploadState.update { it.copy(errorMessage = "Please select an audio file or switch to Direct URL.") }
      return
    }

    viewModelScope.launch {
      _uploadState.update { it.copy(isUploading = true, progress = 0.05f, errorMessage = null) }
      if (!OfflineSoundStore.isNetworkAvailable(appContext)) {
        OfflineSoundStore.enqueuePendingUpload(appContext, uid, draft)
          .onSuccess {
            _uploadState.value = UploadModalState(isOpen = false)
            showMessage(
              if (settings.value.isBanglaUi)
                "\"${it.title}\" অফলাইনে সেভ হয়েছে! ইন্টারনেট এলে অটো আপলোড হবে।"
              else "Saved \"${it.title}\" offline — will auto-sync when online!"
            )
          }
          .onFailure { err ->
            _uploadState.update {
              it.copy(isUploading = false, errorMessage = err.localizedMessage ?: "Offline save failed")
            }
          }
        return@launch
      }

      val result =
        repo.uploadSound(appContext, uid, draft) { pct ->
          _uploadState.update { it.copy(progress = pct) }
        }
      result
        .onSuccess { created ->
          OfflineSoundStore.cacheSoundFromSource(appContext, created)
          if (draft.addToFavorites) {
            OfflineSoundStore.setFavoriteLocal(appContext, created.id, true)
          }
          _uploadState.value = UploadModalState(isOpen = false)
          showMessage("Uploaded \"${created.title}\" (${created.visibility.uppercase()}) & cached offline!")
        }
        .onFailure { err ->
          if (!OfflineSoundStore.isNetworkAvailable(appContext)) {
            OfflineSoundStore.enqueuePendingUpload(appContext, uid, draft)
              .onSuccess {
                _uploadState.value = UploadModalState(isOpen = false)
                showMessage(
                  if (settings.value.isBanglaUi)
                    "নেটওয়ার্ক না থাকায় \"${it.title}\" অফলাইনে সেভ হয়েছে! পরে অটো আপলোড হবে।"
                  else "Saved \"${it.title}\" offline — will auto-sync when online!"
                )
              }
              .onFailure { offlineErr ->
                _uploadState.update {
                  it.copy(isUploading = false, errorMessage = offlineErr.localizedMessage ?: "Upload failed")
                }
              }
          } else {
            _uploadState.update {
              it.copy(
                isUploading = false,
                errorMessage = err.localizedMessage ?: "Upload failed. Check Storage rules & network.",
              )
            }
          }
        }
    }
  }

  // --- Game Mode HUD ---

  fun openGameMode() {
    _isGameModeOpen.value = true
  }

  fun closeGameMode() {
    _isGameModeOpen.value = false
  }

  fun toggleGameModeMic() {
    _isGameModeMicOn.update { !it }
  }

  // --- Floating Window & Overlay Permission ---

  private fun hasPrivilegeForFloatingWindow(): Boolean {
    val st = shizukuState.value
    return st.hasShizukuPermission || st.isRootAvailable
  }

  fun launchFloatingWindow() {
    if (FloatingOverlayBus.isSystemOverlayRunning.value || _isInAppFloatingOpen.value) {
      closeFloatingWindow()
      return
    }
    ShizukuRootManager.refreshShizukuStatus()
    if (!hasPrivilegeForFloatingWindow()) {
      viewModelScope.launch { prefs.setFloatingBubbleEnabled(false) }
      showMessage(
        if (settings.value.isBanglaUi)
          "ফ্লোটিং উইন্ডো চালু করতে আগে Shizuku বা Root পারমিশন কানেক্ট করুন!"
        else "Connect Shizuku or Root permission first to launch Floating Window!"
      )
      openShizukuDialog()
      return
    }
    _isInAppFloatingMinimized.value = true
    if (Settings.canDrawOverlays(appContext)) {
      _isInAppFloatingOpen.value = false
      _showOverlayPermissionDialog.value = false
      startSystemOverlayService()
    } else {
      _isInAppFloatingOpen.value = false
      _showOverlayPermissionDialog.value = true
    }
  }

  fun openInAppFallbackFloating() {
    if (!hasPrivilegeForFloatingWindow()) {
      _showOverlayPermissionDialog.value = false
      openShizukuDialog()
      return
    }
    _showOverlayPermissionDialog.value = false
    if (!FloatingOverlayBus.isSystemOverlayRunning.value) {
      _isInAppFloatingMinimized.value = true
      _isInAppFloatingOpen.value = true
      viewModelScope.launch { prefs.setFloatingBubbleEnabled(true) }
    }
  }

  fun onOverlayPermissionReturned() {
    if (Settings.canDrawOverlays(appContext) && _showOverlayPermissionDialog.value) {
      _showOverlayPermissionDialog.value = false
      if (!hasPrivilegeForFloatingWindow()) {
        openShizukuDialog()
        return
      }
      _isInAppFloatingOpen.value = false
      startSystemOverlayService()
    } else if (isInAppFloatingOpen.value && Settings.canDrawOverlays(appContext)) {
      _isInAppFloatingOpen.value = false
      if (hasPrivilegeForFloatingWindow()) {
        startSystemOverlayService()
      }
    } else if (FloatingOverlayBus.isSystemOverlayRunning.value) {
      _isInAppFloatingOpen.value = false
    }
  }

  fun dismissOverlayPermissionDialog() {
    _showOverlayPermissionDialog.value = false
    if (!FloatingOverlayBus.isSystemOverlayRunning.value && !_isInAppFloatingOpen.value) {
      viewModelScope.launch { prefs.setFloatingBubbleEnabled(false) }
    }
  }

  fun startSystemOverlayService() {
    if (!Settings.canDrawOverlays(appContext)) return
    if (!hasPrivilegeForFloatingWindow()) {
      viewModelScope.launch { prefs.setFloatingBubbleEnabled(false) }
      openShizukuDialog()
      return
    }
    _isInAppFloatingOpen.value = false
    viewModelScope.launch { prefs.setFloatingBubbleEnabled(true) }
    val intent = Intent(appContext, FloatingSoundboardService::class.java)
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        appContext.startForegroundService(intent)
      } else {
        appContext.startService(intent)
      }
    }
  }

  fun toggleMinimizeFloatingWindow() {
    _isInAppFloatingMinimized.update { !it }
  }

  fun closeFloatingWindow() {
    _isInAppFloatingOpen.value = false
    FloatingOverlayBus.setRunning(false)
    runCatching { appContext.stopService(Intent(appContext, FloatingSoundboardService::class.java)) }
    viewModelScope.launch { prefs.setFloatingBubbleEnabled(false) }
  }

  // --- Notice Controls ---

  fun dismissAdminBanner() {
    val text = adminNotice.value.bannerText
    viewModelScope.launch { prefs.setDismissedBannerText(text) }
  }

  fun dismissNoticePopup() {
    _isNoticePopupDismissed.value = true
  }

  // --- Mixer & Settings ---

  fun setThemeStyle(style: AppThemeStyle) {
    viewModelScope.launch { prefs.setThemeStyle(style) }
  }

  fun setMasterVolume(value: Float) {
    viewModelScope.launch { prefs.setMasterVolume(value) }
  }

  fun setSfxChannelVolume(value: Float) {
    viewModelScope.launch { prefs.setSfxChannelVolume(value) }
  }

  fun setMediaChannelVolume(value: Float) {
    ZonexAudioEngine.setMediaChannelVolume(appContext, value)
    viewModelScope.launch { prefs.setMediaChannelVolume(value) }
  }

  fun setFreeFireVoiceBoost(enabled: Boolean) {
    viewModelScope.launch { prefs.setFreeFireVoiceBoost(enabled) }
  }

  fun setFloatingBubbleEnabled(enabled: Boolean) {
    if (enabled) {
      launchFloatingWindow()
    } else {
      closeFloatingWindow()
    }
  }

  fun setQuickNotificationControl(enabled: Boolean) {
    viewModelScope.launch { prefs.setQuickNotificationControl(enabled) }
  }

  fun setHapticFeedback(enabled: Boolean) {
    viewModelScope.launch { prefs.setHapticFeedback(enabled) }
  }

  fun setSoundAnimations(enabled: Boolean) {
    viewModelScope.launch { prefs.setSoundAnimations(enabled) }
  }

  fun restoreDefaultSoundLibrary() {
    viewModelScope.launch {
      prefs.restoreDefaults()
      _searchQuery.value = ""
      _selectedCategory.value = "All"
      _isNoticePopupDismissed.value = false
      showMessage("Restored default audio settings & refreshed live library")
    }
  }

  // --- Audio Lab Real Diagnostics ---

  fun refreshHardwareDiagnostics() {
    ShizukuRootManager.refreshShizukuStatus()
    _audioLabState.value =
      AudioLabState(
        activeOutputSummary = ZonexAudioEngine.getActiveOutputSummary(appContext),
        compatibilityChecks = ZonexAudioEngine.inspectCompatibilityChecks(appContext),
        detectedDevices = ZonexAudioEngine.inspectAudioDevices(appContext),
        gameMixingStatus = ZonexAudioEngine.inspectGameMixingStatus(appContext),
        hasOverlayPermission = Settings.canDrawOverlays(appContext),
      )
  }

  fun playSpeakerTest() {
    val firstHomeSound = filteredPublicSounds.value.firstOrNull() ?: publicSounds.value.firstOrNull()
    if (firstHomeSound != null) {
      ZonexAudioEngine.toggleOrPlaySound(
        context = appContext,
        sound = firstHomeSound,
        triggerHaptic = settings.value.hapticFeedback,
      )
      showMessage(
        if (settings.value.isBanglaUi) "হোম পেজের প্রথম সাউন্ড বাজছে: ${firstHomeSound.title}"
        else "Playing 1st Home sound: ${firstHomeSound.title}"
      )
    } else {
      ZonexAudioEngine.playSpeakerTestTone()
    }
  }

  fun startMicTest() {
    ZonexAudioEngine.startMicTest(appContext)
    refreshHardwareDiagnostics()
  }

  fun stopMicTest() {
    ZonexAudioEngine.stopMicTest()
  }

  // --- Shizuku / Root Mic Transfer ---

  fun openShizukuDialog() {
    ShizukuRootManager.refreshShizukuStatus()
    _showShizukuDialog.value = true
  }

  fun closeShizukuDialog() {
    _showShizukuDialog.value = false
  }

  fun checkRootAccess() {
    viewModelScope.launch { ShizukuRootManager.checkRootAccess() }
  }

  fun checkOrRequestShizuku() {
    ShizukuRootManager.requestShizukuPermission()
  }

  fun enableShizukuInjection() {
    viewModelScope.launch {
      val detail = ShizukuRootManager.enableMicTransfer(appContext)
      prefs.setMicInjectionEnabled(true)
      refreshHardwareDiagnostics()
      showMessage(detail)
    }
  }

  fun disableShizukuInjection() {
    viewModelScope.launch {
      ShizukuRootManager.disableMicTransfer(appContext)
      prefs.setMicInjectionEnabled(false)
      refreshHardwareDiagnostics()
      showMessage("Mic Transfer stopped")
    }
  }

  fun runShizukuDiagnosticReport() {
    viewModelScope.launch { ShizukuRootManager.generateDiagnosticReport(appContext) }
  }

  // --- Volume Max & Language Toggle ---

  fun maxSystemVolume() {
    ZonexAudioEngine.maxMediaVolume(appContext)
    viewModelScope.launch {
      prefs.setMasterVolume(1.0f)
      prefs.setSfxChannelVolume(1.0f)
      prefs.setMediaChannelVolume(1.0f)
    }
    showMessage(if (settings.value.isBanglaUi) "ভলিউম ১০০% ম্যাক্স করা হয়েছে!" else "Volume set to 100% MAX!")
  }

  fun toggleLanguageBangla() {
    val next = !settings.value.isBanglaUi
    viewModelScope.launch { prefs.setIsBanglaUi(next) }
  }

  // --- Cartesia & Device Text-to-Voice ---

  fun updateTtsInputText(text: String) {
    _ttsInputText.value = text
    _ttsError.value = null
  }

  fun updateTtsPitch(pitch: Float) {
    _ttsPitch.value = pitch.coerceIn(0.5f, 1.7f)
  }

  fun updateTtsSpeechRate(rate: Float) {
    _ttsSpeechRate.value = rate.coerceIn(0.6f, 1.6f)
  }

  fun selectCartesiaVoice(voiceId: String) {
    val preset = cartesiaVoices.value.firstOrNull { it.id == voiceId }
    if (preset != null && preset.isLocalPreset) {
      _ttsPitch.value = preset.pitch
      _ttsSpeechRate.value = preset.speechRate
    }
    viewModelScope.launch { prefs.setSelectedCartesiaVoiceId(voiceId) }
  }

  fun openVoiceChooserDialog() {
    _showVoiceChooserDialog.value = true
  }

  fun closeVoiceChooserDialog() {
    _showVoiceChooserDialog.value = false
  }

  fun openCustomVoiceDialog() {
    _showCustomVoiceDialog.value = true
  }

  fun closeCustomVoiceDialog() {
    _showCustomVoiceDialog.value = false
  }

  fun openCartesiaKeyDialog() {
    _showCartesiaKeyDialog.value = true
  }

  fun closeCartesiaKeyDialog() {
    _showCartesiaKeyDialog.value = false
  }

  fun addCartesiaApiKey(rawKey: String) {
    val trimmed = rawKey.trim()
    if (trimmed.isBlank()) return
    viewModelScope.launch {
      val current = settings.value.cartesiaApiKeys.toMutableList()
      if (!current.contains(trimmed)) {
        current.add(trimmed)
        val newIndex = current.lastIndex
        prefs.setCartesiaApiKeys(current, newIndex)
        CartesiaTtsClient.fetchAccountVoices(trimmed).onSuccess { fetched ->
          if (fetched.isNotEmpty()) {
            _accountCartesiaVoices.value = fetched
          }
        }
        showMessage(
          if (settings.value.isBanglaUi) "Cartesia Key যোগ করা হয়েছে! (#${current.size})"
          else "Added Cartesia API Key (#${current.size})"
        )
      } else {
        val idx = current.indexOf(trimmed).coerceAtLeast(0)
        prefs.setActiveCartesiaKeyIndex(idx)
        showMessage(if (settings.value.isBanglaUi) "এই Key-টি সক্রিয় করা হয়েছে" else "Activated existing Cartesia Key")
      }
    }
  }

  fun removeCartesiaApiKey(index: Int) {
    viewModelScope.launch {
      val current = settings.value.cartesiaApiKeys.toMutableList()
      if (index !in current.indices) return@launch
      current.removeAt(index)
      val nextIdx =
        if (current.isEmpty()) 0
        else settings.value.activeCartesiaKeyIndex.coerceIn(0, current.lastIndex)
      prefs.setCartesiaApiKeys(current, nextIdx)
      showMessage(if (settings.value.isBanglaUi) "Key সরানো হয়েছে" else "Removed Cartesia Key")
    }
  }

  fun setActiveCartesiaKey(index: Int) {
    viewModelScope.launch {
      val current = settings.value.cartesiaApiKeys
      if (index !in current.indices) return@launch
      prefs.setActiveCartesiaKeyIndex(index)
      showMessage(
        if (settings.value.isBanglaUi) "Key #${index + 1} অ্যাক্টিভ করা হয়েছে"
        else "Switched to Cartesia Key #${index + 1}"
      )
    }
  }

  fun addCustomTtsVoice(name: String, cartesiaVoiceId: String, avatarUrl: String = "") {
    val cleanId = cartesiaVoiceId.trim()
    val cleanName = name.trim().ifBlank { "Custom Voice" }
    if (cleanId.isBlank()) return
    viewModelScope.launch {
      val currentCustom = CartesiaTtsClient.decodeCustomVoices(settings.value.customTtsVoicesJson).toMutableList()
      val uniqueId = "custom_${cleanId.take(12)}_${System.currentTimeMillis() % 10000}"
      val newVoice =
        CartesiaVoiceOption(
          id = uniqueId,
          name = cleanName,
          cartesiaVoiceId = cleanId,
          language = "Custom ID",
          avatarUrl = avatarUrl.trim(),
          badgeColorHex = "#7C3AED",
          isCustom = true,
        )
      currentCustom.add(0, newVoice)
      prefs.setCustomTtsVoicesJson(CartesiaTtsClient.encodeCustomVoices(currentCustom))
      prefs.setSelectedCartesiaVoiceId(newVoice.id)
      _showCustomVoiceDialog.value = false
      showMessage(
        if (settings.value.isBanglaUi) "\"$cleanName\" ভয়েস তালিকায় যোগ হয়েছে!"
        else "Added custom voice \"$cleanName\""
      )
    }
  }

  fun deleteCustomTtsVoice(voiceId: String) {
    viewModelScope.launch {
      val updated = CartesiaTtsClient.decodeCustomVoices(settings.value.customTtsVoicesJson).filterNot { it.id == voiceId }
      prefs.setCustomTtsVoicesJson(CartesiaTtsClient.encodeCustomVoices(updated))
      if (settings.value.selectedCartesiaVoiceId == voiceId) {
        prefs.setSelectedCartesiaVoiceId(CartesiaTtsClient.defaultVoices.first().id)
      }
    }
  }

  fun moveVoiceOrder(voiceId: String, moveUp: Boolean) {
    viewModelScope.launch {
      val currentIds = cartesiaVoices.value.map { it.id }.toMutableList()
      val idx = currentIds.indexOf(voiceId)
      if (idx == -1) return@launch
      val targetIdx = if (moveUp) idx - 1 else idx + 1
      if (targetIdx !in currentIds.indices) return@launch
      val tmp = currentIds[idx]
      currentIds[idx] = currentIds[targetIdx]
      currentIds[targetIdx] = tmp
      prefs.setTtsVoiceOrderIds(currentIds)
    }
  }

  fun refreshCartesiaVoices() {
    viewModelScope.launch {
      _isRefreshingVoices.value = true
      val remoteList = repo.fetchRemoteTtsVoicesOnce()
      if (remoteList.isNotEmpty()) {
        _manualRemoteTtsVoices.value = remoteList
      }
      val keys = settings.value.cartesiaApiKeys
      val activeKey = keys.getOrNull(settings.value.activeCartesiaKeyIndex) ?: keys.firstOrNull().orEmpty()
      var syncedAccount = 0
      if (activeKey.isNotBlank()) {
        CartesiaTtsClient.fetchAccountVoices(activeKey)
          .onSuccess { fetched ->
            if (fetched.isNotEmpty()) {
              _accountCartesiaVoices.value = fetched
              syncedAccount = fetched.size
            }
          }
      }
      _isRefreshingVoices.value = false
      showMessage(
        if (settings.value.isBanglaUi)
          "ভয়েস তালিকা রিফ্রেশ হয়েছে (${cartesiaVoices.value.size} টি ভয়েস${if (syncedAccount > 0) " • +$syncedAccount অ্যাকাউন্ট" else ""})"
        else
          "Voice list refreshed (${cartesiaVoices.value.size} voices)"
      )
    }
  }

  fun synthesizeAndPlayTts() {
    val text = _ttsInputText.value.trim()
    if (text.isBlank()) {
      _ttsError.value =
        if (settings.value.isBanglaUi) "দয়া করে কিছু টেক্সট লিখুন।" else "Please enter text to synthesize."
      return
    }
    viewModelScope.launch {
      _isSynthesizingTts.value = true
      _ttsError.value = null
      val currentSettings = settings.value
      val selectedVoice =
        cartesiaVoices.value.firstOrNull { it.id == currentSettings.selectedCartesiaVoiceId }
          ?: CartesiaVoiceOption(
            id = currentSettings.selectedCartesiaVoiceId,
            name = "Voice",
            cartesiaVoiceId = currentSettings.selectedCartesiaVoiceId,
          )
      val keys = currentSettings.cartesiaApiKeys
      val startIdx = currentSettings.activeCartesiaKeyIndex
      val (result, usedIdx) =
        CartesiaTtsClient.synthesizeWithKeyRotation(
          context = appContext,
          apiKeys = keys,
          activeKeyIndex = startIdx,
          voice = selectedVoice,
          text = text,
          pitchOverride = _ttsPitch.value,
          rateOverride = _ttsSpeechRate.value,
        )
      if (keys.isNotEmpty() && usedIdx != startIdx && usedIdx in keys.indices) {
        prefs.setActiveCartesiaKeyIndex(usedIdx)
      }
      _isSynthesizingTts.value = false
      result
        .onSuccess { wavFile ->
          _lastSynthesizedTtsFile.value = wavFile
          lastSynthesizedTtsTitle = text.take(24)
          val tempSound =
            SoundItem(
              id = "tts_${System.currentTimeMillis()}",
              title = text.take(28),
              category = "Voice",
              audioUrl = wavFile.absolutePath,
              emoji = "🗣️",
              defaultVolume = 100,
              visibility = "private",
            )
          ZonexAudioEngine.playSound(
            context = appContext,
            sound = tempSound,
            triggerHaptic = settings.value.hapticFeedback,
          )
        }
        .onFailure { err ->
          _ttsError.value = err.localizedMessage ?: "TTS failed"
        }
    }
  }

  fun saveLastTtsAsSoundCard() {
    val file = _lastSynthesizedTtsFile.value ?: return
    val uid = currentUser.value?.uid ?: return
    viewModelScope.launch {
      _isSavingTtsCard.value = true
      val draft =
        UploadSoundDraft(
          title = lastSynthesizedTtsTitle.ifBlank { "TTS Voice" },
          category = "Voice",
          emoji = "🗣️",
          audioUri = Uri.fromFile(file),
          audioFileName = file.name,
          useDirectUrl = false,
          defaultVolume = 100,
          addToFavorites = true,
          visibility = SoundVisibility.PRIVATE,
        )
      if (!OfflineSoundStore.isNetworkAvailable(appContext)) {
        OfflineSoundStore.enqueuePendingUpload(appContext, uid, draft)
          .onSuccess { showMessage("Saved \"${it.title}\" offline — will sync online when connected!") }
          .onFailure { err -> _ttsError.value = err.localizedMessage ?: "Offline save failed" }
        _isSavingTtsCard.value = false
        return@launch
      }
      val res = repo.uploadSound(appContext, uid, draft) {}
      _isSavingTtsCard.value = false
      res
        .onSuccess { created ->
          OfflineSoundStore.cacheLocalFileForSound(appContext, created, file)
          OfflineSoundStore.setFavoriteLocal(appContext, created.id, true)
          showMessage("Saved \"${created.title}\" offline to My Sounds & Floating Window!")
        }
        .onFailure { err ->
          _ttsError.value = err.localizedMessage ?: "Failed to save voice card"
        }
    }
  }

  private fun registerNetworkSyncCallback() {
    val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
    val cb =
      object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          syncPendingUploadsIfOnline()
        }
      }
    connectivityCallback = cb
    val req = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
    runCatching { cm.registerNetworkCallback(req, cb) }
  }

  private fun syncPendingUploadsIfOnline() {
    val uid = currentUser.value?.uid ?: return
    if (!OfflineSoundStore.isNetworkAvailable(appContext)) return
    if (isSyncingPendingUploads) return
    val pendingForUser = OfflineSoundStore.pendingUploads.value.filter { it.uid == uid }
    if (pendingForUser.isEmpty()) return

    viewModelScope.launch {
      if (isSyncingPendingUploads) return@launch
      isSyncingPendingUploads = true
      try {
        var syncedCount = 0
        for (pending in pendingForUser) {
          if (!OfflineSoundStore.isNetworkAvailable(appContext)) break
          val draft = pending.toDraft()
          val res = repo.uploadSound(appContext, uid, draft) {}
          res.onSuccess { created ->
            OfflineSoundStore.completePendingUpload(appContext, pending, created)
            syncedCount++
          }
        }
        if (syncedCount > 0) {
          showMessage(
            if (settings.value.isBanglaUi)
              "অফলাইনে সেভ করা $syncedCount টি সাউন্ড অনলাইনে সিঙ্ক হয়েছে!"
            else "Synced $syncedCount offline sound(s) to cloud!"
          )
        }
      } finally {
        isSyncingPendingUploads = false
      }
    }
  }

  override fun onCleared() {
    val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    connectivityCallback?.let { cb -> runCatching { cm?.unregisterNetworkCallback(cb) } }
    connectivityCallback = null
    super.onCleared()
  }

  // --- Access Key Gate (ZONEX-ZIXU-XXXXX-XXXXX • 24h • 1 User) ---

  @SuppressLint("HardwareIds")
  private fun getDeviceFingerprint(): String =
    runCatching {
      Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
    }.getOrNull().orEmpty().ifBlank { "${Build.BRAND}_${Build.MODEL}" }

  private suspend fun kickoutToKeyScreen(reason: String) {
    ZonexAudioEngine.stopAll()
    ZonexAudioEngine.stopMicTest()
    closeFloatingWindow()
    _isGameModeOpen.value = false
    prefs.setCachedAccessKey("", 0L)
    _accessKeyState.value =
      AccessKeyState(
        isChecking = false,
        isActivated = false,
        keyCode = "",
        expiresAt = 0L,
        errorMessage = reason,
      )
  }

  private suspend fun prepareKeyGateForUser(uid: String) {
    val (cachedCode, cachedExp) = prefs.getCachedAccessKey()
    val now = System.currentTimeMillis()
    val isOfflineValid = cachedCode.isNotBlank() && (cachedExp == 0L || cachedExp > now)

    if (cachedExp in 1L..now) {
      prefs.setCachedAccessKey("", 0L)
    }

    // Show KeyActivationScreen IMMEDIATELY with saved valid Key pre-filled (no blocking spinner!)
    _accessKeyState.value =
      AccessKeyState(
        isChecking = false,
        isActivated = false,
        keyCode = if (isOfflineValid) cachedCode else "",
        expiresAt = if (isOfflineValid) cachedExp else 0L,
      )

    // Quietly validate or recover the key in the background without delaying the UI
    if (OfflineSoundStore.isNetworkAvailable(appContext)) {
      viewModelScope.launch {
        val result =
          withTimeoutOrNull(3_000L) {
            repo.validateExistingUserKeyDetailed(uid, getDeviceFingerprint(), if (isOfflineValid) cachedCode else "")
          } ?: KeyValidationResult.NetworkError

        val latestState = _accessKeyState.value
        if (latestState.isActivated || latestState.isActivating) return@launch

        when (result) {
          is KeyValidationResult.Valid -> {
            prefs.setCachedAccessKey(result.code, result.expiresAt)
            _accessKeyState.update { cur ->
              if (cur.isActivated || cur.isActivating) cur
              else cur.copy(isChecking = false, keyCode = result.code, expiresAt = result.expiresAt)
            }
          }
          is KeyValidationResult.Invalid -> {
            prefs.setCachedAccessKey("", 0L)
            _accessKeyState.update { cur ->
              if (cur.isActivated || cur.isActivating) cur
              else cur.copy(isChecking = false, keyCode = "", expiresAt = 0L)
            }
          }
          is KeyValidationResult.NetworkError -> Unit
        }
      }
    }
  }

  fun activateAccessKey(rawCode: String) {
    val uid = currentUser.value?.uid ?: return
    val cleanCode = rawCode.trim().uppercase()
    viewModelScope.launch {
      _accessKeyState.update { it.copy(isActivating = true, errorMessage = null) }

      val isOnline = OfflineSoundStore.isNetworkAvailable(appContext)
      if (!isOnline) {
        val (cachedCode, cachedExp) = prefs.getCachedAccessKey()
        if (cachedCode.equals(cleanCode, ignoreCase = true) && cachedExp > System.currentTimeMillis()) {
          _accessKeyState.value =
            AccessKeyState(
              isChecking = false,
              isActivated = true,
              keyCode = cachedCode,
              expiresAt = cachedExp,
              remainingText = formatRemaining(cachedExp - System.currentTimeMillis()),
              expiryDateText = formatExpiryDate(cachedExp),
              isActivating = false,
              errorMessage = null,
            )
          showMessage(
            if (settings.value.isBanglaUi) "অফলাইন মোড: আপনার ডাউনলোড করা সাউন্ড ব্যবহার করুন!"
            else "Offline Mode: cached key unlocked!"
          )
          return@launch
        }
      }

      val result = repo.verifyAndActivateAccessKey(uid, getDeviceFingerprint(), cleanCode)
      result
        .onSuccess { (code, exp) ->
          prefs.setCachedAccessKey(code, exp)
          _accessKeyState.value =
            AccessKeyState(
              isChecking = false,
              isActivated = true,
              keyCode = code,
              expiresAt = exp,
              remainingText = formatRemaining(exp - System.currentTimeMillis()),
              expiryDateText = formatExpiryDate(exp),
              isActivating = false,
              errorMessage = null,
            )
          val expStr = formatExpiryDate(exp)
          showMessage(
            if (settings.value.isBanglaUi) "Key অ্যাক্টিভ হয়েছে! শেষ ডেট $expStr"
            else "Key Activated! Expires $expStr"
          )
        }
        .onFailure { err ->
          val msg = err.localizedMessage ?: "Invalid or expired key"
          if (msg.contains("পাওয়া যায়নি") || msg.contains("বাতিল") || msg.contains("মেয়াদ শেষ")) {
            prefs.setCachedAccessKey("", 0L)
          }
          _accessKeyState.update {
            it.copy(
              isActivating = false,
              isActivated = false,
              errorMessage = msg,
            )
          }
        }
    }
  }

  fun openGetKeyWebsite() {
    val rawUrl = keyGateConfig.value.getKeyUrl.trim()
    if (rawUrl.isBlank()) {
      val msg =
        if (settings.value.isBanglaUi)
          "অ্যাডমিন এখনো Get Key ওয়েবসাইট লিংক সেট করেননি। Sound Admin → Keys থেকে লিংক দিন।"
        else "Get Key website URL not set yet. Admin can configure it in Sound Admin → Keys."
      _accessKeyState.update { it.copy(errorMessage = msg) }
      showMessage(msg)
      return
    }
    val safeUrl = com.example.security.ZonexSecurityGuard.sanitizeExternalUrl(rawUrl)
    if (safeUrl == null) {
      showMessage("Invalid Get Key website URL.")
      return
    }
    runCatching {
      appContext.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
    }.onFailure {
      showMessage("Could not open browser: ${it.localizedMessage}")
    }
  }

  private fun formatRemaining(ms: Long): String {
    if (ms <= 0L) return "Expired"
    val totalMins = ms / 60_000L
    val hours = totalMins / 60L
    val mins = totalMins % 60L
    return if (settings.value.isBanglaUi) "${hours} ঘণ্টা ${mins} মিনিট" else "${hours}h ${mins}m"
  }

  private fun formatExpiryDate(expiresAtMs: Long): String {
    if (expiresAtMs <= 0L) return ""
    val formatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(expiresAtMs))
    return if (settings.value.isBanglaUi) toBanglaDigits(formatted) else formatted
  }

  private fun toBanglaDigits(raw: String): String {
    val bn = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return buildString(raw.length) {
      for (c in raw) {
        if (c in '0'..'9') append(bn[c - '0']) else append(c)
      }
    }
  }

  fun dismissUpdatePopup() {
    if (!appUpdateConfig.value.forceUpdate) {
      _isUpdatePopupDismissed.value = true
    }
  }

  fun openAppUpdateLink() {
    val safeUrl = ZonexSecurityGuard.sanitizeExternalUrl(appUpdateConfig.value.downloadUrl)
    if (safeUrl == null) {
      showMessage(
        if (settings.value.isBanglaUi) "আপডেট লিংক পাওয়া যায়নি।"
        else "Update download link is unavailable."
      )
      return
    }
    runCatching {
      appContext.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      )
    }.onFailure {
      showMessage("Could not open update link: ${it.localizedMessage}")
    }
  }

  fun showMessage(msg: String) {
    _statusBannerMessage.value = msg
  }

  fun clearMessage() {
    _statusBannerMessage.value = null
  }
}
