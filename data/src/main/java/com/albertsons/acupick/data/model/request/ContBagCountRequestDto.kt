package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the ContBagCountReq swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ContBagCountRequestDto(
    @Json(name = "bagCount") val bagCount: Int? = null,
    @Json(name = "containerId") val containerId: String? = null,
    @Json(name = "looseItemCount") val looseItemCount: Int? = null,
) : Parcelable, Dto
