package com.albertsons.acupick.wifi.band

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotSame
import org.junit.Test

class WiFiChannelTest {
    private val channel = 1
    private val frequency = 200
    private val fixture: WiFiChannel = WiFiChannel(channel, frequency)
    private val other: WiFiChannel = WiFiChannel(channel, frequency)

    @Test
    fun testInRange() {
        assertTrue(fixture.inRange(frequency))
        assertTrue(fixture.inRange(frequency - 2))
        assertTrue(fixture.inRange(frequency + 2))
        assertFalse(fixture.inRange(frequency - 3))
        assertFalse(fixture.inRange(frequency + 3))
    }

    @Test
    fun testEquals() {
        assertEquals(fixture, other)
        assertNotSame(fixture, other)
    }

    @Test
    fun testHashCode() {
        assertEquals(fixture.hashCode(), other.hashCode())
    }

    @Test
    fun testCompareTo() {
        assertEquals(0, fixture.compareTo(other))
    }
}
