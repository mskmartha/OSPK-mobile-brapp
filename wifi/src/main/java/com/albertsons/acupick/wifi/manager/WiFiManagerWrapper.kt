package com.albertsons.acupick.wifi.manager

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.albertsons.acupick.wifi.utils.buildMinVersionR

class WiFiManagerWrapper(
    private val context: Context,
    private val wifiManager: WifiManager,
    private val wiFiSwitch: WiFiSwitch = WiFiSwitch(context)
) {
    fun wiFiEnabled(): Boolean =
        try {
            wifiManager.isWifiEnabled
        } catch (e: Exception) {
            false
        }

    fun enableWiFi(): Boolean =
        try {
            wiFiEnabled() || wiFiSwitch.on()
        } catch (e: Exception) {
            false
        }

    fun disableWiFi(): Boolean =
        try {
            !wiFiEnabled() || wiFiSwitch.off()
        } catch (e: Exception) {
            false
        }

    @Suppress("DEPRECATION")
    fun startScan(): Boolean =
        try {
            wifiManager.startScan()
        } catch (e: Exception) {
            false
        }

    @SuppressLint("MissingPermission")
    fun scanResults(): List<ScanResult> =
        try {
            wifiManager.scanResults ?: listOf()
        } catch (e: Exception) {
            listOf()
        }

    fun wiFiInfo(): WifiInfo? =
        try {
            wifiManager.connectionInfo
        } catch (e: Exception) {
            null
        }

    fun is5GHzBandSupported(): Boolean =
        wifiManager.is5GHzBandSupported

    @TargetApi(Build.VERSION_CODES.R)
    fun is6GHzBandSupported(): Boolean =
        if (minVersionR()) {
            wifiManager.is6GHzBandSupported
        } else {
            false
        }

    fun setWifiEnabled(enabled: Boolean) =
        wifiManager.setWifiEnabled(enabled)

    @TargetApi(Build.VERSION_CODES.Q)
    fun startWiFiSettings(activity: Activity) =
        activity.startActivityForResult(Intent(Settings.Panel.ACTION_WIFI), 0)

    fun minVersionR(): Boolean = buildMinVersionR()
}
