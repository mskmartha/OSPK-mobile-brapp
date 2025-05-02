package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class OrderType {
    @Json(name = "FLASH") FLASH,
    @Json(name = "REGULAR") REGULAR,
    @Json(name = "EXPRESS") EXPRESS,
    @Json(name = "FLASH3P") FLASH3P,
}

fun OrderType?.shouldShowCountdownTimer() =
    this == OrderType.FLASH || this == OrderType.FLASH3P
