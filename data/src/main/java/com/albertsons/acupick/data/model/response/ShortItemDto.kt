package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the ShortItemDto swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ShortItemDto(
    @Json(name = "iaId") val iaId: Long? = null,
    @Json(name = "shortageReasonCodes") val shortageReasonCodes: List<ShortedItemUpcDto>? = null,
    @Json(name = "exceptionQty") val exceptionQty: Double? = null
) : Parcelable, Dto
