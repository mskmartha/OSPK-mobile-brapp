package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class ScanContainerReasonCode {
    @Json(name = "SCAN_EXISTING_LOCATION") SCAN_EXISTING_LOCATION,
    @Json(name = "LOCATION_FULL") LOCATION_FULL
}
