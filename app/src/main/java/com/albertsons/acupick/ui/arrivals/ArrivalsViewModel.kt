package com.albertsons.acupick.ui.arrivals

import android.app.Application
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.BuildConfig
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.R
import com.albertsons.acupick.data.logic.AgeVerificationLogic.activityHasRegulatedItems
import com.albertsons.acupick.data.logic.AgeVerificationLogic.hasActivityDetails
import com.albertsons.acupick.data.logic.AgeVerificationLogic.listHasRegulatedItems
import com.albertsons.acupick.data.logic.AgeVerificationLogic.selectedItemIsRegulated
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.CancelReasonCode
import com.albertsons.acupick.data.model.Complete1PLHandoffData
import com.albertsons.acupick.data.model.CustomerArrivalStatus
import com.albertsons.acupick.data.model.HandOff1PLInterstitialParams
import com.albertsons.acupick.data.model.ImageSizePreset
import com.albertsons.acupick.data.model.VanStatus
import com.albertsons.acupick.data.model.request.AssignUserWrapperRequestDto
import com.albertsons.acupick.data.model.request.Cancel1PLHandoffRequestDto
import com.albertsons.acupick.data.model.request.CancelHandoffRequestDto
import com.albertsons.acupick.data.model.request.Get1PLTruckRemovalItemListRequestDto
import com.albertsons.acupick.data.model.request.RemoveItems1PLRequestDto
import com.albertsons.acupick.data.model.request.UpdateDugArrivalStatusRequestDto
import com.albertsons.acupick.data.model.request.UpdateOnePlArrivalStatusRequestDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.CannotAssignToOrderDialogTypes
import com.albertsons.acupick.data.model.response.Remove1PLItemsResponseDto
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.hasPharmacyServicingOrders
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.CompleteHandoff1PLRepository
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.image.ImagePreCacher
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.SERVER_DOB_FORMAT
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.formattedWith
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.arrivals.destage.DestageOrderPagerViewModel
import com.albertsons.acupick.ui.arrivals.destage.removeitems.RemoveRejected1PLFragmentDirections
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.dialog.HAND_OFF_ALREADY_ASSIGNED_ARG_DATA
import com.albertsons.acupick.ui.dialog.OF_AGE_ASSOCIATE_VERIFICATION_DATA
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.models.CustomerArrivalStatusUI
import com.albertsons.acupick.ui.models.OrderItemUI
import com.albertsons.acupick.ui.models.combineArriveStatuses
import com.albertsons.acupick.ui.models.CustomerData
import com.albertsons.acupick.ui.models.CustomerInfo
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.combineWith
import com.albertsons.acupick.ui.util.getOrZero
import com.albertsons.acupick.ui.util.getSizedImageUrl
import com.albertsons.acupick.ui.util.orFalse
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.koin.core.component.inject
import timber.log.Timber
import java.time.ZoneId
import java.time.ZonedDateTime

class ArrivalsViewModel(val app: Application, private val activityViewModel: MainActivityViewModel) : BaseViewModel(app) {

    // DI
    private val apsRepo: ApsRepository by inject()
    private val userRepo: UserRepository by inject()
    private val conversationsRepository: ConversationsRepository by inject()
    private val pickRepository: PickRepository by inject()
    private val dispatcherProvider: DispatcherProvider by inject()
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    private val siteRepo: SiteRepository by inject()
    private val imagePreCacher: ImagePreCacher by inject()
    private val completeHandoff1PLRepository: CompleteHandoff1PLRepository by inject()

    val searchQuery = MutableStateFlow<String>("")
    private val searchQueryDebounced = MutableStateFlow("")

    // Input state
    private val selectedItemIds = MutableStateFlow<List<Long>>(emptyList())
    private val activityDtoArray = arrayListOf<ActivityDto>()

    // UI Observables
    val results = MutableLiveData<List<OrderItemUI?>>(listOf())
    val filteredResult = combine(results.asFlow(), searchQueryDebounced) { results, query ->
        if (query.isEmpty()) results
        else results?.filter { it?.orderNumber?.contains(query) == true }
    }.asLiveData()
    val isInternalBuild = MutableLiveData(BuildConfig.DEBUG || BuildConfig.INTERNAL)

