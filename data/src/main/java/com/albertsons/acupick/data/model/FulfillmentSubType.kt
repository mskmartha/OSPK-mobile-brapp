package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Not currently tied to swagger json directly. Inferred from the /api/searchActivities fulfillment subType response value description
 * from associated documentation at https://confluence.safeway.com/display/EOM/Activity+Search
 */
@JsonClass(generateAdapter = false)
enum class FulfillmentSubType {
    @Json(name = "3PL") THREEPL,
    @Json(name = "1PL") ONEPL,
}
