package com.zonex.admin

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val AdminGold = Color(0xFFF5B82E)
private val AdminBg = AdminClayBg
private val AdminSurface = AdminClaySurface
private val AdminSurfaceElevated = AdminClaySurfaceElevated
private val AdminBorder = AdminClayBorder
private val AdminTextPrimary = Color(0xFFF8FAFC)
private val AdminTextSecondary = Color(0xFFA6B0CE)
private val AdminDanger = Color(0xFFE11D48)
private val AdminSuccess = Color(0xFF22C55E)

@Composable
fun AdminLoginScreen(
  isLoading: Boolean,
  errorMessage: String?,
  onLogin: (String, String) -> Unit,
) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }

  Box(
    modifier = Modifier.fillMaxSize().background(AdminBg).padding(20.dp),
    contentAlignment = Alignment.Center,
  ) {
    AdminClayCard(
      modifier = Modifier.fillMaxWidth(),
      cornerRadius = 26.dp,
      elevation = 16.dp,
      baseColor = AdminSurface,
      borderColor = AdminGold.copy(alpha = 0.65f),
      borderWidth = 1.5.dp,
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier =
            Modifier.size(64.dp)
              .adminClaySurface(
                baseColor = AdminSurfaceElevated,
                cornerRadius = 32.dp,
                elevation = 8.dp,
                accentBorderColor = AdminGold.copy(alpha = 0.6f),
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(Icons.Filled.Security, contentDescription = null, tint = AdminGold, modifier = Modifier.size(30.dp))
        }
        Text(
          text = "SOUND ADMIN",
          style = MaterialTheme.typography.headlineSmall,
          color = AdminGold,
          fontWeight = FontWeight.ExtraBold,
        )
        Text(
          text = "Sign in to manage Zonex Sound sounds, notices, logo & users",
          style = MaterialTheme.typography.bodySmall,
          color = AdminTextSecondary,
        )

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Admin Email") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = adminFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text("Password") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          visualTransformation = PasswordVisualTransformation(),
          colors = adminFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )

        if (!errorMessage.isNullOrBlank()) {
          Text(errorMessage, color = AdminDanger, style = MaterialTheme.typography.bodySmall)
        }

        AdminClayButton(
          onClick = { onLogin(email, password) },
          enabled = !isLoading,
          containerColor = AdminGold,
          contentColor = Color.Black,
          cornerRadius = 18.dp,
          elevation = 10.dp,
          modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
          if (isLoading) {
            CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
          } else {
            Text("SIGN IN TO ADMIN PANEL", fontWeight = FontWeight.ExtraBold)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSoundsTab(
  sounds: List<AdminSound>,
  managedCategories: List<String>,
  playingId: String?,
  onAddCategory: (String) -> Unit,
  onRemoveCategory: (String) -> Unit,
  onAddClick: () -> Unit,
  onPlayToggle: (AdminSound) -> Unit,
  onEditClick: (AdminSound) -> Unit,
  onDeleteClick: (AdminSound) -> Unit,
) {
  var query by remember { mutableStateOf("") }
  var newCategoryInput by remember { mutableStateOf("") }
  var showCategoryManager by remember { mutableStateOf(false) }
  val filtered =
    remember(sounds, query) {
      val q = query.trim().lowercase()
      if (q.isEmpty()) sounds
      else sounds.filter { it.title.lowercase().contains(q) || it.category.lowercase().contains(q) }
    }

  Box(modifier = Modifier.fillMaxSize().background(AdminBg)) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("Search public sounds (${sounds.size})...") },
        singleLine = true,
        colors = adminFieldColors(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp),
      )

      // Category Manager Card (All & Trending fixed; rest addable/deletable)
      AdminClayCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        cornerRadius = 18.dp,
        elevation = 8.dp,
        baseColor = AdminSurface,
        borderColor = AdminGold.copy(alpha = 0.45f),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth().clickable { showCategoryManager = !showCategoryManager },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Categories (${managedCategories.size} editable • All & Trending fixed)",
              color = AdminGold,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = if (showCategoryManager) "Hide" else "Manage",
              color = AdminGold,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
            )
          }

          if (showCategoryManager) {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              listOf("All (Fixed)", "Trending (Most ❤️ • Fixed)").forEach { fixedLabel ->
                AdminClayPill(
                  cornerRadius = 10.dp,
                  baseColor = AdminSurfaceElevated,
                  borderColor = AdminBorder,
                  elevation = 3.dp,
                ) {
                  Text(
                    text = fixedLabel,
                    color = AdminTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                  )
                }
              }
              managedCategories.forEach { cat ->
                AdminClayPill(
                  cornerRadius = 10.dp,
                  baseColor = AdminSurfaceElevated,
                  borderColor = AdminGold.copy(alpha = 0.65f),
                  elevation = 4.dp,
                ) {
                  Row(
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Text(cat, color = AdminGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Icon(
                      imageVector = Icons.Filled.Close,
                      contentDescription = "Delete $cat",
                      tint = AdminDanger,
                      modifier =
                        Modifier.size(16.dp).clickable {
                          onRemoveCategory(cat)
                        },
                    )
                  }
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedTextField(
                value = newCategoryInput,
                onValueChange = { newCategoryInput = it },
                placeholder = { Text("New category name...") },
                singleLine = true,
                colors = adminFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
              )
              AdminClayButton(
                onClick = {
                  onAddCategory(newCategoryInput)
                  newCategoryInput = ""
                },
                enabled = newCategoryInput.isNotBlank(),
                containerColor = AdminGold,
                contentColor = Color.Black,
                cornerRadius = 14.dp,
                elevation = 6.dp,
                modifier = Modifier.height(50.dp),
              ) {
                Text("+ ADD", fontWeight = FontWeight.ExtraBold)
              }
            }
          }
        }
      }

      LazyColumn(
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(filtered, key = { it.id }) { sound ->
          val isPlaying = playingId == sound.id
          AdminClayCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            elevation = if (isPlaying) 12.dp else 8.dp,
            baseColor = AdminSurface,
            borderColor = if (isPlaying) AdminGold else AdminBorder,
            borderWidth = if (isPlaying) 1.5.dp else 1.dp,
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier =
                  Modifier.size(54.dp)
                    .adminClaySurface(baseColor = AdminSurfaceElevated, cornerRadius = 14.dp, elevation = 4.dp),
                contentAlignment = Alignment.Center,
              ) {
                if (sound.thumbnailUrl.isNotBlank()) {
                  AsyncImage(
                    model = sound.thumbnailUrl,
                    contentDescription = sound.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                  )
                } else {
                  Text(sound.emoji, fontSize = 24.sp)
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = sound.title,
                  color = AdminTextPrimary,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${sound.category} • Vol ${sound.defaultVolume}% • ♥ ${sound.likes}",
                  color = AdminTextSecondary,
                  style = MaterialTheme.typography.bodySmall,
                )
              }
              IconButton(onClick = { onPlayToggle(sound) }) {
                Icon(
                  imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                  contentDescription = "Play/Stop",
                  tint = if (isPlaying) AdminDanger else AdminSuccess,
                )
              }
              IconButton(onClick = { onEditClick(sound) }) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = AdminGold)
              }
              IconButton(onClick = { onDeleteClick(sound) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = AdminDanger)
              }
            }
          }
        }
      }
    }

    AdminClayPill(
      onClick = onAddClick,
      baseColor = AdminGold,
      borderColor = Color.White.copy(alpha = 0.45f),
      cornerRadius = 28.dp,
      elevation = 12.dp,
      modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp).size(56.dp),
    ) {
      Icon(Icons.Filled.Add, contentDescription = "Add Public Sound", tint = Color.Black)
    }
  }
}

