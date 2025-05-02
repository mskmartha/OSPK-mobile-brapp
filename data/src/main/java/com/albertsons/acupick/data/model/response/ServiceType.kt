package com.albertsons.acupick.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class ServiceType {
    /** Delivery to customers address */
    @Json(name = "DELIVERY") DELIVERY
}
