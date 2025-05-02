package com.albertsons.acupick.ui.arrivals.destage.removeitems

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.Complete1PLHandoffData
import com.albertsons.acupick.data.model.HandOff1PLInterstitialParams
import com.albertsons.acupick.data.model.RemovedItems
import com.albertsons.acupick.data.model.RemovedItemsAnalytics
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.request.Cancel1PLHandoffRequestDto
import com.albertsons.acupick.data.model.request.RemoveItems1PLRequestDto
import com.albertsons.acupick.data.model.request.RemoveItemsRequestDto
import com.albertsons.acupick.data.model.response.Remove1PLItemsResponseDto
import com.albertsons.acupick.data.model.thatNeedsToBeSplit
import com.albertsons.acupick.data.model.toRemoveItems
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.CompleteHandoff1PLRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.arrivals.destage.DestageOrderPagerViewModel.Companion.GIFTING_CONFIRMATION_DIALOG_TAG
import com.albertsons.acupick.ui.dialog.CANCEL_1PL_HANDOFF_DATA
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.getOrZero
import com.albertsons.acupick.ui.util.orFalse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.time.ZonedDateTime

class RemoveRejected1PLViewModel(app: Application) : BaseViewModel(app) {

    private val apsRepo: ApsRepository by inject()
    private val dispatcherProvider: DispatcherProvider by inject()
    private val siteRepository: SiteRepository by inject()
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    private val completeHandoff1PLRepository: CompleteHandoff1PLRepository by inject()

    val rejectedItems = MutableLiveData(Remove1PLItemsResponseDto())

    val totalOrders = rejectedItems.map { it.totalOrders }
    val vanId = rejectedItems.map { it.vanId }
    val amCount = rejectedItems.map { it.ordersPerZone?.find { it.zone == StorageType.AM }?.rejectedItems ?: 0 }
    val chCount = rejectedItems.map { it.ordersPerZone?.find { it.zone == StorageType.CH }?.rejectedItems ?: 0 }
    val fzCount = rejectedItems.map { it.ordersPerZone?.find { it.zone == StorageType.FZ }?.rejectedItems ?: 0 }
    val htCount = rejectedItems.map { it.ordersPerZone?.find { it.zone == StorageType.HT }?.rejectedItems ?: 0 }

    val removedItemsByStorageType = MutableLiveData<HashMap<StorageType, List<RejectedItemHeaderViewModel>>>(hashMapOf())

    val isAMItemsRemoved = removedItemsByStorageType.map { it.containsKey(StorageType.AM) }
    val isCHItemsRemoved = removedItemsByStorageType.map { it.containsKey(StorageType.CH) }
    val isFZItemsRemoved = removedItemsByStorageType.map { it.containsKey(StorageType.FZ) }
    val isHTItemsRemoved = removedItemsByStorageType.map { it.containsKey(StorageType.HT) }

    val removeEnabled = MutableLiveData(false)

    private var removedItemsStartTime: ZonedDateTime

