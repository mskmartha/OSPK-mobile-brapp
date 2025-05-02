package com.albertsons.acupick.ui.arrivals.destage.updatecustomers

import android.app.Application
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.response.ErDto
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.arrivals.destage.MAX_HANDOFF_COUNT
import com.albertsons.acupick.ui.models.CustomerArrivalStatusUI
import com.albertsons.acupick.ui.models.OrderItemUI
import com.albertsons.acupick.ui.util.getOrZero
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.inject

abstract class UpdateCustomerBaseViewModel(
    app: Application,
) : BaseViewModel(app) {

    // DI
    protected val apsRepo: ApsRepository by inject()
    protected val dispatcherProvider: DispatcherProvider by inject()
    protected val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    protected val userRepo: UserRepository by inject()
    protected val siteRepo: SiteRepository by inject()
    protected val activityViewModel: MainActivityViewModel by inject()

    val selectedOrderItems = MutableLiveData<MutableList<OrderItemUI?>>(mutableListOf())
    val alreadyAssignedCount = MutableLiveData(1)
    val activityIdList: ArrayList<Long>? = null
    val ctaEnabled = selectedOrderItems.map { it.isNotEmpty() }
    val isComplete = MutableLiveData(false)

    val arrivingOrders = MutableLiveData<List<OrderItemUI>>()
    val unAssignedOrders = MutableLiveData<List<OrderItemUI>>()
    val showNoCustomersAssignedUi: LiveData<Boolean> = unAssignedOrders.map { it.isNullOrEmpty() }
    val showNoCustomersArrivedUi: LiveData<Boolean> = arrivingOrders.map { it.isNullOrEmpty() }
    private var fromAddCustomer = false
    val isMaxOrderSelected get() = ((alreadyAssignedCount.value ?: 1) + selectedOrderItems.value?.count().getOrZero()) >= MAX_HANDOFF_COUNT && fromAddCustomer

    abstract fun onUpdateCustomerCta()

    fun loadDetails(isAddCustomer: Boolean = fromAddCustomer) {
        fromAddCustomer = isAddCustomer
        viewModelScope.launch(dispatcherProvider.IO) {
            val user = userRepo.user.value ?: run {
                acuPickLogger.w("[loadHomeData] user null - bypassing function execution")
                return@launch
            }

            val result = isBlockingUi.wrap { apsRepo.searchCustomerPickupOrders(siteId = user.selectedStoreId, onlyPickupReady = true) }

            when (networkAvailabilityManager.isConnected.first()) {
                true -> {
                    when (result) {
                        is ApiResult.Success<List<ErDto>> -> {
                            unAssignedOrders.postValue(
                                result.data.map { OrderItemUI(it) }.filter { order ->
                                    order.customerArrivalStatus == CustomerArrivalStatusUI.ARRIVED_NOT_STARTED
                                }.sortedBy { it.customerArrivalTime }
                            )
                            arrivingOrders.postValue(
                                result.data.map { OrderItemUI(it) }.filter { order ->
                                    order.customerArrivalStatus == CustomerArrivalStatusUI.PICKUP_READY || order.customerArrivalStatus == null
                                }
                            )
                        }

                        is ApiResult.Failure -> {
                            handleApiError(result, retryAction = { loadDetails() })
                        }
                    }.exhaustive
                }

                false -> networkAvailabilityManager.triggerOfflineError { loadDetails() }
            }
        }
    }

    fun onClickItem(item: OrderItemUI?) {
        when {
            isOrderSelected(item) -> deselectItem(item)
            isMaxOrderSelected -> return
            else -> selectItem(item)
        }
        unAssignedOrders.postValue(unAssignedOrders.value)
        arrivingOrders.postValue(arrivingOrders.value)
    }

    private fun selectItem(orderItem: OrderItemUI?) {
        selectedOrderItems.value = selectedOrderItems.value?.apply { add(orderItem) }?.distinct()?.toMutableList()
    }

    private fun deselectItem(item: OrderItemUI?) {
        removeOrderFromSelection(item)
    }

    private fun removeOrderFromSelection(item: OrderItemUI?) {
        selectedOrderItems.value = selectedOrderItems.value?.filterNot { it?.orderNumber == item?.orderNumber }?.toMutableList()
    }

    fun isOrderSelected(item: OrderItemUI?) = selectedOrderItems.value?.any { it?.orderNumber == item?.orderNumber } ?: false
}

@Parcelize
data class AddToHandoffUI(
    val erIdIdList: List<Long>? = null,
    val snackBarData: Pair<String, Int>? = null,
) : Parcelable

@Parcelize
data class MarkedArrivedUI(
    val markedArrived: Long? = null,
    val snackBarData: Pair<String, Int>? = null,
) : Parcelable
