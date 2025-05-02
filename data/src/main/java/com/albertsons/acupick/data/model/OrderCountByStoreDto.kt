package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the OrderCountByStoreDto swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class OrderCountByStoreDto(
    @Json(name = "count") val count: Long? = null,
    @Json(name = "countByFulfillmentTypes") val countFulfillmentTypes: List<CountByFulfillmentTypes>? = null,
    @Json(name = "type") val type: OrderByCountType? = null,
) : Parcelable, Dto

@JsonClass(generateAdapter = false)
enum class OrderByCountType {
    /**
     * Open Orders
     */
    @Json(name = "PENDING_TO_STAGE") PENDING_TO_STAGE,

    /**
     * Hand-Offs
     */
    @Json(name = "HAND_OFFS") HAND_OFFS
}
