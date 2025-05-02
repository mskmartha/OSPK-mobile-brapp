package com.albertsons.acupick.wifi.band

import androidx.annotation.StringRes
import com.albertsons.acupick.wifi.R
import com.albertsons.acupick.wifi.manager.WiFiManagerWrapper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

typealias Available = () -> Boolean

class WiFiBandHelper : KoinComponent {
    val wiFiManagerWrapper: WiFiManagerWrapper by inject()

    internal val availableGHZ2: Available = { true }
    internal val availableGHZ5: Available = { wiFiManagerWrapper.is5GHzBandSupported() }
    internal val availableGHZ6: Available = { wiFiManagerWrapper.is6GHzBandSupported() }
}

val wifiBandHelper = WiFiBandHelper()

enum class WiFiBand(@StringRes val textResource: Int, val wiFiChannels: WiFiChannels, val available: Available) {
    GHZ2(R.string.wifi_band_2ghz, WiFiChannelsGHZ2(), wifiBandHelper.availableGHZ2),
    GHZ5(R.string.wifi_band_5ghz, WiFiChannelsGHZ5(), wifiBandHelper.availableGHZ5),
    GHZ6(R.string.wifi_band_6ghz, WiFiChannelsGHZ6(), wifiBandHelper.availableGHZ6);

    val ghz2: Boolean get() = GHZ2 == this
    val ghz5: Boolean get() = GHZ5 == this
    val ghz6: Boolean get() = GHZ6 == this

    companion object {
        fun find(frequency: Int): WiFiBand = values().find { it.wiFiChannels.inRange(frequency) }
            ?: GHZ2
    }
}
