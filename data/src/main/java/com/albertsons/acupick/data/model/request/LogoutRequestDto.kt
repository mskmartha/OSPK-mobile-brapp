package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class LogoutRequestDto(
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "app_id") val appId: String = "APS_APP",
    @Json(name = "user_id") val userId: String,
) : Parcelable, Dto
