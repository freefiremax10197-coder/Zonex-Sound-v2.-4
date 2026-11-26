package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.shizuku.ShizukuRootManager
import com.example.shizuku.ShizukuRootState
import com.example.ui.components.ClayButton
import com.example.ui.components.ClayCard
import com.example.ui.components.ClayPill
import com.example.ui.theme.LocalZonexPalette

private const val SHIZUKU_PLAY_STORE_URL =
  "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"

@Composable
fun ShizukuRootDialog(
  state: ShizukuRootState,
  isBangla: Boolean,
  onDismiss: () -> Unit,
  onCheckRoot: () -> Unit,
  onCheckOrRequestShizuku: () -> Unit,
  onEnableInjection: () -> Unit,
  onDisableInjection: () -> Unit,
  onRunDiagnostic: () -> Unit,
) {
  val palette = LocalZonexPalette.current
  val context = LocalContext.current
  var showGuide by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .safeDrawingPadding()
          .padding(horizontal = 16.dp, vertical = 14.dp),
      contentAlignment = Alignment.Center,
    ) {
      ClayCard(
        modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
        cornerRadius = 24.dp,
        elevation = 16.dp,
        baseColor = palette.surfaceElevated,
        borderColor = palette.goldPrimary.copy(alpha = 0.7f),
        borderWidth = 1.5.dp,
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .padding(18.dp)
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = palette.goldPrimary,
              )
              Text(
                text = if (isBangla) "Shizuku / Root অপশন" else "Shizuku / Root Options",
                style = MaterialTheme.typography.titleLarge,
                color = palette.goldBright,
              )
            }
            IconButton(onClick = onDismiss) {
              Icon(Icons.Filled.Close, contentDescription = "Close", tint = palette.textSecondary)
            }
          }

          // Live Status Banner
          ClayCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            elevation = 6.dp,
            baseColor = palette.surface,
            borderColor = if (state.isInjectionActive) palette.success else palette.border,
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = state.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color =
                  if (state.isInjectionActive || state.hasShizukuPermission || state.isRootAvailable) palette.success
                  else palette.textPrimary,
                fontWeight = FontWeight.SemiBold,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadgeChip(
                  label =
                    "Shizuku: ${if (state.hasShizukuPermission) "ON ✓" else if (state.isShizukuRunning) "NEED PERM" else "OFF"}",
                  active = state.hasShizukuPermission,
                )
                StatusBadgeChip(
                  label = "Root: ${if (state.isRootAvailable) "YES ✓" else "NO"}",
                  active = state.isRootAvailable,
                )
                StatusBadgeChip(
                  label = "Transfer: ${if (state.isInjectionActive) "ACTIVE" else "IDLE"}",
                  active = state.isInjectionActive,
                )
              }
            }
          }

          // 1. Root Check — Red when disconnected, Green + Checkmark when connected
          ShizukuActionBtn(
            label = if (isBangla) "🔑 Root চেক করো" else "🔑 Check Root Access",
            icon = null,
            containerColor =
              if (state.isRootAvailable) palette.success.copy(alpha = 0.22f)
              else palette.danger.copy(alpha = 0.20f),
            contentColor =
              if (state.isRootAvailable) palette.success else Color(0xFFFF7A90),
            borderColor = if (state.isRootAvailable) palette.success else palette.danger,
            isConnectedCheck = state.isRootAvailable,
            onClick = onCheckRoot,
            modifier = Modifier.testTag("btn_check_root"),
          )

          // 2. Shizuku Check / Permission — Red when disconnected, Green + Checkmark when connected
          ShizukuActionBtn(
            label =
              if (isBangla) "🛡️ Shizuku চেক / পারমিশন (রুট ছাড়া)"
              else "🛡️ Check / Request Shizuku Permission",
            icon = null,
            containerColor =
              if (state.hasShizukuPermission) palette.success.copy(alpha = 0.22f)
              else palette.danger.copy(alpha = 0.20f),
            contentColor =
              if (state.hasShizukuPermission) palette.success else Color(0xFFFF7A90),
            borderColor = if (state.hasShizukuPermission) palette.success else palette.danger,
            isConnectedCheck = state.hasShizukuPermission,
            onClick = onCheckOrRequestShizuku,
            modifier = Modifier.testTag("btn_check_shizuku"),
          )

          // Open or Install Shizuku App Shortcut
          if (!state.hasShizukuPermission) {
            OutlinedButton(
              onClick = {
                val installedPkg = ShizukuRootManager.findInstalledShizukuPackage(context)
                val launch =
                  installedPkg?.let { context.packageManager.getLaunchIntentForPackage(it) }
                if (launch != null) {
                  context.startActivity(launch)
                } else {
                  runCatching {
                    context.startActivity(
                      Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_PLAY_STORE_URL))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                  }
                }
              },
              border = BorderStroke(1.dp, palette.info),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.info),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(46.dp).testTag("btn_install_shizuku"),
            ) {
              Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                if (state.isShizukuInstalled) {
                  if (isBangla) "Shizuku অ্যাপ খোলো" else "Open Shizuku App"
                } else {
                  if (isBangla) "Shizuku ইন্সটল করো" else "Install Shizuku App"
                }
              )
            }
          }

          // 3. Setup Guide Toggle (emoji only, no duplicate vector icon)
          ShizukuActionBtn(
            label = if (isBangla) "📖 Shizuku সেটআপ গাইড" else "📖 Shizuku Setup Guide",
            icon = null,
            containerColor = palette.surface,
            contentColor = palette.textPrimary,
            borderColor = palette.border,
            onClick = { showGuide = !showGuide },
          )

          AnimatedVisibility(visible = showGuide) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = palette.background,
              border = BorderStroke(1.dp, palette.border),
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text =
                    if (isBangla) {
                      "১. Play Store থেকে Shizuku অ্যাপ ইন্সটল ও চালু করুন (Wireless Debugging দিয়ে Start করুন)।\n" +
                        "২. উপরের 'Shizuku চেক / পারমিশন' বাটনে ট্যাপ করে Allow দিন (সবুজ রঙ ও টিক চিহ্ন ✓ আসবে) অথবা Shizuku → Application management থেকে Zonex Sound অন করুন।\n" +
                        "৩. Shizuku বা Root কানেক্ট হলেই ফ্লোটিং উইন্ডো এবং মাইক ইনজেকশন কাজ করবে।"
                    } else {
                      "1. Install and start Shizuku via Wireless Debugging.\n" +
                        "2. Tap 'Check / Request Shizuku Permission' above until it turns Green with a checkmark (✓).\n" +
                        "3. Toggle Mic Injection ON (green open mic icon) for +12dB communication speaker routing."
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.textSecondary,
                )
              }
            }
          }

          // 4. Single Toggle Button for Mic Injection (Green + Mic ON when active, Red + MicOff when OFF)
          val injectionOn = state.isInjectionActive
          ShizukuActionBtn(
            label =
              if (injectionOn) {
                if (isBangla) "মাইক ইনজেকশন চালু (বন্ধ করতে ট্যাপ)" else "Mic Injection ON (Tap to Stop)"
              } else {
                if (isBangla) "মাইক ইনজেকশন চালু করো" else "Start Mic Injection"
              },
            icon = if (injectionOn) Icons.Filled.Mic else Icons.Filled.MicOff,
            containerColor =
              if (injectionOn) palette.success else palette.danger.copy(alpha = 0.22f),
            contentColor =
              if (injectionOn) Color(0xFF041C10) else Color(0xFFFF7A90),
            borderColor = if (injectionOn) palette.success else palette.danger,
            isConnectedCheck = injectionOn,
            onClick = { if (injectionOn) onDisableInjection() else onEnableInjection() },
            modifier = Modifier.testTag("btn_toggle_injection"),
          )

          // 5. Diagnostic Report (emoji only, no duplicate vector icon)
          ShizukuActionBtn(
            label =
              if (isBangla) "🧪 Root / Shizuku ডায়াগনস্টিক রিপোর্ট"
              else "🧪 Run Shizuku / Root Diagnostic Report",
            icon = null,
            containerColor = palette.surface,
            contentColor = palette.goldPrimary,
            borderColor = palette.goldPrimary.copy(alpha = 0.5f),
            onClick = onRunDiagnostic,
            modifier = Modifier.testTag("btn_shizuku_diag"),
          )

          if (state.diagnosticReport.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = palette.background,
              border = BorderStroke(1.dp, palette.border),
            ) {
              Text(
                text = state.diagnosticReport,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = palette.textPrimary,
                modifier = Modifier.padding(12.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StatusBadgeChip(label: String, active: Boolean) {
  val palette = LocalZonexPalette.current
  ClayPill(
    baseColor = if (active) palette.success.copy(alpha = 0.18f) else palette.danger.copy(alpha = 0.14f),
    borderColor = if (active) palette.success else palette.danger.copy(alpha = 0.55f),
    cornerRadius = 10.dp,
    elevation = 3.dp,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (active) palette.success else Color(0xFFFF7A90),
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

@Composable
private fun ShizukuActionBtn(
  label: String,
  icon: ImageVector?,
  containerColor: Color,
  contentColor: Color,
  borderColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isConnectedCheck: Boolean = false,
) {
  ClayButton(
    onClick = onClick,
    containerColor = containerColor,
    contentColor = contentColor,
    borderColor = borderColor,
    cornerRadius = 16.dp,
    elevation = 6.dp,
    modifier = modifier.fillMaxWidth().height(50.dp),
  ) {
    if (icon != null) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
      Spacer(modifier = Modifier.width(8.dp))
    }
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = if (isConnectedCheck) Modifier.weight(1f, fill = false) else Modifier,
    )
    if (isConnectedCheck) {
      Spacer(modifier = Modifier.width(8.dp))
      Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = "Connected",
        tint = contentColor,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}
