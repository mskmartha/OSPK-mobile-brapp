package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

private const val TOTE_FULL_VALUE = "Tray Full"
private const val PICKING_ANOTHER_ORDER_VALUE = "Picking Another Priority Order"
private const val HANDOFF_ANOTHER_CUSTOMER_VALUE = "Handoff For Another Customer"
private const val OTHER_VALUE = "Other"

@JsonClass(generateAdapter = false)
enum class EndPickReasonCode {
    @Json(name = TOTE_FULL_VALUE) TOTE_FULL,
    @Json(name = PICKING_ANOTHER_ORDER_VALUE) PICKING_ANOTHER_ORDER,
    @Json(name = HANDOFF_ANOTHER_CUSTOMER_VALUE) HANDOFF_CUSTOMER,
    @Json(name = OTHER_VALUE) OTHER,
}
