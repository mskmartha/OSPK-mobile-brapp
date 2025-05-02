package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class CategoryStatus {
    @Json(name = "open")
    OPEN,
    @Json(name = "assigned")
    ASSIGNED,
    @Json(name = "assignedToMe")
    ASSIGNED_TO_ME
}
