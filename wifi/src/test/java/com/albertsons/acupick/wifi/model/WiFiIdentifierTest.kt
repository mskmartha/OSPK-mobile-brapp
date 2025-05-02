package com.albertsons.acupick.wifi.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotSame
import org.junit.Test

class WiFiIdentifierTest {
    private val hidden = "*hidden*"
    private val ssid = "xyzSSID"
    private val bssid = "xyzBSSID"
    private val fixture = WiFiIdentifier(ssid, bssid)

    @Test
    fun testWiFiIdentifier() {
        // setup
        val expectedTitle = "$ssid ($bssid)"
        // validate
        assertEquals(ssid, fixture.ssidRaw)
        assertEquals(ssid, fixture.ssid)
        assertEquals(bssid, fixture.bssid)
        assertEquals(expectedTitle, fixture.title)
    }

    @Test
    fun testTitleWithEmptySSID() {
        // setup
        val expectedTitle = "*hidden* ($bssid)"
        val fixture = WiFiIdentifier("", bssid)
        // validate
        assertEquals(expectedTitle, fixture.title)
    }

    @Test
    fun testEquals() {
        // setup
        val other = WiFiIdentifier(ssid, bssid)
        // execute & validate
        assertEquals(fixture, other)
        assertNotSame(fixture, other)
    }

    @Test
    fun testHashCode() {
        // setup
        val other = WiFiIdentifier(ssid, bssid)
        // execute & validate
        assertEquals(fixture.hashCode(), other.hashCode())
    }

    @Test
    fun testEqualsIgnoreCase() {
        // setup
        val other = WiFiIdentifier(ssid.lowercase(), bssid.uppercase())
        // execute & validate
        assertTrue(fixture.equals(other, true))
    }

    @Test
    fun testCompareTo() {
        // setup
        val other = WiFiIdentifier(ssid, bssid)
        // execute & validate
        assertEquals(0, fixture.compareTo(other))
    }

    @Test
    fun testRawSSID() {
        // setup
        val fixture = WiFiIdentifier("", bssid)
        // execute & validate
        assertEquals("", fixture.ssidRaw)
        assertEquals(hidden, fixture.ssid)
    }
}
