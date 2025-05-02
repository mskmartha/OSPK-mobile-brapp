package com.albertsons.acupick.wifi.predicate

import com.albertsons.acupick.wifi.band.WiFiBand
import com.albertsons.acupick.wifi.model.WiFiDetail
import com.albertsons.acupick.wifi.model.WiFiIdentifier
import com.albertsons.acupick.wifi.model.WiFiSignal
import com.albertsons.acupick.wifi.model.WiFiWidth
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WiFiBandPredicateTest {
    @Test
    fun testWiFiBandPredicateWith2GHzFrequency() {
        // setup
        val wiFiDetail = makeWiFiDetail(2455)
        // execute & validate
        assertTrue(WiFiBand.GHZ2.predicate()(wiFiDetail))
        assertFalse(WiFiBand.GHZ5.predicate()(wiFiDetail))
    }

    @Test
    fun testWiFiBandPredicateWith5GHzFrequency() {
        // setup
        val wiFiDetail = makeWiFiDetail(5455)
        // execute & validate
        assertFalse(WiFiBand.GHZ2.predicate()(wiFiDetail))
        assertTrue(WiFiBand.GHZ5.predicate()(wiFiDetail))
    }

    private fun makeWiFiDetail(frequency: Int): WiFiDetail =
        WiFiDetail(
            WiFiIdentifier("ssid", "bssid"),
            "wpa",
            WiFiSignal(frequency, frequency, WiFiWidth.MHZ_20, 1, true)
        )
}
