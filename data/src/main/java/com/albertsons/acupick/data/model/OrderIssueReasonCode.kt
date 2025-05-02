package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class OrderIssueReasonCode {
    @Json(name = "BAG_LABELS")
    BAG_LABELS,
    @Json(name = "BAGS_MISSING")
    BAGS_MISSING,
    @Json(name = "TOTE_LABELS")
    TOTE_LABELS,
    @Json(name = "TOTES_MISSING")
    TOTES_MISSING,
    @Json(name = "LOOSE_ITEM_LABELS")
    LOOSE_ITEM_LABELS,
    @Json(name = "LOOSE_ITEMS_MISSING")
    LOOSE_ITEMS_MISSING,
    NONE
}
