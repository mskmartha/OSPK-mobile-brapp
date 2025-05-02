package com.albertsons.acupick.ui.arrivals.pharmacy

import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.os.Parcelable
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.RxBag
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.barcode.StagingContainer
import com.albertsons.acupick.data.model.request.RxDeliveryFailedReason
import com.albertsons.acupick.data.model.request.RxOrder
import com.albertsons.acupick.data.model.request.RxOrderStatus
import com.albertsons.acupick.data.model.request.RxRemoveRequestDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.FE_SCREEN_STATUS_STORE_NOTIFIED
import com.albertsons.acupick.data.model.response.fullContactName
import com.albertsons.acupick.data.model.response.getFulfillmentTypeDescriptions
import com.albertsons.acupick.data.model.response.getRxBag
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.arrivals.complete.COMPLETE_HANDOFF_MESSAGE_DURATION_MS
import com.albertsons.acupick.ui.arrivals.complete.HandOffArgData
import com.albertsons.acupick.ui.arrivals.complete.HandOffUI
import com.albertsons.acupick.ui.arrivals.destage.DestageOrderPagerViewModel.Companion.MANUAL_ENTRY_PHARMACY_BOTTOMSHEET_TAG
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetArgDataAndTag
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetType
import com.albertsons.acupick.ui.bottomsheetdialog.CustomBottomSheetArgData
import com.albertsons.acupick.ui.manualentry.ManualEntryPharmacyParams
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.models.CustomerArrivalStatusUI
import com.albertsons.acupick.ui.models.FulfillmentTypeUI
import com.albertsons.acupick.ui.models.RxBagUI
import com.albertsons.acupick.ui.models.SnackBarEvent
import com.albertsons.acupick.ui.picklistitems.ScanTarget
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.UserFeedback
import com.albertsons.acupick.ui.util.combineWith
import com.albertsons.acupick.ui.util.getOrZero
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.toFormatHelper
import com.albertsons.acupick.ui.util.toFulfillmentTypeUI
import com.albertsons.acupick.ui.util.transform
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.koin.core.component.inject
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class PrescriptionReturnViewModel(val app: Application) : BaseViewModel(app) {

    // DI
    private val userFeedback: UserFeedback by inject()
    private val dispatcherProvider: DispatcherProvider by inject()
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    private val apsRepo: ApsRepository by inject()
    private val siteRepository: SiteRepository by inject()

    private val bagList: LiveData<MutableList<RxBagUI>> = MutableLiveData(mutableListOf())

    private val scannedRxBags = MutableStateFlow<List<RxBagUI>>(emptyList())
    val totalScannedItems = scannedRxBags.asLiveData().transform { it?.size ?: 0 }
    val showPrescritonReturned = MutableLiveData(false)
    val isBackToHomeButtonEnable = MutableStateFlow(false)
    private val pickedBagNumbers = MutableLiveData<List<String>>()
    private val isPickedBagsAvailable get() = pickedBagNumbers.value.isNotNullOrEmpty()

    val totalBagCount = bagList.combineWith(pickedBagNumbers) { bags, pickedBags ->
        if (pickedBags.isNotNullOrEmpty()) pickedBags?.size.getOrZero() else bags?.size.getOrZero()
    }

    val isCompleteButtonEnabled = combine(scannedRxBags, totalBagCount.asFlow(), pickedBagNumbers.asFlow()) { totalScannedItems, totalBoxCount, pickedBags ->
        if (isPickedBagsAvailable) {
            totalScannedItems.size == pickedBags.size
        } else {
            totalScannedItems.size == totalBoxCount
        }
    }.asLiveData()

    val rxOrdersLabelText = bagList.map { if (it.isNotNullOrEmpty()) R.string.rx_order_numbers.toFormatHelper(it.size.toString()).getString(app) else "" }

    val showCompleteCta = combine(isCompleteButtonEnabled.asFlow(), isDisplayingSnackbar.asFlow(), showPrescritonReturned.asFlow()) { isCompleteEnabled, isDisplayingSnackbar, prescritonReturned ->
        isCompleteEnabled.orFalse() && isDisplayingSnackbar.orFalse().not() && prescritonReturned.not()
    }.asLiveData()

    val hideStaticPrompt = combine(isDisplayingSnackbar.asFlow(), isCompleteButtonEnabled.asFlow()) { isDisplayingSnackbar, isComplete ->
        // The static prompt needs to hide when a snack bar is showing or when isComplete
        isDisplayingSnackbar || isComplete
    }.asLiveData()

    private var rxBagReturnScanTimestamp: ZonedDateTime = ZonedDateTime.now()
    private var rxLocationReturnScanTimestamp: ZonedDateTime = ZonedDateTime.now()

    private val allOrderIds = MutableLiveData<List<String>>()

    val prescriptionListUi = combine(allOrderIds.asFlow(), pickedBagNumbers.asFlow(), scannedRxBags) { allIds, pickedIds, scannedBags ->
        allIds?.mapIndexed { index, bagNumber ->
            PrescriptionReturnListUi(
                bagNumber = R.string.rx_order_prescription.toFormatHelper((index + 1).toString(), bagNumber).getString(app),
                picked = if (isPickedBagsAvailable) pickedIds.contains(bagNumber) else true,
                scanned = scannedBags.any { it.bagNumber == bagNumber }
            )
        }
    }.asLiveData()

    private var handoffUI: List<HandOffUI> = emptyList()
    private var isFromNotification = false
    private var fromPartialPrescriptionPickup = false
    private var timerJob: Job? = null

    init {
        changeToolbarTitleEvent.postValue(app.getString(R.string.pharmacy_return_prescription_title))
    }

    // header
    val shortOrderNumber = MutableLiveData("")
    val contactName = MutableLiveData("")
    val customerOrderNumber = MutableLiveData("")
    val rxOrderNumberOne = MutableLiveData<String>()

    private val activityDto = MutableStateFlow<ActivityDto?>(null)

    private val arrivalLabel = MutableStateFlow<BarcodeType.PharmacyArrivalLabel?>(null)
    private val returnLabel = MutableStateFlow<BarcodeType.PharmacyReturnLabel?>(null)

    val hasArrivalLabel = arrivalLabel.map { it != null }.asLiveData()
    val hasReturnLabel = returnLabel.map { it != null }.asLiveData()

    val arrivalLabelText = arrivalLabel.map { it?.rawBarcode }.asLiveData()
    val returnLabelText = returnLabel.map { app.getString(R.string.pharmacy_return) }.asLiveData()

    val showPharmacyStaffRequired = combine(arrivalLabelText.asFlow(), showPrescritonReturned.asFlow()) { arrivalLabel, prescriptionReturned ->
        arrivalLabel.isNotNullOrEmpty() && prescriptionReturned.not()
    }.asLiveData()
    val showNoLocationScanned = arrivalLabelText.map { it.isNullOrBlank() }
    val showNoReturnBarcodeScanned = hasArrivalLabel.combineWith(hasReturnLabel) { hasArrival, returnLabel ->
        hasArrival.orFalse() && returnLabel.orFalse().not()
    }
    private val isPartialPrescriptionReturn get() = pickedBagNumbers.value != null && allOrderIds.value?.size.getOrZero() != pickedBagNumbers.value?.size.getOrZero()

    /** Tracks whether an item or tote is expected to be scanned next */
    private val activeScanTarget: LiveData<ScanTarget> = MutableLiveData(ScanTarget.PharmacyArrivalLabel)
    val snackbarEvent = activeScanTarget.transform {
        when (it) {
            ScanTarget.PharmacyArrivalLabel -> SnackBarEvent<String>(prompt = StringIdHelper.Id(R.string.pharmacy_arrival_label_prompt), action = { onManualEntryClicked() })
            ScanTarget.PharmacyReturnLabel -> SnackBarEvent(prompt = StringIdHelper.Id(R.string.pharmacy_return_label_prompt), action = { onManualEntryClicked() })
            ScanTarget.Bag -> SnackBarEvent(prompt = StringIdHelper.Id(R.string.pharmacy_scan_bag_prompt), action = { onManualEntryClicked() })
            else -> null
        }
    }

    fun updateData(data: PrescriptionReturnData?) {
        data?.let {
            pickedBagNumbers.postValue(it.scannedData)
            handoffUI = it.handOffUI
            isFromNotification = it.isFromNotification
            fromPartialPrescriptionPickup = it.fromPartialPrescriptionPickup
            startTimer()
        }
    }

    private fun startTimer() {
        if (fromPartialPrescriptionPickup) {
            setUpHeader()
        }
    }

    // We will be having only one order for RX so we can rely on handoffUI.firstOrNull()
    private fun setUpHeader() {
        if (isShowTimer()) {
            handoffUI.firstOrNull()?.startTime?.let {
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
        timerJob?.cancel()
        changeToolbarSmallTitleEvent.postValue("")
        changeToolbarTitleBackgroundImageEvent.postValue(null)

        changeToolbarTitleEvent.postValue(app.getString(R.string.pharmacy_return_prescription_title))
    }

    private fun isShowTimer() = handoffUI.firstOrNull()?.run {
        if (fulfillmentType?.toFulfillmentTypeUI() == FulfillmentTypeUI.DUG)
            (customerArrivalStatusUI == CustomerArrivalStatusUI.ARRIVED || customerArrivalStatusUI == CustomerArrivalStatusUI.ARRIVED_NOT_STARTED) &&
                ((feScreenStatus == FE_SCREEN_STATUS_STORE_NOTIFIED || feScreenStatus == null))
        else customerArrivalStatusUI != CustomerArrivalStatusUI.ARRIVING && customerArrivalStatusUI != CustomerArrivalStatusUI.EN_ROUTE
    }.orFalse()

    fun loadData(erId: Long) {
        viewModelScope.launch(dispatcherProvider.Main) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> {
                    when (val result = isBlockingUi.wrap { apsRepo.pickUpActivityDetails(id = erId, loadCI = true) }) {
                        is ApiResult.Success<ActivityDto> -> {
                            // convert
                            shortOrderNumber.set(result.data.getFulfillmentTypeDescriptions())
                            contactName.set(result.data.fullContactName())
                            customerOrderNumber.set(result.data.customerOrderNumber)
                            rxOrderNumberOne.set(result.data.rxDetails?.rxOrderId?.firstOrNull())
                            activityDto.value = result.data
                            bagList.postValue(result.data.getRxBag().map { it.toUI() }.toMutableList())
                            allOrderIds.postValue(result.data.rxDetails?.rxOrderId)
                        }

                        is ApiResult.Failure -> {
                            handleApiError(result, retryAction = { loadData(erId) })
                        }
                    }
                }

                false -> {
                    networkAvailabilityManager.triggerOfflineError {
                        loadData(erId)
                    }
                }
            }
        }
    }

    fun onCompleteClicked() {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { onCompleteClicked() }
            } else {
                fun RxBagUI.toRxdItem(): RxOrder {
                    return RxOrder(
                        rxOrderId = bagNumber,
                        rxOrderStatus = RxOrderStatus.DELIVERY_FAILED,
                        deliveryFailReason = getDeliveryFailedReasonCode(bagNumber),
                        rxReturnBagScanTimestamp = rxReturnBagScanTimestamp
                    )
                }

                val removeItem = RxRemoveRequestDto(
                    orderId = customerOrderNumber.value,
                    storeNumber = siteRepository.siteDetails.value?.siteId,
                    orderStatus = activityDto.value?.status?.name,
                    cartType = activityDto.value?.cartType,
                    rxBagReturnScanTimestamp = rxBagReturnScanTimestamp, // return label
                    rxLocationReturnScanTimestamp = rxLocationReturnScanTimestamp, // arrival location
                    rxReturnCompleteTimestamp = ZonedDateTime.now(),
                    rxOrders = bagList.value?.map { it.toRxdItem() }.orEmpty()
                )

                when (val result = apsRepo.recordRxRemoveItems(removeItem)) {
                    is ApiResult.Success -> {
                        if (fromPartialPrescriptionPickup) {
                            navigateToHandOff()
                        } else {
                            showPrescritonReturned.postValue(true)
                            delay(COMPLETE_HANDOFF_MESSAGE_DURATION_MS)
                            isBackToHomeButtonEnable.value = true
                            // _navigationEvent.postValue(NavigationEvent.Back(destinationId = R.id.destageOrderFragment, inclusive = true))
                        }
                    }

                    is ApiResult.Failure -> handleApiError(result, retryAction = { onCompleteClicked() })
                }
            }
        }
    }

    private fun navigateToHandOff() {
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                NavGraphDirections.actionToHandOffFragment(
                    handOffArgData = HandOffArgData(handoffUI),
                    isFromNotification = isFromNotification,
                    isFromPartialPrescriptionReturn = true,
                    pickedBagNumbers = PrescriptionReturnData(pickedBagNumbers.value.orEmpty())
                )
            )
        )
    }

    fun backToArrivalOrHomeScreen() {
        _navigationEvent.postValue(NavigationEvent.Back(destinationId = R.id.destageOrderFragment, inclusive = true))
    }

    private fun getDeliveryFailedReasonCode(bagNumber: String?): String? {
        pickedBagNumbers.value?.contains(bagNumber)?.let {
            return if (it) RxDeliveryFailedReason.BAG_PROCESSED_SUCCESSFULLY.value else RxDeliveryFailedReason.BAG_FAILED_TO_PROCESS.value
        }
        return null
    }

    private fun handleScanFailure(errorMessage: String) {
        userFeedback.setFailureScannedSoundAndHaptic()
        showSnackBar(AcupickSnackEvent(message = StringIdHelper.Raw(errorMessage), type = SnackType.ERROR))
    }

    private fun handleScanSuccess(message: String) {
        userFeedback.setSuccessScannedSoundAndHaptic()
        showSnackBar(AcupickSnackEvent(message = StringIdHelper.Raw(message), type = SnackType.SUCCESS))
    }

    fun onManualEntryClicked() {
        inlineBottomSheetEvent.postValue(
            BottomSheetArgDataAndTag(
                data = CustomBottomSheetArgData(
                    dialogType = BottomSheetType.ManualEntryPharmacy,
                    title = StringIdHelper.Raw(""),
                    isFullScreen = true,
                    customDataParcel = ManualEntryPharmacyParams(
                        orderNumber = bagList.value?.getOrNull(0)?.orderNumber ?: "", // TODO : change for multiple bags
                        scanTarget = activeScanTarget.value ?: ScanTarget.PharmacyArrivalLabel,
                        shortOrderId = shortOrderNumber.value,
                        customerName = contactName.value,
                        customerOrderNumber = customerOrderNumber.value
                    )
                ),
                tag = MANUAL_ENTRY_PHARMACY_BOTTOMSHEET_TAG
            )
        )
    }

    /** Entry point for a barcode received from the scanner/DataWedge */
    fun onScannerBarcodeReceived(barcodeType: BarcodeType) {
        when (barcodeType) {
            is BarcodeType.PharmacyArrivalLabel -> handleScannedArrivalLabel(barcodeType)
            is BarcodeType.PharmacyReturnLabel -> handleScannedReturnLabel(barcodeType)
            is BarcodeType.PharmacyBag -> handleScannedBag(barcodeType, getValidBagFromScannedBag(barcodeType))
            else -> handleScanFailure(generateBarcodeScanErrorMessage())
        }
    }

    private fun handleScannedArrivalLabel(barcodeType: BarcodeType.PharmacyArrivalLabel?) {
        rxLocationReturnScanTimestamp = ZonedDateTime.now()
        activeScanTarget.set(ScanTarget.PharmacyReturnLabel)
        arrivalLabel.value = barcodeType
        handleScanSuccess(app.applicationContext?.getString(R.string.prompt_scan_a_box, barcodeType?.rawBarcode).orEmpty())
    }

    private fun handleScannedReturnLabel(barcodeType: BarcodeType.PharmacyReturnLabel?) {
        if (arrivalLabel.value == null) {
            handleScanFailure(app.applicationContext?.getString(R.string.pharmacy_arrival_label_prompt).orEmpty())
        } else {
            rxBagReturnScanTimestamp = ZonedDateTime.now()
            activeScanTarget.set(ScanTarget.Bag)
            returnLabel.value = barcodeType
            handleScanSuccess(app.applicationContext?.getString(R.string.prompt_return_label).orEmpty())
        }
    }

    private fun handleScannedBag(barcodeType: StagingContainer, rxBagUI: RxBagUI?) {

        when {
            // no arrival label scanned
            arrivalLabel.value == null -> handleScanFailure(app.applicationContext?.getString(R.string.pharmacy_arrival_label_prompt).orEmpty())

            // no return label scanned
            returnLabel.value == null && arrivalLabel.value != null -> handleScanFailure(app.applicationContext?.getString(R.string.pharmacy_return_label_prompt).orEmpty())

            itemIsNotInOrder(rxBagUI) -> handleScanFailure(app.applicationContext?.getString(R.string.error_item_not_in_order).orEmpty())

            // box was already scanned before
            scannedRxBags.value.any { it.bagNumber == rxBagUI?.bagNumber } -> rxBagUI?.let { handleBagAlreadyScanned(it) }

            else -> {
                activeScanTarget.set(ScanTarget.Bag)
                findRxBags(barcodeType.bagOrToteId) { bag ->
                    handleScanSuccess(StringIdHelper.Format(R.string.rx_bag_scanned_successfully_format, bag.toString()).getString(app))
                }
                returnLabel.value?.let { _ ->
                    rxBagUI?.let { ui -> addScannedRXBagToList(ui.copy(rxReturnBagScanTimestamp = ZonedDateTime.now())) }
                }
            }
        }
    }

    private fun findRxBags(scannedBag: String, onSuccess: (bagNumber: Int) -> Unit) {
        allOrderIds.value?.forEachIndexed { index, bagNumber ->
            if (bagNumber == scannedBag) {
                onSuccess(index + 1)
                return
            }
        }
    }

    private fun handleBagAlreadyScanned(rxBagUI: RxBagUI) {
        findRxBags(rxBagUI.bagNumber.orEmpty()) { bag ->
            handleScanFailure(
                StringIdHelper.Format(R.string.rx_bag_error_already_scanned, bag.toString()).getString(app)
            )
        }
    }

    private fun addScannedRXBagToList(boxInOrder: RxBagUI) {
        scannedRxBags.value = scannedRxBags.value + boxInOrder
    }

    private fun itemIsNotInOrder(rxBagUI: RxBagUI?): Boolean {
        return if (isPickedBagsAvailable) {
            !pickedBagNumbers.value?.filter { it == rxBagUI?.bagNumber }.isNotNullOrEmpty()
        } else {
            !bagList.value?.filter { it.orderNumber == rxBagUI?.orderNumber }.isNotNullOrEmpty()
        } || rxBagUI == null
    }

    private fun generateBarcodeScanErrorMessage(): String {
        return when (activeScanTarget.value) {
            ScanTarget.PharmacyArrivalLabel -> app.applicationContext?.getString(R.string.pharmacy_error_location_not_scanned).orEmpty()
            ScanTarget.PharmacyReturnLabel -> app.applicationContext?.getString(R.string.pharmacy_error_return_label_not_scanned).orEmpty()
            ScanTarget.Bag -> app.applicationContext?.getString(R.string.pharmacy_bag_not_scanned).orEmpty()
            else -> "" // nothing
        }
    }

    private fun getValidBagFromScannedBag(stagingContainerBarcode: BarcodeType.PharmacyBag) =
        getValidBag(bagOrToteId = stagingContainerBarcode.rawBarcode, customerOrderNumber = stagingContainerBarcode.customerOrderNumber)

    private fun getValidBag(bagOrToteId: String, customerOrderNumber: String) =
        bagList.value?.find { it.bagNumber == bagOrToteId }

    fun onManualEntryBarcodeReceived(stagingData: ManualEntryPharmacyData) {
        stagingData?.stagingContainer?.let {
            onScannerBarcodeReceived(it)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

private fun RxBag.toUI(): RxBagUI {
    return RxBagUI(
        orderNumber = this.orderNumber,
        bagNumber = this.bagNumber,
        rxReturnBagScanTimestamp = null,
        deliveryFailReason = ""
    )
}

@Parcelize
data class ManualEntryPharmacyData(
    val scanTarget: ScanTarget,
    val stagingContainer: BarcodeType? = null, //
) : Parcelable
