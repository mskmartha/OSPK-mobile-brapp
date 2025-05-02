
package com.albertsons.acupick.wifi.model

private const val BSSID_LENGTH = 17

data class WiFiVirtual(val bssid: String, val frequency: Int) {
    val key: String
        get() = "$bssid-$frequency"
}

val WiFiDetail.wiFiVirtual: WiFiVirtual
    get() =
        if (BSSID_LENGTH == wiFiIdentifier.bssid.length)
            WiFiVirtual(
                this.wiFiIdentifier.bssid.substring(2, BSSID_LENGTH - 1),
                this.wiFiSignal.primaryFrequency
            )
        else
            WiFiVirtual(
                wiFiIdentifier.bssid,
                wiFiSignal.primaryFrequency
            )
