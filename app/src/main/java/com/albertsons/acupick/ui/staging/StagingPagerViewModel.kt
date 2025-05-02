package com.albertsons.acupick.ui.staging

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.StagingOneData
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.Tote
import com.albertsons.acupick.data.model.request.ContBagCountRequestDto
import com.albertsons.acupick.data.model.request.UpdateErBagRequestDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.CannotAssignToOrderDialogTypes
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.StagingStateRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel.Companion.RETRY_ERROR_DIALOG_TAG
import com.albertsons.acupick.ui.arrivals.destage.OrderCompletionState
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.models.StagingTabUI
import com.albertsons.acupick.ui.models.StagingUI
import com.albertsons.acupick.ui.models.ToteUI
import com.albertsons.acupick.ui.staging.print.PrintLabelsHeaderUi
import com.albertsons.acupick.ui.util.DrawableIdHelper
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.displayName
import com.albertsons.acupick.ui.util.notZeroOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.util.Collections

enum class NavDirection {
    BACK,
    NEXT
}

// TODO - need to survive app globally and process death, writing to bundle is best bet for both?
class StagingPagerViewModel(app: Application) : BaseViewModel(app) {
    // DI
    private val dispatcherProvider: DispatcherProvider by inject()
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    private val pickRepository: PickRepository by inject()
    private val apsRepo: ApsRepository by inject()
    private val siteRepo: SiteRepository by inject()
    private val stagingStateRepo: StagingStateRepository by inject()

    // input flows
    val activityId = MutableStateFlow("")
    val toteLabelsPrintedSuccessfully = MutableStateFlow(false)
    var shouldClearStagingData: Boolean = false

    val currentOrderToteUiList: MutableLiveData<MutableList<ToteUI>> = MutableLiveData(mutableListOf())

    // Tote count info acts as input event to populate toteCountMap
    val toteCountInfo = MutableStateFlow<Pair<String, List<StagingToteDbViewModel>>>(Pair("", emptyList()))
    private val toteCountMap = HashMap<String, List<StagingToteDbViewModel>>()

    // Cached state data
    private var stagingOneData: StagingOneData

    // The direction of the last page navigation using custom PREV/NEXT buttons
    var lastNavEvent: NavDirection? = null

    // This event acts as a relay that allows each page of VM to signal hosting PagerFragment via shared VM
    val backEvent = MutableSharedFlow<Unit>()
    val advanceEvent = MutableSharedFlow<Unit>()
    val orderCompletionStateChangeEvent = MutableSharedFlow<Unit>()

    // The way this attribute is set
    // it is intended only to be used for focus calculations
    // It doesn't change if the keyboard isn't showing when the user selects a tab.
    val activeOrderNumberForFocus = MutableSharedFlow<String>()

    // Grouped API results
    private val orderMap: LiveData<Map<StagingUI, List<ToteUI>>> = MutableLiveData()
    private val labelHeaderUi: LiveData<List<PrintLabelsHeaderUi>?> = MutableLiveData()

    val completeOrderFlow = MutableSharedFlow<OrderCompletionState?>()

    val isCompleteList = toteCountInfo.asLiveData().map {
        it.second.filter { dbVm -> dbVm.stagingUI?.orderNumber != null }
            .map { dbVm -> OrderCompletionState(dbVm.stagingUI?.orderNumber!!) }
    }

    // Tab UI
    val tabs: LiveData<List<StagingTabUI>> = orderMap.map { orders ->
        orders.map {
            StagingTabUI(
                tabLabel = it.key.customerFirstInitialDotLast,
                tabArgument = StagingPagerFragmentArgs(
                    activityId = activityId.value,
                    isPreviousPrintSuccessful = toteLabelsPrintedSuccessfully.value,
                    orderNumber = it.key.orderNumber ?: "",
                    shouldClearData = shouldClearStagingData
                )
            )
        }
    }

    // UI
    val isKeyboardVisible = MutableLiveData(false)
    val areAllOrdersCompleted: LiveData<Boolean> = MutableLiveData(false)
    val isLastTab: LiveData<Boolean> = MutableLiveData()
    val isMfc: LiveData<Boolean> = MutableLiveData()
    private val isCustomerBagPreference: LiveData<Boolean> = MutableLiveData()
    private val isAllCustomerPreferNoBag: LiveData<Boolean> = MutableLiveData()
    val isAnyCustomerPreferNoBag: LiveData<Boolean> = MutableLiveData()

