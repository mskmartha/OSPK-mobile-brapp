package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Corresponds to the ActivityDto.HandshakeType swagger api
 */
@JsonClass(generateAdapter = false)
enum class HandshakeType {
    @Json(name = "OTP") OTP,
    @Json(name = "PAPER") PAPER,
    @Json(name = "SYSTEM") SYSTEM
}
