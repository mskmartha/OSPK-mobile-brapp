package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the TODO swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class SubstitutionItemDetailsDto(
    @Json(name = "iaId") val iaId: Long? = null,
    @Json(name = "smartSubItemDetails") val smartSubItemDetails: List<ItemActivityDto>? = null,
) : Parcelable, Dto
