package com.albertsons.acupick.wifi.permission

import android.app.Activity
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionServiceTest {
    private val activity: Activity = mock()
    private val systemPermission: SystemPermission = mock()
    private val applicationPermission: ApplicationPermission = mock()
    private val fixture = PermissionService(activity, systemPermission, applicationPermission)

    @After
    fun tearDown() {
        verifyNoMoreInteractions(activity)
        verifyNoMoreInteractions(applicationPermission)
        verifyNoMoreInteractions(systemPermission)
    }

    @Test
    fun testEnabled() {
        // setup
        whenever(systemPermission.enabled()).thenReturn(true)
        whenever(applicationPermission.granted()).thenReturn(true)
        // execute
        val actual = fixture.enabled()
        // validate
        assertTrue(actual)
        verify(systemPermission).enabled()
        verify(applicationPermission).granted()
    }

    @Test
    fun testEnabledWhenSystemPermissionIsNotEnabled() {
        // setup
        whenever(systemPermission.enabled()).thenReturn(false)
        // execute
        val actual = fixture.enabled()
        // validate
        assertFalse(actual)
        verify(systemPermission).enabled()
    }

    @Test
    fun testEnabledWhenApplicationPermissionAreNotGranted() {
        // setup
        whenever(systemPermission.enabled()).thenReturn(true)
        whenever(applicationPermission.granted()).thenReturn(false)
        // execute
        val actual = fixture.enabled()
        // validate
        assertFalse(actual)
        verify(systemPermission).enabled()
        verify(applicationPermission).granted()
    }

    @Test
    fun testSystemEnabled() {
        // setup
        whenever(systemPermission.enabled()).thenReturn(true)
        // execute
        val actual = fixture.systemEnabled()
        // validate
        assertTrue(actual)
        verify(systemPermission).enabled()
    }

    @Test
    fun testPermissionGranted() {
        // setup
        whenever(applicationPermission.granted()).thenReturn(true)
        // execute
        val actual = fixture.permissionGranted()
        // validate
        assertTrue(actual)
        verify(applicationPermission).granted()
    }

    @Test
    fun testPermissionCheck() {
        // execute
        fixture.check()
        // validate
        verify(applicationPermission).check()
    }

    @Test
    fun testGranted() {
        // setup
        val requestCode = 111
        val results = intArrayOf(1, 2, 3)
        whenever(applicationPermission.granted(requestCode, results)).thenReturn(true)
        // execute
        val actual = fixture.granted(requestCode, results)
        // validate
        assertTrue(actual)
        verify(applicationPermission).granted(requestCode, results)
    }
}
