package com.albertsons.acupick.data.network.auth.token

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.response.AuthUserDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class AccessTokenDto(
    @Json(name = "access_token") val accessToken: String? = "",
    @Json(name = "scope") val scope: String? = "",
    @Json(name = "expires_in") val expiresInSeconds: Int? = 0,
    @Json(name = "refresh_token") val refreshToken: String? = "",
    @Json(name = "token_type") val tokenType: String? = "",
    @Json(name = "jti") val jti: String? = "",
    @Json(name = "user") val user: AuthUserDto? = null
) : Parcelable, Dto {

    internal fun toAccessToken() = AccessToken(
        accessToken = accessToken,
        expiresInSeconds = expiresInSeconds,
        refreshToken = refreshToken,
        tokenType = tokenType
    )
}
