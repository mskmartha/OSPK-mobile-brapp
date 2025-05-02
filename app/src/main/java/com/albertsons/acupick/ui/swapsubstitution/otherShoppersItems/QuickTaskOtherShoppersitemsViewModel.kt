package com.albertsons.acupick.ui.swapsubstitution.otherShoppersItems

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.SwapItem
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.swapsubstitution.QuickTaskBaseViewModel
import com.albertsons.acupick.ui.swapsubstitution.QuickTaskCategories
import com.albertsons.acupick.ui.util.StringIdHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class QuickTaskOtherShoppersitemsViewModel(
    app: Application,
) : QuickTaskBaseViewModel(app) {
    init {

        registerCloseAction(GENERIC_RELOAD_DIALOG) {
            closeActionFactory(positive = {
                loadData()
            })
        }
    }

    override fun loadData(isRefresh: Boolean) {
        loadActivityDetailsData(isRefresh, true)
    }

    override fun onSwapSubstitutionButtonClick(swapItem: SwapItem) {
        swapSubItem = swapItem
        validateOtherPickerState()
    }

    private fun validateOtherPickerState() {
        viewModelScope.launch(dispatcherProvider.IO) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> {
                    val actId = pickRepository.pickList.first()?.actId.toString()
                    val customerOrderNumber = swapSubItem?.customerOrderNumber.takeIf { isBatchOrder }
                    val result = isBlockingUi.wrap {
                        pickRepository.isOtherPickerActive(id = actId, itemId = swapSubItem?.itemId, orderNumber = customerOrderNumber)
                    }
                    when (result) {
                        is ApiResult.Success -> {
                            navigateToSubstitution()
                        }

                        is ApiResult.Failure -> {
                            if (result is ApiResult.Failure.Server) {
                                when (result.error?.errorCode?.resolvedType) {
                                    ServerErrorCode.PICKING_IN_PROGRESS -> showSwapSubstitutioErrorDialog()
                                    else -> handleApiError(errorType = result)
                                }
                            } else {
                                handleApiError(errorType = result)
                            }
                        }
                    }.exhaustive
                }

                false -> {
                    networkAvailabilityManager.triggerOfflineError { validateOtherPickerState() }
                }
            }
        }
    }

    private fun showSwapSubstitutioErrorDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Format(R.string.swap_substitution_error_dialog_title, swapSubItem?.assignedTo?.firstName ?: ""),
                    body = StringIdHelper.Id(R.string.swap_substitution_error_dialog_description),
                    positiveButtonText = StringIdHelper.Id(R.string.ok_cta),
                    cancelOnTouchOutside = true
                ),
                tag = SWAP_SUBSTITUTION_ERROR_DIALOG_TAG
            )
        )
    }

    override val quickTaskCategory: QuickTaskCategories
        get() = QuickTaskCategories.OTHER_SHOPPERS_ITEM

    companion object {
        private const val SWAP_SUBSTITUTION_ERROR_DIALOG_TAG = "swapSubstitutionErrorDialogTag"
    }
}
