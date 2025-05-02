package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.CartType
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to the PickupCompleteReq swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class PickupCompleteRequestDto(
    @Json(name = "customerCode") val customerCode: String? = null,
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "handshakeFailureReason") val handshakeFailureReason: String? = null,
    @Json(name = "handshakeType") val handshakeType: String? = null,
    @Json(name = "rxOrders") val rxOrders: List<RxOrder>? = null,
    @Json(name = "idVerified") val idVerified: Boolean? = null,
    @Json(name = "otp") val otp: String? = null,
    @Json(name = "pickUpCompTime") val pickUpCompTime: ZonedDateTime = ZonedDateTime.now(),

    @Json(name = "orderId") val orderId: String?,
    @Json(name = "storeNumber") val storeNumber: String?,
    @Json(name = "orderStatus") val orderStatus: ActivityStatus?,
    @Json(name = "cartType") val cartType: CartType?,
    @Json(name = "customerArrivalTimestamp") val customerArrivalTimestamp: ZonedDateTime?,
    @Json(name = "deliveryCompleteTimestamp") val deliveryCompleteTimestamp: ZonedDateTime?,
    @Json(name = "groceryDestageStartTimestamp") val groceryDestageStartTimestamp: ZonedDateTime?,
    @Json(name = "groceryDestageCompleteTimestamp") val groceryDestageCompleteTimestamp: ZonedDateTime?,
    @Json(name = "otpCapturedTimestamp") val otpCapturedTimestamp: ZonedDateTime?,
    @Json(name = "otpBypassTimestamp") val otpBypassTimestamp: ZonedDateTime?,
    @Json(name = "scheduledPickupTimestamp") val scheduledPickupTimestamp: ZonedDateTime? = null,
    @Json(name = "pickupUserInfoReq") val pickupUserInfoReq: PickUpUserRequestDto? = null,
    @Json(name = "giftLabelPrintConfirmation") val giftLabelPrintConfirmation: Boolean? = null,
) : Parcelable, Dto
