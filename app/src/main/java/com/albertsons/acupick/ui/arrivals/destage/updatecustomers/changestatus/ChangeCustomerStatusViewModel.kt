package com.albertsons.acupick.ui.arrivals.destage.updatecustomers.changestatus

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.CustomerArrivalStatus
import com.albertsons.acupick.data.model.request.UpdateDugArrivalStatusRequestDto
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.models.OrderItemUI
import com.albertsons.acupick.ui.arrivals.destage.updatecustomers.MarkedArrivedUI
import com.albertsons.acupick.ui.arrivals.destage.updatecustomers.UpdateCustomerBaseViewModel
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class ChangeCustomerStatusViewModel(
    val app: Application,
) : UpdateCustomerBaseViewModel(app) {

    val returnOrderToUpdateDataEvent = LiveEvent<MarkedArrivedUI>()

    override fun onUpdateCustomerCta() {
        updateCustomerStatus(selectedOrderItems.value?.toList() ?: emptyList())
    }

    private fun updateCustomerStatus(orderList: List<OrderItemUI?>) {
        viewModelScope.launch(dispatcherProvider.Main) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> {
                    orderList.map { order ->
                        isBlockingUi.wrap {
                            apsRepo.updateDugArrivalStatus(
                                UpdateDugArrivalStatusRequestDto(
                                    customerArrivalStatus = CustomerArrivalStatus.ARRIVED,
                                    erId = order?.erId,
                                    orderNumber = order?.orderNumber,
                                    siteId = order?.siteId?.toLongOrNull() ?: 0,
                                    statusEventTimestamp = ZonedDateTime.now(),
                                    estimateTimeOfArrival = null
                                )
                            )
                        }
                    }
                    returnOrderToUpdateDataEvent.postValue(getCompleteEventForStatusChange())
                    _navigationEvent.postValue(NavigationEvent.Up)
                }
                false -> networkAvailabilityManager.triggerOfflineError { updateCustomerStatus(orderList) }
            }
        }
    }

    private fun getCompleteEventForStatusChange(): MarkedArrivedUI {
        val orderCount = selectedOrderItems.value?.count() ?: 0
        val message = app.resources.getQuantityString(R.plurals.marked_as_arrived_snackbar_prompt_plural_format, orderCount, orderCount - 1, selectedOrderItems.value?.first()?.nameShort ?: "")
        return MarkedArrivedUI(
            markedArrived = selectedOrderItems.value?.first()?.actId,
            snackBarData = Pair(message, orderCount)
        )
    }

    init {
        viewModelScope.launch {
            changeToolbarTitleEvent.postValue(app.getString(R.string.mark_as_arrived))
        }
    }

    companion object {
        const val UPDATE_CUSTOMER_STATUS_RETURN_RESULT = "UpdateCustomerStatusReturnResult"
        const val UPDATE_CUSTOMER_STATUS_RETURN = "UpdateCustomerStatusReturn"
    }
}
