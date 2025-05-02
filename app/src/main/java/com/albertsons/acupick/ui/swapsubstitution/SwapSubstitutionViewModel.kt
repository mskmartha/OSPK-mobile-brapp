package com.albertsons.acupick.ui.swapsubstitution

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.request.SubstitutionRejectedReason
import com.albertsons.acupick.data.model.request.UndoPickLocalDto
import com.albertsons.acupick.data.model.request.UndoPickRequestDto
import com.albertsons.acupick.data.model.request.UndoShortRequestDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.SubApprovalStatus
import com.albertsons.acupick.data.model.response.SwapItem
import com.albertsons.acupick.data.model.response.fullContactName
import com.albertsons.acupick.data.model.response.isIssueScanned
import com.albertsons.acupick.data.model.response.isShorted
import com.albertsons.acupick.data.model.response.isSubstituted
import com.albertsons.acupick.data.model.response.toSwapItem
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.toast.Toaster
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.picklistitems.REMOVE_SUBSTITUTION_DIALOG_TAG
import com.albertsons.acupick.ui.picklistitems.getRemoveSubstitutionDialogArgDataAndTag
import com.albertsons.acupick.ui.substitute.SubstituteParams
import com.albertsons.acupick.ui.substitute.SubstitutionPath
import com.albertsons.acupick.ui.util.getOrZero
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class SwapSubstitutionViewModel(val app: Application) : BaseViewModel(app) {
    // DI
    val dispatcherProvider: DispatcherProvider by inject()
    private val toaster: Toaster by inject()

    val swapSubstitutionData = MutableLiveData<List<SwapItem?>>()
    val pickRepository: PickRepository by inject()
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()

    val isDataLoading: LiveData<Boolean> = MutableLiveData(true)
    private var swapSubItem: SwapItem? = null

    init {
        changeToolbarTitleEvent.postValue(pickRepository.pickList.value?.fullContactName() ?: "")

        registerCloseAction(REMOVE_SUBSTITUTION_DIALOG_TAG) {
            closeActionFactory(
                positiveWithData = { undoPicksSwapSubstitution() },
                negative = { swapSubItem = null },
                dismiss = { swapSubItem = null },
            )
        }
    }

    fun setupData(swapItems: List<SwapItem?>) {
        isDataLoading.postValue(false)
        swapSubstitutionData.postValue(swapItems)
    }

    fun getSwapSubstitutionDataFromNetwork() {
        CoroutineScope(Dispatchers.IO).launch {
            if (networkAvailabilityManager.isConnected.first()) {
                val actId = pickRepository.pickList.first()?.actId.toString()
                val result = pickRepository.getActivityDetails(id = actId)
                when (result) {
                    is ApiResult.Success -> {
                        setupData(result.data.toSubstituteItems())
                    }

                    is ApiResult.Failure -> {
                        // TODO add faioure case
                    }
                }.exhaustive
                isDataLoading.postValue(false)
            } else {
                networkAvailabilityManager.triggerOfflineError { getSwapSubstitutionDataFromNetwork() }
                isDataLoading.postValue(false)
            }
        }
    }

    fun onSwapSubstitutionButtonClick(swapItem: SwapItem) {
        swapSubItem = swapItem
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

    // Unshort oos item before navigating to swap subsitution flow
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

    private fun navigateToSubstitution(substitutionRemovedQty: Int? = null) {
        viewModelScope.launch(dispatcherProvider.Main) {
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    SwapSubstitutionFragmentDirections.actionSwapSubstitutionFragmentToSubstituteFragment(
                        SubstituteParams(
                            iaId = swapSubItem?.id,
                            pickListId = pickRepository.pickList.first()?.actId.toString(),
                            path = SubstitutionPath.SWAPSUBSTITUTION,
                            substitutionRemovedQty = substitutionRemovedQty,
                        )
                    )
                )
            )
        }
    }
}

fun ActivityDto.toSubstituteItems(): List<SwapItem?> {
    val swapItems = mutableListOf<SwapItem>()
    this.itemActivities?.filter { it.isSubstituted || it.isIssueScanned }?.forEach { itemActivityDto ->
        swapItems.add(itemActivityDto.toSwapItem())
    }
    this.itemActivities?.filter { it.isShorted }?.forEach { itemActivityDto ->
        swapItems.add(itemActivityDto.toSwapItem(true))
    }
    return swapItems
}
