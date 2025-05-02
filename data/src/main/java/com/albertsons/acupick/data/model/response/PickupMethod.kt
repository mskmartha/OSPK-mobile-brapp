package com.albertsons.acupick.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class PickupMethod {
    /** DUG pickup method */
    @Json(name = "DUG") DUG
}
