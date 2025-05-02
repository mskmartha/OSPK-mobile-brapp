package com.albertsons.acupick.wifi.band

class WiFiChannelsGHZ6 : WiFiChannels(RANGE, SETS) {
    override fun wiFiChannelPairs(): List<WiFiChannelPair> = SETS

    override fun wiFiChannelPairFirst(countryCode: String): WiFiChannelPair =
        if (countryCode.isBlank())
            SET1
        else
            wiFiChannelPairs().find { channelAvailable(countryCode, it.first.channel) }
                ?: SET1

    override fun availableChannels(countryCode: String): List<WiFiChannel> =
        availableChannels(WiFiChannelCountry.find(countryCode).channelsGHZ6())

    override fun channelAvailable(countryCode: String, channel: Int): Boolean =
        WiFiChannelCountry.find(countryCode).channelAvailableGHZ6(channel)

    override fun wiFiChannelByFrequency(frequency: Int, wiFiChannelPair: WiFiChannelPair): WiFiChannel =
        if (inRange(frequency)) wiFiChannel(frequency, wiFiChannelPair) else WiFiChannel.UNKNOWN

    companion object {
        val SET1 = WiFiChannelPair(WiFiChannel(1, 5955), WiFiChannel(29, 6095))
        val SET2 = WiFiChannelPair(WiFiChannel(33, 6115), WiFiChannel(61, 6255))
        val SET3 = WiFiChannelPair(WiFiChannel(65, 6275), WiFiChannel(93, 6415))
        val SET4 = WiFiChannelPair(WiFiChannel(97, 6435), WiFiChannel(125, 6575))
        val SET5 = WiFiChannelPair(WiFiChannel(129, 6595), WiFiChannel(157, 6735))
        val SET6 = WiFiChannelPair(WiFiChannel(161, 6755), WiFiChannel(189, 6895))
        val SET7 = WiFiChannelPair(WiFiChannel(193, 6915), WiFiChannel(229, 7095))
        val SETS = listOf(SET1, SET2, SET3, SET4, SET5, SET6, SET7)
        private val RANGE = WiFiRange(5925, 7125)
    }
}
