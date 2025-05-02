package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the PickedItemUpcDto swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class PickItemDto(
    @Json(name = "iaId") val iaId: Long? = null,
    @Json(name = "pickedUpcCodes") val pickedUpcCodes: List<PickedItemUpcDto>? = null,
    @Json(name = "processedQty") val processedQty: Double? = null
) : Parcelable, Dto
