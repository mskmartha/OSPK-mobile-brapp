package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.CartType
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to the CompleteDropOffAndScanReq swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ConfirmRxPickupRequestDto(
    @Json(name = "orderId") val orderId: String? = null,
    @Json(name = "storeNumber") val storeNumber: String? = null,
    @Json(name = "orderStatus") val orderStatus: ActivityStatus? = null,
    @Json(name = "cartType") val cartType: CartType? = null,
    @Json(name = "rxPickupCompleteTimestamp") val rxPickupCompleteTimestamp: ZonedDateTime? = null,
    @Json(name = "rxLocationScanTimestamp") val rxLocationScanTimestamp: ZonedDateTime? = null,
    @Json(name = "rxOrders") val rxOrders: List<RxOrder>? = null
) : Parcelable, Dto
