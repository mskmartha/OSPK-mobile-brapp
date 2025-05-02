package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the ErItemDto swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ErItemDto(
    @Json(name = "imageUrl") val imageUrl: String? = null,
    @Json(name = "itemDesc") val itemDesc: String? = null,
    @Json(name = "itemId") val itemId: String? = null,
    @Json(name = "pickedUpcCodes") val pickedUpcCodes: List<PickedItemUpcDto>? = null,
    @Json(name = "qty") val qty: Double? = null,
    @Json(name = "regulated") val regulated: Boolean? = null,
) : Parcelable
