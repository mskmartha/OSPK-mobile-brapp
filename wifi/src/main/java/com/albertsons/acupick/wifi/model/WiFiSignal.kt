package com.albertsons.acupick.wifi.model

import com.albertsons.acupick.wifi.band.WiFiBand
import com.albertsons.acupick.wifi.band.WiFiChannel

data class WiFiSignal(
    val primaryFrequency: Int = 0,
    val centerFrequency: Int = 0,
    val wiFiWidth: WiFiWidth = WiFiWidth.MHZ_20,
    val level: Int = 0,
    val is80211mc: Boolean = false,
    val wiFiStandard: WiFiStandard = WiFiStandard.UNKNOWN
) {

    val wiFiBand: WiFiBand = WiFiBand.find(primaryFrequency)

    val frequencyStart: Int
        get() = centerFrequency - wiFiWidth.frequencyWidthHalf

    val frequencyEnd: Int
        get() = centerFrequency + wiFiWidth.frequencyWidthHalf

    val primaryWiFiChannel: WiFiChannel
        get() = wiFiBand.wiFiChannels.wiFiChannelByFrequency(primaryFrequency)

    val centerWiFiChannel: WiFiChannel
        get() = wiFiBand.wiFiChannels.wiFiChannelByFrequency(centerFrequency)

    val strength: Strength
        get() = Strength.calculate(level)

    val distance: String
        get() = String.format("~%.1fm", calculateDistance(primaryFrequency, level))

    fun inRange(frequency: Int): Boolean =
        frequency in frequencyStart..frequencyEnd

    fun channelDisplay(): String {
        val primaryChannel: Int = primaryWiFiChannel.channel
        val centerChannel: Int = centerWiFiChannel.channel
        val channel: String = primaryChannel.toString()
        return if (primaryChannel != centerChannel) "$channel($centerChannel)" else channel
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as WiFiSignal
        if (primaryFrequency != other.primaryFrequency) return false
        if (wiFiWidth != other.wiFiWidth) return false
        return true
    }

    override fun hashCode(): Int = 31 * primaryFrequency + wiFiWidth.hashCode()

    companion object {
        const val FREQUENCY_UNITS = "MHz"
    }
}
