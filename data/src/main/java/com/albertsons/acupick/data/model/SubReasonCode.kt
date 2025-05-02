package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

private const val ISSUE_SCANNING_TEXT = "issueScanning"
@JsonClass(generateAdapter = false)
enum class SubReasonCode {
    @Json(name = "BAD_QUALITY") BadQuality,
    @Json(name = "ISSUE_SCANNING") IssueScanning,
    @Json(name = "NOT_RELEVANT") NotRelevant,
    @Json(name = "OUT_OF_STOCK") OutOfStock,
    @Json(name = "PRICE_DIFFERENCE_IS_TOO_HIGH") PriceDifferenceIsTooHigh,
}

/**
 * This method is used to pass queryType param string to api/itemDetails API based on
 * the reason code
 */
fun SubReasonCode.text(): String {
    return when (this) {
        SubReasonCode.IssueScanning -> ISSUE_SCANNING_TEXT
        else -> ""
    }
}
