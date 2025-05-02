package com.albertsons.acupick.wifi.band

import org.junit.Assert.assertEquals
import org.junit.Test

class WiFiChannelCountryGHZ5Test {
    private val channelsSet1: Set<Int> = setOf(36, 40, 44, 48, 52, 56, 60, 64)
    private val channelsSet2: Set<Int> = setOf(100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144)
    private val channelsSet3: Set<Int> = setOf(149, 153, 157, 161, 165)
    private val fixture = WiFiChannelCountryGHZ5()

    @Test
    fun testChannelsUS() {
        val expected = channelsSet1.union(channelsSet2).union(channelsSet3).union(setOf(169, 173, 177))
        val actual = fixture.findChannels("US")
        assertEquals(expected.size, actual.size)
        assertEquals(expected, actual)
    }
}
