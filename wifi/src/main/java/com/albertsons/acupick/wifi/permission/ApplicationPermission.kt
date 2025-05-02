package com.albertsons.acupick.wifi.permission

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.albertsons.acupick.wifi.utils.buildMinVersionM

class ApplicationPermission(
    private val activity: Activity,
) {
    fun check() {
        if (granted()) {
            return
        }
        if (activity.isFinishing) {
            return
        }
        requestPermissions()
    }

    fun granted(requestCode: Int, grantResults: IntArray): Boolean =
        requestCode == REQUEST_CODE && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }

    fun granted(): Boolean = !buildMinVersionM() || grantedAndroidM()

    private fun grantedAndroidM(): Boolean = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(activity.baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(activity, PERMISSIONS, REQUEST_CODE)
    }

    companion object {
        val PERMISSIONS = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
        ).toTypedArray()
        const val REQUEST_CODE = 0x123450
    }
}
