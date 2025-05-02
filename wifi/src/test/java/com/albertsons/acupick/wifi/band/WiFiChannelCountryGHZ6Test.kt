package com.albertsons.acupick.wifi.band

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.SortedSet

class WiFiChannelCountryGHZ6Test {
    private val channelsSet: SortedSet<Int> = sortedSetOf(
        1, 5, 9, 13, 17, 21, 25, 29,
        33, 37, 41, 45, 49, 53, 57, 61,
        65, 69, 73, 77, 81, 85, 89, 93,
        97, 101, 105, 109, 113, 117, 121, 125,
        129, 133, 137, 141, 145, 149, 153, 157,
        161, 165, 169, 173, 177, 181, 185, 189,
        193, 197, 201, 205, 209, 213, 217, 221, 225, 229
    )

    private val fixture = WiFiChannelCountryGHZ6()

    @Test
    fun testChannelsForWorld() {
        listOf("GB", "XYZ", "US", "AU", "AE")
            .forEach { validateChannels(channelsSet, fixture.findChannels(it)) }
    }

    private fun validateChannels(expected: SortedSet<Int>, actual: SortedSet<Int>) {
        assertEquals(expected.size, actual.size)
        assertTrue(actual.containsAll(expected))
    }
}
