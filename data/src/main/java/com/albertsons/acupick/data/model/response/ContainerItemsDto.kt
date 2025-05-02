package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the ContainerItemsDto swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ContainerItemsDto(
    @Json(name = "imageUrl") val imageUrl: String? = null,
    @Json(name = "itemDesc") val itemDesc: String? = null,
    @Json(name = "itemId") val itemId: String? = null,
    @Json(name = "qty") val qty: Double? = null,
    @Json(name = "regulated") val regulated: Boolean? = null
) : Parcelable, Dto
