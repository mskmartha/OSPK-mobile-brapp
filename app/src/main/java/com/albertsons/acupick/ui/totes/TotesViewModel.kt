package com.albertsons.acupick.ui.totes

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.EndPickReasonCode
import com.albertsons.acupick.data.model.ShortReasonCode
import com.albertsons.acupick.data.model.ToteEstimate
import com.albertsons.acupick.data.model.isAdvancePick
import com.albertsons.acupick.data.model.request.PickCompleteRequestDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.CannotAssignToOrderDialogTypes
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.fullContactName
import com.albertsons.acupick.data.model.response.getListOfOrderNumber
import com.albertsons.acupick.data.model.response.isSubstituted
import com.albertsons.acupick.data.model.response.isWineOrder
import com.albertsons.acupick.data.model.response.stageByTime
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.isValidActivityId
import com.albertsons.acupick.infrastructure.utils.orZero
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.dialog.ALREADY_ASSIGNED_PICKLIST_ARG_DATA
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.RELOAD_DILAOG
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.picklistitems.COMPLETED_PICK_DIALOG_TAG
import com.albertsons.acupick.ui.picklistitems.COMPLETE_PICKING_EARLY_EXIT_DIALOG_TAG
import com.albertsons.acupick.ui.picklistitems.COMPLETE_WINE_PICKING_EARLY_EXIT_DIALOG_TAG
import com.albertsons.acupick.ui.picklistitems.END_PICK_DIALOG_TAG
import com.albertsons.acupick.ui.picklistitems.END_PICK_REASON_DIALOG_TAG
import com.albertsons.acupick.ui.picklistitems.END_PICK_WITH_EXCEPTIONS_DIALOG_TAG
import com.albertsons.acupick.ui.picklistitems.PickListItemsFragmentDirections
import com.albertsons.acupick.ui.picklistitems.PickListItemsViewModel
import com.albertsons.acupick.ui.picklistitems.getCompletePickArgDataAndTag
import com.albertsons.acupick.ui.picklistitems.getCompletePickErrorArgDataAndTag
import com.albertsons.acupick.ui.picklistitems.getEarlyExitArgDataAndTag
import com.albertsons.acupick.ui.picklistitems.getEarlyExitWineArgDataAndTag
import com.albertsons.acupick.ui.picklistitems.getEndPickConfirmationArgDataAndTag
import com.albertsons.acupick.ui.picklistitems.getEndPickReasonConfirmationDialogArgData
import com.albertsons.acupick.ui.picklistitems.getEndPickWithExceptionsArgDataAndTag
import com.albertsons.acupick.ui.staging.winestaging.WineStagingParams
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StringIdHelper
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import timber.log.Timber
import kotlin.math.roundToInt

