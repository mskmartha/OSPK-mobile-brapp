package com.albertsons.acupick.ui.swapsubstitution.myitems

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.request.SubstitutionRejectedReason
import com.albertsons.acupick.data.model.request.UndoPickLocalDto
import com.albertsons.acupick.data.model.request.UndoPickRequestDto
import com.albertsons.acupick.data.model.request.UndoShortRequestDto
import com.albertsons.acupick.data.model.response.SubApprovalStatus
import com.albertsons.acupick.data.model.response.SwapItem
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.picklistitems.REMOVE_SUBSTITUTION_DIALOG_TAG
import com.albertsons.acupick.ui.picklistitems.getRemoveSubstitutionDialogArgDataAndTag
import com.albertsons.acupick.ui.swapsubstitution.QuickTaskBaseViewModel
import com.albertsons.acupick.ui.swapsubstitution.QuickTaskCategories
import com.albertsons.acupick.ui.util.getOrZero
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickTaskMyItemsViewModel(
    app: Application
) : QuickTaskBaseViewModel(app) {

    init {
        registerCloseAction(GENERIC_RELOAD_DIALOG) {
            closeActionFactory(
                positive = {
                    loadData()
                }
            )
        }
        registerCloseAction(REMOVE_SUBSTITUTION_DIALOG_TAG) {
            closeActionFactory(
                positiveWithData = { undoPicksSwapSubstitution() },
                negative = { swapSubItem = null },
                dismiss = { swapSubItem = null },
            )
        }
    }
    override fun loadData(isRefresh: Boolean) {
        loadActivityDetailsData(isRefresh)
    }

    override fun onSwapSubstitutionButtonClick(swapItem: SwapItem) {
        swapSubItem = swapItem
        performSwapSubstitution()
    }

    private fun performSwapSubstitution() {
        swapSubItem?.let {
            when (it.subApprovalStatus) {
                SubApprovalStatus.OUT_OF_STOCK -> undoShorts()
                else -> {
                    it.substitutedWith?.let { substitutedItemList ->
                        inlineDialogEvent.postValue(getRemoveSubstitutionDialogArgDataAndTag(substitutedItemList))
                    }
                }
            }
        }
    }

    private fun undoShorts() {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { undoShorts() }
            } else {
                val requests = swapSubItem?.shortedItemUpc?.map {
                    UndoShortRequestDto(
                        actId = pickRepository.pickList.first()?.actId ?: 0, iaId = swapSubItem?.id, shortedItemId = it.shortedId, qty = it.exceptionQty
                    )
                }
                val results = requests?.let {
                    isBlockingUi.wrap {
                        pickRepository.undoShortages(it)
                    }
                }
                if (results is ApiResult.Failure) {
                    withContext(dispatcherProvider.Main) {
                        toaster.toast(app.getString(R.string.item_details_undo_error))
                        swapSubItem = null
                    }
                } else {
                    navigateToSubstitution()
                }
            }
        }
    }

    private fun undoPicksSwapSubstitution() {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { undoPicksSwapSubstitution() }
            } else {
                swapSubItem?.substitutedWith?.let { substitutionList ->
                    if (substitutionList.isEmpty()) return@launch
                    val substitutionRemovedQty = substitutionList.sumBy { it.qty?.toInt().getOrZero() }
                    val requests =
                        substitutionList.map {
                            UndoPickLocalDto(
                                containerId = it.containerId,
                                undoPickRequestDto = UndoPickRequestDto(
                                    actId = pickRepository.pickList.first()?.actId ?: 0,
                                    iaId = swapSubItem?.id,
                                    netWeight = it.netWeight,
                                    pickedUpcId = it.upcId,
                                    qty = it.qty,
                                    rejectionReason = SubstitutionRejectedReason.SWAP,
                                )
                            )
                        }
                    val results =
                        isBlockingUi.wrap {
                            pickRepository.undoPicks(requests)
                        }
                    if (results is ApiResult.Failure) {
                        withContext(dispatcherProvider.Main) {
                            toaster.toast(app.getString(R.string.item_details_undo_error))
                            swapSubItem = null
                        }
                    } else {
                        navigateToSubstitution(substitutionRemovedQty = substitutionRemovedQty)
                    }
                }
            }
        }
    }

    override val quickTaskCategory: QuickTaskCategories
        get() = QuickTaskCategories.MY_ITEM
}
