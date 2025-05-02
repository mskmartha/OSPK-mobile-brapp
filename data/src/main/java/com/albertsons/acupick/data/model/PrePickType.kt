package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class PrePickType {
    @Json(name = "ADVANCE_PICK") ADVANCE_PICK,
    @Json(name = "PRE_PICK") PRE_PICK,
    /**
     * These two below params will not going to use at app side
     * we have to just consume it. These will be considered as a normal order.
     */
    @Json(name = "PREPNOTREADY_PICK") PREPNOTREADY_PICK,
    @Json(name = "RESHOP_PICK") RESHOP_PICK,
}

fun PrePickType?.isAdvancePickOrPrePick() =
    this == PrePickType.ADVANCE_PICK || this == PrePickType.PRE_PICK

fun PrePickType?.isAdvancePick() =
    this == PrePickType.ADVANCE_PICK
