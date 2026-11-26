package com.example

import com.example.security.ZonexSecurityGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZonexSecurityTest {

  @Test
  fun revealDecodesAllObfuscatedSecretsLosslessly() {
    assertEquals("com.zonexsound.app", ZonexSecurityGuard.expectedPackageName)
    assertEquals("AIzaSyB0fVszlJRL-lI4ywIaBJa6dhH6eHaHUH8", ZonexSecurityGuard.firebaseApiKey)
    assertEquals("https://zonex-voice-default-rtdb.firebaseio.com", ZonexSecurityGuard.firebaseDbUrl)
    assertEquals("zonex-voice", ZonexSecurityGuard.firebaseProjectId)
    assertEquals("zonex-voice.firebasestorage.app", ZonexSecurityGuard.firebaseStorageBucket)
    assertEquals("620554144893", ZonexSecurityGuard.firebaseSenderId)
    assertEquals("1:620554144893:web:147b693fafa8dfe5027514", ZonexSecurityGuard.firebaseAppId)
    assertEquals("https://api.cartesia.ai/tts/bytes", ZonexSecurityGuard.cartesiaTtsUrl)
    assertEquals("https://api.cartesia.ai/voices", ZonexSecurityGuard.cartesiaVoicesUrl)
  }

  @Test
  fun versionComparisonOnlyFlagsVersionsBelowLatest() {
    assertTrue(ZonexSecurityGuard.isVersionBelowLatest("1.9", 19, "2.0", 20))
    assertTrue(ZonexSecurityGuard.isVersionBelowLatest("1.10", 18, "2.0", 20))
    assertFalse(ZonexSecurityGuard.isVersionBelowLatest("2.0", 20, "2.0", 20))
    assertFalse(ZonexSecurityGuard.isVersionBelowLatest("2.1", 21, "2.0", 20))
    assertTrue(ZonexSecurityGuard.isVersionBelowLatest("2.0", 20, "2.1", 21))
    assertTrue(ZonexSecurityGuard.isVersionBelowLatest("2.0", 20, "2.0.1", 0))
  }

  @Test
  fun targetVersionMatchesSpecifiedVersionAndAllBelow() {
    assertTrue(ZonexSecurityGuard.shouldShowAppUpdate("2.1", 21, "2.4", 24, "2.1"))
    assertTrue(ZonexSecurityGuard.shouldShowAppUpdate("2.0", 20, "2.4", 24, "2.1"))
    assertTrue(ZonexSecurityGuard.shouldShowAppUpdate("1.9", 19, "2.4", 24, "2.1"))
    assertTrue(ZonexSecurityGuard.shouldShowAppUpdate("1.8", 18, "2.4", 24, "2.1"))
    assertFalse(ZonexSecurityGuard.shouldShowAppUpdate("2.2", 22, "2.4", 24, "2.1"))
    assertFalse(ZonexSecurityGuard.shouldShowAppUpdate("2.4", 24, "2.4", 24, "2.1"))
    assertTrue(ZonexSecurityGuard.shouldShowAppUpdate("2.3", 23, "2.4", 24, ""))
    assertFalse(ZonexSecurityGuard.shouldShowAppUpdate("2.4", 24, "2.4", 24, ""))
  }
}
