package com.albertsons.acupick.ui.arrivals.destage.removeitems

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.data.model.ItemReasonCode
import com.albertsons.acupick.data.model.RejectedItem
import com.albertsons.acupick.data.model.RejectedItemsByStorageType
import com.albertsons.acupick.data.model.RemovedItems
import com.albertsons.acupick.data.model.RemovedItemsAnalytics
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.request.RemoveItemsRequestDto
import com.albertsons.acupick.data.model.thatNeedsToBeSplit
import com.albertsons.acupick.data.model.toRemoveItems
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.CONFIRM_REMOVAL_OF_ITEMS_THEN_DESTAGE
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.MARK_REJECTED_ITEM_AS_MISPLACED_DIALOG
import com.albertsons.acupick.ui.dialog.REMOVE_ITEMS_BACKOUT
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.models.RemoveRejectedItemUiData
import com.albertsons.acupick.ui.util.orFalse
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.time.ZonedDateTime

class RemoveRejectedItemViewModel(
    app: Application,
    ui: RemoveRejectedItemUiData,
    val currentZone: StorageType? = StorageType.AM,
) : BaseViewModel(app) {

    private val siteRepository: SiteRepository by inject()
    private val dispatcherProvider: DispatcherProvider by inject()

    private val orderUI = MutableLiveData(ui)
    val orderNumber = orderUI.map { it.customerOrderNumber }
    val shortOrderNumber = orderUI.map { it.shortOrderNumber }
    private var removedItemsStartTime: ZonedDateTime
    val contactName = orderUI.map { it.customerName }
    val storageType = orderUI.map { getcurrentZoneList(it.rejectedItemCount)?.storageType }
    val isCustomerBagPreference = orderUI.map { it.isCustomerBagPreference }
    private val removeList = orderUI.map {
        getcurrentZoneList(it.rejectedItemCount)?.rejectedItems
    }
    val itemCount = removeList.map { list ->
        list?.sumOf { rejectedItem ->
            rejectedItem.qty ?: 0
        }
    }
    val rejectedItemsDBList = removeList.map {
        it?.map { item ->
            RejectedItemDbViewModel(
                rejectedItem = item,
                onCheckedChange = ::itemChecked,
                misplacedItemClickListener = ::onMarkAsMisplacedClicked
            )
        } ?: listOf()
    }
    val removeEnabled = MutableLiveData(isRemoveButtonEnabled())
    private var misplacedItem: RejectedItem? = null
    val requestList = MutableLiveData(listOf<RemoveItemsRequestDto>())
    val canceledRejectedItem = MutableLiveData(false)

    init {
        registerCloseAction(MARK_REJECTED_ITEM_AS_MISPLACED_TAG) {
            closeActionFactory(
                positive = { markItemAsMisplaced() }
            )
        }

        registerCloseAction(CONFIRM_REMOVAL_OF_ITEMS_DIALOG) {
            closeActionFactory(
                positive = { navigateToDestaging() }
            )
        }
        registerCloseAction(REMOVE_ITEMS_BACKOUT_DIALOG) {
            closeActionFactory(
                positive = { navigateToDestaging(doNotSavePicks = true) }
            )
        }
        viewModelScope.launch(dispatcherProvider.IO) {
            triggerHomeButtonEvent.asFlow().collect {
                showRemoveItemsBackoutDialog()
            }
        }

        removedItemsStartTime = ZonedDateTime.now()
    }

    private fun itemChecked(rejectedItem: RejectedItem?, isChecked: Boolean) {
        val updatedList = rejectedItemsDBList.value
        updatedList?.find { it.item == rejectedItem }?.apply {
            this.isChecked.value = isChecked
        }
        rejectedItemsDBList.postValue(updatedList)
        removeEnabled.postValue(isRemoveButtonEnabled())
    }

    private fun isRemoveButtonEnabled(): Boolean {
        val notCheckedItems = rejectedItemsDBList.value?.filterNot { it.isChecked.value.orFalse() }
        val notCheckedOrMisplacedItems = notCheckedItems?.filterNot { it.isMisplaced.value.orFalse() }
        return notCheckedOrMisplacedItems?.isEmpty() ?: false
    }

    private fun getcurrentZoneList(rejectedItemCount: List<RejectedItemsByStorageType>) =
        rejectedItemCount.find { it.storageType == currentZone }

    fun navigateUp() = _navigationEvent.postValue(NavigationEvent.Up)

    private fun onMarkAsMisplacedClicked(rejectedItem: RejectedItem?) {
        val dbViewModel = rejectedItemsDBList.value?.find { it.item == rejectedItem }
        if (dbViewModel?.isMisplaced?.value == false) {
            misplacedItem = rejectedItem
            inlineDialogEvent.postValue(
                CustomDialogArgDataAndTag(
                    data = MARK_REJECTED_ITEM_AS_MISPLACED_DIALOG,
                    tag = MARK_REJECTED_ITEM_AS_MISPLACED_TAG
                )
            )
        }
    }

    private fun markItemAsMisplaced() {
        val updatedList = rejectedItemsDBList.value
        updatedList?.find { it.item == misplacedItem }?.apply {
            this.markItemAsMisplaced()
        }
        rejectedItemsDBList.postValue(updatedList)
        removeEnabled.postValue(isRemoveButtonEnabled())
    }

    private fun getRemovedItems(): RemoveItemsRequestDto {
        val splitRejectedItems = rejectedItemsDBList.value?.filter { it.item?.itemType?.thatNeedsToBeSplit().orFalse() } ?: emptyList()
        val notSplitRejectedItems = rejectedItemsDBList.value?.filter { !it.item?.itemType?.thatNeedsToBeSplit().orFalse() } ?: emptyList()
        val removedItemsList = mutableListOf<RemovedItems?>()
        if (splitRejectedItems.isNotEmpty()) {
            val groupedList = splitRejectedItems.groupBy { it.item?.upcOrPlu }
            groupedList.forEach {
                removedItemsList.addAll(mergeSplitRejectedItems(it))
            }
        }
        if (notSplitRejectedItems.isNotEmpty()) {
            removedItemsList.addAll(notSplitRejectedItems.filter { it.isChecked.value.orFalse() }.map { it.item?.toRemoveItems(scanTimestamp = ZonedDateTime.now()) })
            removedItemsList.addAll(notSplitRejectedItems.filter { it.isMisplaced.value.orFalse() }.map { it.item?.toRemoveItems(isMisplaced = true, scanTimestamp = ZonedDateTime.now()) })
        }

        return RemoveItemsRequestDto(
            orderNo = orderUI.value?.customerOrderNumber,
            siteId = siteRepository.siteDetails.value?.siteId,
            entityReference = orderUI.value?.entityReference,
            timestamp = ZonedDateTime.now(),
            removedItems = removedItemsList,
            analytics = RemovedItemsAnalytics(
                storageType = currentZone,
                startTimestamp = removedItemsStartTime,
                endTimestamp = ZonedDateTime.now(),
                quantity = getRemovedAmount()
            )
        )
    }

    private fun mergeSplitRejectedItems(itemMap: Map.Entry<String?, List<RejectedItemDbViewModel>>): MutableList<RemovedItems> {
        val mergedSplitItems = mutableListOf<RemovedItems>()
        when {
            itemMap.value.all { it.isChecked.value.orFalse() } -> {
                itemMap.value.first().item?.toRemoveItems(
                    splitCount = itemMap.value.size,
                    scanTimestamp = ZonedDateTime.now()
                )?.let { mergedSplitItems.add(it) }
            }
            itemMap.value.all { it.isMisplaced.value.orFalse() } -> {
                itemMap.value.first().item?.toRemoveItems(
                    isMisplaced = true,
                    splitCount = itemMap.value.size,
                    scanTimestamp = ZonedDateTime.now()
                )?.let { mergedSplitItems.add(it) }
            }
            else -> {
                val completedItems = itemMap.value.filter { it.isChecked.value.orFalse() }
                val misplacedItems = itemMap.value.filter { it.isMisplaced.value.orFalse() }

                if (completedItems.isNotNullOrEmpty()) {
                    completedItems.first().item?.toRemoveItems(
                        splitCount = completedItems.size,
                        scanTimestamp = ZonedDateTime.now()
                    )?.let { mergedSplitItems.add(it) }
                }
                if (misplacedItems.isNotNullOrEmpty()) {
                    misplacedItems.first().item?.toRemoveItems(
                        isMisplaced = true,
                        splitCount = misplacedItems.size,
                        scanTimestamp = ZonedDateTime.now()
                    )?.let { mergedSplitItems.add(it) }
                }
            }
        }
        return mergedSplitItems
    }

    private fun getRemovedAmount() = rejectedItemsDBList.value?.sumOf { it.item?.qty ?: 0 } ?: 0

    fun removeItemsClicked() =
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CONFIRM_REMOVAL_OF_ITEMS_THEN_DESTAGE,
                tag = CONFIRM_REMOVAL_OF_ITEMS_DIALOG
            )
        )

    fun showRemoveItemsBackoutDialog() =
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = REMOVE_ITEMS_BACKOUT,
                tag = REMOVE_ITEMS_BACKOUT_DIALOG
            )
        )

    private fun navigateToDestaging(doNotSavePicks: Boolean = false) {
        val list = mutableListOf<RemoveItemsRequestDto>()
        getRemovedItems().let { removeItemsRequestDto ->
            if (!doNotSavePicks && removeItemsRequestDto.allItemsAreComplete()) {
                list.add(removeItemsRequestDto)
            } else {
                canceledRejectedItem.postValue(value = true)
                navigateUp()
            }
        }
        requestList.postValue(list)
    }

    private fun RemoveItemsRequestDto.allItemsAreComplete() =
        this.removedItems?.all {
            it?.itemReasonCode == ItemReasonCode.REMOVED_ITEM || it?.itemReasonCode == ItemReasonCode.MISSING_ITEM ||
                it?.itemReasonCode == ItemReasonCode.REMOVED_INEDIT
        }.orFalse()

    companion object {
        private const val MARK_REJECTED_ITEM_AS_MISPLACED_TAG = "markRejectedItemAsMisplaced"
        private const val CONFIRM_REMOVAL_OF_ITEMS_DIALOG = "confirmRemovalOfItemsDialog"
        private const val REMOVE_ITEMS_BACKOUT_DIALOG = "removeItemsBackoutDialog"
    }
}
