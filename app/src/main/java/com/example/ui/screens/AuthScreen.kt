package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.ClayButton
import com.example.ui.components.ClayCard
import com.example.ui.components.ZonexBrandTitle
import com.example.ui.components.claySurface
import com.example.ui.theme.LocalZonexPalette
import com.example.ui.viewmodel.AuthFormState

@Composable
fun AuthScreen(
  state: AuthFormState,
  onSelectTab: (Boolean) -> Unit,
  onFullNameChange: (String) -> Unit,
  onEmailChange: (String) -> Unit,
  onPasswordChange: (String) -> Unit,
  onConfirmPasswordChange: (String) -> Unit,
  onKeepLoggedInChange: (Boolean) -> Unit,
  onSubmitLogin: () -> Unit,
  onSubmitSignUp: () -> Unit,
) {
  val palette = LocalZonexPalette.current
  var showPassword by remember { mutableStateOf(false) }
  var showConfirmPassword by remember { mutableStateOf(false) }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(palette.background)
        .safeDrawingPadding()
        .imePadding()
  ) {
    // Atmospheric hero art header
    Image(
      painter = painterResource(id = R.drawable.img_zonex_hero_1795412379800),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxWidth().height(230.dp).alpha(0.45f),
    )
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(250.dp)
          .background(
            Brush.verticalGradient(
              listOf(Color.Transparent, palette.background.copy(alpha = 0.85f), palette.background)
            )
          )
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .heightIn(min = maxHeight)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {

      // Custom Zonex Voice Logo header
      Box(
        modifier =
          Modifier.size(86.dp)
            .claySurface(
              baseColor = Color(0xFFFEFEFE),
              cornerRadius = 26.dp,
              elevation = 14.dp,
              accentBorderColor = palette.goldPrimary.copy(alpha = 0.8f),
            ),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_zonex_logo),
          contentDescription = "Zonex Sound Logo",
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize().padding(6.dp),
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      ZonexBrandTitle(large = true)
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Your Sounds. Your Game.",
        style = MaterialTheme.typography.bodyMedium,
        color = palette.textSecondary,
      )

      Spacer(modifier = Modifier.height(22.dp))

      ClayCard(
        modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp),
        baseColor = palette.surface,
        cornerRadius = 26.dp,
        elevation = 16.dp,
        borderColor = palette.border,
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          // Tab switcher: সাইন ইন / Login | রেজিস্টার / Sign Up
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .claySurface(
                  baseColor = palette.background,
                  cornerRadius = 16.dp,
                  elevation = 4.dp,
                )
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            AuthTabPill(
              title = "সাইন ইন / Login",
              selected = !state.isSignUpTab,
              onClick = { onSelectTab(false) },
              modifier = Modifier.weight(1f).testTag("auth_tab_login"),
            )
            AuthTabPill(
              title = "রেজিস্টার / Sign Up",
              selected = state.isSignUpTab,
              onClick = { onSelectTab(true) },
              modifier = Modifier.weight(1f).testTag("auth_tab_signup"),
            )
          }

          Spacer(modifier = Modifier.height(18.dp))

          val fieldColors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = palette.goldPrimary,
              unfocusedBorderColor = palette.border,
              focusedLabelColor = palette.goldPrimary,
              unfocusedLabelColor = palette.textSecondary,
              cursorColor = palette.goldPrimary,
              focusedTextColor = palette.textPrimary,
              unfocusedTextColor = palette.textPrimary,
            )

          if (state.isSignUpTab) {
            OutlinedTextField(
              value = state.fullName,
              onValueChange = onFullNameChange,
              label = { Text("Full Name") },
              leadingIcon = {
                Icon(Icons.Filled.Person, contentDescription = null, tint = palette.goldPrimary)
              },
              singleLine = true,
              colors = fieldColors,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("signup_name_input"),
            )
            Spacer(modifier = Modifier.height(12.dp))
          }

          OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text("Email Address") },
            leadingIcon = {
              Icon(Icons.Filled.Email, contentDescription = null, tint = palette.goldPrimary)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            colors = fieldColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
          )

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = {
              Icon(Icons.Filled.Lock, contentDescription = null, tint = palette.goldPrimary)
            },
            trailingIcon = {
              IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                  imageVector =
                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                  contentDescription = if (showPassword) "Hide password" else "Show password",
                  tint = palette.textSecondary,
                )
              }
            },
            visualTransformation =
              if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            colors = fieldColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
          )

          if (state.isSignUpTab) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
              value = state.confirmPassword,
              onValueChange = onConfirmPasswordChange,
              label = { Text("Confirm Password") },
              leadingIcon = {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = palette.goldPrimary)
              },
              trailingIcon = {
                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                  Icon(
                    imageVector =
                      if (showConfirmPassword) Icons.Filled.VisibilityOff
                      else Icons.Filled.Visibility,
                    contentDescription = "Toggle confirm password visibility",
                    tint = palette.textSecondary,
                  )
                }
              },
              visualTransformation =
                if (showConfirmPassword) VisualTransformation.None
                else PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              singleLine = true,
              colors = fieldColors,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("signup_confirm_password_input"),
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "লগইন সেভ রাখুন / Keep me logged in",
              style = MaterialTheme.typography.bodyMedium,
              color = palette.textSecondary,
              modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            Switch(
              checked = state.keepLoggedIn,
              onCheckedChange = onKeepLoggedInChange,
              colors =
                SwitchDefaults.colors(
                  checkedThumbColor = palette.onGold,
                  checkedTrackColor = palette.goldPrimary,
                  uncheckedThumbColor = palette.textSecondary,
                  uncheckedTrackColor = palette.surfaceElevated,
                ),
              modifier = Modifier.testTag("keep_logged_in_switch"),
            )
          }

          if (!state.errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = palette.danger.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.5f)),
            ) {
              Text(
                text = state.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = palette.danger,
                modifier = Modifier.fillMaxWidth().padding(10.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          ClayButton(
            onClick = { if (state.isSignUpTab) onSubmitSignUp() else onSubmitLogin() },
            enabled = !state.isLoading,
            containerColor = palette.goldPrimary,
            contentColor = palette.onGold,
            cornerRadius = 18.dp,
            elevation = 10.dp,
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("auth_submit_button"),
          ) {
            if (state.isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = palette.onGold,
              )
            } else {
              Text(
                text = if (state.isSignUpTab) "রেজিস্টার / Sign Up" else "সাইন ইন / Login",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))
      Text(
        text = "Connected to Zonex Cloud Audio • Real-Time Sync",
        style = MaterialTheme.typography.labelSmall,
        color = palette.textSecondary,
        textAlign = TextAlign.Center,
      )
      }
    }
  }
}

@Composable
private fun AuthTabPill(
  title: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalZonexPalette.current
  Box(
    modifier =
      modifier
        .height(44.dp)
        .clip(RoundedCornerShape(11.dp))
        .background(if (selected) palette.goldPrimary else Color.Transparent)
        .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge,
      color = if (selected) palette.onGold else palette.textSecondary,
      maxLines = 1,
    )
  }
}
