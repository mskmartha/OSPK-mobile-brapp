package com.albertsons.acupick.wifi.band

import com.albertsons.acupick.wifi.utils.toCapitalize
import java.util.Locale
import java.util.SortedSet

internal class WiFiChannelCountryGHZ2 {
    private val countries = setOf("AS", "CA", "CO", "DO", "FM", "GT", "GU", "MP", "MX", "PA", "PR", "UM", "US", "UZ", "VI")
    private val channels = sortedSetOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
    private val world = channels.union(setOf(12, 13)).toSortedSet()

    fun findChannels(countryCode: String): SortedSet<Int> =
        if (countries.contains(countryCode.toCapitalize(Locale.getDefault()))) channels else world
}
