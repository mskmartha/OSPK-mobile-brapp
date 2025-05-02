package com.albertsons.acupick.wifi.band

import com.albertsons.acupick.wifi.R
import com.albertsons.acupick.wifi.band.WiFiBand.Companion.find
import com.albertsons.acupick.wifi.manager.WiFiManagerWrapper
import org.mockito.kotlin.verifyNoMoreInteractions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class WiFiBandTest {
    private val wiFiManagerWrapper: WiFiManagerWrapper = mock()

    @After
    fun tearDown() {
        verifyNoMoreInteractions(wiFiManagerWrapper)
    }

    @Test
    fun testWiFiBand() {
        assertEquals(3, WiFiBand.values().size)
    }

    @Test
    fun testAvailable() {
        assertTrue(WiFiBand.GHZ2.available.javaClass.isInstance(wifiBandHelper.availableGHZ2))
        assertTrue(WiFiBand.GHZ5.available.javaClass.isInstance(wifiBandHelper.availableGHZ5))
        assertTrue(WiFiBand.GHZ6.available.javaClass.isInstance(wifiBandHelper.availableGHZ6))
    }

    @Test
    fun testTextResource() {
        assertEquals(R.string.wifi_band_2ghz, WiFiBand.GHZ2.textResource)
        assertEquals(R.string.wifi_band_5ghz, WiFiBand.GHZ5.textResource)
        assertEquals(R.string.wifi_band_6ghz, WiFiBand.GHZ6.textResource)
    }

    @Test
    fun testGhz5() {
        assertFalse(WiFiBand.GHZ2.ghz5)
        assertTrue(WiFiBand.GHZ5.ghz5)
        assertFalse(WiFiBand.GHZ6.ghz5)
    }

    @Test
    fun testGhz2() {
        assertTrue(WiFiBand.GHZ2.ghz2)
        assertFalse(WiFiBand.GHZ5.ghz2)
        assertFalse(WiFiBand.GHZ6.ghz2)
    }

    @Test
    fun testGhz6() {
        assertFalse(WiFiBand.GHZ2.ghz6)
        assertFalse(WiFiBand.GHZ5.ghz6)
        assertTrue(WiFiBand.GHZ6.ghz6)
    }

    @Test
    fun testWiFiBandFind() {
        assertEquals(WiFiBand.GHZ2, find(2399))
        assertEquals(WiFiBand.GHZ2, find(2400))
        assertEquals(WiFiBand.GHZ2, find(2499))
        assertEquals(WiFiBand.GHZ2, find(2500))

        assertEquals(WiFiBand.GHZ2, find(4899))
        assertEquals(WiFiBand.GHZ5, find(4900))
        assertEquals(WiFiBand.GHZ5, find(5899))
        assertEquals(WiFiBand.GHZ2, find(5900))

        assertEquals(WiFiBand.GHZ2, find(5924))
        assertEquals(WiFiBand.GHZ6, find(5925))
        assertEquals(WiFiBand.GHZ6, find(7125))
        assertEquals(WiFiBand.GHZ2, find(7126))
    }

    @Test
    fun testAvailableGHZ2() {
        // execute
        val actual = WiFiBand.GHZ2.available()
        // validate
        assertTrue(actual)
    }
}
