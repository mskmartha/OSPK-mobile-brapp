package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class RxDetailsDto(
    @Json(name = "orderId") val orderId: String? = null,
    @Json(name = "orderType") val orderType: OrderType? = null,
    @Json(name = "pickupMethod") val pickupMethod: PickupMethod? = null,
    @Json(name = "requestId") val requestId: String? = null,
    @Json(name = "rxOrderIds") val rxOrderId: List<String>? = null,
    @Json(name = "pharmacyServicingOrders") val pharmacyServicingOrders: Boolean? = null,
    @Json(name = "noOfBags") val noOfBags: Int? = null,
    @Json(name = "orderStatus") val orderStatus: OrderStatus? = null
) : Parcelable, Dto
