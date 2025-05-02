package com.albertsons.acupick.wifi

import com.albertsons.acupick.wifi.band.WiFiBand
import com.albertsons.acupick.wifi.band.WiFiChannelPair
import com.albertsons.acupick.wifi.band.WiFiChannels

const val SIZE_MIN = 1024
const val SIZE_MAX = 4096

class Configuration {
    private var wiFiChannelPair = mutableMapOf<WiFiBand, WiFiChannelPair>()

    var size = SIZE_MAX

    val sizeAvailable: Boolean
        get() = size == SIZE_MAX

    fun wiFiChannelPair(countryCode: String): Unit =
        WiFiBand.values().forEach {
            this.wiFiChannelPair[it] = it.wiFiChannels.wiFiChannelPairFirst(countryCode)
        }

    init {
        WiFiBand.values().forEach {
            this.wiFiChannelPair[it] = WiFiChannels.UNKNOWN
        }
    }
}
