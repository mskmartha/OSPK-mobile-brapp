package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class ContainerType {
    @Json(name = "BAG") BAG,
    @Json(name = "LOOSE_ITEM") LOOSE_ITEM,
    @Json(name = "TOTE") TOTE,
}
