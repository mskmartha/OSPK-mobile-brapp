package com.albertsons.acupick.ui.picklists.open

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.CategoryStatus
import com.albertsons.acupick.data.model.response.ActivityAndErDto
import com.albertsons.acupick.data.model.response.CannotAssignToOrderDialogTypes
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.getListOfConversationSid
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.infrastructure.utils.isValidActivityId
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.dialog.CONTINUE_ORDER_DIALOG_ARG_DATA
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.RELOAD_DILAOG
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.picklists.PickListsBaseViewModel
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.asIcon
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import timber.log.Timber

class OpenPickListsViewModel(
    app: Application,
    activityViewModel: MainActivityViewModel
) : PickListsBaseViewModel(app, activityViewModel) {

    init {
        registerCloseAction(ORDER_CONTINUE_DIALOG_TAG) {
            closeActionFactory(positive = { continueOrder() })
        }
        registerCloseAction(CONFIRMATION_DIALOG_TAG) {
            closeActionFactory(positive = { transferPickToMe() })
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
                            OpenPickListsFragmentDirections.actionToPickListItemsFragment(
                                selectedPickListActivityId.value ?: "",
                                toteEstimate =
                                selectedPickListToteEstimate.value
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
        loadPickData(true, isRefresh) { it?.find { activity -> activity.category == CategoryStatus.OPEN } }
    }

    override fun transferPick() {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (pickRepository.hasActivePickListActivityId()) {
                showContinueOrder()
            } else {

                if (!selectedPickListActivityId.value.isValidActivityId()) {
                    inlineDialogEvent.postValue(
                        CustomDialogArgDataAndTag(
                            data = RELOAD_DILAOG,
                            tag = GENERIC_RELOAD_DIALOG
                        )
                    )
                    return@launch
                }

                isSpinnerShowing.postValue(true)
                val result = assignPickToMe(false)
                isSpinnerShowing.postValue(false)
                when (result) {
                    is ApiResult.Success ->
                        _navigationEvent.postValue(
                            NavigationEvent.Directions(
                                OpenPickListsFragmentDirections.actionToPickListItemsFragment(
                                    selectedPickListActivityId.value ?: "",
                                    toteEstimate =
                                    selectedPickListToteEstimate.value
                                )
                            )
                        )
                    is ApiResult.Failure -> {
                        if (result is ApiResult.Failure.Server) {
                            val type = result.error?.errorCode?.resolvedType
                            when (type?.cannotAssignToOrder()) {
                                true -> {
                                    val selectedList = pickLists.value?.find { it.actId == selectedPickListActivityId.value?.toLong() ?: 0 }
                                    val serverErrorType =
                                        if (type == ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY) CannotAssignToOrderDialogTypes.PICKLIST else CannotAssignToOrderDialogTypes.REGULAR
                                    serverErrorCannotAssignUser(serverErrorType, selectedList?.erId == null)
                                }
                                else -> handleApiError(result, retryAction = { transferPick() })
                            }
                        } else {
                            handleApiError(result, retryAction = { transferPick() })
                        }
                    }
                }
            }
        }.exhaustive
    }

    override val categoryStatus: CategoryStatus
        get() = CategoryStatus.OPEN

    // /////////////////////////////////////////////////////////////////////////
    // Helpers
    // /////////////////////////////////////////////////////////////////////////
    fun continueOrder() {
        if (pickAssigned?.status == ActivityStatus.RELEASED ||
            isPickListCompletedButHasNotStartedStaging(pickAssigned)
        ) {
            _navigationEvent.postValue(navigateToStagingDirections(pickAssigned))
        } else {
            if (!pickRepository.getActivePickListActivityId().isValidActivityId()) {
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = RELOAD_DILAOG,
                        tag = GENERIC_RELOAD_DIALOG
                    )
                )
                return
            }
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
                        OpenPickListsFragmentDirections.actionToPickListItemsFragment(
                            pickRepository.getActivePickListActivityId() ?: "",
                            toteEstimate = pickAssigned?.toteEstimate
                        )
                    )
                )
            }
        }
    }

    private fun showContinueOrder() {
        inlineDialogEvent.postValue(CustomDialogArgDataAndTag(CONTINUE_ORDER_DIALOG_ARG_DATA, tag = ORDER_CONTINUE_DIALOG_TAG))
    }

    override fun onPickClicked(pickList: ActivityAndErDto) {
        Timber.v("[onPickListClicked] pickList=$pickList")
        if (pickAssigned != null) {
            showContinueOrder()
        } else {
            pickList.apply {
                selectedPickListActivityId.set(pickList.actId?.toString() ?: "")
                selectedPickListToteEstimate.set(pickList.toteEstimate)
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = CustomDialogArgData(
                            titleIcon = fulfillment?.asIcon(),
                            title = StringIdHelper.Raw(activityNo.orEmpty()),
                            body = StringIdHelper.Id(R.string.select_picklist_confirmation_dialog_body),
                            positiveButtonText = StringIdHelper.Id(R.string.start),
                            negativeButtonText = StringIdHelper.Id(R.string.cancel),
                            cancelOnTouchOutside = true
                        ),
                        tag = CONFIRMATION_DIALOG_TAG
                    )
                )
            }
        }
    }

/*    private fun checkForAcknowledgedFlashOrder() {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first()) {
                userRepo.user.value?.userId?.let { userId ->
                    val result = apsRepo.getAcknowledgedPickerDetails(userId)
                    if (result is ApiResult.Success) {
                        acknowledgedFlashOrderActId.postValue(result.data.actId)
                    }
                }
            }
        }
    }*/

    override val tagList = listOf(CONFIRMATION_DIALOG_TAG, ORDER_CONTINUE_DIALOG_TAG)

    companion object {
        private const val CONFIRMATION_DIALOG_TAG = "openConfirmationDialog"
        private const val ORDER_CONTINUE_DIALOG_TAG = "openContinueOrderDialog"
    }
}
