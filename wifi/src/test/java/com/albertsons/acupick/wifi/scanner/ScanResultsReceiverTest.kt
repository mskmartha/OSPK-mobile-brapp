package com.albertsons.acupick.wifi.scanner

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class ScanResultsReceiverTest {
    private val activity: Activity = mock()
    private val callback: Callback = mock()
    private val intentFilter: IntentFilter = mock()
    private val intent: Intent = mock()
    private val fixture: ScanResultsReceiver = spy(ScanResultsReceiver(activity, callback))

    @Before
    fun setUp() {
        whenever(fixture.makeIntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)).thenReturn(intentFilter)
    }

    @After
    fun tearDown() {
        verifyNoMoreInteractions(activity)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun testRegisterOnce() {
        // execute
        fixture.register()
        // verify
        verify(activity).registerReceiver(fixture, intentFilter)
    }

    @Test
    fun testRegisterMoreThanOnce() {
        // execute
        fixture.register()
        fixture.register()
        // verify
        verify(activity).registerReceiver(fixture, intentFilter)
    }

    @Test
    fun testUnregisterOnce() {
        // setup
        fixture.register()
        // execute
        fixture.unregister()
        // verify
        verify(activity).registerReceiver(fixture, intentFilter)
        verify(activity).unregisterReceiver(fixture)
    }

    @Test
    fun testUnregisterMoreThanOnce() {
        // setup
        fixture.register()
        // execute
        fixture.unregister()
        fixture.unregister()
        // verify
        verify(activity).registerReceiver(fixture, intentFilter)
        verify(activity).unregisterReceiver(fixture)
    }

    @Test
    fun testOnReceiveWithScanResultsAction() {
        // setup
        whenever(intent.action).thenReturn(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        whenever(intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)).thenReturn(true)
        // execute
        fixture.onReceive(activity, intent)
        // verify
        verify(intent).action
        verify(intent).getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
        verify(callback).onSuccess()
    }

    @Test
    fun testOnReceiveWithSomeOtherAction() {
        // setup
        whenever(intent.action).thenReturn(WifiManager.ACTION_PICK_WIFI_NETWORK)
        // execute
        fixture.onReceive(activity, intent)
        // verify
        verify(intent).action
        verify(intent, never()).getBooleanExtra(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean())
        verify(callback, never()).onSuccess()
    }

    @Test
    fun testOnReceiveWithBooleanExtraFalse() {
        // setup
        whenever(intent.action).thenReturn(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        whenever(intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)).thenReturn(false)
        // execute
        fixture.onReceive(activity, intent)
        // verify
        verify(intent).action
        verify(intent).getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
        verify(callback, never()).onSuccess()
    }
}
