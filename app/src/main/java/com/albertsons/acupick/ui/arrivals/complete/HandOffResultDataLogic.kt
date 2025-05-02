package com.albertsons.acupick.ui.arrivals.complete

import com.albertsons.acupick.data.model.HandOffAction
import com.albertsons.acupick.data.model.HandOffInterstitialParams
import com.albertsons.acupick.data.model.HandOffInterstitialParamsList
import com.albertsons.acupick.data.model.OrderSummaryParams
import com.albertsons.acupick.data.model.OrderSummaryParamsList
import com.albertsons.acupick.data.model.request.RxOrderStatus
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.models.FulfillmentTypeUI
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.toFulfillmentTypeUI

fun List<HandOffUI>.areAllOrdersCompleted(resultsMap: Map<HandOffUI, HandOffResultData>) = isNotEmpty() && resultsMap.size == size && resultsMap.all { it.value.markedCompleted }

fun List<HandOffUI>.getIncompleteHandOffUIs(resultsMap: Map<HandOffUI, HandOffResultData>) = filterNot { resultsMap[it]?.markedCompleted == true }

fun Map<HandOffUI, HandOffResultData>.toHandOffInterstitialParamsList(isFromPartialPrescriptionReturn: Boolean = false) = HandOffInterstitialParamsList(
    map { (handOffUI, resultData) ->
        HandOffInterstitialParams(
            activityId = handOffUI.activityId,
            cancelReasonCode = resultData.cancelReasonCode,
            authenticatedPin = resultData.userInputAuthCode,
            authCodeUnavailableReasonCode = resultData.authCodeUnavailableReasonCode,
            otp = handOffUI.authenticatedPin,
            erId = handOffUI.erId,
            handOffAction = getStatus(resultData, isFromPartialPrescriptionReturn),
            isIdVerified = resultData.isIdVerified,
            isPreCompleted = handOffUI.isHandOffPreComplete,
            orderNumber = handOffUI.orderNumber,
            scanContainerWrapperRequestDto = handOffUI.scanContainerWrapperRequestDto,
            confirmRxPickupRequestDto = handOffUI.confirmRxPickupRequestDto,
            siteId = handOffUI.siteId,
            issuesScanningBag = handOffUI.issueScanningBags,
            confirmOrderTime = handOffUI.confirmOrderTime,
            completeOrCancelTime = resultData.completeOrCancelTime,
            orderId = handOffUI.orderNumber,
            orderStatus = handOffUI.status,
            storeNumber = handOffUI.siteId,
            cartType = handOffUI.cartType,
            otpCapturedTimestamp = resultData.otpCapturedTimestamp,
            otpBypassTimestamp = resultData.otpByPassTimeStamp,
            deliveryCompleteTimestamp = resultData.completeOrCancelTime,
            customerArrivalTimestamp = handOffUI.startTime,
            groceryDestageStartTimestamp = handOffUI.groceryDestageStartTimestamp,
            groceryDestageCompleteTimestamp = handOffUI.groceryDestageCompleteTimestamp,
            scheduledPickupTimestamp = handOffUI.scheduledPickupTimestamp,
            rxOrders = resultData.rxOrders,
            unableToPickOrder = handOffUI.rxDeliveryFailedReason.isNotNullOrEmpty(),
            pickupUserInfoReq = resultData.pickupUserInfoReq,
            isDugOrder = handOffUI.fulfillmentType?.toFulfillmentTypeUI() == FulfillmentTypeUI.DUG,
            giftLabelConfirmation = handOffUI.isGiftLabelPrinted
        )
    }
)

fun getStatus(resultData: HandOffResultData, isFromPartialPrescriptionReturn: Boolean): HandOffAction {
    val isRxOrder = resultData.rxOrders?.isNotNullOrEmpty()
    return if (isRxOrder.orFalse()) {
        return if (resultData.cancelReasonCode == null && resultData.rxOrders?.getOrNull(0)?.rxOrderStatus == RxOrderStatus.DELIVERY_COMPLETED) HandOffAction.COMPLETE
        else if (resultData.rxOrders?.getOrNull(0)?.rxOrderStatus == RxOrderStatus.DELIVERY_FAILED) {
            // if it is a partial prescription return flow order status is delivery failed
            //  we need to handoff the groceries so it is a Handoff completed action
            if (isFromPartialPrescriptionReturn)
                HandOffAction.COMPLETE
            else
                HandOffAction.COMPLETE_WITH_EXCEPTION
        } else if (resultData.rxOrders?.getOrNull(0)?.rxOrderStatus == RxOrderStatus.DELIVERY_FAILED_NO_PICKUP) HandOffAction.COMPLETE
        else HandOffAction.CANCEL
    } else {
        if (resultData.cancelReasonCode == null) HandOffAction.COMPLETE else HandOffAction.CANCEL
    }
}

// create OrderSummary list to display on the Handoff completed screen
fun Map<HandOffUI, HandOffResultData>.toOrderSummaryList() = OrderSummaryParamsList(mapNotNull { (handOffUI, _) -> orderSummary(handOffUI) })

fun orderSummary(handOffUI: HandOffUI): OrderSummaryParams? {
    return handOffUI.orderSummary?.let {
        OrderSummaryParams(
            orderNumber = handOffUI.orderNumber,
            isCas = handOffUI.isCas,
            orderSummary = it,
            is3p = handOffUI.is3p,
            source = handOffUI.source
        )
    }
}
