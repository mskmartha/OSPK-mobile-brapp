package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class UpdateOnePlArrivalStatusRequestDto(
    @Json(name = "siteId") val siteId: String? = null,
    @Json(name = "vanNumber") val vanNumber: String?,
    @Json(name = "date") val date: String? = null,
    @Json(name = "eventTime") val eventTime: ZonedDateTime?
) : Parcelable, Dto
