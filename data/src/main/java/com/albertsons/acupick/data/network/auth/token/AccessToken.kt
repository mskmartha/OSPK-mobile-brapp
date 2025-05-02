package com.albertsons.acupick.data.network.auth.token

import com.albertsons.acupick.data.model.response.AuthUserDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AccessToken(
    @Json(name = "user") val user: AuthUserDto? = null,
    @Json(name = "scope") val scope: String = "",
    @Json(name = "access_token") val accessToken: String? = "",
    @Json(name = "refresh_token") val refreshToken: String? = "",
    @Json(name = "expires_in") val expiresInSeconds: Int? = 0,
    @Json(name = "token_type") val tokenType: String? = ""
)