    init {

        viewModelScope.launch(dispatcherProvider.IO) {
            triggerHomeButtonEvent.asFlow().collect {
                showLeavingScreenDialog()
            }
        }

        registerCloseAction(CANCEL_1PL_HANDOFF_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    cancel1PLHandoff()
                }
            )
        }
        registerCloseAction(GIFTING_CONFIRMATION_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    complete1PLHandoff()
                }
            )
        }

        removedItemsStartTime = ZonedDateTime.now()
    }

    fun navigateUp() = _navigationEvent.postValue(NavigationEvent.Up)

    fun itemClicked(storageType: StorageType) {

        val rejectedItemByZone = rejectedItems.value?.ordersPerZone?.find { it.zone == storageType }

        _navigationEvent.postValue(
            NavigationEvent.Directions(
                RemoveRejected1PLFragmentDirections.actionToRemove1PLRejectedItemsFragment(rejectedItemByZone, vanId.value.orEmpty())
            )
        )
    }

    private fun showLeavingScreenDialog() = inlineDialogEvent.postValue(
        CustomDialogArgDataAndTag(
            data = CANCEL_1PL_HANDOFF_DATA,
            tag = CANCEL_1PL_HANDOFF_DIALOG_TAG
        )
    )

    fun isRemoveButtonEnabled() {
        if ((isAMItemsRemoved.value == true || (amCount.value ?: 0) <= 0) &&
            (isCHItemsRemoved.value == true || (chCount.value ?: 0) <= 0) &&
            (isFZItemsRemoved.value == true || (fzCount.value ?: 0) <= 0) &&
            (isHTItemsRemoved.value == true || (htCount.value ?: 0) <= 0)
        ) {
            removeEnabled.postValue(true)
        } else {
            removeEnabled.postValue(false)
        }
    }

    fun complete1PLHandoffCTA() {
        if (rejectedItems.value?.giftOrderCount.getOrZero() > 0) {
            showGiftNoteConfirmationDialog(rejectedItems.value?.giftOrderCount.getOrZero())
        } else {
            complete1PLHandoff()
        }
    }

    private fun complete1PLHandoff() {
        // val itemsRemovedCount = removedItemsByStorageType.value?.values?.flatten()?.map { it }?.flatMap { it.childItems }?.filter { it.isChecked.value.orFalse() }?.size
        // val misplacedItemsCount = removedItemsByStorageType.value?.values?.flatten()?.map { it }?.flatMap { it.childItems }?.filter { it.isMisplaced.value.orFalse() }?.size
        val itemsRemovedCount = removedItemsByStorageType.value?.values?.flatten()?.map { it }?.flatMap { it.childItems }?.filter { it.isChecked.value.orFalse() }?.sumOf { it.item.qty ?: 0 }
        val misplacedItemsCount = removedItemsByStorageType.value?.values?.flatten()?.map { it }?.flatMap { it.childItems }?.filter { it.isMisplaced.value.orFalse() }?.sumOf { it.item.qty ?: 0 }

        viewModelScope.launch(dispatcherProvider.Main) {
            val handOffDetails = prepareHandOffDetails()
            val handoffInterstitialParam = HandOff1PLInterstitialParams(actId = handOffDetails.actId, removeItems1PLRequestDto = handOffDetails)
            completeHandoff1PLRepository.saveCompleteHandoff(Complete1PLHandoffData(handoffInterstitialParam))

            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    RemoveRejected1PLFragmentDirections.actionToRemove1PLHandoffFragment(
                        totalOrderCount = totalOrders.value.getOrZero(),
                        removedItemsCount = itemsRemovedCount.getOrZero(),
                        misplacedItemsCount = misplacedItemsCount.getOrZero(),
                        handOffInterstitialParamsList = handoffInterstitialParam
                    )
                )
            )
        }
    }

    private fun prepareHandOffDetails(): RemoveItems1PLRequestDto {

        val removeItemRequestList = mutableListOf<RemoveItemsRequestDto>()

        removedItemsByStorageType.value?.flatMap { it.value }?.forEach {

            val splitRejectedItems = it.childItems.filter { it.item.itemType?.thatNeedsToBeSplit().orFalse() }
            val notSplitRejectedItems = it.childItems.filter { !it.item.itemType?.thatNeedsToBeSplit().orFalse() }

            val removedItemsList = mutableListOf<RemovedItems?>()
            if (splitRejectedItems.isNotEmpty()) {
                val groupedList = splitRejectedItems.groupBy { it.item.upcOrPlu }
                groupedList.forEach {
                    removedItemsList.addAll(mergeSplitRejectedItems(it))
                }
            }
            if (notSplitRejectedItems.isNotEmpty()) {
                removedItemsList.addAll(notSplitRejectedItems.filter { it.isChecked.value.orFalse() }.map { it.item.toRemoveItems(scanTimestamp = ZonedDateTime.now()) })
                removedItemsList.addAll(notSplitRejectedItems.filter { it.isMisplaced.value.orFalse() }.map { it.item.toRemoveItems(isMisplaced = true, scanTimestamp = ZonedDateTime.now()) })
            }

            removeItemRequestList.add(
                RemoveItemsRequestDto(
                    orderNo = it.orderNumber,
                    siteId = siteRepository.siteDetails.value?.siteId,
                    entityReference = it.entityReference,
                    timestamp = ZonedDateTime.now(),
                    removedItems = removedItemsList,
                    analytics = RemovedItemsAnalytics(
                        storageType = it.storageType,
                        startTimestamp = removedItemsStartTime,
                        endTimestamp = ZonedDateTime.now(),
                    )
                )
            )
        }
        return RemoveItems1PLRequestDto(
            actId = rejectedItems.value?.activityId,
            siteId = siteRepository.siteDetails.value?.siteId,
            giftLabelPrintConfirmation = rejectedItems.value?.giftOrderCount.getOrZero() > 0,
            vanNumber = vanId.value,
            removeItemsReqs = removeItemRequestList
        )
    }

    private fun mergeSplitRejectedItems(itemMap: Map.Entry<String?, List<Rejected1PLItemDbViewModel>>): MutableList<RemovedItems> {
        val mergedSplitItems = mutableListOf<RemovedItems>()
        when {
            itemMap.value.all { it.isChecked.value.orFalse() } -> {
                itemMap.value.first().item.toRemoveItems(
                    splitCount = itemMap.value.size,
                    scanTimestamp = ZonedDateTime.now()
                ).let { mergedSplitItems.add(it) }
            }

            itemMap.value.all { it.isMisplaced.value.orFalse() } -> {
                itemMap.value.first().item.toRemoveItems(
                    isMisplaced = true,
                    splitCount = itemMap.value.size,
                    scanTimestamp = ZonedDateTime.now()
                ).let { mergedSplitItems.add(it) }
            }

            else -> {
                val completedItems = itemMap.value.filter { it.isChecked.value.orFalse() }
                val misplacedItems = itemMap.value.filter { it.isMisplaced.value.orFalse() }

                if (completedItems.isNotNullOrEmpty()) {
                    completedItems.first().item.toRemoveItems(
                        splitCount = completedItems.size,
                        scanTimestamp = ZonedDateTime.now()
                    ).let { mergedSplitItems.add(it) }
                }
                if (misplacedItems.isNotNullOrEmpty()) {
                    misplacedItems.first().item.toRemoveItems(
                        isMisplaced = true,
                        splitCount = misplacedItems.size,
                        scanTimestamp = ZonedDateTime.now()
                    ).let { mergedSplitItems.add(it) }
                }
            }
        }
        return mergedSplitItems
    }

    private fun cancel1PLHandoff() {
        viewModelScope.launch(dispatcherProvider.IO) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> {
                    val result = isBlockingUi.wrap {
                        apsRepo.cancel1PLHandoff(
                            Cancel1PLHandoffRequestDto(
                                activityId = rejectedItems.value?.activityId,
                                unassignTime = ZonedDateTime.now()
                            )
                        )
                    }
                    when (result) {
                        is ApiResult.Success -> {
                            navigateUp()
                        }

                        is ApiResult.Failure -> {
                            handleApiError(result, retryAction = { cancel1PLHandoff() })
                        }
                    }
                }

                false -> networkAvailabilityManager.triggerOfflineError { cancel1PLHandoff() }
            }
        }
    }

    private fun showGiftNoteConfirmationDialog(giftOrderCount: Int) {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.InformationDialog,
                    title = StringIdHelper.Plural(
                        R.plurals.gift_confirmation_count, giftOrderCount
                    ),
                    positiveButtonText =
                    StringIdHelper.Id(R.string.confirm),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel)
                ),
                GIFTING_CONFIRMATION_DIALOG_TAG
            )
        )
    }

    companion object {
        private const val CANCEL_1PL_HANDOFF_DIALOG_TAG = "cancel1PLHandoff"
    }
}
