package com.albertsons.acupick.wifi.scanner

import android.os.Handler
import com.albertsons.acupick.wifi.settings.Settings
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.Test

class PeriodicScanTest {
    private val handler: Handler = mock()
    private val settings: Settings = mock()
    private val scanner: ScannerService = mock()
    private val fixture: PeriodicScan = PeriodicScan(scanner, handler, settings)

    @Test
    fun testRun() {
        // setup
        val delayInterval = 1000L
        val scanSpeed = 15
        whenever(settings.scanSpeed()).thenReturn(scanSpeed)
        // execute
        fixture.run()
        // validate
        verify(scanner).update()
        verify(handler).removeCallbacks(fixture)
        verify(handler).postDelayed(fixture, scanSpeed * delayInterval)
    }

    @Test
    fun testStop() {
        // execute
        fixture.stop()
        // validate
        verify(handler).removeCallbacks(fixture)
    }

    @Test
    fun testStart() {
        // setup
        val delayInitial = 1L
        // execute
        fixture.start()
        // validate
        verify(handler).removeCallbacks(fixture)
        verify(handler).postDelayed(fixture, delayInitial)
    }

    @Test
    fun testStartWithDelay() {
        // setup
        val scanSpeed = 15
        whenever(settings.scanSpeed()).thenReturn(scanSpeed)
        // execute
        fixture.startWithDelay()
        // validate
        verify(handler).removeCallbacks(fixture)
        verify(handler).postDelayed(fixture, scanSpeed * PeriodicScan.DELAY_INTERVAL)
        verify(settings).scanSpeed()
    }
}
