package com.albertsons.acupick.wifi.predicate

import com.albertsons.acupick.wifi.model.WiFiDetail
import com.albertsons.acupick.wifi.model.WiFiIdentifier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SSIDPredicateTest {
    @Test
    fun testSSIDPredicate() {
        // setup
        val wiFiDetail = WiFiDetail(WiFiIdentifier("ssid", "bssid"), "wpa")
        // execute & validate
        assertTrue("ssid".predicate()(wiFiDetail))
        assertTrue("id".predicate()(wiFiDetail))
        assertTrue("ss".predicate()(wiFiDetail))
        assertTrue("s".predicate()(wiFiDetail))
        assertTrue("".predicate()(wiFiDetail))
        assertFalse("SSID".predicate()(wiFiDetail))
        assertFalse("B".predicate()(wiFiDetail))
    }
}
