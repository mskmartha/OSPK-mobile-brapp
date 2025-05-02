package com.albertsons.acupick.ui.staging.winestaging.weight

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.barcode.StagingContainer
import com.albertsons.acupick.data.model.request.CompleteDropOffRequestDto
import com.albertsons.acupick.data.model.request.ScanContainerRequestDto
import com.albertsons.acupick.data.model.request.ScanContainerWrapperRequestDto
import com.albertsons.acupick.data.model.request.ValidatePalletRequestDto
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.WineStagingData
import com.albertsons.acupick.data.model.response.WineStagingType
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.StagingStateRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.data.repository.WineShippingRepository
import com.albertsons.acupick.data.repository.WineShippingStageStateRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.infrastructure.utils.logError
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogStyle
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.manualentry.ManualEntryStagingParams
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryStagingData
import com.albertsons.acupick.ui.models.BoxScanUI
import com.albertsons.acupick.ui.models.BoxUI
import com.albertsons.acupick.ui.models.SnackBarEvent
import com.albertsons.acupick.ui.models.ZoneBagCountUI
import com.albertsons.acupick.ui.models.toBoxScanUI
import com.albertsons.acupick.ui.picklistitems.ScanTarget
import com.albertsons.acupick.ui.staging.StagingPart2PagerViewModel
import com.albertsons.acupick.ui.staging.winestaging.BoxUiData
import com.albertsons.acupick.ui.staging.winestaging.WineStaging3FragmentDirections
import com.albertsons.acupick.ui.staging.winestaging.WineStagingParams
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.UserFeedback
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.transform
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.time.ZonedDateTime
import java.util.TimeZone

typealias IS_PALLET_OPEN = Boolean
typealias PALLET_MESSAGE = String

class WineStaging3ViewModel(val app: Application) : BaseViewModel(app) {

    // DI
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    private val apsRepo: ApsRepository by inject()
    private val wineRepo: WineShippingRepository by inject()
    private val userFeedback: UserFeedback by inject()
    private val dispatcherProvider: DispatcherProvider by inject()
    private val stagingStateRepo: StagingStateRepository by inject()
    private val sitesRepo: SiteRepository by inject()
    private val userRepo: UserRepository by inject()
    private val wineStagingStateRepo: WineShippingStageStateRepository by inject()

    // NAV ARGS
    private val boxList: LiveData<MutableList<BoxUI>> = MutableLiveData(mutableListOf())
    private val wineStagingParams = MutableLiveData<WineStagingParams?>()
    val totalBoxCount = boxList.transform { it?.size ?: 0 }

    val shortOrderNumber = wineStagingParams.transform { it?.shortOrderNumber.orEmpty() }
    val longOrderNumber = wineStagingParams.transform { it?.customerOrderNumber.orEmpty() }
    val customerName = wineStagingParams.transform { it?.contactName.orEmpty() }
    val boxQuantityHeader = MutableLiveData<Int>(0)

    // Complete UI animation
    val showAnimation: LiveData<Boolean> = MutableLiveData(false)
    val showAnimationBackground: LiveData<Boolean> = MutableLiveData(false)

    // STATE
    private val scannedBoxes = MutableStateFlow<List<BoxScanUI>>(emptyList())
    val totalScannedItems = scannedBoxes.asLiveData().transform { it?.size ?: 0 }
    val isScanFromManualEntry = MutableLiveData(false)
    var shouldShowReminder = false

    private val orderComplete = combine(scannedBoxes, boxList.asFlow(), isDisplayingSnackbar.asFlow()) { scannedBoxes, boxList, snackShown ->
        scannedBoxes.count() == boxList.size && !snackShown
    }
    val isCompleteButtonEnabled = combine(scannedBoxes, totalBoxCount.asFlow()) { totalScannedItems, totalBoxCount ->
        totalScannedItems.size == totalBoxCount
    }

    val showCompleteCta = combine(isCompleteButtonEnabled, isDisplayingSnackbar.asFlow()) { isCompleteEnabled, isDisplayingSnackbar ->
        isCompleteEnabled.orFalse() && isDisplayingSnackbar.orFalse().not()
    }.asLiveData()
    val hideStaticPrompt = combine(isDisplayingSnackbar.asFlow(), isCompleteButtonEnabled) { isDisplayingSnackbar, isComplete ->
        // The static prompt needs to hide when a snack bar is showing or when isComplete
        isDisplayingSnackbar || isComplete
    }.asLiveData()

