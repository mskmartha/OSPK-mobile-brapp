package com.albertsons.acupick.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class SubstitutionRejectedReason {
    @Json(name = "SWAP") SWAP,
    @Json(name = "SWAP_OOS") SWAP_OOS,
    @Json(name = "SWAP_OTHER_PICKLIST") SWAP_OTHER_PICKLIST,
    @Json(name = "SWAP_OOS_OTHER_PICKLIST") SWAP_OOS_OTHER_PICKLIST
}
