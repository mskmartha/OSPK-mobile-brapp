package com.albertsons.acupick.ui.arrivals.complete

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.HandOffAction
import com.albertsons.acupick.data.model.HandOffInterstitialParams
import com.albertsons.acupick.data.model.HandOffInterstitialParamsList
import com.albertsons.acupick.data.model.OrderSummaryParams
import com.albertsons.acupick.data.model.OrderSummaryParamsList
import com.albertsons.acupick.data.model.response.PlayerWaitTimeBreakdownDto
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.GamePointsRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.HAND_OFF_ALREADY_ASSIGNED_ARG_DATA
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.usecase.handoff.CompleteHandoffUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import timber.log.Timber
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

const val COMPLETE_HANDOFF_MESSAGE_DURATION_MS = 2000L

class HandOffInterstitialViewModel(
    val app: Application,
    private val completeHandoffUseCase: CompleteHandoffUseCase,
) : BaseViewModel(app) {
    private val apiCallTimeStamp: GamePointsRepository by inject()

    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    private val apsRepository: ApsRepository by inject()
    val dispatcherProvider: DispatcherProvider by inject()
    val isShowingTroubleMessage = MutableStateFlow(false)
    var handOffAction: LiveData<HandOffAction> = MutableLiveData()
    private var isReassigned: Boolean = false
    var isBackToHomeButtonEnable = MutableStateFlow(false)
    var isFromNotification: Boolean = false
    private var orderSummaryParamsList: List<OrderSummaryParams> = emptyList()
    var handOffCompletedItems: List<HandOffCompletedItem> = emptyList()
    var orderNumber: String? = null
    var isDugOrder: Boolean = false
    // flag to make sure the api not getting called multiple times when we go to new screen and come back
    var isActive: Boolean = false
    var totalPoints : LiveData<String> = MutableLiveData()
    var pointsBreakDown : LiveData<PlayerWaitTimeBreakdownDto?> = MutableLiveData()

    init {
        registerCloseAction(SINGLE_ORDER_ERROR_DIALOG_TAG) {
            closeActionFactory(
                positive = { navigateToHome() }
            )
        }
        viewModelScope.launch {
            completeHandoffUseCase.handOffReassigned.collect {
                isReassigned = it
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = HAND_OFF_ALREADY_ASSIGNED_ARG_DATA,
                        tag = SINGLE_ORDER_ERROR_DIALOG_TAG
                    )
                )
            }
        }
        getTotalPoints()
        getPointsBreakDown()
    }


    private fun getTotalPoints() {
        viewModelScope.launch {
            totalPoints.postValue(apiCallTimeStamp.getPoints())
        }
    }
    private fun getPointsBreakDown() {
        Timber.e("setCustomBarData getPointsBreakDown")
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { getPointsBreakDown() }
            } else {

                val result = apsRepository.getTotalGamesPoint()
                when (result) {
                    is ApiResult.Success -> {
                        handleBreakDownData( result.data.playerWaitTimeBreakdown)
                    }
                    is ApiResult.Failure -> {
                        handleApiError(result, retryAction = { getPointsBreakDown() })

                    }
                }.exhaustive
            }
        }
    }

    private fun handleBreakDownData(playerWaitTimeBreakdown: PlayerWaitTimeBreakdownDto?) {
        viewModelScope.launch {
            Timber.e("setCustomBarData call from api")
            pointsBreakDown.postValue(playerWaitTimeBreakdown)
        }
    }

    fun handleHandoffCompletion(params: HandOffInterstitialParamsList, orderSummaryParamsList: OrderSummaryParamsList, isFromNotification: Boolean = false) {
        if (isActive) return
        this.isFromNotification = isFromNotification
        this.orderSummaryParamsList = orderSummaryParamsList.list
        calculatePointsStore( params.list.firstOrNull())
        handOffCompletedItems = params.list.mapNotNull { item ->
            if (item.handOffAction != HandOffAction.CANCEL) {
                HandOffCompletedItem(
                    orderNumber = item.orderNumber,
                    waitingTime = item.customerArrivalTimestamp,
                    otpCapturedOrByPassTime = item.otpCapturedTimestamp ?: item.otpBypassTimestamp ?: ZonedDateTime.now(),
                    isDugOrder = item.isDugOrder

                )
            } else {
                null
            }
        }
        orderNumber = params.list.firstOrNull()?.orderNumber
        isDugOrder = params.list.firstOrNull()?.isDugOrder == true
        val paramsAction =
            if (params.list.none { it.handOffAction == HandOffAction.COMPLETE }) {
                HandOffAction.CANCEL
            } else {
                HandOffAction.COMPLETE
            }
        handOffAction.set(
            paramsAction
        )

        viewModelScope.launch {
            delay(COMPLETE_HANDOFF_MESSAGE_DURATION_MS)
            // pickRepo.clearAllData()
            isActive = true
            isShowingTroubleMessage.value = !networkAvailabilityManager.isConnected.first()
            completeHandoffUseCase()
            isBackToHomeButtonEnable.value = true
            isShowingTroubleMessage.value = !networkAvailabilityManager.isConnected.first()
        }
    }

    fun onViewOrderSummaryClicked(orderNumber: String?) {
        orderSummaryParamsList.find { it.orderNumber == orderNumber }?.let {
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    NavGraphDirections.actionHandoffToOrderSummary(
                        OrderSummaryArg(
                            isCas = it.isCas,
                            orderSummary = it.orderSummary,
                            is3p = it.is3p,
                            source = it.source
                        )
                    )
                )
            )
        }
    }

    fun backToArrivalOrHomeScreen() {
        if (isFromNotification) {
            navigateToHome()
        } else {
            _navigationEvent.postValue(NavigationEvent.Back(destinationId = R.id.destageOrderFragment, inclusive = true))
        }
    }

    private fun calculatePointsStore(data: HandOffInterstitialParams?) {
        if (data == null) return
        viewModelScope.launch {
            var points = totalPoints.value ?: ""
            val endTime = data.otpCapturedTimestamp ?: ZonedDateTime.now()
            val totalMinutes = (ChronoUnit.SECONDS.between(data.customerArrivalTimestamp, endTime) / 60).toInt()
            var pointsToAdd = 0
            val otpCapturedOrByPassTime =data.otpCapturedTimestamp ?: data.otpBypassTimestamp ?: ZonedDateTime.now()
            if (otpCapturedOrByPassTime != null){
                pointsToAdd += 1
            }

            if (handOffAction.value == HandOffAction.COMPLETE_WITH_EXCEPTION ||
                handOffAction.value == HandOffAction.COMPLETE){
                pointsToAdd += 1
            }

            if (totalMinutes <= 2){
                pointsToAdd += 3
            }

            if (totalMinutes in 3..5){
                pointsToAdd += 2
            }
            points += pointsToAdd
            // Total Points logic
            totalPoints.postValue(pointsToAdd.toString())
        }

    }
}

// HandOffCompletedItem stores data of the completed handoff order to be displayed on the Handoff Completion screen
data class HandOffCompletedItem(
    val orderNumber: String?,
    val waitingTime: ZonedDateTime?,
    val otpCapturedOrByPassTime: ZonedDateTime?,
    val isDugOrder: Boolean,
)