@Composable
private fun AdminSubTabRow(
  tabs: List<String>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .adminClaySurface(
          baseColor = AdminSurface,
          cornerRadius = 18.dp,
          elevation = 7.dp,
          accentBorderColor = AdminBorder.copy(alpha = 0.8f),
        )
        .padding(5.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    tabs.forEachIndexed { index, title ->
      val selected = index == selectedIndex
      AdminClayPill(
        onClick = { onSelect(index) },
        baseColor = if (selected) AdminGold else AdminSurfaceElevated.copy(alpha = 0.7f),
        borderColor = if (selected) Color.White.copy(alpha = 0.35f) else AdminBorder.copy(alpha = 0.5f),
        cornerRadius = 13.dp,
        elevation = if (selected) 6.dp else 2.dp,
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = title,
          color = if (selected) Color.Black else AdminTextSecondary,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
        )
      }
    }
  }
}

@Composable
fun AdminNoticeTab(
  initial: AdminNotice,
  maintenanceInitial: AdminMaintenanceConfig,
  updateInitial: AdminAppUpdateConfig,
  onSave: (AdminNotice) -> Unit,
  onSaveMaintenance: (AdminMaintenanceConfig) -> Unit,
  onSaveUpdateConfig: (AdminAppUpdateConfig) -> Unit,
) {
  var subTab by rememberSaveable { mutableIntStateOf(0) }

  var bannerActive by remember(initial) { mutableStateOf(initial.bannerActive) }
  var bannerText by remember(initial) { mutableStateOf(initial.bannerText) }
  var popupActive by remember(initial) { mutableStateOf(initial.popupActive) }
  var popupTitle by remember(initial) { mutableStateOf(initial.popupTitle) }
  var popupMessage by remember(initial) { mutableStateOf(initial.popupMessage) }

  var maintEnabled by remember(maintenanceInitial) { mutableStateOf(maintenanceInitial.enabled) }
  var maintTitle by remember(maintenanceInitial) { mutableStateOf(maintenanceInitial.title) }
  var maintMessage by remember(maintenanceInitial) { mutableStateOf(maintenanceInitial.message) }

  var updEnabled by remember(updateInitial) { mutableStateOf(updateInitial.enabled) }
  var updForce by remember(updateInitial) { mutableStateOf(updateInitial.forceUpdate) }
  var updVersionName by remember(updateInitial) { mutableStateOf(updateInitial.latestVersionName) }
  var updVersionCode by remember(updateInitial) { mutableStateOf(updateInitial.latestVersionCode.toString()) }
  var updTargetVersion by remember(updateInitial) { mutableStateOf(updateInitial.targetVersionName) }
  var updTitle by remember(updateInitial) { mutableStateOf(updateInitial.title) }
  var updMessage by remember(updateInitial) { mutableStateOf(updateInitial.message) }
  var updUrl by remember(updateInitial) { mutableStateOf(updateInitial.downloadUrl) }

  Column(
    modifier = Modifier.fillMaxSize().background(AdminBg).padding(horizontal = 16.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    AdminSubTabRow(
      tabs = listOf("নোটিশ ও পপআপ", "মেইনটেন্যান্স", "অ্যাপ আপডেট"),
      selectedIndex = subTab,
      onSelect = { subTab = it },
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      when (subTab) {
        0 -> {
          AdminClayCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            elevation = 9.dp,
            baseColor = AdminSurface,
            borderColor = AdminBorder,
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("Home Banner Notice", color = AdminGold, fontWeight = FontWeight.Bold)
                Switch(
                  checked = bannerActive,
                  onCheckedChange = { bannerActive = it },
                  colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = AdminGold),
                )
              }
              OutlinedTextField(
                value = bannerText,
                onValueChange = { bannerText = it },
                label = { Text("Banner Notice Text") },
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }

          AdminClayCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            elevation = 9.dp,
            baseColor = AdminSurface,
            borderColor = AdminBorder,
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("Startup Popup Dialog", color = AdminGold, fontWeight = FontWeight.Bold)
                Switch(
                  checked = popupActive,
                  onCheckedChange = { popupActive = it },
                  colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = AdminGold),
                )
              }
              OutlinedTextField(
                value = popupTitle,
                onValueChange = { popupTitle = it },
                label = { Text("Popup Title") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = popupMessage,
                onValueChange = { popupMessage = it },
                label = { Text("Popup Message") },
                minLines = 3,
                shape = RoundedCornerShape(14.dp),
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }

          AdminClayButton(
            onClick = { onSave(AdminNotice(bannerActive, bannerText, popupActive, popupTitle, popupMessage)) },
            containerColor = AdminGold,
            contentColor = Color.Black,
            cornerRadius = 18.dp,
            elevation = 10.dp,
            modifier = Modifier.fillMaxWidth().height(50.dp),
          ) {
            Text("PUBLISH NOTICE LIVE", fontWeight = FontWeight.ExtraBold)
          }
        }

        1 -> {
          AdminClayCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            elevation = 9.dp,
            baseColor = AdminSurface,
            borderColor = if (maintEnabled) AdminDanger else AdminBorder,
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    "🛠️ Maintenance Mode (কাজ চলতেছে)",
                    color = if (maintEnabled) AdminDanger else AdminGold,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    "অন করলে ইউজার অ্যাপ ব্লক হয়ে কাজের মেসেজ দেখাবে",
                    color = AdminTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                  )
                }
                Switch(
                  checked = maintEnabled,
                  onCheckedChange = { maintEnabled = it },
                  colors =
                    SwitchDefaults.colors(
                      checkedThumbColor = Color.White,
                      checkedTrackColor = AdminDanger,
                    ),
                )
              }
              OutlinedTextField(
                value = maintTitle,
                onValueChange = { maintTitle = it },
                label = { Text("Maintenance Title") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = maintMessage,
                onValueChange = { maintMessage = it },
                label = { Text("Maintenance Message") },
                minLines = 3,
                shape = RoundedCornerShape(14.dp),
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth(),
              )
              AdminClayButton(
                onClick = { onSaveMaintenance(AdminMaintenanceConfig(maintEnabled, maintTitle, maintMessage)) },
                containerColor = if (maintEnabled) AdminDanger else AdminSurfaceElevated,
                contentColor = if (maintEnabled) Color.White else AdminGold,
                borderColor = if (maintEnabled) AdminDanger else AdminGold.copy(alpha = 0.7f),
                cornerRadius = 16.dp,
                elevation = 8.dp,
                modifier = Modifier.fillMaxWidth().height(50.dp),
              ) {
                Text("SAVE MAINTENANCE MODE", fontWeight = FontWeight.ExtraBold)
              }
            }
          }
        }

        else -> {
          AdminClayCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            elevation = 9.dp,
            baseColor = AdminSurface,
            borderColor = AdminGold.copy(alpha = 0.55f),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("🚀 App Update Popup", color = AdminGold, fontWeight = FontWeight.Bold)
                  Text(
                    "নির্দিষ্ট ভার্সন ও তার নিচের সব ভার্সনে আপডেট মেসেজ পাঠান",
                    color = AdminTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                  )
                }
                Switch(
                  checked = updEnabled,
                  onCheckedChange = { updEnabled = it },
                  colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = AdminGold),
                )
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("Force Update (বাধ্যতামূলক)", color = AdminTextPrimary)
                Switch(
                  checked = updForce,
                  onCheckedChange = { updForce = it },
                  colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = AdminGold),
                )
              }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                  value = updVersionName,
                  onValueChange = { updVersionName = it },
                  label = { Text("বর্তমান ভার্সন (যেমন 2.4)") },
                  singleLine = true,
                  shape = RoundedCornerShape(14.dp),
                  colors = adminFieldColors(),
                  modifier = Modifier.weight(1.3f),
                )
                OutlinedTextField(
                  value = updVersionCode,
                  onValueChange = { v -> updVersionCode = v.filter { it.isDigit() } },
                  label = { Text("Code (24)") },
                  singleLine = true,
                  shape = RoundedCornerShape(14.dp),
                  colors = adminFieldColors(),
                  modifier = Modifier.weight(0.7f),
                )
              }
              OutlinedTextField(
                value = updTargetVersion,
                onValueChange = { updTargetVersion = it },
                label = { Text("কত ভার্সন ও তার নিচে মেসেজ যাবে (যেমন 2.1)") },
                placeholder = { Text("যেমন 2.1 (খালি রাখলে বর্তমান ভার্সনের নিচে যাবে)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth(),
              )
              AdminClayPill(
                cornerRadius = 12.dp,
                baseColor = AdminSurfaceElevated,
                borderColor = AdminGold.copy(alpha = 0.4f),
                elevation = 3.dp,
                modifier = Modifier.fillMaxWidth(),
              ) {
                val targetTrim = updTargetVersion.trim()
                val previewMsg =
                  if (targetTrim.isNotBlank()) {
                    "টার্গেট: v$targetTrim ও তার নিচের সব ভার্সনে (যেমন $targetTrim, 2.0, 1.9, 1.8...) আপডেট মেসেজ যাবে। ইউজারের পপআপে বর্তমান ভার্সন v${updVersionName.trim().ifBlank { "2.4" }} এবং আপনার ভার্সন দেখাবে।"
                  } else {
                    "টার্গেট: খালি রাখা হয়েছে — বর্তমান ভার্সন (v${updVersionName.trim().ifBlank { "2.4" }}) এর নিচের সব ভার্সনে মেসেজ যাবে।"
                  }
                Text(
                  text = previewMsg,
                  color = AdminTextSecondary,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
              }
              OutlinedTextField(
                value = updTitle,
                onValueChange = { updTitle = it },
                label = { Text("Update Popup Title") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = updMessage,
                onValueChange = { updMessage = it },
                label = { Text("What's New / Release Notes") },
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = updUrl,
                onValueChange = { updUrl = it },
                label = { Text("APK Download URL (https://...)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth(),
              )
              AdminClayButton(
                onClick = {
                  onSaveUpdateConfig(
                    AdminAppUpdateConfig(
                      enabled = updEnabled,
                      latestVersionName = updVersionName.trim().ifBlank { "2.4" },
                      latestVersionCode = updVersionCode.toIntOrNull() ?: 24,
                      targetVersionName = updTargetVersion.trim(),
                      forceUpdate = updForce,
                      title = updTitle,
                      message = updMessage,
                      downloadUrl = updUrl,
                    )
                  )
                },
                containerColor = AdminGold,
                contentColor = Color.Black,
                cornerRadius = 16.dp,
                elevation = 8.dp,
                modifier = Modifier.fillMaxWidth().height(50.dp),
              ) {
                Text("SAVE UPDATE POPUP", fontWeight = FontWeight.ExtraBold)
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
fun AdminBrandingTab(
  appConfig: AdminAppConfig,
  devConfig: AdminDeveloperConfig,
  ttsVoices: List<AdminTtsVoice>,
  onSaveAppBranding: (brandName: String, logoUrl: String, pickedUri: Uri?) -> Unit,
  onSaveDeveloper: (AdminDeveloperConfig, pickedLogoUri: Uri?) -> Unit,
  onSaveTtsVoice: (AdminTtsVoice, pickedAvatarUri: Uri?) -> Unit,
  onDeleteTtsVoice: (AdminTtsVoice) -> Unit,
) {
  var subTab by rememberSaveable { mutableIntStateOf(0) }

  var brandName by remember(appConfig) { mutableStateOf(appConfig.floatingBrandName) }
  var appLogoUrl by remember(appConfig) { mutableStateOf(appConfig.logoUrl) }
  var pickedAppLogoUri by remember { mutableStateOf<Uri?>(null) }

  var devName by remember(devConfig) { mutableStateOf(devConfig.name) }
  var devBio by remember(devConfig) { mutableStateOf(devConfig.bio) }
  var devLogoUrl by remember(devConfig) { mutableStateOf(devConfig.logoUrl) }
  var pickedDevLogoUri by remember { mutableStateOf<Uri?>(null) }
  var buttons by remember(devConfig) { mutableStateOf(devConfig.buttons) }

  var editingVoice by remember { mutableStateOf<AdminTtsVoice?>(null) }
  var pickedVoiceAvatarUri by remember { mutableStateOf<Uri?>(null) }

  val appLogoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) pickedAppLogoUri = uri
    }
  val devLogoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) pickedDevLogoUri = uri
    }
  val voiceAvatarPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) pickedVoiceAvatarUri = uri
    }

  Column(
    modifier = Modifier.fillMaxSize().background(AdminBg).padding(horizontal = 16.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    AdminSubTabRow(
      tabs = listOf("অ্যাপ লোগো", "ডেভেলপার", "TTS ভয়েস (${ttsVoices.size})"),
      selectedIndex = subTab,
      onSelect = { subTab = it },
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      when (subTab) {
        0 -> {
          AdminClayCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            elevation = 9.dp,
            baseColor = AdminSurface,
            borderColor = AdminGold.copy(alpha = 0.5f),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("Floating Window Logo & Brand", color = AdminGold, fontWeight = FontWeight.Bold)
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                  modifier =
                    Modifier.size(56.dp)
                      .adminClaySurface(baseColor = AdminSurfaceElevated, cornerRadius = 28.dp, elevation = 5.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  val preview = pickedAppLogoUri ?: appLogoUrl.takeIf { it.isNotBlank() }
                  if (preview != null) {
                    AsyncImage(
                      model = preview,
                      contentDescription = "App Logo",
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                  } else {
                    Icon(Icons.Filled.Image, contentDescription = null, tint = AdminGold)
                  }
                }
                AdminClayButton(
                  onClick = {
                    appLogoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                  },
                  containerColor = AdminSurfaceElevated,
                  contentColor = AdminGold,
                  borderColor = AdminGold.copy(alpha = 0.7f),
                  cornerRadius = 14.dp,
                  elevation = 6.dp,
                  modifier = Modifier.weight(1f),
                ) {
                  Text("PICK FLOATING LOGO", fontWeight = FontWeight.Bold)
                }
              }
              OutlinedTextField(
                value = brandName,
                onValueChange = { brandName = it },
                label = { Text("Floating Brand Title (e.g. ZONEX)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = appLogoUrl,
                onValueChange = { appLogoUrl = it },
                label = { Text("Or Paste App Logo URL") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth(),
              )
              AdminClayButton(
                onClick = { onSaveAppBranding(brandName, appLogoUrl, pickedAppLogoUri) },
                containerColor = AdminGold,
                contentColor = Color.Black,
                cornerRadius = 16.dp,
                elevation = 8.dp,
                modifier = Modifier.fillMaxWidth().height(48.dp),
              ) {
                Text("SAVE FLOATING LOGO", fontWeight = FontWeight.ExtraBold)
              }
            }
          }
        }

        1 -> {
          AdminClayCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            elevation = 9.dp,
            baseColor = AdminSurface,
            borderColor = AdminBorder,
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("Developer Profile & Action Buttons", color = AdminGold, fontWeight = FontWeight.Bold)
              AdminClayButton(
                onClick = {
                  devLogoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                containerColor = AdminSurfaceElevated,
                contentColor = AdminTextPrimary,
                borderColor = AdminBorder,
                cornerRadius = 14.dp,
                elevation = 5.dp,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (pickedDevLogoUri != null) "Dev Logo Selected" else "Pick Developer Avatar")
              }
              OutlinedTextField(
                value = devName,
                onValueChange = { devName = it },
                label = { Text("Developer / Brand Name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = devLogoUrl,
                onValueChange = { devLogoUrl = it },
                label = { Text("Developer Logo URL") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = devBio,
                onValueChange = { devBio = it },
                label = { Text("Bio / About Text") },
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth(),
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("Action Buttons (${buttons.size})", color = AdminTextSecondary)
                AdminClayButton(
                  onClick = { buttons = buttons + AdminDevButton("Join Telegram", "https://t.me/", "#0EA5E9") },
                  containerColor = AdminSurfaceElevated,
                  contentColor = AdminGold,
                  borderColor = AdminGold.copy(alpha = 0.7f),
                  cornerRadius = 12.dp,
                  elevation = 4.dp,
                ) {
                  Text("+ Add Button", color = AdminGold, fontWeight = FontWeight.Bold)
                }
              }

              buttons.forEachIndexed { idx, btn ->
                AdminClayCard(
                  cornerRadius = 16.dp,
                  elevation = 6.dp,
                  baseColor = AdminSurfaceElevated,
                  borderColor = AdminBorder,
                ) {
                  Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Text("Button #${idx + 1}", color = AdminGold, style = MaterialTheme.typography.labelMedium)
                      IconButton(
                        onClick = { buttons = buttons.toMutableList().also { it.removeAt(idx) } },
                        modifier = Modifier.size(28.dp),
                      ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = AdminDanger, modifier = Modifier.size(16.dp))
                      }
                    }
                    OutlinedTextField(
                      value = btn.label,
                      onValueChange = { newVal ->
                        buttons = buttons.toMutableList().also { it[idx] = btn.copy(label = newVal) }
                      },
                      label = { Text("Label") },
                      singleLine = true,
                      colors = adminFieldColors(),
                      modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                      value = btn.url,
                      onValueChange = { newVal ->
                        buttons = buttons.toMutableList().also { it[idx] = btn.copy(url = newVal) }
                      },
                      label = { Text("Link URL (https://...)") },
                      singleLine = true,
                      colors = adminFieldColors(),
                      modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                      value = btn.color,
                      onValueChange = { newVal ->
                        buttons = buttons.toMutableList().also { it[idx] = btn.copy(color = newVal) }
                      },
                      label = { Text("Hex Color (#F5B82E)") },
                      singleLine = true,
                      colors = adminFieldColors(),
                      modifier = Modifier.fillMaxWidth(),
                    )
                  }
                }
              }

              AdminClayButton(
                onClick = {
                  onSaveDeveloper(
                    AdminDeveloperConfig(devName, devLogoUrl, devBio, buttons),
                    pickedDevLogoUri,
                  )
                },
                containerColor = AdminGold,
                contentColor = Color.Black,
                cornerRadius = 16.dp,
                elevation = 9.dp,
                modifier = Modifier.fillMaxWidth().height(48.dp),
              ) {
                Text("SAVE DEVELOPER PROFILE", fontWeight = FontWeight.ExtraBold)
              }
            }
          }
        }

        else -> {
          AdminClayCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            elevation = 9.dp,
            baseColor = AdminSurface,
            borderColor = AdminGold.copy(alpha = 0.5f),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    "🗣️ TTS Character Voices (${ttsVoices.size})",
                    color = AdminGold,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    "Cartesia AI চরিত্রের নাম, Voice ID ও প্রোফাইল ছবি সেট করুন",
                    color = AdminTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                  )
                }
                AdminClayButton(
                  onClick = {
                    pickedVoiceAvatarUri = null
                    editingVoice = AdminTtsVoice(sortOrder = ttsVoices.size)
                  },
                  containerColor = AdminSurfaceElevated,
                  contentColor = AdminGold,
                  borderColor = AdminGold.copy(alpha = 0.7f),
                  cornerRadius = 12.dp,
                  elevation = 4.dp,
                ) {
                  Text("+ Add Voice", fontWeight = FontWeight.Bold)
                }
              }

              editingVoice?.let { draft ->
                AdminClayCard(
                  cornerRadius = 16.dp,
                  elevation = 6.dp,
                  baseColor = AdminSurfaceElevated,
                  borderColor = AdminGold.copy(alpha = 0.7f),
                ) {
                  Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                      if (draft.id.isBlank()) "New TTS Character" else "Edit TTS Character",
                      color = AdminGold,
                      fontWeight = FontWeight.Bold,
                    )
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Box(
                        modifier =
                          Modifier.size(48.dp)
                            .adminClaySurface(baseColor = AdminSurface, cornerRadius = 24.dp, elevation = 4.dp),
                        contentAlignment = Alignment.Center,
                      ) {
                        val preview = pickedVoiceAvatarUri ?: draft.avatarUrl.takeIf { it.isNotBlank() }
                        if (preview != null) {
                          AsyncImage(
                            model = preview,
                            contentDescription = draft.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                          )
                        } else {
                          Icon(Icons.Filled.Image, contentDescription = null, tint = AdminGold)
                        }
                      }
                      AdminClayButton(
                        onClick = {
                          voiceAvatarPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                          )
                        },
                        containerColor = AdminSurface,
                        contentColor = AdminGold,
                        borderColor = AdminGold.copy(alpha = 0.6f),
                        cornerRadius = 12.dp,
                        elevation = 4.dp,
                        modifier = Modifier.weight(1f),
                      ) {
                        Text(if (pickedVoiceAvatarUri != null) "Avatar Selected" else "Pick Avatar Photo")
                      }
                    }
                    OutlinedTextField(
                      value = draft.name,
                      onValueChange = { editingVoice = draft.copy(name = it) },
                      label = { Text("Character Name (e.g. Omor on Fire)") },
                      singleLine = true,
                      colors = adminFieldColors(),
                      modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                      value = draft.cartesiaVoiceId,
                      onValueChange = { editingVoice = draft.copy(cartesiaVoiceId = it) },
                      label = { Text("Cartesia Voice ID") },
                      singleLine = true,
                      colors = adminFieldColors(),
                      modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      OutlinedTextField(
                        value = draft.language,
                        onValueChange = { editingVoice = draft.copy(language = it) },
                        label = { Text("Language") },
                        singleLine = true,
                        colors = adminFieldColors(),
                        modifier = Modifier.weight(1f),
                      )
                      OutlinedTextField(
                        value = draft.sortOrder.toString(),
                        onValueChange = { v ->
                          editingVoice = draft.copy(sortOrder = v.filter { it.isDigit() }.toIntOrNull() ?: 0)
                        },
                        label = { Text("Order") },
                        singleLine = true,
                        colors = adminFieldColors(),
                        modifier = Modifier.width(84.dp),
                      )
                    }
                    OutlinedTextField(
                      value = draft.avatarUrl,
                      onValueChange = { editingVoice = draft.copy(avatarUrl = it) },
                      label = { Text("Or Paste Avatar URL") },
                      singleLine = true,
                      colors = adminFieldColors(),
                      modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      AdminClayButton(
                        onClick = {
                          editingVoice = null
                          pickedVoiceAvatarUri = null
                        },
                        containerColor = AdminSurface,
                        contentColor = AdminTextSecondary,
                        borderColor = AdminBorder,
                        cornerRadius = 12.dp,
                        elevation = 4.dp,
                        modifier = Modifier.weight(1f),
                      ) {
                        Text("Cancel")
                      }
                      AdminClayButton(
                        onClick = {
                          val v = editingVoice
                          val uri = pickedVoiceAvatarUri
                          if (v != null) onSaveTtsVoice(v, uri)
                          editingVoice = null
                          pickedVoiceAvatarUri = null
                        },
                        containerColor = AdminGold,
                        contentColor = Color.Black,
                        cornerRadius = 12.dp,
                        elevation = 6.dp,
                        modifier = Modifier.weight(1f),
                      ) {
                        Text("SAVE VOICE", fontWeight = FontWeight.ExtraBold)
                      }
                    }
                  }
                }
              }

              if (ttsVoices.isEmpty()) {
                Text(
                  text = "এখনও কোনো কাস্টম ক্যারেক্টার যোগ করা হয়নি — '+ Add Voice' ট্যাপ করুন।",
                  color = AdminTextSecondary,
                  style = MaterialTheme.typography.bodySmall,
                )
              } else {
                ttsVoices.forEach { voice ->
                  AdminClayCard(
                    cornerRadius = 14.dp,
                    elevation = 5.dp,
                    baseColor = AdminSurfaceElevated,
                    borderColor = AdminBorder,
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Box(
                        modifier =
                          Modifier.size(40.dp)
                            .clip(CircleShape)
                            .background(AdminSurface),
                        contentAlignment = Alignment.Center,
                      ) {
                        if (voice.avatarUrl.isNotBlank()) {
                          AsyncImage(
                            model = voice.avatarUrl,
                            contentDescription = voice.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                          )
                        } else {
                          Text("🗣️", fontSize = 18.sp)
                        }
                      }
                      Spacer(modifier = Modifier.width(10.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Text(voice.name, color = AdminTextPrimary, fontWeight = FontWeight.Bold)
                        Text(
                          "${voice.language} • ID: ${voice.cartesiaVoiceId.take(12)}...",
                          color = AdminTextSecondary,
                          style = MaterialTheme.typography.labelSmall,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                      IconButton(
                        onClick = {
                          pickedVoiceAvatarUri = null
                          editingVoice = voice
                        },
                      ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit voice", tint = AdminGold)
                      }
                      IconButton(onClick = { onDeleteTtsVoice(voice) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete voice", tint = AdminDanger)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
fun AdminUsersTab(
  users: List<AdminUserRecord>,
  playingId: String?,
  onPlayUserSound: (AdminUserSoundItem) -> Unit,
  onDeleteUserSound: (uid: String, item: AdminUserSoundItem) -> Unit,
) {
  val totalAddedAllUsers = remember(users) { users.sumOf { it.totalAddedCount } }
  var expandedUids by remember { mutableStateOf<Set<String>>(emptySet()) }

  LazyColumn(
    modifier = Modifier.fillMaxSize().background(AdminBg),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Users & Uploaded Audio (${users.size} users)",
          style = MaterialTheme.typography.titleMedium,
          color = AdminGold,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Total User Audio Added: $totalAddedAllUsers • Tap any user to view/hide their audio",
          style = MaterialTheme.typography.bodySmall,
          color = AdminTextSecondary,
        )
      }
    }
    items(users, key = { it.uid }) { user ->
      val isExpanded = expandedUids.contains(user.uid)
      AdminClayCard(
        onClick = {
          expandedUids = if (isExpanded) expandedUids - user.uid else expandedUids + user.uid
        },
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        elevation = if (isExpanded) 11.dp else 8.dp,
        baseColor = AdminSurface,
        borderColor =
          when {
            isExpanded -> AdminGold
            user.totalAddedCount > 0 -> AdminGold.copy(alpha = 0.45f)
            else -> AdminBorder
          },
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              val headerTitle =
                when {
                  user.displayName.isNotBlank() -> user.displayName + if (user.isAdminAccount) " (Admin)" else ""
                  user.email.isNotBlank() -> user.email + if (user.isAdminAccount) " (Admin)" else ""
                  else -> "UID: ${user.uid.take(14)}..." + if (user.isAdminAccount) " (Admin)" else ""
                }
              Text(
                text = headerTitle,
                color = AdminTextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (user.email.isNotBlank() && user.displayName.isNotBlank()) {
                Text(
                  text = user.email,
                  color = AdminTextSecondary,
                  style = MaterialTheme.typography.labelSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Text(
                text = "UID: ${user.uid}",
                color = AdminTextSecondary.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              AdminClayPill(
                cornerRadius = 10.dp,
                baseColor = AdminSurfaceElevated,
                borderColor = AdminGold.copy(alpha = 0.6f),
                elevation = 4.dp,
              ) {
                Text(
                  text = "${user.totalAddedCount} Audio",
                  color = AdminGold,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.ExtraBold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
              }
              Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) "Hide sounds" else "Show sounds",
                tint = AdminGold,
              )
            }
          }

          Text(
            text = "Public: ${user.publicUploadCount} • Private: ${user.privateCount} • Downloads: ${user.downloadCount} • Favs: ${user.favoriteCount}",
            color = AdminTextSecondary,
            style = MaterialTheme.typography.bodySmall,
          )

          if (isExpanded) {
            if (user.uploadedSounds.isEmpty()) {
              Text(
                text = "No audio/video uploaded by this user yet.",
                color = AdminTextSecondary.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
              )
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                user.uploadedSounds.forEach { item ->
                  val isPlaying = playingId == "user_${item.id}"
                  AdminClayCard(
                    cornerRadius = 14.dp,
                    elevation = 5.dp,
                    baseColor = AdminSurfaceElevated,
                    borderColor = if (isPlaying) AdminGold else AdminBorder,
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      AdminClayPill(
                        cornerRadius = 8.dp,
                        baseColor =
                          if (item.isPublic) AdminGold.copy(alpha = 0.22f)
                          else Color(0xFF0EA5E9).copy(alpha = 0.22f),
                        borderColor = null,
                        elevation = 2.dp,
                      ) {
                        Text(
                          text = if (item.isPublic) "PUBLIC" else "PRIVATE",
                          color = if (item.isPublic) AdminGold else Color(0xFF38BDF8),
                          fontSize = 9.sp,
                          fontWeight = FontWeight.ExtraBold,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                      }
                      Spacer(modifier = Modifier.width(8.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = item.title,
                          color = AdminTextPrimary,
                          style = MaterialTheme.typography.bodySmall,
                          fontWeight = FontWeight.SemiBold,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        if (item.category.isNotBlank()) {
                          Text(
                            text = item.category,
                            color = AdminTextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                          )
                        }
                      }
                      IconButton(
                        onClick = { onPlayUserSound(item) },
                        modifier = Modifier.size(32.dp),
                      ) {
                        Icon(
                          imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                          contentDescription = "Preview",
                          tint = if (isPlaying) AdminDanger else AdminSuccess,
                          modifier = Modifier.size(18.dp),
                        )
                      }
                      IconButton(
                        onClick = { onDeleteUserSound(user.uid, item) },
                        modifier = Modifier.size(32.dp),
                      ) {
                        Icon(
                          imageVector = Icons.Filled.Delete,
                          contentDescription = "Delete User Audio",
                          tint = AdminDanger,
                          modifier = Modifier.size(18.dp),
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSoundEditorDialog(
  draft: AdminSoundDraft,
  managedCategories: List<String>,
  isSaving: Boolean,
  progress: Float,
  onUpdateDraft: ((AdminSoundDraft) -> AdminSoundDraft) -> Unit,
  onDismiss: () -> Unit,
  onSave: () -> Unit,
) {
  val context = LocalContext.current
  val mediaPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        val name =
          runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
              val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
              if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
            }
          }.getOrNull() ?: "audio.mp3"
        onUpdateDraft {
          it.copy(
            audioUri = uri,
            audioFileName = name,
            useDirectUrl = false,
            title = it.title.ifBlank { name.substringBeforeLast(".") },
          )
        }
      }
    }
  val thumbPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) onUpdateDraft { it.copy(thumbnailUri = uri) }
    }

  val categories =
    remember(managedCategories, draft.category) {
      val list = (managedCategories + draft.category).filter {
        it.isNotBlank() && !it.equals("All", ignoreCase = true) && !it.equals("Trending", ignoreCase = true)
      }.distinct()
      list.ifEmpty { listOf("General") }
    }
  val emojis = listOf("🔥", "😂", "💀", "⚡", "🎮", "🔊", "👑", "💥")

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    Box(
      modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp),
      contentAlignment = Alignment.Center,
    ) {
      AdminClayCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        elevation = 16.dp,
        baseColor = AdminSurfaceElevated,
        borderColor = AdminGold.copy(alpha = 0.8f),
        borderWidth = 1.5.dp,
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = if (draft.editingId == null) "Add Public Sound" else "Edit Public Sound",
              style = MaterialTheme.typography.titleLarge,
              color = AdminGold,
              fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onDismiss, enabled = !isSaving) {
              Icon(Icons.Filled.Close, contentDescription = "Close", tint = AdminTextSecondary)
            }
          }

          Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { mediaPicker.launch(arrayOf("audio/*", "video/*")) },
                border = BorderStroke(1.dp, AdminGold),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminGold),
                modifier = Modifier.weight(1f),
              ) {
                Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AUDIO / VIDEO")
              }
              FilterChip(
                selected = draft.useDirectUrl,
                onClick = { onUpdateDraft { it.copy(useDirectUrl = !it.useDirectUrl) } },
                label = { Text("Direct URL") },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AdminGold,
                    selectedLabelColor = Color.Black,
                  ),
              )
            }

            if (!draft.useDirectUrl && draft.audioFileName.isNotBlank()) {
              Text("Selected: ${draft.audioFileName}", color = AdminSuccess, style = MaterialTheme.typography.bodySmall)
            }

            if (draft.useDirectUrl) {
              OutlinedTextField(
                value = draft.directAudioUrl,
                onValueChange = { v -> onUpdateDraft { it.copy(directAudioUrl = v) } },
                label = { Text("Direct Audio URL") },
                singleLine = true,
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth(),
              )
            }

            OutlinedButton(
              onClick = {
                thumbPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              },
              border = BorderStroke(1.dp, AdminBorder),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminTextPrimary),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                if (draft.thumbnailUri != null || draft.existingThumbnailUrl.isNotBlank())
                  "CHANGE THUMBNAIL"
                else "+ ADD THUMBNAIL",
              )
            }

            OutlinedTextField(
              value = draft.title,
              onValueChange = { v -> onUpdateDraft { it.copy(title = v) } },
              label = { Text("Sound Name") },
              singleLine = true,
              colors = adminFieldColors(),
              modifier = Modifier.fillMaxWidth(),
            )

            Text("Category", color = AdminTextSecondary, style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              categories.forEach { cat ->
                FilterChip(
                  selected = draft.category == cat,
                  onClick = { onUpdateDraft { it.copy(category = cat) } },
                  label = { Text(cat) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = AdminGold,
                      selectedLabelColor = Color.Black,
                    ),
                )
              }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              emojis.forEach { em ->
                val sel = draft.emoji == em
                Box(
                  modifier =
                    Modifier.size(38.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(if (sel) AdminGold.copy(alpha = 0.25f) else AdminSurface)
                      .clickable { onUpdateDraft { it.copy(emoji = em) } },
                  contentAlignment = Alignment.Center,
                ) {
                  Text(em, fontSize = 18.sp)
                }
              }
            }

            Text("Default Volume: ${draft.defaultVolume}%", color = AdminGold)
            Slider(
              value = draft.defaultVolume / 100f,
              onValueChange = { v -> onUpdateDraft { it.copy(defaultVolume = (v * 100f).roundToInt()) } },
              colors = SliderDefaults.colors(thumbColor = AdminGold, activeTrackColor = AdminGold),
            )

            if (isSaving) {
              LinearProgressIndicator(
                progress = { progress },
                color = AdminGold,
                modifier = Modifier.fillMaxWidth(),
              )
            }

            AdminClayButton(
              onClick = onSave,
              enabled = !isSaving,
              containerColor = AdminGold,
              contentColor = Color.Black,
              cornerRadius = 16.dp,
              elevation = 9.dp,
              modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
              Text(if (isSaving) "SAVING..." else "SAVE PUBLIC SOUND", fontWeight = FontWeight.ExtraBold)
            }
          }
        }
      }
    }
  }
}