    private val selectionHasRegulatedItems = MutableLiveData(false)
    val showBeginButton = selectedItemIds.debounce(UI_INPUT_DEBOUNCE_MILLIS).map { it.isNotEmpty() }.asLiveData().combineWith(selectionHasRegulatedItems) { hasIds, _ ->
        hasIds == true
    }
    val isDataRefreshing = MutableLiveData(false)
    val isDataLoading = MutableLiveData(true)
    private val isMultiSelect: LiveData<Boolean> = MutableLiveData()
    val beginButtonEnabled = showBeginButton.combineWith(isDataLoading) { showButton, loading ->
        showButton.orFalse() && loading.orFalse().not()
    }
    val isSkeletonStateShowing = isDataLoading.combineWith(isDataRefreshing) { loading, refreshing ->
        loading == true && refreshing == false
    }
    val showNoOrdersReadyUi = results.combineWith(isSkeletonStateShowing) { orders, isSkeletonShowing ->
        orders?.none { it?.pickerName.isNullOrEmpty() } == true && isSkeletonShowing == false
    }
    val showNoOrdersReadyInProgressUi = results.combineWith(isSkeletonStateShowing) { orders, isSkeletonShowing ->
        orders?.none { it?.pickerName.isNotNullOrEmpty() || it?.vanStatus == VanStatus.IN_PROGRESS } == true && isSkeletonShowing == false
    }

    val loadDataEvent: MutableLiveData<Boolean> = LiveEvent()
    val onEllipsisClickEvent: MutableLiveData<OrderItemUI> = LiveEvent()
    private val maxSelection get() = if (activityViewModel.is1Pl.value.orFalse()) MAX_ORDER_SELECTION_1PL else MAX_ORDER_SELECTION
    val isMaxOrderSelected get() = selectedItemIds.value.count() >= maxSelection
    val pickupReadyNoDataAvailableText
        get() = app.getString(
            if (activityViewModel.is1Pl.value.orFalse()) R.string.one_pl_pickup_ready_order_unavailable
            else R.string.pickup_ready_order_unavailable
        )
    val inProgressNoDataAvailableText
        get() = app.getString(
            if (activityViewModel.is1Pl.value.orFalse()) R.string.one_pl_in_progress_order_unavailable
            else R.string.in_progress_order_unavailable
        )
    val firstNotificationETATime get() = siteRepo.notificationEtaTime.firstNotificationETATime ?: 8
    val secondNotificationETATime get() = siteRepo.notificationEtaTime.secondNotificationETATime ?: 4
    val showUnreadMessages = MutableLiveData(false)

