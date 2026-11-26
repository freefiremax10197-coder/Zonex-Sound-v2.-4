package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.security.ZonexSecurityGuard
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class CartesiaVoiceOption(
  val id: String,
  val name: String,
  val cartesiaVoiceId: String = id,
  val language: String = "Multi",
  val avatarUrl: String = "",
  val badgeColorHex: String = "#16A34A",
  val isLocalPreset: Boolean = false,
  val isCustom: Boolean = false,
  val pitch: Float = 1.0f,
  val speechRate: Float = 1.0f,
)

class CartesiaApiQuotaException(val statusCode: Int, message: String) : IllegalStateException(message)

object CartesiaTtsClient {

  private val client =
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(35, TimeUnit.SECONDS)
      .build()

  val defaultVoices =
    listOf(
      CartesiaVoiceOption(
        id = "char_omor_on_fire",
        name = "OmorOnFire",
        cartesiaVoiceId = "694f9389-aac1-45b6-b726-9d9369183238",
        language = "Bangla / Multi",
        badgeColorHex = "#16A34A",
      ),
      CartesiaVoiceOption(
        id = "char_mr_triple_r",
        name = "MR Triple R",
        cartesiaVoiceId = "a0e99841-438c-4a64-b679-ae501e7d6091",
        language = "Gaming / Multi",
        badgeColorHex = "#2563EB",
      ),
      CartesiaVoiceOption(
        id = "char_sheikh_mujib",
        name = "শেখ মুজিব",
        cartesiaVoiceId = "63ff761f-c1e8-414b-b969-d1833d1c870a",
        language = "Bangla Bold",
        badgeColorHex = "#0EA5E9",
      ),
      CartesiaVoiceOption(
        id = "char_meye_voice_1",
        name = "মেয়ে Voice 1",
        cartesiaVoiceId = "f9836c6e-a0bd-460e-9d3c-f7299fa60f94",
        language = "Bangla Girl",
        badgeColorHex = "#EC4899",
      ),
      CartesiaVoiceOption(
        id = "char_zara_gaming",
        name = "Zara Gaming",
        cartesiaVoiceId = "e3827ec5-697a-4b7c-9704-1a23041bbc51",
        language = "Gaming Girl",
        badgeColorHex = "#D946EF",
      ),
      CartesiaVoiceOption(
        id = "char_ajju_bhai",
        name = "Ajju Bhai [HINDI]",
        cartesiaVoiceId = "be79f378-47fe-4f9c-b92b-f02cefa62ccf",
        language = "Hindi Gaming",
        badgeColorHex = "#F97316",
      ),
      CartesiaVoiceOption(
        id = "char_kt_gamerz",
        name = "KT GAMERZ [Hindi]",
        cartesiaVoiceId = "694f9389-aac1-45b6-b726-9d9369183238",
        language = "Hindi",
        badgeColorHex = "#EF4444",
      ),
      CartesiaVoiceOption(
        id = "char_tarek_rahman",
        name = "Tarek Rahman",
        cartesiaVoiceId = "a0e99841-438c-4a64-b679-ae501e7d6091",
        language = "Bangla / Multi",
        badgeColorHex = "#0D9488",
      ),
      CartesiaVoiceOption(
        id = "char_sheikh_hasina",
        name = "Sheikh Hasina",
        cartesiaVoiceId = "69267136-1bdc-412f-ad78-0caad210fb40",
        language = "Bangla",
        badgeColorHex = "#8B5CF6",
      ),
      CartesiaVoiceOption(
        id = "char_tom_ff",
        name = "Tom FF",
        cartesiaVoiceId = "bf0a246a-8642-498a-9950-80c35e9276b5",
        language = "FF Gaming",
        badgeColorHex = "#F59E0B",
      ),
      CartesiaVoiceOption(
        id = "char_dipjol",
        name = "ডিবজল",
        cartesiaVoiceId = "a0e99841-438c-4a64-b679-ae501e7d6091",
        language = "Bangla Heavy",
        badgeColorHex = "#B45309",
      ),
      CartesiaVoiceOption(
        id = "char_raistar",
        name = "RAISTAR [Hindi]",
        cartesiaVoiceId = "694f9389-aac1-45b6-b726-9d9369183238",
        language = "Hindi FF",
        badgeColorHex = "#E11D48",
      ),
      CartesiaVoiceOption(
        id = "char_amit_bhai",
        name = "Amit Bhai [Hindi]",
        cartesiaVoiceId = "be79f378-47fe-4f9c-b92b-f02cefa62ccf",
        language = "Hindi",
        badgeColorHex = "#0284C7",
      ),
      CartesiaVoiceOption(
        id = "char_sooneeta",
        name = "Sooneeta [Hindi]",
        cartesiaVoiceId = "f9836c6e-a0bd-460e-9d3c-f7299fa60f94",
        language = "Hindi Girl",
        badgeColorHex = "#DB2777",
      ),
      CartesiaVoiceOption(
        id = "char_girl_02_bn",
        name = "Girl 02 [BANGLA]",
        cartesiaVoiceId = "e3827ec5-697a-4b7c-9704-1a23041bbc51",
        language = "Bangla Girl",
        badgeColorHex = "#10B981",
      ),
      CartesiaVoiceOption(
        id = "char_girl_03_bn",
        name = "Girl 03 [BANGLA]",
        cartesiaVoiceId = "f9836c6e-a0bd-460e-9d3c-f7299fa60f94",
        language = "Bangla Girl",
        badgeColorHex = "#6366F1",
      ),
      CartesiaVoiceOption(
        id = "char_girls_01_hi",
        name = "Girls 01 [Hindi]",
        cartesiaVoiceId = "69267136-1bdc-412f-ad78-0caad210fb40",
        language = "Hindi Girl",
        badgeColorHex = "#EC4899",
      ),
      // Offline Device TTS presets
      CartesiaVoiceOption(
        id = "DEVICE_CREATOR_NORMAL",
        name = "Free Creator Normal (Offline)",
        cartesiaVoiceId = "DEVICE_CREATOR_NORMAL",
        language = "bn-BD / en",
        badgeColorHex = "#475569",
        isLocalPreset = true,
        pitch = 1.0f,
        speechRate = 1.0f,
      ),
      CartesiaVoiceOption(
        id = "DEVICE_GAMING_HYPE",
        name = "Free Gaming Hype (Offline)",
        cartesiaVoiceId = "DEVICE_GAMING_HYPE",
        language = "bn-BD / en",
        badgeColorHex = "#475569",
        isLocalPreset = true,
        pitch = 1.12f,
        speechRate = 1.18f,
      ),
      CartesiaVoiceOption(
        id = "DEVICE_DEEP_BOSS",
        name = "Free Deep Boss (Offline)",
        cartesiaVoiceId = "DEVICE_DEEP_BOSS",
        language = "bn-BD / en",
        badgeColorHex = "#475569",
        isLocalPreset = true,
        pitch = 0.72f,
        speechRate = 0.90f,
      ),
      CartesiaVoiceOption(
        id = "DEVICE_FUNNY_HIGH",
        name = "Free Funny Chipmunk (Offline)",
        cartesiaVoiceId = "DEVICE_FUNNY_HIGH",
        language = "bn-BD / en",
        badgeColorHex = "#475569",
        isLocalPreset = true,
        pitch = 1.48f,
        speechRate = 1.22f,
      ),
    )

