package com.albertsons.acupick.ui.arrivals.complete

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.viewpager2.widget.ViewPager2
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.CancelReasonCode
import com.albertsons.acupick.data.model.CompleteHandoffData
import com.albertsons.acupick.data.model.HandOffAction
import com.albertsons.acupick.data.model.request.CancelHandoffRequestDto
import com.albertsons.acupick.data.model.request.RemoveItemsRequestDto
import com.albertsons.acupick.data.model.response.OrderStatus
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.CompleteHandoffRepository
import com.albertsons.acupick.data.repository.IdRepository
import com.albertsons.acupick.data.repository.RemoveItemsRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel.Companion.ERROR_DIALOG_TAG
import com.albertsons.acupick.ui.arrivals.destage.OrderCompletionState
import com.albertsons.acupick.ui.arrivals.pharmacy.PrescriptionReturnData
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.dialog.ORDER_DETAILS_CANCEL_ARG_DATA
import com.albertsons.acupick.ui.dialog.RX_ORDER_DETAILS_CANCEL_HANDOFF_ARG_DATA
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.models.SnackBarEvent
import com.albertsons.acupick.ui.util.DrawableIdHelper
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.orFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.io.Serializable

class HandOffPagerViewModel(
    app: Application,
) : BaseViewModel(app) {

    // DI
    private val dispatcher: DispatcherProvider by inject()
    private val apsRepo: ApsRepository by inject()
    private val idRepository: IdRepository by inject()

    private val completeHandoffRepository: CompleteHandoffRepository by inject()
    private val removeItemsRepository: RemoveItemsRepository by inject()

    private val handOffOrderUIList: LiveData<List<HandOffUI>> = MutableLiveData()
    private val rejectedItemsDtoList: MutableLiveData<MutableList<RemoveItemsRequestDto>> = MutableLiveData(mutableListOf())

    private val currentHandOffUIList: List<HandOffUI>
        get() = handOffOrderUIList.value.orEmpty()

    val pendingActionsMapLiveData: LiveData<Map<HandOffUI, HandOffResultData>> = MutableLiveData(emptyMap())
    fun updateCurrentHandoffUi(handOffUI: HandOffUI?) = currentHandoffUI.postValue(handOffUI)
    private val currentHandoffUI: MutableLiveData<HandOffUI?> = MutableLiveData(null)

    private val currentlyIncompleteHandOffUIList: List<HandOffUI>
        get() = currentHandOffUIList.getIncompleteHandOffUIs(pendingActionsMapLiveData.value.orEmpty())

    val restageOrderByOrderNumberEvent: SharedFlow<String> = MutableSharedFlow()

    val isFromNotification = MutableLiveData(false)

    val exitHandOffEvent: MutableSharedFlow<Pair<String, Boolean>> = MutableSharedFlow()

    val isFromPartialPrescriptionReturn = MutableLiveData(false)
    private var isPrescriptionReturnShown = false
    private var scannedBags: PrescriptionReturnData? = null

    // Tab UI
    val tabsLiveData: LiveData<List<HandOffTabUI>> = handOffOrderUIList.map { uiList ->
        uiList.mapIndexed { index, ui ->
            HandOffTabUI(
                tabLabel = ui.tabLabel,
                tabArgument = HandOffPagerFragmentArgs(
                    handOffArgData = HandOffArgData(
                        handOffUIList = uiList,
                        currentHandOffUI = ui,
                        currentHandOffResultData = pendingActionsMapLiveData.value?.getOrDefault(ui, null),
                        currentHandOffIndex = index
                    ),
                    isFromPartialPrescriptionReturn = isFromPartialPrescriptionReturn.value.orFalse()
                )
            )
        }
    }

    private var rxOrderStatus: OrderStatus = OrderStatus.READY_FOR_PU

    // Page event
    val pageEvent = MutableStateFlow(0)

    val activeOrderNumber = MutableLiveData<String>()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getPageEventFlow(viewPager2: ViewPager2): Flow<Int> {
        return callbackFlow {

            val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    trySend(position)
                }
            }

            viewPager2.registerOnPageChangeCallback(pageChangeCallback)

            awaitClose {
                viewPager2.unregisterOnPageChangeCallback(pageChangeCallback)
            }
        }
    }

    val isCompleteList: LiveData<List<OrderCompletionState>> = combine(
        tabsLiveData.asFlow(),
        pendingActionsMapLiveData.asFlow()
    ) { tabs, pendingActionsMap ->
        tabs.map { tab ->
            OrderCompletionState(
                tab.tabArgument.handOffArgData.currentHandOffUI?.orderNumber.orEmpty(),
                isComplete = tab.tabArgument.handOffArgData.currentHandOffUI?.let { pendingActionsMap[it] != null } ?: false
            )
        }
    }.asLiveData()

    init {
        changeToolbarRightSecondExtraImageEvent.postValue(DrawableIdHelper.Id(R.drawable.ic_pick_up_blue))
        viewModelScope.launch(dispatcher.IO) {
            toolbarRightSecondImageEvent.asFlow().collect {
                val incompleteHandOffUIList = currentlyIncompleteHandOffUIList
                // If current working a single handoff or orders
                if (incompleteHandOffUIList.count() == 1) {
                    incompleteHandOffUIList.firstOrNull()?.let {
                        restageOrderByOrderNumberEvent.emit(it.orderNumber)
                    }
                } else {
                    showSelectOrderDialog()
                }
                clearSnackBarEvents()
            }
        }

        viewModelScope.launch(dispatcher.IO) {
            triggerHomeButtonEvent.asFlow().collect {
                handleExitButton()
            }
        }
    }

    init {
        registerCloseAction(HANDOFF_LEAVE_SCREEN_DIALOG_TAG) {
            closeActionFactory(positive = { selection ->
                exitHandOff(selection ?: 0)
            })
        }
        registerCloseAction(HANDOFF_RETURN_PRESCRIPTION_DIALOG_TAG) {
            closeActionFactory(positive = { exitRxHandOff() })
        }
        registerCloseAction(HANDOFF_ORDER_TO_RESTAGE_DIALOG_TAG) {
            closeActionFactory(
                positive = { selection ->
                    viewModelScope.launch {
                        currentlyIncompleteHandOffUIList[selection ?: 0].let {
                            restageOrderByOrderNumberEvent.emit(it.orderNumber)
                        }
                    }
                }
            )
        }

        registerCloseAction(ERROR_DIALOG_TAG) {
            serverErrorListener
        }
    }

    fun switchToOrder(orderNumber: String?) {
        tabsLiveData.value?.indexOfFirst { it.tabArgument.handOffArgData.currentHandOffUI?.orderNumber == orderNumber }?.let {
            if (it != -1) pageEvent.value = it
        }
    }

    private fun showSelectOrderDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.CustomRadioButtons,
                    title = StringIdHelper.Id(R.string.select_order_restage_title),
                    customData = currentlyIncompleteHandOffUIList.map { order -> StringIdHelper.Raw(order.nameAndOrderTypeDisplayString) } as Serializable,
                    positiveButtonText = StringIdHelper.Id(R.string.next),
                    negativeButtonText = StringIdHelper.Id(R.string.close),
                    cancelOnTouchOutside = false
                ),
                tag = HANDOFF_ORDER_TO_RESTAGE_DIALOG_TAG
            )
        )
    }

    fun updateRxOrderStatus(orderStatus: OrderStatus) {
        rxOrderStatus = orderStatus
    }

    fun setHandOffOrderUiList(handOffUiList: List<HandOffUI>?) = handOffOrderUIList.postValue(handOffUiList)
    fun setPickedBagNumbers(pickedBagNumbers: PrescriptionReturnData?) {
        scannedBags = pickedBagNumbers
    }
    fun setPartialPrescriptionInfo(isFromPartialPrescriptionReturn: Boolean) {
        this.isFromPartialPrescriptionReturn.value = isFromPartialPrescriptionReturn
        if (isFromPartialPrescriptionReturn && !isPrescriptionReturnShown) {
            showSnackBar(
                AcupickSnackEvent(
                    message = StringIdHelper.Id(R.string.partial_prescription_returned),
                    type = SnackType.SUCCESS
                )
            )
            isPrescriptionReturnShown = true
        }
    }
    fun updateHandOffState(handOffUI: HandOffUI, result: HandOffResultData) {
        val currentPendingActionsMap = pendingActionsMapLiveData.value.orEmpty()
        val newPendingActionsMap = currentPendingActionsMap + (handOffUI to result)
        pendingActionsMapLiveData.postValue(newPendingActionsMap)
        checkPendingActions(newPendingActionsMap)
    }

    private fun checkPendingActions(actionsMap: Map<HandOffUI, HandOffResultData>) {
        if (currentHandOffUIList.areAllOrdersCompleted(actionsMap)) {
            rejectedItemsDtoList.value?.let {
                viewModelScope.launch(dispatcher.IO) {
                    removeItemsRepository.saveRemoveItemRequest(it)
                }
            }
            if (showRxItems()) {
                navigateToRxInterstitial(actionsMap)
            } else {
                navigateToInterstitial(actionsMap)
            }
        }
    }

    fun updateRejectedItems(removeItemsRequestDtos: MutableList<RemoveItemsRequestDto>) {
        rejectedItemsDtoList.value = rejectedItemsDtoList.value?.apply { addAll(removeItemsRequestDtos) }
    }

    private fun showRxItems() = isFromPartialPrescriptionReturn.value == false && handOffOrderUIList.value?.firstOrNull()?.isRxDug == true &&
        handOffOrderUIList.value?.firstOrNull()?.isPharmacyServicingOrders.orFalse() &&
        rxOrderStatus == OrderStatus.READY_FOR_PU && handOffOrderUIList.value?.firstOrNull()?.rxDeliveryFailedReason?.isEmpty() == true

    fun handleExitButton() {
        if (showRxItems()) {
            showLeavingRxHandoffDialog()
        } else {
            showLeavingHandoffDialog()
        }
    }

    fun handleBackButton() {
        viewModelScope.launch {
            exitHandOffEvent.emit(activeOrderNumber.value to isFromPartialPrescriptionReturn.value.orFalse())
        }
    }

    fun showLeavingHandoffDialog() = inlineDialogEvent.postValue(
        CustomDialogArgDataAndTag(
            data = ORDER_DETAILS_CANCEL_ARG_DATA,
            tag = HANDOFF_LEAVE_SCREEN_DIALOG_TAG
        )
    )

    fun showLeavingRxHandoffDialog() =
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = RX_ORDER_DETAILS_CANCEL_HANDOFF_ARG_DATA,
                tag = HANDOFF_RETURN_PRESCRIPTION_DIALOG_TAG
            )
        )

    private fun exitHandOff(selection: Int) {
        handOffOrderUIList.value?.let {
            viewModelScope.launch(dispatcher.IO) {
                idRepository.clear()
                cancelHandoff(it, selection)
                navigateToSearchOrdersPager()
            }
        }
    }

    private fun exitRxHandOff() {
        handOffOrderUIList.value?.let {
            viewModelScope.launch(dispatcher.IO) {
                idRepository.clear()
                cancelHandoff(it)
                if (!currentHandoffUI.value?.rxDeliveryFailedReason.isNotNullOrEmpty()) navigateToPrescriptionReturnFragment()
                else _navigationEvent.postValue(NavigationEvent.Back(destinationId = R.id.destageOrderFragment, inclusive = true))
            }
        }
    }

    private fun selectionCode(selection: Int): CancelReasonCode {
        return when (selection) {
            0 -> CancelReasonCode.CUSTOMER_NOT_HERE
            1 -> CancelReasonCode.CUSTOMER_ID_INVALID
            2 -> CancelReasonCode.OTHER
            else -> CancelReasonCode.WRONG_HANDOFF
        }
    }
    private suspend fun cancelHandoff(it: List<HandOffUI>, selection: Int = -1) {
        val cancelHandoffReqList = it.map {
            CancelHandoffRequestDto(
                cancelReasonCode = CancelReasonCode.WRONG_HANDOFF, // TODO change when BE chage is ready
                erId = it.erId,
                siteId = it.siteId,
            )
        }
        when (isBlockingUi.wrap { apsRepo.cancelHandoffs(cancelHandoffReqList) }) {
            is ApiResult.Success -> Unit
            is ApiResult.Failure -> {
                withContext(dispatcher.Main) {
                    showSnackBar(
                        SnackBarEvent(
                            prompt = StringIdHelper.Raw("Error cancelling handoff"),
                            cta = null
                        )
                    )
                }
            }
        }
    }

    private fun navigateToSearchOrdersPager() {
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                HandOffPagerFragmentDirections.actionHandOffFragmentToArrivalsOrdersPagerFragment()
            )
        )
    }

    private fun navigateToInterstitial(actionsMap: Map<HandOffUI, HandOffResultData>) {
        viewModelScope.launch(dispatcher.Main) {
            val handOffInterstitialParamsList = actionsMap.toHandOffInterstitialParamsList(isFromPartialPrescriptionReturn.value.orFalse())
            val orderSummaryList = actionsMap.toOrderSummaryList()
            completeHandoffRepository.saveCompleteHandoff(CompleteHandoffData(handOffInterstitialParamsList))
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    HandOffPagerFragmentDirections.actionHandOffFragmentToHandOffInterstitialFragment(
                        handOffInterstitialParamsList = handOffInterstitialParamsList,
                        isFromNotification = isFromNotification.value.orFalse(),
                        orderSummaryParamsList = orderSummaryList
                    )
                )
            )
        }
    }

    private fun navigateToPrescriptionReturnFragment() {
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                HandOffPagerFragmentDirections.actionToPrescriptionReturnFragment(
                    currentHandoffUI.value?.erId.toString(), scannedBags
                )
            )
        )
    }

    private fun navigateToRxInterstitial(actionsMap: Map<HandOffUI, HandOffResultData>) {
        viewModelScope.launch(dispatcher.Main) {
            val handOffInterstitialParamsList = actionsMap.toHandOffInterstitialParamsList()
            val orderSummaryList = actionsMap.toOrderSummaryList()
            completeHandoffRepository.saveCompleteHandoff(CompleteHandoffData(handOffInterstitialParamsList))
            val paramsAction = if (handOffInterstitialParamsList.list.none { it.handOffAction == HandOffAction.COMPLETE } &&
                handOffInterstitialParamsList.list.none { it.handOffAction == HandOffAction.COMPLETE_WITH_EXCEPTION }
            ) {
                HandOffAction.CANCEL
            } else if (handOffInterstitialParamsList.list.any { it.handOffAction == HandOffAction.COMPLETE_WITH_EXCEPTION }) {
                HandOffAction.COMPLETE_WITH_EXCEPTION
            } else {
                HandOffAction.COMPLETE
            }

            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    HandOffPagerFragmentDirections.actionHandOffFragmentToHandOffRxInterstitialFragment(
                        handOffInterstitialParamsList = handOffInterstitialParamsList,
                        handOffAction = paramsAction,
                        pickedBagNumbers = scannedBags,
                        orderSummaryParamsList = orderSummaryList
                    )
                )
            )
        }
    }

    fun setActiveOderNumber(orderNumber: String?) { activeOrderNumber.value = orderNumber }

    companion object {
        const val HANDOFF_RETURN_PRESCRIPTION_DIALOG_TAG = "handoffReturnPrescriptionDialogTag"
        const val HANDOFF_LEAVE_SCREEN_DIALOG_TAG = "handoffLeaveScreenDialogTag"
        const val HANDOFF_ORDER_TO_RESTAGE_DIALOG_TAG = "handoffOrderToRestageDialogTag"
    }
}
