package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppThemeStyle
import com.example.data.model.UserSettingsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.zonexDataStore: DataStore<Preferences> by preferencesDataStore(name = "zonex_sound_prefs")

class ZonexPreferences(private val context: Context) {

  private object Keys {
    val THEME_STYLE = stringPreferencesKey("theme_style")
    val KEEP_LOGGED_IN = booleanPreferencesKey("keep_logged_in")
    val MASTER_VOLUME = floatPreferencesKey("master_volume")
    val SFX_CHANNEL_VOLUME = floatPreferencesKey("sfx_channel_volume")
    val MEDIA_CHANNEL_VOLUME = floatPreferencesKey("media_channel_volume")
    val FREE_FIRE_BOOST = booleanPreferencesKey("free_fire_boost")
    val FLOATING_BUBBLE = booleanPreferencesKey("floating_bubble")
    val QUICK_NOTIFICATION = booleanPreferencesKey("quick_notification")
    val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    val SOUND_ANIMATIONS = booleanPreferencesKey("sound_animations")
    val DISMISSED_BANNER = stringPreferencesKey("dismissed_banner")
    val CARTESIA_API_KEY = stringPreferencesKey("cartesia_api_key")
    val CARTESIA_API_KEYS_JSON = stringPreferencesKey("cartesia_api_keys_json")
    val ACTIVE_CARTESIA_KEY_INDEX = intPreferencesKey("active_cartesia_key_index")
    val SELECTED_CARTESIA_VOICE_ID = stringPreferencesKey("selected_cartesia_voice_id")
    val CUSTOM_TTS_VOICES_JSON = stringPreferencesKey("custom_tts_voices_json")
    val TTS_VOICE_ORDER_JSON = stringPreferencesKey("tts_voice_order_json")
    val IS_BANGLA_UI = booleanPreferencesKey("is_bangla_ui")
    val MIC_INJECTION_ENABLED = booleanPreferencesKey("mic_injection_enabled")
    val ACTIVE_ACCESS_KEY = stringPreferencesKey("active_access_key")
    val ACTIVE_ACCESS_KEY_EXPIRES = longPreferencesKey("active_access_key_expires")
  }

  val settingsFlow: Flow<UserSettingsState> =
    context.zonexDataStore.data.map { prefs ->
      val themeName = prefs[Keys.THEME_STYLE] ?: AppThemeStyle.PURE_BLACK.name
      val theme =
        AppThemeStyle.entries.firstOrNull { it.name == themeName } ?: AppThemeStyle.PURE_BLACK
      val legacySingleKey = prefs[Keys.CARTESIA_API_KEY].orEmpty().trim()
      val parsedKeys = parseStringList(prefs[Keys.CARTESIA_API_KEYS_JSON].orEmpty())
      val combinedKeys =
        if (parsedKeys.isNotEmpty()) parsedKeys
        else if (legacySingleKey.isNotBlank()) listOf(legacySingleKey)
        else emptyList()
      val rawIndex = prefs[Keys.ACTIVE_CARTESIA_KEY_INDEX] ?: 0
      val safeIndex = if (combinedKeys.isEmpty()) 0 else rawIndex.coerceIn(0, combinedKeys.lastIndex)
      val activeKey = combinedKeys.getOrElse(safeIndex) { legacySingleKey }

      UserSettingsState(
        themeStyle = theme,
        keepLoggedIn = prefs[Keys.KEEP_LOGGED_IN] ?: true,
        masterVolume = prefs[Keys.MASTER_VOLUME] ?: 0.9f,
        sfxChannelVolume = prefs[Keys.SFX_CHANNEL_VOLUME] ?: 1.0f,
        mediaChannelVolume = prefs[Keys.MEDIA_CHANNEL_VOLUME] ?: 0.85f,
        freeFireVoiceBoost = prefs[Keys.FREE_FIRE_BOOST] ?: false,
        floatingBubbleEnabled = prefs[Keys.FLOATING_BUBBLE] ?: false,
        quickNotificationControl = prefs[Keys.QUICK_NOTIFICATION] ?: false,
        hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
        soundAnimations = prefs[Keys.SOUND_ANIMATIONS] ?: true,
        dismissedBannerText = prefs[Keys.DISMISSED_BANNER] ?: "",
        cartesiaApiKey = activeKey,
        cartesiaApiKeys = combinedKeys,
        activeCartesiaKeyIndex = safeIndex,
        selectedCartesiaVoiceId =
          prefs[Keys.SELECTED_CARTESIA_VOICE_ID] ?: "char_omor_on_fire",
        customTtsVoicesJson = prefs[Keys.CUSTOM_TTS_VOICES_JSON] ?: "",
        ttsVoiceOrderIds = parseStringList(prefs[Keys.TTS_VOICE_ORDER_JSON].orEmpty()),
        isBanglaUi = prefs[Keys.IS_BANGLA_UI] ?: true,
        micInjectionEnabled = prefs[Keys.MIC_INJECTION_ENABLED] ?: false,
      )
    }

