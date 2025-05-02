package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class BoxTypeCountDto(
    @Json(name = "boxType") val boxType: BoxTypeDto? = null,
    @Json(name = "count") val count: Int? = null
) : Parcelable, Dto
