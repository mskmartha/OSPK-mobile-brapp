package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the Dimension swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class DimensionDto(
    @Json(name = "breadth") val breadth: String? = null,
    @Json(name = "height") val height: String? = null,
    @Json(name = "length") val length: String? = null,
    @Json(name = "size") val size: String? = null,
    @Json(name = "uom") val uom: String? = null,
    @Json(name = "weight") val weight: String? = null,
    @Json(name = "weightUOM") val weightUom: String? = null,
    @Json(name = "width") val width: String? = null
) : Parcelable, Dto