  fun decodeCustomVoices(json: String): List<CartesiaVoiceOption> {
    if (json.isBlank()) return emptyList()
    return runCatching {
      val arr = JSONArray(json)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val id = obj.optString("id").trim()
          val name = obj.optString("name").trim()
          val cartesiaId = obj.optString("cartesiaVoiceId", id).trim()
          val avatarUrl = obj.optString("avatarUrl").trim()
          if (id.isNotBlank() && name.isNotBlank() && cartesiaId.isNotBlank()) {
            add(
              CartesiaVoiceOption(
                id = id,
                name = name,
                cartesiaVoiceId = cartesiaId,
                language = "Custom ID",
                avatarUrl = avatarUrl,
                badgeColorHex = "#7C3AED",
                isCustom = true,
              )
            )
          }
        }
      }
    }.getOrDefault(emptyList())
  }

  fun encodeCustomVoices(voices: List<CartesiaVoiceOption>): String {
    val arr = JSONArray()
    voices.filter { it.isCustom }.forEach { v ->
      arr.put(
        JSONObject().apply {
          put("id", v.id)
          put("name", v.name)
          put("cartesiaVoiceId", v.cartesiaVoiceId)
          put("avatarUrl", v.avatarUrl)
        }
      )
    }
    return arr.toString()
  }

  suspend fun fetchAccountVoices(apiKey: String): Result<List<CartesiaVoiceOption>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val req =
          Request.Builder()
            .url(ZonexSecurityGuard.cartesiaVoicesUrl)
            .header("X-API-Key", apiKey.trim())
            .header("Cartesia-Version", "2025-04-16")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
          val body = resp.body?.string().orEmpty()
          if (!resp.isSuccessful) {
            error("Cartesia voices error (${resp.code}): ${body.take(140)}")
          }
          val arr = JSONArray(body)
          val list = mutableListOf<CartesiaVoiceOption>()
          for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.optString("id")
            val name = obj.optString("name", "Voice ${i + 1}")
            val lang = obj.optString("language", "multi")
            if (id.isNotBlank()) {
              list.add(
                CartesiaVoiceOption(
                  id = id,
                  name = name,
                  cartesiaVoiceId = id,
                  language = lang,
                  badgeColorHex = "#0EA5E9",
                )
              )
            }
          }
          list
        }
      }
    }

  suspend fun synthesizeWithKeyRotation(
    context: Context,
    apiKeys: List<String>,
    activeKeyIndex: Int,
    voice: CartesiaVoiceOption,
    text: String,
    pitchOverride: Float? = null,
    rateOverride: Float? = null,
  ): Pair<Result<File>, Int> =
    withContext(Dispatchers.IO) {
      if (text.isBlank()) {
        return@withContext Result.failure<File>(IllegalArgumentException("Please type text to speak.")) to activeKeyIndex
      }
      val useDeviceTts =
        voice.isLocalPreset || voice.id == "DEVICE_TTS" || voice.id.startsWith("DEVICE_")

      if (useDeviceTts) {
        val outDir = File(context.cacheDir, "tts_outputs").apply { mkdirs() }
        val outFile = File(outDir, "zonex_tts_${System.currentTimeMillis()}.wav")
        val pitch = (pitchOverride ?: voice.pitch).coerceIn(0.5f, 1.8f)
        val rate = (rateOverride ?: voice.speechRate).coerceIn(0.5f, 1.8f)
        return@withContext synthesizeWithAndroidTts(context, text.trim(), outFile, pitch, rate) to activeKeyIndex
      }

      val cleanKeys = apiKeys.map { it.trim() }.filter { it.isNotBlank() }
      if (cleanKeys.isEmpty()) {
        return@withContext Result.failure<File>(
          IllegalStateException(
            "Cartesia API Key যোগ করুন (🔑 আমার Cartesia Key) অথবা নিচের ফ্রি Offline প্রিসেট বাছাই করুন।"
          )
        ) to 0
      }

      var currentIdx = activeKeyIndex.coerceIn(0, cleanKeys.lastIndex)
      var lastFailure: Throwable = IllegalStateException("TTS failed")

      for (attempt in cleanKeys.indices) {
        val tryIdx = (currentIdx + attempt) % cleanKeys.size
        val key = cleanKeys[tryIdx]
        val res =
          synthesizeToAudioFile(
            context = context,
            apiKey = key,
            voiceId = voice.cartesiaVoiceId.ifBlank { voice.id },
            text = text,
            pitchOverride = pitchOverride,
            rateOverride = rateOverride,
          )
        if (res.isSuccess) {
          return@withContext res to tryIdx
        }
        val err = res.exceptionOrNull() ?: IllegalStateException("TTS failed")
        lastFailure = err
        val quotaOrAuthError =
          err is CartesiaApiQuotaException && err.statusCode in setOf(401, 402, 403, 429)
        if (!quotaOrAuthError) {
          return@withContext Result.failure<File>(err) to tryIdx
        }
      }

      Result.failure<File>(lastFailure) to currentIdx
    }

  suspend fun synthesizeToAudioFile(
    context: Context,
    apiKey: String,
    voiceId: String,
    text: String,
    pitchOverride: Float? = null,
    rateOverride: Float? = null,
  ): Result<File> =
    withContext(Dispatchers.IO) {
      if (text.isBlank()) {
        return@withContext Result.failure(IllegalArgumentException("Please type text to speak."))
      }
      val outDir = File(context.cacheDir, "tts_outputs").apply { mkdirs() }
      val outFile = File(outDir, "zonex_tts_${System.currentTimeMillis()}.wav")

      val preset = defaultVoices.firstOrNull { it.id == voiceId }
      val useDeviceTts =
        voiceId == "DEVICE_TTS" || voiceId.startsWith("DEVICE_") || preset?.isLocalPreset == true

      if (useDeviceTts) {
        val pitch = (pitchOverride ?: preset?.pitch ?: 1.0f).coerceIn(0.5f, 1.8f)
        val rate = (rateOverride ?: preset?.speechRate ?: 1.0f).coerceIn(0.5f, 1.8f)
        return@withContext synthesizeWithAndroidTts(context, text.trim(), outFile, pitch, rate)
      }

      if (apiKey.isBlank()) {
        return@withContext Result.failure(
          IllegalStateException("Enter your Cartesia API Key or select any 'Free ... (Offline)' voice preset.")
        )
      }

      runCatching {
        val targetVoiceUuid = preset?.cartesiaVoiceId ?: voiceId.trim()
        val json =
          JSONObject().apply {
            put("model_id", "sonic-2")
            put("transcript", text.trim())
            put(
              "voice",
              JSONObject().apply {
                put("mode", "id")
                put("id", targetVoiceUuid)
              },
            )
            put(
              "output_format",
              JSONObject().apply {
                put("container", "wav")
                put("encoding", "pcm_s16le")
                put("sample_rate", 44100)
              },
            )
          }

        val request =
          Request.Builder()
            .url(ZonexSecurityGuard.cartesiaTtsUrl)
            .header("X-API-Key", apiKey.trim())
            .header("Cartesia-Version", "2025-04-16")
            .header("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { resp ->
          if (!resp.isSuccessful) {
            val errBody = resp.body?.string().orEmpty()
            throw CartesiaApiQuotaException(
              resp.code,
              "Cartesia TTS failed (${resp.code}): ${errBody.take(160)}",
            )
          }
          val bytes = resp.body?.bytes() ?: error("Empty audio response from Cartesia")
          outFile.writeBytes(bytes)
          outFile
        }
      }
    }

  private suspend fun synthesizeWithAndroidTts(
    context: Context,
    text: String,
    outFile: File,
    pitch: Float,
    speechRate: Float,
  ): Result<File> =
    suspendCancellableCoroutine { cont ->
      var tts: TextToSpeech? = null
      tts =
        TextToSpeech(context.applicationContext) { status ->
          val engine = tts
          if (status != TextToSpeech.SUCCESS || engine == null) {
            if (cont.isActive) {
              cont.resume(Result.failure(IllegalStateException("Device TextToSpeech initialization failed.")))
            }
            return@TextToSpeech
          }
          val hasBangla = text.any { it in '\u0980'..'\u09FF' }
          val locale = if (hasBangla) Locale("bn", "BD") else Locale.US
          val langRes = engine.setLanguage(locale)
          if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.language = Locale.US
          }
          engine.setPitch(pitch.coerceIn(0.5f, 1.8f))
          engine.setSpeechRate(speechRate.coerceIn(0.5f, 1.8f))

          val utteranceId = UUID.randomUUID().toString()
          engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
              override fun onStart(id: String?) {}
              override fun onDone(id: String?) {
                engine.shutdown()
                if (cont.isActive) cont.resume(Result.success(outFile))
              }

              @Deprecated("Deprecated in Java")
              override fun onError(id: String?) {
                engine.shutdown()
                if (cont.isActive) {
                  cont.resume(Result.failure(IllegalStateException("Device TTS synthesis error.")))
                }
              }
            }
          )
          val params = Bundle()
          engine.synthesizeToFile(text, params, outFile, utteranceId)
        }
      cont.invokeOnCancellation { runCatching { tts?.shutdown() } }
    }
}
