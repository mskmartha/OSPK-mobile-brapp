package com.albertsons.acupick.wifi.band

import com.albertsons.acupick.wifi.utils.toCapitalize
import java.util.Locale
import java.util.SortedSet

private typealias Rules = Pair<Set<String>, Set<Int>>

private fun Rules.find(countryCode: String): Set<Int> =
    if (first.any { it == countryCode }) {
        second
    } else setOf()

private val countriesETSI: Set<String> = setOf(
    "AT", // ETSI Austria
    "BE", // ETSI Belgium
    "CH", // ETSI Switzerland
    "CY", // ETSI Cyprus
    "CZ", // ETSI Czechia
    "DE", // ETSI Germany
    "DK", // ETSI Denmark
    "EE", // ETSI Estonia
    "ES", // ETSI Spain
    "FI", // ETSI Finland
    "FR", // ETSI France
    "GR", // ETSI Greece
    "HU", // ETSI Hungary
    "IE", // ETSI Ireland
    "IS", // ETSI Iceland
    "IT", // ETSI Italy
    "LI", // ETSI Liechtenstein
    "LT", // ETSI Lithuania
    "LU", // ETSI Luxembourg
    "LV", // ETSI Latvia
    "MT", // ETSI Malta
    "NL", // ETSI Netherlands
    "NO", // ETSI Norway
    "PL", // ETSI Poland
    "PT", // ETSI Portugal
    "RO", // ETSI Romania
    "SE", // ETSI Sweden
    "SI", // ETSI Slovenia
    "SK", // ETSI Slovakia
    "IL" // ETSI Israel
)

internal class WiFiChannelCountryGHZ5 {
    private val channelsSet1: Set<Int> = setOf(36, 40, 44, 48, 52, 56, 60, 64)
    private val channelsSet2: Set<Int> = setOf(100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144)
    private val channelsSet3: Set<Int> = setOf(149, 153, 157, 161, 165)
    private val channels: Set<Int> = channelsSet1.union(channelsSet2).union(channelsSet3).toSet()

    private val exclude: List<Rules> = listOf(
        Rules(setOf("AU", "CA"), sortedSetOf(120, 124, 128)),
        Rules(setOf("RU"), sortedSetOf(100, 104, 108, 112, 116, 120, 124, 128)),
        Rules(setOf("CN", "KR"), channelsSet2),
        Rules(setOf("JP", "TR", "ZA"), channelsSet3)
    )

    private val include: List<Rules> = listOf(
        Rules(countriesETSI, sortedSetOf(169, 173)),
        Rules(setOf("US"), sortedSetOf(169, 173, 177))
    )

    fun findChannels(countryCode: String): SortedSet<Int> =
        channels.subtract(exclude.flatMap { it.find(countryCode.toCapitalize(Locale.getDefault())) }.toSet())
            .union(include.flatMap { it.find(countryCode.toCapitalize(Locale.getDefault())) }.toSet())
            .toSortedSet()
}
