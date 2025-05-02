package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class BoxCountPerOrderDto(
    @Json(name = "referenceEntityId") val referenceEntityId: String? = null,
    @Json(name = "boxTypeCount") val boxTypeCount: List<BoxTypeCountDto>? = null
) : Parcelable, Dto
