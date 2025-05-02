package com.albertsons.acupick.wifi.model

typealias GroupByKey<T> = (T) -> String

internal val groupByChannel: GroupByKey<WiFiDetail> = { it.wiFiSignal.primaryFrequency.toString() }

internal val groupBySSID: GroupByKey<WiFiDetail> = { it.wiFiIdentifier.ssid }

internal val groupByVirtual: GroupByKey<WiFiDetail> = { it.wiFiVirtual.key }

enum class GroupBy(val sort: Comparator<WiFiDetail>, val group: GroupByKey<WiFiDetail>) {
    NONE(sortByDefault(), groupBySSID),
    SSID(sortBySSID(), groupBySSID),
    CHANNEL(sortByChannel(), groupByChannel),
    VIRTUAL(sortBySSID(), groupByVirtual);

    val none: Boolean
        get() = NONE == this
}
