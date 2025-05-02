package com.albertsons.acupick.wifi.predicate

import com.albertsons.acupick.wifi.model.Security
import com.albertsons.acupick.wifi.model.WiFiDetail
import com.albertsons.acupick.wifi.model.WiFiIdentifier
import com.albertsons.acupick.wifi.model.WiFiSignal
import com.albertsons.acupick.wifi.model.WiFiWidth
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityPredicateTest {
    @Test
    fun testSecurityPredicateWithFoundWPAValue() {
        // setup
        val wiFiDetail = wiFiDetail()
        val fixture = Security.WPA.predicate()
        // execute
        val actual = fixture(wiFiDetail)
        // validate
        assertTrue(actual)
    }

    @Test
    fun testSecurityPredicateWithFoundWEPValue() {
        // setup
        val wiFiDetail = wiFiDetail()
        val fixture = Security.WEP.predicate()
        // execute
        val actual = fixture(wiFiDetail)
        // validate
        assertTrue(actual)
    }

    @Test
    fun testSecurityPredicateWithFoundNoneValue() {
        // setup
        val wiFiDetail = wiFiDetailWithNoSecurity()
        val fixture = Security.NONE.predicate()
        // execute
        val actual = fixture(wiFiDetail)
        // validate
        assertTrue(actual)
    }

    @Test
    fun testSecurityPredicateWithNotFoundValue() {
        // setup
        val wiFiDetail = wiFiDetail()
        val fixture = Security.WPA2.predicate()
        // execute
        val actual = fixture(wiFiDetail)
        // validate
        assertFalse(actual)
    }

    private fun wiFiDetail(): WiFiDetail =
        WiFiDetail(
            WiFiIdentifier("ssid", "bssid"),
            "ess-wep-wpa",
            WiFiSignal(2455, 2455, WiFiWidth.MHZ_20, 1, true)
        )

    private fun wiFiDetailWithNoSecurity(): WiFiDetail =
        WiFiDetail(
            WiFiIdentifier("ssid", "bssid"),
            "ess",
            WiFiSignal(2455, 2455, WiFiWidth.MHZ_20, 1, true)
        )
}
