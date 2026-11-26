package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.model.AccessKeyState
import com.example.data.model.DeveloperConfig
import com.example.data.model.KeySocialLink
import com.example.security.ZonexSecurityGuard
import com.example.ui.components.ClayButton
import com.example.ui.components.ClayCard
import com.example.ui.components.claySurface
import com.example.ui.components.parseHexColorSafe
import com.example.ui.theme.LocalZonexPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyActivationScreen(
  accessKeyState: AccessKeyState,
  isBangla: Boolean,
  getKeyUrl: String,
  socialLinks: List<KeySocialLink>,
  developer: DeveloperConfig,
  onToggleLanguage: () -> Unit,
  onActivateKey: (String) -> Unit,
  onGetKeyClick: () -> Unit,
  onLogout: () -> Unit,
) {
  val palette = LocalZonexPalette.current
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  var keyInput by remember(accessKeyState.keyCode) { mutableStateOf(accessKeyState.keyCode) }

  val resolvedSocials: List<KeySocialLink> =
    remember(socialLinks, developer.buttons) {
      if (socialLinks.isNotEmpty()) {
        socialLinks
      } else {
        developer.buttons.map {
          KeySocialLink(platform = it.label, label = it.label, url = it.url, color = it.color)
        }
      }
    }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(palette.background)
        .safeDrawingPadding()
        .imePadding()
  ) {
    // Top Bar: Logout + বাং / EN Pill Toggle pinned cleanly at top
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onLogout) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ExitToApp,
          contentDescription = "Logout",
          tint = palette.textSecondary,
        )
      }

      Surface(
        modifier =
          Modifier.clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggleLanguage)
            .testTag("key_lang_toggle"),
        shape = RoundedCornerShape(14.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "বাং",
            style = MaterialTheme.typography.labelLarge,
            color = if (isBangla) palette.onGold else palette.textSecondary,
            modifier =
              Modifier.clip(RoundedCornerShape(8.dp))
                .background(if (isBangla) palette.goldPrimary else Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 3.dp),
          )
          Text(
            text = "EN",
            style = MaterialTheme.typography.labelLarge,
            color = if (!isBangla) palette.onGold else palette.textSecondary,
            modifier =
              Modifier.clip(RoundedCornerShape(8.dp))
                .background(if (!isBangla) palette.goldPrimary else Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }
    }

    // Centered Main Body
    BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .heightIn(min = maxHeight)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Box(
          modifier =
            Modifier.size(94.dp)
              .claySurface(
                baseColor = Color(0xFFFEFEFE),
                cornerRadius = 28.dp,
                elevation = 14.dp,
                accentBorderColor = palette.goldPrimary.copy(alpha = 0.85f),
              ),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_zonex_logo),
            contentDescription = "Zonex Sound Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(8.dp),
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "ZONEX SOUND",
          style = MaterialTheme.typography.headlineMedium,
          color = palette.goldBright,
          fontWeight = FontWeight.ExtraBold,
        )
        Text(
          text = if (isBangla) "গেমিং ভয়েস সাউন্ডবোর্ড" else "Gaming Voice Soundboard",
          style = MaterialTheme.typography.bodyMedium,
          color = palette.textSecondary,
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Key Input Card
        ClayCard(
          modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
          baseColor = palette.surface,
          cornerRadius = 26.dp,
          elevation = 16.dp,
          borderColor = palette.goldPrimary.copy(alpha = 0.5f),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = if (isBangla) "শুরু করতে API Key দিন" else "Enter API Key to get started",
              style = MaterialTheme.typography.titleMedium,
              color = palette.textPrimary,
              fontWeight = FontWeight.Bold,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Key,
                contentDescription = null,
                tint = palette.goldPrimary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "KEY (24h • 1 User Only)",
                style = MaterialTheme.typography.labelLarge,
                color = palette.goldPrimary,
                fontWeight = FontWeight.Bold,
              )
            }

            OutlinedTextField(
              value = keyInput,
              onValueChange = { keyInput = it.uppercase() },
              placeholder = {
                Text(
                  text = "ZONEX-ZIXU-XXXXX-XXXXX",
                  fontFamily = FontFamily.Monospace,
                  color = palette.textSecondary.copy(alpha = 0.7f),
                )
              },
              trailingIcon = {
                IconButton(
                  onClick = {
                    val clip = clipboard.getText()?.text?.trim().orEmpty()
                    if (clip.isNotBlank()) keyInput = clip.uppercase()
                  }
                ) {
                  Icon(
                    imageVector = Icons.Filled.ContentPaste,
                    contentDescription = "Paste Key",
                    tint = palette.goldPrimary,
                  )
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = palette.goldPrimary,
                  unfocusedBorderColor = palette.border,
                  focusedContainerColor = palette.background,
                  unfocusedContainerColor = palette.background,
                  focusedTextColor = palette.textPrimary,
                  unfocusedTextColor = palette.textPrimary,
                  cursorColor = palette.goldPrimary,
                ),
              modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
            )

            if (!accessKeyState.errorMessage.isNullOrBlank()) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = palette.danger.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.5f)),
              ) {
                Text(
                  text = accessKeyState.errorMessage,
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.danger,
                  modifier = Modifier.fillMaxWidth().padding(10.dp),
                )
              }
            }

            ClayButton(
              onClick = { onActivateKey(keyInput) },
              enabled = !accessKeyState.isActivating && keyInput.isNotBlank(),
              containerColor = palette.goldPrimary,
              contentColor = palette.onGold,
              cornerRadius = 18.dp,
              elevation = 10.dp,
              modifier = Modifier.fillMaxWidth().height(52.dp).testTag("activate_key_btn"),
            ) {
              if (accessKeyState.isActivating) {
                CircularProgressIndicator(
                  color = palette.onGold,
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(20.dp),
                )
              } else {
                Text(
                  text = if (isBangla) "অ্যাক্টিভেট / Log in" else "Log in",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                )
              }
            }

            ClayButton(
              onClick = onGetKeyClick,
              containerColor = palette.surfaceElevated,
              contentColor = palette.goldBright,
              borderColor = palette.goldPrimary.copy(alpha = 0.7f),
              cornerRadius = 18.dp,
              elevation = 8.dp,
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("get_key_website_btn"),
            ) {
              Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (isBangla) "গেট এপিআই কি / Get a Key" else "Get a Key",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
            }

            if (getKeyUrl.isBlank()) {
              Text(
                text =
                  if (isBangla) "টিপস: অ্যাডমিন প্যানেল থেকে Get Key ওয়েবসাইট লিংক সেট করুন।"
                  else "Admin can set the Get Key website URL live from Sound Admin.",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
              )
            }
          }
        }

        // Dynamic Social Media Platform Links under Get a Key
        if (resolvedSocials.isNotEmpty()) {
          Spacer(modifier = Modifier.height(22.dp))
          FlowRow(
            modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            resolvedSocials.forEach { link ->
              val btnColor = parseHexColorSafe(link.color, palette.info)
              val icon = socialPlatformIcon(link.platform, link.url)
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                  Modifier.widthIn(min = 64.dp, max = 96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                      val safeUrl = ZonexSecurityGuard.sanitizeExternalUrl(link.url) ?: return@clickable
                      runCatching {
                        context.startActivity(
                          Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                      }
                    }
                    .padding(6.dp),
              ) {
                Box(
                  modifier =
                    Modifier.size(50.dp)
                      .claySurface(
                        baseColor = btnColor,
                        cornerRadius = 25.dp,
                        elevation = 8.dp,
                        highlightAlpha = 0.34f,
                      ),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = icon,
                    contentDescription = link.label,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                  )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                  text = link.label.ifBlank { link.platform },
                  style = MaterialTheme.typography.labelMedium,
                  color = btnColor,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Text(
          text = "Powered by ${developer.name}",
          style = MaterialTheme.typography.labelMedium,
          color = palette.goldPrimary,
        )
        Text(
          text = "Zonex Sound • v${BuildConfig.VERSION_NAME} • 24h Key Protection",
          style = MaterialTheme.typography.labelSmall,
          color = palette.textSecondary,
        )
      }
    }
  }
}

private fun socialPlatformIcon(platform: String, url: String): ImageVector {
  val key = "$platform $url".lowercase()
  return when {
    "t.me" in key || "telegram" in key -> Icons.AutoMirrored.Filled.Send
    "youtube" in key || "youtu.be" in key -> Icons.Filled.PlayArrow
    "tiktok" in key -> Icons.Filled.MusicNote
    "facebook" in key || "fb.com" in key -> Icons.Filled.Public
    "instagram" in key || "insta" in key -> Icons.Filled.CameraAlt
    "discord" in key -> Icons.Filled.Forum
    "whatsapp" in key || "wa.me" in key -> Icons.Filled.Chat
    "twitter" in key || "x.com" in key -> Icons.Filled.Share
    else -> Icons.Filled.OpenInNew
  }
}
