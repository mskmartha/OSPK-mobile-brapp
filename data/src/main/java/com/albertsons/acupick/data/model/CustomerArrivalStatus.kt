package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class CustomerArrivalStatus {
    @Json(name = "STORE-NOTIFIED")
    STORE_NOTIFIED,

    @Json(name = "ARRIVED")
    ARRIVED,

    @Json(name = "UNARRIVED")
    UNARRIVED,

    @Json(name = "ETA-SHARED")
    ETA_SHARED,

    @Json(name = "ON-THE-WAY")
    ON_THE_WAY,

    @Json(name = "GEO-FENCE-BROKEN")
    GEO_FENCE_BROKEN,
}
