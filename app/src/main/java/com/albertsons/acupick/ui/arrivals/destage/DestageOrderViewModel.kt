package com.albertsons.acupick.ui.arrivals.destage

import android.app.Application
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.AcuPickConfig
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.RejectedItemsByStorageType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.response.FE_SCREEN_STATUS_STORE_NOTIFIED
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryBaseViewModel.Companion.MANUAL_ENTRY_STORAGE_TYPE
import com.albertsons.acupick.ui.models.BagLabel
import com.albertsons.acupick.ui.models.CustomerArrivalStatusUI
import com.albertsons.acupick.ui.models.DestageOrderUiData
import com.albertsons.acupick.ui.models.FulfillmentTypeUI
import com.albertsons.acupick.ui.models.RxBagUI
import com.albertsons.acupick.ui.models.ZonedBagsScannedData
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.combineWith
import com.albertsons.acupick.ui.util.getOrZero
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.toFormatHelper
import com.albertsons.acupick.ui.util.transform
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.io.Serializable
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class DestageOrderViewModel(val app: Application) : BaseViewModel(app) {

    // DI
    private val siteRepo: SiteRepository by inject()

    // Input
    val activity = MutableLiveData<DestageOrderUiData>()

    // Event
    val pharmacySheetEvent: LiveData<Boolean> = LiveEvent()

    // UI
    val isMfcOrder = MutableLiveData<Boolean>()
    val isCustomerBagPreference: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val isBagBypassEnabled = AcuPickConfig.bagBypassEnabled.asLiveData()
    val detailsHeaderUi = MutableLiveData<DetailsHeaderUi>()
    val zonedBagUiData = MutableLiveData<List<ZonedBagsScannedData>>()
    val scannedOrderNumber: MutableStateFlow<String?> = MutableStateFlow(null)
    val currentBagLabel: MutableStateFlow<BagLabel?> = MutableStateFlow(null)
    val isCustomerSubsEnabled = ObservableBoolean(false)
    val rxDeliveryFailureReason = MutableLiveData("")
    val rxDeliveryFailureReasonLiveEvent = LiveEvent<Boolean>()
    val isRxDug = MutableLiveData(false)
    val rxBagList = MutableLiveData<List<RxBagUI>>(emptyList())
    val hasAddOnPrescription = MutableLiveData(false)
    val hasLooseItem = MutableLiveData(false)
    val selectedStorageType = MutableSharedFlow<Int?>()

    val totalRxBagCount = rxBagList.transform {
        it?.size ?: 0
    }
    val scannedRxBags = MutableStateFlow<List<RxBagUI>>(emptyList())
    val totalScannedItems = MutableStateFlow(value = 0)
    val arrivalLabel = MutableStateFlow<BarcodeType.PharmacyArrivalLabel?>(null)
    val arrivalLabelText = arrivalLabel.map { it?.rawBarcode }.asLiveData()
    val hasArrivalLabel = arrivalLabel.map { it != null }.asLiveData()
    val showEllipsis = combine(hasArrivalLabel.asFlow(), scannedRxBags, totalRxBagCount.asFlow()) { labelScanned, scannedRxBags, totalRxBags ->
        labelScanned && scannedRxBags.size != totalRxBags
    }.asLiveData()
    val rxOrderNumberOne = MutableLiveData<RxBagUI>()
    val reprintGiftNote = LiveEvent<Boolean>()
    val rxOrdersLabelText = rxBagList.map { if (it.isNotNullOrEmpty()) R.string.rx_order_numbers.toFormatHelper(it.size.toString()).getString(app) else "" }
    val rxOrdersPrescriptions = rxBagList.map {
        it.mapIndexed { index, rxBagUI ->
            rxBagUI.bagNumber.orEmpty() to R.string.rx_order_prescription.toFormatHelper((index + 1).toString(), rxBagUI.bagNumber.orEmpty()).getString(app)
        }
    }

    val showNoLocationScanned = isRxDug.combineWith(arrivalLabelText) { isRx, scannedRxLocation ->
        isRx.orFalse() && scannedRxLocation.isNullOrBlank()
    }

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            siteRepo.siteDetails.collect { siteDetails ->
                isCustomerSubsEnabled.set(siteDetails?.isCustomerApprovedSubstitutionEnabled ?: false)
            }
        }
    }

    // Group bags by zone to count, also mark current zone here
    private val ordersByZoneAndId = combine(currentBagLabel, zonedBagUiData.asFlow()) { current, bags ->

        bags.groupBy { Pair(it.bagData?.zoneId, it.bagData?.zoneType) }.map {
            ZonedBagsScannedData(
                bagData = it.value.first().bagData,
                currentBagsScanned = it.value.sumOf { scanned -> scanned.currentBagsScanned },
                bagsScanned = it.value.sumOf { scanned -> scanned.bagsScanned },
                looseScanned = it.value.sumOf { scanned -> scanned.looseScanned },
                totalBagsForZone = it.value.sumOf { total -> total.totalBagsForZone },
                totalBags = it.value.filter { scanned -> scanned.bagData?.isLoose == false || scanned.bagData?.isLoose == null }.sumOf { total -> total.totalBags },
                totalLoose = it.value.filter { scanned -> scanned.bagData?.isLoose == true }.sumOf { total -> total.totalLoose },
                totalBagsPerLocation = it.value.filter { scanned -> scanned.bagData?.isLoose == false || scanned.bagData?.isLoose == null }.size,
                totalLoosePerLocation = it.value.filter { scanned -> scanned.bagData?.isLoose == true }.size,
                isActive = if (scannedOrderNumber.value == it.value.first().bagData?.customerOrderNumber) {
                    it.key.first == current?.zoneId && it.key.second == current?.zoneType
                } else {
                    false
                },
                bagsForcedScanned = it.value.first().bagsForcedScanned,
                looseItemCount = it.value.filter { looseItem -> looseItem.bagData?.isLoose.orFalse() }.size
            )
        }
    }.asLiveData()

    // Events
    // TRUE if MFC
    val orderIssuesButtonEvent = LiveEvent<Boolean>()
    val bagBypassClickEvent = LiveEvent<StorageType>()
    val amStorageType = StorageType.AM
    val fzStorageType = StorageType.FZ
    val chStorageType = StorageType.CH
    val htStorageType = StorageType.HT
    val amZone = ordersByZoneAndId.map { zones -> zones.filter { it.bagData?.zoneType == StorageType.AM } }
    val chZone = ordersByZoneAndId.map { zones -> zones.filter { it.bagData?.zoneType == StorageType.CH } }
    val fzZone = ordersByZoneAndId.map { zones -> zones.filter { it.bagData?.zoneType == StorageType.FZ } }
    val htZone = ordersByZoneAndId.map { zones -> zones.filter { it.bagData?.zoneType == StorageType.HT } }

    val amZoneCounts = amZone.map { it.count() }
    val chZoneCounts = chZone.map { it.count() }
    val fzZoneCounts = fzZone.map { it.count() }
    val htZoneCounts = htZone.map { it.count() }

    val amZoneRejectedItemCount = ObservableInt()
    val chZoneRejectedItemCount = ObservableInt()
    val fzZoneRejectedItemCount = ObservableInt()
    val htZoneRejectedItemCount = ObservableInt()

    val amZoneBagCounts = ordersByZoneAndId.map { orders -> orders.filter { it.bagData?.zoneType == StorageType.AM } }
    val chZoneBagCounts = ordersByZoneAndId.map { orders -> orders.filter { it.bagData?.zoneType == StorageType.CH } }
    val fzZoneBagCounts = ordersByZoneAndId.map { orders -> orders.filter { it.bagData?.zoneType == StorageType.FZ } }
    val htZoneBagCounts = ordersByZoneAndId.map { orders -> orders.filter { it.bagData?.zoneType == StorageType.HT } }

    val amBagOrToteScannedCount = amZoneBagCounts.map { orders -> orders.sumOf { it.bagsScanned } }
    val chBagOrToteScannedCount = chZoneBagCounts.map { orders -> orders.sumOf { it.bagsScanned } }
    val fzBagOrToteScannedCount = fzZoneBagCounts.map { orders -> orders.sumOf { it.bagsScanned } }
    val htBagOrToteScannedCount = htZoneBagCounts.map { orders -> orders.sumOf { it.bagsScanned } }

    val amBagOrToteTotalCount = amZoneBagCounts.map { orders -> orders.sumOf { it.totalBags } }
    val chBagOrToteTotalCount = chZoneBagCounts.map { orders -> orders.sumOf { it.totalBags } }
    val fzBagOrToteTotalCount = fzZoneBagCounts.map { orders -> orders.sumOf { it.totalBags } }
    val htBagOrToteTotalCount = htZoneBagCounts.map { orders -> orders.sumOf { it.totalBags } }

    val amLooseScannedCount = amZoneBagCounts.map { orders -> orders.sumOf { it.looseScanned } }
    val chLooseScannedCount = chZoneBagCounts.map { orders -> orders.sumOf { it.looseScanned } }
    val fzLooseScannedCount = fzZoneBagCounts.map { orders -> orders.sumOf { it.looseScanned } }
    val htLooseScannedCount = htZoneBagCounts.map { orders -> orders.sumOf { it.looseScanned } }

    val amLooseTotalCount = amZoneBagCounts.map { orders -> orders.sumOf { it.totalLoose } }
    val chLooseTotalCount = chZoneBagCounts.map { orders -> orders.sumOf { it.totalLoose } }
    val fzLooseTotalCount = fzZoneBagCounts.map { orders -> orders.sumOf { it.totalLoose } }
    val htLooseTotalCount = htZoneBagCounts.map { orders -> orders.sumOf { it.totalLoose } }

    val scannedCount = ordersByZoneAndId.map { it.sumOf { bagData -> bagData.currentBagsScanned }.toString() }
    val rxScannedCount = MutableLiveData(0.toString())
    val showGroceriesScannedCount = MutableLiveData(true)
    val showRxScannedCount = MutableLiveData(false)
    val totalCount = ordersByZoneAndId.map { it.sumOf { bagData -> bagData.totalBagsForZone } }
    val forceScannedCount = MutableLiveData(0)
    val isComplete = ordersByZoneAndId.map { bagsInThisOrder -> bagsInThisOrder.isNotEmpty() && bagsInThisOrder.all { it.isComplete() } }
    val isRxComplete = MutableLiveData<Boolean>()
    val showBagBypassEllipsis = AcuPickConfig.bagBypassEnabled.asLiveData()
    val showOrderIssueCta = showBagBypassEllipsis.combineWith(isComplete) { showBagBypassEllipsis, isComplete ->
        !isComplete.orFalse() && !showBagBypassEllipsis.orFalse()
    }

    // This is coming back from RemoveRejectedItemFragment to hide rejectedItemCount for zones.
    fun updateRejectedItemsVisibility(completedRejectedItems: MutableList<RejectedItemsByStorageType>) {
        val rejectedItemsForOrder = completedRejectedItems.filter { item ->
            item.customerOrderNumber == detailsHeaderUi.value?.customerOrderNumber
        }.toMutableList()

        val currentAMCount = activity.value?.rejectedItemCount?.find { it.storageType == StorageType.AM }?.rejectedItems?.size ?: 0
        val currentCHCount = activity.value?.rejectedItemCount?.find { it.storageType == StorageType.CH }?.rejectedItems?.size ?: 0
        val currentFZCount = activity.value?.rejectedItemCount?.find { it.storageType == StorageType.FZ }?.rejectedItems?.size ?: 0
        val currentHTCount = activity.value?.rejectedItemCount?.find { it.storageType == StorageType.HT }?.rejectedItems?.size ?: 0

        rejectedItemsForOrder.forEach { rejectedItem ->
            when (rejectedItem.storageType) {
                StorageType.AM -> {
                    amZoneRejectedItemCount.set(currentAMCount.minus(rejectedItem.rejectedItems?.size ?: 0))
                }

                StorageType.CH -> {
                    chZoneRejectedItemCount.set(currentCHCount.minus(rejectedItem.rejectedItems?.size ?: 0))
                }

                StorageType.FZ -> {
                    fzZoneRejectedItemCount.set(currentFZCount.minus(rejectedItem.rejectedItems?.size ?: 0))
                }

                StorageType.HT -> {
                    htZoneRejectedItemCount.set(currentHTCount.minus(rejectedItem.rejectedItems?.size ?: 0))
                }

                else -> {
                    // No-Op
                }
            }
        }
    }

    fun updateBagBypass(bagBypass: MutableList<ZonedBagsScannedData>) {
        val filteredbags = bagBypass.filter {
            it.bagData?.customerOrderNumber == detailsHeaderUi.value?.customerOrderNumber
        }
        forceScannedCount.postValue(filteredbags.size)
    }

    fun updateRxCount(rxBagUIs: List<RxBagUI>) {
        scannedRxBags.value = rxBagUIs.toList()
        totalScannedItems.value = rxBagUIs.count()
        isRxComplete.postValue(rxBagUIs.count() == totalRxBagCount.value)
        rxScannedCount.postValue(rxBagUIs.count().toString())
        rxOrdersPrescriptions.postValue(rxOrdersPrescriptions.value)
    }

    fun showRxScannedCount() {
        showGroceriesScannedCount.postValue(value = false)
        showRxScannedCount.postValue(value = true)
    }

    // Events
    fun onOrderIssuesButtonPressed() = orderIssuesButtonEvent.postValue(isMfcOrder.value)
    fun bagBypassClicked(storageType: StorageType) = bagBypassClickEvent.postValue(storageType)
    fun rePrintGiftButtonPressed() = reprintGiftNote.postValue(true)
    fun updateZonedBagUiData(data: List<ZonedBagsScannedData>) {
        zonedBagUiData.value = data.filter { it.bagData?.customerOrderNumber == detailsHeaderUi.value?.customerOrderNumber }
    }

    fun updateRejectedCount() {
        amZoneRejectedItemCount.set(activity.value?.rejectedItemCount?.find { it.storageType == StorageType.AM }?.rejectedItems?.sumOf { it.qty ?: 0 } ?: 0)
        chZoneRejectedItemCount.set(activity.value?.rejectedItemCount?.find { it.storageType == StorageType.CH }?.rejectedItems?.sumOf { it.qty ?: 0 } ?: 0)
        fzZoneRejectedItemCount.set(activity.value?.rejectedItemCount?.find { it.storageType == StorageType.FZ }?.rejectedItems?.sumOf { it.qty ?: 0 } ?: 0)
        htZoneRejectedItemCount.set(activity.value?.rejectedItemCount?.find { it.storageType == StorageType.HT }?.rejectedItems?.sumOf { it.qty ?: 0 } ?: 0)
    }

    fun populateRxOrderIds() {
        rxOrderNumberOne.postValue(rxBagList.value?.getOrNull(FIRST_RX_ORDER_ID))
    }

    fun onPharmacyEllipsisClicked() {
        pharmacySheetEvent.postValue(scannedRxBags.value.isNotEmpty() && scannedRxBags.value.size.getOrZero() != totalRxBagCount.value)
    }

    fun showPharmacyIssueModal() {
        viewModelScope.launch {
            registerCloseAction(RX_REPORT_ISSUE_DIALOG_TAG) {
                closeActionFactory(positive = {
                    val selections = listOf(
                        app.getString(R.string.rx_dug_pharmacy_issue_dialog_choice_one_value),
                        app.getString(R.string.rx_dug_pharmacy_issue_dialog_choice_two_value)
                    )
                    rxDeliveryFailureReason.postValue(selections[it ?: 0])
                    rxDeliveryFailureReasonLiveEvent.postValue(true)
                })
            }
            inlineDialogEvent.postValue(
                CustomDialogArgDataAndTag(
                    data = CustomDialogArgData(
                        dialogType = DialogType.CustomRadioButtons,
                        title = StringIdHelper.Id(R.string.rx_dug_pharmacy_issue_dialog_title),
                        body = StringIdHelper.Id(R.string.rx_dug_pharmacy_issue_dialog_body),
                        customData = listOf(
                            StringIdHelper.Id(R.string.rx_dug_pharmacy_issue_dialog_choice_one),
                            StringIdHelper.Id(R.string.rx_dug_pharmacy_issue_dialog_choice_two),
                        ) as Serializable,
                        positiveButtonText = StringIdHelper.Id(R.string.rx_dug_pharmacy_issue_dialog_choice_positive_action),
                        negativeButtonText = StringIdHelper.Id(R.string.cancel),
                        cancelOnTouchOutside = false
                    ),
                    tag = RX_REPORT_ISSUE_DIALOG_TAG
                )
            )
        }
    }

    private fun setUpHeader(uiData: DestageOrderUiData) {
        if (isShowTimer(uiData)) {
            uiData.customerArrivlaTime?.let {
                timerJob = viewModelScope.launch {
                    flow {
                        do {
                            emit(ChronoUnit.SECONDS.between(it, ZonedDateTime.now()))
                            delay(1000)
                        } while (coroutineContext.isActive)
                    }.collect {
                        // reset if there's any existing header text
                        changeToolbarTitleEvent.postValue("")
                        changeToolbarSmallTitleEvent.postValue(app.getString(R.string.wait_time_countdown, it.div(60), it.rem(60)))
                        val titleBackground = ContextCompat.getDrawable(app.applicationContext, R.drawable.rounded_corner_picklist_status_button) as GradientDrawable
                        titleBackground.setColor(ContextCompat.getColor(app.applicationContext, R.color.picklist_stageByTime_pastDue))
                        titleBackground.alpha = 170
                        changeToolbarTitleBackgroundImageEvent.postValue(titleBackground)
                    }
                }
                return
            }
        }
        // reset any existing timer text
        changeToolbarSmallTitleEvent.postValue("")
        changeToolbarTitleBackgroundImageEvent.postValue(null)

        if (isRxDug.value == true) changeToolbarTitleEvent.postValue(app.getString(R.string.toolbar_title_rx_dug))
        else changeToolbarTitleEvent.postValue(app.getString(R.string.toolbar_title_destage_order))
    }

    private fun isShowTimer(uiData: DestageOrderUiData) = uiData.run {
        if (fulfillmentTypeUI == FulfillmentTypeUI.DUG)
            customerArrivalStatusUI == null || (
                (customerArrivalStatusUI == CustomerArrivalStatusUI.ARRIVED || customerArrivalStatusUI == CustomerArrivalStatusUI.ARRIVED_NOT_STARTED) &&
                    (feScreenStatus == FE_SCREEN_STATUS_STORE_NOTIFIED || feScreenStatus == null)
                )
        else customerArrivalStatusUI != CustomerArrivalStatusUI.ARRIVING && customerArrivalStatusUI != CustomerArrivalStatusUI.EN_ROUTE
    }

    fun onChangeActiveOrder(activeOrderNumber: String?, uiData: DestageOrderUiData?) {
        timerJob?.cancel()
        val isCurrent = activeOrderNumber == uiData?.customerOrderNumber
        if (uiData != null && isCurrent) {
            setUpHeader(uiData)
        }
    }

    fun getStorageType(input: CustomDialogArgDataAndTag) {
        registerCloseAction(MANUAL_ENTRY_STORAGE_TYPE) {
            closeActionFactory(positive = {
                viewModelScope.launch {
                    selectedStorageType.emit(it)
                }
            })
        }
        // Implement your logic to determine the storage type
        inlineDialogEvent.postValue(input)
    }

    companion object {
        const val DESTAGE_BOTTOM_SHEET_DIALOG_TAG = "DestagebottomSheetDialogTag"
        const val FIRST_RX_ORDER_ID = 0
        private val RX_REPORT_ISSUE_DIALOG_TAG = "RxReportIssueDialogTag${this.hashCode()}"
    }
}
