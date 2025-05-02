package com.albertsons.acupick.ui.staging

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.CustomerArrivalStatus
import com.albertsons.acupick.data.model.ErOrderStatus
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.ScanContainerReasonCode
import com.albertsons.acupick.data.model.ScannedBagData
import com.albertsons.acupick.data.model.StagingTwoData
import com.albertsons.acupick.data.model.StorageLocation
import com.albertsons.acupick.data.model.StorageLocationType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.barcode.StagingContainer
import com.albertsons.acupick.data.model.request.AssignUserWrapperRequestDto
import com.albertsons.acupick.data.model.request.CompleteDropOffRequestDto
import com.albertsons.acupick.data.model.request.FetchOrderStatusRequestDto
import com.albertsons.acupick.data.model.request.ScanContainerRequestDto
import com.albertsons.acupick.data.model.request.ScanContainerWrapperRequestDto
import com.albertsons.acupick.data.model.request.UpdateDugArrivalStatusRequestDto
import com.albertsons.acupick.data.model.request.ValidatePalletRequestDto
import com.albertsons.acupick.data.model.request.firstInitialDotLastName
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.CannotAssignToOrderDialogTypes
import com.albertsons.acupick.data.model.response.CustomerOrderStagingLocationDto
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.StagingSummaryDto
import com.albertsons.acupick.data.model.response.StagingType
import com.albertsons.acupick.data.model.response.location
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.ItemProcessorRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.StagingStateRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.infrastructure.utils.logError
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.arrivals.SelectedActivities
import com.albertsons.acupick.ui.arrivals.destage.OrderCompletionState
import com.albertsons.acupick.ui.bottomsheetdialog.ActionSheetOptions
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetArgDataAndTag
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetType
import com.albertsons.acupick.ui.bottomsheetdialog.CustomBottomSheetArgData
import com.albertsons.acupick.ui.bottomsheetdialog.ActionSheetDetails
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.manualentry.ManualEntryStagingParams
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryStagingData
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.models.BagScanUI
import com.albertsons.acupick.ui.models.BagUI
import com.albertsons.acupick.ui.models.SnackBarEvent
import com.albertsons.acupick.ui.models.StagingPart2TabUI
import com.albertsons.acupick.ui.models.StagingPart2UiData
import com.albertsons.acupick.ui.models.ToteUI
import com.albertsons.acupick.ui.models.ZoneBagCountUI
import com.albertsons.acupick.ui.models.ZoneLocationScanUI
import com.albertsons.acupick.ui.models.fullContactName
import com.albertsons.acupick.ui.picklistitems.MANUAL_ENTRY_STAGING_BOTTOMSHEET_TAG
import com.albertsons.acupick.ui.picklistitems.ScanTarget
import com.albertsons.acupick.ui.staging.print.PrintLabelUi
import com.albertsons.acupick.ui.staging.print.PrintLabelsHeaderUi
import com.albertsons.acupick.ui.util.DrawableIdHelper
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.UserFeedback
import com.albertsons.acupick.ui.util.isNullOrEqualTo
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.toIdHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.time.ZonedDateTime
import java.util.TimeZone

typealias IS_PALLET_OPEN = Boolean
typealias PALLET_MESSAGE = String

class StagingPart2PagerViewModel(val app: Application) : BaseViewModel(app) {
    // DI
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    private val apsRepo: ApsRepository by inject()
    private val pickRepo: PickRepository by inject()
    private val stagingStateRepo: StagingStateRepository by inject()
    private val siteRepo: SiteRepository by inject()
    private val dispatcherProvider: DispatcherProvider by inject()
    private val userFeedback: UserFeedback by inject()
    private val context: Context by inject()
    private val userRepo: UserRepository by inject()
    private val itemProcessorRepository: ItemProcessorRepository by inject()

    // Input flows
    var pickingAcitivityId = 0L
    val stagingActivityId = MutableStateFlow(0L)
    val toteList = MutableStateFlow<List<ToteUI>>(emptyList())
    val doBagLabelsStillNeedToBePrinted = MutableStateFlow(true)
    val isScanFromManualEntry = MutableLiveData(false)
    val orderCompletionUpdateEvent = MutableSharedFlow<OrderCompletionState>()
    var hasNavigatedToAddBags = false
    var hasNavigatedToUnAssignTote = false
    var hasNavigatedToStagingOptions = false

    // private val unassignedToteIdList = MutableLiveData<MutableList<String>>()
    private var erIdList = listOf<Long>()

    // API Data
    private val activityMap = MutableStateFlow<Map<String, StagingPart2UiData>>(emptyMap())
    fun getActivityByOrder(orderNumber: String): Flow<StagingPart2UiData?> = activityMap.map { it[orderNumber] }
    private val bagList: LiveData<MutableList<BagUI>> = MutableLiveData(mutableListOf())
    fun bagsByOrder(orderNumber: String): LiveData<List<BagUI>> = bagList.map { list -> list.filter { it.customerOrderNumber == orderNumber } }

    // Scanned bags flow
    private val scannedBags = MutableStateFlow<List<BagScanUI>>(emptyList())
    fun scannedBagsByOrder(orderNumber: String): Flow<List<BagScanUI>> = scannedBags.map { list -> list.filter { it.customerOrderNumber == orderNumber } }

    // Bags not scanned
    private val bagsNotScanned: LiveData<List<BagUI>> = scannedBags.combine(bagList.asFlow()) { scanned, all ->
        val scannedBagsUI = scanned.map { it.bag }
        all.minus(scannedBagsUI)
    }.asLiveData()

    // Tote count info acts as input event to populate toteCountMap
    var allDiscardedTotes = arrayListOf<Pair<String, List<String>>>()

    //  Complete scanned bag count equals original bag count.
    val isCompleteButtonEnabled = combine(scannedBags, activityMap, isDisplayingSnackbar.asFlow()) { scannedBags, allBags, snackShown ->
        scannedBags.isNotEmpty() && scannedBags.count() == allBags.values.sumOf { it.totalCount } && !snackShown
    }.asLiveData()

    val isManualEntryEnabled: LiveData<Boolean> = MutableLiveData()
    val isLoading = MutableLiveData<Boolean>()

    // Complete UI animation
    // val showAnimation: LiveData<Boolean> = MutableLiveData(false)
    val showAnimationBackground: LiveData<Boolean> = MutableLiveData(false)

    // State
    val currentZone = MutableStateFlow<ZoneBagCountUI?>(null)
    val scannedOrderNumber = MutableStateFlow<String?>(null)
    private var stagingCompleteTime: ZonedDateTime? = null

    // Page event
    val pageEvent = MutableStateFlow(0)

    var currentOrderShortId = ""
    var currentOrderNumber = ""
    var nextZoneBarcode = ""
    var isMultiLocationEnabled = siteRepo.locationFullEnabled
    var currentCustomerName = ""
    var currentStagingUI: StagingPart2UiData? = null
    var currentZoneBarcode = ""
    var currentOrderScannedBagData = listOf<ScannedBagData>()
    var isCurrentOrderMultiSource = MutableStateFlow(true)
    var isCurrentOrderBagPreferred = MutableStateFlow(true)

    private var shopFloorLockedStagingLocationList: MutableList<CustomerOrderStagingLocationDto>? = mutableListOf()

    // Static prompt (replaces snackbar)
    val snackbarEvent = MutableLiveData<SnackBarEvent<String>?>()
    val hideStaticPrompt = combine(isDisplayingSnackbar.asFlow(), isCompleteButtonEnabled.asFlow()) { isDisplayingSnackbar, isComplete ->
        // The static prompt needs to hide when a snack bar is showing or when isComplete
        isDisplayingSnackbar || isComplete
    }.asLiveData()

    val stagingSummary = MutableStateFlow<StagingSummaryDto?>(null)

    private fun switchToOrder(customerOrderNumber: String) {
        tabs.value?.indexOfFirst { it.tabArgument?.stagingPart2Params?.customerOrderNumber == customerOrderNumber }?.let {
            if (it != -1) pageEvent.value = it
        }
    }

    // Tab UI
    val tabs: LiveData<List<StagingPart2TabUI>> = bagList.map { bags ->
        bags.distinctBy { it.customerOrderNumber }.map {
            StagingPart2TabUI(
                tabLabel = firstInitial(it.contactFirstName, it.contactLastName),
                tabArgument = StagingPart2PagerFragmentArgs(
                    stagingPart2Params = StagingPart2Params(
                        pickingActivityId = pickingAcitivityId,
                        stagingActivityId = stagingActivityId.value,
                        toteList = toteList.value,
                        isPrintingStillNeeded = doBagLabelsStillNeedToBePrinted.value,
                        customerOrderNumber = it.customerOrderNumber
                    )
                )
            )
        }
    }

    val isCompleteList = MutableLiveData<List<OrderCompletionState>>(
        bagList.value?.map {
            OrderCompletionState(it.customerOrderNumber.orEmpty())
        }
    )

    val lastTabOrderNumber = tabs.map { it.lastOrNull()?.tabArgument?.stagingPart2Params?.customerOrderNumber }

    /** Tracks whether an item or tote is expected to be scanned next */
    private val activeScanTarget: LiveData<ScanTarget> = MutableLiveData(ScanTarget.Zone)

    private var currentStagingManualData: ManualEntryStagingData? = null
    private var currentBarCodeType: BarcodeType? = null

    val isStagingCompleted = MutableLiveData<Boolean>()