    init {
        // appease lateinit definition with a reasonable default
        stagingOneData = StagingOneData(savedTitle = String(), activityId = -1L, nextActivityId = null, bagLabelsPrintedSuccessfully = false, emptyMap(), false)
        viewModelScope.launch(dispatcherProvider.IO) {
            toteCountInfo.collect { (key, list) ->
                toteCountMap[key] = list
            }
        }
        handledSavedPartialStageData()
        listenForAllOrdersCompleted()
        showReloadDialogIfNoConnectionAtStartup()
        showReprintToteLabelDialogOnButtonClick()
        navigateHomeOnBackButtonClick()
        setToolbarTitleUsingOrder(app)
        updateTabIcons()
    }

    fun onPagerPageSeleceted(position: Int, isKeyboardVisible: Boolean) {
        val orderNumberOnCurrentTab = tabs.value?.get(position)?.tabArgument?.orderNumber
        viewModelScope.launch {
            handleHowPageChangeAffectsFocus(isKeyboardVisible, orderNumberOnCurrentTab)
        }
        isLastTab.postValue(position + 1 == tabs.value?.size)
    }

    private suspend fun handleHowPageChangeAffectsFocus(isKeyboardVisible: Boolean, orderNumberOnCurrentTab: String?) {
        if (isKeyboardVisible) {
            orderNumberOnCurrentTab?.let {
                activeOrderNumberForFocus.emit(it)
            }
            lastNavEvent = NavDirection.NEXT
        }
    }

