package com.albertsons.acupick.wifi.band

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class WiFiChannelsGHZ5Test {
    private val fixture: WiFiChannelsGHZ5 = WiFiChannelsGHZ5()

    @Test
    fun testWiFiChannelByFrequency() {
        validateFrequencyToChannel(5180, 5320, 10, 36, 2)
        validateFrequencyToChannel(5500, 5720, 10, 100, 2)
        validateFrequencyToChannel(5745, 5885, 10, 149, 2)
    }

    private fun validateFrequencyToChannel(frequencyStart: Int, frequencyEnd: Int, frequencyIncrement: Int, channelStart: Int, channelIncrement: Int) {
        var channel = channelStart
        var frequency = frequencyStart
        while (frequency <= frequencyEnd) {
            assertEquals(channel, fixture.wiFiChannelByFrequency(frequency).channel)
            channel += channelIncrement
            frequency += frequencyIncrement
        }
    }

    @Test
    fun testWiFiChannelByFrequencyFail() {
        assertEquals(WiFiChannel.UNKNOWN, fixture.wiFiChannelByFrequency(5149))
        assertEquals(WiFiChannel.UNKNOWN, fixture.wiFiChannelByFrequency(5896))
    }

    @Test
    fun testWiFiChannelFirst() {
        assertEquals(36, fixture.wiFiChannelFirst().channel)
    }

    @Test
    fun testWiFiChannelLast() {
        assertEquals(177, fixture.wiFiChannelLast().channel)
    }

    @Test
    fun testWiFiChannelPair() {
        validatePair(36, 64, fixture.wiFiChannelPairFirst(Locale.JAPAN.country))
        validatePair(36, 64, fixture.wiFiChannelPairFirst(""))
        validatePair(36, 64, fixture.wiFiChannelPairFirst("XYZ"))
    }

    @Test
    fun testWiFiChannelPairs() {
        val wiFiChannelPairs: List<WiFiChannelPair> = fixture.wiFiChannelPairs()
        assertEquals(3, wiFiChannelPairs.size)
        validatePair(36, 64, wiFiChannelPairs[0])
        validatePair(100, 144, wiFiChannelPairs[1])
        validatePair(149, 177, wiFiChannelPairs[2])
    }

    private fun validatePair(expectedFirst: Int, expectedSecond: Int, pair: WiFiChannelPair) {
        assertEquals(expectedFirst, pair.first.channel)
        assertEquals(expectedSecond, pair.second.channel)
    }

    @Test
    fun testWiFiChannelByFrequency5GHZ() {
        // setup
        val wiFiChannelPair: WiFiChannelPair = fixture.wiFiChannelPairs()[1]
        // execute
        val actual: WiFiChannel = fixture.wiFiChannelByFrequency(2000, wiFiChannelPair)
        // validate
        assertEquals(WiFiChannel.UNKNOWN, actual)
    }

    @Test
    fun testWiFiChannelByFrequency5GHZInRange() {
        // setup
        val wiFiChannelPair: WiFiChannelPair = fixture.wiFiChannelPairs()[1]
        // execute
        val actual: WiFiChannel = fixture.wiFiChannelByFrequency(wiFiChannelPair.first.frequency, wiFiChannelPair)
        // validate
        assertEquals(wiFiChannelPair.first, actual)
    }
}
