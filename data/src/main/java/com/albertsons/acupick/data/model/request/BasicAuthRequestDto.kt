package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
class BasicAuthRequestDto(
    @Json(name = "user_id") val userId: String,
    @Json(name = "password") val password: String,
    @Json(name = "grant_type") val grantType: String? = "password",
    @Json(name = "app_id") val appId: String? = "APS_APP",
    @Json(name = "app_name") val appName: String? = "APS_APP",
) : Parcelable, Dto
