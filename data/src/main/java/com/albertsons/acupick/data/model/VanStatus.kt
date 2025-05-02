package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class VanStatus {
    @Json(name = "ARRIVED")
    ARRIVED,

    @Json(name = "ARRIVING")
    ARRIVING,

    @Json(name = "IN_PROGRESS")
    IN_PROGRESS,
}
