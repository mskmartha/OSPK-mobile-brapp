package com.albertsons.acupick.wifi.permission

import android.app.Activity

class PermissionService(
    private val activity: Activity,
    private val systemPermission: SystemPermission = SystemPermission(activity),
    private val applicationPermission: ApplicationPermission = ApplicationPermission(activity)
) {
    fun enabled(): Boolean = systemEnabled() && permissionGranted()

    fun systemEnabled(): Boolean = systemPermission.enabled()

    fun check(): Unit = applicationPermission.check()

    fun granted(requestCode: Int, grantResults: IntArray): Boolean =
        applicationPermission.granted(requestCode, grantResults)

    fun permissionGranted(): Boolean = applicationPermission.granted()
}
