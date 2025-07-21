package com.albertsons.acupick.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.request.ConfirmRxPickupRequestDto
import com.albertsons.acupick.data.model.request.PickUpUserRequestDto
import com.albertsons.acupick.data.model.request.RxOrder
import com.albertsons.acupick.data.model.request.ScanContainerWrapperRequestDto
import com.albertsons.acupick.data.model.response.OrderSummary
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
@Keep // needed due to inclusion in the nav graph - see https://developer.android.com/guide/navigation/navigation-pass-data#proguard_considerations
data class HandOffInterstitialParamsList(val list: List<HandOffInterstitialParams>) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
@Keep // needed due to inclusion in the nav graph - see https://developer.android.com/guide/navigation/navigation-pass-data#proguard_considerations
data class HandOffInterstitialParams(
    val activityId: Long,
    val cancelReasonCode: CancelReasonCode?,
    val authenticatedPin: String?,
    val otp: String?,
    val authCodeUnavailableReasonCode: AuthCodeUnavailableReasonCode?,
    val erId: Long,
    val handOffAction: HandOffAction,
    val isIdVerified: Boolean,
    var isPreCompleted: Boolean,
    val confirmOrderTime: ZonedDateTime?,
    val completeOrCancelTime: ZonedDateTime,
    val orderNumber: String,
    val scanContainerWrapperRequestDto: ScanContainerWrapperRequestDto?,
    val confirmRxPickupRequestDto: ConfirmRxPickupRequestDto?,
    val siteId: String,
    var isHandOffReassigned: Boolean = false,
    var isScanContainersCompleted: Boolean = false,
    val orderId: String?,
    val storeNumber: String?,
    val orderStatus: ActivityStatus?,
    val cartType: CartType?,
    val customerArrivalTimestamp: ZonedDateTime?,
    val deliveryCompleteTimestamp: ZonedDateTime?,
    val groceryDestageStartTimestamp: ZonedDateTime?,
    val groceryDestageCompleteTimestamp: ZonedDateTime?,
    val otpCapturedTimestamp: ZonedDateTime?,
    val otpBypassTimestamp: ZonedDateTime?,
    val scheduledPickupTimestamp: ZonedDateTime?,
    val rxOrders: List<RxOrder>?,
    val unableToPickOrder: Boolean?,
    val pickupUserInfoReq: PickUpUserRequestDto?,
    val issuesScanningBag: List<IssuesScanningBag>?,
    val isDugOrder: Boolean = false,
    val giftLabelConfirmation: Boolean?,
    val authCodeFromApi : String?,
    val authCodeFromUserInput : String?,
) : Parcelable

@JsonClass(generateAdapter = false)
@Keep // needed due to inclusion in the nav graph - see https://developer.android.com/guide/navigation/navigation-pass-data#proguard_considerations
enum class HandOffAction {
    COMPLETE,
    CANCEL,
    COMPLETE_WITH_EXCEPTION // case when grocery is success and pharmacy handoff failed
}

// Order Summary Params to show the order summary in the handoff completion screen
@Parcelize
@Keep
data class OrderSummaryParams(
    val orderNumber: String,
    val isCas: Boolean?,
    val orderSummary: List<OrderSummary>,
    val is3p: Boolean?,
    val source: String?
) : Parcelable

@Parcelize
@Keep
data class OrderSummaryParamsList(val list: List<OrderSummaryParams>) : Parcelable
