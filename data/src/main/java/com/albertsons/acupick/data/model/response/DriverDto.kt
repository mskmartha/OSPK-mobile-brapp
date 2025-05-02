package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class DriverDto(
    @Json(name = "firstName") val firstName: String?,
    @Json(name = "lastName") val lastName: String?,
    @Json(name = "phoneNumber") val phoneNumber: String?
) : Dto, Parcelable

fun DriverDto.asFirstInitialDotLastString() =
    "${firstName?.take(1)}. $lastName"
