package com.albertsons.acupick.wifi.scanner

import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import com.albertsons.acupick.wifi.manager.WiFiManagerWrapper
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.junit.After
import org.junit.Test

class ScannerCallbackTest {
    private val wiFiManagerWrapper: WiFiManagerWrapper = mock()
    private val cache: Cache = mock()
    private val scanner: Scanner = mock()
    private val wifiInfo: WifiInfo = mock()
    private val scanResults: List<ScanResult> = listOf()
    private val fixture: ScannerCallback = ScannerCallback(wiFiManagerWrapper, cache)

    @After
    fun tearDown() {
        verifyNoMoreInteractions(cache)
        verifyNoMoreInteractions(scanner)
        verifyNoMoreInteractions(wiFiManagerWrapper)
    }

    @Test
    fun testOnSuccess() {
        // setup
        whenever(wiFiManagerWrapper.scanResults()).thenReturn(scanResults)
        whenever(wiFiManagerWrapper.wiFiInfo()).thenReturn(wifiInfo)
        // execute
        fixture.onSuccess()
        // validate
        verify(wiFiManagerWrapper).scanResults()
        verify(wiFiManagerWrapper).wiFiInfo()
        verify(cache).add(scanResults, wifiInfo)
    }
}
