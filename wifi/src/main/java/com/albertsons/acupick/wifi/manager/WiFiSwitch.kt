package com.albertsons.acupick.wifi.manager

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.os.Build
import com.albertsons.acupick.wifi.utils.buildMinVersionQ
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WiFiSwitch(private val context: Context) : KoinComponent {

    private val wiFiManagerWrapper: WiFiManagerWrapper by inject()

    @TargetApi(Build.VERSION_CODES.Q)
    fun startWiFiSettings() = wiFiManagerWrapper.startWiFiSettings(context as Activity)

    fun on(): Boolean = enable(true)

    fun off(): Boolean = enable(false)

    internal fun minVersionQ(): Boolean = buildMinVersionQ()

    private fun enable(enabled: Boolean): Boolean = if (minVersionQ()) enableWiFiAndroidQ() else enableWiFiLegacy(enabled)

    @TargetApi(Build.VERSION_CODES.Q)
    private fun enableWiFiAndroidQ(): Boolean {
        startWiFiSettings()
        return true
    }

    @Suppress("DEPRECATION")
    private fun enableWiFiLegacy(enabled: Boolean): Boolean = wiFiManagerWrapper.setWifiEnabled(enabled)
}
