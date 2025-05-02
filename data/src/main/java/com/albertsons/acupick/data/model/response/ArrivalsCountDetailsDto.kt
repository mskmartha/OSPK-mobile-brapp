package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ArrivalsCountDetailsDto(
    @Json(name = "siteId") val siteId: Long? = null,
    @Json(name = "customerArrivalCount") val customerArrivalsCount: Int? = null,
) : Parcelable, Dto
