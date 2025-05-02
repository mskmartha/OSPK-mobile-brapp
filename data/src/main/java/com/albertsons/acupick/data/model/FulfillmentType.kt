package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Not currently tied to swagger json directly. Inferred from the /api/activitiesInTimeRange fulfillmentType response value description
 * from associated documentation at https://confluence.safeway.com/display/EOM/Activity+Search. Descriptions from https://confluence.safeway.com/display/EOM/Introduction
 */
@JsonClass(generateAdapter = false)
enum class FulfillmentType {
    /** Drive Up and Go == Curbside customer pick up */
    @Json(name = "DUG") DUG,
    /** Delivery to customers address */
    @Json(name = "DELIVERY") DELIVERY,
    /** Shipping wine to customers address */
    @Json(name = "SHIPPING") SHIPPING
}
