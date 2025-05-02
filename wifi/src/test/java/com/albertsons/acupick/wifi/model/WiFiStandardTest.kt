package com.albertsons.acupick.wifi.model

import android.net.wifi.ScanResult
import org.junit.Assert.assertEquals
import org.junit.Test

class WiFiStandardTest {
    @Test
    fun testWidth() {
        assertEquals(5, WiFiStandard.values().size)
    }

    @Test
    fun testWiFIStandard() {
        assertEquals(ScanResult.WIFI_STANDARD_UNKNOWN, WiFiStandard.UNKNOWN.wiFiStandardId)
        assertEquals(ScanResult.WIFI_STANDARD_LEGACY, WiFiStandard.LEGACY.wiFiStandardId)
        assertEquals(ScanResult.WIFI_STANDARD_11N, WiFiStandard.N.wiFiStandardId)
        assertEquals(ScanResult.WIFI_STANDARD_11AC, WiFiStandard.AC.wiFiStandardId)
        assertEquals(ScanResult.WIFI_STANDARD_11AX, WiFiStandard.AX.wiFiStandardId)
    }

    @Test
    fun testFindOne() {
        assertEquals(WiFiStandard.UNKNOWN, WiFiStandard.findOne(ScanResult.WIFI_STANDARD_UNKNOWN))
        assertEquals(WiFiStandard.LEGACY, WiFiStandard.findOne(ScanResult.WIFI_STANDARD_LEGACY))
        assertEquals(WiFiStandard.N, WiFiStandard.findOne(ScanResult.WIFI_STANDARD_11N))
        assertEquals(WiFiStandard.AC, WiFiStandard.findOne(ScanResult.WIFI_STANDARD_11AC))
        assertEquals(WiFiStandard.AX, WiFiStandard.findOne(ScanResult.WIFI_STANDARD_11AX))
        assertEquals(WiFiStandard.UNKNOWN, WiFiStandard.findOne(ScanResult.WIFI_STANDARD_UNKNOWN - 1))
        assertEquals(WiFiStandard.UNKNOWN, WiFiStandard.findOne(ScanResult.WIFI_STANDARD_11AX + 1))
    }
}