class TotesViewModel(
    private val app: Application,
    private val activityViewModel: MainActivityViewModel,
    private val pickRepository: PickRepository,
    private val dispatcherProvider: DispatcherProvider,
) : BaseViewModel(app) {

    private val apsRepo: ApsRepository by inject()
    private val userRepo: UserRepository by inject()
    private val pickRepo: PickRepository by inject()
    private val conversationsRepository: ConversationsRepository by inject()
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()

    /** True when code paths to execute the complete pick api currently being run. False when outside that code block/logic */
    private var completeCallInProgress = false

    /** True when the complete pick api call returns a successful result */
    private var completeCallSuccessful = false
    private var isPrintingSuccessful = false
    val unAssignSuccessfulAction: LiveData<Unit> = LiveEvent()

    /** Source of truth */
    val pickList = pickRepository.pickList.asLiveData()
    private var isAnyItemPicked = false
    private var areAllItemsPicked = false
    private var areAllItemsShorted = false

    fun onChatClicked(orderNumber: String) {
        viewModelScope.launch {
            pickRepository.pickList.value?.orderChatDetails?.firstOrNull { orderChatDetail ->
                orderNumber == orderChatDetail.customerOrderNumber
            }?.let {
                _navigationEvent.postValue(
                    NavigationEvent.Directions(
                        NavGraphDirections.actionToChatFragment(
                            orderNumber = orderNumber,
                            convetsationId = it.conversationSid.orEmpty(),
                            fulfullmentOrderNumber = it.referenceEntityId.orEmpty()
                        )
                    )
                )
            }
        }
    }

    fun isWineShipping(): Boolean = pickList.value?.isWineOrder() ?: false
    lateinit var pickListId: String
    var toteEstimate: ToteEstimate? = null

    /** Current assignedUserId */
    private val assignedUserId = pickList.map {
        it?.assignedTo?.userId
    }

    /** Unit count in Picked tab */
    private val pickedItemCount = pickList.map {
        it?.itemActivities?.sumOf { item ->
            when {
                item.isSubstituted -> item.qty.orZero()
                else -> item.processedQty.orZero()
            }
        }.orZero().roundToInt().toString()
    }

    private val activityDto: LiveData<ActivityDto> = MutableLiveData()
    val totesList = activityDto.map { activity ->
        val dbVmList = arrayListOf<TotesHeaderItemDbViewModel>()
        val totesUi = activity.itemActivities?.map { item ->
            TotesUi(item, activity)
        }
        totesUi?.groupBy { it.orderNumber }?.forEach { entry ->
            dbVmList.add(
                TotesHeaderItemDbViewModel(entry.value, activity, TotesListSubRv(entry.value.first().totesSubUi), toteEstimate)
            )
        }
        dbVmList
    }

    private val pickListObserver: Observer<ActivityDto?> = Observer<ActivityDto?> {
        if (it != null) {
            areAllItemsPicked = (it.itemActivities?.isNotEmpty() ?: false) && (pickList.value?.itemActivities?.all { item -> item.isFullyPicked() } ?: false)
            areAllItemsShorted = it.itemActivities?.all { item -> item.isFullyShorted() } == true
            isAnyItemPicked = it.itemActivities?.any { item -> item.isPartiallyPicked() } == true
        }
    }

    init {
        viewModelScope.launch(dispatcherProvider.IO) {
            Timber.v("[init]")
            pickRepository.pickList.filterNotNull().collect { pickList ->
                updateToolbarUi(pickList)
                activityDto.postValue(pickList)
            }
        }

        pickList.observeForever(pickListObserver)
        registerCloseAction(END_PICK_DIALOG_TAG) { closeActionFactory(positive = { unAssignPicker() }) }

        registerCloseAction(END_PICK_WITH_EXCEPTIONS_DIALOG_TAG) {
            closeActionFactory(positive = {
                showEndPickReasonDialog()
            })
        }

        registerCloseAction(END_PICK_REASON_DIALOG_TAG) {
            closeActionFactory(positive = { selection ->
                val endPickReasonCode = getSelectedEndPickReason((selection as Int))
                when (isWineShipping()) {
                    true -> completeWinePicking(completeWithExceptions = true, endPickReasonCode = endPickReasonCode)
                    else -> completePicking(completeWithExceptions = true, endPickReasonCode = endPickReasonCode)
                }
            })
        }

        registerCloseAction(COMPLETE_PICKING_EARLY_EXIT_DIALOG_TAG) { closeActionFactory(positive = { showEndPickReasonDialog() }) }

        registerCloseAction(COMPLETED_PICK_DIALOG_TAG) { closeActionFactory(positive = { completePicking(completeWithExceptions = false) }) }

        registerCloseAction(COMPLETE_WINE_PICKING_EARLY_EXIT_DIALOG_TAG) { closeActionFactory(positive = { showEndPickReasonDialog() }) }

        registerCloseAction(GENERIC_RELOAD_DIALOG) {
            closeActionFactory(
                positive = {
                    // Re using the Live data observer to re direct to home screen
                    unAssignSuccessfulAction.postValue(Unit)
                }
            )
        }
    }

    private fun updateToolbarUi(activity: ActivityDto?) {
        if (isUiActive) {
            with(activityViewModel) {
                setToolbarTitle(app.resources.getString(R.string.pick_list_info_header))
                setToolbarRightExtraCta(app.resources.getString(R.string.end_pick)) {
                    onEndPickCtaClicked(false)
                }
            }
        }
    }

    /** Call from associated fragment's onCreateView and onDestroyView functions to update UI active state */
    override fun updateUiLifecycle(active: Boolean) {
        super.updateUiLifecycle(active)
        if (active) {
            viewModelScope.launch(dispatcherProvider.Default) {
                updateToolbarUi(pickRepository.pickList.first())
            }
        }
    }

    // TODO Redesign - End pick flow is duplicated for now. Need to refactor this. Find is there any way to pass pickListItemViewModel object to this view model and access end pick flow.
    private fun onEndPickCtaClicked(skipConfirmationDialog: Boolean = false) {
        viewModelScope.launch(dispatcherProvider.Main) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> if (isWineShipping()) {
                    startEndWinePickingProcess()
                } else {
                    if (areAllItemsPicked || areAllItemsShorted) { // making sure todolist is empty
                        _navigationEvent.postValue(NavigationEvent.Up)
                    } else {
                        startEndPickProcess(skipConfirmationDialog)
                    }
                }
                false -> networkAvailabilityManager.triggerOfflineError { onEndPickCtaClicked(skipConfirmationDialog) }
            }.exhaustive
        }
    }

    private fun startEndWinePickingProcess(skipConfirmationDialog: Boolean = true) {
        acuPickLogger.v("[onCompletePickListCtaClicked]")
        if (skipConfirmationDialog) {
            when {
                areAllItemsPicked ->
                    if (areAllItemsShorted) {
                        inlineDialogEvent.postValue(getEarlyExitWineArgDataAndTag())
                    } else {
                        completeWinePicking(completeWithExceptions = areAllItemsShorted)
                    }
                isAnyItemPicked -> showEndPickReasonDialog()
                else -> unAssignPicker()
            }
        } /*else {
            when {
                areAllItemsPicked && areAllItemsShorted -> inlineDialogEvent.postValue(getEarlyExitWineArgDataAndTag())
            }
        }*/
        completeCallInProgress = false
    }

    private fun startEndPickProcess(skipConfirmationDialog: Boolean) {
        acuPickLogger.v("[onCompletePickListCtaClicked]")
       /* if (skipConfirmationDialog) {
            when {
                areAllItemsPicked -> {
                    if (areAllItemsShorted) {
                        completePicking(completeWithExceptions = true)
                    } else {
                        completePicking(completeWithExceptions = false)
                    }
                }
                isAnyItemPicked -> completePicking(completeWithExceptions = true)
                else -> unAssignPicker()
            }
        } else {*/
        when {
            areAllItemsPicked ->
                inlineDialogEvent.postValue(
                    if (areAllItemsShorted) {
                        // All item shorted end pick reason code required
                        getEarlyExitArgDataAndTag()
                    } else {
                        // No items present in to-do list, end pick reason code not required
                        getCompletePickArgDataAndTag()
                    }
                )
            isAnyItemPicked -> {
                val allItems = pickList.value?.itemActivities
                val itemsNotPicked = allItems?.filter { item -> !item.isFullyPicked() }?.size ?: 0
                // Partially picked end pick reason code required
                inlineDialogEvent.postValue(getEndPickWithExceptionsArgDataAndTag(itemsNotPicked))
            }
            else -> inlineDialogEvent.postValue(getEndPickConfirmationArgDataAndTag()) // No any items picked
        }
        // }
    }

    /** Notifies the backend the user wishes to end the current picking activity */
    private fun completePicking(completeWithExceptions: Boolean, endPickReasonCode: EndPickReasonCode? = null) {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { completePicking(completeWithExceptions, endPickReasonCode) }
                acuPickLogger.v("[completePicking] offline - unable to complete picking")
            } else {
                // TODO handle auto short/skip/shortage code
                val result = isBlockingUi.wrap {
                    pickRepository.completePickForStaging(
                        getPickCompleteRequestDto(completeWithExceptions, endPickReasonCode), (userRepo.user.value?.tokenizedLdapId ?: "")
                    )
                }
                when (result) {
                    is ApiResult.Success -> {
                        completeCallSuccessful = true
                        if (result.data.nextActivityId != null) {
                            printToteLabel(activityId = pickList.value?.actId.toString())
                        } else {
                            _navigationEvent.postValue(NavigationEvent.Up)
                        }
                    }
                    is ApiResult.Failure -> {
                        if (result is ApiResult.Failure.Server) {
                            val type = result.error?.errorCode?.resolvedType
                            if (type?.cannotAssignToOrder() == true) {
                                val serverErrorType = if (type == ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY) CannotAssignToOrderDialogTypes.PICKLIST else CannotAssignToOrderDialogTypes.REGULAR
                                serverErrorCannotAssignUser(serverErrorType, pickRepository.pickList.value?.erId == null, true)
                            } else {
                                handleApiError(result, tag = PickListItemsViewModel.RELOAD_LOAD_PICKLIST_DIALOG_TAG, retryAction = { completePicking(completeWithExceptions) })
                            }
                        } else {
                            inlineDialogEvent.postValue(getCompletePickErrorArgDataAndTag())
                        }
                    }
                }.exhaustive
            }
            completeCallInProgress = false
        }
    }

    private fun getPickCompleteRequestDto(completeWithExceptions: Boolean, endPickReasonCode: EndPickReasonCode? = null): PickCompleteRequestDto {
        val shortageReasonCode = if (pickList.value?.prePickType.isAdvancePick()) ShortReasonCode.PICK_LATER else ShortReasonCode.TOTE_FULL
        return PickCompleteRequestDto(
            actId = pickList.value?.actId ?: 0,
            autoShortage = completeWithExceptions,
            shortageReasonCode = shortageReasonCode.takeIf { completeWithExceptions },
            endPickReasonCode = endPickReasonCode,
            skipCompleteValidation = completeWithExceptions,
            userId = userRepo.user.value?.userId
        )
    }

    private fun printToteLabel(activityId: String) {
        acuPickLogger.v("[printToteLabel]")
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                isPrintingSuccessful = false
                networkAvailabilityManager.triggerOfflineError { printToteLabel(activityId) }
            } else {
                val result = isBlockingUi.wrap { apsRepo.printToteLabel(activityId) }
                isPrintingSuccessful = when (result) {
                    is ApiResult.Success -> true
                    is ApiResult.Failure -> false
                }.exhaustive
            }
            onLabelSentToPrinter(activityId)
            // inlineDialogEvent.postValue(getLabelSentToPrinterArgDataAndTag())
        }
    }

    fun onLabelSentToPrinter(activityId: String) {
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                TotesFragmentDirections.actionTotesFragmentToStagingFragment(
                    activityId = activityId,
                    isPreviousPrintSuccessful = isPrintingSuccessful,
                    shouldClearData = true
                )
            )
        )
    }

    /** Notifies the backend the user wishes to end the current picking activity */
    private fun completeWinePicking(completeWithExceptions: Boolean, endPickReasonCode: EndPickReasonCode? = null) {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { completeWinePicking(completeWithExceptions, endPickReasonCode) }
                acuPickLogger.v("[completePicking] offline - unable to complete picking")
            } else {
                // TODO handle auto short/skip/shortage code
                val result = isBlockingUi.wrap {
                    pickRepository.completePickForStaging(
                        getPickCompleteRequestDto(completeWithExceptions = completeWithExceptions, endPickReasonCode = endPickReasonCode),
                        (userRepo.user.value?.tokenizedLdapId ?: "")
                    )
                }
                when (result) {
                    is ApiResult.Success -> {
                        completeCallSuccessful = true
                        if (result.data.nextActivityId != null) {
                            navigateToWineShipping()
                        } else {
                            delay(500)
                            unAssignSuccessfulAction.postValue(Unit)
                        }
                    }
                    is ApiResult.Failure -> {
                        if (result is ApiResult.Failure.Server) {
                            val type = result.error?.errorCode?.resolvedType
                            if (type?.cannotAssignToOrder() == true) {
                                val serverErrorType = if (type == ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY) CannotAssignToOrderDialogTypes.PICKLIST else CannotAssignToOrderDialogTypes.REGULAR
                                serverErrorCannotAssignUser(serverErrorType, pickRepository.pickList.value?.erId == null, true)
                            } else {
                                handleApiError(result, tag = PickListItemsViewModel.RELOAD_LOAD_PICKLIST_DIALOG_TAG, retryAction = { completePicking(completeWithExceptions) })
                            }
                        } else {
                            inlineDialogEvent.postValue(getCompletePickErrorArgDataAndTag())
                        }
                    }
                }.exhaustive
            }
            completeCallInProgress = false
        }
    }

    private fun navigateToWineShipping() {
        val params = WineStagingParams(
            contactName = pickList.value?.fullContactName().orEmpty(),
            activityId = pickList.value?.actId?.toString().orEmpty(),
            entityId = pickList.value?.entityReference?.entityId.orEmpty(),
            shortOrderNumber = pickList.value?.shortOrderNumber.orEmpty(),
            customerOrderNumber = pickList.value?.customerOrderNumber.orEmpty(),
            stageByTime = pickList.value?.stageByTime().orEmpty(),
            pickedUpBottleCount = pickedItemCount.value.orEmpty()
        )
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                PickListItemsFragmentDirections.actionToWineStagingFragment(
                    wineStagingParams = params
                )
            )
        )
    }

    private fun unAssignPicker() {
        if (!pickListId.isValidActivityId()) {
            inlineDialogEvent.postValue(
                CustomDialogArgDataAndTag(
                    data = RELOAD_DILAOG,
                    tag = GENERIC_RELOAD_DIALOG
                )
            )
            return
        }
        viewModelScope.launch(dispatcherProvider.IO) {
            val assignedUserId = getFreshAssignedId()
            if (userRepo.user.value?.userId == assignedUserId) {
                // Assigned User ID is your User ID.  Continue as normal.
                val activityNo = pickList.value?.activityNo.orEmpty()
                val result = isBlockingUi.wrap {
                    pickRepository.unAssignUser(
                        actId = pickListId,
                        userId = userRepo.user.value?.userId,
                        orderIds = pickList.value?.getListOfOrderNumber(),
                        tokenizedLdap = (userRepo.user.value?.tokenizedLdapId ?: "")
                    )
                }
                when (result) {
                    is ApiResult.Success -> {
                        withContext(dispatcherProvider.Main) {
                            showSnackBar(getUnassignSuccessSnackEvent(activityNo))
                            delay(2000)
                            unAssignSuccessfulAction.postValue(Unit)
                        }
                    }
                    is ApiResult.Failure -> {
                        // retryAction = { unAssignPicker() }
                        handleApiError(result, tag = PickListItemsViewModel.RETRY_UNASSIGN_PICKER_DIALOG_TAG)
                    }
                }.exhaustive
            } else {
                // Assigned User Id is different. Show toast.
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(ALREADY_ASSIGNED_PICKLIST_ARG_DATA, PickListItemsViewModel.PICK_ASSIGNED_TO_DIFFERENT_USER_TAG)
                )
            }
        }
    }

    private suspend fun getFreshAssignedId(): String? {
        return when (
            val result = pickRepository.getActivityDetails(
                id = pickListId.also {
                    it.logError(
                        "Activity Id is empty. TotesViewModel(getFreshAssignedId), Order Id-${pickRepository.pickList.first()?.customerOrderNumber}, User Id-${
                        userRepo.user
                            .value?.userId
                        },"
                    )
                }
            )
        ) {
            is ApiResult.Success -> {
                result.data.assignedTo?.userId
            }
            is ApiResult.Failure -> {
                assignedUserId.value
            }
        }
    }

    private fun getUnassignSuccessSnackEvent(activityNo: String) = AcupickSnackEvent(
        StringIdHelper.Format(R.string.un_assign_success_format, activityNo),
        SnackType.SUCCESS
    )

    override fun onCleared() {
        super.onCleared()
        Timber.v("[onCleared]")
        pickList.removeObserver(pickListObserver)
    }

    private fun showEndPickReasonDialog() {
        inlineDialogEvent.postValue(getEndPickReasonConfirmationDialogArgData())
    }

    private fun getSelectedEndPickReason(selection: Int?): EndPickReasonCode? {
        return selection?.let { endPickReasonCodeList[it] }
    }

    companion object {
        private val endPickReasonCodeList =
            listOf(EndPickReasonCode.PICKING_ANOTHER_ORDER, EndPickReasonCode.HANDOFF_CUSTOMER, EndPickReasonCode.TOTE_FULL, EndPickReasonCode.OTHER)
    }
}
