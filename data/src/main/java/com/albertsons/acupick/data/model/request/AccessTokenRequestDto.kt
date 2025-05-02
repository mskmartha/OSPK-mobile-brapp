package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class AccessTokenRequestDto(
    @Json(name = "code") val code: String,
    @Json(name = "redirectUri") val redirectUri: String
) : Parcelable, Dto
