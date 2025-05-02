package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class CancelReasonCode {
    @Json(name = "CUSTOMER_NOT_HERE")
    CUSTOMER_NOT_HERE,
    @Json(name = "CUSTOMER_ID_INVALID")
    CUSTOMER_ID_INVALID,
    @Json(name = "OTHER")
    OTHER,
    @Json(name = "WRONG_HANDOFF")
    WRONG_HANDOFF,
}
