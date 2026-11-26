package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.MainTab
import com.example.service.FloatingOverlayBus
import com.example.ui.components.AppUpdatePopupModal
import com.example.ui.components.CartesiaApiKeyDialog
import com.example.ui.components.CurrentlyPlayingBar
import com.example.ui.components.CustomVoiceIdDialog
import com.example.ui.components.FloatingWindowOverlayWidget
import com.example.ui.components.MaintenanceBlockingModal
import com.example.ui.components.NoticePopupModal
import com.example.ui.components.OverlayPermissionDialog
import com.example.ui.components.VoiceSelectorDialog
import com.example.ui.components.claySurface
import com.example.ui.screens.AudioLabScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GameModeHudScreen
import com.example.ui.screens.KeyActivationScreen
import com.example.ui.screens.MixerScreen
import com.example.ui.screens.MySoundsScreen
import com.example.ui.screens.OnboardingOverlaySequence
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShizukuRootDialog
import com.example.ui.screens.SoundsHomeScreen
import com.example.ui.screens.UploadSoundModal
import com.example.ui.theme.LocalZonexPalette
import com.example.ui.theme.ZonexSoundTheme
import com.example.ui.viewmodel.ZonexViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    setContent {
      val vm: ZonexViewModel = viewModel()
      val settings by vm.settings.collectAsStateWithLifecycle()

      ZonexSoundTheme(themeStyle = settings.themeStyle) {
        ZonexRootHost(vm = vm)
      }
    }
  }
}

