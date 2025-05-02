package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Corresponds to the ContainerActivityDto.Status swagger api
 */
@JsonClass(generateAdapter = false)
enum class ContainerActivityStatus {
    @Json(name = "EXPECTED") EXPECTED,
    @Json(name = "ASSIGNED") ASSIGNED,
    @Json(name = "PROCESSED") PROCESSED,
    @Json(name = "CANCELLED") CANCELLED,
    @Json(name = "REMOVED") REMOVED,
    @Json(name = "MISSING") MISSING
}
