package com.albertsons.acupick.wifi.band

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class WiFiChannelsGHZ2Test {
    private val fixture: WiFiChannelsGHZ2 = WiFiChannelsGHZ2()

    @Test
    fun testInRange() {
        assertTrue(fixture.inRange(2400))
        assertTrue(fixture.inRange(2499))
    }

    @Test
    fun testNotInRange() {
        assertFalse(fixture.inRange(2399))
        assertFalse(fixture.inRange(2500))
    }

    @Test
    fun testWiFiChannelByFrequency() {
        assertEquals(1, fixture.wiFiChannelByFrequency(2410).channel)
        assertEquals(1, fixture.wiFiChannelByFrequency(2412).channel)
        assertEquals(1, fixture.wiFiChannelByFrequency(2414).channel)
        assertEquals(6, fixture.wiFiChannelByFrequency(2437).channel)
        assertEquals(7, fixture.wiFiChannelByFrequency(2442).channel)
        assertEquals(13, fixture.wiFiChannelByFrequency(2470).channel)
        assertEquals(13, fixture.wiFiChannelByFrequency(2472).channel)
        assertEquals(13, fixture.wiFiChannelByFrequency(2474).channel)
        assertEquals(14, fixture.wiFiChannelByFrequency(2482).channel)
        assertEquals(14, fixture.wiFiChannelByFrequency(2484).channel)
        assertEquals(14, fixture.wiFiChannelByFrequency(2486).channel)
    }

    @Test
    fun testWiFiChannelByFrequencyNotFound() {
        assertEquals(WiFiChannel.UNKNOWN, fixture.wiFiChannelByFrequency(2399))
        assertEquals(WiFiChannel.UNKNOWN, fixture.wiFiChannelByFrequency(2409))
        assertEquals(WiFiChannel.UNKNOWN, fixture.wiFiChannelByFrequency(2481))
        assertEquals(WiFiChannel.UNKNOWN, fixture.wiFiChannelByFrequency(2481))
        assertEquals(WiFiChannel.UNKNOWN, fixture.wiFiChannelByFrequency(2487))
        assertEquals(WiFiChannel.UNKNOWN, fixture.wiFiChannelByFrequency(2500))
    }

    @Test
    fun testWiFiChannelByChannel() {
        assertEquals(2412, fixture.wiFiChannelByChannel(1).frequency)
        assertEquals(2437, fixture.wiFiChannelByChannel(6).frequency)
        assertEquals(2442, fixture.wiFiChannelByChannel(7).frequency)
        assertEquals(2472, fixture.wiFiChannelByChannel(13).frequency)
        assertEquals(2484, fixture.wiFiChannelByChannel(14).frequency)
    }

    @Test
    fun testWiFiChannelByChannelNotFound() {
        assertEquals(WiFiChannel.UNKNOWN, fixture.wiFiChannelByChannel(0))
        assertEquals(WiFiChannel.UNKNOWN, fixture.wiFiChannelByChannel(15))
    }

    @Test
    fun testWiFiChannelFirst() {
        assertEquals(1, fixture.wiFiChannelFirst().channel)
    }

    @Test
    fun testWiFiChannelLast() {
        assertEquals(14, fixture.wiFiChannelLast().channel)
    }

    @Test
    fun testWiFiChannelPairs() {
        val pair: List<WiFiChannelPair> = fixture.wiFiChannelPairs()
        assertEquals(1, pair.size)
        validatePair(1, 14, pair[0])
    }

    @Test
    fun testWiFiChannelPair() {
        validatePair(1, 14, fixture.wiFiChannelPairFirst(Locale.US.country))
        validatePair(1, 14, fixture.wiFiChannelPairFirst(""))
    }

    private fun validatePair(expectedFirst: Int, expectedSecond: Int, pair: WiFiChannelPair) {
        assertEquals(expectedFirst, pair.first.channel)
        assertEquals(expectedSecond, pair.second.channel)
    }

    @Test
    fun testAvailableChannels() {
        assertEquals(11, fixture.availableChannels(Locale.US.country).size)
        assertEquals(13, fixture.availableChannels(Locale.UK.country).size)
    }

    @Test
    fun testWiFiChannelByFrequency2GHZ() {
        // setup
        val wiFiChannelPair: WiFiChannelPair = fixture.wiFiChannelPairs()[0]
        // execute
        val actual: WiFiChannel = fixture.wiFiChannelByFrequency(2000, wiFiChannelPair)
        // validate
        assertEquals(WiFiChannel.UNKNOWN, actual)
    }

    @Test
    fun testWiFiChannelByFrequency2GHZInRange() {
        // setup
        val wiFiChannelPair: WiFiChannelPair = fixture.wiFiChannelPairs()[0]
        // execute
        val actual: WiFiChannel = fixture.wiFiChannelByFrequency(wiFiChannelPair.first.frequency, wiFiChannelPair)
        // validate
        assertEquals(wiFiChannelPair.first, actual)
    }
}
