package com.albertsons.acupick.ui.arrivals.complete

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.HandOffAction
import com.albertsons.acupick.data.model.HandOffInterstitialParamsList
import com.albertsons.acupick.data.model.OrderSummaryParams
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.GamePointsRepository
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.arrivals.pharmacy.PrescriptionReturnData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.HAND_OFF_ALREADY_ASSIGNED_ARG_DATA
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.util.transform
import com.albertsons.acupick.usecase.handoff.CompleteHandoffUseCase
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.time.ZonedDateTime

class HandOffRxInterstitialViewModel(
    val app: Application,
    private val completeHandoffUseCase: CompleteHandoffUseCase,
) : BaseViewModel(app) {

    private val apiCallTimeStamp: GamePointsRepository by inject()
    private val messageDuration = 1500L

    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    private var unableToPickUpOrder: Boolean = false

    val isShowingTroubleMessage = MutableStateFlow(false)
    var handOffAction: LiveData<HandOffAction> = MutableLiveData()
    val showCompleteWithException = handOffAction.transform { handOffAction.value == HandOffAction.COMPLETE_WITH_EXCEPTION }
    val showCancel = handOffAction.transform { handOffAction.value == HandOffAction.CANCEL }
    val closeAction = LiveEvent<Boolean>()
    var isBackToHomeButtonEnable = MutableStateFlow(false)
    var customerArrivalTime: ZonedDateTime? = null
    var otpCapturedOrByPassTime: ZonedDateTime? = null
    var orderNumber: String? = null
    var erId: String = ""
    var scannedBags: PrescriptionReturnData? = null
    var orderSummaryParamsList: List<OrderSummaryParams> = emptyList()
    var totalPoints :LiveData<String> = MutableLiveData()
    // flag to make sure the api not getting called multiple times when we go to new screen and come back
    var isActive: Boolean = false

    init {
        registerCloseAction(SINGLE_ORDER_ERROR_DIALOG_TAG) {
            closeActionFactory(
                positive = { navigateToHome() }
            )
        }
        viewModelScope.launch {
            completeHandoffUseCase.handOffReassigned.collect {
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = HAND_OFF_ALREADY_ASSIGNED_ARG_DATA,
                        tag = SINGLE_ORDER_ERROR_DIALOG_TAG
                    )
                )
            }
        }
        setTotalPoints()
    }

    private fun setTotalPoints() {
        viewModelScope.launch {
            totalPoints.postValue(apiCallTimeStamp.getPoints())
        }
    }

    fun handleHandoffCompletion(params: HandOffInterstitialParamsList, handOffActions: HandOffAction) {
        if (isActive) return
        orderNumber = params.list.firstOrNull()?.orderNumber
        customerArrivalTime = params.list.firstOrNull()?.customerArrivalTimestamp
        otpCapturedOrByPassTime = params.list.firstOrNull()?.otpCapturedTimestamp ?: params.list.firstOrNull()?.otpBypassTimestamp
        unableToPickUpOrder = params.list.firstOrNull()?.unableToPickOrder == true
        erId = params.list.firstOrNull()?.erId.toString()
        handOffAction.set(
            handOffActions
        )
        viewModelScope.launch {
            delay(messageDuration)
            isActive = true
            isShowingTroubleMessage.value = !networkAvailabilityManager.isConnected.first()
            completeHandoffUseCase()
            isBackToHomeButtonEnable.value = true
            isShowingTroubleMessage.value = !networkAvailabilityManager.isConnected.first()
        }
    }

    fun navigateToPrescriptionFragmentOrHome() {
        if (unableToPickUpOrder) {
            _navigationEvent.postValue(NavigationEvent.Back(destinationId = R.id.destageOrderFragment, inclusive = true))
        } else if (handOffAction.value == HandOffAction.CANCEL || handOffAction.value == HandOffAction.COMPLETE_WITH_EXCEPTION) {
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    NavGraphDirections.actionToPrescriptionReturnFragment(
                        erId,
                        scannedBags
                    )
                )
            )
        } else {
            _navigationEvent.postValue(NavigationEvent.Back(destinationId = R.id.destageOrderFragment, inclusive = true))
        }
        closeAction.set(true)
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
}