@Composable
private fun ZonexRootHost(vm: ZonexViewModel) {
  val palette = LocalZonexPalette.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val isAuthChecking by vm.isAuthChecking.collectAsStateWithLifecycle()
  val currentUser by vm.currentUser.collectAsStateWithLifecycle()
  val authForm by vm.authForm.collectAsStateWithLifecycle()
  val onboarding by vm.onboardingState.collectAsStateWithLifecycle()
  val settings by vm.settings.collectAsStateWithLifecycle()
  val selectedTab by vm.selectedTab.collectAsStateWithLifecycle()
  val filteredPublicSounds by vm.filteredPublicSounds.collectAsStateWithLifecycle()
  val categories by vm.categories.collectAsStateWithLifecycle()
  val managedCategories by vm.managedCategories.collectAsStateWithLifecycle()
  val selectedCategory by vm.selectedCategory.collectAsStateWithLifecycle()
  val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
  val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
  val likedIds by vm.likedSoundIds.collectAsStateWithLifecycle()
  val downloadedSounds by vm.downloadedSounds.collectAsStateWithLifecycle()
  val userPublicUploads by vm.userPublicUploads.collectAsStateWithLifecycle()
  val privateSounds by vm.privateSounds.collectAsStateWithLifecycle()
  val favoriteSounds by vm.favoriteSounds.collectAsStateWithLifecycle()
  val playback by vm.playbackState.collectAsStateWithLifecycle()
  val adminNotice by vm.adminNotice.collectAsStateWithLifecycle()
  val developerConfig by vm.developerConfig.collectAsStateWithLifecycle()
  val appConfig by vm.appConfig.collectAsStateWithLifecycle()
  val isGameModeOpen by vm.isGameModeOpen.collectAsStateWithLifecycle()
  val isGameModeMicOn by vm.isGameModeMicOn.collectAsStateWithLifecycle()
  val isInAppFloatingOpen by vm.isInAppFloatingOpen.collectAsStateWithLifecycle()
  val isInAppFloatingMinimized by vm.isInAppFloatingMinimized.collectAsStateWithLifecycle()
  val isSystemOverlayRunning by FloatingOverlayBus.isSystemOverlayRunning.collectAsStateWithLifecycle()
  val showOverlayPermissionDialog by vm.showOverlayPermissionDialog.collectAsStateWithLifecycle()
  val isNoticePopupDismissed by vm.isNoticePopupDismissed.collectAsStateWithLifecycle()
  val uploadState by vm.uploadState.collectAsStateWithLifecycle()
  val audioLabState by vm.audioLabState.collectAsStateWithLifecycle()
  val isTestingSpeaker by vm.isTestingSpeaker.collectAsStateWithLifecycle()
  val isTestingMic by vm.isTestingMic.collectAsStateWithLifecycle()
  val micLevelNormalized by vm.micLevelNormalized.collectAsStateWithLifecycle()
  val micDbLevel by vm.micDbLevel.collectAsStateWithLifecycle()
  val statusMessage by vm.statusBannerMessage.collectAsStateWithLifecycle()
  val showShizukuDialog by vm.showShizukuDialog.collectAsStateWithLifecycle()
  val shizukuState by vm.shizukuState.collectAsStateWithLifecycle()
  val ttsInputText by vm.ttsInputText.collectAsStateWithLifecycle()
  val ttsPitch by vm.ttsPitch.collectAsStateWithLifecycle()
  val ttsSpeechRate by vm.ttsSpeechRate.collectAsStateWithLifecycle()
  val cartesiaVoices by vm.cartesiaVoices.collectAsStateWithLifecycle()
  val isSynthesizingTts by vm.isSynthesizingTts.collectAsStateWithLifecycle()
  val isSavingTtsCard by vm.isSavingTtsCard.collectAsStateWithLifecycle()
  val ttsError by vm.ttsError.collectAsStateWithLifecycle()
  val lastSynthesizedTtsFile by vm.lastSynthesizedTtsFile.collectAsStateWithLifecycle()
  val showCartesiaKeyDialog by vm.showCartesiaKeyDialog.collectAsStateWithLifecycle()
  val showVoiceChooserDialog by vm.showVoiceChooserDialog.collectAsStateWithLifecycle()
  val showCustomVoiceDialog by vm.showCustomVoiceDialog.collectAsStateWithLifecycle()
  val isRefreshingVoices by vm.isRefreshingVoices.collectAsStateWithLifecycle()
  val appUpdateConfig by vm.appUpdateConfig.collectAsStateWithLifecycle()
  val shouldShowUpdatePopup by vm.shouldShowUpdatePopup.collectAsStateWithLifecycle()
  val keyGateConfig by vm.keyGateConfig.collectAsStateWithLifecycle()
  val accessKeyState by vm.accessKeyState.collectAsStateWithLifecycle()
  val maintenanceConfig by vm.maintenanceConfig.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        vm.refreshHardwareDiagnostics()
        vm.onOverlayPermissionReturned()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(statusMessage) {
    statusMessage?.let {
      snackbarHostState.showSnackbar(it)
      vm.clearMessage()
    }
  }

  LaunchedEffect(playback.errorMessage) {
    playback.errorMessage?.let {
      snackbarHostState.showSnackbar(it)
      vm.clearPlaybackError()
    }
  }

  if (isAuthChecking) {
    Box(
      modifier = Modifier.fillMaxSize().background(palette.background),
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator(color = palette.goldPrimary)
    }
    return
  }

  if (maintenanceConfig.enabled) {
    MaintenanceBlockingModal(
      config = maintenanceConfig,
      isBangla = settings.isBanglaUi,
    )
    return
  }

  if (currentUser == null) {
    AuthScreen(
      state = authForm,
      onSelectTab = vm::selectAuthTab,
      onFullNameChange = vm::updateFullName,
      onEmailChange = vm::updateEmail,
      onPasswordChange = vm::updatePassword,
      onConfirmPasswordChange = vm::updateConfirmPassword,
      onKeepLoggedInChange = vm::updateKeepLoggedIn,
      onSubmitLogin = vm::submitLogin,
      onSubmitSignUp = vm::submitSignUp,
    )
    return
  }

  if (keyGateConfig.requireKeyAfterLogin) {
    if (accessKeyState.isChecking) {
      Box(
        modifier = Modifier.fillMaxSize().background(palette.background),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(color = palette.goldPrimary)
      }
      return
    }
    if (!accessKeyState.isActivated) {
      KeyActivationScreen(
        accessKeyState = accessKeyState,
        isBangla = settings.isBanglaUi,
        getKeyUrl = keyGateConfig.getKeyUrl,
        socialLinks = keyGateConfig.socialLinks,
        developer = developerConfig,
        onToggleLanguage = vm::toggleLanguageBangla,
        onActivateKey = vm::activateAccessKey,
        onGetKeyClick = vm::openGetKeyWebsite,
        onLogout = vm::logout,
      )
      return
    }
  }

  if (onboarding.isActive) {
    OnboardingOverlaySequence(
      userName = currentUser?.displayName ?: "Gamer",
      showWelcomeOnTop = onboarding.showWelcomeOnTop,
      developer = developerConfig,
      isBangla = settings.isBanglaUi,
      onDismissWelcome = vm::dismissWelcomePopupLayer,
      onGetStarted = { vm.finishOnboarding(goToAudioLab = false) },
      onTestAudioCompatibility = { vm.finishOnboarding(goToAudioLab = true) },
    )
    return
  }

  if (isGameModeOpen) {
    GameModeHudScreen(
      favoriteSounds = favoriteSounds,
      playback = playback,
      isMicOn = isGameModeMicOn,
      isBangla = settings.isBanglaUi,
      onToggleMic = vm::toggleGameModeMic,
      onPlaySound = vm::playSound,
      onStopAll = vm::stopAllSounds,
      onExit = vm::closeGameMode,
    )
    return
  }

  if (selectedTab != MainTab.SOUNDS) {
    BackHandler { vm.selectTab(MainTab.SOUNDS) }
  }

  val downloadedIds = remember(downloadedSounds) { downloadedSounds.map { it.id }.toSet() }
  val isFloatingOpen = isSystemOverlayRunning || isInAppFloatingOpen

  Box(modifier = Modifier.fillMaxSize().background(palette.background)) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavRail = maxWidth >= 840.dp

      Scaffold(
        containerColor = palette.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
          SnackbarHost(hostState = snackbarHostState) { data ->
            Snackbar(
              snackbarData = data,
              shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
              containerColor = palette.surfaceElevated,
              contentColor = palette.goldBright,
            )
          }
        },
        bottomBar = {
          if (!useNavRail) {
            Column(modifier = Modifier.fillMaxWidth()) {
              if (playback.currentSound != null) {
                CurrentlyPlayingBar(
                  playback = playback,
                  onTogglePlayPause = vm::togglePlayPause,
                  onStop = vm::stopAllSounds,
                  isBangla = settings.isBanglaUi,
                )
              }
              ZonexBottomNavigationBar(
                selectedTab = selectedTab,
                isBangla = settings.isBanglaUi,
                onSelectTab = vm::selectTab,
              )
            }
          }
        },
      ) { innerPadding ->
        Row(
          modifier =
            Modifier.fillMaxSize()
              .padding(innerPadding)
              .statusBarsPadding()
        ) {
          if (useNavRail) {
            ZonexNavigationRail(
              selectedTab = selectedTab,
              isBangla = settings.isBanglaUi,
              onSelectTab = vm::selectTab,
            )
          }
          Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when (selectedTab) {
              MainTab.SOUNDS ->
                SoundsHomeScreen(
                  sounds = filteredPublicSounds,
                  categories = categories,
                  selectedCategory = selectedCategory,
                  searchQuery = searchQuery,
                  favoriteIds = favoriteIds,
                  likedIds = likedIds,
                  downloadedIds = downloadedIds,
                  playback = playback,
                  animationsEnabled = settings.soundAnimations,
                  activeOutputSummary = audioLabState.activeOutputSummary,
                  adminNotice = adminNotice,
                  dismissedBannerText = settings.dismissedBannerText,
                  isBanglaUi = settings.isBanglaUi,
                  isFloatingOpen = isFloatingOpen,
                  shizukuState = shizukuState,
                  ttsVoices = cartesiaVoices,
                  selectedVoiceId = settings.selectedCartesiaVoiceId,
                  savedKeyCount = settings.cartesiaApiKeys.size,
                  ttsPitch = ttsPitch,
                  ttsSpeechRate = ttsSpeechRate,
                  ttsInputText = ttsInputText,
                  isSynthesizingTts = isSynthesizingTts,
                  isSavingTtsCard = isSavingTtsCard,
                  ttsError = ttsError,
                  canSaveLastTtsOutput = lastSynthesizedTtsFile != null,
                  accessKeyState = accessKeyState,
                  onSearchQueryChange = vm::updateSearchQuery,
                  onSelectCategory = vm::selectCategory,
                  onPlaySound = vm::playSound,
                  onToggleFavorite = vm::toggleFavorite,
                  onToggleLike = vm::toggleLike,
                  onSaveSound = vm::saveToMySounds,
                  onStopAll = vm::stopAllSounds,
                  onMaxVolume = vm::maxSystemVolume,
                  onToggleLanguage = vm::toggleLanguageBangla,
                  onOpenShizukuDialog = vm::openShizukuDialog,
                  onExtendKeyClick = vm::openGetKeyWebsite,
                  onRedeemKey = vm::activateAccessKey,
                  onFloatClick = vm::launchFloatingWindow,
                  onOpenAudioLab = { vm.selectTab(MainTab.AUDIO_LAB) },
                  onDismissBanner = vm::dismissAdminBanner,
                  onTtsInputChange = vm::updateTtsInputText,
                  onOpenVoiceChooser = vm::openVoiceChooserDialog,
                  onTtsPitchChange = vm::updateTtsPitch,
                  onTtsRateChange = vm::updateTtsSpeechRate,
                  onOpenCartesiaKeyDialog = vm::openCartesiaKeyDialog,
                  onRefreshCartesiaVoices = vm::refreshCartesiaVoices,
                  onSendPlayTts = vm::synthesizeAndPlayTts,
                  onSaveTtsCard = vm::saveLastTtsAsSoundCard,
                )
              MainTab.MY_SOUNDS ->
                MySoundsScreen(
                  downloadedSounds = downloadedSounds,
                  userPublicUploads = userPublicUploads,
                  privateSounds = privateSounds,
                  favoriteIds = favoriteIds,
                  downloadedIds = downloadedIds,
                  playback = playback,
                  animationsEnabled = settings.soundAnimations,
                  isBanglaUi = settings.isBanglaUi,
                  onPlaySound = vm::playSound,
                  onStopAll = vm::stopAllSounds,
                  onToggleFavorite = vm::toggleFavorite,
                  onSaveSound = vm::saveToMySounds,
                  onRemoveDownload = vm::removeFromDownloads,
                  onDeleteUploaded = vm::deleteUploadedSound,
                  onOpenUploadModal = vm::openUploadModal,
                  onOpenGameMode = vm::openGameMode,
                )
              MainTab.MIXER ->
                MixerScreen(
                  settings = settings,
                  activeOutputSummary = audioLabState.activeOutputSummary,
                  isMicActive = isTestingMic,
                  onMasterVolumeChange = vm::setMasterVolume,
                  onSfxVolumeChange = vm::setSfxChannelVolume,
                  onMediaVolumeChange = vm::setMediaChannelVolume,
                )
              MainTab.AUDIO_LAB ->
                AudioLabScreen(
                  labState = audioLabState,
                  shizukuState = shizukuState,
                  isTestingSpeaker = isTestingSpeaker,
                  isTestingMic = isTestingMic,
                  micLevelNormalized = micLevelNormalized,
                  micDbLevel = micDbLevel,
                  isBangla = settings.isBanglaUi,
                  isFloatingOpen = isFloatingOpen,
                  onRefreshDiagnostics = vm::refreshHardwareDiagnostics,
                  onTestSpeaker = vm::playSpeakerTest,
                  onStartMicTest = vm::startMicTest,
                  onStopMicTest = vm::stopMicTest,
                  onLaunchFloatingWindow = vm::launchFloatingWindow,
                  onOpenShizukuDialog = vm::openShizukuDialog,
                )
              MainTab.SETTINGS ->
                SettingsScreen(
                  user = currentUser,
                  settings = settings,
                  developer = developerConfig,
                  onLogout = vm::logout,
                  onSelectTheme = vm::setThemeStyle,
                  onMasterVolumeChange = vm::setMasterVolume,
                  onToggleFreeFireBoost = vm::setFreeFireVoiceBoost,
                  onToggleFloatingBubble = vm::setFloatingBubbleEnabled,
                  onToggleQuickNotification = vm::setQuickNotificationControl,
                  onToggleHapticFeedback = vm::setHapticFeedback,
                  onToggleSoundAnimations = vm::setSoundAnimations,
                  onOpenAudioLab = { vm.selectTab(MainTab.AUDIO_LAB) },
                  onOpenShizukuDialog = vm::openShizukuDialog,
                  onRestoreDefaultLibrary = vm::restoreDefaultSoundLibrary,
                )
            }

            if (useNavRail && playback.currentSound != null) {
              CurrentlyPlayingBar(
                playback = playback,
                onTogglePlayPause = vm::togglePlayPause,
                onStop = vm::stopAllSounds,
                isBangla = settings.isBanglaUi,
                modifier = Modifier.align(Alignment.BottomCenter),
              )
            }
          }
        }
      }
    }

    // Draggable In-App Floating Window (fallback only when system overlay is not active)
    if (isInAppFloatingOpen && !isSystemOverlayRunning) {
      FloatingWindowOverlayWidget(
        favorites = favoriteSounds,
        appLogoUrl = appConfig.logoUrl.ifBlank { developerConfig.logoUrl },
        brandTitle = appConfig.floatingBrandName,
        isMinimized = isInAppFloatingMinimized,
        playingSoundId = if (playback.isPlaying) playback.currentSound?.id else null,
        isBangla = settings.isBanglaUi,
        onPlaySound = vm::playSound,
        onStopAll = vm::stopAllSounds,
        onToggleMinimize = vm::toggleMinimizeFloatingWindow,
        onClose = vm::closeFloatingWindow,
      )
    }

    if (showOverlayPermissionDialog) {
      OverlayPermissionDialog(
        isBangla = settings.isBanglaUi,
        onDismiss = vm::dismissOverlayPermissionDialog,
        onContinueInAppOnly = vm::openInAppFallbackFloating,
      )
    }

    if (uploadState.isOpen) {
      UploadSoundModal(
        uploadState = uploadState,
        availableCategories = managedCategories,
        isBangla = settings.isBanglaUi,
        onDismiss = vm::closeUploadModal,
        onSubmit = vm::submitSoundUpload,
      )
    }

    if (adminNotice.popupActive && !isNoticePopupDismissed) {
      NoticePopupModal(
        notice = adminNotice,
        onDismiss = vm::dismissNoticePopup,
      )
    }

    if (showShizukuDialog) {
      ShizukuRootDialog(
        state = shizukuState,
        isBangla = settings.isBanglaUi,
        onDismiss = vm::closeShizukuDialog,
        onCheckRoot = vm::checkRootAccess,
        onCheckOrRequestShizuku = vm::checkOrRequestShizuku,
        onEnableInjection = vm::enableShizukuInjection,
        onDisableInjection = vm::disableShizukuInjection,
        onRunDiagnostic = vm::runShizukuDiagnosticReport,
      )
    }

    if (showCartesiaKeyDialog) {
      CartesiaApiKeyDialog(
        savedKeys = settings.cartesiaApiKeys,
        activeKeyIndex = settings.activeCartesiaKeyIndex,
        isBangla = settings.isBanglaUi,
        onAddKey = vm::addCartesiaApiKey,
        onRemoveKey = vm::removeCartesiaApiKey,
        onSetActiveKey = vm::setActiveCartesiaKey,
        onDismiss = vm::closeCartesiaKeyDialog,
      )
    }

    if (showVoiceChooserDialog) {
      VoiceSelectorDialog(
        voices = cartesiaVoices,
        selectedVoiceId = settings.selectedCartesiaVoiceId,
        isRefreshing = isRefreshingVoices,
        isBangla = settings.isBanglaUi,
        onSelectVoice = vm::selectCartesiaVoice,
        onMoveVoiceOrder = vm::moveVoiceOrder,
        onDeleteCustomVoice = vm::deleteCustomTtsVoice,
        onOpenCustomVoiceDialog = vm::openCustomVoiceDialog,
        onRefreshVoices = vm::refreshCartesiaVoices,
        onDismiss = vm::closeVoiceChooserDialog,
      )
    }

    if (showCustomVoiceDialog) {
      CustomVoiceIdDialog(
        isBangla = settings.isBanglaUi,
        onAdd = vm::addCustomTtsVoice,
        onDismiss = vm::closeCustomVoiceDialog,
      )
    }

    if (shouldShowUpdatePopup) {
      AppUpdatePopupModal(
        config = appUpdateConfig,
        isBangla = settings.isBanglaUi,
        onUpdateClick = vm::openAppUpdateLink,
        onDismiss = vm::dismissUpdatePopup,
      )
    }
  }
}

