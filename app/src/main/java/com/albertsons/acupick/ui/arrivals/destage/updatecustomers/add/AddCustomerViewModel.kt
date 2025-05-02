package com.albertsons.acupick.ui.arrivals.destage.updatecustomers.add

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.request.AssignUserWrapperRequestDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.hasAddOnPrescription
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.arrivals.ArrivalsViewModel
import com.albertsons.acupick.ui.arrivals.destage.updatecustomers.AddToHandoffUI
import com.albertsons.acupick.ui.arrivals.destage.updatecustomers.UpdateCustomerBaseViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.util.StringIdHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddCustomerViewModel(
    val app: Application,
) : UpdateCustomerBaseViewModel(app) {

    val returnOrderToAddDataEvent = MutableLiveData<AddToHandoffUI>()
    private val activityDtos: MutableList<ActivityDto> = mutableListOf()
    private val activityDtosWithRx: MutableList<ActivityDto> = mutableListOf()

    override fun onUpdateCustomerCta() {
        val actIdList = selectedOrderItems.value?.map { orderItemUi ->
            orderItemUi?.actId ?: 0L
        }

        activityIdList?.addAll(actIdList ?: emptyList())

        val erIds = selectedOrderItems.value?.map { orderItemUI ->
            orderItemUI?.erId ?: 0
        }
        activityDtos.clear()
        if (erIds != null) {
            getCustomerActivityDtoData(erIds, actIdList)
        }
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
                    negativeButtonText = StringIdHelper.Id(R.string.cancel)
                ),
                tag = ArrivalsViewModel.SHOW_RX_NOT_PREMITTED_FOR_BATCH_DIALOG
            )
        )
    }

    private fun createCustomerNameDialogBody(customerNames: List<ActivityDto>): Pair<String, String> {
        var names = ""
        // val spannableStringBuilder = SpannableStringBuilder()
        val text = StringBuilder()

        names = if (customerNames.size < 2) {
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

    private fun getCustomerActivityDtoData(erIds: List<Long>, actIdList: List<Long>?) {
        viewModelScope.launch(dispatcherProvider.IO) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> {
                    erIds.forEach { erId ->
                        val result = isBlockingUi.wrap { apsRepo.pickUpActivityDetails(id = erId, loadCI = true) }
                        when (result) {
                            is ApiResult.Success<ActivityDto> -> {
                                activityDtos.add(result.data)
                                return@forEach
                            }
                            is ApiResult.Failure -> {
                                if (result is ApiResult.Failure.Server) {
                                    val type = result.error?.errorCode?.resolvedType
                                } else {
                                    handleApiError(result, retryAction = { getCustomerActivityDtoData(erIds, actIdList) })
                                }
                            }
                        }.exhaustive
                    }

                    activityDtosWithRx.clear()
                    activityDtos.forEach {
                        if (it.hasAddOnPrescription()) {
                            activityDtosWithRx.add(it)
                        }
                    }

                    if (activityDtosWithRx.isNotNullOrEmpty()) {
                        val customerNamesString = createCustomerNameDialogBody(activityDtosWithRx)
                        showRxNotPremittedForBatchDialog(customerNamesString)
                        return@launch
                    }
                    val firstOrderItemUi = selectedOrderItems.value?.firstOrNull()
                    if (firstOrderItemUi?.pickerId != null || firstOrderItemUi?.pickerId == userRepo.user.value?.userId) {
                        showTransferHandOffDialog(firstOrderItemUi?.pickerName ?: "")
                    } else {
                        assignToMe(actIdList)
                    }
                }
                else -> {}
            }
        }
    }

    private fun assignToMe(actIdList: List<Long>?, override: Boolean = false) {
        viewModelScope.launch(dispatcherProvider.Main) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> {
                    val result =
                        isBlockingUi.wrap {
                            apsRepo.assignUserToHandoffs(
                                AssignUserWrapperRequestDto(
                                    actIds = actIdList,
                                    replaceOverride = override,
                                    // Todo find out what this does
                                    resetPickList = true,
                                    user = userRepo.user.value!!.toUserDto(),
                                    etaArrivalFlag = siteRepo.isEtaArrivalEnabled
                                )
                            )
                        }

                    when (result) {
                        is ApiResult.Success -> {
                            returnOrderToAddDataEvent.postValue(getCompleteEventForAddCustomer())
                            _navigationEvent.postValue(NavigationEvent.Up)
                        }
                        is ApiResult.Failure -> {
                            handleApiError(result, retryAction = { assignToMe(actIdList, override) })
                        }
                    }
                }
                false -> networkAvailabilityManager.triggerOfflineError { assignToMe(actIdList, override) }
            }
        }
    }

    private fun getCompleteEventForAddCustomer(): AddToHandoffUI {
        val selectedCustomerNames = selectedOrderItems.value?.map { it?.nameShort ?: "" } ?: emptyList()
        val snackBarMessage = app.resources.getQuantityString(
            R.plurals.add_to_handoff_snackbar_prompt_plural_format,
            selectedCustomerNames.count(),
            selectedCustomerNames.firstOrNull(),
            selectedCustomerNames.lastOrNull()
        )

        return AddToHandoffUI(
            erIdIdList = selectedOrderItems.value?.map { it?.erId ?: 0L },
            snackBarData = Pair(snackBarMessage, selectedCustomerNames.count())
        )
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

    init {
        viewModelScope.launch(dispatcherProvider.IO) {
            changeToolbarTitleEvent.postValue(app.getString(R.string.add_customer))
        }

        registerCloseAction(TRANSFER_ORDER_DIALOG_TAG) {
            closeActionFactory(positive = { assignToMe(activityIdList?.toList(), true) })
        }
    }

    companion object {
        const val TRANSFER_ORDER_DIALOG_TAG = "transferOrderDialogTag"
        const val ADD_CUSTOMER_RETURN_RESULT = "addCustomerReturnResult"
        const val ADD_CUSTOMER_RETURN = "addCustomerReturn"
    }
}
