package com.albertsons.acupick.usecase.handoff

import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.AuthCodeUnavailableReasonCode
import com.albertsons.acupick.data.model.CancelReasonCode
import com.albertsons.acupick.data.model.HandOffAction
import com.albertsons.acupick.data.model.HandOffInterstitialParams
import com.albertsons.acupick.data.model.request.CancelHandoffRequestDto
import com.albertsons.acupick.data.model.request.PickupCompleteRequestDto
import com.albertsons.acupick.data.model.request.PreCompleteActivityRequestDto
import com.albertsons.acupick.data.model.request.RxOrderStatus
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.CompleteHandoffRepository
import com.albertsons.acupick.domain.AcuPickLoggerInterface
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.logError
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime

class CompleteHandoffUseCase(
    private val completeHandoffRepository: CompleteHandoffRepository,
    private val apsRepository: ApsRepository,
    private val networkAvailabilityManager: NetworkAvailabilityManager,
    private val acuPickLoggerInterface: AcuPickLoggerInterface
) {

    companion object {
        private const val RETRY_DELAY_MS = 5000L
    }

    // Shared Flow to trigger handoff reassigned dialog
    private val _handOffReassigned = MutableSharedFlow<Boolean>()
    val handOffReassigned: SharedFlow<Boolean> = _handOffReassigned

    suspend operator fun invoke() {
        completeHandoffRepository.loadCompleteHandoff()?.handOffInterstitialParamsList?.let { params ->
            coroutineScope {
                params.list.map {
                    it.isScanContainersCompleted = it.scanContainerWrapperRequestDto == null
                    when (it.handOffAction) {
                        HandOffAction.COMPLETE -> async { completeHandOff(it) }
                        HandOffAction.COMPLETE_WITH_EXCEPTION -> async { completeHandOff(it) }
                        HandOffAction.CANCEL -> async { restageOrder(it) }
                    }.exhaustive
                }.forEach { it.await() }
                params.list.getOrNull(0)?.confirmRxPickupRequestDto?.let {
                    if (networkAvailabilityManager.isConnected.first()) {
                        val result = apsRepository.confirmRxPickup(it)
                    }
                }
            }
        }
    }

    private suspend fun completeHandOff(params: HandOffInterstitialParams) {
        var isSuccess = false
        do {
            scanContainers(params)

            // Only continue if scanned containers succeeded
            if (params.isScanContainersCompleted) {
                preCompleteActivity(params)

                // Only continue if pre complete succeeded and online
                if (params.isPreCompleted && networkAvailabilityManager.isConnected.first()) {
                    isSuccess = when (val result = apsRepository.pickupComplete(params.toPickupCompleteRequestDto())) {
                        is ApiResult.Success -> true
                        is ApiResult.Failure -> {
                            params.isHandOffReassigned = isHandOffReassigned(result)
                            isDuplicateRequest(result)
                        }
                    }
                }
            }
            //  Continue looping (after delay) if failed for any reason other than reassigned
        } while ((!isSuccess && !params.isHandOffReassigned).also {
            if (it) delay(RETRY_DELAY_MS)
        }
        )
        completeHandoffRepository.clear()
    }

    private suspend fun restageOrder(params: HandOffInterstitialParams) {
        var isSuccess = false
        do {
            scanContainers(params)

            // Only continue if scanned containers succeeded
            if (params.isScanContainersCompleted) {
                preCompleteActivity(params)

                // Only continue if pre complete succeeded and online
                if (params.isScanContainersCompleted && params.isPreCompleted && networkAvailabilityManager.isConnected.first()) {
                    isSuccess = when (val result = apsRepository.cancelHandoff(params.toCancelHandoffRequestDto())) {
                        is ApiResult.Success -> true
                        is ApiResult.Failure -> {
                            params.isHandOffReassigned = isHandOffReassigned(result)
                            isDuplicateRequest(result)
                        }
                    }
                }
            }
            //  Continue looping (after delay) if failed for any reason other than reassigned
        } while ((!isSuccess && !params.isHandOffReassigned).also {
            if (it) delay(RETRY_DELAY_MS)
        }
        )
        completeHandoffRepository.clear()
    }

    private suspend fun scanContainers(params: HandOffInterstitialParams) {
        params.scanContainerWrapperRequestDto?.let {

            it.actId.toString().logError(
                "Activity Id is null. CompleteHandoffUseCase(scanContainers)," +
                    " Order Id-${params.orderNumber}, storeId-${params.siteId}",
                acuPickLoggerInterface
            )

            if (networkAvailabilityManager.isConnected.first()) {
                if (!params.isScanContainersCompleted) {
                    params.isScanContainersCompleted = when (val result = apsRepository.scanContainers(it)) {
                        is ApiResult.Success -> true
                        is ApiResult.Failure -> {
                            params.isHandOffReassigned = isHandOffReassigned(result)
                            isDuplicateRequest(result)
                        }
                    }
                }
            }
        }
    }

    private suspend fun preCompleteActivity(params: HandOffInterstitialParams) {
        if (networkAvailabilityManager.isConnected.first()) {
            if (!params.isPreCompleted) {
                params.isPreCompleted = when (val result = apsRepository.preCompleteActivity(params.toPreCompleteActivityRequestDto())) {
                    is ApiResult.Success -> true
                    is ApiResult.Failure -> {
                        params.isHandOffReassigned = isHandOffReassigned(result)
                        isDuplicateRequest(result)
                    }
                }
            }
        }
    }

    // //////////////////
    // DTO Converters
    // //////////////////
    private fun HandOffInterstitialParams.toPreCompleteActivityRequestDto() = PreCompleteActivityRequestDto(
        actId = activityId,
        validateMissingContainer = issuesScanningBag.isNullOrEmpty(),
        preCompTime = confirmOrderTime ?: ZonedDateTime.now(),
        scanAllContainers = false,
        issuesScanningBags = issuesScanningBag,
    )

    private fun HandOffInterstitialParams.toPickupCompleteRequestDto() = PickupCompleteRequestDto(
        actId = activityId,
        idVerified = isIdVerified,
        pickUpCompTime = completeOrCancelTime,
        customerCode = authenticatedPin,
        handshakeFailureReason = authCodeUnavailableReasonCode?.name
            ?: if (otp != null && otp == authenticatedPin) AuthCodeUnavailableReasonCode.CODE_VERIFIED.name else null,
        otp = otp,
        orderId = orderId,
        orderStatus = orderStatus,
        cartType = cartType,
        storeNumber = storeNumber,
        customerArrivalTimestamp = customerArrivalTimestamp,
        deliveryCompleteTimestamp = deliveryCompleteTimestamp,
        groceryDestageStartTimestamp = groceryDestageStartTimestamp,
        groceryDestageCompleteTimestamp = groceryDestageCompleteTimestamp,
        otpCapturedTimestamp = otpCapturedTimestamp,
        otpBypassTimestamp = otpBypassTimestamp,
        scheduledPickupTimestamp = scheduledPickupTimestamp,
        rxOrders = rxOrders?.map { if (it.rxOrderStatus == RxOrderStatus.DELIVERY_FAILED_NO_PICKUP) it.copy(rxOrderStatus = RxOrderStatus.DELIVERY_FAILED) else it },
        pickupUserInfoReq = pickupUserInfoReq,
        giftLabelPrintConfirmation = giftLabelConfirmation,
    )

    private fun HandOffInterstitialParams.toCancelHandoffRequestDto() = CancelHandoffRequestDto(
        erId = erId,
        orderNumber = orderNumber,
        siteId = siteId,
        cancelReasonCode = cancelReasonCode,
        cancelReasonText = when (cancelReasonCode) {
            CancelReasonCode.CUSTOMER_NOT_HERE -> "Customer not here"
            CancelReasonCode.CUSTOMER_ID_INVALID -> "Customer ID invalid"
            CancelReasonCode.OTHER -> "Other"
            else -> ""
        },
        cancelTime = completeOrCancelTime,
    )

    // //////////////////
    // Result checks
    // //////////////////
    private suspend fun isHandOffReassigned(failure: ApiResult.Failure): Boolean {
        if (failure !is ApiResult.Failure.Server) return false

        val errorType = failure.error?.errorCode?.resolvedType
        if (errorType == ServerErrorCode.USER_NOT_VALID) {
            _handOffReassigned.emit(true)
            return true
        }
        return false
    }

    /** Returns true if the API failure code indicates that this API call is invalid because it is a duplicate of a successful call */
    private fun isDuplicateRequest(failure: ApiResult.Failure) =
        failure is ApiResult.Failure.Server && (
            failure.error?.errorCode?.resolvedType == ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY ||
                failure.error?.errorCode?.resolvedType == ServerErrorCode.CANNOT_CANCEL_RELEASED_HANDOFF ||
                failure.error?.errorCode?.resolvedType == ServerErrorCode.DUPLICATE_CALL
            )
}
