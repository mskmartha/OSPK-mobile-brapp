package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class CartType {
    @Json(name = "WINE") WINE,
    @Json(name = "GROCERYDUG") GROCERYDUG,
}
