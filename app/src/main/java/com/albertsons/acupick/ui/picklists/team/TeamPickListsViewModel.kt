package com.albertsons.acupick.ui.picklists.team

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.ActivityType
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.CategoryStatus
import com.albertsons.acupick.data.model.request.firstInitialDotLastName
import com.albertsons.acupick.data.model.response.ActivityAndErDto
import com.albertsons.acupick.data.model.response.ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY
import com.albertsons.acupick.data.model.response.fullContactName
import com.albertsons.acupick.data.model.response.getListOfConversationSid
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.infrastructure.utils.isValidActivityId
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.dialog.ALREADY_ASSIGNED_PICKLIST_ARG_DATA
import com.albertsons.acupick.ui.dialog.CANCELED_BATCH_PICKLIST_ARG_DATA
import com.albertsons.acupick.ui.dialog.CANCELED_SINGLE_PICKLIST_ARG_DATA
import com.albertsons.acupick.ui.dialog.CONTINUE_ORDER_DIALOG_ARG_DATA
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.RELOAD_DILAOG
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.picklists.PickListsBaseViewModel
import com.albertsons.acupick.ui.staging.winestaging.WineStagingParams
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.asIcon
import com.albertsons.acupick.ui.util.orFalse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import timber.log.Timber

class TeamPickListsViewModel(
    app: Application,
    activityViewModel: MainActivityViewModel,
) : PickListsBaseViewModel(app, activityViewModel) {

    // TODO - Look into moving pieces of init into parent
    init {
        registerCloseAction(RETRY_TRANSFER_TO_ME_TAG) {
            closeActionFactory(positive = { transferPick() })
        }

        registerCloseAction(ORDER_CONTINUE_DIALOG_TAG) {
            closeActionFactory(positive = { continueOrder() })
        }

        registerCloseAction(TRANSFER_PICK_TO_ME_DIALOG_TAG) {
            closeActionFactory(positive = { transferPickToMe() })
        }

        registerCloseAction(REASSIGN_ORDER_DIALOG_TAG) {
            closeActionFactory(positive = { transferPick() })
        }

        registerCloseAction(SINGLE_ORDER_ERROR_DIALOG_TAG) {
            closeActionFactory(positive = { loadData() })
        }

        registerCloseAction(GENERIC_RELOAD_DIALOG) {
            closeActionFactory(
                positive = {
                    loadData()
                }
            )
        }

        registerCloseAction(BATCH_ORDER_ERROR_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    _navigationEvent.postValue(
                        NavigationEvent.Directions(
                            TeamPickListsFragmentDirections.actionToPickListItemsFragment(
                                selectedPickListActivityId.value ?: "", toteEstimate = selectedPickListToteEstimate.value
                            )
                        )
                    )
                }
            )
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Overrides from PickListBaseViewModel
    // /////////////////////////////////////////////////////////////////////////
    override fun loadData(isRefresh: Boolean) {
        loadPickData(false, isRefresh) {
            it?.find { activity -> activity.category == CategoryStatus.ASSIGNED }
        }
    }

    // Logic to determine wether we reassign the pick because it is in staging or to just transfer it because it has not been picked yet
    override fun transferPick() {
        val pick = pickLists.value?.filter { it.actId == selectedPickListActivityId.value?.toLong() }
        pickAssigned = pick?.firstOrNull()
        val prevId = pick?.firstOrNull()?.prevActivityId.toString()
        if (pick?.firstOrNull()?.actType == ActivityType.DROP_OFF) {
            reAssignStaging(prevId)
        } else {
            transferPickList()
        }
    }

    override val categoryStatus: CategoryStatus
        get() = CategoryStatus.ASSIGNED

    private fun reAssignStaging(prevId: String) {
        viewModelScope.launch(dispatcherProvider.IO) {
            isSpinnerShowing.postValue(true)
            val results = reAssignToMe()
            isSpinnerShowing.postValue(false)
            handleReAssignResults(results, prevId)
        }
    }

    private fun handleReAssignResults(result: ApiResult<Unit>, prevId: String) {
        viewModelScope.launch(dispatcherProvider.IO) {
            when (result) {
                is ApiResult.Success -> navigateAfterTransfer()
                is ApiResult.Failure -> {
                    isDataLoading.postValue(false)
                    if (result is ApiResult.Failure.Server) {
                        val type = result.error?.errorCode?.resolvedType
                        when (type?.cannotAssignToOrder()) {
                            true -> {
                                if (type == CANNOT_ASSIGN_COMPLETED_ACTIVITY) {
                                    inlineDialogEvent.postValue(
                                        CustomDialogArgDataAndTag(
                                            data = CANCELED_SINGLE_PICKLIST_ARG_DATA,
                                            tag = RELOAD_LOAD_DATA_DIALOG_TAG
                                        )
                                    )
                                } else {
                                    inlineDialogEvent.postValue(CustomDialogArgDataAndTag(data = ALREADY_ASSIGNED_PICKLIST_ARG_DATA, tag = SINGLE_ORDER_ERROR_DIALOG_TAG))
                                }
                            }

                            else -> handleApiError(result, retryAction = { handleReAssignResults(result, prevId) })
                        }
                    } else {
                        handleApiError(errorType = result, retryAction = { handleReAssignResults(result, prevId) })
                    }
                }
            }.exhaustive
        }
    }

    private fun navigateAfterTransfer() {
        if (!isWineOrder.value.orFalse()) {
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    NavGraphDirections.actionPickListFragmentToStagingFragment(
                        activityId = pickAssigned?.prevActivityId?.toString().orEmpty(),
                        isPreviousPrintSuccessful = true,
                        shouldClearData = true
                    )
                )
            )
        } else {
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    TeamPickListsFragmentDirections.actionToWineStagingFragment(
                        wineStagingParams = WineStagingParams(
                            contactName = pickAssigned?.fullContactName().orEmpty(),
                            shortOrderNumber = pickAssigned?.shortOrderNumber.orEmpty(),
                            customerOrderNumber = pickAssigned?.customerOrderNumber.orEmpty(),
                            stageByTime = pickAssigned?.stageByTime().orEmpty(),
                            activityId = pickAssigned?.prevActivityId.toString(),
                            entityId = pickAssigned?.entityReference?.entityId.orEmpty(),
                            pickedUpBottleCount = pickAssigned?.itemQty?.toString().orEmpty()
                        )
                    )
                )
            )
        }
    }

    private fun handleTransferResults() {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (pickRepository.hasActivePickListActivityId()) {
                showContinueOrder()
            } else {
                if (!selectedPickListActivityId.value.isValidActivityId()) {
                    inlineDialogEvent.postValue(CustomDialogArgDataAndTag(data = RELOAD_DILAOG, tag = GENERIC_RELOAD_DIALOG))
                    return@launch
                }
                isSpinnerShowing.postValue(true)
                val assignResult = assignPickToMe(replaceOverride = true, isFromTeamPickList = true)
                when (assignResult) {
                    is ApiResult.Success ->
                        _navigationEvent.postValue(NavigationEvent.Directions(TeamPickListsFragmentDirections.actionToPickListItemsFragment(selectedPickListActivityId.value ?: "")))
                    is ApiResult.Failure -> {
                        if (assignResult is ApiResult.Failure.Server) {
                            val type = assignResult.error?.errorCode?.resolvedType
                            when (type?.cannotAssignToOrder()) {
                                true -> {
                                    if (type == CANNOT_ASSIGN_COMPLETED_ACTIVITY) {
                                        val selectedList = pickLists.value?.find { activity -> activity.actId == (selectedPickListActivityId.value?.toLong() ?: 0) }
                                        val data = if (selectedList?.erId == null) CANCELED_BATCH_PICKLIST_ARG_DATA else CANCELED_SINGLE_PICKLIST_ARG_DATA
                                        inlineDialogEvent.postValue(
                                            CustomDialogArgDataAndTag(
                                                data = data,
                                                tag = RELOAD_LOAD_DATA_DIALOG_TAG
                                            )
                                        )
                                    } else {
                                        inlineDialogEvent.postValue(CustomDialogArgDataAndTag(data = ALREADY_ASSIGNED_PICKLIST_ARG_DATA, tag = SINGLE_ORDER_ERROR_DIALOG_TAG))
                                    }
                                }

                                else -> handleApiError(assignResult, retryAction = { handleTransferResults() })
                            }
                        } else {
                            handleApiError(errorType = assignResult, retryAction = { handleTransferResults() })
                        }
                    }
                }.exhaustive

                isSpinnerShowing.postValue(false)
            }
        }
    }

    private fun transferPickList() {
        viewModelScope.launch(dispatcherProvider.IO) {
            handleTransferResults()
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Helpers
    // /////////////////////////////////////////////////////////////////////////
    fun continueOrder() {
        if ((pickAssigned != null && !pickAssigned?.actId.toString().isValidActivityId()) || !pickRepository.getActivePickListActivityId().isValidActivityId()) {
            inlineDialogEvent.postValue(
                CustomDialogArgDataAndTag(
                    data = RELOAD_DILAOG,
                    tag = GENERIC_RELOAD_DIALOG
                )
            )
            return
        }

        if (pickAssigned != null) {
            if (pickAssigned?.status == ActivityStatus.RELEASED) {
                navigationEvent.postValue(navigateToStagingDirections(pickAssigned))
            } else {
                viewModelScope.launch {
                    if (pickAssigned?.erId != null) {
                        pickAssigned?.customerOrderNumber?.let {
                            val sid = conversationsRepository.getConversationId(it)
                            if (sid.isNotNullOrEmpty()) {
                                conversationsRepository.insertOrUpdateConversation(sid)
                            }
                        } ?: run { conversationsRepository.clear() }
                    } else {
                        pickAssigned?.getListOfConversationSid()?.map { sid ->
                            async { conversationsRepository.insertOrUpdateConversation(sid) }
                        }?.awaitAll() ?: run { conversationsRepository.clear() }
                    }
                    _navigationEvent.postValue(
                        NavigationEvent.Directions(
                            TeamPickListsFragmentDirections.actionToPickListItemsFragment(
                                pickAssigned?.actId.toString(), toteEstimate = selectedPickListToteEstimate.value
                            )
                        )
                    )
                }
            }
        } else {
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    TeamPickListsFragmentDirections.actionToPickListItemsFragment(
                        pickRepository.getActivePickListActivityId() ?: "",
                        toteEstimate = selectedPickListToteEstimate.value
                    )
                )
            )
        }
    }

    private fun showContinueOrder() {
        inlineDialogEvent.postValue(CustomDialogArgDataAndTag(CONTINUE_ORDER_DIALOG_ARG_DATA, tag = ORDER_CONTINUE_DIALOG_TAG))
    }

    override fun onPickClicked(pickList: ActivityAndErDto) {
        Timber.v("[onTeamPickListClicked] pickList=$pickList")
        selectedPickListActivityId.set(pickList.actId?.toString() ?: "")
        selectedPickListToteEstimate.set(pickList.toteEstimate)
        val pick = pickLists.value?.filter { it.actId == selectedPickListActivityId.value?.toLong() }
        when {
            (isPickListCompletedButHasNotStartedStaging(pickList)) -> _navigationEvent.postValue(navigateToStagingDirections(pickList))

            // If pick list was already assigned to myself, still try to assign to myself in case someone took it (ACUPICK-1313)
            (pickList.actId == pickAssigned?.prevActivityId || pickList.actId == pickAssigned?.actId) -> {
                viewModelScope.launch(dispatcherProvider.IO) {
                    val assignResult = assignPickToMe(replaceOverride = false, isFromTeamPickList = true)
                    when (assignResult) {
                        is ApiResult.Success ->
                            continueOrder()
                        is ApiResult.Failure -> {
                            if (assignResult is ApiResult.Failure.Server) {
                                val type = assignResult.error?.errorCode?.resolvedType
                                when (type?.cannotAssignToOrder()) {
                                    true -> {
                                        if (type == CANNOT_ASSIGN_COMPLETED_ACTIVITY) {
                                            val selectedList = pickLists.value?.find { it.actId == selectedPickListActivityId.value?.toLong() ?: 0 }
                                            val data = if (selectedList?.erId == null) CANCELED_BATCH_PICKLIST_ARG_DATA else CANCELED_SINGLE_PICKLIST_ARG_DATA
                                            inlineDialogEvent.postValue(
                                                CustomDialogArgDataAndTag(
                                                    data = data,
                                                    tag = RELOAD_LOAD_DATA_DIALOG_TAG
                                                )
                                            )
                                        } else {
                                            inlineDialogEvent.postValue(CustomDialogArgDataAndTag(data = ALREADY_ASSIGNED_PICKLIST_ARG_DATA, tag = SINGLE_ORDER_ERROR_DIALOG_TAG))
                                        }
                                    }
                                    else -> handleApiError(assignResult, retryAction = { onPickClicked(pickList) })
                                }
                            } else {
                                handleApiError(errorType = assignResult, retryAction = { onPickClicked(pickList) })
                            }
                        }
                    }.exhaustive
                }
            }

            (pickAssigned != null) -> showContinueOrder()

            (pick?.firstOrNull()?.actType == ActivityType.DROP_OFF) -> {
                pick.firstOrNull()?.apply {
                    val argData = CustomDialogArgData(
                        titleIcon = R.drawable.ic_alert,
                        title = StringIdHelper.Id(R.string.select_team_picklist_reassign_confirmation_dialog_title),
                        body = StringIdHelper.Format(R.string.select_team_picklist_reassign_confirmation_dialog_body, assignedTo?.firstInitialDotLastName() ?: ""),
                        positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
                        negativeButtonText = StringIdHelper.Id(R.string.cancel),
                        cancelOnTouchOutside = true
                    )
                    selectedPickListActivityId.set(pickList.actId?.toString() ?: "")
                    inlineDialogEvent.postValue(CustomDialogArgDataAndTag(argData, tag = REASSIGN_ORDER_DIALOG_TAG))
                }
            }
            else -> {
                pickList.apply {
                    val argData = CustomDialogArgData(
                        titleIcon = fulfillment?.asIcon(),
                        title = StringIdHelper.Id(R.string.select_team_picklist_confirmation_dialog_title),
                        body = StringIdHelper.Id(R.string.select_team_picklist_confirmation_dialog_body),
                        positiveButtonText = StringIdHelper.Id(R.string.start),
                        negativeButtonText = StringIdHelper.Id(R.string.cancel),
                        cancelOnTouchOutside = true
                    )
                    selectedPickListActivityId.set(pickList.actId?.toString() ?: "")
                    inlineDialogEvent.postValue(CustomDialogArgDataAndTag(argData, tag = TRANSFER_PICK_TO_ME_DIALOG_TAG))
                }
            }
        }
    }

    override val tagList = listOf(RELOAD_LOAD_DATA_DIALOG_TAG, TRANSFER_PICK_TO_ME_DIALOG_TAG, RETRY_TRANSFER_TO_ME_TAG, ORDER_CONTINUE_DIALOG_TAG, REASSIGN_ORDER_DIALOG_TAG)

    companion object {
        private const val RELOAD_LOAD_DATA_DIALOG_TAG = "reloadTeamLoadDataDialogTag"
        private const val TRANSFER_PICK_TO_ME_DIALOG_TAG = "transferTeamPickToMeDialogTag"
        private const val RETRY_TRANSFER_TO_ME_TAG = "retryTeamTransferToMeDialogTag"
        private const val ORDER_CONTINUE_DIALOG_TAG = "teamContinueOrderDialogTag"
        private const val REASSIGN_ORDER_DIALOG_TAG = "reAssignOrder"
    }
}
