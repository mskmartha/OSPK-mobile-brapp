package com.albertsons.acupick.wifi.scanner

import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import com.albertsons.acupick.wifi.model.BSSID
import com.albertsons.acupick.wifi.model.SSID
import com.albertsons.acupick.wifi.model.WiFiConnection
import com.albertsons.acupick.wifi.model.WiFiDetail
import com.albertsons.acupick.wifi.model.WiFiIdentifier
import com.albertsons.acupick.wifi.model.WiFiStandard
import com.albertsons.acupick.wifi.model.WiFiWidth
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class TransformerTest {
    private val scanResult1 = withScanResult(SSID_1, BSSID_1, WiFiWidth.MHZ_160, WiFiStandard.AX)
    private val scanResult2 = withScanResult(SSID_2, BSSID_2, WiFiWidth.MHZ_80, WiFiStandard.AC)
    private val scanResult3 = withScanResult(SSID_3, BSSID_3, WiFiWidth.MHZ_40, WiFiStandard.N)
    private val cacheResults = listOf(
        CacheResult(scanResult1, scanResult1.level),
        CacheResult(scanResult2, scanResult2.level),
        CacheResult(scanResult3, scanResult2.level)
    )
    private val wifiInfo = withWiFiInfo()
    private val cache: Cache = mock()
    private val fixture = spy(Transformer(cache))

    @After
    fun tearDown() {
        verifyNoMoreInteractions(wifiInfo)
        verifyNoMoreInteractions(cache)
    }

    @Test
    fun testTransformWifiInfo() {
        // setup
        val expected = WiFiConnection(WiFiIdentifier(SSID_1, BSSID_1), IP_ADDRESS, LINK_SPEED)
        whenever(cache.wifiInfo()).thenReturn(wifiInfo)
        // execute
        val actual = fixture.transformWifiInfo()
        // validate
        assertEquals(expected, actual)
        verify(cache).wifiInfo()
        verifyWiFiInfo()
    }

    @Test
    fun testTransformWithNulls() {
        // setup
        whenever(cache.wifiInfo()).thenReturn(null)
        // execute
        val actual = fixture.transformWifiInfo()
        // validate
        assertEquals(WiFiConnection(), actual)
        verify(cache).wifiInfo()
    }

    @Test
    fun testTransformWifiInfoNotConnected() {
        // setup
        whenever(cache.wifiInfo()).thenReturn(wifiInfo)
        whenever(wifiInfo.networkId).thenReturn(-1)
        // execute
        val actual = fixture.transformWifiInfo()
        // validate
        assertEquals(WiFiConnection(), actual)
        verify(wifiInfo).networkId
        verify(cache).wifiInfo()
    }

    @Test
    fun testTransformScanResults() {
        // setup
        doReturn(true).whenever(fixture).minVersionM()
        doReturn(true).whenever(fixture).minVersionR()
        whenever(cache.scanResults()).thenReturn(cacheResults)
        // execute
        val actual = fixture.transformCacheResults()
        // validate
        assertEquals(cacheResults.size, actual.size)
        validateWiFiDetail(SSID_1, BSSID_1, WiFiWidth.MHZ_160, WiFiStandard.AX, actual[0])
        validateWiFiDetail(SSID_2, BSSID_2, WiFiWidth.MHZ_80, WiFiStandard.AC, actual[1])
        validateWiFiDetail(SSID_3, BSSID_3, WiFiWidth.MHZ_40, WiFiStandard.N, actual[2])
        verify(fixture, times(9)).minVersionM()
        verify(fixture, times(3)).minVersionR()
        verify(cache).scanResults()
    }

    @Test
    fun testWiFiData() {
        // setup
        val expectedWiFiConnection = WiFiConnection(WiFiIdentifier(SSID_1, BSSID_1), IP_ADDRESS, LINK_SPEED)
        whenever(cache.wifiInfo()).thenReturn(wifiInfo)
        whenever(cache.scanResults()).thenReturn(cacheResults)
        // execute
        val actual = fixture.transformToWiFiData()
        // validate
        assertEquals(expectedWiFiConnection, actual.wiFiConnection)
        assertEquals(cacheResults.size, actual.wiFiDetails.size)
        verifyWiFiInfo()
        verify(cache).wifiInfo()
        verify(cache).scanResults()
    }

    private fun withScanResult(ssid: SSID, bssid: BSSID, wiFiWidth: WiFiWidth, wiFiStandard: WiFiStandard): ScanResult {
        val scanResult: ScanResult = mock()
        scanResult.SSID = ssid
        scanResult.BSSID = bssid
        scanResult.capabilities = WPA
        scanResult.frequency = FREQUENCY
        scanResult.centerFreq0 = when (wiFiWidth) {
            WiFiWidth.MHZ_20 -> FREQUENCY
            WiFiWidth.MHZ_40 -> FREQUENCY + wiFiWidth.frequencyWidth
            WiFiWidth.MHZ_80 -> FREQUENCY + wiFiWidth.frequencyWidthHalf
            WiFiWidth.MHZ_160 -> FREQUENCY + wiFiWidth.frequencyWidth
            WiFiWidth.MHZ_80_PLUS -> FREQUENCY + wiFiWidth.frequencyWidthHalf
        }
        scanResult.level = LEVEL
        scanResult.channelWidth = wiFiWidth.ordinal
        whenever(scanResult.wifiStandard).thenReturn(wiFiStandard.wiFiStandardId)
        return scanResult
    }

    private fun withWiFiInfo(): WifiInfo {
        val wifiInfo: WifiInfo = mock()
        whenever(wifiInfo.networkId).thenReturn(0)
        whenever(wifiInfo.ssid).thenReturn(SSID_1)
        whenever(wifiInfo.bssid).thenReturn(BSSID_1)
        whenever(wifiInfo.ipAddress).thenReturn(IP_ADDRESS_VALUE)
        whenever(wifiInfo.linkSpeed).thenReturn(LINK_SPEED)
        return wifiInfo
    }

    private fun verifyWiFiInfo() {
        verify(wifiInfo).networkId
        verify(wifiInfo).ssid
        verify(wifiInfo).bssid
        verify(wifiInfo).ipAddress
        verify(wifiInfo).linkSpeed
    }

    private fun validateWiFiDetail(SSID: String, BSSID: String, wiFiWidth: WiFiWidth, wiFiStandard: WiFiStandard, wiFiDetail: WiFiDetail) {
        assertEquals(SSID, wiFiDetail.wiFiIdentifier.ssid)
        assertEquals(BSSID, wiFiDetail.wiFiIdentifier.bssid)
        assertEquals(WPA, wiFiDetail.capabilities)
        with(wiFiDetail.wiFiSignal) {
            assertEquals(wiFiWidth, this.wiFiWidth)
            assertEquals(wiFiStandard, this.wiFiStandard)
            assertEquals(LEVEL, this.level)
            assertEquals(FREQUENCY, this.primaryFrequency)
            assertEquals(FREQUENCY + wiFiWidth.frequencyWidthHalf, this.centerFrequency)
        }
    }

    companion object {
        private const val SSID_1 = "SSID_1-123"
        private const val BSSID_1 = "BSSID_1-123"
        private const val SSID_2 = "SSID_2-123"
        private const val BSSID_2 = "BSSID_2-123"
        private const val SSID_3 = "SSID_3-123"
        private const val BSSID_3 = "BSSID_3-123"
        private const val WPA = "WPA"
        private const val FREQUENCY = 5170
        private const val LEVEL = -40
        private const val IP_ADDRESS_VALUE = 123456789
        private const val IP_ADDRESS = "21.205.91.7"
        private const val LINK_SPEED = 21
    }
}
