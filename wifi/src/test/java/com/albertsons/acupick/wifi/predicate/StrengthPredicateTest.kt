package com.albertsons.acupick.wifi.predicate

import com.albertsons.acupick.wifi.model.Strength
import com.albertsons.acupick.wifi.model.WiFiDetail
import com.albertsons.acupick.wifi.model.WiFiIdentifier
import com.albertsons.acupick.wifi.model.WiFiSignal
import com.albertsons.acupick.wifi.model.WiFiWidth
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthPredicateTest {
    @Test
    fun testStrengthPredicate() {
        // setup
        val wiFiDetail = makeWiFiDetail(-60)
        // execute & validate
        assertTrue(Strength.THREE.predicate()(wiFiDetail))
        assertFalse(Strength.FOUR.predicate()(wiFiDetail))
    }

    private fun makeWiFiDetail(level: Int): WiFiDetail =
        WiFiDetail(
            WiFiIdentifier("ssid", "bssid"),
            "wpa",
            WiFiSignal(2445, 2445, WiFiWidth.MHZ_20, level, true)
        )
}
