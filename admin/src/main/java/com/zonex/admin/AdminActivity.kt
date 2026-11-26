package com.zonex.admin

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class AdminActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
    )
    setContent {
      MaterialTheme(
        colorScheme =
          darkColorScheme(
            primary = Color(0xFFF5B82E),
            onPrimary = Color.Black,
            background = AdminClayBg,
            surface = AdminClaySurface,
          )
      ) {
        AdminRootHost()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminRootHost(vm: AdminViewModel = viewModel()) {
  val authEmail by vm.authEmail.collectAsStateWithLifecycle()
  val isLoggingIn by vm.isLoggingIn.collectAsStateWithLifecycle()
  val loginError by vm.loginError.collectAsStateWithLifecycle()
  val selectedTab by vm.selectedTab.collectAsStateWithLifecycle()
  val publicSounds by vm.publicSounds.collectAsStateWithLifecycle()
  val managedCategories by vm.managedCategories.collectAsStateWithLifecycle()
  val playingId by vm.playingSoundId.collectAsStateWithLifecycle()
  val soundDraft by vm.soundEditorDraft.collectAsStateWithLifecycle()
  val isSavingSound by vm.isSavingSound.collectAsStateWithLifecycle()
  val saveProgress by vm.saveProgress.collectAsStateWithLifecycle()
  val noticeConfig by vm.noticeConfig.collectAsStateWithLifecycle()
  val maintenanceConfig by vm.maintenanceConfig.collectAsStateWithLifecycle()
  val appUpdateConfig by vm.appUpdateConfig.collectAsStateWithLifecycle()
  val appConfig by vm.appConfig.collectAsStateWithLifecycle()
  val devConfig by vm.developerConfig.collectAsStateWithLifecycle()
  val ttsVoices by vm.ttsVoices.collectAsStateWithLifecycle()
  val users by vm.usersList.collectAsStateWithLifecycle()
  val keyGateConfig by vm.keyGateConfig.collectAsStateWithLifecycle()
  val accessKeys by vm.accessKeys.collectAsStateWithLifecycle()
  val isOnline by vm.isOnline.collectAsStateWithLifecycle()
  val isSyncing by vm.isSyncing.collectAsStateWithLifecycle()
  val pendingSyncCount by vm.pendingSyncCount.collectAsStateWithLifecycle()
  val statusMessage by vm.statusMessage.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(statusMessage) {
    statusMessage?.let {
      snackbarHostState.showSnackbar(it)
      vm.clearMessage()
    }
  }

  if (authEmail == null) {
    AdminLoginScreen(
      isLoading = isLoggingIn,
      errorMessage = loginError,
      onLogin = vm::signIn,
    )
    return
  }

  Scaffold(
    containerColor = AdminClayBg,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = {
      SnackbarHost(snackbarHostState) { data ->
        Snackbar(
          snackbarData = data,
          shape = RoundedCornerShape(18.dp),
          containerColor = AdminClaySurfaceElevated,
          contentColor = Color(0xFFF5B82E),
        )
      }
    },
    topBar = {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .adminClaySurface(
              baseColor = AdminClaySurface,
              cornerRadius = 20.dp,
              elevation = 10.dp,
              accentBorderColor = AdminClayBorder.copy(alpha = 0.7f),
            )
      ) {
        TopAppBar(
          title = {
            Row {
              Text(
                "SOUND ADMIN",
                color = Color(0xFFF5B82E),
                fontWeight = FontWeight.ExtraBold,
              )
            }
          },
          actions = {
            if (!isOnline || pendingSyncCount > 0 || isSyncing) {
              val badgeColor = if (!isOnline) Color(0xFFE11D48) else Color(0xFFF5B82E)
              val badgeText =
                when {
                  isSyncing -> "Syncing..."
                  !isOnline && pendingSyncCount > 0 -> "Offline ($pendingSyncCount)"
                  !isOnline -> "Offline"
                  else -> "Pending ($pendingSyncCount)"
                }
              AdminClayPill(
                onClick = { if (isOnline && pendingSyncCount > 0) vm.syncPendingOperations() },
                baseColor = badgeColor.copy(alpha = 0.16f),
                borderColor = badgeColor.copy(alpha = 0.7f),
                cornerRadius = 12.dp,
                elevation = 2.dp,
                modifier = Modifier.padding(end = 6.dp),
              ) {
                Text(
                  text = badgeText,
                  color = badgeColor,
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
            IconButton(onClick = vm::signOut) {
              Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Logout",
                tint = Color(0xFFE11D48),
              )
            }
          },
          windowInsets = WindowInsets(0, 0, 0, 0),
          colors =
            TopAppBarDefaults.topAppBarColors(
              containerColor = Color.Transparent,
              titleContentColor = Color.White,
            ),
        )
      }
    },
    bottomBar = {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
            .adminClaySurface(
              baseColor = AdminClaySurface,
              cornerRadius = 24.dp,
              elevation = 14.dp,
              accentBorderColor = AdminClayBorder.copy(alpha = 0.7f),
            )
      ) {
        NavigationBar(
          containerColor = Color.Transparent,
          windowInsets = WindowInsets.navigationBars,
        ) {
          AdminTab.entries.forEach { tab ->
            val icon =
              when (tab) {
                AdminTab.SOUNDS -> Icons.Filled.GraphicEq
                AdminTab.KEYS -> Icons.Filled.Key
                AdminTab.NOTICE -> Icons.Filled.Campaign
                AdminTab.BRANDING -> Icons.Filled.Brush
                AdminTab.USERS -> Icons.Filled.Group
              }
            NavigationBarItem(
              selected = selectedTab == tab,
              onClick = { vm.selectTab(tab) },
              icon = { Icon(icon, contentDescription = tab.label) },
              label = { Text(tab.label) },
              colors =
                NavigationBarItemDefaults.colors(
                  selectedIconColor = Color.Black,
                  selectedTextColor = Color(0xFFF5B82E),
                  indicatorColor = Color(0xFFF5B82E),
                  unselectedIconColor = Color(0xFF9CA3AF),
                  unselectedTextColor = Color(0xFF9CA3AF),
                ),
            )
          }
        }
      }
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(AdminClayBg)) {
      when (selectedTab) {
        AdminTab.SOUNDS ->
          AdminSoundsTab(
            sounds = publicSounds,
            managedCategories = managedCategories,
            playingId = playingId,
            onAddCategory = vm::addCategory,
            onRemoveCategory = vm::removeCategory,
            onAddClick = vm::openNewSoundDialog,
            onPlayToggle = vm::togglePlayPreview,
            onEditClick = vm::openEditSoundDialog,
            onDeleteClick = vm::deletePublicSound,
          )
        AdminTab.KEYS ->
          AdminKeysTab(
            config = keyGateConfig,
            keys = accessKeys,
            onSaveConfig = vm::saveKeyGateConfig,
            onGenerateKeys = vm::generateKeys,
            onToggleRevoke = vm::toggleRevokeKey,
            onResetBinding = vm::resetKeyBinding,
            onDeleteKey = vm::deleteAccessKey,
          )
        AdminTab.NOTICE ->
          AdminNoticeTab(
            initial = noticeConfig,
            maintenanceInitial = maintenanceConfig,
            updateInitial = appUpdateConfig,
            onSave = vm::saveNotice,
            onSaveMaintenance = vm::saveMaintenanceConfig,
            onSaveUpdateConfig = vm::saveAppUpdateConfig,
          )
        AdminTab.BRANDING ->
          AdminBrandingTab(
            appConfig = appConfig,
            devConfig = devConfig,
            ttsVoices = ttsVoices,
            onSaveAppBranding = vm::saveAppBranding,
            onSaveDeveloper = vm::saveDeveloperConfig,
            onSaveTtsVoice = vm::saveTtsVoice,
            onDeleteTtsVoice = vm::deleteTtsVoice,
          )
        AdminTab.USERS ->
          AdminUsersTab(
            users = users,
            playingId = playingId,
            onPlayUserSound = vm::togglePlayUserSound,
            onDeleteUserSound = vm::deleteUserSound,
          )
      }
    }
  }

  soundDraft?.let { draft ->
    AdminSoundEditorDialog(
      draft = draft,
      managedCategories = managedCategories,
      isSaving = isSavingSound,
      progress = saveProgress,
      onUpdateDraft = vm::updateDraft,
      onDismiss = vm::closeSoundDialog,
      onSave = vm::saveSoundDraft,
    )
  }
}
