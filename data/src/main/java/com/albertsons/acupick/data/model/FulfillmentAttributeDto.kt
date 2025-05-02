package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the FulfillmentAttribute swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class FulfillmentAttributeDto(
    @Json(name = "subType") val subType: FulfillmentSubType? = null,
    @Json(name = "type") val type: FulfillmentType? = null,
) : Parcelable, Dto
