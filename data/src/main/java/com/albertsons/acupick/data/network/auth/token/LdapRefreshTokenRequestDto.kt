package com.albertsons.acupick.data.network.auth.token

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the LdapRefreshTokenReq swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class LdapRefreshTokenRequestDto(
    @Json(name = "refresh_token") val refreshToken: String? = "",
    @Json(name = "grant_type") val grantType: String = "refresh_token",
    @Json(name = "app_id") val appId: String = "APS_APP",
) : Parcelable, Dto