@Composable
fun AdminKeysTab(
  config: AdminKeyGateConfig,
  keys: List<AdminAccessKey>,
  onSaveConfig: (AdminKeyGateConfig) -> Unit,
  onGenerateKeys: (count: Int, durationHours: Int) -> Unit,
  onToggleRevoke: (AdminAccessKey) -> Unit,
  onResetBinding: (AdminAccessKey) -> Unit,
  onDeleteKey: (AdminAccessKey) -> Unit,
) {
  val clipboard = LocalClipboardManager.current
  var subTab by rememberSaveable { mutableIntStateOf(0) }
  var requireKey by remember(config) { mutableStateOf(config.requireKeyAfterLogin) }
  var getKeyUrl by remember(config) { mutableStateOf(config.getKeyUrl) }
  var socialLinks by remember(config) { mutableStateOf(config.socialLinks) }
  var durationHours by remember { mutableStateOf(24) }
  var keyQuery by remember { mutableStateOf("") }
  val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US) }

  val filteredKeys =
    remember(keys, keyQuery) {
      val q = keyQuery.trim().lowercase()
      if (q.isEmpty()) keys
      else keys.filter {
        it.code.lowercase().contains(q) || it.boundUid.lowercase().contains(q)
      }
    }

  Column(
    modifier = Modifier.fillMaxSize().background(AdminBg).padding(horizontal = 16.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    AdminSubTabRow(
      tabs = listOf("সব Key (${keys.size})", "Key তৈরি", "গেট ও লিংক"),
      selectedIndex = subTab,
      onSelect = { subTab = it },
    )

    when (subTab) {
      0 -> {
        OutlinedTextField(
          value = keyQuery,
          onValueChange = { keyQuery = it },
          placeholder = { Text("Search Key code or UID...") },
          singleLine = true,
          colors = adminFieldColors(),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          contentPadding = PaddingValues(bottom = 18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (filteredKeys.isEmpty()) {
            item {
              AdminClayCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                elevation = 6.dp,
                baseColor = AdminSurface,
                borderColor = AdminBorder,
              ) {
                Text(
                  text = "কোনো Key পাওয়া যায়নি। 'Key তৈরি' ট্যাব থেকে নতুন Key জেনারেট করুন।",
                  color = AdminTextSecondary,
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.padding(16.dp),
                )
              }
            }
          }
          items(filteredKeys, key = { it.code }) { key ->
            val now = System.currentTimeMillis()
            val effectiveExpiry =
              when {
                key.expiresAt > 0L -> key.expiresAt
                key.createdAt > 0L -> key.createdAt + key.durationHours.coerceAtLeast(1) * 3600_000L
                else -> 0L
              }
            val statusText =
              when {
                key.isRevoked -> "REVOKED"
                effectiveExpiry > 0L && now > effectiveExpiry -> "EXPIRED"
                effectiveExpiry > 0L -> {
                  val minsLeft = (effectiveExpiry - now) / 60_000L
                  val stateLabel = if (key.boundUid.isBlank()) "READY (UNBOUND)" else "ACTIVE"
                  "$stateLabel • ${minsLeft / 60}h ${minsLeft % 60}m left"
                }
                else -> "READY (${key.durationHours}h)"
              }
            val statusColor =
              when {
                key.isRevoked || (effectiveExpiry > 0L && now > effectiveExpiry) -> AdminDanger
                key.boundUid.isBlank() -> AdminGold
                else -> AdminSuccess
              }
            val expiryDateLabel =
              if (effectiveExpiry > 0L) "শেষ ডেট: ${dateFmt.format(Date(effectiveExpiry))}" else ""

            AdminClayCard(
              modifier = Modifier.fillMaxWidth(),
              cornerRadius = 18.dp,
              elevation = 8.dp,
              baseColor = AdminSurface,
              borderColor = AdminBorder,
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = key.code,
                    fontFamily = FontFamily.Monospace,
                    color = AdminTextPrimary,
                    fontWeight = FontWeight.Bold,
                  )
                  IconButton(
                    onClick = { clipboard.setText(AnnotatedString(key.code)) },
                    modifier = Modifier.size(30.dp),
                  ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = AdminGold, modifier = Modifier.size(16.dp))
                  }
                }
                Text(statusText, color = statusColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                if (expiryDateLabel.isNotBlank()) {
                  Text(
                    text = expiryDateLabel,
                    color = AdminTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                  )
                }
                if (key.boundUid.isNotBlank()) {
                  Text(
                    text = "Bound UID: ${key.boundUid.take(14)}... • Device: ${key.boundDeviceId.take(10)}...",
                    color = AdminTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                  )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  AdminClayButton(
                    onClick = { onResetBinding(key) },
                    containerColor = AdminSurfaceElevated,
                    contentColor = AdminGold,
                    borderColor = AdminGold.copy(alpha = 0.7f),
                    cornerRadius = 12.dp,
                    elevation = 4.dp,
                  ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                  }
                  AdminClayButton(
                    onClick = { onToggleRevoke(key) },
                    containerColor = AdminSurfaceElevated,
                    contentColor = AdminDanger,
                    borderColor = AdminDanger.copy(alpha = 0.7f),
                    cornerRadius = 12.dp,
                    elevation = 4.dp,
                  ) {
                    Text(if (key.isRevoked) "Unrevoke" else "Revoke")
                  }
                  IconButton(onClick = { onDeleteKey(key) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = AdminDanger)
                  }
                }
              }
            }
          }
        }
      }

      1 -> {
        Column(
          modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          AdminClayCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            elevation = 9.dp,
            baseColor = AdminSurface,
            borderColor = AdminGold.copy(alpha = 0.55f),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("Generate ZONEX-ZIXU Keys", color = AdminGold, fontWeight = FontWeight.Bold)
              Text(
                "Timer starts immediately on creation • 1 User/Device Lock",
                color = AdminTextSecondary,
                style = MaterialTheme.typography.bodySmall,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(24 to "24 Hours", 48 to "48 Hours", 168 to "7 Days").forEach { (hrs, label) ->
                  FilterChip(
                    selected = durationHours == hrs,
                    onClick = { durationHours = hrs },
                    label = { Text(label) },
                    colors =
                      FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AdminGold,
                        selectedLabelColor = Color.Black,
                      ),
                  )
                }
              }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminClayButton(
                  onClick = { onGenerateKeys(1, durationHours) },
                  containerColor = AdminGold,
                  contentColor = Color.Black,
                  cornerRadius = 16.dp,
                  elevation = 8.dp,
                  modifier = Modifier.weight(1f).height(48.dp),
                ) {
                  Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("GENERATE 1 KEY", fontWeight = FontWeight.ExtraBold)
                }
                AdminClayButton(
                  onClick = { onGenerateKeys(5, durationHours) },
                  containerColor = AdminSurfaceElevated,
                  contentColor = AdminGold,
                  borderColor = AdminGold.copy(alpha = 0.7f),
                  cornerRadius = 16.dp,
                  elevation = 6.dp,
                  modifier = Modifier.height(48.dp),
                ) {
                  Text("+5 KEYS", fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          keys.firstOrNull()?.let { latest ->
            AdminClayCard(
              modifier = Modifier.fillMaxWidth(),
              cornerRadius = 18.dp,
              elevation = 7.dp,
              baseColor = AdminSurface,
              borderColor = AdminBorder,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("সর্বশেষ তৈরি Key", color = AdminTextSecondary, style = MaterialTheme.typography.labelSmall)
                  Text(
                    latest.code,
                    color = AdminGold,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                  )
                }
                IconButton(onClick = { clipboard.setText(AnnotatedString(latest.code)) }) {
                  Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Latest Key", tint = AdminGold)
                }
              }
            }
          }
        }
      }

      else -> {
        Column(
          modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          AdminClayCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            elevation = 9.dp,
            baseColor = AdminSurface,
            borderColor = AdminGold.copy(alpha = 0.55f),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("API Key Gate & Website Link", color = AdminGold, fontWeight = FontWeight.Bold)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("Require Key After Login", color = AdminTextPrimary)
                Switch(
                  checked = requireKey,
                  onCheckedChange = { requireKey = it },
                  colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = AdminGold),
                )
              }
              OutlinedTextField(
                value = getKeyUrl,
                onValueChange = { getKeyUrl = it },
                label = { Text("Get Key Website URL (https://...)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth(),
              )

              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    "Social Links (${socialLinks.size}) • App & Website Sync",
                    color = AdminGold,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    "যতগুলো খুশি যোগ করুন — অ্যাপের নিচে ও ওয়েবসাইট দুই জায়গায়ই দেখা যাবে।",
                    color = AdminTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AdminClayButton(
                  onClick = {
                    val presets = listOf(
                      AdminKeySocialLink("Telegram", "Telegram", "https://t.me/", "#24A1DE"),
                      AdminKeySocialLink("YouTube", "YouTube", "https://youtube.com/", "#FF0000"),
                      AdminKeySocialLink("TikTok", "TikTok", "https://tiktok.com/", "#FE2C55"),
                      AdminKeySocialLink("Facebook", "Facebook", "https://facebook.com/", "#1877F2"),
                      AdminKeySocialLink("Instagram", "Instagram", "https://instagram.com/", "#E1306C"),
                      AdminKeySocialLink("Discord", "Discord", "https://discord.gg/", "#5865F2"),
                    )
                    socialLinks = socialLinks + presets[socialLinks.size % presets.size]
                  },
                  containerColor = AdminSurfaceElevated,
                  contentColor = AdminGold,
                  borderColor = AdminGold.copy(alpha = 0.7f),
                  cornerRadius = 12.dp,
                  elevation = 4.dp,
                ) {
                  Text("+ Add Social", color = AdminGold, fontWeight = FontWeight.Bold)
                }
              }

              socialLinks.forEachIndexed { idx, link ->
                AdminClayCard(
                  cornerRadius = 16.dp,
                  elevation = 6.dp,
                  baseColor = AdminSurfaceElevated,
                  borderColor = AdminBorder,
                ) {
                  Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Text("Social #${idx + 1}", color = AdminGold, style = MaterialTheme.typography.labelMedium)
                      IconButton(
                        onClick = { socialLinks = socialLinks.toMutableList().also { it.removeAt(idx) } },
                        modifier = Modifier.size(28.dp),
                      ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = AdminDanger, modifier = Modifier.size(16.dp))
                      }
                    }
                    OutlinedTextField(
                      value = link.label,
                      onValueChange = { v ->
                        socialLinks = socialLinks.toMutableList().also { it[idx] = link.copy(label = v, platform = v) }
                      },
                      label = { Text("Platform / Name (e.g. YouTube, Telegram, TikTok, Facebook)") },
                      singleLine = true,
                      colors = adminFieldColors(),
                      modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                      value = link.url,
                      onValueChange = { v ->
                        socialLinks = socialLinks.toMutableList().also { it[idx] = link.copy(url = v) }
                      },
                      label = { Text("Link URL (https://...)") },
                      singleLine = true,
                      colors = adminFieldColors(),
                      modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                      value = link.color,
                      onValueChange = { v ->
                        socialLinks = socialLinks.toMutableList().also { it[idx] = link.copy(color = v) }
                      },
                      label = { Text("Color Hex (#24A1DE)") },
                      singleLine = true,
                      colors = adminFieldColors(),
                      modifier = Modifier.fillMaxWidth(),
                    )
                  }
                }
              }

              AdminClayButton(
                onClick = { onSaveConfig(AdminKeyGateConfig(requireKey, getKeyUrl, socialLinks)) },
                containerColor = AdminGold,
                contentColor = Color.Black,
                cornerRadius = 16.dp,
                elevation = 8.dp,
                modifier = Modifier.fillMaxWidth().height(48.dp),
              ) {
                Text("SAVE KEY GATE & SOCIAL LINKS", fontWeight = FontWeight.ExtraBold)
              }
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }
}

@Composable
private fun adminFieldColors() =
  OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AdminGold,
    unfocusedBorderColor = AdminBorder,
    focusedLabelColor = AdminGold,
    unfocusedLabelColor = AdminTextSecondary,
    focusedTextColor = AdminTextPrimary,
    unfocusedTextColor = AdminTextPrimary,
    cursorColor = AdminGold,
  )
