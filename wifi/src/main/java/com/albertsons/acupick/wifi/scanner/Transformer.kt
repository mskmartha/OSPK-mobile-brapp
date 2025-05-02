package com.albertsons.acupick.wifi.scanner

import android.annotation.TargetApi
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.os.Build
import com.albertsons.acupick.wifi.utils.buildMinVersionM
import com.albertsons.acupick.wifi.utils.buildMinVersionR
import com.albertsons.acupick.wifi.model.ChannelWidth
import com.albertsons.acupick.wifi.model.WiFiConnection
import com.albertsons.acupick.wifi.model.WiFiData
import com.albertsons.acupick.wifi.model.WiFiDetail
import com.albertsons.acupick.wifi.model.WiFiIdentifier
import com.albertsons.acupick.wifi.model.WiFiSignal
import com.albertsons.acupick.wifi.model.WiFiStandard
import com.albertsons.acupick.wifi.model.WiFiStandardId
import com.albertsons.acupick.wifi.model.WiFiWidth
import com.albertsons.acupick.wifi.model.convertIpAddress
import com.albertsons.acupick.wifi.model.convertSSID

internal class Transformer(private val cache: Cache) {

    internal fun transformWifiInfo(): WiFiConnection {
        val wifiInfo: WifiInfo? = cache.wifiInfo()
        return if (wifiInfo == null || wifiInfo.networkId == -1) {
            WiFiConnection()
        } else {
            val ssid = convertSSID(wifiInfo.ssid ?: "")
            val wiFiIdentifier = WiFiIdentifier(ssid, wifiInfo.bssid ?: "")
            WiFiConnection(wiFiIdentifier, convertIpAddress(wifiInfo.ipAddress), wifiInfo.linkSpeed)
        }
    }

    internal fun transformCacheResults(): List<WiFiDetail> =
        cache.scanResults().map { transform(it) }

    internal fun transformToWiFiData(): WiFiData =
        WiFiData(transformCacheResults(), transformWifiInfo())

    internal fun channelWidth(scanResult: ScanResult): ChannelWidth =
        if (minVersionM()) {
            scanResult.channelWidth
        } else {
            WiFiWidth.MHZ_20.channelWidth
        }

    @TargetApi(Build.VERSION_CODES.R)
    internal fun wiFiStandard(scanResult: ScanResult): WiFiStandardId =
        if (minVersionR()) {
            scanResult.wifiStandard
        } else {
            WiFiStandard.UNKNOWN.wiFiStandardId
        }

    internal fun centerFrequency(scanResult: ScanResult, wiFiWidth: WiFiWidth): Int =
        if (minVersionM()) {
            wiFiWidth.calculateCenter(scanResult.frequency, scanResult.centerFreq0)
        } else {
            scanResult.frequency
        }

    internal fun mc80211(scanResult: ScanResult): Boolean = minVersionM() && scanResult.is80211mcResponder

    internal fun minVersionM(): Boolean = buildMinVersionM()

    internal fun minVersionR(): Boolean = buildMinVersionR()

    private fun transform(cacheResult: CacheResult): WiFiDetail {
        val scanResult = cacheResult.scanResult
        val wiFiWidth = WiFiWidth.findOne(channelWidth(scanResult))
        val centerFrequency = centerFrequency(scanResult, wiFiWidth)
        val mc80211 = mc80211(scanResult)
        val wiFiStandard = WiFiStandard.findOne(wiFiStandard(scanResult))
        val wiFiSignal = WiFiSignal(scanResult.frequency, centerFrequency, wiFiWidth, cacheResult.average, mc80211, wiFiStandard)
        val wiFiIdentifier = WiFiIdentifier(
            if (scanResult.SSID == null) "" else scanResult.SSID,
            if (scanResult.BSSID == null) "" else scanResult.BSSID
        )
        return WiFiDetail(
            wiFiIdentifier,
            if (scanResult.capabilities == null) "" else scanResult.capabilities,
            wiFiSignal
        )
    }
}