    init {
        registerCloseAction(RETRIEVE_LABEL_DIALOG_TAG) {
            closeActionFactory(positive = { shouldShowReminder = false })
        }
        viewModelScope.launch(dispatcherProvider.Main) {
            orderComplete.distinctUntilChanged().collectLatest {
                if (it) showRetriveShippingLabelDialog()
            }
        }
    }

    private val currentZone = MutableStateFlow<ZoneBagCountUI?>(null)
    private var currentZoneBarcode = ""
    private val stagingActivityId = MutableStateFlow(0L)
    private var stagingCompleteTime: ZonedDateTime? = null
    val scannedZoneBoxCountList = combine(currentZone, scannedBoxes) { current, bags ->
        bags.groupBy { it.zone }.map {
            ZoneBagCountUI(
                zone = it.key,
                zoneType = StorageType.AM,
                scannedBagCount = bags.count { bag -> bag.zone == it.key },
                isCurrent = true,
                false
            )
        }
    }.asLiveData()

    /** Tracks whether an item or tote is expected to be scanned next */
    private val activeScanTarget: LiveData<ScanTarget> = MutableLiveData(ScanTarget.Zone)
    val staticPrompt = activeScanTarget.transform {
        when (it) {
            ScanTarget.Zone -> StringIdHelper.Id(R.string.prompt_scan_location)
            ScanTarget.Box -> StringIdHelper.Id(R.string.prompt_scan_a_box_label)
            else -> StringIdHelper.Id(R.string.empty)
        }
    }

    fun setupHeader(params: WineStagingParams?, totalBoxQuantityCopy: Int, shouldShowPrintReminder: Boolean) {
        wineStagingParams.value = params
        shouldShowReminder = shouldShowPrintReminder
        boxQuantityHeader.postValue(totalBoxQuantityCopy)
        changeToolbarTitleEvent.postValue(app.getString(R.string.toolbar_stage_by_format, params?.stageByTime))
    }

    fun fetchData(params: WineStagingParams?) {
        wineStagingParams.value = params

        viewModelScope.launch(dispatcherProvider.IO) {
            val result = isBlockingUi.wrap {
                wineRepo.getBoxDetails(params?.activityId.orEmpty())
            }

            when (result) {
                is ApiResult.Failure -> handleApiError(result)
                is ApiResult.Success -> loadData(result.data.toBoxUiData(), wineStagingParams.value?.activityId)
            }
        }
    }

    fun loadData(boxUiData: BoxUiData?, activityId: String?) {
        boxList.postValue(
            mutableListOf<BoxUI>().apply {
                addAll(
                    boxUiData?.boxDataList?.map {
                        BoxUI(
                            zoneType = StorageType.AM,
                            referenceEntityId = it.referenceEntityId,
                            type = it.type,
                            orderNumber = it.orderNumber,
                            boxNumber = it.boxNumber,
                            isLoose = false,
                            label = it.label,
                        )
                    } ?: listOf()
                )
            }
        )
        boxQuantityHeader.postValue(boxUiData?.boxDataList?.size)
        stagingActivityId.value = activityId?.toLong() ?: 0L

        val savedData = wineStagingStateRepo.loadStagingPartOne(wineStagingParams.value?.customerOrderNumber.orEmpty())
        scannedBoxes.value = savedData?.scannedBoxes?.map { it.toBoxScanUI() } ?: emptyList()

        // save box data for offline use
        wineStagingStateRepo.saveStagingPartOne(
            WineStagingData(
                activityId = wineStagingParams.value?.activityId?.toIntOrNull(),
                shorOrderId = wineStagingParams.value?.shortOrderNumber.orEmpty(),
                nextActivityId = WineStagingType.WineStaging3,
                contactName = wineStagingParams.value?.contactName.orEmpty(),
                customerOrderNumber = wineStagingParams.value?.customerOrderNumber.orEmpty(),
                entityId = wineStagingParams.value?.entityId,
                stageByTime = wineStagingParams.value?.stageByTime,
                boxInfo = boxUiData?.boxDataList,
                scannedBoxes = scannedBoxes.value.map { it.asScannedBoxData }
            ),
            wineStagingParams.value?.customerOrderNumber.orEmpty()
        )
    }