@Composable
private fun ZonexBottomNavigationBar(
  selectedTab: MainTab,
  isBangla: Boolean,
  onSelectTab: (MainTab) -> Unit,
) {
  val palette = LocalZonexPalette.current
  Box(
    modifier =
      Modifier.fillMaxWidth()
        .background(palette.background)
        .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    NavigationBar(
      modifier =
        Modifier.fillMaxWidth()
          .claySurface(
            baseColor = palette.surface,
            cornerRadius = 24.dp,
            elevation = 14.dp,
            accentBorderColor = palette.border.copy(alpha = 0.7f),
          ),
      containerColor = androidx.compose.ui.graphics.Color.Transparent,
      contentColor = palette.textPrimary,
      windowInsets = WindowInsets.navigationBars,
    ) {
      MainTab.entries.forEach { tab ->
        val selected = tab == selectedTab
        val (selectedIcon, unselectedIcon) = tabIcons(tab)
        val tabText = tab.localizedLabel(isBangla)
        NavigationBarItem(
          selected = selected,
          onClick = { onSelectTab(tab) },
          icon = {
            Icon(
              imageVector = if (selected) selectedIcon else unselectedIcon,
              contentDescription = tabText,
              modifier = Modifier.size(22.dp),
            )
          },
          label = {
            Text(
              text = tabText,
              style = MaterialTheme.typography.labelSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          colors =
            NavigationBarItemDefaults.colors(
              selectedIconColor = palette.onGold,
              selectedTextColor = palette.goldPrimary,
              indicatorColor = palette.goldPrimary,
              unselectedIconColor = palette.textSecondary,
              unselectedTextColor = palette.textSecondary,
            ),
          modifier = Modifier.testTag("nav_tab_${tab.route}"),
        )
      }
    }
  }
}

@Composable
private fun ZonexNavigationRail(
  selectedTab: MainTab,
  isBangla: Boolean,
  onSelectTab: (MainTab) -> Unit,
) {
  val palette = LocalZonexPalette.current
  NavigationRail(
    containerColor = palette.surface,
    contentColor = palette.textPrimary,
  ) {
    MainTab.entries.forEach { tab ->
      val selected = tab == selectedTab
      val (selectedIcon, unselectedIcon) = tabIcons(tab)
      val tabText = tab.localizedLabel(isBangla)
      NavigationRailItem(
        selected = selected,
        onClick = { onSelectTab(tab) },
        icon = {
          Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = tabText,
          )
        },
        label = { Text(tabText, style = MaterialTheme.typography.labelSmall) },
        colors =
          NavigationRailItemDefaults.colors(
            selectedIconColor = palette.onGold,
            selectedTextColor = palette.goldPrimary,
            indicatorColor = palette.goldPrimary,
            unselectedIconColor = palette.textSecondary,
            unselectedTextColor = palette.textSecondary,
          ),
      )
    }
  }
}

private fun tabIcons(tab: MainTab) =
  when (tab) {
    MainTab.SOUNDS -> Icons.Filled.GraphicEq to Icons.Outlined.GraphicEq
    MainTab.MY_SOUNDS -> Icons.Filled.FolderSpecial to Icons.Outlined.FolderSpecial
    MainTab.MIXER -> Icons.Filled.Tune to Icons.Outlined.Tune
    MainTab.AUDIO_LAB -> Icons.Filled.Science to Icons.Outlined.Science
    MainTab.SETTINGS -> Icons.Filled.Settings to Icons.Outlined.Settings
  }