    private fun showReloadDialogIfNoConnectionAtStartup() {
        // Display reloadDialog if network is not connected on startup
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                showReloadDialog()
            }
        }
    }

    private fun updateTabIcons() {
        viewModelScope.launch {
            completeOrderFlow.collect { isCompleteUpdateEvent ->
                val dropList = isCompleteList.value?.filter { it.customerOrderNumber == isCompleteUpdateEvent?.customerOrderNumber }
                isCompleteList.set(
                    isCompleteList.value
                        ?.toMutableList()
                        ?.apply {
                            removeAll(dropList ?: emptyList())
                            isCompleteUpdateEvent?.let(::add)
                        }
                )
            }
        }
    }

    private fun handledSavedPartialStageData() {
        // Combine connection status and activity Id to make sure we always end up making API call.
        viewModelScope.launch(dispatcherProvider.IO) {
            combine(networkAvailabilityManager.isConnected, activityId.filterNot { it == "" }) { isConnected, isNotEmptyString -> Pair(isConnected, isNotEmptyString) }
                // Show reload dialog when disconnected
                .onEach { (connected, _) -> if (!connected) showReloadDialog() }
                // When connected, perform API lookup
                .filter { it.first }.collect { (_, _) ->
                    when (
                        val result = isBlockingUi.wrap {
                            pickRepository.getActivityDetails(
                                activityId.value.also {
                                    it.logError(
                                        "Activity ID Is empty. StagingPagerViewModel(handledSavedPartialStageData), " +
                                            "Order Id-${pickRepository.pickList.first()?.customerOrderNumber}"
                                    )
                                }
                            )
                        }
                    ) {
                        is ApiResult.Success -> {
                            restoredSavedStagingData(result)
                        }

                        is ApiResult.Failure -> {
                            handleApiError(errorType = result)
                        }
                    }
                }
        }
    }

    private fun restoredSavedStagingData(result: ApiResult.Success<ActivityDto>) {
        if (shouldClearStagingData) {
            stagingStateRepo.clear()
        }
        // ACUPICK-836
        // load/seed staging part one data
        // if we don't have it, we won't have staging part two data either
        stagingStateRepo.loadStagingPartOne(activityId.value)?.let {
            // only overwrite the default if we found the cached version
            stagingOneData = it
        }

        // This is checking for multi-source at the actiivty level. This should work because
        // we should not be mixing multi-source and non-multisource orders in a batch. If we were to start doing that
        // we would need to start getting it at the order level
        stagingOneData.isMultiSource = result.data.isMultiSource ?: false
        isMfc.postValue(result.data.isMultiSource ?: false)
        isCustomerBagPreference.postValue(result.data.isCustomerBagPreference ?: true)
        isAllCustomerPreferNoBag.postValue(result.data.containerActivities?.all { !(it.isCustomerBagPreference ?: true) })
        isAnyCustomerPreferNoBag.postValue(result.data.containerActivities?.any { !(it.isCustomerBagPreference ?: true) })

        // ACUPICK-896
        // if we cached values from staging part two, go directly there
        var skipStagingPartOne = false
        stagingOneData.nextActivityId?.let { savedNextActivityId ->
            if (savedNextActivityId != -1L) {
                val anyCustomerId = stagingOneData.totesByToteIdMap.values.first().customerOrderNumber
                stagingStateRepo.loadStagingPartTwo(anyCustomerId, savedNextActivityId.toString())?.let { _ ->
                    skipStagingPartOne = true
                }
            }
        }

        // Group containers by unique info, then map to ToteUI
        val groupedData = result.data.containerActivities?.groupBy { StagingUI(result.data, it) }
            ?.mapValues { entry ->
                entry.value.map { tote ->
                    // ACUPICK-836 retrieve saved bag/loose counts if available
                    val toteMap: Map<String, Tote> = stagingOneData.totesByToteIdMap
                    val savedTote = toteMap[tote.containerId]
                    val bagCount = savedTote?.bagCount
                    val looseCount = savedTote?.looseItemCount
                    ToteUI(tote, bagCount, looseCount)
                }
            } ?: emptyMap()

        orderMap.postValue(groupedData)
        // TODO Redesign Commenting as labelHeaderUi live data is not used
        /*val listData = groupedData.map { entries ->
            PrintLabelsHeaderUi(toteUiList = entries.value, customerOrderNumber = entries.value.first().customerOrderNumber, singleOrder = groupedData.count() == 1, customeName  = entries.value
                .first().customerName )
        }
        labelHeaderUi.postValue(listData)*/

        if (skipStagingPartOne) {

            if (!needToPrintBagLabels(stagingOneData)) {
                navigateToPart2()
            } else {
                showPrintBagLabelDialog()
            }
        }
    }

    private fun setToolbarTitleUsingOrder(app: Application) {
        // Use first entry to set title
        viewModelScope.launch(dispatcherProvider.IO) {
            orderMap.asFlow().collect {
                (it.entries.firstOrNull()?.key)?.let { stagingUI ->
                    showStagingTimeOnTitle(
                        stagingUI.stageByTime, stagingUI.orderType, siteRepo.concernTime, siteRepo.warningTime, stagingUI.releasedEventDateTime, stagingUI.expectedEndDateTime
                    )
                }
                showCollectToteBottomSheet()
            }
        }
    }

    private fun needToPrintBagLabels(stagingOneDataIn: StagingOneData): Boolean {
        // Multi-source orders do not need bag labels because the MFC totes are staged instead of the bags
        val orderTypeUsesBagLables = !stagingOneDataIn.isMultiSource
        return isCustomerBagPreference.value == false || (orderTypeUsesBagLables && !stagingOneData.bagLabelsPrintedSuccessfully)
    }

    private fun navigateHomeOnBackButtonClick() {

        viewModelScope.launch(dispatcherProvider.IO) {
            navigationButtonEvent.asFlow().collect {
                navigateHome()
            }
        }
    }

    private fun showReprintToteLabelDialogOnButtonClick() {
        //  Setup toolbar image and connect to event.
        changeToolbarRightSecondExtraImageEvent.postValue(DrawableIdHelper.Id(R.drawable.ic_print))
        viewModelScope.launch(dispatcherProvider.IO) {
            toolbarRightSecondImageEvent.asFlow().collect {
                showReprintToteLabelDialog()
            }
        }
    }

    private fun listenForAllOrdersCompleted() {
        viewModelScope.launch(dispatcherProvider.IO) {
            orderCompletionStateChangeEvent.filterNotNull().collect {
                // Creating synchronized hashmap to prevent ConcurrentModificationException - ACIP-178676
                val toteCountMapValues = Collections.synchronizedMap(toteCountMap)
                val toteValidFlags = toteCountMapValues.values.flatten().map { model ->
                    val bagCount = model.bagCount.value
                    val looseCount = model.looseCount.value
                    // both must be non-null and at least one must be non-zero
                    val isOrderMultiSource = model.stagingUI?.isOrderMultiSource == true

                    // if regular order and both bagCount and looseCount must be non-null and at least one is not equal to ) -> true
                    // if multisource order and both bagCount and looseCount must be non-null and both values 0 or greater ) -> true
                    // else -> false
                    if (!model.isCustomerPreferBag) {
                        true
                    } else if (isOrderMultiSource) {
                        bagCount !== null || looseCount !== null
                    } else {
                        bagCount.notZeroOrNull() || looseCount.notZeroOrNull()
                    }
                }
                val areAllFieldsFilled = toteValidFlags.all { it }

                val toteIdsFromUI = orderMap.value?.values?.flatten()?.map { it.toteId }
                val toteCt = toteIdsFromUI?.size ?: 0
                val doToteCountsMatch = toteValidFlags.size == toteCt

                areAllOrdersCompleted.postValue(doToteCountsMatch && areAllFieldsFilled)
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Public functions to retrieve flows of data
    // /////////////////////////////////////////////////////////////////////////
    fun getUiForOrder(orderNumber: String) = runBlocking {
        orderMap.value?.toList()?.find { it.first.orderNumber == orderNumber }
    }

    // /////////////////////////////////////////////////////////////////////////
    // API calls
    // /////////////////////////////////////////////////////////////////////////
    internal fun reprintLabels() {
        viewModelScope.launch(dispatcherProvider.IO) {
            when (apsRepo.printToteLabel(activityId.value)) {
                is ApiResult.Success -> toteLabelsPrintedSuccessfully.value = true
                is ApiResult.Failure -> {
                    toteLabelsPrintedSuccessfully.value = false
                    withContext(dispatcherProvider.Main) {
                        showSnackBar(AcupickSnackEvent(message = StringIdHelper.Id(R.string.error_printing_labels), SnackType.ERROR))
                    }
                }
            }.exhaustive
        }
    }

    fun onClickCta(orderCompletionState: OrderCompletionState?, incompleteStorageTypes: List<StorageType>) {
        val isEnabled = if (isMfc.value == true || areAllOrdersCompleted.value == true) true
        else if (isLastTab.value == true) areAllOrdersCompleted.value == true
        else orderCompletionState?.isComplete == true
        when {
            !isEnabled -> showSnackBar(AcupickSnackEvent(StringIdHelper.Format(R.string.please_enter_count_for_bags_and_loose_items, getIncompleteZones(incompleteStorageTypes)), SnackType.INFO))
            isLastTab.value == true -> recordCount()
            else -> viewModelScope.launch { advanceEvent.emit(Unit) }
        }
    }

    private fun getIncompleteZones(incompleteStorageTypes: List<StorageType>) = with(incompleteStorageTypes) {
        // If new storage type is added, we need ro modify this function
        // Otherwise it will show upto 4 Storage Types only
        when (size) {
            0 -> ""
            1 -> name(0)
            2 -> "${name(0)} & ${name(1)}"
            3 -> "${name(0)}, ${name(1)} & ${name(2)}"
            else -> "${name(0)}, ${name(1)}}, ${name(2)} & ${name(3)}"
        }
    }

    private fun List<StorageType>.name(index: Int) = this[index].displayName(getApplication())

    private fun recordCount() {
        viewModelScope.launch(dispatcherProvider.IO) {
            keyboardActiveEvent.postValue(false)
            if (!networkAvailabilityManager.isConnected.first()) {
                networkAvailabilityManager.triggerOfflineError { recordCount() }
            }
            when (
                val results = isBlockingUi.wrap {
                    apsRepo.recordBagCount(
                        UpdateErBagRequestDto(
                            activityId = activityId.first().toLong(),
                            contContBagCount = toteCountMap.values.flatten().distinct().map {
                                ContBagCountRequestDto(
                                    containerId = it.item.toteId,
                                    bagCount = it.bagCount.value ?: 0,
                                    looseItemCount = it.looseCount.value ?: 0,
                                )
                            }
                        )
                    )
                }
            ) {
                is ApiResult.Success -> {
                    stagingOneData.nextActivityId = results.data.nextActivityId ?: 0L
                    updateAndSaveStagingOne()
                    if (needToPrintBagLabels(stagingOneData)) {
                        showPrintBagLabelDialog()
                    } else {
                        saveStagingOneDataAndNavigateToPartTwo()
                    }
                }

                is ApiResult.Failure -> {
                    if (results is ApiResult.Failure.Server) {
                        val type = results.error?.errorCode?.resolvedType
                        val errorCode = (results as? ApiResult.Failure.Server)?.error?.errorCode?.rawValue
                        when (type?.cannotAssignToOrder()) {
                            true -> {
                                val tabCount = tabs.value?.size ?: 1
                                val serverErrorType = if (type == ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY) CannotAssignToOrderDialogTypes.STAGING else CannotAssignToOrderDialogTypes.REGULAR
                                serverErrorCannotAssignUser(serverErrorType, tabCount > 1)
                            }

                            else -> if (errorCode == STAGING_WRONG_HOT_ZONE_BAG_AND_ITEM_COUNT_ERROR_CODE) {
                                showWrongHotZoneBagsAndItemCountDialog()
                            } else {
                                handleApiError(errorType = results)
                            }
                        }
                    } else {
                        handleApiError(errorType = results)
                    }
                }
            }.exhaustive
        }
    }

    private fun saveStagingOneDataAndNavigateToPartTwo() {
        updateAndSaveStagingOne()
        navigateToPart2()
    }

    private suspend fun attemptToPrintBagLabels(activityIdString: Long?): Boolean {
        return (isBlockingUi.wrap { apsRepo.printBagLabels(activityIdString.toString()) }) is ApiResult.Success
    }

    private fun reload() {
        activityId.value.let { actId ->
            activityId.value = ""
            activityId.value = actId
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Navigation
    // /////////////////////////////////////////////////////////////////////////
    fun navigateHome() {
        _navigationEvent.postValue(
            NavigationEvent.Directions(StagingPagerFragmentDirections.actionStagingFragmentToHomeFragment())
        )
    }

    private fun navigateToPart2() {

        val totesFromUI = toteCountMap.values.flatten().map { it.item }
        val toteList = totesFromUI.ifEmpty {

            // ACUPICK-836/896
            // we're navigating to part two before fully populating the UI
            // the only thing that matters in this list of totes is the ID and customer order number
            // they are used to populate the checkboxes to unassign
            // we might scrap this parameter completely and derive from staging two data?
            stagingOneData.totesByToteIdMap.values.map {
                ToteUI(toteId = it.toteId, customerOrderNumber = it.customerOrderNumber, storageType = it.storageType)
            }
        }

        _navigationEvent.postValue(
            NavigationEvent.Directions(
                StagingPagerFragmentDirections.actionStagingFragmentToStagingPart2Fragment(
                    StagingPart2Params(
                        pickingActivityId = activityId.value.toLong(),
                        stagingActivityId = stagingOneData.nextActivityId!!,
                        toteList = toteList,
                        isPrintingStillNeeded = needToPrintBagLabels(stagingOneData),
                        customerOrderNumber = null
                    )
                )
            )
        )
    }

    // /////////////////////////////////////////////////////////////////////////
    // Dialog setup
    // /////////////////////////////////////////////////////////////////////////

    init {
        registerCloseAction(STAGING_LOAD_DATA_RETRY_DIALOG_TAG) {
            closeActionFactory(positive = { reload() })
        }

        registerCloseAction(STAGING_BACK_PRESSED_DIALOG_TAG) {
            closeActionFactory(positive = { navigateHome() })
        }

        registerCloseAction(STAGING_REPRINT_TOTE_BAGS_BOTTOMSHEET_TAG) {
            closeActionFactory(positive = { reprintLabels() })
        }

        registerCloseAction(ATTACH_BAG_LABEL_BOTTOMSHEET_TAG) {
            closeActionFactory(positive = { saveStagingOneDataAndNavigateToPartTwo() })
        }

        registerCloseAction(RETRY_ERROR_DIALOG_TAG) {
            serverErrorListener
            closeActionFactory(positive = { reload() })
        }
    }

    private fun showReprintToteLabelDialog() = inlineBottomSheetEvent.postValue(getReprintToteLabelsArgDataAndTagForBottomSheet())

    private fun showReloadDialog() =
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    title = StringIdHelper.Id(R.string.wifi_error_title),
                    body = StringIdHelper.Id(R.string.wifi_error_body),
                    positiveButtonText = StringIdHelper.Id(R.string.refresh),
                    cancelOnTouchOutside = false
                ),
                tag = STAGING_LOAD_DATA_RETRY_DIALOG_TAG
            )
        )

    private fun showPrintBagLabelDialog() {
        viewModelScope.launch(dispatcherProvider.IO) {
            val isSuccess = isBlockingUi.wrap { attemptToPrintBagLabels(stagingOneData.nextActivityId) }
            stagingOneData.bagLabelsPrintedSuccessfully = isSuccess
            inlineBottomSheetEvent.postValue(
                if (isAllCustomerPreferNoBag.value == true)
                    getAttachToteAndLooseLabelsArgDataAndTagForBottomSheet()
                else
                    getAttachBagLabelsArgDataAndTagForBottomSheet()
            )
            if (!isSuccess) {
                delay(500)
                showSnackBar(AcupickSnackEvent(message = StringIdHelper.Id(R.string.error_printing_labels), SnackType.ERROR))
            }
        }
    }

    private fun showWrongHotZoneBagsAndItemCountDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.staging_wrong_hot_zone_bag_and_item_count_title),
                    body = StringIdHelper.Id(R.string.staging_wrong_hot_zone_bag_and_item_count_body),
                    positiveButtonText = StringIdHelper.Id(R.string.ok),
                ),
                tag = STAGING_WRONG_HOT_ZONE_BAG_AND_ITEM_COUNT_TAG
            )
        )
    }

    fun updateAndSaveStagingOne() {
        // TODO this is called too often and does too much, but it works
        stagingOneData.totesByToteIdMap = getCurrentViewModelTotesByIdMap()
        stagingStateRepo.saveStagingPartOne(stagingOneData, activityId.value)
    }

    private fun getCurrentViewModelTotesByIdMap(): Map<String, Tote> {
        val toteList = toteCountMap.values.flatten().map { toteModel ->
            Tote(
                customerOrderNumber = toteModel.item.customerOrderNumber!!,
                toteId = toteModel.item.toteId!!,
                bagCount = toteModel.bagCount.value,
                looseItemCount = toteModel.looseCount.value,
                storageType = toteModel.item.storageType
            )
        }
        val totesByToteIdMap = toteList.associateBy { it.toteId }
        return totesByToteIdMap
    }

    private fun showCollectToteBottomSheet() {
        if (isCustomerBagPreference.value == false) {
            inlineBottomSheetEvent.postValue(getReprintToteLabelsArgDataAndTagForBagPreferredBottomSheet())
        } else {
            val totesCount = orderMap.value?.values?.sumOf { it.size } ?: 0
            inlineBottomSheetEvent.postValue(getCollectToteLabelsArgDataAndTagForBottomSheet(totesCount))
        }
    }

    companion object {
        const val STAGING_LOAD_DATA_RETRY_DIALOG_TAG = "stagingLoadDataRetryDialogTag"
        const val STAGING_BACK_PRESSED_DIALOG_TAG = "stagingBackPressedDialogTag"
        const val STAGING_ORDER_TAKEN_DIALOG_TAG = "stagingOrderTakenDialogTag"
        private const val STAGING_WRONG_HOT_ZONE_BAG_AND_ITEM_COUNT_TAG = "stagingWrongHotZoneBagAndItemCountDialogTag"
        private const val STAGING_WRONG_HOT_ZONE_BAG_AND_ITEM_COUNT_ERROR_CODE = 172
    }
}