    private fun showRetriveShippingLabelDialog() {
        if (!shouldShowReminder) return

        shouldShowReminder = false
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    title = StringIdHelper.Id(R.string.retrieve_shipping_labels),
                    titleIcon = R.drawable.ic_alert,
                    dialogStyle = DialogStyle.PrintShippingLabel,
                    body = StringIdHelper.Plural(R.plurals.retrieve_shipping_labels_body, boxList.value?.count() ?: 0),
                    positiveButtonText = StringIdHelper.Id(R.string.ok),
                    cancelOnTouchOutside = false
                ),
                tag = RETRIEVE_LABEL_DIALOG_TAG
            )
        )
    }

    private fun handleScanFailure(errorMessage: String) {
        userFeedback.setFailureScannedSoundAndHaptic()
        showSnackBar(SnackBarEvent(prompt = StringIdHelper.Raw(errorMessage), isError = true))
    }

    private fun handleScanSuccess(message: String) {
        userFeedback.setSuccessScannedSoundAndHaptic()
        showSnackBar(
            SnackBarEvent(
                prompt = StringIdHelper.Raw(message),
                isSuccess = true
            )
        )
    }

    fun onManualEntryClicked() {
        val params = ManualEntryStagingParams(
            scannedBagUiList = emptyList(),
            scannedBoxUiList = boxList.value,
            isWineShipping = true,
            zone = currentZone.value?.zone,
            customerOrderNumber = longOrderNumber.value,
            activityId = boxList.value?.firstOrNull()?.boxNumber?.take(4),
            isMutliSource = false
        )
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                NavGraphDirections.actionToManualEntryStagingFragment(params)
            )
        )
    }

    private fun showAnimation() {
        viewModelScope.launch(dispatcherProvider.IO) {
            showAnimationBackground.postValue(true)
            delay(STAGING_ANIMATION_DELAY_MS)
            showAnimation.postValue(true)
            delay(STAGING_ANIMATION_VISIBLE_DURATION_MS)
            navigateHome()
        }
    }

    private fun navigateHome() {
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                WineStaging3FragmentDirections.actionWineStaging3FragmentToHomeFragment()
            )
        )
    }

    // /////////////////////////////////////////////////////////////////////////
    // Scanner logic
    // /////////////////////////////////////////////////////////////////////////

    /** Entry point for a barcode received from the scanner/DataWedge */
    fun onScannerBarcodeReceived(barcodeType: BarcodeType) {
        when (barcodeType) {
            is BarcodeType.Zone -> {
                viewModelScope.launch(dispatcherProvider.Main) {
                    if (networkAvailabilityManager.isConnected.first()) {
                        handleScannedZone(barcodeType, null)
                    } else {
                        networkAvailabilityManager.triggerOfflineError { onScannerBarcodeReceived(barcodeType) }
                    }
                }
            }
            is StagingContainer -> handleScannedBox(barcodeType, getValidBoxFromScannedBox(barcodeType))
            else -> handleScanFailure(generateBarcodeScanErrorMessage())
        }
    }

    private fun getValidBoxFromScannedBox(stagingContainerBarcode: StagingContainer) =
        if (stagingContainerBarcode is BarcodeType.Box) {
            getValidBag(bagOrToteId = stagingContainerBarcode.bagOrToteId, customerOrderNumber = stagingContainerBarcode.customerOrderNumber)
        } else {
            null
        }

    private fun generateBarcodeScanErrorMessage(): String {
        return when (activeScanTarget.value) {
            ScanTarget.Zone -> app.applicationContext?.getString(R.string.error_zone_not_scanned).orEmpty()
            ScanTarget.Box -> app.applicationContext?.getString(R.string.box_no_boxes_scanned).orEmpty()
            else -> "" // nothing
        }
    }

    private suspend fun isPalletOpen(pallet: String): Pair<IS_PALLET_OPEN, PALLET_MESSAGE> {
        val siteId = userRepo.user.value?.selectedStoreId.orEmpty()
        val timezoneID: String = TimeZone.getDefault().id

        val result = isBlockingUi.wrap {
            apsRepo.validatePallet(
                ValidatePalletRequestDto(
                    siteId = siteId,
                    timeZone = timezoneID,
                    pallet = pallet
                ),
                isWineOrder = sitesRepo.isWineFulfillment,
                isDarkStore = sitesRepo.isDarkStoreEnabled
            )
        }
        return when (result) {
            is ApiResult.Success -> Pair(true, "")

            is ApiResult.Failure -> {
                if (result is ApiResult.Failure.Server && result.error?.errorCode?.resolvedType == ServerErrorCode.STAGING_PALLET_CLOSED) {
                    Pair(false, palletFailureMessage(result.error?.message))
                } else {
                    Pair(false, "")
                }
            }
        }
    }

    private suspend fun handleScannedZone(barcodeType: BarcodeType.Zone?, stagingManualData: ManualEntryStagingData?) {
        val zoneBarcodeType = (stagingManualData?.zone as? BarcodeType.Zone ?: barcodeType) as BarcodeType.Zone
        val isPalletOpen = isPalletOpen(zoneBarcodeType.rawBarcode)
        if (isPalletOpen.first) {
            activeScanTarget.set(ScanTarget.Box)
            currentZoneBarcode = zoneBarcodeType.rawBarcode

            // Update current zone
            currentZone.value = ZoneBagCountUI(
                zone = zoneBarcodeType.rawBarcode,
                zoneType = zoneBarcodeType.zoneType,
                isMultiSource = false
            )

            handleScanSuccess(app.applicationContext?.getString(R.string.prompt_scan_a_box, zoneBarcodeType.rawBarcode).orEmpty())
        } else {
            handleScanFailure(isPalletOpen.second)
        }

        if (isScanFromManualEntry.value != false && isPalletOpen.first) {
            stagingManualData?.stagingContainer?.let {
                onScannerBarcodeReceived(it)
            }
        }
    }

    private fun handleScannedBox(barcodeType: StagingContainer, boxUi: BoxUI?) {

        when {
            // no zone scanned
            currentZone.value == null -> handleScanFailure(app.applicationContext?.getString(R.string.error_scan_zone_first).orEmpty())

            itemIsNotInOrder(boxUi) -> handleScanFailure(app.applicationContext?.getString(R.string.error_box_not_in_order).orEmpty())

            // box was already scanned before
            scannedBoxes.value.any { it.box.boxNumber == boxUi?.boxNumber } -> boxUi?.let { handleBoxAlreadyScanned(it) }

            else -> {
                activeScanTarget.set(ScanTarget.Zone)
                handleScanSuccess(
                    app.applicationContext?.getString(
                        R.string.success_box_scanned_out_format,
                        barcodeType.bagOrToteId.takeLast(BOX_ID_LENGTH)
                    ).orEmpty()
                )
                currentZone.value?.zone?.let { zone ->
                    boxUi?.let { ui -> addScannedBoxToList(ui, zone) }
                }
            }
        }
    }

    private fun itemIsNotInOrder(boxUi: BoxUI?) = !boxList.value?.filter {
        it.orderNumber == boxUi?.orderNumber
    }.isNotNullOrEmpty() || boxUi == null

    fun onCompleteClicked() = stageOrder()

    private fun stageOrder() {
        viewModelScope.launch(dispatcherProvider.IO) {
            setStagingCompleteTime()
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { stageOrder() }
            } else if (sendScannedBags() && completeStaging()) {
                showAnimation()
                clearStateData()
            }
        }
    }
    private fun palletFailureMessage(message: String?) = if (message.orEmpty().contains(app.getString(R.string.prompt_closed_pallet))) {
        app.getString(R.string.prompt_closed_pallet)
    } else {
        message.orEmpty()
    }
    private fun clearStateData() = stagingStateRepo.clear()
    private suspend fun completeStaging(): Boolean {
        val result = isBlockingUi.wrap {
            apsRepo.completeDropOffActivity(
                CompleteDropOffRequestDto(
                    actId = stagingActivityId.value,
                    containerIdList = emptyList(),
                    dropOffCompTime = stagingCompleteTime,
                )
            )
        }
        (result as? ApiResult.Failure)?.let { handleApiError(it) }
        return result is ApiResult.Success
    }

    private suspend fun sendScannedBags(): Boolean {

        var orderNumber: String? = null
        val result = isBlockingUi.wrap {
            apsRepo.scanContainers(
                ScanContainerWrapperRequestDto(
                    actId = stagingActivityId.first(),
                    containerReqs = scannedBoxes.first().map { scannedBag ->
                        orderNumber = scannedBag.orderNumber
                        ScanContainerRequestDto(
                            containerId = scannedBag.box.label,
                            stagingLocation = scannedBag.zone,
                            containerScanTime = scannedBag.containerScanTime,
                            isLoose = scannedBag.box.isLoose
                        )
                    },
                    lastScanTime = stagingCompleteTime,
                    multipleHandoff = false,
                    isDarkStore = sitesRepo.isDarkStoreEnabled,
                    isWineFulfillment = sitesRepo.isWineFulfillment
                )
            )
        }

        stagingActivityId.first().toString().logError(
            "Activity Id is null. WineStaging3ViewModel(sendScannedBags)," +
                " Order Id-$orderNumber, User Id-${userRepo.user.value?.userId}, storeId-${sitesRepo.siteDetails.value?.siteId}",
            acuPickLogger
        )

        (result as? ApiResult.Failure)?.let { handleApiError(it) }
        return result is ApiResult.Success
    }

    private fun setStagingCompleteTime() {
        // Record the timestamp of the first time the user is able to press Complete Stage.
        // This timestamp will be used on subsequent retries if network is unavailable.
        if (stagingCompleteTime == null) {
            stagingCompleteTime = scannedBoxes.value.map { bag -> bag.containerScanTime }.maxOfOrNull { it ?: ZonedDateTime.now() }
        }
    }

    private fun addScannedBoxToList(boxInOrder: BoxUI, zone: String) {
        scannedBoxes.value = scannedBoxes.value + BoxScanUI(
            box = boxInOrder,
            zone = zone,
            orderNumber = boxInOrder.orderNumber,
            containerScanTime = ZonedDateTime.now()
        )

        wineStagingStateRepo.saveStagingPartOne(
            WineStagingData(
                activityId = wineStagingParams.value?.activityId?.toIntOrNull(),
                shorOrderId = wineStagingParams.value?.shortOrderNumber.orEmpty(),
                nextActivityId = WineStagingType.WineStaging3,
                contactName = wineStagingParams.value?.contactName.orEmpty(),
                customerOrderNumber = wineStagingParams.value?.customerOrderNumber.orEmpty(),
                entityId = wineStagingParams.value?.entityId,
                stageByTime = wineStagingParams.value?.stageByTime,
                boxInfo = boxList.value?.map { it.toBoxData() },
                scannedBoxes = scannedBoxes.value.filter {
                    it.orderNumber == boxInOrder.orderNumber
                }.map { it.asScannedBoxData }
            ),
            wineStagingParams.value?.customerOrderNumber.orEmpty()
        )

        // stagingStateRepo.saveWineStagingPartTwo(stagingTwoData, boxInOrder.orderNumber, stagingActivityId.value.toString())
    }

    private fun handleBoxAlreadyScanned(boxUi: BoxUI) {
        val boxScanned = scannedBoxes.value.first { it.box.boxNumber == boxUi.boxNumber }
        val takeLastCount = BOX_ID_LENGTH
        val idFormat = R.string.box_id_format

        // If the same bag is scanned into the same zone twice, notify error
        if (boxScanned.zone == currentZone.value?.zone) {
            handleScanFailure(
                app.applicationContext?.getString(
                    R.string.error_item_already_scanned_format,
                    app.applicationContext?.getString(idFormat, boxUi.boxNumber.takeLast(takeLastCount)),
                    currentZoneBarcode.takeLast(StagingPart2PagerViewModel.MFC_TOTE_ID_UI_LENGTH)
                ).orEmpty()
            )
            // else, the bag is moved to the new zone
        } else {
            // remove scanned bag from old zone
            scannedBoxes.value = scannedBoxes.value - boxScanned
            // add scanned bag to new zone
            addScannedBoxToList(boxUi, currentZone.value?.zone!!)

            // beep and show snackbar
            handleScanSuccess(
                app.applicationContext?.getString(
                    R.string.bag_moved_format,
                    app.applicationContext?.getString(idFormat, boxUi.boxNumber.takeLast(takeLastCount)),
                    currentZoneBarcode
                ).orEmpty()
            )
        }
    }

    fun onManualEntryBarcodeReceived(stagingManualData: ManualEntryStagingData?) {
        viewModelScope.launch(dispatcherProvider.Main) {
            if (networkAvailabilityManager.isConnected.first()) {
                handleScannedZone(barcodeType = null, stagingManualData)
            } else {
                networkAvailabilityManager.triggerOfflineError { onManualEntryBarcodeReceived(stagingManualData) }
            }
        }
    }

    private fun getValidBag(bagOrToteId: String, customerOrderNumber: String) =
        boxList.value?.find { it.boxNumber == bagOrToteId || it.label == bagOrToteId && it.orderNumber == customerOrderNumber }

    companion object {
        private const val STAGING_ANIMATION_VISIBLE_DURATION_MS = 2000L
        private const val STAGING_ANIMATION_DELAY_MS = 250L
        private const val RETRIEVE_LABEL_DIALOG_TAG = "retrieveLabel"
        const val BOX_ID_LENGTH = 6
    }
}
