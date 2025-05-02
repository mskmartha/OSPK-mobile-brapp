package com.albertsons.acupick.ui.swapsubstitution

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.response.SubApprovalStatus
import com.albertsons.acupick.data.model.response.SwapItem
import com.albertsons.acupick.data.model.response.isBatchOrder
import com.albertsons.acupick.data.model.response.toMySwapSubstitutedItem
import com.albertsons.acupick.data.model.response.toOtherPickerSwapSubstitutedItem
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.PushNotificationsRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.data.toast.Toaster
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.substitute.SubstituteParams
import com.albertsons.acupick.ui.substitute.SubstitutionPath
import com.albertsons.acupick.ui.substitute.SwapSubstitutionReason
import com.albertsons.acupick.ui.util.orFalse
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.inject

abstract class QuickTaskBaseViewModel(
    val app: Application
) : BaseViewModel(app) {
    // DI
    protected val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    protected val dispatcherProvider: DispatcherProvider by inject()
    protected val userRepo: UserRepository by inject()
    protected val pickRepository: PickRepository by inject()
    protected val siteRepository: SiteRepository by inject()
    protected val toaster: Toaster by inject()
    private val pushNotificationsRepository: PushNotificationsRepository by inject()
    // Data
    val swapSubstitutionList = MutableLiveData<List<SwapItem?>>()
    val isBatchOrder = pickRepository.pickList.value?.isBatchOrder().orFalse()
    var swapSubItem: SwapItem? = null
    var substitutionReason: SwapSubstitutionReason? = null
    // UI
    val isDataLoading: LiveData<Boolean> = MutableLiveData(true)
    val isSpinnerShowing: LiveData<Boolean> = MutableLiveData(false)
    val isDataRefreshing: LiveData<Boolean> = MutableLiveData(false)
    val emptyView = swapSubstitutionList.map {
        it.isEmpty()
    }
    val isSkeletonStateShowing = combine(isDataLoading.asFlow(), isDataRefreshing.asFlow()) { loading, refreshing ->
        loading && refreshing.not()
    }.asLiveData()

    val isRepickOriginalItemAllowed = siteRepository.twoWayCommsFlags?.allowRepickOriginalItem.orFalse()
    val isMasterOrderViewPhase1Enabled = siteRepository.twoWayCommsFlags?.masterOrderView.orFalse()
    val isMasterOrderViewPhase2Enabled = siteRepository.twoWayCommsFlags?.masterOrderView2.orFalse()
    abstract fun loadData(isRefresh: Boolean = false)
    abstract val quickTaskCategory: QuickTaskCategories
    abstract fun onSwapSubstitutionButtonClick(swapItem: SwapItem)

    // /////////////////////////////////////////////////////////////////////////
    // Common functions
    // /////////////////////////////////////////////////////////////////////////

    fun loadActivityDetailsData(isRefresh: Boolean = false, isMasterViewEnabled: Boolean? = null) {
        viewModelScope.launch(dispatcherProvider.IO) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> {
                    val actId = pickRepository.pickList.first()?.actId.toString()
                    val result = (if (isRefresh) isDataRefreshing else isDataLoading).wrap { pickRepository.getActivityDetailsForSwapSubstitution(id = actId, loadMasterView = isMasterViewEnabled) }
                    when (result) {
                        is ApiResult.Success -> {
                            val resultList = when (isMasterViewEnabled) {
                                true -> result.data.toOtherPickerSwapSubstitutedItem()
                                else -> result.data.toMySwapSubstitutedItem()
                            }
                            /**
                             * Shopassist for batching: To filter the respective customer order items.
                             * Setting customer current chat conversation order number in [PushNotificationsRepository] on chat screen.
                             * here we are getting the same value from [PushNotificationsRepository.getInProgressChatOrderId]
                             * There is only single path to launch quick task screen from chat screen.
                             */
                            swapSubstitutionList.postValue(
                                resultList
                                    .filter {
                                        it?.customerOrderNumber == (
                                            pushNotificationsRepository.getInProgressChatOrderId()
                                                ?: pickRepository.pickList.value?.customerOrderNumber
                                            )
                                    }
                            )
                        }

                        is ApiResult.Failure -> {
                            loadActivityDetailsData()
                        }
                    }.exhaustive
                }

                false -> {
                    networkAvailabilityManager.triggerOfflineError { loadActivityDetailsData() }
                }
            }
        }
    }
    protected fun navigateToSubstitution(substitutionRemovedQty: Int? = null) {
        viewModelScope.launch(dispatcherProvider.Main) {
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    QuickTaskPagerFragmentDirections.actionQuickTaskPagerFragmentToSubstituteFragment(
                        SubstituteParams(
                            iaId = swapSubItem?.id,
                            path = SubstitutionPath.SWAPSUBSTITUTION,
                            substitutionRemovedQty = substitutionRemovedQty,
                            swapSubstitutionReason = getSwapSubstitutionReason(),
                            pickListId = pickRepository.pickList.first()?.actId.toString()
                        )
                    )
                )
            )
        }
    }

    private fun getSwapSubstitutionReason(): SwapSubstitutionReason? {
        swapSubItem?.let {
            substitutionReason = when (it.subApprovalStatus) {
                SubApprovalStatus.OUT_OF_STOCK -> when (quickTaskCategory) {
                    QuickTaskCategories.MY_ITEM -> if (isMasterOrderViewPhase1Enabled) SwapSubstitutionReason.SWAP_OOS else SwapSubstitutionReason.SWAP
                    QuickTaskCategories.OTHER_SHOPPERS_ITEM -> SwapSubstitutionReason.SWAP_OOS_OTHER_PICKLIST
                }

                else -> when (quickTaskCategory) {
                    QuickTaskCategories.MY_ITEM -> SwapSubstitutionReason.SWAP
                    QuickTaskCategories.OTHER_SHOPPERS_ITEM -> SwapSubstitutionReason.SWAP_OTHER_PICKLIST
                }
            }
        }
        return substitutionReason
    }
}
