package com.albertsons.acupick.ui.arrivals.destage.removeitems

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.data.model.RejectedItem
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.response.RejectedItemsByZone
import com.albertsons.acupick.data.model.response.build1PLRejectedList
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.CANCEL_1PL_HANDOFF_DATA
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.MARK_REJECTED_1PL_ITEM_AS_MISPLACED_DIALOG
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.util.orFalse
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class RemoveRejected1PLItemsViewModel(app: Application, rejectedItemsByZone: RejectedItemsByZone/*, vanId: String*/) : BaseViewModel(app) {

    private val dispatcherProvider: DispatcherProvider by inject()

    private val rejectedOrdersUI = MutableLiveData(rejectedItemsByZone)
    val storageType = rejectedOrdersUI.map { it.zone }
    val orderDetails = rejectedOrdersUI.map { it.orderDetails }
    var vanId = ""

    var rejectedItemsDBList: LiveData<List<RejectedItemHeaderViewModel>> = MutableLiveData(listOf())

    val removeEnabled = MutableLiveData(false)
    var misplacedItem: RejectedItem? = null
    val requestList = MutableLiveData(listOf<RejectedItemHeaderViewModel>())

    init {
        viewModelScope.launch(dispatcherProvider.IO) {
            triggerHomeButtonEvent.asFlow().collect {
                navigateUp()
            }
        }
        registerCloseAction(MARK_REJECTED_1PL_ITEM_AS_MISPLACED_TAG) {
            closeActionFactory(
                positive = { markItemAsMisplaced() },
                negative = {}
            )
        }

        registerCloseAction(CANCEL_1PL_HANDOFF_DIALOG_TAG) {
            closeActionFactory(
                positive = { navigateToArrival() }
            )
        }
    }

    private fun isRemoveButtonEnabled(): Boolean {
        rejectedItemsDBList.value?.forEach { header ->
            header.childItems.forEach { item ->
                if ((item.isChecked.value?.orFalse() == false) && (item.isMisplaced.value?.orFalse() == false)) {
                    return false
                }
            }
        }
        return true
    }

    fun onRemoveClicked() {
        requestList.postValue(rejectedItemsDBList.value)
    }

    fun getRejectedItems() {
        rejectedItemsDBList = orderDetails.map { list ->
            list.map { orderDetails ->
                RejectedItemHeaderViewModel(
                    shortOrderNumber = "$vanId - ${orderDetails.stopNumber}",
                    orderNumber = orderDetails.orderNumber,
                    entityReference = orderDetails.entityReference,
                    storageType = storageType.value ?: StorageType.AM,
                    childItems =
                    build1PLRejectedList(orderDetails.totesPerZone).map { rejectedItem ->
                        Rejected1PLItemDbViewModel(
                            rejectedItem = rejectedItem,
                            onCheckedChange = ::itemChecked,
                            misplacedItemClickListener = ::onMarkAsMisplacedClicked
                        )
                    }
                )
            }
        }
    }

    fun navigateUp() = _navigationEvent.postValue(NavigationEvent.Up)

    private fun navigateToArrival() {
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                RemoveRejected1PLItemsFragmentDirections.actionToArrivalsOrdersPagerFragment()
            )
        )
    }

    private fun showLeavingScreenDialog() = inlineDialogEvent.postValue(
        CustomDialogArgDataAndTag(
            data = CANCEL_1PL_HANDOFF_DATA,
            tag = CANCEL_1PL_HANDOFF_DIALOG_TAG
        )
    )

    private fun itemChecked(rejectedItem: RejectedItem?, isChecked: Boolean) {
        val updatedList = rejectedItemsDBList.value
        updatedList?.flatMap { it.childItems }?.find { it.item === rejectedItem }?.apply {
            this.isChecked.value = isChecked
        }
        removeEnabled.postValue(isRemoveButtonEnabled())
    }

    private fun markItemAsMisplaced() {
        val updatedList = rejectedItemsDBList.value
        updatedList?.flatMap { it.childItems }?.find { it.item === misplacedItem }?.apply {
            this.markItemAsMisplaced()
        }

        removeEnabled.postValue(isRemoveButtonEnabled())
    }

    fun onMarkAsMisplacedClicked(rejectedItem: RejectedItem?) {
        misplacedItem = rejectedItem
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = MARK_REJECTED_1PL_ITEM_AS_MISPLACED_DIALOG,
                tag = MARK_REJECTED_1PL_ITEM_AS_MISPLACED_TAG
            )
        )
    }

    companion object {
        private const val MARK_REJECTED_1PL_ITEM_AS_MISPLACED_TAG = "markRejectedItemAsMisplaced"
        private const val CANCEL_1PL_HANDOFF_DIALOG_TAG = "cancel1PLHandoff"
    }
}
