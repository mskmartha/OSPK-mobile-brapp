package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

/**
 * Corresponds to the ItemAddress swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ItemAddressDto(
    @Json(name = "aisleSeq") val aisleSeq: String? = null,
    @Json(name = "bay") val bay: String? = null,
    @Json(name = "deptShortName") val deptShortName: String? = null,
    @Json(name = "level") val level: String? = null,
    @Json(name = "side") val side: String? = null,
) : Parcelable
