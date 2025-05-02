package com.albertsons.acupick.wifi.band

import com.albertsons.acupick.wifi.band.WiFiChannelCountry.Companion.find
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class WiFiChannelCountryTest {
    private val currentLocale: Locale = Locale.getDefault()

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(currentLocale)
    }

    @Test
    fun testChannelAvailableWithGHZ2() {
        assertFalse(find(Locale.US.country).channelAvailableGHZ2(0))
        assertFalse(find(Locale.US.country).channelAvailableGHZ2(12))
        assertFalse(find(Locale.UK.country).channelAvailableGHZ2(0))
        assertFalse(find(Locale.UK.country).channelAvailableGHZ2(14))
    }

    @Test
    fun testChannelAvailableWithGHZ5() {
        assertTrue(find(Locale.US.country).channelAvailableGHZ5(36))
        assertTrue(find(Locale.US.country).channelAvailableGHZ5(165))
        assertTrue(find(Locale.UK.country).channelAvailableGHZ5(36))
        assertTrue(find(Locale.UK.country).channelAvailableGHZ5(140))
        assertTrue(find("AE").channelAvailableGHZ5(36))
        assertTrue(find("AE").channelAvailableGHZ5(64))
    }

    @Test
    fun testChannelAvailableWithGHZ6() {
        assertTrue(find(Locale.US.country).channelAvailableGHZ6(1))
        assertTrue(find(Locale.US.country).channelAvailableGHZ6(93))
        assertTrue(find(Locale.UK.country).channelAvailableGHZ6(1))
        assertTrue(find(Locale.UK.country).channelAvailableGHZ6(93))
        assertTrue(find("AE").channelAvailableGHZ6(1))
        assertTrue(find("AE").channelAvailableGHZ6(93))
    }

    @Test
    fun testFindCorrectlyPopulatesGHZ() {
        // setup
        val expectedCountryCode = Locale.US.country
        val expectedGHZ2: Set<Int> = WiFiChannelCountryGHZ2().findChannels(expectedCountryCode)
        val expectedGHZ5: Set<Int> = WiFiChannelCountryGHZ5().findChannels(expectedCountryCode)
        val expectedGHZ6: Set<Int> = WiFiChannelCountryGHZ6().findChannels(expectedCountryCode)
        // execute
        val actual: WiFiChannelCountry = find(expectedCountryCode)
        // validate
        assertEquals(expectedCountryCode, actual.countryCode())
        assertArrayEquals(expectedGHZ2.toTypedArray(), actual.channelsGHZ2().toTypedArray())
        assertArrayEquals(expectedGHZ5.toTypedArray(), actual.channelsGHZ5().toTypedArray())
        assertArrayEquals(expectedGHZ6.toTypedArray(), actual.channelsGHZ6().toTypedArray())
    }

    @Test
    fun testFindCorrectlyPopulatesCountryCodeAndName() {
        // setup
        val expected = Locale.SIMPLIFIED_CHINESE
        val expectedCountryCode = expected.country
        // execute
        val actual: WiFiChannelCountry = find(expectedCountryCode)
        // validate
        assertEquals(expectedCountryCode, actual.countryCode())
        assertNotEquals(expected.displayCountry, actual.countryName(expected))
        assertEquals(expected.getDisplayCountry(expected), actual.countryName(expected))
    }

    @Test
    fun testCountryName() {
        // setup
        val fixture = WiFiChannelCountry(Locale.US)
        val expected = "United States"
        // execute & validate
        val actual = fixture.countryName(Locale.US)
        // execute & validate
        assertEquals(expected, actual)
    }

    @Test
    fun testCountryNameUnknown() {
        // setup
        val fixture = WiFiChannelCountry(Locale("XYZ"))
        val expected = "-Unknown"
        // execute & validate
        val actual = fixture.countryName(Locale.US)
        // execute & validate
        assertEquals(expected, actual)
    }
}
