package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.DeveloperConfig
import com.example.security.ZonexSecurityGuard
import com.example.ui.components.ClayButton
import com.example.ui.components.ClayCard
import com.example.ui.components.ZonexBrandTitle
import com.example.ui.components.claySurface
import com.example.ui.components.parseHexColorSafe
import com.example.ui.theme.LocalZonexPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingOverlaySequence(
  userName: String,
  showWelcomeOnTop: Boolean,
  developer: DeveloperConfig,
  isBangla: Boolean = true,
  onDismissWelcome: () -> Unit,
  onGetStarted: () -> Unit,
  onTestAudioCompatibility: () -> Unit,
) {
  val palette = LocalZonexPalette.current
  val context = LocalContext.current

  // Layer 1 (Base layer behind Welcome popup): Feature Overview Screen
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(palette.background)
        .safeDrawingPadding()
        .testTag("onboarding_feature_overview")
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(modifier = Modifier.height(12.dp))
      ZonexBrandTitle(large = true)
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = if (isBangla) "নেক্সট-জেন গেমিং অডিও ইঞ্জিন" else "Next-Gen Gaming Audio Engine",
        style = MaterialTheme.typography.bodyMedium,
        color = palette.goldPrimary,
      )

      Spacer(modifier = Modifier.height(24.dp))

      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        FeatureOverviewCard(
          icon = Icons.Filled.Bolt,
          title = if (isBangla) "আল্ট্রা-ফাস্ট প্লেব্যাক" else "Ultra-Fast Playback",
          description =
            if (isBangla) "ওয়ান-ট্যাপ সাউন্ড ট্রিগার ও রিয়েল-টাইম মিক্সার কন্ট্রোল।"
            else "Instant one-tap sound triggers with hardware-accelerated audio buffers and real-time mixer gain.",
        )
        FeatureOverviewCard(
          icon = Icons.Filled.Layers,
          title = if (isBangla) "ফ্লোটিং গেমিং ওভারলে" else "Floating Gaming Overlay",
          description =
            if (isBangla) "Shizuku বা Root দিয়ে গেমের উপরে ভাসমান ZONEX GOLD উইজেট চালান।"
            else "Launch the draggable ZONEX GOLD widget with your favorite sounds right over your game HUD.",
        )
        FeatureOverviewCard(
          icon = Icons.Filled.Route,
          title = if (isBangla) "অডিও রাউটিং ও হার্ডওয়্যার ল্যাব" else "Transparent Audio Routing",
          description =
            if (isBangla) "স্পিকার, ব্লুটুথ, হেডসেট এবং লাইভ মাইক টেস্টের সম্পূর্ণ সাপোর্ট।"
            else "Honest hardware diagnostics for Phone Speaker, Bluetooth, Wired Aux, and live microphone testing.",
        )
      }

      Spacer(modifier = Modifier.height(28.dp))

      ClayButton(
        onClick = onGetStarted,
        containerColor = palette.goldPrimary,
        contentColor = palette.onGold,
        cornerRadius = 18.dp,
        elevation = 10.dp,
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 520.dp)
            .height(52.dp)
            .testTag("onboarding_get_started_btn"),
      ) {
        Text(
          text = if (isBangla) "শুরু করুন" else "GET STARTED",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      ClayButton(
        onClick = onTestAudioCompatibility,
        containerColor = palette.surfaceElevated,
        contentColor = palette.goldPrimary,
        borderColor = palette.goldPrimary.copy(alpha = 0.7f),
        cornerRadius = 18.dp,
        elevation = 6.dp,
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 520.dp)
            .height(50.dp)
            .testTag("onboarding_test_audio_btn"),
      ) {
        Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isBangla) "অডিও কমপ্যাটিবিলিটি টেস্ট করুন" else "TEST AUDIO COMPATIBILITY",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    // Layer 2 (Rendered ON TOP first): Welcome / Creator Popup
    if (showWelcomeOnTop) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .padding(20.dp)
            .testTag("onboarding_welcome_popup"),
        contentAlignment = Alignment.Center,
      ) {
        ClayCard(
          modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp),
          cornerRadius = 26.dp,
          elevation = 18.dp,
          baseColor = palette.surfaceElevated,
          borderColor = palette.goldPrimary.copy(alpha = 0.85f),
          borderWidth = 1.5.dp,
        ) {
          Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Box(
              modifier =
                Modifier.size(72.dp)
                  .claySurface(
                    baseColor = palette.surface,
                    cornerRadius = 36.dp,
                    elevation = 8.dp,
                    accentBorderColor = palette.goldPrimary.copy(alpha = 0.65f),
                  ),
              contentAlignment = Alignment.Center,
            ) {
              if (developer.logoUrl.isNotBlank()) {
                AsyncImage(
                  model = developer.logoUrl,
                  contentDescription = developer.name,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
              } else {
                Icon(
                  imageVector = Icons.Filled.VolumeUp,
                  contentDescription = "Creator Logo",
                  tint = palette.goldPrimary,
                  modifier = Modifier.size(34.dp),
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = if (isBangla) "স্বাগতম, $userName!" else "Welcome, $userName!",
              style = MaterialTheme.typography.headlineSmall,
              color = palette.goldBright,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (isBangla) "তৈরি করেছেন ${developer.name}" else "Created by ${developer.name}",
              style = MaterialTheme.typography.titleSmall,
              color = palette.textPrimary,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = developer.bio,
              style = MaterialTheme.typography.bodyMedium,
              color = palette.textSecondary,
              textAlign = TextAlign.Center,
            )

            if (developer.buttons.isNotEmpty()) {
              Spacer(modifier = Modifier.height(16.dp))
              FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                developer.buttons.forEach { btn ->
                  val btnColor = parseHexColorSafe(btn.color, palette.goldPrimary)
                  ClayButton(
                    onClick = {
                      val safeUri = ZonexSecurityGuard.sanitizeExternalUrl(btn.url) ?: return@ClayButton
                      runCatching {
                        context.startActivity(
                          Intent(Intent.ACTION_VIEW, Uri.parse(safeUri))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                      }
                    },
                    containerColor = btnColor,
                    contentColor = Color.White,
                    cornerRadius = 14.dp,
                    elevation = 6.dp,
                  ) {
                    Text(btn.label, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                      Icons.Filled.OpenInNew,
                      contentDescription = null,
                      modifier = Modifier.size(14.dp),
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(20.dp))

            ClayButton(
              onClick = onDismissWelcome,
              containerColor = palette.goldPrimary,
              contentColor = palette.onGold,
              cornerRadius = 16.dp,
              elevation = 10.dp,
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("welcome_continue_btn"),
            ) {
              Text(
                text = if (isBangla) "এগিয়ে যান" else "CONTINUE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureOverviewCard(
  icon: ImageVector,
  title: String,
  description: String,
) {
  val palette = LocalZonexPalette.current
  ClayCard(
    modifier = Modifier.fillMaxWidth(),
    cornerRadius = 20.dp,
    elevation = 9.dp,
    baseColor = palette.surface,
    borderColor = palette.border,
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier =
          Modifier.size(44.dp)
            .claySurface(
              baseColor = palette.goldPrimary.copy(alpha = 0.18f),
              cornerRadius = 14.dp,
              elevation = 5.dp,
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, contentDescription = null, tint = palette.goldPrimary, modifier = Modifier.size(22.dp))
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = palette.textPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = description,
          style = MaterialTheme.typography.bodyMedium,
          color = palette.textSecondary,
        )
      }
    }
  }
}