  private fun parseStringList(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val s = arr.optString(i).trim()
          if (s.isNotBlank()) add(s)
        }
      }.distinct()
    }.getOrDefault(emptyList())
  }

  private fun encodeStringList(items: List<String>): String {
    val arr = JSONArray()
    items.map { it.trim() }.filter { it.isNotBlank() }.distinct().forEach { arr.put(it) }
    return arr.toString()
  }

  suspend fun getKeepLoggedIn(): Boolean {
    return context.zonexDataStore.data.map { it[Keys.KEEP_LOGGED_IN] ?: true }.first()
  }

  suspend fun setKeepLoggedIn(keep: Boolean) {
    context.zonexDataStore.edit { it[Keys.KEEP_LOGGED_IN] = keep }
  }

  suspend fun setThemeStyle(style: AppThemeStyle) {
    context.zonexDataStore.edit { it[Keys.THEME_STYLE] = style.name }
  }

  suspend fun setMasterVolume(value: Float) {
    context.zonexDataStore.edit { it[Keys.MASTER_VOLUME] = value.coerceIn(0f, 1f) }
  }

  suspend fun setSfxChannelVolume(value: Float) {
    context.zonexDataStore.edit { it[Keys.SFX_CHANNEL_VOLUME] = value.coerceIn(0f, 1f) }
  }

  suspend fun setMediaChannelVolume(value: Float) {
    context.zonexDataStore.edit { it[Keys.MEDIA_CHANNEL_VOLUME] = value.coerceIn(0f, 1f) }
  }

  suspend fun setFreeFireVoiceBoost(enabled: Boolean) {
    context.zonexDataStore.edit { it[Keys.FREE_FIRE_BOOST] = enabled }
  }

  suspend fun setFloatingBubbleEnabled(enabled: Boolean) {
    context.zonexDataStore.edit { it[Keys.FLOATING_BUBBLE] = enabled }
  }

  suspend fun setQuickNotificationControl(enabled: Boolean) {
    context.zonexDataStore.edit { it[Keys.QUICK_NOTIFICATION] = enabled }
  }

  suspend fun setHapticFeedback(enabled: Boolean) {
    context.zonexDataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
  }

  suspend fun setSoundAnimations(enabled: Boolean) {
    context.zonexDataStore.edit { it[Keys.SOUND_ANIMATIONS] = enabled }
  }

  suspend fun setDismissedBannerText(text: String) {
    context.zonexDataStore.edit { it[Keys.DISMISSED_BANNER] = text }
  }

  suspend fun setCartesiaApiKeys(keys: List<String>, activeIndex: Int = 0) {
    val clean = keys.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    val safeIndex = if (clean.isEmpty()) 0 else activeIndex.coerceIn(0, clean.lastIndex)
    context.zonexDataStore.edit {
      it[Keys.CARTESIA_API_KEYS_JSON] = encodeStringList(clean)
      it[Keys.ACTIVE_CARTESIA_KEY_INDEX] = safeIndex
      it[Keys.CARTESIA_API_KEY] = clean.getOrElse(safeIndex) { "" }
    }
  }

  suspend fun setActiveCartesiaKeyIndex(index: Int) {
    context.zonexDataStore.edit {
      val keys = parseStringList(it[Keys.CARTESIA_API_KEYS_JSON].orEmpty())
      if (keys.isNotEmpty()) {
        val safe = index.coerceIn(0, keys.lastIndex)
        it[Keys.ACTIVE_CARTESIA_KEY_INDEX] = safe
        it[Keys.CARTESIA_API_KEY] = keys[safe]
      }
    }
  }

  suspend fun setSelectedCartesiaVoiceId(voiceId: String) {
    context.zonexDataStore.edit { it[Keys.SELECTED_CARTESIA_VOICE_ID] = voiceId }
  }

  suspend fun setCustomTtsVoicesJson(json: String) {
    context.zonexDataStore.edit { it[Keys.CUSTOM_TTS_VOICES_JSON] = json }
  }

  suspend fun setTtsVoiceOrderIds(ids: List<String>) {
    context.zonexDataStore.edit { it[Keys.TTS_VOICE_ORDER_JSON] = encodeStringList(ids) }
  }

  suspend fun setIsBanglaUi(isBangla: Boolean) {
    context.zonexDataStore.edit { it[Keys.IS_BANGLA_UI] = isBangla }
  }

  suspend fun getMicInjectionEnabled(): Boolean {
    return context.zonexDataStore.data.map { it[Keys.MIC_INJECTION_ENABLED] ?: false }.first()
  }

  suspend fun setMicInjectionEnabled(enabled: Boolean) {
    context.zonexDataStore.edit { it[Keys.MIC_INJECTION_ENABLED] = enabled }
  }

  suspend fun getCachedAccessKey(): Pair<String, Long> {
    return context.zonexDataStore.data.map { prefs ->
      (prefs[Keys.ACTIVE_ACCESS_KEY] ?: "") to (prefs[Keys.ACTIVE_ACCESS_KEY_EXPIRES] ?: 0L)
    }.first()
  }

  suspend fun setCachedAccessKey(code: String, expiresAt: Long) {
    context.zonexDataStore.edit {
      it[Keys.ACTIVE_ACCESS_KEY] = code
      it[Keys.ACTIVE_ACCESS_KEY_EXPIRES] = expiresAt
    }
  }

  suspend fun getLocalOnboardingSeen(uid: String): Boolean {
    val key = booleanPreferencesKey("onboarding_seen_$uid")
    return context.zonexDataStore.data.map { it[key] ?: false }.first()
  }

  suspend fun setLocalOnboardingSeen(uid: String, seen: Boolean = true) {
    val key = booleanPreferencesKey("onboarding_seen_$uid")
    context.zonexDataStore.edit { it[key] = seen }
  }

  suspend fun restoreDefaults() {
    context.zonexDataStore.edit { prefs ->
      prefs[Keys.THEME_STYLE] = AppThemeStyle.PURE_BLACK.name
      prefs[Keys.MASTER_VOLUME] = 0.9f
      prefs[Keys.SFX_CHANNEL_VOLUME] = 1.0f
      prefs[Keys.MEDIA_CHANNEL_VOLUME] = 0.85f
      prefs[Keys.FREE_FIRE_BOOST] = false
      prefs[Keys.HAPTIC_FEEDBACK] = true
      prefs[Keys.SOUND_ANIMATIONS] = true
      prefs[Keys.DISMISSED_BANNER] = ""
    }
  }
}
