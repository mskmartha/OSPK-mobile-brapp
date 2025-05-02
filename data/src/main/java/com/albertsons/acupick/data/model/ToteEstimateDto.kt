package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the ToteEstimate swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ToteEstimateDto(
    @Json(name = "ambient") val amount: Int? = null,
    @Json(name = "chilled") val chilled: Int? = null,
    @Json(name = "frozen") val frozen: Int? = null,
) : Parcelable, Dto
