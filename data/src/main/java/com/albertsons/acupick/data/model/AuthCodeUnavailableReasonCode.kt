package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class AuthCodeUnavailableReasonCode {
    @Json(name = "NO_CUSTOMER_CODE")
    NO_CUSTOMER_CODE,
    @Json(name = "PICKED_UP_BY_SOMEONE_ELSE")
    PICKED_UP_BY_SOMEONE_ELSE,
    @Json(name = "WRONG_CUSTOMER_CODE")
    WRONG_CUSTOMER_CODE,
    @Json(name = "NO_AUTHENTICATION_CODE_PROVIDED")
    NO_AUTHENTICATION_CODE_PROVIDED,
    @Json(name = "CODE_VERIFIED")
    CODE_VERIFIED,
}
