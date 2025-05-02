package com.albertsons.acupick.wifi.band

data class WiFiChannel(val channel: Int = 0, val frequency: Int = 0) : Comparable<WiFiChannel> {
    fun inRange(value: Int): Boolean =
        value in frequency - ALLOWED_RANGE..frequency + ALLOWED_RANGE

    override fun compareTo(other: WiFiChannel): Int =
        compareBy<WiFiChannel> { it.channel }.thenBy { it.frequency }.compare(this, other)

    companion object {
        val UNKNOWN = WiFiChannel()
        private const val ALLOWED_RANGE = WiFiChannels.FREQUENCY_SPREAD / 2
    }
}
