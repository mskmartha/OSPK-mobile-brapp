package com.albertsons.acupick.wifi.model

data class WiFiConnection(
    val wiFiIdentifier: WiFiIdentifier = WiFiIdentifier(),
    val ipAddress: String = "",
    val linkSpeed: Int = LINK_SPEED_INVALID
) :
    Comparable<WiFiConnection> {

    val connected: Boolean
        get() = WiFiConnection() != this

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WiFiConnection

        return wiFiIdentifier == other.wiFiIdentifier
    }

    override fun hashCode(): Int = wiFiIdentifier.hashCode()

    override fun compareTo(other: WiFiConnection): Int = wiFiIdentifier.compareTo(other.wiFiIdentifier)

    companion object {
        const val LINK_SPEED_INVALID = -1
    }
}
