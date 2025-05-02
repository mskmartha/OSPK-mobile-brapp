package com.albertsons.acupick.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class OrderStatus {
    /** New status */
    @Json(name = "NEW") NEW,
    /** Staging status */
    @Json(name = "STAGING") STAGING,
    /** Scanned pickup status */
    @Json(name = "STAGED") STAGED,
    /** Scanned pickup status */
    @Json(name = "CANCELLED") CANCELLED,
    /** Ready for pickup status */
    @Json(name = "READY_FOR_PU") READY_FOR_PU,
    /** Scanned pickup status */
    @Json(name = "SCANNED") SCANNED,
    /** Delivery failed status */
    @Json(name = "DELIVERY_FAILED") DELIVERY_FAILED,
    /** Delivery completed status */
    @Json(name = "DELIVERY_COMPLETED") DELIVERY_COMPLETED,
}