    private val orderItemsFor1PLToSelect = results.combineWith(activityViewModel.orderNumberFor1PlToSelect) { orders, orderNumber -> orders to orderNumber }
    private val orderItemsFor1PLToSelectObserver: Observer<Pair<List<OrderItemUI?>?, Long?>> = Observer { pair ->
        if (isCas1PlOrderEligibleToBeSelected(pair)) {
            viewModelScope.launch {
                delay(300)
                selectItem(pair.second.toString())
                results.postValue(pair.first)
                activityViewModel.orderNumberFor1PlToSelect.postValue(null)
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Listeners
    // /////////////////////////////////////////////////////////////////////////

    val is1Pl get() = activityViewModel.is1Pl.value.orFalse()
    private val timerjobs = mutableListOf<Job>()

    private var activeHandoffToBegin: Remove1PLItemsResponseDto? = null
    private var has1PLGiftorders: Boolean = false

    fun onChatClicked(orderNumber: String) { // Chat button click
        viewModelScope.launch {
            pickRepository.pickList.value?.actId?.let {
                _navigationEvent.postValue(
                    NavigationEvent.Directions(
                        NavGraphDirections.actionArrivalsPagerFragmentToPickListItemsFragment(
                            activityId = it.toString(),
                            orderNumber = orderNumber,
                            navigateToChat = true
                        )
                    )
                )
            }
        }
    }

    fun clearAllPreviousSelections() {
        selectedItemIds.value = emptyList()
        activityDtoArray.clear()
    }

    init {
        orderItemsFor1PLToSelect.observeForever(orderItemsFor1PLToSelectObserver)
        viewModelScope.launch {
            registerCloseAction(TRANSFER_ORDER_DIALOG_TAG) {
                closeActionFactory(positive = { beginHandoff(true) })
            }
            registerCloseAction(OF_AGE_ASSOCIATE_VERIFICATION_TAG) {
                closeActionFactory(
                    positive = { },
                    negative = {
                        selectedItemIds.value = emptyList()
                        activityDtoArray.clear()
                        loadDataEvent.postValue(true)
                    }
                )
            }
            registerCloseAction(REJECTED_ITEMS_DIALOG_TAG) {
                closeActionFactory(positive = {
                    complete1PLHandoff(selected1PLItem?.actId, selected1PLItem?.source, selected1PLItem?.orderCount)
                }, negative = {
                    cancel1PLHandoff(selected1PLItem?.actId)
                })
            }

            registerCloseAction(ONE_PL_GIFTING_CONFIRMATION_DIALOG_TAG) {
                closeActionFactory(positive = {
                    printGiftNotes()
                })
            }
        }

        viewModelScope.launch {
            searchQuery
                .debounce(1000) // debounce for 1 seconds
                .collect { query ->
                    searchQueryDebounced.value = query
                }
        }

        viewModelScope.launch {
            siteRepo.siteDetails.collect {
                isMultiSelect.set(it?.isMultipleHandoffAllowed ?: true)
            }
        }

        registerCloseAction(SINGLE_ORDER_ERROR_DIALOG_TAG) {
            closeActionFactory(positive = {
                selectedItemIds.value = emptyList()
                activityDtoArray.clear()
                loadDataEvent.postValue(false)
            })
        }

        registerCloseAction(BATCH_ORDER_ERROR_DIALOG_TAG) {
            closeActionFactory(positive = {
                selectedItemIds.value = emptyList()
                activityDtoArray.clear()
                loadDataEvent.postValue(false)
            })
        }
    }

    private fun isCas1PlOrderEligibleToBeSelected(pair: Pair<List<OrderItemUI?>?, Long?>): Boolean {
        val (orders, orderNumber) = pair
        return orders.isNotNullOrEmpty() && orderNumber != null && orderNumber != -1L &&
            results.value?.firstOrNull { it?.pickerId == null && it?.orderNumber == orderNumber.toString() } != null
    }

    // /////////////////////////////////////////////////////////////////////////
    // API CALL
    // /////////////////////////////////////////////////////////////////////////
    private fun loadSelectionDetails(selectionIds: List<Long>, erIds: List<Long>?) {
        viewModelScope.launch(dispatcherProvider.IO) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> {
                    erIds?.forEach { erId ->

                        // Check if order details has already been fetch
                        if (activityDtoArray.hasActivityDetails(erId)) {
                            selectedItemIds.value = selectionIds
                            if (activityDtoArray.selectedItemIsRegulated(selectionIds)) return@forEach
                        }

                        val result = isBlockingUi.wrap { apsRepo.pickUpActivityDetails(id = erId, loadCI = true) }
                        when (result) {
                            is ApiResult.Success<ActivityDto> -> {
                                checkForRegulatedItems(result.data)
                            }

                            is ApiResult.Failure -> {
                                if (result is ApiResult.Failure.Server) {
                                    val type = result.error?.errorCode?.resolvedType
                                    if (type == ServerErrorCode.USER_NOT_VALID) {
                                        inlineDialogEvent.postValue(
                                            CustomDialogArgDataAndTag(
                                                data = HAND_OFF_ALREADY_ASSIGNED_ARG_DATA,
                                                tag = DestageOrderPagerViewModel.HANDOFF_ALREADY_ASSIGNED_DIALOG_TAG,
                                            )
                                        )
                                    } else {
                                        handleApiError(result, retryAction = { loadSelectionDetails(selectionIds, erIds) })
                                    }
                                } else {
                                    handleApiError(result, retryAction = { loadSelectionDetails(selectionIds, erIds) })
                                }
                            }
                        }.exhaustive
                    }
                    selectedItemIds.value = selectionIds
                }

                false -> {
                    networkAvailabilityManager.triggerOfflineError {
                        loadSelectionDetails(selectionIds, erIds)
                    }
                }
            }
        }
    }

    private fun handleItemSelection(selectionIds: List<Long>) = viewModelScope.launch(dispatcherProvider.IO) {
        // Fetch order details for last selected item
        val erIds = results.value?.filter {
            it?.orderNumber?.toLongOrNull()?.toString() == selectionIds.lastOrNull()?.toString()
        }?.mapNotNull { it?.erId }

        loadSelectionDetails(selectionIds, erIds)
    }

    private fun showRxNotPremittedForBatchDialog(customerNames: Pair<String, String>) {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.Informational,
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.rx_dug_rx_not_premitted_in_batch_title),
                    body = StringIdHelper.Raw(customerNames.first),
                    bodyWithBold = customerNames.second,
                    positiveButtonText = StringIdHelper.Id(R.string.order_details_exit_title),
                ),
                tag = SHOW_RX_NOT_PREMITTED_FOR_BATCH_DIALOG
            )
        )
    }

    private fun createCustomerNameDialogBody(customerNames: List<ActivityDto>): Pair<String, String> {
        val text = StringBuilder()
        val names = if (customerNames.size < 2) {
            "${customerNames.first().contactFirstName} ${customerNames.first().contactLastName}"
        } else if (customerNames.size == 2) {
            // Two customers in a batch with RX data
            customerNames.joinToString(separator = " & ") { "${it.contactFirstName} ${it.contactLastName}" }
        } else {
            // Three customers in batch with RX data
            customerNames.joinToString(
                limit = 2,
                truncated =
                "& ${customerNames[customerNames.lastIndex].contactFirstName} ${customerNames[customerNames.lastIndex].contactLastName} "
            ) {
                "${it.contactFirstName} ${it.contactLastName}"
            }
        }
        text.apply {
            append(names)
            // setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, names.lastIndex + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(app.getString(R.string.rx_dug_rx_not_premitted_in_batch_body))
        }
        return Pair(text.toString(), names)
    }

    private fun checkForRegulatedItems(activityDto: ActivityDto) {
        // Cache details to avoid refresh on deselect/reselect
        activityDtoArray.add(activityDto)

        // Hide cta & navigate via dialog when regulated items exist
        selectionHasRegulatedItems.postValue(activityDtoArray.listHasRegulatedItems())
        if (siteRepo.isDigitizeAgeVerificationEnabled) {
            if (activityDto.activityHasRegulatedItems()) showOfAgeVerificationDialog()
        }
    }

    fun updateArrivalStatus(id: Long, arrived: Boolean) {
        viewModelScope.launch(dispatcherProvider.IO) {
            val currentResults = results.asFlow().first()
            val item = currentResults.find { it?.orderNumber?.toLongOrNull() == id } ?: return@launch

            // Remove item from list so UI updates
            results.postValue(currentResults.filter { it?.orderNumber?.toLongOrNull() != id })

            // Call API to update status
            apsRepo.updateDugArrivalStatus(
                UpdateDugArrivalStatusRequestDto(
                    customerArrivalStatus = if (arrived) CustomerArrivalStatus.ARRIVED else CustomerArrivalStatus.UNARRIVED,
                    erId = item.erId,
                    orderNumber = item.orderNumber,
                    siteId = item.siteId?.toLongOrNull() ?: 0,
                    statusEventTimestamp = ZonedDateTime.now(),
                    estimateTimeOfArrival = if (arrived) ZonedDateTime.now() else null
                )
            )

            if (!arrived) {
                cancelHandOff(
                    CancelHandoffRequestDto(
                        cancelReasonCode = CancelReasonCode.WRONG_HANDOFF,
                        erId = item.erId,
                        siteId = item.siteId,
                    )
                )
            }

            // Wait for update to finish then refresh list to get new statuses
            loadDataEvent.postValue(false)
        }
    }

    private fun updateOnePlArrivalStatus(item: OrderItemUI) {
        viewModelScope.launch(dispatcherProvider.IO) {
            val currentResults = results.asFlow().first()
            // Remove item from list so UI updates
            results.postValue(currentResults.filter { it?.actId != item.actId })

            apsRepo.updateOnePlArrivalStatus(
                UpdateOnePlArrivalStatusRequestDto(
                    siteId = item.siteId,
                    vanNumber = item.source,
                    date = item.customerArrivalTime?.withZoneSameInstant(ZoneId.systemDefault()).formattedWith(SERVER_DOB_FORMAT),
                    eventTime = item.customerArrivalTime
                )
            )

            // Wait for update to finish then refresh list to get new statuses
            loadDataEvent.postValue(false)
        }
    }

    fun beginHandoff(reAssign: Boolean = false) {
        if (activityViewModel.is1Pl.value.orFalse()) {
            beginHandOffOnePl(selected1PLItem?.actId, selected1PLItem?.source)
        } else {
            viewModelScope.launch(dispatcherProvider.IO) {

                if (activityDtoArray.hasPharmacyServicingOrders()) {
                    val activityDtos = activityDtoArray.filter { it.rxDetails?.pharmacyServicingOrders == true }
                    if (activityDtos.size >= ONE_ORDER && activityDtoArray.count() > 1) {
                        val customerNamesString = createCustomerNameDialogBody(activityDtos)
                        showRxNotPremittedForBatchDialog(customerNamesString)
                        return@launch
                    }
                }
                results.asFlow().first().filter {
                    selectedItemIds.value.find { id -> id == it?.orderNumber?.toLong() } != null
                }.also { selectedOrderItemUIList ->
                    selectedOrderItemUIList.forEach { order ->
                        val assignedToMe = order?.pickerId == userRepo.user.value?.userId
                        val isAssigned = order?.pickerId.isNotNullOrEmpty()

                        if ((!assignedToMe && isAssigned) && !reAssign) {
                            showTransferHandOffDialog(order?.pickerName.toString())
                            return@launch
                        }
                    }
                    val actIdList = selectedOrderItemUIList.map { it?.actId ?: 0L }
                    val customerInfo = selectedOrderItemUIList.map { CustomerInfo(it?.name.orEmpty(), it?.contactPersonId.orEmpty(), it?.erId ?: 0L) }
                    refreshActivityArray(actIdList)
                    assignToMe(actIdList, reAssign, CustomerData(customerInfo))
                }
            }
        }
    }

    private val selected1PLItem get() = results.value?.firstOrNull { it?.actId == selectedItemIds.value.firstOrNull() }

    private fun refreshActivityArray(actIdList: List<Long>) {
        activityDtoArray.removeAll { !actIdList.contains(it.actId) }
    }

    private fun cancelHandOff(cancelRequestDto: CancelHandoffRequestDto) {
        viewModelScope.launch {
            val result =
                apsRepo.cancelHandoff(
                    cancelRequestDto
                )
            if (result is ApiResult.Success) {
                Timber.d("HandOff has been canceled and unassigned from user.")
            } else {
                handleApiError(result as ApiResult.Failure, retryAction = { cancelHandOff(cancelRequestDto) })
            }
        }
    }

    private fun assignToMe(actIds: List<Long>, shouldOverride: Boolean, customerData: CustomerData) {
        viewModelScope.launch(dispatcherProvider.IO) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> {
                    val result = isBlockingUi.wrap {
                        apsRepo.assignUserToHandoffs(
                            AssignUserWrapperRequestDto(
                                actIds = actIds,
                                replaceOverride = shouldOverride,
                                // Todo find out what this does
                                resetPickList = true,
                                user = userRepo.user.value?.toUserDto(),
                                etaArrivalFlag = siteRepo.isEtaArrivalEnabled
                            )
                        )
                    }
                    when (result) {
                        is ApiResult.Success<List<ActivityDto>> -> {
                            selectedItemIds.value = emptyList()
                            if (siteRepo.isCctEnabled) {
                                // precache image urls
                                val imageUrls = mutableListOf<String?>()
                                activityDtoArray.forEach {
                                    it.orderSummary?.forEach { orderSummaryDto ->
                                        imageUrls.add(orderSummaryDto?.imageUrl)
                                        if (orderSummaryDto?.substitutedWith.isNotNullOrEmpty()) {
                                            orderSummaryDto?.substitutedWith?.forEach { subSummary ->
                                                imageUrls.add(subSummary?.imageUrl)
                                            }
                                        }
                                    }
                                }
                                imagePreCacher.preCacheImages(imageUrls.mapNotNull { getSizedImageUrl(it, ImageSizePreset.ItemDetails) })
                            } else {
                                // adding to fix compliler error
                            }
                            navigateToPickup(
                                SelectedActivities(
                                    activityList = activityDtoArray.distinct().map {
                                        it.copy(nextActExpStartTime = it.nextActExpStartTime ?: ZonedDateTime.now())
                                    }
                                ),
                                customerData
                            )
                        }

                        is ApiResult.Failure -> {
                            if (result is ApiResult.Failure.Server) {
                                val type = result.error?.errorCode?.resolvedType
                                when (type?.cannotAssignToOrder()) {
                                    true -> {
                                        val isBatch = selectedItemIds.value.size > 1
                                        val dialogType = if (type == ServerErrorCode.NO_OVER_RIDE_FLAG || type == ServerErrorCode.USER_NOT_VALID)
                                            CannotAssignToOrderDialogTypes.HANDOFF_REASSIGN
                                        else
                                            CannotAssignToOrderDialogTypes.HANDOFF

                                        serverErrorCannotAssignUser(dialogType, isBatch)
                                    }

                                    else -> {
                                        handleApiError(result, retryAction = { assignToMe(actIds, shouldOverride, customerData) })
                                    }
                                }
                            } else {
                                handleApiError(result, retryAction = { assignToMe(actIds, shouldOverride, customerData) })
                            }
                        }
                    }
                }

                false -> networkAvailabilityManager.triggerOfflineError { assignToMe(actIds, shouldOverride, customerData) }
            }
        }
    }

    private fun complete1PLHandoff(actId: Long?, vanId: String?, orderCount: String?) {
        // Completing 1PL hand off as there is no rejected items
        selectedItemIds.value = emptyList()
        viewModelScope.launch(dispatcherProvider.Main) {
            val handoffInterstitialParam = HandOff1PLInterstitialParams(
                actId = actId,
                removeItems1PLRequestDto = RemoveItems1PLRequestDto(
                    actId = actId,
                    giftLabelPrintConfirmation = has1PLGiftorders,
                    siteId = userRepo.user.value?.selectedStoreId,
                    vanNumber = vanId,
                    removeItemsReqs = emptyList()
                )
            )
            completeHandoff1PLRepository.saveCompleteHandoff(Complete1PLHandoffData(handoffInterstitialParam))

            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    RemoveRejected1PLFragmentDirections.actionToRemove1PLHandoffFragment(
                        totalOrderCount = orderCount?.toInt().getOrZero(),
                        handOffInterstitialParamsList = handoffInterstitialParam
                    )
                )
            )
        }
    }

    private fun beginHandOffOnePl(actId: Long?, vanId: String?) {
        viewModelScope.launch(dispatcherProvider.IO) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> {
                    val result = isBlockingUi.wrap {
                        apsRepo.get1PLTruckRemovalList(
                            Get1PLTruckRemovalItemListRequestDto(
                                activityId = actId,
                                replaceOverride = true,
                                resetPickList = true,
                                user = userRepo.user.value?.toUserDto(),
                                vanId = vanId,
                                siteId = userRepo.user.value?.selectedStoreId,
                                eventTimeStamp = ZonedDateTime.now()
                            )
                        )
                    }
                    when (result) {
                        is ApiResult.Success<Remove1PLItemsResponseDto> -> {
                            activeHandoffToBegin = result.data
                            if (result.data.giftOrderCount.getOrZero() > 0 && result.data.giftOrderErIds.isNotNullOrEmpty()) {
                                has1PLGiftorders = true
                                showGiftConfirmationDialog(result.data)
                            } else {
                                handleCtaAction()
                            }
                        }

                        is ApiResult.Failure -> {
                            handleApiError(result, retryAction = { beginHandOffOnePl(actId, vanId) })
                        }
                    }
                }

                false -> networkAvailabilityManager.triggerOfflineError { beginHandOffOnePl(actId, vanId) }
            }
        }
    }

    private fun handleCtaAction() {
        activeHandoffToBegin?.let {
            if (it.ordersPerZone.isNullOrEmpty()) {
                showRejectedItemsDialog()
            } else {
                navigateTo1PLPickup(it)
            }
        }
    }

    private fun showGiftConfirmationDialog(data: Remove1PLItemsResponseDto) {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.OnePlGiftingDialog,
                    title = StringIdHelper.Plural(
                        R.plurals.one_pl_collect_gift_notes_title,
                        data.giftOrderCount.getOrZero()
                    ),
                    largeImage = R.drawable.ic_gift_note,
                    body = StringIdHelper.Id(R.string.one_pl_gift_note_body),
                    secondaryBody = StringIdHelper.Id(R.string.one_pl_gift_note_secondary_body),
                    boldWord = StringIdHelper.Raw("1PL ${data.vanId}"),
                    positiveButtonText = StringIdHelper.Plural(
                        R.plurals.one_pl_print_gift_notes_cta,
                        data.giftOrderCount.getOrZero()
                    ),
                    cancelOnTouchOutside = false,
                ),
                ONE_PL_GIFTING_CONFIRMATION_DIALOG_TAG
            )
        )
    }

    private fun printGiftNotes() {
        activeHandoffToBegin?.giftOrderErIds?.let {
            viewModelScope.launch {
                // if (networkAvailabilityManager.isConnected.value) {
                isBlockingUi.wrap { apsRepo.printGiftLabel(erIds = it) }.let { result ->
                    if (result is ApiResult.Failure) {
                        showSnackBar(
                            AcupickSnackEvent(
                                message = StringIdHelper.Id(R.string.error_print_gift),
                                type = SnackType.ERROR
                            )
                        )
                    }
                }
                handleCtaAction()
                // } else {
                //     networkAvailabilityManager.triggerOfflineError { printGiftNotes() }
                // }
            }
        }
    }

    private fun cancel1PLHandoff(actId: Long?) {
        viewModelScope.launch(dispatcherProvider.IO) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> {
                    val result = isBlockingUi.wrap {
                        apsRepo.cancel1PLHandoff(
                            Cancel1PLHandoffRequestDto(
                                activityId = actId,
                                unassignTime = ZonedDateTime.now()
                            )
                        )
                    }
                    when (result) {
                        is ApiResult.Success -> {
                            selectedItemIds.value = emptyList()
                            activityDtoArray.clear()
                            loadDataEvent.postValue(true)
                            /* Doesn't require any action Since We are alerday in Arrivals fragment,*/
                        }

                        is ApiResult.Failure -> {
                            handleApiError(result, retryAction = { cancel1PLHandoff(actId) })
                        }
                    }
                }

                false -> networkAvailabilityManager.triggerOfflineError { cancel1PLHandoff(actId) }
            }
        }
    }

    private fun navigateTo1PLPickup(rejectedItems: Remove1PLItemsResponseDto) {
        selectedItemIds.value = emptyList()
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                NavGraphDirections.actionToRemove1PLRejectedFragment(rejectedItems)
            )
        )
    }

    private fun navigateToPickup(activities: SelectedActivities?, customerData: CustomerData) {
        viewModelScope.launch {
            activities?.let {
                _navigationEvent.postValue(
                    NavigationEvent.Directions(
                        NavGraphDirections.actionToDestageOrder(it, customerData)
                    )
                )
            }
        }
    }

    private fun showTransferHandOffDialog(user: String) {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.handoff_reassign_title),
                    body = StringIdHelper.Raw(app.getString(R.string.handoff_reassign_body, user)),
                    positiveButtonText = StringIdHelper.Id(R.string.confirm),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelOnTouchOutside = false,
                    cancelable = true
                ),
                tag = TRANSFER_ORDER_DIALOG_TAG
            )
        )
    }

    private fun showRejectedItemsDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    title = StringIdHelper.Id(R.string.no_rejected_items),
                    body = StringIdHelper.Id(R.string.please_complete_handoff_to_proceed),
                    positiveButtonText = StringIdHelper.Id(R.string.rejected_item_complete_handoff_cta),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelOnTouchOutside = false,
                    cancelable = false,
                ),
                tag = REJECTED_ITEMS_DIALOG_TAG
            )
        )
    }

    private fun showOfAgeVerificationDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = OF_AGE_ASSOCIATE_VERIFICATION_DATA,
                tag = DestageOrderPagerViewModel.OF_AGE_ASSOCIATE_VERIFICATION_TAG,
            )
        )
    }

    fun isInProgess(order: OrderItemUI) = order.pickerName.isNotNullOrEmpty()

    fun isOrderSelected(orderNumber: String) = selectedItemIds.value.contains(orderNumber.toLong())

    private fun addOrderToSelection(orderNumber: String) {
        selectedItemIds.value = selectedItemIds.value.toMutableList().apply { add(orderNumber.toLong()) }
    }

    private fun removeOrderFromSelection(orderNumber: String) {
        selectedItemIds.value = selectedItemIds.value.filterNot { it == orderNumber.toLong() }
    }

    fun onClickItem(item: OrderItemUI) {
        when {
            isOrderSelected(item.orderNumber) -> deslectItem(item.orderNumber)
            isMaxOrderSelected -> showMaxQuantityReachedSnackbar()
            else -> selectItem(if (activityViewModel.is1Pl.value.orFalse()) item.actId?.toString().orEmpty() else item.orderNumber)
        }
        results.value.let { results.postValue(it) }
    }

    private fun selectItem(orderNumber: String) {
        addOrderToSelection(orderNumber)
        selectedItemIds.value.let { idList ->
            if (idList.isNotEmpty()) handleItemSelection(idList) else activityDtoArray.clear()
        }
    }

    private fun deslectItem(orderNumber: String) {
        removeOrderFromSelection(orderNumber)
        activityDtoArray.removeIf { it.customerOrderNumber?.toLongOrNull() == orderNumber.toLong() }
    }

    private fun showMaxQuantityReachedSnackbar() = showSnackBar(
        AcupickSnackEvent(
            message = StringIdHelper.Format(R.string.info_snack_orders_allowed_for_batch_handoff, maxSelection.toString()),
            type = SnackType.INFO
        )
    )

    fun onEllipsisClick(order: OrderItemUI) {
        if (!isOrderSelected(order.orderNumber))
            onEllipsisClickEvent.postValue(order)
    }

    fun onClickMarkAsNotHere(order: OrderItemUI) {
        if (activityViewModel.is1Pl.value.orFalse()) {
            updateOnePlArrivalStatus(order)
        } else {
            updateArrivalStatus(order.orderNumber.toLong(), arrived = order.customerArrivalStatus?.combineArriveStatuses() != CustomerArrivalStatusUI.ARRIVED)
        }
    }

    fun onStartTimer(job: Job) {
        timerjobs.add(job)
    }

    override fun onCleared() {
        super.onCleared()
        orderItemsFor1PLToSelect.removeObserver(orderItemsFor1PLToSelectObserver)
        timerjobs.forEach { it.cancel() }
    }

    companion object {
        const val TRANSFER_ORDER_DIALOG_TAG = "transferOrderDialogTag"
        const val OF_AGE_ASSOCIATE_VERIFICATION_TAG = "ofAgeAssociateVerificationTag"
        const val REJECTED_ITEMS_DIALOG_TAG = "rejetctedItemsDialogTag"
        const val SHOW_RX_NOT_PREMITTED_FOR_BATCH_DIALOG = "showRxNotPremittedForBatchDialog"
        const val ONE_PL_GIFTING_CONFIRMATION_DIALOG_TAG = "onePlGiftingConfirmationDialogTag"
        const val ONE_ORDER = 1
    }
}

@Parcelize
@Keep
data class SelectedActivities(val activityList: List<ActivityDto>) : Parcelable
