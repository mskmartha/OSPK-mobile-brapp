package com.albertsons.acupick.usecase.handoff

import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.HandOff1PLAction
import com.albertsons.acupick.data.model.HandOff1PLInterstitialParams
import com.albertsons.acupick.data.model.request.Cancel1PLHandoffRequestDto
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.CompleteHandoff1PLRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime

class CompleteHandoff1PLUseCase(
    private val completeHandoffRepository: CompleteHandoff1PLRepository,
    private val apsRepository: ApsRepository,
    private val networkAvailabilityManager: NetworkAvailabilityManager,
) {

    companion object {
        private const val RETRY_DELAY_MS = 5000L
    }

    // Shared Flow to trigger handoff reassigned dialog
    private val _handOffReassigned = MutableSharedFlow<Boolean>()
    val handOffReassigned: SharedFlow<Boolean> = _handOffReassigned

    suspend operator fun invoke() {
        completeHandoffRepository.loadCompleteHandoff()?.let { params ->
            coroutineScope {
                params.handOffInterstitialParamsList.let {
                    when (it.handOffAction) {
                        HandOff1PLAction.COMPLETE -> async { completeHandOff(it) }
                        HandOff1PLAction.CANCEL -> async { restageOrder(it) }
                    }.await()
                }
            }
        }
    }

    private suspend fun completeHandOff(params: HandOff1PLInterstitialParams) {
        var isSuccess = false
        do {
            if (networkAvailabilityManager.isConnected.first()) {
                isSuccess = when (val result = apsRepository.complete1PLPickup(params.removeItems1PLRequestDto)) {
                    is ApiResult.Success -> true
                    is ApiResult.Failure -> {
                        params.isHandOffReassigned = isHandOffReassigned(result)
                        isDuplicateRequest(result)
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

    private suspend fun restageOrder(params: HandOff1PLInterstitialParams) {
        var isSuccess = false
        do {
            if (networkAvailabilityManager.isConnected.first()) {
                isSuccess = when (
                    val result = apsRepository.cancel1PLHandoff(
                        Cancel1PLHandoffRequestDto(
                            activityId = params.actId,
                            unassignTime = ZonedDateTime.now()
                        )
                    )
                ) {
                    is ApiResult.Success -> true
                    is ApiResult.Failure -> {
                        params.isHandOffReassigned = isHandOffReassigned(result)
                        isDuplicateRequest(result)
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

    // //////////////////
    // Result checks
    // //////////////////
    private suspend fun isHandOffReassigned(failure: ApiResult.Failure): Boolean {
        if (failure !is ApiResult.Failure.Server) return false

        val errorType = failure.error?.errorCode?.resolvedType
        if (errorType == ServerErrorCode.USER_NOT_VALID) {
            _handOffReassigned.emit(true)
            return true
        } else if (errorType == ServerErrorCode.REJECTED_REMOVAL_COMPLETED) return true
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
