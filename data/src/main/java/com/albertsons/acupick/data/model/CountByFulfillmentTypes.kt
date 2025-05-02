package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the CountByFulfillmentTypes swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class CountByFulfillmentTypes(
    @Json(name = "count") val count: Long?,
    @Json(name = "fulfillmentType") val fulfilmentType: FulfillmentType? = null,
) : Parcelable, Dto
