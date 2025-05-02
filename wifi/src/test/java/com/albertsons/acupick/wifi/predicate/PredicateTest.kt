package com.albertsons.acupick.wifi.predicate

import com.albertsons.acupick.wifi.settings.Settings
import com.albertsons.acupick.wifi.band.WiFiBand
import com.albertsons.acupick.wifi.model.Security
import com.albertsons.acupick.wifi.model.Strength
import com.albertsons.acupick.wifi.model.WiFiDetail
import com.albertsons.acupick.wifi.model.WiFiIdentifier
import com.albertsons.acupick.wifi.model.WiFiSignal
import com.albertsons.acupick.wifi.model.WiFiWidth
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal enum class TestObject {
    VALUE1, VALUE3, VALUE2
}

class PredicateTest {
    private val ssid = "SSID"
    private val wpa2 = "WPA2"

    private val settings: Settings = mock()

    @After
    fun tearDown() {
        verifyNoMoreInteractions(settings)
    }

    @Test
    fun testMakeAccessPointsPredicate() {
        // setup
        whenSettings()
        // execute
        val fixture: Predicate = makeAccessPointsPredicate(settings)
        // validate
        assertNotNull(fixture)
        verifySettings()
    }

    @Test
    fun testMakeAccessPointsPredicateIsTrue() {
        // setup
        whenSettings()
        val fixture: Predicate = makeAccessPointsPredicate(settings)
        val wiFiDetail = makeWiFiDetail(ssid, wpa2)
        // execute
        val actual = fixture(wiFiDetail)
        // validate
        assertTrue(actual)
        verifySettings()
    }

    @Test
    fun testMakeAccessPointsPredicateWithSecurityToFalse() {
        // setup
        whenSettings()
        val fixture: Predicate = makeAccessPointsPredicate(settings)
        val wiFiDetail = makeWiFiDetail(ssid, "WPA")
        // execute
        val actual = fixture(wiFiDetail)
        // validate
        assertFalse(actual)
        verifySettings()
    }

    @Test
    fun testMakeAccessPointsPredicateWithSSIDToFalse() {
        // setup
        whenSettings()
        val fixture: Predicate = makeAccessPointsPredicate(settings)
        val wiFiDetail = makeWiFiDetail("WIFI", wpa2)
        // execute
        val actual = fixture(wiFiDetail)
        // validate
        assertFalse(actual)
        verifySettings()
    }

    @Test
    fun testMakeAccessPointsPredicateIsAllPredicate() {
        // setup
        val wiFiDetail = WiFiDetail()
        whenSettingsWithFullSets()
        // execute
        val fixture: Predicate = makeAccessPointsPredicate(settings)
        // validate
        assertTrue(fixture(wiFiDetail))
        verifySettings()
    }

    @Test
    fun testMakeAccessPointsPredicateIsTrueWhenFullSet() {
        // setup
        whenSettingsWithFullSets()
        val wiFiDetail = makeWiFiDetail(ssid, wpa2)
        val fixture: Predicate = makeAccessPointsPredicate(settings)
        // execute
        val actual = fixture(wiFiDetail)
        // validate
        assertTrue(actual)
        verifySettings()
    }

    @Test
    fun testMakeOtherPredicate() {
        // setup
        whenever(settings.wiFiBand()).thenReturn(WiFiBand.GHZ5)
        whenever(settings.findSSIDs()).thenReturn(setOf(ssid, ssid))
        whenever(settings.findStrengths()).thenReturn(setOf(Strength.TWO, Strength.FOUR))
        whenever(settings.findSecurities()).thenReturn(setOf(Security.WEP, Security.WPA2))
        // execute
        val fixture: Predicate = makeOtherPredicate(settings)
        // validate
        assertNotNull(fixture)
        verify(settings).wiFiBand()
        verify(settings).findSSIDs()
        verify(settings).findStrengths()
        verify(settings).findSecurities()
    }

    private fun whenSettingsWithFullSets() {
        whenever(settings.findSSIDs()).thenReturn(setOf())
        whenever(settings.findWiFiBands()).thenReturn(WiFiBand.values().toSet())
        whenever(settings.findStrengths()).thenReturn(Strength.values().toSet())
        whenever(settings.findSecurities()).thenReturn(Security.values().toSet())
    }

    private fun whenSettings() {
        whenever(settings.findSSIDs()).thenReturn(setOf(ssid, ssid))
        whenever(settings.findWiFiBands()).thenReturn(setOf(WiFiBand.GHZ2))
        whenever(settings.findStrengths()).thenReturn(setOf(Strength.TWO, Strength.FOUR))
        whenever(settings.findSecurities()).thenReturn(setOf(Security.WEP, Security.WPA2))
    }

    private fun verifySettings() {
        verify(settings).findSSIDs()
        verify(settings).findWiFiBands()
        verify(settings).findStrengths()
        verify(settings).findSecurities()
    }

    private fun makeWiFiDetail(ssid: String, security: String): WiFiDetail =
        WiFiDetail(
            WiFiIdentifier(ssid, "bssid"),
            security,
            WiFiSignal(2445, 2445, WiFiWidth.MHZ_20, -40, true)
        )
}
