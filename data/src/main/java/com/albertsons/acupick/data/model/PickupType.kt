package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class PickupType {
    @Json(name = "CUSTOMER") CUSTOMER,
    @Json(name = "DELIVERY_DRIVER") DELIVERY_DRIVER
}
