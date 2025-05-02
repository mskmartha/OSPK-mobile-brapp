package com.albertsons.acupick.wifi.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotSame
import org.junit.Test

class WiFiConnectionTest {
    private val ipAddress = "21.205.91.7"
    private val linkSpeed = 21
    private val wiFiIdentifier = WiFiIdentifier("SSID-123", "BSSID-123")
    private val fixture: WiFiConnection = WiFiConnection(wiFiIdentifier, ipAddress, linkSpeed)

    @Test
    fun testWiFiConnectionEmpty() {
        // validate
        assertEquals(WiFiIdentifier.EMPTY, WiFiConnection().wiFiIdentifier)
        assertEquals("", WiFiConnection().ipAddress)
        assertEquals(WiFiConnection.LINK_SPEED_INVALID, WiFiConnection().linkSpeed)
        assertFalse(WiFiConnection().connected)
    }

    @Test
    fun testWiFiConnection() {
        // validate
        assertEquals(wiFiIdentifier, fixture.wiFiIdentifier)
        assertEquals(ipAddress, fixture.ipAddress)
        assertEquals(linkSpeed, fixture.linkSpeed)
        assertTrue(fixture.connected)
    }

    @Test
    fun testEquals() {
        // setup
        val wiFiIdentifier = WiFiIdentifier("SSID-123", "BSSID-123")
        val other = WiFiConnection(wiFiIdentifier, "", WiFiConnection.LINK_SPEED_INVALID)
        // execute & validate
        assertEquals(fixture, other)
        assertNotSame(fixture, other)
    }

    @Test
    fun testHashCode() {
        // setup
        val wiFiIdentifier = WiFiIdentifier("SSID-123", "BSSID-123")
        val other = WiFiConnection(wiFiIdentifier, "", WiFiConnection.LINK_SPEED_INVALID)
        // execute & validate
        assertEquals(fixture.hashCode(), other.hashCode())
    }

    @Test
    fun testCompareTo() {
        // setup
        val wiFiIdentifier = WiFiIdentifier("SSID-123", "BSSID-123")
        val other = WiFiConnection(wiFiIdentifier, "", WiFiConnection.LINK_SPEED_INVALID)
        // execute & validate
        assertEquals(0, fixture.compareTo(other))
    }
}