    // Scanned zone locations from remote
    private val existingScannedZoneLocations = MutableStateFlow<List<ZoneLocationScanUI>>(emptyList())
    // Combine flow that emits a merged list of Zone locations from remote, scanned bags into one unified list
    // Ensures all known locations per order number and Zone Type are grouped
    private val mergedZoneLocations = combine(existingScannedZoneLocations, scannedBags) { existingZoneLocations, scannedBags ->
        mergeExistingAndBagScannedZoneLists(existingZoneLocations, scannedBags)
    }.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = emptyList())
    var scanContainerReasonCode: ScanContainerReasonCode? = null

    // List of previous scanned zone locations from remote grouped by order number
    fun existingScannedZoneLocationsByOrder(orderNumber: String): Flow<List<ZoneLocationScanUI>> = existingScannedZoneLocations.map { list -> list.filter { it.customerOrderNumber == orderNumber } }
    // List of scanned zone locations combined from remote and scanned bags grouped by order number
    fun mergedZoneLocationsByOrder(orderNumber: String): Flow<List<ZoneLocationScanUI>> = mergedZoneLocations.map { list -> list.filter { it.customerOrderNumber == orderNumber } }

    init {
        // Launch reload dialog if not connected on startup
        viewModelScope.launch {
            if (networkAvailabilityManager.isConnected.first().not()) {
                showInternetIssueDialog()
            }
        }

        viewModelScope.launch(dispatcherProvider.IO) {
            orderCompletionUpdateEvent.collect {
                refreshPersistentPrompt()
            }
        }

        // Update toolbar title
        viewModelScope.launch(dispatcherProvider.IO) {
            activityMap.collect { actUI ->
                actUI.entries.firstOrNull()?.value.let {
                    showStagingTimeOnTitle(it?.stageByTime, it?.orderType, siteRepo.concernTime, siteRepo.warningTime, it?.releasedEventDateTime, it?.expectedEndDateTime)
                }
            }
        }

        // show back dialog if back button pressed
        viewModelScope.launch(dispatcherProvider.IO) {
            navigationButtonEvent.asFlow().collect {
                navigateHome()
            }
        }

        // TODO: MFC-Redesign  showToolbar method from other place. Need to remove this below code block.
        viewModelScope.launch(dispatcherProvider.IO) {
            /*isCurrentOrderMultiSource.collect {
                if (!it) showToolbarIcons()
            }*/
        }

        viewModelScope.launch(dispatcherProvider.IO) {
            toolbarRightSecondImageEvent.asFlow().collect {
                if (isCurrentOrderMultiSource.value) {
                    navigateToPickListSummary()
                } else {
                    inlineBottomSheetEvent.postValue(getStagingActionSheetArgsDataAndTagForBottomSheet())
                }
            }
        }

        // Register Dialog actions callbacks
        registerCloseAction(STAGING_ORDER_RELOAD_DIALOG_TAG) {
            closeActionFactory(positive = { stageOrder() })
        }
        registerCloseAction(STAGING_LOAD_DATA_RETRY_DIALOG_TAG) {
            closeActionFactory(
                positive = { loadData() },
                negative = { navigateHome() }
            )
        }
        registerCloseAction(STAGING_WIFI_ISSUE_DIALOG_TAG) {
            closeActionFactory(positive = { loadData() })
        }
        registerCloseAction(StagingPagerViewModel.STAGING_BACK_PRESSED_DIALOG_TAG) {
            closeActionFactory(positive = { navigateHome() })
        }

        registerCloseAction(StagingPagerViewModel.STAGING_ORDER_TAKEN_DIALOG_TAG) {
            closeActionFactory(positive = { navigateHome() })
        }

        registerCloseAction(STAGING_DRIVER_WARNING_DIALOG_TAG) {
            closeActionFactory(positive = { skipToDestaging() })
        }

        registerCloseAction(STAGING_NEW_LOCATION_DIALOG_TAG) {
            closeActionFactory(negative = { onSuccesfulScanLocation(nextZoneBarcode) })
        }

        registerCloseAction(STAGING_ACTION_SHEET_DIALOG_TAG) {
            closeActionFactory(
                positive = { index ->
                    // TODO Redesign Need to optimize action sheet bottom sheet click action handling.
                    hasNavigatedToStagingOptions = true
                    when (index) {
                        0 -> {
                            navigateToPickListSummary()
                        }
                        1 -> {
                            // currentOrderToteList = loadToteList()
                            navigateToUnAssignTote()
                        }
                        2 -> {
                            navigateToReprintBagLabels()
                        }
                        3 -> {
                            navigateToAddBagLabel()
                        }
                    }
                },
                negative = {
                }
            )
        }

        registerCloseAction(DIRECTION_STAGING_NEW_LOCATION_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    scanContainerReasonCode = ScanContainerReasonCode.SCAN_EXISTING_LOCATION
                },
                negative = {
                    scanContainerReasonCode = ScanContainerReasonCode.LOCATION_FULL
                    val zoneBarcodeType = (currentStagingManualData?.zone as? BarcodeType.Zone ?: currentBarCodeType) as BarcodeType.Zone
                    updateCurrentZone(zoneBarcodeType, currentStagingManualData)
                }
            )
        }

        updateTabIcons()
        loadData(fetchOrderStatus = true)
    }

    fun loadUnassignTote() {
        allDiscardedTotes.removeAll {
            it.first == currentOrderNumber
        }
        allDiscardedTotes.add(
            Pair(
                currentOrderNumber,
                stagingStateRepo.loadStagingPartTwo(
                    custId = currentOrderNumber,
                    activityId = stagingActivityId.value.toString(),
                )?.unassignedToteIdList ?: emptyList()
            )
        )
    }

    private fun navigateToPickListSummary() {
        viewModelScope.launch {
            delay(500)
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    StagingPart2PagerFragmentDirections.actionStagingPart2FragmentToPickListSummaryFragment(
                        picklistid = pickingAcitivityId,
                        customerordernumber = currentOrderNumber
                    )
                )
            )
        }
    }

    private fun navigateToUnAssignTote() {
        viewModelScope.launch {
            delay(500)
            hasNavigatedToUnAssignTote = true
            getActivityByOrder(currentOrderNumber).collect {
                currentStagingUI = it
                _navigationEvent.postValue(
                    NavigationEvent.Directions(
                        StagingPart2PagerFragmentDirections.actionStagePart2FragmentToUnassignTotesFragment(
                            unAssignTotesParams = UnAssignToteParams(
                                stagingActivityId = stagingActivityId.value,
                                toteList = toteList.value.filter { toteUI -> toteUI.customerOrderNumber == currentOrderNumber },
                                customerOrderNumber = currentOrderNumber,
                                currentStagingUI?.activityNo.orEmpty(),
                                currentStagingUI?.customerName.orEmpty(),
                                currentStagingUI?.shortOrderId.orEmpty(),
                                currentStagingUI?.isMultiSource.orFalse()
                            )
                        )
                    )
                )
            }
        }
    }

    private fun navigateToReprintBagLabels() {
        viewModelScope.launch {
            // TODO ReDesign Delay added to close the bottom sheet before navigating to other screen. Need to fix this without delay.
            delay(500)
            val groupedBagsByType = bagList.value?.filter { it.customerOrderNumber == currentOrderNumber }?.groupBy { it.zoneType }

            val bagsToPrint = groupedBagsByType?.map { entries ->
                PrintLabelsHeaderUi(
                    bagsUiList = entries.value,
                    scannedBagIds = scannedBags.value.map { it.bag.bagId },
                    customerOrderNumber = entries.value.first().customerOrderNumber,
                    shortOrderId = currentOrderShortId,
                    customeName = entries.value.first().fullContactName(),
                    isCustomerPreferBag = isCurrentOrderBagPreferred.value
                )
            }
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    StagingPart2PagerFragmentDirections.actionStagingPart2FragmentToPrintLabelsFragment(
                        activityId = stagingActivityId.value.toString(),
                        isCustomerPreferBag = isCurrentOrderBagPreferred.value,
                        printLabelUi = PrintLabelUi(bagsToPrint)
                    )
                )
            )
        }
    }

    private fun navigateToAddBagLabel() {
        viewModelScope.launch {
            hasNavigatedToAddBags = true
            scannedBags.value = emptyList()
            delay(500)
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    StagingPart2PagerFragmentDirections.actionStagingPart2FragmentToAddBagsFragment(
                        AddBagsUiData(
                            toteList = toteList.value.filter { it.customerOrderNumber == currentOrderNumber },
                            customerOrderNumber = currentOrderNumber,
                            shortOrderId = currentOrderShortId,
                            customeName = toteList.value.first { it.customerOrderNumber == currentOrderNumber }.customerName,
                            stagingId = stagingActivityId.value,
                            isCustomerPreferBag = isCurrentOrderBagPreferred.value
                        )
                    )
                )
            )
        }
    }

    // Update toolbar icons
    private fun showToolbarIcons(isCurrentOrderMfc: Boolean) {
        val drawabaleIcon = when (isCurrentOrderMfc) {
            true -> DrawableIdHelper.Id(R.drawable.ic_pick_list_info)
            false -> DrawableIdHelper.Id(R.drawable.ic_ellipses)
        }
        changeToolbarRightSecondExtraImageEvent.postValue(drawabaleIcon)
    }

    private fun getStagingActionSheetArgsDataAndTagForBottomSheet(): BottomSheetArgDataAndTag {
        return BottomSheetArgDataAndTag(
            data = CustomBottomSheetArgData(
                dialogType = BottomSheetType.ActionSheet,
                draggable = false,
                title = StringIdHelper.Raw(""),
                customDataParcel = ActionSheetDetails(if (isCurrentOrderBagPreferred.value) stagingOptions else customerBagPreferredStagingOptions),
                peekHeight = R.dimen.actionsheet_peek_height
            ),
            tag = STAGING_ACTION_SHEET_DIALOG_TAG
        )
    }

    private fun updateTabIcons() {
        viewModelScope.launch {
            orderCompletionUpdateEvent.collect { isCompleteUpdateEvent ->
                val dropList = isCompleteList.value?.filter { it.customerOrderNumber == isCompleteUpdateEvent.customerOrderNumber }
                isCompleteList.set(
                    isCompleteList.value
                        ?.toMutableList()
                        ?.apply {
                            removeAll(dropList ?: emptyList())
                            add(isCompleteUpdateEvent)
                        }
                )
            }
        }
    }

    private fun setManualEntryEnabledByOrder(orderStateList: List<OrderCompletionState>?, currentOrderNumber: String) {
        val currentOrder = orderStateList?.groupBy { it.customerOrderNumber }?.get(currentOrderNumber)
        val isCurrentComplete = currentOrder?.all { it.isComplete } ?: false
        isManualEntryEnabled.postValue(!isCurrentComplete)
    }

    fun updateCurrentOrder(stagingPart2Params: StagingPart2Params?) {
        currentOrderNumber = stagingPart2Params?.customerOrderNumber.orEmpty()
        setManualEntryEnabledByOrder(isCompleteList.value, currentOrderNumber)
        viewModelScope.launch {
            getActivityByOrder(currentOrderNumber).collect {
                isCurrentOrderMultiSource.value = it?.isMultiSource == true
                isCurrentOrderBagPreferred.value = it?.isCustomerBagPreference ?: true
                showToolbarIcons(it?.isMultiSource.orFalse()) // To update toolbar right image icon
                updateScanTargetIfCustomerNotPreferBag(isCurrentOrderBagPreferred.value)
            }
        }
        viewModelScope.launch {
            scannedBagsByOrder(currentOrderNumber).collect {
                currentOrderScannedBagData = it.map { scanned -> scanned.asScannedBagData }
            }
        }
        if (siteRepo.stagingType == StagingType.SHOP_FLOOR) {
            activeScanTarget.set(ScanTarget.Zone)
        }
        refreshPersistentPrompt()
    }

    private fun updateScanTargetIfCustomerNotPreferBag(isPreferBag: Boolean) {
        if (!isPreferBag) {
            if (currentZone.value != null) {
                activeScanTarget.set(ScanTarget.ToteOrZone)
            } else {
                activeScanTarget.set(ScanTarget.Zone)
            }
        } else {
            if (currentZone.value != null) {
                activeScanTarget.set(ScanTarget.Bag)
            } else {
                activeScanTarget.set(ScanTarget.Zone)
            }
        }

        refreshPersistentPrompt()
    }

    private fun advancePageIfOrderComplete(orderStateList: List<OrderCompletionState>?, currentOrderNumber: String) {
        val allOrdersComplete = scannedBags.value.count() == activityMap.value.values.sumOf { it.totalCount }
        if (!allOrdersComplete) {
            val currentOrder = orderStateList?.groupBy { it.customerOrderNumber }?.get(currentOrderNumber)
            val isCurrentComplete = currentOrder?.all { it.isComplete } ?: false
            if (isCurrentComplete) {
                val tabCount = tabs.value?.count() ?: 0
                // pageEvent starts at 0 tabCount starts at 1. Minus 1 on tabCount to keep even
                pageEvent.value = if (pageEvent.value < tabCount - 1) pageEvent.value.plus(1) else 0
            }
        }
    }

    private fun firstInitial(firstName: String?, lastName: String?) =
        if (firstName.isNotNullOrBlank()) {
            "${firstName?.first()}. "
        } else {
            " "
        } + (lastName ?: "")

    // /////////////////////////////////////////////////////////////////////////
    // Scanner logic
    // /////////////////////////////////////////////////////////////////////////

    /** Entry point for a barcode received from the scanner/DataWedge */
    fun onScannerBarcodeReceived(barcodeType: BarcodeType) {
        if (isCurrentOrderMultiSource.value && siteRepo.stagingType == StagingType.SHOP_FLOOR) { // For MFC store that stages to know location, see site 1531 QA
            when (activeScanTarget.value) {
                ScanTarget.Zone -> {
                    if (barcodeType.rawBarcode.length >= SHOP_FLOOR_ZONE_MIN_LENGTH &&
                        barcodeType !is BarcodeType.MfcPickingToteLicensePlate &&
                        barcodeType !is BarcodeType.MfcTote
                    ) {
                        handleScannedShopFloorZone(barcodeType = barcodeType, stagingManualData = null)
                    } else handleScanFailure(context.getString(R.string.error_zone_not_scanned))
                }
                ScanTarget.Tote, ScanTarget.ToteOrZone -> {
                    // Combining both Tote and ToteOrZone to allow user to scan a zone after scanning a tote
                    // and rescan older tote into this new zone for multilocation enabled case and if it is single location enabled only tote can be scanned
                    if (barcodeType.rawBarcode.length >= SHOP_FLOOR_ZONE_MIN_LENGTH &&
                        barcodeType !is BarcodeType.MfcPickingToteLicensePlate &&
                        barcodeType !is BarcodeType.MfcTote /*&& isMultiLocationEnabled*/
                    ) {
                        handleScannedShopFloorZone(barcodeType = barcodeType, stagingManualData = null)
                    } else {
                        when (barcodeType) {
                            is BarcodeType.MfcTote -> handleScannedShopFloorTote(barcodeType = barcodeType, getValidBagFromScannedBagOrMfcTote(barcodeType))
                            else -> handleScanFailure(context.getString(R.string.error_no_tote_scanned))
                        }.exhaustive
                    }
                }
                else -> handleScanFailure(generateBarcodeScanErrorMessage())
            }.exhaustive
        } else if (isCurrentOrderMultiSource.value && siteRepo.isDarkStoreEnabled) { // For MFC store that is dark store, phases 2
            if (barcodeType is BarcodeType.MfcPickingToteLicensePlate) handleScanFailure(generateBarcodeScanErrorMessage())
            when (barcodeType) {
                is BarcodeType.Zone ->
                    viewModelScope.launch(dispatcherProvider.Main) {
                        if (networkAvailabilityManager.isConnected.first()) {
                            handleScannedZoneForDarkStoreTempZoneStaging(barcodeType, null)
                        } else {
                            networkAvailabilityManager.triggerOfflineError { onScannerBarcodeReceived(barcodeType) }
                        }
                    }
                is StagingContainer -> handleScannedToteForMultisourceTempZoneStaging(barcodeType, getValidBagFromScannedBagOrMfcTote(barcodeType))
                else -> handleScanFailure(generateBarcodeScanErrorMessage())
            }.exhaustive
        } else if (isCurrentOrderMultiSource.value) { // For MFC store that is not dark store, phases 1
            if (barcodeType is BarcodeType.MfcPickingToteLicensePlate) handleScanFailure(generateBarcodeScanErrorMessage())
            when (barcodeType) {
                is BarcodeType.Zone -> handleScannedZoneForMultiSourceTempZoneStaging(barcodeType, null)
                is StagingContainer -> handleScannedToteForMultisourceTempZoneStaging(barcodeType, getValidBagFromScannedBagOrMfcTote(barcodeType))
                else -> handleScanFailure(generateBarcodeScanErrorMessage())
            }.exhaustive
        } else { // For regular store
            when (barcodeType) {
                is BarcodeType.Zone -> handleScannedZoneForNonMultisourceOrders(barcodeType, null)
                is BarcodeType.NonMfcTote -> handleScannedToteForCustomerBagPrefOrders(barcodeType, getValidBagFromScannedBagOrMfcTote(barcodeType))
                is StagingContainer -> handleScannedBagForNonMultiSourceOrders(barcodeType, getValidBagFromScannedBagOrMfcTote(barcodeType))
                else -> handleScanFailure(generateBarcodeScanErrorMessage())
            }.exhaustive
        }
    }

    fun onManualEntryBarcodeReceived(stagingManualData: ManualEntryStagingData?) {
        if (isCurrentOrderMultiSource.value && siteRepo.stagingType == StagingType.SHOP_FLOOR) {
            if (stagingManualData?.zone?.rawBarcode?.length ?: 0 >= SHOP_FLOOR_ZONE_MIN_LENGTH &&
                stagingManualData?.zone !is BarcodeType.MfcPickingToteLicensePlate &&
                stagingManualData?.zone !is BarcodeType.MfcTote
            ) {
                handleScannedShopFloorZone(barcodeType = null, stagingManualData)
            } else handleScanFailure(context.getString(R.string.error_zone_not_scanned))
        } else if (isCurrentOrderMultiSource.value && siteRepo.isDarkStoreEnabled) {
            if (stagingManualData?.stagingContainer is BarcodeType.MfcPickingToteLicensePlate) handleScanFailure(generateBarcodeScanErrorMessage())
            viewModelScope.launch(dispatcherProvider.Main) {
                if (networkAvailabilityManager.isConnected.first()) {
                    handleScannedZoneForDarkStoreTempZoneStaging(null, stagingManualData)
                } else {
                    networkAvailabilityManager.triggerOfflineError { onManualEntryBarcodeReceived(stagingManualData) }
                }
            }
        } else if (isCurrentOrderMultiSource.value) {
            if (stagingManualData?.stagingContainer is BarcodeType.MfcPickingToteLicensePlate) handleScanFailure(generateBarcodeScanErrorMessage())
            handleScannedZoneForMultiSourceTempZoneStaging(null, stagingManualData)
        } else {
            handleScannedZoneForNonMultisourceOrders(null, stagingManualData)
        }
    }

    /**
     * START Bag and MfcTote Scan Handling
     */
    private fun getValidBagFromScannedBagOrMfcTote(stagingContainerBarcode: StagingContainer): BagUI? {
        return when (stagingContainerBarcode) {
            is BarcodeType.Bag -> {
                getValidBag(bagOrToteId = stagingContainerBarcode.bagOrToteId, customerOrderNumber = stagingContainerBarcode.customerOrderNumber)
            }
            is BarcodeType.NonMfcTote -> {
                getValidNonMfcTote(bagOrToteId = stagingContainerBarcode.bagOrToteId, customerOrderNumber = stagingContainerBarcode.customerOrderNumber)
            }
            is BarcodeType.MfcTote -> {
                getValidMfcTote(toteId = stagingContainerBarcode.bagOrToteId, customerOrderNumber = stagingContainerBarcode.customerOrderNumber)
            }
            else -> null
        }
    }

    private fun getValidBag(bagOrToteId: String, customerOrderNumber: String) =
        bagList.value?.find { it.bagId == bagOrToteId && it.customerOrderNumber == customerOrderNumber }

    private fun getValidNonMfcTote(bagOrToteId: String, customerOrderNumber: String) =
        bagList.value?.find { it.bagId.uppercase() == bagOrToteId.uppercase() && it.customerOrderNumber == customerOrderNumber }

    private fun getValidMfcTote(toteId: String, customerOrderNumber: String): BagUI? {
        return bagList.value?.find {
            it.bagId == toteId &&
                (it.customerOrderNumber == customerOrderNumber || it.fulfillmentOrderNumber == customerOrderNumber)
        }
    }

    /**
     * Manages Zone location scanning logic: validation, shows messages and ensures aligns with the current order and bag mapping
     */
    private fun handleScannedZoneForNonMultisourceOrders(barcodeType: BarcodeType.Zone?, stagingManualData: ManualEntryStagingData?) {
        if (handleScannedZoneTempNotInOrderIfNeeded(stagingManualData, barcodeType)) return
        currentBarCodeType = barcodeType
        currentStagingManualData = stagingManualData

        // Scanned Location Code
        val zoneBarcode = stagingManualData?.zone?.rawBarcode ?: barcodeType?.rawBarcode
        val zoneBarcodeType = (stagingManualData?.zone as? BarcodeType.Zone ?: barcodeType) as BarcodeType.Zone

        // Reinitialize reason code upon re-scanning the zone post staging popup interaction
        // When a picker scans the last scanned location and there's a reason code, we need to send it.
        // If picker scans a location different from the last scanned location, we need not send the reason code.
        if (currentZone.value != null && currentZone.value?.zone != zoneBarcode) {
            scanContainerReasonCode = null
        }

        // Retrieves scanned locations by combining data from remote and scanned bags
        val scannedZoneLocations: List<String> = getScannedZoneLocationByOrder(combinedList = mergedZoneLocations.value, currentOrderNumber, zoneBarcodeType.zoneType)
        // Verifies if the scanned zone type exists in the bag list mapped to current order
        val currentOrderBags = bagList.value?.filter { it.customerOrderNumber == currentOrderNumber }
        val zoneTypeExists = currentOrderBags?.any { it.zoneType == zoneBarcodeType.zoneType }.orFalse()
        if (zoneTypeExists) {
            // Displays a direction staging popup when the zone already contains bag/loose items but does not include the currently scanned barcode
            if (scannedZoneLocations.isNotEmpty() && zoneBarcode !in scannedZoneLocations) {
                val joinedLocations = scannedZoneLocations.joinToString(", ")
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = CustomDialogArgData(
                            title = StringIdHelper.Id(R.string.see_existing_direction_location_title),
                            body = StringIdHelper.Format(R.string.part_of_the_order_direction_staged_in_other_location_error_body, joinedLocations),
                            bodyWithBold = joinedLocations,
                            positiveButtonText = StringIdHelper.Id(R.string.scan_existing_location),
                            negativeButtonText = StringIdHelper.Id(R.string.location_is_full_cta),
                            cancelOnTouchOutside = false
                        ),
                        tag = DIRECTION_STAGING_NEW_LOCATION_DIALOG_TAG
                    )
                )
                return
            }
        } else {
            // Shows message indicating the zone is not part of this order
            handleScanFailure(context.getString(R.string.error_zone_not_in_order))
            return
        }
        // Allow scanning
        updateCurrentZone(barcodeType, stagingManualData)
    }

    /**
     * Updates the current zone based on the provided barcode type and staging manual data.
     * This function determines the new current zone based on whether a zone barcode was scanned or if manual staging data is available.
     *
     * @param barcodeType The scanned barcode type.
     * @param stagingManualData manual entry staging data. It contains bar code information of zone and bags
     */
    private fun updateCurrentZone(barcodeType: BarcodeType.Zone?, stagingManualData: ManualEntryStagingData?) {
        val zoneBarcodeType = (stagingManualData?.zone as? BarcodeType.Zone ?: barcodeType) as BarcodeType.Zone
        currentZoneBarcode = zoneBarcodeType.rawBarcode

        // Update current zone
        currentZone.value = ZoneBagCountUI(
            zone = zoneBarcodeType.rawBarcode,
            zoneType = zoneBarcodeType.zoneType,
            isMultiSource = isCurrentOrderMultiSource.value
        )

        // Notify user to handle scan as success or, if coming from the Manual Entry screen, to trigger Bags scan
        if (isScanFromManualEntry.value == false) {
            if (isCurrentOrderBagPreferred.value) activeScanTarget.set(ScanTarget.Bag) else activeScanTarget.set(ScanTarget.Tote)
            // Reset isAdvancePageOrderRequired to false to block auto-navigation
            handleScanSuccess(context.getString(R.string.success_zone_scanned, zoneBarcodeType.rawBarcode), isAdvancePageOrderRequired = false)
        } else {
            stagingManualData?.stagingContainer?.let {
                onScannerBarcodeReceived(it)
            }
        }

        refreshPersistentPrompt()
    }

    /**
     * Retrieves a list of scanned locations for a specific customer order number and zone type
     * from a combined list of scanned zone locations (existing scan zone location and scanned bags).
     *
     * @return A list of strings representing the scanned locations for the specified
     * customer order number and zone type.
     */
    private fun getScannedZoneLocationByOrder(combinedList: List<ZoneLocationScanUI>, customerOrderNumber: String, zoneType: StorageType): List<String> {
        return combinedList
            .firstOrNull { it.customerOrderNumber == customerOrderNumber }
            ?.storageTypes
            ?.firstOrNull { it.containerType == zoneType }
            ?.locations ?: emptyList()
    }

    /**
     * Merges the existing scanned zone locations with newly scanned bags to create a consolidated list
     * of zone locations, incorporating the bag scan information. This function aims to update or
     * create [ZoneLocationScanUI] entries based on the scanned zone locations and bags.
     */
    private fun mergeExistingAndBagScannedZoneLists(existingScannedZoneLocations: List<ZoneLocationScanUI>, scannedBags: List<BagScanUI>): List<ZoneLocationScanUI> {
        val combineList = mutableListOf<ZoneLocationScanUI>()

        // Store all scanned locations from api
        combineList.addAll(existingScannedZoneLocations)

        // update from scanned bags
        scannedBags.forEach { bag ->
            val newData = ZoneLocationScanUI(customerOrderNumber = bag.customerOrderNumber, storageTypes = listOf(StorageLocationType(containerType = bag.bag.zoneType, locations = listOf(bag.zone))))
            val existingScanLocationUI = combineList.find { it.customerOrderNumber == newData.customerOrderNumber }

            if (existingScanLocationUI != null) {
                // Merge Locations with same customer Order No
                val updatedStorageTypes = existingScanLocationUI.storageTypes?.toMutableList() ?: mutableListOf()

                newData.storageTypes?.forEach { newLocationType ->
                    val existingLocationType = updatedStorageTypes.find { it.containerType == newLocationType.containerType }

                    if (existingLocationType != null) {
                        // Merge Locations
                        val updatedLocations = (existingLocationType.locations + newLocationType.locations).distinct()
                        updatedStorageTypes[updatedStorageTypes.indexOf(existingLocationType)] = StorageLocationType(existingLocationType.containerType, updatedLocations)
                    } else {
                        // If zone does not exist, add a new entry
                        updatedStorageTypes.add(newLocationType)
                    }
                }

                // Replace the old entry with updated one
                combineList[combineList.indexOf(existingScanLocationUI)] = ZoneLocationScanUI(existingScanLocationUI.customerOrderNumber, updatedStorageTypes)
            } else {
                // If customerOrderNumber does not exist, simply add it
                combineList.add(newData)
            }
        }

        return combineList
    }

    private fun handleScannedZoneTempNotInOrderIfNeeded(
        stagingManualData: ManualEntryStagingData?,
        barcodeType: BarcodeType.Zone?,
    ): Boolean {
        // order has no items in scanned zone type
        val zoneBarcode = stagingManualData?.zone as? BarcodeType.Zone ?: barcodeType

        if (bagList.value?.none { it.zoneType == zoneBarcode?.zoneType } == true) {
            handleScanFailure(context.getString(R.string.error_wrong_location))
            return true
        }
        return false
    }

    private fun handleScannedZoneForMultiSourceTempZoneStaging(zone: BarcodeType.Zone?, stagingManualData: ManualEntryStagingData?) {
        // order has no items in scanned zone type
        if (handleScannedZoneTempNotInOrderIfNeeded(stagingManualData, zone)) return
        val barcodeType = (if (stagingManualData != null) stagingManualData.zone as BarcodeType.Zone else zone) as BarcodeType.Zone
        currentZoneBarcode = barcodeType.rawBarcode

        // Update current zone
        currentZone.value = ZoneBagCountUI(
            zone = currentZoneBarcode,
            zoneType = barcodeType.zoneType,
            isMultiSource = isCurrentOrderMultiSource.value
        )

        // Notify user to handle scan as success or, if coming from the Manual Entry screen, to trigger Bags scan
        if (isScanFromManualEntry.value == false) {
            activeScanTarget.set(if (isCurrentOrderMultiSource.value) ScanTarget.Tote else ScanTarget.Bag)

            handleScanSuccess(context.getString(R.string.success_zone_scanned, currentZoneBarcode.takeLast(MFC_TOTE_ID_UI_LENGTH)))
        } else {
            stagingManualData?.stagingContainer?.let {
                onScannerBarcodeReceived(it)
            }
        }
        refreshPersistentPrompt()
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
                isWineOrder = siteRepo.isWineFulfillment,
                isDarkStore = siteRepo.isDarkStoreEnabled
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

    private fun palletFailureMessage(message: String?) = if (message.orEmpty().contains(app.getString(R.string.prompt_closed_pallet))) {
        app.getString(R.string.prompt_closed_pallet)
    } else {
        message.orEmpty()
    }

    private suspend fun handleScannedZoneForDarkStoreTempZoneStaging(zone: BarcodeType.Zone?, stagingManualData: ManualEntryStagingData?) {
        // order has no items in scanned zone type
        if (handleScannedZoneTempNotInOrderIfNeeded(stagingManualData, zone)) return

        val barcodeType = (if (stagingManualData != null) stagingManualData.zone as BarcodeType.Zone else zone) as BarcodeType.Zone
        val isPalletOpen = isPalletOpen(barcodeType.rawBarcode)
        if (isPalletOpen.first) {
            activeScanTarget.set(if (isCurrentOrderMultiSource.value) ScanTarget.Tote else ScanTarget.Bag)
            currentZoneBarcode = barcodeType.rawBarcode

            // Update current zone
            currentZone.value = ZoneBagCountUI(
                zone = currentZoneBarcode,
                zoneType = barcodeType.zoneType,
                isMultiSource = isCurrentOrderMultiSource.value
            )
            handleScanSuccess(context.getString(R.string.success_zone_scanned, currentZoneBarcode.takeLast(MFC_TOTE_ID_UI_LENGTH)))
        } else {
            handleScanFailure(isPalletOpen.second)
        }

        // Notify user to handle scan as success or, if coming from the Manual Entry screen, to trigger Bags scan
        if (isScanFromManualEntry.value != false && isPalletOpen.first) {
            stagingManualData?.stagingContainer?.let {
                onScannerBarcodeReceived(it)
            }
        }

        refreshPersistentPrompt()
    }

    private fun handleScannedShopFloorZone(barcodeType: BarcodeType?, stagingManualData: ManualEntryStagingData?) {
        currentBarCodeType = barcodeType
        currentStagingManualData = stagingManualData
        val shopFloorZoneBarcode = stagingManualData?.zone?.rawBarcode ?: barcodeType?.rawBarcode

        // Check if there is already a set zone for the erId
        shopFloorLockedStagingLocationList.getCurrentOrdersShopFloorLockedStagingLocationDto(activityMap.value[currentOrderNumber])?.location.let { lockedStagingLocation ->

            when {
                // There is no set storage location or the user scanned the correct storage location
                lockedStagingLocation.isNullOrEqualTo(shopFloorZoneBarcode) -> {
                    onSuccesfulScanLocation(shopFloorZoneBarcode)
                }
                // The incorrect location was scanned & assuming server don't allow more than one location
                lockedStagingLocation != shopFloorZoneBarcode -> {
                    nextZoneBarcode = shopFloorZoneBarcode.orEmpty()
                    inlineDialogEvent.postValue(
                        CustomDialogArgDataAndTag(
                            data = CustomDialogArgData(
                                title = StringIdHelper.Id(R.string.see_exisiting_location_title),
                                body = StringIdHelper.Format(R.string.part_of_the_order_staged_in_other_location_error_body, lockedStagingLocation?.takeLast(LOCATION_CHAR_LIMIT_MFC).orEmpty()),
                                positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
                                negativeButtonText = getLocationFullText(),
                                cancelOnTouchOutside = false
                            ),
                            tag = STAGING_NEW_LOCATION_DIALOG_TAG
                        )
                    )
                    return
                }
                else -> {}
            }
        }
    }

    private fun onSuccesfulScanLocation(shopFloorZoneBarcode: String?) {
        currentZoneBarcode = shopFloorZoneBarcode.orEmpty()

        if (isScanFromManualEntry.value == false) {
            // Update current zone
            currentZone.value = ZoneBagCountUI(
                zone = currentZoneBarcode,
                zoneType = null,
                isMultiSource = isCurrentOrderMultiSource.value
            )

            activeScanTarget.set(ScanTarget.Tote)
            handleScanSuccess(context.getString(R.string.success_location_scanned, currentBarCodeType?.rawBarcode?.takeLast(LOCATION_CHAR_LIMIT_MFC).orEmpty()))
        } else {
            currentStagingManualData?.stagingContainer?.let {
                activeScanTarget.set(ScanTarget.ToteOrZone)
                onScannerBarcodeReceived(it)
            }
        }
    }

    // TODO: Check for site level flag if there's more than one location is allowed
    private fun getLocationFullText() = if (isMultiLocationEnabled) StringIdHelper.Id(R.string.location_full_cta) else null
    private fun handleScannedBagForNonMultiSourceOrders(barcodeType: StagingContainer, bagUi: BagUI?) {
        when {
            // no zone scanned
            currentZone.value == null -> handleScanFailure(context.getString(R.string.error_scan_zone_first))

            // item is not in order
            !activityMap.value.keys.contains(barcodeType.customerOrderNumber) || bagUi == null ||
                barcodeType.customerOrderNumber != currentOrderNumber -> handleScanFailure(
                context.getString(
                    if (isCurrentOrderBagPreferred.value) R.string.error_item_not_in_order
                    else R.string.error_loose_item_not_in_order
                )
            )

            // item is scanned to wrong zone type
            bagUi.zoneType != currentZone.value?.zoneType -> {
                val resId = if (isCurrentOrderBagPreferred.value) R.string.error_wrong_zone_format else R.string.error_loose_wrong_zone_format
                handleScanFailure(context.getString(resId, barcodeType.bagOrToteId.takeLast(BAGS_ID_LENGTH)))
            }

            // bag was already scanned before
            scannedBags.value.any { it.bag.bagId == bagUi.bagId } -> handleBagAlreadyScanned(bagUi)

            // Success
            else -> {
                handleScanSuccess(context.getString(R.string.successfully_scanned))

                // If switching to new customer order number, switch tabs.
                if (barcodeType.customerOrderNumber != currentOrderNumber) {
                    switchToOrder(barcodeType.customerOrderNumber)
                }
                // Add scanned bag to list
                addScannedBagToList(bagUi, currentZone.value?.zone!!)

                // Set the snackbar to be both Bag and zone
                activeScanTarget.set(ScanTarget.BagOrZone)

                refreshPersistentPrompt()
            }
        }
        isScanFromManualEntry.postValue(false)
    }

    private fun handleScannedToteForCustomerBagPrefOrders(barcodeType: BarcodeType.NonMfcTote, bagUi: BagUI?) {
        when {
            // no zone scanned
            currentZone.value == null -> handleScanFailure(context.getString(R.string.error_scan_zone_first))

            // item is not in order
            !activityMap.value.keys.contains(barcodeType.customerOrderNumber) ||
                bagUi == null -> handleScanFailure(context.getString(R.string.error_tote_not_in_order))

            // item is scanned to wrong zone type
            bagUi.zoneType != currentZone.value?.zoneType ->
                handleScanFailure(context.getString(R.string.error_wrong_zone_format_tote, barcodeType.bagOrToteId.takeLast(NON_MFC_TOTE_ID_UI_LENGTH)))

            // bag was already scanned before
            scannedBags.value.any { it.bag.bagId == barcodeType.bagOrToteId } -> handleBagAlreadyScanned(bagUi)

            // Success
            else -> {
                handleScanSuccess(
                    context.getString(
                        R.string.success_tote_scanned_into_zone_format,
                        barcodeType.bagOrToteId.takeLast(NON_MFC_TOTE_ID_UI_LENGTH), currentZone.value?.zone?.takeLast(NON_MFC_TOTE_ID_UI_LENGTH) ?: ""
                    )
                )

                // If switching to new customer order number, switch tabs.
                if (barcodeType.customerOrderNumber != currentOrderNumber) {
                    switchToOrder(barcodeType.customerOrderNumber)
                }
                // Add scanned tote to list
                addScannedBagToList(bagUi, currentZone.value?.zone!!)

                // Set the snackbar to Tote
                activeScanTarget.set(ScanTarget.ToteOrZone)

                refreshPersistentPrompt()
            }
        }
    }

    private fun handleScannedToteForMultisourceTempZoneStaging(barcodeType: StagingContainer, bagUi: BagUI?) {
        when {
            // no zone scanned
            currentZone.value == null -> handleScanFailure(context.getString(R.string.error_scan_zone_first))

            // item is not in order
            !activityMap.value.keys.contains(barcodeType.customerOrderNumber) ||
                bagUi == null -> handleScanFailure(context.getString(R.string.error_tote_not_in_order))

            // item is scanned to wrong zone type
            bagUi.zoneType != currentZone.value?.zoneType ->
                handleScanFailure(context.getString(R.string.error_wrong_zone_format_tote, barcodeType.bagOrToteId.takeLast(MFC_TOTE_ID_UI_LENGTH)))

            // bag was already scanned before
            scannedBags.value.any { it.bag.bagId == barcodeType.bagOrToteId } -> handleBagAlreadyScanned(bagUi)

            // Success
            else -> {
                handleScanSuccess(
                    context.getString(
                        R.string.success_tote_scanned_into_zone_format,
                        barcodeType.bagOrToteId.takeLast(MFC_TOTE_ID_UI_LENGTH), currentZone.value?.zone?.takeLast(MFC_TOTE_ID_UI_LENGTH) ?: ""
                    )
                )

                // If switching to new customer order number, switch tabs.
                if (barcodeType.customerOrderNumber != currentOrderNumber) {
                    switchToOrder(barcodeType.customerOrderNumber)
                }
                // Add scanned tote to list
                addScannedBagToList(bagUi, currentZone.value?.zone!!)

                // Set the snackbar to Tote
                activeScanTarget.set(ScanTarget.ToteOrZone)

                refreshPersistentPrompt()
            }
        }
        isScanFromManualEntry.postValue(false)
    }

    private fun handleScannedShopFloorTote(barcodeType: StagingContainer, bagUi: BagUI?) {

        val orderNumberByBag = bagList.value?.find { scanned -> scanned.containerId == barcodeType.bagOrToteId }?.customerOrderNumber

        // Get the current zone based on what was scanned
        currentZone.value = ZoneBagCountUI(
            zone = currentZoneBarcode,
            zoneType = bagUi?.zoneType,
            isMultiSource = true
        )

        when {
            // item is not in order
            bagUi == null || orderNumberByBag != currentOrderNumber -> handleScanFailure(context.getString(R.string.error_tote_not_in_order))

            // bag was already scanned before
            scannedBags.value.any { it.bag.bagId == barcodeType.bagOrToteId } -> {
                // If this was the first bag scanned to this location or multilocation enabled, send the staging location to the server
                // also if the bag needs to be staged in another location we send the information to the server (only for the multilocation enabled)
                if ((!doesCurrentOrderAlreadyHaveAStagingLocationSet() || isMultiLocationEnabled) &&
                    getCurrentOrderStagingLocationSet() != currentZone.value?.zone!!
                ) {
                    viewModelScope.launch(dispatcherProvider.IO) {
                        if (isShopFloorStagingLocationAccepted(barcodeType.customerOrderNumber, currentZone.value?.zone!!)) {
                            handleBagAlreadyScanned(bagUi)
                        }
                    }
                } else {
                    handleBagAlreadyScanned(bagUi)
                }
            }

            else -> {
                // If this was the first bag scanned to this location or multilocation enabled, send the staging location to the server
                // If it is single location enabled or loation already sent don't send it to server
                if ((!doesCurrentOrderAlreadyHaveAStagingLocationSet() || isMultiLocationEnabled) &&
                    getCurrentOrderStagingLocationSet() != currentZone.value?.zone!!
                ) {
                    viewModelScope.launch(dispatcherProvider.IO) {
                        if (isShopFloorStagingLocationAccepted(barcodeType.customerOrderNumber, currentZone.value?.zone!!)) {
                            showShopFloorToteScanSuccessful(barcodeType, bagUi)
                        }
                    }
                } else {
                    showShopFloorToteScanSuccessful(barcodeType, bagUi)
                }
            }
        }
        isScanFromManualEntry.postValue(false)
    }

    private fun doesCurrentOrderAlreadyHaveAStagingLocationSet(): Boolean {
        val currentSetStagingLocationDto = shopFloorLockedStagingLocationList.getCurrentOrdersShopFloorLockedStagingLocationDto(activityMap.value[currentOrderNumber])
        return currentSetStagingLocationDto?.stagingLocation != null
    }

    private fun getCurrentOrderStagingLocationSet(): String? {
        val currentSetStagingLocationDto = shopFloorLockedStagingLocationList.getCurrentOrdersShopFloorLockedStagingLocationDto(activityMap.value[currentOrderNumber])
        return currentSetStagingLocationDto?.stagingLocation
    }

    private fun showShopFloorToteScanSuccessful(barcodeType: StagingContainer, bagUi: BagUI) {
        // Show the success snackbar
        handleScanSuccess(context.getString(R.string.success_tote_scanned_into_zone_format, barcodeType.bagOrToteId.takeLast(5), currentZone.value?.zone?.takeLast(LOCATION_CHAR_LIMIT_MFC) ?: ""))

        // If switching to new customer order number, switch tabs.
        if (barcodeType.customerOrderNumber != currentOrderNumber) {
            switchToOrder(barcodeType.customerOrderNumber)
        }

        // Add scanned bag to list
        addScannedBagToList(bagUi, currentZone.value?.zone!!)
        // if it is a multi location enabled then set the active scan target to ToteOrZone instead of Tote.
        /* if (isMultiLocationEnabled)*/ activeScanTarget.postValue(ScanTarget.ToteOrZone)

        refreshPersistentPrompt()
    }

    // /** Check if current order number has additional bags */
    private fun isOrderComplete() =
        isCompleteList.value?.find { orderCompletionState ->
            orderCompletionState.customerOrderNumber == currentOrderNumber
        }?.isComplete == true

    private fun addScannedBagToList(bagInOrder: BagUI, zone: String) {
        scannedBags.value = scannedBags.value + BagScanUI(
            bag = bagInOrder,
            zone = zone,
            customerOrderNumber = bagInOrder.customerOrderNumber.orEmpty(),
            containerScanTime = ZonedDateTime.now(),
            scanContainerReasonCode = scanContainerReasonCode
        )
        scanContainerReasonCode = null
        scannedOrderNumber.value = bagInOrder.customerOrderNumber
        val customerBags = scannedBags.value.filter {
            it.customerOrderNumber == bagInOrder.customerOrderNumber
        }
        val stagingTwoData = StagingTwoData(
            customerOrderNumber = bagInOrder.customerOrderNumber.orEmpty(),
            scannedBagList = customerBags.map { it.asScannedBagData },
            activityId = stagingActivityId.value,
            unassignedToteIdList = allDiscardedTotes.find { it.first == bagInOrder.customerOrderNumber.orEmpty() }?.second ?: mutableListOf(),
            isMultiSource = isCurrentOrderMultiSource.value
        )
        stagingStateRepo.saveStagingPartTwo(stagingTwoData, bagInOrder.customerOrderNumber.orEmpty(), stagingActivityId.value.toString())
    }

    private fun clearStateData() = stagingStateRepo.clear()

    private fun handleScanSuccess(message: String, isAdvancePageOrderRequired: Boolean = true) {
        userFeedback.setSuccessScannedSoundAndHaptic()
        showAnchoredSnackBar(
            AcupickSnackEvent(
                message = StringIdHelper.Raw(message),
                type = SnackType.SUCCESS,
                isDismissable = true,
                onDismiss = {
                    if (isAdvancePageOrderRequired) advancePageIfOrderComplete(isCompleteList.value, currentOrderNumber)
                    refreshPersistentPrompt()
                }
            )
        )
        refreshPersistentPrompt()
    }

    private fun handleScanFailure(errorMessage: String) {
        userFeedback.setFailureScannedSoundAndHaptic()
        showAnchoredSnackBar(AcupickSnackEvent(message = StringIdHelper.Raw(errorMessage), type = SnackType.ERROR, isDismissable = true))
    }

    private fun handleBagAlreadyScanned(bagInOrder: BagUI) {
        val bagScanned = scannedBags.value.first { it.bag.bagId == bagInOrder.bagId }
        val takeLastCount = if (isCurrentOrderMultiSource.value) MFC_TOTE_ID_UI_LENGTH else BAGS_ID_LENGTH
        val idFormat = if (isCurrentOrderMultiSource.value) R.string.tote_id_format else R.string.bag_id_format
        val zoneOrLocation = isCurrentOrderMultiSource.value && siteRepo.stagingType == StagingType.SHOP_FLOOR

        // If the same bag is scanned into the same zone twice, notify error
        if (bagScanned.zone == currentZone.value?.zone) {
            handleScanFailure(
                if (zoneOrLocation)
                    context.getString(
                        R.string.error_item_already_scanned_location_format,
                        context.getString(idFormat, bagInOrder.bagId.takeLast(takeLastCount)),
                        currentZoneBarcode.takeLast(LOCATION_CHAR_LIMIT_MFC)
                    )
                else
                    context.getString(
                        R.string.error_item_already_scanned_zone_format,
                        context.getString(idFormat, bagInOrder.bagId.takeLast(takeLastCount)),
                        currentZoneBarcode.takeLast(MFC_TOTE_ID_UI_LENGTH)
                    )
            )

            // else, the bag is moved to the new zone
        } else {

            // remove scanned bag from old zone
            scannedBags.value = scannedBags.value - bagScanned

            // add scanned bag to new zone
            addScannedBagToList(bagInOrder, currentZoneBarcode)

            // beep and show snackbar
            handleScanSuccess(
                context.getString(
                    R.string.bag_moved_format,
                    context.getString(idFormat, bagInOrder.bagId.takeLast(takeLastCount)),
                    currentZoneBarcode
                )
            )
        }
    }

    private fun generateBarcodeScanErrorMessage(): String {
        return when (activeScanTarget.value) {
            ScanTarget.Zone -> {
                context.getString(R.string.error_zone_not_scanned)
            }
            ScanTarget.Bag -> {
                context.getString(R.string.error_no_bag_scanned)
            }
            ScanTarget.BagOrZone -> {
                context.getString(R.string.no_bag_or_zone_scanned)
            }
            ScanTarget.Tote -> {
                context.getString(R.string.error_no_tote_scanned)
            }
            ScanTarget.ToteOrZone -> {
                context.getString(R.string.no_tote_or_zone_scanned)
            }
            else -> {
                "" // nothing
            }
        }
    }

    private fun scanRemainingBagsToDummyZones() {
        val scannedBagsUi: List<BagUI> = scannedBags.value.map { it.bag }
        val unscannedBags = bagList.value?.minus(scannedBagsUi)
        unscannedBags?.map { bag ->
            BagScanUI(
                bag = bag,
                zone = DUMMY_ZONE_FORMAT.format(bag.zoneType.zonePrefix),
                customerOrderNumber = bag.customerOrderNumber.orEmpty(),
                containerScanTime = ZonedDateTime.now(),
            )
        }?.also {
            scannedBags.value = it.plus(scannedBags.value)
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // UI callbacks
    // /////////////////////////////////////////////////////////////////////////

    private fun onManualEntryClicked() {
        val scannedBagUI = scannedBags.value.map { it.bag }
        val bagsNotScanned = bagList.value?.minus(scannedBagUI)

        val params = ManualEntryStagingParams(
            scannedBagUiList = bagsNotScanned,
            zone = currentZone.value?.zone,
            customerOrderNumber = currentOrderNumber,
            activityId = activityMap.value.entries.firstOrNull()?.value?.activityNo ?: "",
            isMutliSource = isCurrentOrderMultiSource.value,
            shortOrderId = activityMap.value[currentOrderNumber]?.shortOrderId,
            customerName = activityMap.value[currentOrderNumber]?.customerName,
            isCustomerPreferBag = isCurrentOrderBagPreferred.value
        )

        if (isCurrentOrderMultiSource.value) {
            _navigationEvent.postValue(
                NavigationEvent.Directions(NavGraphDirections.actionToManualEntryStagingMfcFragment(params))
            )
        } else {
            inlineBottomSheetEvent.postValue(getManualEntryStagingArgData(params))
        }
    }

    private fun getManualEntryStagingArgData(params: ManualEntryStagingParams) = BottomSheetArgDataAndTag(
        data = CustomBottomSheetArgData(
            dialogType = BottomSheetType.ManualEntryStaging,
            title = StringIdHelper.Raw(""),
            customDataParcel = params,
            peekHeight = R.dimen.expanded_bottomsheet_peek_height
        ),
        tag = MANUAL_ENTRY_STAGING_BOTTOMSHEET_TAG
    )

    fun onCompleteClicked(customerOrderNumber: String) {
        val isComplete = isCompleteList.value?.firstOrNull { it.customerOrderNumber == customerOrderNumber }?.isComplete == true
        val isLastTab = lastTabOrderNumber.value == customerOrderNumber
        val isAllComplete = isCompleteList.value?.none { !it.isComplete }
        val isEnabled = if (isLastTab) isAllComplete == true else isComplete
        when {
            !isEnabled -> showDisableButtonInfo()
            isLastTab -> stageOrder()
            else -> switchToNextTab(customerOrderNumber)
        }
    }

    private fun showDisableButtonInfo() {
        val message = if (activeScanTarget.value == ScanTarget.Bag) R.string.please_scan_bag_or_location_to_continue else R.string.please_scan_location_to_continue
        showAnchoredSnackBar(
            AcupickSnackEvent(
                message = StringIdHelper.Id(message),
                type = SnackType.INFO,
                isDismissable = true
            )
        )
    }

    private fun switchToNextTab(customerOrderNumber: String) {
        viewModelScope.launch {
            isCompleteList.value?.indexOfFirst { it.customerOrderNumber == customerOrderNumber }?.plus(1)?.let {
                pageEvent.emit(it)
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // API Calls
    // /////////////////////////////////////////////////////////////////////////
    internal fun loadData(fetchOrderStatus: Boolean = false) {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                showInternetIssueDialog()
            } else {
                when (val result = isBlockingUi.wrap { pickRepo.getActivityDetails(stagingActivityId.filter { it != 0L }.first().toString()) }) {
                    is ApiResult.Success -> {
                        checkUserStillAssigned(result)
                        activityMap.value = result.data.containerActivities?.map { StagingPart2UiData(result.data, it) }
                            ?.groupBy { it.customerOrderNumber ?: "" }?.mapValues { it.value.first() } ?: emptyMap()

                        // ACUPICK-896 persist staging part 2
                        activityMap.value.values.forEach { custData ->
                            custData.customerOrderNumber?.let { orderNumber ->
                                // load the previous data if it exists, if not, construct and save the empty version
                                // its presence will cause staging part one to be bypassed
                                val activityId = result.data.actId!!
                                val activityIdString = activityId.toString()
                                var stagingPartTwoData = stagingStateRepo.loadStagingPartTwo(orderNumber, activityIdString)
                                if (stagingPartTwoData == null) {
                                    stagingPartTwoData = StagingTwoData(
                                        customerOrderNumber = orderNumber,
                                        scannedBagList = emptyList(),
                                        activityId = activityId,
                                        unassignedToteIdList = mutableListOf(),
                                        isMultiSource = custData.isMultiSource == true
                                    )
                                    stagingStateRepo.saveStagingPartTwo(stagingPartTwoData, orderNumber, activityIdString)
                                }
                                if (stagingPartTwoData.unassignedToteIdList.isNotEmpty())
                                    allDiscardedTotes.add(Pair(orderNumber, stagingPartTwoData.unassignedToteIdList))

                                val list = scannedBags.value.toMutableList()
                                if (!scannedBags.value.containsAll(stagingPartTwoData.scannedBagList?.map { BagScanUI.fromScannedBagData(it) }.orEmpty())) {
                                    list.addAll(
                                        stagingPartTwoData.scannedBagList?.map { BagScanUI.fromScannedBagData(it) }.orEmpty()
                                    )
                                    scannedBags.value = list
                                }
                            }
                        }

                        bagList.postValue(
                            mutableListOf<BagUI>().apply {
                                addAll(
                                    result.data.containerActivities?.filter { containerActivity ->
                                        containerActivity.containerId.isNotNullOrBlank() && containerActivity.containerType != null &&
                                            (containerActivity.type == ContainerType.BAG || containerActivity.type == ContainerType.LOOSE_ITEM || containerActivity.type == ContainerType.TOTE)
                                    }?.map {
                                        BagUI(
                                            bagId = it.containerId!!,
                                            zoneType = it.containerType!!,
                                            customerOrderNumber = it.customerOrderNumber,
                                            fulfillmentOrderNumber = it.reference?.entityId,
                                            contactFirstName = it.contactFirstName,
                                            contactLastName = it.contactLastName,
                                            containerId = it.containerId ?: "",
                                            isBatch = result.data.erId == null,
                                            isLoose = it.type == ContainerType.LOOSE_ITEM
                                        )
                                    } ?: listOf()
                                )
                            }
                        )
                        erIdList = result.data.containerActivities?.map { it.erId ?: 0L }?.distinct() ?: emptyList()
                        val orderType = result.data.orderType
                        if ((orderType == OrderType.FLASH || orderType == OrderType.FLASH3P) && !siteRepo.isMFCSite && fetchOrderStatus) {
                            if (erIdList.size == 1) {
                                showWarningIfDriverArrived(erIdList.first(), orderType)
                            }
                        }

                        // Scanned Zone Locations from remote
                        result.data.previousLocations?.let {
                            existingScannedZoneLocations.value = convertScannedZoneLocationsFromApiToUiModel(it)
                        }
                    }
                    is ApiResult.Failure -> {
                        if (result is ApiResult.Failure.Server) {
                            val type = result.error?.errorCode?.resolvedType
                            when (type?.cannotAssignToOrder()) {
                                true -> {
                                    val count = tabs.value?.size ?: 1
                                    val serverErrorType =
                                        if (type == ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY) CannotAssignToOrderDialogTypes.STAGING else CannotAssignToOrderDialogTypes.REGULAR
                                    serverErrorCannotAssignUser(serverErrorType, count > 1)
                                }
                                else -> handleApiError(errorType = result)
                            }
                        } else {
                            handleApiError(errorType = result)
                        }
                    }
                }
            }
        }
    }

    /**
     * Converts a list of scanned locations received from the API into a list of UI-specific zone location models grouped by order number
     * Merges all zone-location pairs (ZoneType) under the same Order Number
     *
     * @param apiLocations A list of [StorageLocation] objects received from the API.
     * @return A list of [ZoneLocationScanUI] objects, representing the transformed data for UI consumption.
     */
    private fun convertScannedZoneLocationsFromApiToUiModel(apiLocations: List<StorageLocation>): List<ZoneLocationScanUI> {
        val uiModelMap = mutableMapOf<String, MutableList<StorageLocationType>>()
        apiLocations.forEach { apiLocation ->
            val storageTypes = mutableListOf<StorageLocationType>()
            apiLocation.storageTypes?.forEach { apiStorageType ->
                storageTypes.add(StorageLocationType(apiStorageType.containerType, apiStorageType.locations))
            }
            // Merge storageTypes if Order number already exists
            uiModelMap.merge(apiLocation.customerOrderNumber, storageTypes) { existing, new ->
                mergeScannedZoneLocations(existing, new)
            }
        }
        return uiModelMap.map { ZoneLocationScanUI(it.key, it.value) }
    }

    /**
     *  Merges two lists of StorageLocationType by zone, combining and de-duplicating locations and ensures only unique locations per zone while preserving all zone from both lists
     *
     *  @param existingLocations The existing mutable list of [StorageLocationType] objects.
     *  @param newLocations The mutable list of newly scanned [StorageLocationType] objects.
     *  @return A mutable list of [StorageLocationType] containing all unique locations from both input lists.
     */
    private fun mergeScannedZoneLocations(existingLocations: MutableList<StorageLocationType>, newLocations: MutableList<StorageLocationType>): MutableList<StorageLocationType>? {
        // Convert the list of existing StorageLocationType objects into a map with 'containerType' as the key
        val containerMap = existingLocations.associateBy { it.containerType }.toMutableMap()
        newLocations.forEach { newLocationType ->
            containerMap.merge(newLocationType.containerType, newLocationType) { old, new ->
                StorageLocationType(old.containerType, (old.locations + new.locations).distinct())
            }
        }
        return containerMap.values.toMutableList()
    }

    private fun loadStagingSummaryData() {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                showInternetIssueDialog()
            } else {
                when (val result = isBlockingUi.wrap { pickRepo.getStagingSummary(stagingActivityId.filter { it != 0L }.first().toString()) }) {
                    is ApiResult.Success -> {
                        stagingSummary.emit(result.data)
                    }
                    is ApiResult.Failure -> handleApiError(result, retryAction = { loadStagingSummaryData() })
                }
            }
        }
    }

    private fun stageOrder() {
        viewModelScope.launch(dispatcherProvider.IO) {
            setStagingCompleteTime()
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { stageOrder() }
            } else if (sendScannedBags() && completeStaging()) {
                showStagingCompletedView()
                clearStateData()
                pickRepo.clearAllData()
                itemProcessorRepository.clearItemProcessorData()
            }
        }
    }

    private fun setStagingCompleteTime() {
        // Record the timestamp of the first time the user is able to press Complete Stage.
        // This timestamp will be used on subsequent retries if network is unavailable.
        if (stagingCompleteTime == null) {
            stagingCompleteTime = scannedBags.value.map { bag -> bag.containerScanTime }.maxOfOrNull { it ?: ZonedDateTime.now() }
        }
    }

    private suspend fun sendScannedBags(): Boolean {

        var orderNumber: String? = null
        val result = isBlockingUi.wrap {
            apsRepo.scanContainers(
                ScanContainerWrapperRequestDto(
                    actId = stagingActivityId.first(),
                    containerReqs = scannedBags.first().map { scannedBag ->
                        orderNumber = scannedBag.customerOrderNumber
                        ScanContainerRequestDto(
                            containerId = scannedBag.bag.bagId,
                            stagingLocation = scannedBag.zone,
                            containerScanTime = scannedBag.containerScanTime,
                            isLoose = scannedBag.bag.isLoose,
                            reasonCode = scannedBag.scanContainerReasonCode
                        )
                    },
                    lastScanTime = stagingCompleteTime,
                    multipleHandoff = tabs.value?.size ?: 0 > 1,
                    isDarkStore = siteRepo.isDarkStoreEnabled,
                    isWineFulfillment = siteRepo.isWineFulfillment
                )
            )
        }

        stagingActivityId.first().toString().logError(
            "Activity Id is null. StagingPart2PagerViewModel(sendScannedBags)," +
                " Order Id-$orderNumber, User Id-${userRepo.user.value?.userId}, storeId-${siteRepo.siteDetails.value?.siteId}",
            acuPickLogger
        )

        (result as? ApiResult.Failure)?.let { handleApiError(it) }
        return result is ApiResult.Success
    }

    private suspend fun completeStaging(): Boolean {
        val result = isBlockingUi.wrap {
            apsRepo.completeDropOffActivity(
                CompleteDropOffRequestDto(
                    actId = stagingActivityId.first(),
                    containerIdList = getDiscardedTotes(),
                    dropOffCompTime = stagingCompleteTime,
                )
            )
        }
        (result as? ApiResult.Failure)?.let { handleApiError(it) }
        return result is ApiResult.Success
    }

    private fun getDiscardedTotes(): List<String> {
        val discardedTotes = arrayListOf<String>()
        allDiscardedTotes.forEach {
            discardedTotes.addAll(it.second)
        }
        return discardedTotes.distinct()
    }

    fun skipToDestaging() {
        // cannot skip if batch staging
        if (erIdList.size != 1) {
            acuPickLogger.d("[skipToDestaging] Cannot skip to De-staging if staging batch pick list")
            return
        }
        scanRemainingBagsToDummyZones()
        setStagingCompleteTime()
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.value.not()) {
                networkAvailabilityManager.triggerOfflineError { skipToDestaging() }
            } else {
                if (sendScannedBags() && completeStaging()) {

                    // ACIP-203884 -> Logging additional info in AppD to analyze how storeId going as null for this Api call.
                    userRepo.user.value?.let {
                        it.selectedStoreId.logError(
                            "Null StoreId. StagingPart2PagerViewModel(skipToDestaging), " +
                                "OrderId-${scannedBags.value.firstOrNull()?.customerOrderNumber}, UserId-${it.userId}, siteId-${it.selectedStoreId}"
                        )
                    }

                    val searchOrderResult = isBlockingUi.wrap {
                        apsRepo.searchCustomerPickupOrders(
                            siteId = userRepo.user.value?.selectedStoreId,
                            orderNumber = scannedBags.value.firstOrNull()?.customerOrderNumber,
                            onlyPickupReady = true,
                        )
                    }
                    if (searchOrderResult is ApiResult.Success) {
                        val matchingOrder = searchOrderResult.data.firstOrNull { it.status == ErOrderStatus.DROPPED_OFF }
                        if (matchingOrder != null) {
                            isBlockingUi.wrap {
                                apsRepo.updateDugArrivalStatus(
                                    UpdateDugArrivalStatusRequestDto(
                                        customerArrivalStatus = CustomerArrivalStatus.ARRIVED,
                                        erId = matchingOrder.erId,
                                        orderNumber = matchingOrder.customerOrderNumber,
                                        siteId = userRepo.user.value?.selectedStoreId?.toLongOrNull() ?: 0,
                                        statusEventTimestamp = ZonedDateTime.now(),
                                        estimateTimeOfArrival = ZonedDateTime.now(),
                                    )
                                )
                            }
                            val assignResult = isBlockingUi.wrap {
                                apsRepo.assignUserToHandoffs(
                                    AssignUserWrapperRequestDto(
                                        actIds = listOf(matchingOrder.pickupActId ?: 0),
                                        replaceOverride = true,
                                        resetPickList = true,
                                        user = userRepo.user.value!!.toUserDto(),
                                        etaArrivalFlag = siteRepo.isEtaArrivalEnabled
                                    )
                                )
                            }
                            if (assignResult is ApiResult.Success) {
                                // make call to fetch container activities
                                val detailResult = assignResult.data.first().erId?.let {
                                    apsRepo.pickUpActivityDetails(it, true)
                                }

                                if (detailResult is ApiResult.Success) {
                                    _navigationEvent.postValue(
                                        NavigationEvent.Directions(
                                            StagingPart2PagerFragmentDirections.actionStagingPart2FragmentToDestageOrderFragment(
                                                SelectedActivities(arrayListOf(detailResult.data))
                                            )
                                        )
                                    )
                                } else {
                                    _navigationEvent.postValue(
                                        NavigationEvent.Directions(
                                            StagingPart2PagerFragmentDirections.actionStagingPart2FragmentToDestageOrderFragment(
                                                SelectedActivities(assignResult.data)
                                            )
                                        )
                                    )
                                }
                            } else {
                                (assignResult as? ApiResult.Failure)?.let { handleApiError(it) }
                                acuPickLogger.d("[skipToDestaging] error assigning order")
                            }
                        } else {
                            acuPickLogger.d("[skipToDestaging] no matching order to handoff")
                        }
                    } else {
                        (searchOrderResult as? ApiResult.Failure)?.let { handleApiError(it) }
                        acuPickLogger.d("[skipToDestaging] error searching for orders")
                    }
                } else {
                    acuPickLogger.d("[skipToDestaging] error sending scanned bags or completing staging activity")
                }
            }
        }
    }

    private fun showWarningIfDriverArrived(erId: Long, orderType: OrderType) {
        viewModelScope.launch {
            when (
                val result = isBlockingUi.wrap {
                    apsRepo.fetchOrderStatus(FetchOrderStatusRequestDto(erIds = listOf(erId)))
                }
            ) {
                is ApiResult.Success -> {
                    val firstArrivedCustomer = result.data.firstOrNull { it.subStatus == CustomerArrivalStatus.ARRIVED }
                    if (firstArrivedCustomer != null) {
                        showDriverWarning(orderType)
                    }
                }
                is ApiResult.Failure -> handleApiError(result)
            }
        }
    }

    private suspend fun getShopFloorLockedStagingLocation(): String? {
        // If site is tempZoneEnabled or if the current order is not MultiSource, return null
        if (siteRepo.stagingType == StagingType.TEMP_ZONE || !isCurrentOrderMultiSource.value) return null

        // If the shopFloorLockedStagingLocation is already set, return it
        val stagingPart2UiData = activityMap.value[currentOrderNumber]
        val shopFloorLockedStagingLocation = shopFloorLockedStagingLocationList.getCurrentOrdersShopFloorLockedStagingLocationDto(stagingPart2UiData)?.location
        if (shopFloorLockedStagingLocation.isNotNullOrEmpty()) return shopFloorLockedStagingLocation

        if (networkAvailabilityManager.isConnected.first().not()) {
            showReloadDialog()
        } else {
            val erId = stagingPart2UiData?.erId
            when (erId) {
                null -> {
                    shopFloorLockedStagingLocationList = null
                }
                else -> {
                    val result = isBlockingUi.wrap {
                        apsRepo.getCustomerOrderStagingLocation((erIdList))
                    }
                    shopFloorLockedStagingLocationList = when (result) {
                        is ApiResult.Success -> {
                            result.data.toMutableList()
                        }
                        is ApiResult.Failure -> {
                            null
                        }
                    }
                }
            }
        }
        return shopFloorLockedStagingLocationList.getCurrentOrdersShopFloorLockedStagingLocationDto(stagingPart2UiData)?.location
    }

    private suspend fun isShopFloorStagingLocationAccepted(toteCustomerOrderNumber: String, zone: String): Boolean {
        var canContinueWithScan = true
        if (networkAvailabilityManager.isConnected.first().not()) {
            showReloadDialog()
        } else {
            if (doesCurrentOrderAlreadyHaveAStagingLocationSet() && !isMultiLocationEnabled) return true
            val erIdForScannedTote: Long? = activityMap.value[toteCustomerOrderNumber]?.erId
            when (
                val result = isBlockingUi.wrap {
                    apsRepo.setCustomerOrderStagingLocation(CustomerOrderStagingLocationDto(erIdForScannedTote ?: 0L, zone))
                }
            ) {
                is ApiResult.Success -> {
                    val itemInList = shopFloorLockedStagingLocationList?.find {
                        it.erId == erIdForScannedTote
                    }
                    shopFloorLockedStagingLocationList?.remove(itemInList)
                    shopFloorLockedStagingLocationList?.add(result.data)
                    canContinueWithScan = true
                }

                is ApiResult.Failure -> {
                    if (result is ApiResult.Failure.Server) {
                        val type = result.error?.errorCode?.resolvedType
                        when (type) {
                            ServerErrorCode.STAGING_LOCATION_ALREADY_EXISTS -> {
                                activeScanTarget.postValue(ScanTarget.Zone)
                                delay(500)
                                canContinueWithScan = false
                                refreshPersistentPrompt()
                            }
                            ServerErrorCode.STAGING_LOCATION_MAX_REACHED -> {
                                activeScanTarget.postValue(ScanTarget.Zone)
                                delay(500)
                                canContinueWithScan = false
                                refreshPersistentPrompt()
                                shopFloorLockedStagingLocationList.getCurrentOrdersShopFloorLockedStagingLocationDto(activityMap.value[currentOrderNumber])?.location.let { lockedStagingLocation ->
                                    inlineDialogEvent.postValue(
                                        CustomDialogArgDataAndTag(
                                            data = CustomDialogArgData(
                                                title = StringIdHelper.Id(R.string.max_locations_reached_title),
                                                body = StringIdHelper.Format(R.string.max_staging_location, lockedStagingLocation?.takeLast(LOCATION_CHAR_LIMIT_MFC).orEmpty()),
                                                positiveButtonText = StringIdHelper.Id(R.string.got_it_cta),
                                                negativeButtonText = null,
                                                cancelOnTouchOutside = false
                                            ),
                                            tag = STAGING_NEW_LOCATION_DIALOG_TAG
                                        )
                                    )
                                }
                            }
                            else -> {
                                shopFloorLockedStagingLocationList?.add(CustomerOrderStagingLocationDto(erId = erIdForScannedTote, stagingLocation = zone))
                                canContinueWithScan = true
                            }
                        }
                    }
                }
            }
        }
        return canContinueWithScan
    }

    /** Given the API result from an API call,
     *  show the "Pick List Claimed" error diallog if error code is [STAGING_TAKEN_OVER_ERROR_CODE],
     *  or show the generic "Server Error" error dialog otherwise */
    private fun handleApiError(errorType: ApiResult.Failure) {
        val type = (errorType as? ApiResult.Failure.Server)?.error?.errorCode?.resolvedType
        val errorCode = (errorType as? ApiResult.Failure.Server)?.error?.errorCode?.rawValue
        val errorMessage = (errorType as? ApiResult.Failure.Server)?.error?.message
        viewModelScope.launch {
            acuPickLogger.e(
                "API error StagingPartPagerViewModel stagingActivityId= ${stagingActivityId.first()} " +
                    "on time $stagingCompleteTime, error message $errorMessage, error code $errorCode"
            )
            when {
                type?.cannotAssignToOrder() == true -> {
                    val tabCount = tabs.value?.size ?: 1
                    val serverErrorType = if (type == ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY) CannotAssignToOrderDialogTypes.STAGING else CannotAssignToOrderDialogTypes.REGULAR
                    serverErrorCannotAssignUser(serverErrorType, tabCount > 1)
                }
                type == ServerErrorCode.SCAN_CONTAINER_ISSUE -> {
                    showScanContainerErrorDialog()
                }
                errorCode == STAGING_TAKEN_OVER_ERROR_CODE -> {
                    stagingStateRepo.clear()
                    showStagingTakenOverDialog(context.getString(R.string.staging_taken_over_no_user_name))
                }
                isOrderComplete() -> showErrorDialog()
                else -> showReloadDialog()
            }
        }
    }

    fun handlePalletApiError(errorType: ApiResult.Failure, tag: String = "", retryAction: (() -> Unit)? = null) {
        retryActionEvent.postValue(retryAction)
        apiErrorEvent.postValue(Pair(tag, errorType))
    }

    // /////////////////////////////////////////////////////////////////////////
    // Navigation
    // /////////////////////////////////////////////////////////////////////////

    private fun showStagingCompletedView() {
        isStagingCompleted.postValue(true)
        showAnimationBackground.postValue(true)
        loadStagingSummaryData()
    }

    fun navigateHome() {
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                StagingPart2PagerFragmentDirections.actionStagingPart2FragmentToHomeFragment()
            )
        )
    }

    // /////////////////////////////////////////////////////////////////////////
    // Dialogs
    // /////////////////////////////////////////////////////////////////////////
    private fun showScanContainerErrorDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.staging_scan_error),
                    body = StringIdHelper.Id(R.string.staging_scan_error_body),
                    positiveButtonText = StringIdHelper.Id(R.string.scan_again),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel_cta),
                    cancelOnTouchOutside = true
                ),
                tag = STAGING_SCAN_CONTAINER_ERROR_DIALOG_TAG
            )
        )
    }

    private fun showErrorDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.staging_server_error),
                    body = StringIdHelper.Id(R.string.staging_server_body_error),
                    positiveButtonText = StringIdHelper.Id(R.string.try_again),
                    negativeButtonText = StringIdHelper.Id(R.string.ok),
                    cancelOnTouchOutside = true
                ),
                tag = STAGING_ORDER_RELOAD_DIALOG_TAG
            )
        )
    }

    private fun showStagingTakenOverDialog(otherUser: String) =
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    title = StringIdHelper.Id(R.string.staging_order_taken_dialog_title),
                    body = StringIdHelper.Format(R.string.staging_order_taken_dialog_body, otherUser),
                    positiveButtonText = StringIdHelper.Id(R.string.ok),
                    cancelOnTouchOutside = false
                ),
                tag = StagingPagerViewModel.STAGING_ORDER_TAKEN_DIALOG_TAG
            )
        )

    private fun showInternetIssueDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    title = StringIdHelper.Id(R.string.wifi_error_title),
                    body = StringIdHelper.Id(R.string.wifi_error_body),
                    positiveButtonText = StringIdHelper.Id(R.string.refresh),
                    cancelOnTouchOutside = false
                ),
                tag = STAGING_WIFI_ISSUE_DIALOG_TAG
            )
        )
    }

    private fun showReloadDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    title = R.string.something_went_wrong.toIdHelper(),
                    body = R.string.staging_something_went_wrong.toIdHelper(),
                    positiveButtonText = R.string.try_again.toIdHelper(),
                    negativeButtonText = R.string.staging_exit.toIdHelper(),
                    cancelOnTouchOutside = false
                ),
                tag = STAGING_LOAD_DATA_RETRY_DIALOG_TAG
            )
        )
    }

    private fun showDriverWarning(orderType: OrderType) {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.ModalFiveConfirmation,
                    titleIcon = R.drawable.ic_flash_driver_arrived,
                    title = R.string.flash_order_driver_arrived_notification_title.toIdHelper(),
                    body = if (orderType == OrderType.FLASH) R.string.flash_order_driver_arrived_notification_body.toIdHelper() else
                        R.string.partnerpick_order_driver_arrived_notification_body.toIdHelper(),
                    positiveButtonText = R.string.confirm.toIdHelper(),
                    negativeButtonText = R.string.cancel_cta.toIdHelper(),
                    cancelOnTouchOutside = false,
                    cancelable = false
                ),
                tag = STAGING_DRIVER_WARNING_DIALOG_TAG
            )
        )
    }

    private fun checkUserStillAssigned(result: ApiResult.Success<ActivityDto>) {
        if (result.data.assignedTo?.userId != userRepo.user.value?.userId) {
            stagingStateRepo.clear()
            showStagingTakenOverDialog(result.data.assignedTo?.firstInitialDotLastName() ?: context.getString(R.string.staging_taken_over_no_user_name))
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Prompt/Snackbars
    // /////////////////////////////////////////////////////////////////////////
    private fun refreshPersistentPrompt() {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (!isOrderComplete()) {
                when (activeScanTarget.value) {
                    ScanTarget.Zone -> {
                        val shopFloorLockedStagingLocation = getShopFloorLockedStagingLocation()
                        // Due to a multi-threading issue, check isOrderComplete() again before displaying the zone prompt
                        if (!isOrderComplete()) {
                            zoneStaticPrompt(shopFloorLockedStagingLocation)
                        }
                    }
                    // Scan a bag into Zone XXXXX
                    ScanTarget.Bag -> {
                        snackbarEvent.postValue(
                            dispatchSnackbarEvent(
                                StringIdHelper.Id(R.string.prompt_please_scan_bag_or_location)
                            )
                        )
                    }
                    // Scan a new bag or zone
                    ScanTarget.BagOrZone -> {
                        snackbarEvent.postValue(dispatchSnackbarEvent(StringIdHelper.Id(R.string.prompt_please_scan_bag_or_location)))
                    }
                    ScanTarget.Tote -> {
                        if (isCurrentOrderMultiSource.value && siteRepo.stagingType == StagingType.SHOP_FLOOR)
                            snackbarEvent.postValue(
                                dispatchSnackbarEvent(StringIdHelper.Format(R.string.prompt_scan_a_tote_in_location, currentZoneBarcode.takeLast(LOCATION_CHAR_LIMIT_MFC)))
                            )
                        else if (isCurrentOrderMultiSource.value && siteRepo.isDarkStoreEnabled)
                            snackbarEvent.postValue(dispatchSnackbarEvent(StringIdHelper.Id(R.string.prompt_please_scan_tote)))
                        else
                            snackbarEvent.postValue(dispatchSnackbarEvent(StringIdHelper.Id(R.string.prompt_scan_tote)))
                    }
                    ScanTarget.ToteOrZone -> {
                        if (isCurrentOrderMultiSource.value && siteRepo.stagingType == StagingType.SHOP_FLOOR)
                            snackbarEvent.postValue(dispatchSnackbarEvent(StringIdHelper.Id(R.string.prompt_scan_a_location_or_tote)))
                        else
                            snackbarEvent.postValue(dispatchSnackbarEvent(StringIdHelper.Id(R.string.prompt_scan_tote)))
                    }
                    else -> {
                        // no action
                    }
                }
            }
        }
    }

    private fun zoneStaticPrompt(stagingLocation: String?) {
        this.snackbarEvent.postValue(
            // If the site has isTempZoneEnabled as false AND already has a set Shop Floor stagingLocation, show that location
            if (stagingLocation.isNotNullOrBlank()) {
                dispatchSnackbarEvent(StringIdHelper.Format(R.string.prompt_scan_specific_location_to_begin_format, stagingLocation?.takeLast(LOCATION_CHAR_LIMIT_MFC).orEmpty()))
            } else if (isCurrentOrderMultiSource.value && siteRepo.stagingType == StagingType.SHOP_FLOOR && activeScanTarget.value == ScanTarget.Zone) {
                dispatchSnackbarEvent(StringIdHelper.Id(R.string.prompt_scan_a_location))
            } else if (isCurrentOrderMultiSource.value && siteRepo.stagingType == StagingType.SHOP_FLOOR && activeScanTarget.value == ScanTarget.ToteOrZone) {
                dispatchSnackbarEvent(StringIdHelper.Id(R.string.prompt_scan_a_location_or_tote))
            } else if (isCurrentOrderMultiSource.value && siteRepo.isDarkStoreEnabled && activeScanTarget.value == ScanTarget.Zone) {
                dispatchSnackbarEvent(StringIdHelper.Id(R.string.prompt_please_scan_pallet))
            } else {
                val scanPrompt = getZoneLocationScanPrompt(currentOrderNumber)
                dispatchSnackbarEvent(StringIdHelper.Id(scanPrompt))
            }
        )
    }

    // Determines the appropriate prompt message based on whether new or existing scanned locations by order
    private fun getZoneLocationScanPrompt(customerOrderNumber: String): Int {
        return when {
            hasNewScannedLocationsByOrder(customerOrderNumber) -> R.string.prompt_scan_a_location
            hasExistingScannedLocationByOrder(customerOrderNumber) -> R.string.prompt_scan_a_existing_location
            else -> R.string.prompt_scan_a_location
        }
    }

    // Checks if existing locations are present for specific order
    private fun hasExistingScannedLocationByOrder(customerOrderNumber: String): Boolean {
        val existingLocations = getFlattenedLocationsByOrder(existingScannedZoneLocations.value, customerOrderNumber)
        return existingLocations.isNotEmpty()
    }

    // Checks if there are any new locations for a specific order compared to existing scanned locations
    private fun hasNewScannedLocationsByOrder(customerOrderNumber: String): Boolean {
        val mergedLocations = getFlattenedLocationsByOrder(mergedZoneLocations.value, customerOrderNumber)
        val existingLocations = getFlattenedLocationsByOrder(existingScannedZoneLocations.value, customerOrderNumber)
        return mergedLocations.any { it !in existingLocations }
    }

    // Helper function to retrieve and flatten locations for a specific order
    private fun getFlattenedLocationsByOrder(
        zoneLocations: List<ZoneLocationScanUI>,
        customerOrderNumber: String,
    ): List<String> {
        return zoneLocations
            .find { it.customerOrderNumber == customerOrderNumber }
            ?.storageTypes.orEmpty()
            .flatMap { it.locations }
    }

    private fun dispatchSnackbarEvent(stringIdHelper: StringIdHelper) =
        SnackBarEvent<String>(prompt = stringIdHelper, action = { onManualEntryClicked() })

    companion object {
        private const val STAGING_ANIMATION_VISIBLE_DURATION_MS = 2000L
        private const val STAGING_ANIMATION_DELAY_MS = 250L
        private const val STAGING_WIFI_ISSUE_DIALOG_TAG = "stagingWifiIssueDialogTag"
        private const val STAGING_LOAD_DATA_RETRY_DIALOG_TAG = "stagingLoadDataRetryDialogTag"
        private const val STAGING_ORDER_RELOAD_DIALOG_TAG = "stagingOrderReloadDialogTag"
        private const val STAGING_SCAN_CONTAINER_ERROR_DIALOG_TAG = "stagingScanContainerErrorDialogTag"
        const val STAGING_DRIVER_WARNING_DIALOG_TAG = "stagingDriverWarningDialogTag"
        const val STAGING_NEW_LOCATION_DIALOG_TAG = "stagingNewLocationDialogTag"
        const val DIRECTION_STAGING_NEW_LOCATION_DIALOG_TAG = "directionStagingNewLocationDialogTag"
        const val STAGING_ACTION_SHEET_DIALOG_TAG = "StagingActionSheetDialogTag"
        private const val STAGING_TAKEN_OVER_ERROR_CODE = 57

        // When skipping staging part 2 (on a flash order), bags not already scanned into zones are given a dummy zone location
        // in the format of "xxDUMMY" where xx represents the temperature zone ("AM", "CH", "FZ", "HT").
        // For example: "AMDUMMY", "CHDUMMY", "FZDUMMY", "HTDUMMY"
        private const val DUMMY_ZONE_FORMAT = "%sDUMMY"
        const val BAGS_ID_LENGTH = 2
        const val MAX_MFC_TOTE_ID_LENGTH = 8
        const val MFC_TOTE_ID_UI_LENGTH = 5
        const val NON_MFC_TOTE_ID_UI_LENGTH = 5
        const val SHOP_FLOOR_ZONE_MIN_LENGTH = 4
        const val LOCATION_CHAR_LIMIT_MFC = 3

        private val customerBagPreferredStagingOptions = listOf(
            ActionSheetOptions(R.drawable.ic_pick_list_info, R.string.staging_picklist_summary),
            ActionSheetOptions(R.drawable.ic_tote_full, R.string.staging_unassign_tote),
            ActionSheetOptions(R.drawable.ic_staging_print, R.string.staging_reprint_tote_labels),
            ActionSheetOptions(R.drawable.ic_edit, R.string.staging_loose_item_label),
        )

        private val stagingOptions = listOf(
            ActionSheetOptions(R.drawable.ic_pick_list_info, R.string.staging_picklist_summary),
            ActionSheetOptions(R.drawable.ic_tote_full, R.string.staging_unassign_tote),
            ActionSheetOptions(R.drawable.ic_staging_print, R.string.staging_reprint_bag_labels),
            ActionSheetOptions(R.drawable.ic_edit, R.string.staging_add_bag_label)
        )
    }
}

fun List<CustomerOrderStagingLocationDto>?.getCurrentOrdersShopFloorLockedStagingLocationDto(stagingPart2UiData: StagingPart2UiData?) =
    this?.find { customerOrderStagingLocationDto ->
        customerOrderStagingLocationDto.erId == stagingPart2UiData?.erId
    }
