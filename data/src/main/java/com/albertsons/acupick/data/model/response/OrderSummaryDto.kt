package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class OrderSummaryDto(
    @Json(name = "imageUrl") val imageUrl: String? = null,
    @Json(name = "itemDesc") val itemDesc: String? = null,
    @Json(name = "qty") val qty: Double? = null,
    @Json(name = "price") val price: Double? = null,
    @Json(name = "upcId") val upcId: String? = null,
    @Json(name = "title") val title: Title? = null,
    @Json(name = "substitutedWith") val substitutedWith: List<OrderSummaryDto?>? = null,
) : Parcelable, Dto

@JsonClass(generateAdapter = false)
enum class Title {
    @Json(name = "OUT_OF_STOCK")
    OUT_OF_STOCK,

    @Json(name = "APPROVED_SUB")
    APPROVED_SUB,

    @Json(name = "DECLINED_SUB")
    DECLINED_SUB,

    @Json(name = "SUBSTITUTION")
    SUBSTITUTION,

    @Json(name = "REST_OF_THE_ITEMS")
    REST_OF_THE_ITEMS,
}
