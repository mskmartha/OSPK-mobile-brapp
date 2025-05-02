package com.albertsons.acupick.wifi.model

import android.net.wifi.ScanResult
import com.albertsons.acupick.wifi.utils.buildMinVersionR

typealias WiFiStandardId = Int

private val unknown: WiFiStandardId = if (buildMinVersionR()) ScanResult.WIFI_STANDARD_UNKNOWN else 0
private val legacy: WiFiStandardId = if (buildMinVersionR()) ScanResult.WIFI_STANDARD_LEGACY else 1
private val n: WiFiStandardId = if (buildMinVersionR()) ScanResult.WIFI_STANDARD_11N else 4
private val ac: WiFiStandardId = if (buildMinVersionR()) ScanResult.WIFI_STANDARD_11AC else 5
private val ax: WiFiStandardId = if (buildMinVersionR()) ScanResult.WIFI_STANDARD_11AX else 6

enum class WiFiStandard(val wiFiStandardId: WiFiStandardId) {
    UNKNOWN(unknown),
    LEGACY(legacy),
    N(n),
    AC(ac),
    AX(ax);

    companion object {
        fun findOne(wiFiStandardId: WiFiStandardId): WiFiStandard =
            values().firstOrNull { it.wiFiStandardId == wiFiStandardId } ?: UNKNOWN
    }
}
