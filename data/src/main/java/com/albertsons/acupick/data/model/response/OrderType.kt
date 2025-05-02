package com.albertsons.acupick.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class OrderType {
    /** Grocery DUG order type */
    @Json(name = "grocerydug") GROCERYDUG
}
