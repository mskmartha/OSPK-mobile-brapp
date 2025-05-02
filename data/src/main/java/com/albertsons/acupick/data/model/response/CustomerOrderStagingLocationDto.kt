package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class CustomerOrderStagingLocationDto(
    @Json(name = "erId") val erId: Long? = null,
    @Json(name = "stagingLocation") val stagingLocation: String? = null,
    @Json(name = "toStagingLocation") val toStagingLocation: String? = null,
) : Parcelable, Dto

val CustomerOrderStagingLocationDto.location
    get() = stagingLocation ?: toStagingLocation
