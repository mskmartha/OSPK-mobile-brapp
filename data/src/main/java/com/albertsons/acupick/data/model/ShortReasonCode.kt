package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

private const val OUT_OF_STOCK_VALUE = "Not In Stock"
private const val TOTE_FULL_VALUE = "Tray Full"
private const val PREP_NOT_READY_VALUE = "Needs Prep"
private const val PICK_LATER_VALUE = "Pick Later"
private const val PRE_PICK_ISSUE_SCANNING_VALUE = "Pre Pick Issue Scan"

@JsonClass(generateAdapter = false)
enum class ShortReasonCode {
    @Json(name = OUT_OF_STOCK_VALUE) OUT_OF_STOCK,
    @Json(name = TOTE_FULL_VALUE) TOTE_FULL,
    @Json(name = PREP_NOT_READY_VALUE) PREP_NOT_READY,
    @Json(name = PICK_LATER_VALUE) PICK_LATER,
    @Json(name = PRE_PICK_ISSUE_SCANNING_VALUE) PRE_PICK_ISSUE_SCANNING,
}

/** Provides the text representation of the given [ShortReasonCode] */
fun ShortReasonCode.textValue(): String {
    return when (this) {
        ShortReasonCode.OUT_OF_STOCK -> OUT_OF_STOCK_VALUE
        ShortReasonCode.TOTE_FULL -> TOTE_FULL_VALUE
        ShortReasonCode.PREP_NOT_READY -> PREP_NOT_READY_VALUE
        ShortReasonCode.PICK_LATER -> PICK_LATER_VALUE
        ShortReasonCode.PRE_PICK_ISSUE_SCANNING -> PRE_PICK_ISSUE_SCANNING_VALUE
    }
}
