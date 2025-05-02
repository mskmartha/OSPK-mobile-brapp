package com.albertsons.acupick.wifi.scanner

import android.app.Activity
import android.os.Handler
import com.albertsons.acupick.wifi.settings.Settings
import com.albertsons.acupick.wifi.manager.WiFiManagerWrapper
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class ScannerServiceTest {
    private val wiFiManagerWrapper: WiFiManagerWrapper = mock()
    private val activity: Activity = mock()
    private val handler: Handler = mock()
    private val settings: Settings = mock()

    @After
    fun tearDown() {
        verifyNoMoreInteractions(wiFiManagerWrapper)
        verifyNoMoreInteractions(activity)
        verifyNoMoreInteractions(handler)
        verifyNoMoreInteractions(settings)
    }

    @Test
    fun testMakeScannerService() {
        // setup
        // execute
        val actual = makeScannerService(activity, wiFiManagerWrapper, handler, settings) as Scanner
        // validate
        assertEquals(wiFiManagerWrapper, actual.wiFiManagerWrapper)
        assertEquals(settings, actual.settings)
        assertNotNull(actual.transformer)
        assertNotNull(actual.periodicScan)
        assertNotNull(actual.scannerCallback)
        assertNotNull(actual.scanResultsReceiver)
        assertFalse(actual.running())
    }
}
