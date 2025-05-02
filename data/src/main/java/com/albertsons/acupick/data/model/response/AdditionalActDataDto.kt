package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class AdditionalActDataDto(
    @Json(name = "parkedSpot") val parkedSpot: String?,
    @Json(name = "vehicleDetail") val vehicleDetail: String?,
) : Parcelable, Dto
