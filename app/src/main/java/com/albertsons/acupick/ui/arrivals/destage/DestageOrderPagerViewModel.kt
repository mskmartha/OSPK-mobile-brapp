package com.albertsons.acupick.ui.arrivals.destage

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.FirebaseAnalyticsInterface
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.R
import com.albertsons.acupick.data.duginterjection.DugInterjectionState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.activityHasRegulatedItems
import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.CancelReasonCode
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.CustomerArrivalStatus
import com.albertsons.acupick.data.model.ImageSizePreset
import com.albertsons.acupick.data.model.IssuesScanningBag
import com.albertsons.acupick.data.model.OrderIssueReasonCode
import com.albertsons.acupick.data.model.RejectedItemsByStorageType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.barcode.StagingContainer
import com.albertsons.acupick.data.model.request.AssignUserWrapperRequestDto
import com.albertsons.acupick.data.model.request.CancelHandoffRequestDto
import com.albertsons.acupick.data.model.request.ConfirmRxPickupRequestDto
import com.albertsons.acupick.data.model.request.RemoveItemsRequestDto
import com.albertsons.acupick.data.model.request.RxOrder
import com.albertsons.acupick.data.model.request.RxOrderStatus
import com.albertsons.acupick.data.model.request.ScanContainerRequestDto
import com.albertsons.acupick.data.model.request.ScanContainerWrapperRequestDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.CannotAssignToOrderDialogTypes
import com.albertsons.acupick.data.model.response.ScanContDto
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.asFirstInitialDotLastString
import com.albertsons.acupick.data.model.response.bagAndLooseItemTotal
import com.albertsons.acupick.data.model.response.fullContactName
import com.albertsons.acupick.data.model.response.getFulfillmentTypeDescriptions
import com.albertsons.acupick.data.model.response.getRejectedItemsByZone
import com.albertsons.acupick.data.model.response.hasAddOnPrescription
import com.albertsons.acupick.data.model.response.hasPharmacyServicingOrdersAndStaged
import com.albertsons.acupick.data.model.response.isReshop
import com.albertsons.acupick.data.model.response.isRxDug
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.PushNotificationsRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.image.ImagePreCacher
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.infrastructure.utils.logError
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.arrivals.complete.BagsPerTempZoneData
import com.albertsons.acupick.ui.arrivals.complete.BagsPerTempZoneParams
import com.albertsons.acupick.ui.arrivals.complete.HandOffArgData
import com.albertsons.acupick.ui.arrivals.complete.HandOffUI
import com.albertsons.acupick.ui.arrivals.destage.reportmissingbag.ReportMissingBagsParams
import com.albertsons.acupick.ui.arrivals.destage.updatecustomers.MarkedArrivedUI
import com.albertsons.acupick.ui.arrivals.pharmacy.ManualEntryPharmacyData
import com.albertsons.acupick.ui.arrivals.pharmacy.PrescriptionReturnData
import com.albertsons.acupick.ui.bottomsheetdialog.ActionSheetDetails
import com.albertsons.acupick.ui.bottomsheetdialog.ActionSheetOptions
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetArgDataAndTag
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetType
import com.albertsons.acupick.ui.bottomsheetdialog.CustomBottomSheetArgData
import com.albertsons.acupick.ui.dialog.CloseAction
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DESTAGING_DIALOG_ARGS
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.dialog.HAND_OFF_ALREADY_ASSIGNED_ARG_DATA
import com.albertsons.acupick.ui.dialog.OF_AGE_ASSOCIATE_VERIFICATION_DATA
import com.albertsons.acupick.ui.dialog.ORDER_DETAILS_CANCEL_ARG_DATA
import com.albertsons.acupick.ui.dialog.ORDER_ISSUE_SCAN_BAGS_ARG_DATA
import com.albertsons.acupick.ui.dialog.ORDER_ISSUE_SCAN_TOTES_ARG_DATA
import com.albertsons.acupick.ui.dialog.PHARMACY_STAFF_REQUIRED_DIALOG
import com.albertsons.acupick.ui.dialog.RX_ORDER_DETAILS_CANCEL_DESTAGE_ARG_DATA
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.dialog.getConfirmAllBagsRemovedDialog
import com.albertsons.acupick.ui.dialog.getDestageOrderHotReminderArgData
import com.albertsons.acupick.ui.dialog.getHandOffAlreadyAssignedWithOrderNumberDialog
import com.albertsons.acupick.ui.dialog.getHandOffBatchAlreadyAssignedWithOrderNumbersDialog
import com.albertsons.acupick.ui.dialog.getNoBagsWithCustomerNameDialog
import com.albertsons.acupick.ui.dialog.getPrescriptionAddOn
import com.albertsons.acupick.ui.dialog.getRejectedItemDialog
import com.albertsons.acupick.ui.manualentry.ManualEntryHandoffParams
import com.albertsons.acupick.ui.manualentry.ManualEntryPharmacyParams
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryHandOffBag
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.models.BagLabel
import com.albertsons.acupick.ui.models.CustomerData
import com.albertsons.acupick.ui.models.DestageOrderTabUI
import com.albertsons.acupick.ui.models.DestageOrderUiData
import com.albertsons.acupick.ui.models.RemoveRejectedItemUiData
import com.albertsons.acupick.ui.models.RxBagUI
import com.albertsons.acupick.ui.models.SnackBarEvent
import com.albertsons.acupick.ui.models.ZonedBagsScannedData
import com.albertsons.acupick.ui.models.takeOrder
import com.albertsons.acupick.ui.picklistitems.ScanTarget
import com.albertsons.acupick.ui.util.DrawableIdHelper
import com.albertsons.acupick.ui.util.SnackAction
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.UserFeedback
import com.albertsons.acupick.ui.util.displayName
import com.albertsons.acupick.ui.util.getSizedImageUrl
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.orTrue
import com.albertsons.acupick.ui.util.triple
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import timber.log.Timber
import java.io.Serializable
import java.time.ZonedDateTime

const val MAX_HANDOFF_COUNT = 3

// if batch handoff is needed from interjection flow then increase the count to 2
// currently no batch handoff in interjection flow
const val MAX_INTERJECTION_HANDOFF_COUNT = 1
const val DUMMY_STAGING_LOCATION_SUFFIX = "DUMMY"

class DestageOrderPagerViewModel(
    val app: Application,
) : BaseViewModel(app) {

    // DI
    private val dispatcherProvider: DispatcherProvider by inject()
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    private val userFeedback: UserFeedback by inject()
    private val apsRepo: ApsRepository by inject()
    private val userRepo: UserRepository by inject()
    private val siteRepo: SiteRepository by inject()
    private val pushNotificationsRepository: PushNotificationsRepository by inject()
    private val context: Context by inject()
    private val imagePreCacher: ImagePreCacher by inject()
    val firebaseAnalytics: FirebaseAnalyticsInterface by inject()

    // UI
    // Zone & bag scanned data for all orders - observed by child fragments (replay is needed so the first event isn't lost)
    val zonedBagUiData = MutableSharedFlow<List<ZonedBagsScannedData>>(replay = 1)

    // Visibility of manual entry FAB
    val isFabVisible: LiveData<Boolean> = MutableLiveData(false)

    val isFromNotification = MutableLiveData(false)

    // Complete scanned bag count equals original bag count
    val isComplete = MutableLiveData<Boolean>()
    val isRxComplete = MutableLiveData(false)
    val isLoading = MutableLiveData<Boolean>()
    private val _showPerscriptionPickup = MutableStateFlow(false)
    val showPerscriptionPickup: LiveData<Boolean> = _showPerscriptionPickup.asLiveData()
    val arrivalLabel = MutableLiveData<BarcodeType.PharmacyArrivalLabel?>(null)
    private val incomingScannedRxBags = ArrayList<RxBagUI>()
    val incomingScannedRxBagsSharedFlow = MutableSharedFlow<MutableList<RxBagUI>>(replay = 1)
    val apiRxBagList: MutableLiveData<List<RxBagUI>> = MutableLiveData(mutableListOf())

    private val activeScanTarget: LiveData<ScanTarget> = MutableLiveData(ScanTarget.PharmacyArrivalLabel)
    val snackbarEvent = MutableLiveData<SnackBarEvent<String>?>()

    // isRxComplete check is added to enable the confirm button when all the bags are scanned
    // this change is done as part of Rx Redesign ACIP-318564 as we are using a bottom sheet instead of a fragment
    val isConfirmButtonEnabled = combine(
        isDisplayingSnackbar.asFlow(),
        isComplete.asFlow(),
        showPerscriptionPickup.asFlow(),
        isRxComplete.asFlow()
    ) { isDisplayingSnackbar, isComplete, showPerscriptionPickup, isRxComplete ->
        // commented incomingScannedRxBags instead using the isRxComplete to determine the confirmation prescription pick
        !isDisplayingSnackbar && isComplete && (!showPerscriptionPickup /*|| incomingScannedRxBags.isNotNullOrEmpty().orFalse()*/ || isRxComplete)
    }.asLiveData()

    val hideStaticPrompt =
        combine(isDisplayingSnackbar.asFlow(), isComplete.asFlow(), showPerscriptionPickup.asFlow(), isRxComplete.asFlow()) { isDisplayingSnackbar, isComplete, showPerscriptionPickup, rxComplete ->
            // The static prompt needs to hide when a snack bar is showing or when isComplete
            isDisplayingSnackbar || (showPerscriptionPickup && rxComplete) || (isComplete && !showPerscriptionPickup) || apiRxBagList.value?.isNotNullOrEmpty().orFalse() && apiRxBagList.value?.size ==
                incomingScannedRxBags.size
        }.asLiveData()
    private val shownStaffRequiredModal = MutableLiveData(false)
    val launchStaffMemberRequiredModal = hideStaticPrompt.triple(arrivalLabel, shownStaffRequiredModal) { hideStaticPrompt, label, shownModal ->
        !hideStaticPrompt.orFalse() && label != null && !shownModal.orFalse()
    }
    val itemRemovalRequired = MutableLiveData<Boolean>()
    val itemRemovalEvent = MutableLiveData(snackbarRemoveItemsEvent())
    val markedArrivedSnackBarEvent = LiveEvent<String?>()

    // Since we are using the bottomsheet for manual entry for knowing the storage type we send this information to the DestagingorderPagerFragment
    val storageTypeEvent: MutableLiveData<CustomDialogArgDataAndTag> = LiveEvent()
    val lockTab = MutableLiveData(false)
    var isManualEntryBottomSheetOpen = false

    // State/Data
    val resultsUiList: LiveData<List<DestageOrderUiData>> = MutableLiveData(mutableListOf())
    private fun isCustomerSubsEnabled() = siteRepo.siteDetails.value?.isCustomerApprovedSubstitutionEnabled.orFalse()
    fun getActivityByOrder(orderNumber: String): LiveData<DestageOrderUiData?> = resultsUiList.map { it.find { order -> order.customerOrderNumber == orderNumber } }
    private val activityDtoArray = arrayListOf<ActivityDto>()
    private val zonedBagsScannedData = ArrayList<ZonedBagsScannedData>()
    private val bagLabelSourceOfTruth = arrayListOf<BagLabel>()
    private val forceScanList = arrayListOf<ZonedBagsScannedData>()
    private val forceScanTempList = arrayListOf<ZonedBagsScannedData>()
    private val orderIssueReasonMap = mutableMapOf<String, MutableList<IssuesScanningBag>>()
    private var orderIssueReasonCodeTemp: OrderIssueReasonCode? = null
    private var hasDugOrder: LiveData<Boolean> = MutableLiveData(false)
    private var hasRejectedItemDialogShown: Boolean = false // Do not re-open the confirmation dialog if its already in visible state. Defect: ACIP-104310
    var currentOrderNumber = ""
    val activeOrderNumber = MutableLiveData<String>()
    private val currentOrderCustomerData = MutableLiveData<CustomerData?>()
    private fun getCustomerInfo(activityDto: ActivityDto) = currentOrderCustomerData.value?.customerInfoList?.firstOrNull { it.erid == activityDto.erId }
    private val currentActNo = MutableLiveData("")
    var isCurrentOrderMfc = false
    var isCurrentOrderHasCustomerBagPreference = true
    var isCurrentOrderMultiSource = false
    var destageOrderUiData: DestageOrderUiData? = null
    private var rejectedItemZone: StorageType? = null
    var currentBagLabel = MutableLiveData<BagLabel>()
    val currentOrderHasLoosItem = MutableStateFlow(false)
    fun setCurrentActNo(actNo: String?) = currentActNo.set(actNo)
    fun setActiveOderNumber(orderNumber: String?) {
        activeOrderNumber.value = orderNumber
    }

    private val rejectedItems = MutableLiveData<List<RejectedItemsByStorageType>>()
    val completedRejectedItems: MutableLiveData<MutableList<RejectedItemsByStorageType>> = MutableLiveData(mutableListOf())
    private val removeItemsRequests = mutableListOf<RemoveItemsRequestDto>()
    private val orderIssueReasonCode = OrderIssueReasonCode.values()

    // Page Events
    val pageEvent = MutableStateFlow(0)
    val previousPageEvent = MutableStateFlow(0)
    val orderCompletionUpdateEvent = MutableSharedFlow<OrderCompletionState>()
    val selectedStorageType = MutableSharedFlow<Int?>()

    // Events
    val orderIssuesButtonEvent = LiveEvent<Unit>()
    val clearOrderIssue = LiveEvent<Unit>()

    // timestamps
    private val groceryDestageStartTimestamp: ZonedDateTime? = ZonedDateTime.now()
    private var groceryDestageCompleteTimestamp: ZonedDateTime? = null
    private var rxLocationScanTimestamp: ZonedDateTime? = null
    var rxDeliveryFailureReason = MutableLiveData<String?>()
    val noBagsMap = mutableMapOf<String, Boolean>()
    val orderNumberAndLastScanTime = mutableListOf<Pair<String, ZonedDateTime>>()

    // Dialog Tags
    private val RX_PHARMACY_STAFF_REQUIRED_TAG = "rxPharmacyStaffRequiredTag${this.hashCode()}"
    private val RX_PRESCRIPTION_ADD_ON_TAG = "rxPharmacyAddOn${this.hashCode()}"

    // Tab UI
    val tabs: LiveData<List<DestageOrderTabUI>> = resultsUiList.map {
        it.map { uiData ->
            DestageOrderTabUI(
                tabLabel = uiData.customerName ?: "",
                tabArgument = DestageOrderPagerFragmentArgs(
                    activityList = null,
                    orderNumber = uiData.customerOrderNumber.orEmpty()
                )
            )
        }
    }
    val isCompleteList: LiveData<List<OrderCompletionState>> = tabs.map {
        it.map { tab ->
            OrderCompletionState(tab.tabArgument?.orderNumber.orEmpty())
        }
    }

    private val failedActivityDtoList = mutableListOf<ActivityDto>()
    val bagBypass = MutableLiveData<MutableList<ZonedBagsScannedData>>()
    private val latestVehicleInfo = hashMapOf<Long?, ScanContDto?>()
    val staticPrompt = MutableLiveData<StringIdHelper?>()
    private val timerjobs = mutableListOf<Job>()

    val lastOrderNumber = tabs.map { it.lastOrNull()?.tabArgument?.orderNumber }
    private val shownGiftDialogData = mutableMapOf<String, Boolean>()
    private val cofirmGiftDialogData = mutableMapOf<String, Boolean>()
    private var fromPartialPrescriptionPickup = false

    val currentOrderZonedBagList get() = zonedBagsScannedData.takeCurrentOrder()

    init {
        viewModelScope.launch {
            currentActNo.asFlow().distinctUntilChanged().collect { actNo ->
                val hasAddOn = activityDtoArray.firstOrNull {
                    it.activityNo == actNo.orEmpty()
                }?.hasAddOnPrescription().orFalse()
                if (hasAddOn) showPrescriptionAddOnDialog()
                else if (actNo.isNotNullOrEmpty()) handleGiftDialog()
            }
        }

        // Connect to event.
        viewModelScope.launch {
            currentActNo.asFlow().collect { currentActNo ->
                val rejects = activityDtoArray.firstOrNull {
                    it.activityNo == currentActNo.orEmpty()
                }?.getRejectedItemsByZone()?.reversed()
                rejectedItems.set(rejects)
            }
        }

        //  Setup toolbar image
        viewModelScope.launch {
            siteRepo.siteDetails.collect { siteDetails ->
                changeToolbarRightSecondExtraImageEvent.postValue(
                    DrawableIdHelper.Id(
                        if (siteDetails?.isMultipleHandoffAllowed == false) 0 else R.drawable.ic_addcustomer
                    )
                )
            }
        }

        registerCloseAction(RX_PHARMACY_STAFF_REQUIRED_TAG) {
            closeActionFactory(positive = {
                shownStaffRequiredModal.postValue(value = true)
            })
        }

        registerCloseAction(CONFIRM_REMOVAL_OF_ITEMS_DIALOG) {
            closeActionFactory(
                positive = { onCompleteDestaging() }
            )
        }

        registerCloseAction(GIFTING_DIALOG_TAG) {
            closeActionFactory(positive = {
                printGiftLabel()
            })
        }
        registerCloseAction(GIFTING_CONFIRMATION_DIALOG_TAG) {
            closeActionFactory(positive = {
                cofirmGiftDialogData.putAll(resultsUiList.value?.filter { it.isGift }?.map { it.customerOrderNumber.orEmpty() to true }?.toMap().orEmpty())
                onConfirmCtaClick()
            })
        }

        changeToolbarRightFirstExtraImageEvent.postValue(
            DrawableIdHelper.Id(R.drawable.ic_markarrived)
        )

        viewModelScope.launch(dispatcherProvider.IO) {
            toolbarRightSecondImageEvent.asFlow().collect {
                val count = tabs.value?.size ?: 1
                if (count >= MAX_HANDOFF_COUNT) {
                    showMaxAssignedDialog()
                } else {
                    navigateToUpdateCustomerAddFragment(count)
                }
            }
        }

        viewModelScope.launch(dispatcherProvider.IO) {
            toolbarRightFirstImageEvent.asFlow().collect {
                navigateToUpdateCustomerArrivedFragment()
            }
        }

        viewModelScope.launch(dispatcherProvider.IO) {
            triggerHomeButtonEvent.asFlow().collect {
                handleExitButton()
            }
        }

        viewModelScope.launch {
            orderCompletionUpdateEvent.collect { isCompleteUpdateEvent ->
                isCompleteList.set(
                    isCompleteList.value
                        ?.dropWhile { it.customerOrderNumber == isCompleteUpdateEvent.customerOrderNumber }
                        ?.toMutableList()
                        ?.apply {
                            add(isCompleteUpdateEvent)
                        }
                )
            }
        }

        showIndefinitePrompt()
    }

    private fun getDestagingActionSheetArgsDataAndTagForBottomSheet(): BottomSheetArgDataAndTag {
        val options = listOf(
            ActionSheetOptions(R.drawable.ic_markarrived, R.string.marked_as_arrived),
            ActionSheetOptions(R.drawable.ic_addcustomer, R.string.add_customer),
        )
        return BottomSheetArgDataAndTag(
            data = CustomBottomSheetArgData(
                dialogType = BottomSheetType.ActionSheet,
                draggable = false,
                title = StringIdHelper.Raw(""),
                customDataParcel = ActionSheetDetails(options),
                peekHeight = R.dimen.actionsheet_peek_height
            ),
            tag = DESTAGING_ACTION_SHEET_DIALOG_TAG
        )
    }

    fun getUiDataForOrder(orderNumber: String) = runBlocking {
        resultsUiList.value?.find { it.detailsHeaderUi.customerOrderNumber == orderNumber }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Scanner logic
    // /////////////////////////////////////////////////////////////////////////
    /** Entry point for a barcode received from the scanner/DataWedge */
    fun onScannerBarcodeReceived(barcodeType: BarcodeType) {
        if (showPerscriptionPickup.value.orFalse()) {
            handlePerscriptionLabelScan(barcodeType)
            return
        }
        if (zonedBagsScannedData.all { it.currentBagsScanned >= 1 }) {
            showWrongScan(app.getString(R.string.error_all_bags_destaged))
        } else {
            viewModelScope.launch(dispatcherProvider.Default) {
                when (barcodeType) {
                    is StagingContainer -> {
                        handleScannedBagOrMfcTote(barcodeType, getValidBagFromScannedBagOrMfcTote(barcodeType = barcodeType))
                    }

                    else -> {
                        showWrongScan(
                            app.getString(
                                when {
                                    !isCurrentOrderHasCustomerBagPreference && currentOrderHasLoosItem.value -> R.string.scan_error_tote_loose_item
                                    !isCurrentOrderHasCustomerBagPreference || isCurrentOrderMfc -> R.string.scan_error_tote
                                    else -> R.string.scan_error_bag
                                }
                            )
                        )
                    }
                }.exhaustive
            }
        }
    }

    fun validateCurrentOrderHasLooseItem() {
        viewModelScope.launch {
            currentOrderHasLoosItem.emit(zonedBagsScannedData.takeCurrentOrder().any { it.bagData?.isLoose.orFalse() })
        }
    }

    fun handleManualEntryBag(handOffBag: ManualEntryHandOffBag) {
        handOffBag.bag?.let {
            onScannerBarcodeReceived(it)
        }
    }

    fun handleManualEntryPharmacy(manualEntryPharmacyData: ManualEntryPharmacyData) {
        manualEntryPharmacyData.stagingContainer?.let {
            onScannerBarcodeReceived(it)
        }
    }

    /**
     * START Bag and MfcTote Scan Handling
     */
    private fun getValidBagFromScannedBagOrMfcTote(barcodeType: StagingContainer): BagLabel? {
        return when (barcodeType) {
            is BarcodeType.Bag -> {
                getValidBagOrReshopMfcTote(bagId = barcodeType.bagOrToteId, customerOrderNumber = barcodeType.customerOrderNumber)
            }

            is BarcodeType.MfcTote -> {
                getValidMfcTote(toteId = barcodeType.bagOrToteId, customerOrderNumber = barcodeType.customerOrderNumber)
            }

            is BarcodeType.MfcReshopTote -> {
                getValidBagOrReshopMfcTote(bagId = barcodeType.bagOrToteId, customerOrderNumber = barcodeType.customerOrderNumber)
            }

            is BarcodeType.NonMfcTote -> {
                getValidNonMfcTote(toteId = barcodeType.bagOrToteId, customerOrderNumber = barcodeType.customerOrderNumber)
            }

            else -> null
        }
    }

    private fun handleScannedBagOrMfcTote(barcodeType: StagingContainer, validBag: BagLabel?) {
        // Ignore scan if item removal is required
        if (itemRemovalRequired.value.orFalse()) return

        val isAlreadyScanned = validBag != null && validBag.isScanned

        if (isBagOrMfcToteScanSuccess(validBag, isAlreadyScanned, barcodeType.bagOrToteId)) {
            // Keep the pager from moving if coming back from manual entry
            updateBagOrMfcToteOnScanSuccess(validBag, barcodeType)
            refreshBagScanSkippedData()
        } else {
            val customerOrderNumberFromBag = validBag?.customerOrderNumber
            if (isAlreadyScanned) {
                if (previousPageEvent.value != pageEvent.value) previousPageEvent.value = pageEvent.value
                // Check if on correct page and if not slide to it
                pageEvent.value = activityDtoArray.indexOf(activityDtoArray.find { it.customerOrderNumber == customerOrderNumberFromBag })
                showWrongScan(
                    when (barcodeType) {
                        is BarcodeType.MfcTote -> app.getString(R.string.error_tote_already_scanned_format, barcodeType.bagOrToteId.takeLast(MFC_TOTE_ID_LENGTH))
                        is BarcodeType.NonMfcTote -> app.getString(R.string.error_non_mfc_tote_already_scanned_format, barcodeType.bagOrToteId.takeLast(2))
                        else -> {
                            val messageId = if (validBag?.isCustomerBagPreference.orTrue())
                                R.string.error_bag_already_scanned_format
                            else R.string.error_loose_bag_already_scanned_format
                            app.getString(messageId, barcodeType.bagOrToteId.takeLast(2))
                        }
                    }
                )
            } else {
                pageEvent.value = activityDtoArray.indexOf(activityDtoArray.find { it.customerOrderNumber == customerOrderNumberFromBag })
                showWrongScan(
                    app.getString(
                        when (barcodeType) {
                            is BarcodeType.MfcTote, is BarcodeType.NonMfcTote -> R.string.error_tote_not_in_order
                            else -> {
                                if (!isCurrentOrderHasCustomerBagPreference && validBag == null) R.string.error_loose_item_not_in_order
                                else R.string.error_item_not_in_order
                            }
                        }
                    )
                )
            }
        }
    }

    // Reset the bag bypass list to reflect the updated number of bags skipped
    private fun refreshBagScanSkippedData() {
        forceScanTempList.clear()
        forceScanTempList.addAll(zonedBagsScannedData.takeCurrentOrder().filter { !it.isComplete() && it.bagData?.labelId == null })
        bagBypass.postValue(forceScanList)
    }

    private fun handlePerscriptionLabelScan(barcodeType: BarcodeType) {
        when (barcodeType) {
            is BarcodeType.PharmacyArrivalLabel -> {
                userFeedback.setSuccessScannedSoundAndHaptic()
                rxLocationScanTimestamp = ZonedDateTime.now()
                showSnackBar(
                    AcupickSnackEvent(
                        message = StringIdHelper.Format(R.string.location_scanned_format, barcodeType.rawBarcode),
                        type = SnackType.SUCCESS,
                        isDismissable = true
                    )
                )
                arrivalLabel.value = barcodeType
                activeScanTarget.postValue(ScanTarget.Bag)
                dispatchSnackbarEvent(StringIdHelper.Id(R.string.pharmacy_scan_bag_prompt))
            }

            is BarcodeType.PharmacyBag -> {
                val bagAlreadyScanned = incomingScannedRxBags.filter { it.bagNumber == barcodeType.rawBarcode }
                if (arrivalLabel.value == null) {
                    showWrongScan(app.applicationContext?.getString(R.string.pharmacy_bagscan_before_arrival_label).orEmpty())
                } else if (bagAlreadyScanned.isNotEmpty()) {
                    findRxBag(barcodeType.rawBarcode) { bag ->
                        showWrongScan(StringIdHelper.Format(R.string.rx_bag_error_already_scanned, bag.toString()).getString(context))
                    }
                } else {
                    val bagExistWithinApi = getValidRxBag(barcodeType.rawBarcode)
                    if (bagExistWithinApi != null) {
                        userFeedback.setSuccessScannedSoundAndHaptic()
                        incomingScannedRxBags.add(
                            RxBagUI(
                                orderNumber = barcodeType.customerOrderNumber,
                                barcodeType.rawBarcode,
                                deliveryFailReason = "",
                                rxReturnBagScanTimestamp = ZonedDateTime.now()
                            )
                        )
                        viewModelScope.launch {
                            incomingScannedRxBagsSharedFlow.emit(incomingScannedRxBags)
                        }
                        findRxBag(barcodeType.rawBarcode) { bag ->
                            showSnackBar(
                                AcupickSnackEvent(
                                    message = StringIdHelper.Format(R.string.rx_bag_scanned_successfully_format, bag.toString()),
                                    type = SnackType.SUCCESS,
                                    isDismissable = true
                                )
                            )
                        }
                    } else {
                        showWrongScan(context.getString(R.string.error_item_not_in_order))
                    }
                }
            }

            else -> {
                if (activeScanTarget.value == ScanTarget.PharmacyArrivalLabel) {
                    showWrongScan(StringIdHelper.Id(R.string.pharmacy_error_location_not_scanned).getString(context))
                } else {
                    showWrongScan(StringIdHelper.Id(R.string.pharmacy_bag_not_scanned).getString(context))
                }
            }
        }
    }

    private fun findRxBag(scannedBag: String, onSuccess: (bagNumber: Int) -> Unit) {
        resultsUiList.value?.firstOrNull()?.rxBags?.forEachIndexed { index, rxBagUI ->
            if (rxBagUI.bagNumber == scannedBag) {
                onSuccess(index + 1)
                return
            }
        }
    }

    private fun getValidRxBag(bagId: String) =
        apiRxBagList.value?.find { it.bagNumber == bagId }

    private fun getValidBagOrReshopMfcTote(bagId: String, customerOrderNumber: String) =
        bagLabelSourceOfTruth.find { it.getShortBagLabel() == bagId && it.customerOrderNumber == customerOrderNumber }

    private fun getValidMfcTote(toteId: String, customerOrderNumber: String) =
        bagLabelSourceOfTruth.find {
            it.labelId == getShortToteLabel(toteId) && (it.customerOrderNumber == customerOrderNumber || it.fulfillmentOrderNumber == customerOrderNumber)
        }

    private fun getValidNonMfcTote(toteId: String, customerOrderNumber: String) =
        bagLabelSourceOfTruth.find { it.getShortBagLabel()?.uppercase() == toteId.uppercase() && it.customerOrderNumber == customerOrderNumber }

    private fun isBagOrMfcToteScanSuccess(validBag: BagLabel?, isAlreadyScanned: Boolean, containerId: String): Boolean {
        var isComplete = false
        val isValidScan = if (validBag != null && !isAlreadyScanned) {
            val indexValue = zonedBagsScannedData.indexOf(zonedBagsScannedData.find { it.bagData?.labelId == validBag.getShortBagLabel() })
            isComplete = zonedBagsScannedData[indexValue].isComplete() == true
            isValidScan(validBag, zonedBagsScannedData[indexValue])
        } else {
            false
        }
        return isValidScan && !isComplete || isValidScan && checkIfScannedBagHasBeenForceScanned(containerId)
    }

    /* This updates the bag or mfc tote when there is a successful scan.  It also confirms the currently displayed page is correct and calls the toast function to give feedback to the user */
    private fun updateBagOrMfcToteOnScanSuccess(validBag: BagLabel?, barcodeType: StagingContainer) {
        if (previousPageEvent.value != pageEvent.value) previousPageEvent.value = pageEvent.value
        // Check if on correct page and if not slide to it
        val customerOrderNumberFromBag = validBag?.customerOrderNumber!!
        pageEvent.value = activityDtoArray.indexOf(activityDtoArray.find { it.customerOrderNumber == customerOrderNumberFromBag })
        viewModelScope.launch {
            delay(50) // delay for race condition
            if (scannedLastItemInZone(validBag) && isCustomerSubsEnabled()) {
                showRejectedItemsDialog(validBag.zoneType)
            }
            checkComplete()
            validBag.isScanned = true
            validBag.containerScanTime = ZonedDateTime.now()
            currentBagLabel.postValue(validBag)

            viewModelScope.launch {
                zonedBagUiData.emit(zonedBagsScannedData.toMutableList())
            }
            showBagOrMfcToteScanSuccess(barcodeType, validBag)
        }
    }

    fun isValidScan(validBag: BagLabel?, parent: ZonedBagsScannedData): Boolean {
        return (parent.bagData?.getShortBagLabel() == validBag?.getShortBagLabel()).also { isValid ->
            // update scan counts
            parent.currentBagsScanned++
            if (parent.bagData?.isLoose == true) parent.looseScanned++
            else parent.bagsScanned++
            if (parent.bagsForcedScanned > 0) parent.bagsForcedScanned--
            // clear all active zones
            zonedBagsScannedData.forEach { it.isActive = false }

            // set new active zone
            zonedBagsScannedData.takeOrder(validBag?.customerOrderNumber)
                .filter { zonedBagsScannedData ->
                    if (isCurrentOrderMfc) {
                        // if MFC, only need to match zone type (AM, CH, etc.) and if tote or reshop tote
                        zonedBagsScannedData.bagData?.zoneType == validBag?.zoneType &&
                            zonedBagsScannedData.bagData?.isReshop == validBag?.isReshop
                    } else
                    // if non-MFC, match zone ID (AMA00, etc.)
                        zonedBagsScannedData.bagData?.zoneId == validBag?.zoneId
                }
                .forEach { it.isActive = isValid }
        }
    }

    private fun checkIfScannedBagHasBeenForceScanned(containerId: String): Boolean {
        val bag = forceScanList.find {
            if (isCurrentOrderMfc) {
                it.bagData?.labelId?.takeLast(MFC_TOTE_ID_LENGTH) == containerId.takeLast(MFC_TOTE_ID_LENGTH)
            } else {
                it.bagData?.labelId == containerId
            }
        } ?: return false
        forceScanList.remove(bag)
        if (forceScanList.isEmpty()) {
            orderIssueReasonMap.clear()
        }
        return true
    }

    /**
     * END Bag and MfcTote Scan Handling
     */

    private fun showBagOrMfcToteScanSuccess(barcodeType: StagingContainer, successBag: BagLabel?) {
        userFeedback.setSuccessScannedSoundAndHaptic()
        val promptId = when (barcodeType) {
            is BarcodeType.Bag ->
                // Use a different snack bar message for bags scanned out of dummy staging locations
                if (successBag?.zoneId?.endsWith(DUMMY_STAGING_LOCATION_SUFFIX) == true) {
                    app.resources.getString(
                        when (successBag.isCustomerBagPreference.orTrue()) {
                            true -> R.string.success_bag_scanned_out_format
                            else -> R.string.success_loose_bag_scanned_out_format
                        },
                        barcodeType.bagOrToteId.takeLast(2),
                    )
                } else {
                    app.resources.getString(
                        when (successBag?.isCustomerBagPreference.orTrue()) {
                            true -> R.string.success_bag_scanned_out_of_zone_format
                            else -> R.string.success_loose_bag_scanned_out_of_zone_format
                        },
                        barcodeType.bagOrToteId.takeLast(2),
                        successBag?.zoneId
                    )
                }

            is BarcodeType.MfcTote, is BarcodeType.MfcReshopTote -> app.resources.getString(
                R.string.success_tote_scanned_out_format,
                barcodeType.displayToteId.takeLast(MFC_TOTE_ID_LENGTH),
                successBag?.zoneType?.displayName(context)
            )

            is BarcodeType.NonMfcTote -> app.resources.getString(
                R.string.success_non_mfc_tote_scanned_out_format,
                barcodeType.displayToteId.takeLast(2),
                successBag?.zoneId
            )

            else -> null
        }
        showSnackBar(
            AcupickSnackEvent(
                message = StringIdHelper.Raw(promptId.toString()),
                type = SnackType.SUCCESS,
                isDismissable = true,
                onDismiss = {
                    if (lockTab.value == false) {
                        advancePageIfOrderComplete()
                    }
                    showIndefinitePrompt()
                }
            )
        )
    }

    private fun showWrongScan(errMsg: String) {
        userFeedback.setFailureScannedSoundAndHaptic()
        showSnackBar(
            AcupickSnackEvent(
                message = StringIdHelper.Raw(errMsg),
                type = SnackType.ERROR,
                isDismissable = true
            )
        )
    }

    fun setOrderIssue(orderIssue: OrderIssueReasonCode) {
        orderIssueReasonCodeTemp = orderIssue
        Timber.d("OrderIssueReasonCode: ${orderIssue.name}")
        extractBagsForForceScan()
    }

    private fun extractBagsForForceScan() {
        forceScanTempList.addAll(zonedBagsScannedData.takeCurrentOrder().filter { !it.isComplete() })
        showOrderIssueCompleteDialog()
    }

    fun completeOrderIssue(labelId: String? = null) {
        // Check if individual bag is selected.
        labelId?.let {
            forceScanTempList.clear()
            forceScanTempList.addAll(zonedBagsScannedData.takeCurrentOrder().filter { !it.isComplete() && it.bagData?.labelId == labelId })
            bagBypass.postValue(forceScanList)
        }
        zonedBagsScannedData.forEach {
            if (forceScanTempList.contains(it)) {
                it.forceScanBags()
            }
            clearSnackBarEvents()
        }
        viewModelScope.launch {
            zonedBagUiData.emit(zonedBagsScannedData.toMutableList())
        }
        forceScanList.addAll(forceScanTempList)

        val orderExist = orderIssueReasonMap[currentOrderNumber].isNotNullOrEmpty()

        if (orderExist) {
            val issuesScanningBag = IssuesScanningBag(labelId, orderIssueReasonCodeTemp?.name.toString())
            orderIssueReasonMap[currentOrderNumber]?.add(issuesScanningBag)
        } else {
            val issuesScannedBags = mutableListOf<IssuesScanningBag>()
            issuesScannedBags.add(IssuesScanningBag(labelId, orderIssueReasonCodeTemp?.name.toString()))
            orderIssueReasonMap[currentOrderNumber] = issuesScannedBags
        }

        orderIssueReasonCodeTemp = null
        forceScanTempList.clear()
        userFeedback.setSuccessScannedSoundAndHaptic()
        forceScanList.forEach { zonedBagsScannedData ->
            zonedBagsScannedData.bagsForcedScanned = zonedBagsScannedData.totalBagsForZone - zonedBagsScannedData.currentBagsScanned
        }
        if (isCustomerSubsEnabled()) {
            showNextRejectedItemsStorageType()
        }
        checkComplete()
        clearOrderIssue.postValue(Unit)
        Timber.d("${forceScanList.size} bags force scanned")
    }

    fun cancelOrderIssue() {
        forceScanTempList.clear()
        orderIssueReasonCodeTemp = null
    }

    private fun showNextRejectedItemsStorageType() {
        if (!hasCompletedRejectingItems()) {
            completedRejectedItems.value?.filter { it.customerOrderNumber == currentOrderNumber }?.let { completedRejectedList ->
                if (rejectedItems.value?.minus(completedRejectedList.toSet())?.isNotEmpty().orFalse()) {
                    rejectedItems.value?.minus(completedRejectedList.toSet())?.first().let { nextRejectedItem ->
                        rejectedItemZone = nextRejectedItem?.storageType
                        showRejectedItemsDialog(rejectedItemZone)
                    }
                }
            }
        }
    }

    fun checkComplete() {
        // This is the updated isComplete value that will be posted to the LiveData
        val allOrdersAreComplete = zonedBagsScannedData.all { it.isComplete() }

        // This is a boolean that checks if a Hot Item exists in the ActivityDtoArray
        val hasHotItems: Boolean = activityDtoArray.filter { dto ->
            dto.isCustomerBagPreference != false &&
                dto.containerActivities?.filter { activity -> activity.containerType == StorageType.HT }.isNotNullOrEmpty()
        }.isNotNullOrEmpty()

        if (allOrdersAreComplete && hasHotItems && (hasCompletedRejectingItemsForOrder() || !isCustomerSubsEnabled())) {
            showHotDialog(getHotDialogActivityBagData())
        }

        isComplete.postValue(
            if (!isCustomerSubsEnabled()) {
                viewModelScope.launch {
                    delay(500)
                }
                allOrdersAreComplete
            } else {
                viewModelScope.launch {
                    delay(500)
                    if (allOrdersAreComplete && !hasCompletedRejectingItems()) {
                        showNextRejectedItemsStorageType()
                    }
                }
                allOrdersAreComplete && hasCompletedRejectingItems()
            }
        )

        updateFabVisibility()
    }

    private fun showRejectedItemsDialog(zoneType: StorageType?) {
        rejectedItemZone = zoneType
        val quantity = getRejectedItems().firstOrNull { it.storageType == zoneType }?.rejectedItems?.sumOf { it.qty ?: 0 }
        val customerName = activityDtoArray.firstOrNull { it.activityNo == destageOrderUiData?.activityNo }?.fullContactName().orEmpty()
        if (zoneHasNotCompletedRejectingItems() && hasRejectedItemDialogShown.not()) {
            lockTab.postValue(true)
            showRejectedItemSnackbar()
            inlineDialogEvent.postValue(
                CustomDialogArgDataAndTag(
                    data = getRejectedItemDialog(getRejectedItemDialogTitle(zoneType) ?: 0, customerName, quantity.toString(), getStorageTypeByName(zoneType).toString()),
                    tag = REJECTED_ITEM_TAG
                )
            ).also { hasRejectedItemDialogShown = true }
        }
    }

    private fun resetRejectedItemDialogShown() {
        hasRejectedItemDialogShown = false
    }

    fun showRemoveBagsDialog(customerName: String) {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = getNoBagsWithCustomerNameDialog(customerName),
                tag = EBT_WARNING_DIALOG_TAG
            )
        )
    }

    private fun zoneHasNotCompletedRejectingItems() = completedRejectedItems.value?.filter { it.customerOrderNumber == currentOrderNumber }?.none { it.storageType == rejectedItemZone } == true

    private fun advancePageIfOrderComplete() {
        if (!zonedBagsScannedData.all { it.isComplete() }) {
            val scannedBagsInCurrentOrder = zonedBagsScannedData.groupBy { it.bagData?.customerOrderNumber }[currentOrderNumber]
            val isCurrentOrderComplete = scannedBagsInCurrentOrder?.all { it.isComplete() } ?: false
            val tabCount = tabs.value?.count() ?: 0
            // pageEvent starts at 0 tabCount starts at 1. Minus 1 on tabCount to keep even
            if (isCurrentOrderComplete) {
                val newIndex = if (pageEvent.value < tabCount - 1) pageEvent.value.plus(1) else tabCount - 1
                pageEvent.value = newIndex
            }
        }
    }

    private fun updateFabVisibility() {
        val scannedListForCurrentOrder = zonedBagsScannedData.takeCurrentOrder()
        val forcedScannedListForCurrentOrder = scannedListForCurrentOrder.filter { it.bagsForcedScanned != 0 }
        val isCurrentOrderComplete = scannedListForCurrentOrder.all { it.isComplete() }
        viewModelScope.launch {
            delay(50)
            isFabVisible.postValue(
                (!isCurrentOrderComplete || !isRxComplete.value.orFalse() || forcedScannedListForCurrentOrder.isNotEmpty()) &&
                    !itemRemovalRequired.value.orFalse() && lockTab.value == false
            )
        }
    }

    private fun getHotDialogActivityBagData(): StringIdHelper {
        val builtBagInfoStrings = mutableListOf<String>()
        activityDtoArray.filter { dto ->
            dto.containerActivities?.filter { activity -> activity.containerType == StorageType.HT }.isNotNullOrEmpty()
        }.forEach { activity ->
            val quantity = zonedBagsScannedData.filter { it.bagData?.customerOrderNumber == activity.customerOrderNumber && it.bagData?.zoneType == StorageType.HT }.size
            builtBagInfoStrings.add(
                app.resources.getQuantityString(
                    R.plurals.hot_item_reminder_piece,
                    quantity,
                    activity.getFulfillmentTypeDescriptions(),
                    quantity,
                )
            )
        }
        val hotZoneRememberItemsAlertBodyText: String = when (builtBagInfoStrings.size) {
            2 -> app.resources.getString(R.string.hot_item_reminder_body_piece_two, builtBagInfoStrings[0], builtBagInfoStrings[1])
            3 -> app.resources.getString(R.string.hot_item_reminder_body_piece_three, builtBagInfoStrings[0], builtBagInfoStrings[1], builtBagInfoStrings[2])
            else -> if (builtBagInfoStrings.isNotNullOrEmpty()) builtBagInfoStrings.first() else ""
        }

        return StringIdHelper.Format(
            R.string.hot_item_reminder_body,
            hotZoneRememberItemsAlertBodyText
        )
    }

    // /////////////////////////////////////////////////////////////////////////
    // Prompt/Snackbars
    // /////////////////////////////////////////////////////////////////////////
    fun showIndefinitePrompt() {

        // Just call this whenever you might need to update the static prompt
        // (i.e., switch from the "begin" version to the "next" version)
        //
        // No longer need to worry about resurrecting it when transient snackbars go away
        //
        // While the prompt widget hides itself based on the isComplete live data flag, it
        // isn't really necessary as the Complete button fully obscures it
        when {
            itemRemovalRequired.value.orFalse() && isCustomerSubsEnabled() -> {
                showRejectedItemSnackbar()
                updateFabVisibility()
            }

            else -> {
                val areAnyScanned = zonedBagsScannedData.any { it.currentBagsScanned > 0 }
                val promptId = when {
                    _showPerscriptionPickup.value && arrivalLabel.value == null -> R.string.scan_pharmacy_arrival_label
                    !isCurrentOrderHasCustomerBagPreference && currentOrderHasLoosItem.value -> R.string.scan_totes_loose_item_prompt
                    !isCurrentOrderHasCustomerBagPreference || isCurrentOrderMfc -> R.string.scan_totes_prompt
                    !areAnyScanned -> R.string.scan_bags_prompt
                    else -> R.string.scan_a_new_bag
                }
                dispatchSnackbarEvent(StringIdHelper.Id(promptId))
            }
        }
    }

    private fun dispatchSnackbarEvent(stringIdHelper: StringIdHelper) =
        snackbarEvent.postValue(
            SnackBarEvent(prompt = stringIdHelper, action = {
                if (showPerscriptionPickup.value == true) onRxManualEntry()
                else onManualCtaClicked()
            })
        )

    private fun RxBagUI.toRxdItem(): RxOrder {
        return RxOrder(
            rxOrderId = bagNumber,
            rxOrderStatus = RxOrderStatus.SCANNED,
            deliveryFailReason = null,
            rxBagsScanTime = rxReturnBagScanTimestamp
        )
    }
    // /////////////////////////////////////////////////////////////////////////
    // UI callbacks
    // /////////////////////////////////////////////////////////////////////////

    fun onCompleteDestagingClick(customerOrderNumber: String/*, isRxDugFlow: Boolean = false*/) {
        val isCompleteCurrent = currentOrderZonedBagList.let { zonedBagList -> zonedBagList.all { it.isComplete() } && zonedBagList.isNotEmpty() }
        val isLastTab = isCompleteList.value?.lastOrNull()?.customerOrderNumber == customerOrderNumber
        // val isAllComplete = isCompleteList.value?.none { !it.isComplete }
        val isEnabled = if (isLastTab) isComplete.value == true else isCompleteCurrent
        when {
            !isEnabled -> showDisableButtonInfo()
            isLastTab -> onCompleteDestagingClicked()
            else -> switchToNextTab(customerOrderNumber)
        }
    }

    private fun showDisableButtonInfo() {
        // TODO: To be done once we get the copy from product/ux team
    }

    private fun switchToNextTab(customerOrderNumber: String) {
        viewModelScope.launch {
            isCompleteList.value?.indexOfFirst { it.customerOrderNumber == customerOrderNumber }?.plus(1)?.let {
                pageEvent.emit(it)
            }
        }
    }

    fun onRxConfirmCtaClick() {
        viewModelScope.launch {
            if (incomingScannedRxBags.isNotNullOrEmpty().orFalse()) {
                val activity = activityDtoArray.find { it.customerOrderNumber == currentOrderNumber }
                val confirmRxPickupRequestDto = ConfirmRxPickupRequestDto(
                    orderId = activity?.customerOrderNumber,
                    storeNumber = activity?.siteId,
                    orderStatus = activity?.status,
                    cartType = activity?.cartType,
                    rxLocationScanTimestamp = rxLocationScanTimestamp,
                    rxPickupCompleteTimestamp = ZonedDateTime.now(),
                    rxOrders = incomingScannedRxBags.map { it.toRxdItem() }
                )

                when (val results = isBlockingUi.wrap { apsRepo.confirmRxPickup(confirmRxPickupRequestDto) }) {
                    is ApiResult.Success -> navigateToHandOff(failedActivityDtoList)
                    is ApiResult.Failure -> {
                        if (results is ApiResult.Failure.Server) {
                            handleApiError(results, retryAction = { onRxConfirmCtaClick() })
                        } else {
                            // For network and other types of errors, move straight to the Complete Handoff screen, passing the scan data
                            navigateToHandOff(failedActivityDtoList, confirmRxPickupRequestDto)
                        }
                    }
                }
            }
        }
    }

    fun onConfirmCtaClick() {
        val results = resultsUiList.value

        results?.let {
            val names = it.filter { order -> order.isCustomerBagPreference == false && order.isMultiSource == true }
                .mapNotNull { order -> order.customerName.takeIf { order.isCustomerBagPreference == false && order.isMultiSource == true } }

            if (names.isNotEmpty()) {
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = getConfirmAllBagsRemovedDialog(names.joinToString(", ")),
                        tag = CONFIRM_REMOVAL_OF_ITEMS_DIALOG
                    )
                )
            } else {
                onCompleteDestaging()
            }
        } ?: onCompleteDestaging()
    }

    private fun onCompleteDestaging() {
        val userInvalidActivityIdList = arrayListOf<String>()
        groceryDestageCompleteTimestamp = ZonedDateTime.now()
        viewModelScope.launch {
            // check to see if there is a dug order present
            hasDugOrder.postValue((resultsUiList.value?.filter { it.isDugOrder }?.isNotNullOrEmpty()))
            // If not connected to network, move straight to the Complete Handoff screen, passing the scan data
            if (!networkAvailabilityManager.isConnected.first()) {
                navigateToHandOff(activityDtoArray)
            } else {
                // Build a list of [ActivityDto] for which we will have to make the scanContainers call later in [HandOffInterstitialViewModel]
                activityDtoArray
                    // If order status is PRE_COMPLETED, skip the network call
                    .filter { it.status != ActivityStatus.PRE_COMPLETED }
                    .map { createScanContainerWrapperRequestDto(it.customerOrderNumber) }
                    .forEach { requestDto ->
                        when (val results = isBlockingUi.wrap { apsRepo.scanContainers(scanContainerWrapperRequestDto = requestDto) }) {
                            is ApiResult.Success -> {
                                // API call successful--won't have to make the call again later for this order
                                failedActivityDtoList.removeIf { it.actId == requestDto.actId }
                                if (results.data.subStatus == CustomerArrivalStatus.ARRIVED) {
                                    latestVehicleInfo[requestDto.actId] = results.data
                                }
                            }

                            is ApiResult.Failure -> {
                                if (results is ApiResult.Failure.Server) {
                                    val type = results.error?.errorCode?.resolvedType
                                    if (type == ServerErrorCode.USER_NOT_VALID) {
                                        requestDto.actId?.let { userInvalidActivityIdList.add(activityDtoArray.find { it.actId == requestDto.actId }?.customerOrderNumber ?: "") }
                                        activityDtoArray.removeIf { it.actId == requestDto.actId }
                                        failedActivityDtoList.removeIf { it.actId == requestDto.actId }
                                        // showHandOffAlreadyAssignedDialog()
                                    } else {
                                        handleApiError(results, retryAction = { onConfirmCtaClick() })
                                    }
                                } else {
                                    // For network and other types of errors, move straight to the Complete Handoff screen, passing the scan data
                                    navigateToHandOff(failedActivityDtoList)
                                }
                            }
                        }
                    }

                if (userInvalidActivityIdList.isEmpty()) {
                    if (hasDugOrder.value == true && hasRxDetails().not()) {
                        inlineDialogEvent.postValue(
                            CustomDialogArgDataAndTag(
                                data = DESTAGING_DIALOG_ARGS,
                                tag = DESTAGING_DIALOG
                            )
                        )
                    } else {
                        navigateToHandOff(failedActivityDtoList)
                    }
                } else {
                    userInvalidActivityIdList.removeIf { it.isEmpty() }
                    showHandOffAlreadyAssignedDialog(userInvalidActivityIdList)
                }
            }
        }
    }

    fun acceptRejectedItems(dtoList: List<RemoveItemsRequestDto>) {
        viewModelScope.launch {
            lockTab.postValue(false)
            removeItemsRequests.addAll(dtoList)
            completedRejectedItems.value = completedRejectedItems.value?.apply {
                addAll(
                    rejectedItems.value?.filter {
                        it.storageType == rejectedItemZone
                    } ?: emptyList()
                )
            }
            if (orderIssueReasonMap.isNotEmpty() && !hasCompletedRejectingItems()) {
                showNextRejectedItemsStorageType()
            } else {
                checkComplete()
            }
            delay(500) // allow the page to move back to De-stage from Rejected items
            advancePageIfOrderComplete()
        }
    }

    private fun getRejectedItems(): List<RejectedItemsByStorageType> {
        return rejectedItems.value.orEmpty()
    }

    private fun getRejectedItemDialogTitle(storageType: StorageType?): Int? {
        if (storageType == null) return null
        return when (storageType) {
            StorageType.AM -> R.string.rejected_ambient_items
            StorageType.CH -> R.string.rejected_chilled_items
            StorageType.FZ -> R.string.rejected_frozen_items
            StorageType.HT -> R.string.rejected_hot_items
        }
    }

    private fun getStorageTypeByName(storageType: StorageType?): String? {
        if (storageType == null) return null
        return app.getString(
            when (storageType) {
                StorageType.AM -> R.string.storage_type_ambient
                StorageType.CH -> R.string.storage_type_chilled
                StorageType.FZ -> R.string.storage_type_frozen
                StorageType.HT -> R.string.storage_type_hot
            }
        )
    }

    fun showRejectedItemSnackbar() {
        itemRemovalRequired.postValue(true)

        viewModelScope.launch {
            delay(100) // page needs to load before locking
            lockTab.postValue(true)
        }
    }

    fun changePage(page: Int) {
        previousPageEvent.value = page
    }

    private fun scannedLastItemInZone(validBag: BagLabel?): Boolean {
        val zoneHasRejectedItem = getRejectedItems().filter { it.storageType == validBag?.zoneType && it.rejectedItems.isNotNullOrEmpty() }.isNotNullOrEmpty()
        if (!zoneHasRejectedItem) return false

        val allZoneBagsScanned = zonedBagsScannedData.toMutableList()
            .filter { it.bagData?.zoneType == validBag?.zoneType && !it.bagData?.isScanned.orFalse() && it.bagData?.labelId != validBag?.labelId }
            .filter { it.bagData?.customerOrderNumber == currentOrderNumber }
        return allZoneBagsScanned.isEmpty()
    }

    private fun hasCompletedRejectingItemsForOrder() =
        rejectedItems.value?.size == completedRejectedItems.value?.filter { it.customerOrderNumber == currentOrderNumber }?.size &&
            rejectedItems.value?.toSet() == completedRejectedItems.value?.filter { it.customerOrderNumber == currentOrderNumber }?.toSet()

    private fun hasCompletedRejectingItems(): Boolean {
        val itemList = activityDtoArray.map { activityDto ->
            activityDto.getRejectedItemsByZone()
        }.flatten()
        return (itemList.size == completedRejectedItems.value?.size) &&
            (itemList.toSet() == completedRejectedItems.value?.toSet())
    }

    // Pass in a list of ActivityDto for which the /api/scanContainers call failed
    private fun createHandOffUiList(failedActivityList: List<ActivityDto>, confirmRxPickupRequestDto: ConfirmRxPickupRequestDto?, updatedVehicleInfo: HashMap<Long?, ScanContDto?>): List<HandOffUI> =
        activityDtoArray.map { activityDto ->
            HandOffUI(
                activityDto = activityDto,
                // pass a ScanContainerWrapperRequestDto if /api/scanContainers call failed, or null if it succeeded
                scanContainerWrapperRequestDto = failedActivityList
                    .firstOrNull { it.customerOrderNumber == activityDto.customerOrderNumber }
                    ?.let { createScanContainerWrapperRequestDto(it.customerOrderNumber) },
                confirmRxPickupRequestDto = confirmRxPickupRequestDto,
                issueScanningBags = orderIssueReasonMap[activityDto.customerOrderNumber],
                rejectedItems = removeItemsRequests,
                groceryDestageStartTimestamp = groceryDestageStartTimestamp,
                groceryDestageCompleteTimestamp = groceryDestageCompleteTimestamp,
                customerInfoData = getCustomerInfo(activityDto),
                rxDeliveryFailedReason = rxDeliveryFailureReason.value,
                bagsPerTempZoneParams = BagsPerTempZoneParams(
                    name = activityDto.fullContactName(),
                    orderNumber = activityDto.customerOrderNumber.orEmpty(),
                    orderType = activityDto.getFulfillmentTypeDescriptions(),
                    startTime = null,
                    bagAndLooseItemCount = activityDto.bagAndLooseItemTotal(),
                    bagsPerTempZoneDataList = activityDto.containerActivities?.map { BagsPerTempZoneData(it.containerType, it.type, it.bagCount ?: 0, it.looseItemCount ?: 0) }
                ),
                updatedVehicleInfo = updatedVehicleInfo[activityDto.actId],
                confirmOrderTime = orderNumberAndLastScanTime.find { it.first == activityDto.customerOrderNumber }?.second ?: ZonedDateTime.now(),
                isGiftLabelPrinted = cofirmGiftDialogData
            )
        }

    private fun createScanContainerWrapperRequestDto(orderNumber: String?): ScanContainerWrapperRequestDto {

        activityDtoArray.find { it.customerOrderNumber == orderNumber }?.actId.toString().logError(
            "Activity Id is null. DestageOrderPagerViewModel(createScanContainerWrapperRequestDto)," +
                " Order Id-$orderNumber, User Id-${userRepo.user.value?.userId}, storeId-${siteRepo.siteDetails.value?.siteId}",
            acuPickLogger
        )

        val scanContainerRequestDtoList = zonedBagsScannedData.filter { it.bagData?.customerOrderNumber == orderNumber }
            .subtract(forceScanList)
            .map { zonedBagData ->
                ScanContainerRequestDto(
                    containerId = zonedBagData.bagData?.labelId,
                    overrideAttemptToRemove = true,
                    overrideRemoved = true,
                    overrideScanUser = true,
                    stagingLocation = zonedBagData.bagData?.zoneId,
                    startIfNotStarted = true,
                    containerScanTime = zonedBagData.bagData?.containerScanTime,
                    isLoose = zonedBagData.bagData?.isLoose
                )
            }

        orderNumber?.let { orderNum ->
            if (orderNumberAndLastScanTime.none { it.first == orderNum }) {
                orderNumberAndLastScanTime.add(Pair(orderNum, ZonedDateTime.now()))
            }
        }

        return ScanContainerWrapperRequestDto(
            actId = activityDtoArray.find { it.customerOrderNumber == orderNumber }?.actId,
            containerReqs = scanContainerRequestDtoList,
            lastScanTime = orderNumberAndLastScanTime.find { it.first == orderNumber }?.second ?: ZonedDateTime.now(),
            multipleHandoff = activityDtoArray.size > 1,
        )
    }

    fun onRxManualEntry() {
        viewModelScope.launch {
            clearSnackBarEvents()
            navigateToRxManualEntry()
        }
    }

    fun onManualCtaClicked() {
        previousPageEvent.value = pageEvent.value
        viewModelScope.launch {
            clearSnackBarEvents()
            navigateToManualEntry()
        }
    }

    fun handleExitButton() {
        if (incomingScannedRxBags.isNotNullOrEmpty()) {
            showLeavingRxScreenDialog()
        } else {
            showLeavingScreenDialog()
        }
    }

    private fun showLeavingScreenDialog() = inlineDialogEvent.postValue(
        CustomDialogArgDataAndTag(
            data = ORDER_DETAILS_CANCEL_ARG_DATA,
            tag = ORDER_DETAILS_LEAVE_SCREEN_DIALOG_TAG
        )
    )

    private fun navigateToReturnFragment(fromPartialPrescriptionPickup: Boolean) {
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                NavGraphDirections.actionToPrescriptionReturnFragment(
                    currentOrderErid().orEmpty(),
                    PrescriptionReturnData(
                        scannedData = incomingScannedRxBags.map { it.bagNumber.orEmpty() },
                        handOffUI = createHandOffUiList(failedActivityDtoList, null, latestVehicleInfo),
                        isFromNotification = isFromNotification.value.orFalse(),
                        fromPartialPrescriptionPickup = fromPartialPrescriptionPickup
                    )
                )
            )
        )
    }

    fun showLeavingRxScreenDialog(fromPartialPrescriptionPickup: Boolean = false) {
        this.fromPartialPrescriptionPickup = fromPartialPrescriptionPickup
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = RX_ORDER_DETAILS_CANCEL_DESTAGE_ARG_DATA,
                tag = RETURN_PRESCRIPTION_DIALOG_TAG
            )
        )
    }

    // RX Dug dialog
    fun showRxStaffRequiredDialog() =
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = PHARMACY_STAFF_REQUIRED_DIALOG,
                tag = RX_PHARMACY_STAFF_REQUIRED_TAG
            )
        )

    private fun showPrescriptionAddOnDialog() {
        val customerName = activityDtoArray.firstOrNull()?.fullContactName().orEmpty()
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = getPrescriptionAddOn(customerName),
                tag = RX_PRESCRIPTION_ADD_ON_TAG
            )
        )
    }

    fun launchGiftingDailog() {
        resultsUiList.value?.firstOrNull { it.customerOrderNumber == currentOrderNumber }?.giftMessage?.let { messageData ->
            inlineDialogEvent.postValue(
                CustomDialogArgDataAndTag(
                    data = CustomDialogArgData(
                        dialogType = DialogType.InformationDialog, title = StringIdHelper.Id(R.string.collect_gift_note), largeImage = R.drawable.ic_gift_note,
                        body = messageData.giftTo?.let { StringIdHelper.Format(R.string.gift_note_to, it) },
                        boldWord = StringIdHelper.Id(R.string.to),
                        bodyWithBold = messageData.giftText,
                        shouldBoldTitle = true,
                        secondaryBody = messageData.giftFrom?.let { StringIdHelper.Format(R.string.gift_note_from, it) },
                        questionBody = StringIdHelper.Id(R.string.from),
                        positiveButtonText =
                        StringIdHelper.Id(R.string.print_gift_note),
                        cancelOnTouchOutside = false
                    ),
                    GIFTING_DIALOG_TAG
                )
            )
        }
    }

    private fun onCompleteDestagingClicked() {
        getGiftConfirmationTextIfAvailable(
            available = { showGiftNoteConfirmationDialog(it) },
            unavailable = { onConfirmCtaClick() }
        )
    }

    private fun handleGiftDialog() {
        getGiftDataIfAvaiable {
            shownGiftDialogData.getOrPut(currentOrderNumber) {
                launchGiftingDailog()
                true
            }
        }
    }

    private fun getGiftDataIfAvaiable(block: () -> Unit) {
        resultsUiList.value?.firstOrNull { it.customerOrderNumber == currentOrderNumber }?.let { currentUiData ->
            if (currentUiData.isGift && currentUiData.giftMessage != null) {
                block.invoke()
            }
        }
    }

    private fun getGiftConfirmationTextIfAvailable(available: (List<String>) -> Unit, unavailable: () -> Unit) {
        resultsUiList.value?.filter { it.isGift && it.giftMessage != null }?.map { it.customerFistNameLastInitial }?.let {
            if (it.isNotEmpty()) available.invoke(it)
            else unavailable.invoke()
        }
    }

    private fun showGiftNoteConfirmationDialog(customerNames: List<String>) {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.InformationDialog,
                    title = StringIdHelper.Format(
                        R.string.gift_confirmation,
                        getAllComaSeparatedCustomerNames(customerNames)
                    ),
                    positiveButtonText =
                    StringIdHelper.Id(R.string.confirm),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel)
                ),
                GIFTING_CONFIRMATION_DIALOG_TAG
            )
        )
    }

    private fun getAllComaSeparatedCustomerNames(customerNames: List<String>) = with(customerNames) {
        when (size) {
            0 -> ""
            1 -> first()
            2 -> "${first()} and ${last()}"
            else -> {
                "${dropLast(1).joinToString(", ")} and ${last()}"
            }
        }
    }

    private fun printGiftLabel() {
        resultsUiList.value?.firstOrNull { it.customerOrderNumber == currentOrderNumber }?.erId?.let { erId ->
            viewModelScope.launch {
                if (networkAvailabilityManager.isConnected.value) {
                    isBlockingUi.wrap { apsRepo.printGiftLabel(erIds = listOf(erId)) }.let { result ->
                        if (result is ApiResult.Failure) {
                            showSnackBar(
                                AcupickSnackEvent(
                                    message = StringIdHelper.Id(R.string.error_print_gift),
                                    type = SnackType.ERROR
                                )
                            )
                        }
                    }
                } else {
                    networkAvailabilityManager.triggerOfflineError { printGiftLabel() }
                }
            }
        }
    }

    fun setupCustomerData(customerData: CustomerData?) {
        currentOrderCustomerData.postValue(customerData)
    }

    fun setupOrderDetails(activityDtoList: List<ActivityDto>) {
        if (!activityDtoArray.containsAll(activityDtoList)) {
            activityDtoList.forEach { activityDto ->
                if (activityDto.isCustomerBagPreference == false && activityDto.isMultiSource.orFalse())
                    activityDto.customerOrderNumber?.let { noBagsMap[it] = false }
            }
            activityDtoArray.addAll(activityDtoList)
            failedActivityDtoList.addAll(activityDtoList)

            isFabVisible.postValue(true)
            if (activityDtoList.firstOrNull()?.isRxDug().orFalse()) {
                changeToolbarRightSecondExtraImageEvent.postValue(DrawableIdHelper.Id(0))
                changeToolbarRightFirstExtraImageEvent.postValue(DrawableIdHelper.Id(0))
                // Set DUG interjection state if the the current destaging order is an Rx order
                setDugInterjectionState(DugInterjectionState.BatchFailureReason.DestagingRxOrder)
            }

            activityDtoArray.map { actDto ->
                actDto.containerActivities?.forEachIndexed { _, container ->
                    val bagLabel = BagLabel(
                        customerOrderNumber = container.customerOrderNumber,
                        fulfillmentOrderNumber = container.reference?.entityId,
                        customerNameFirstInitialLast = container.asFirstInitialDotLastString(),
                        zoneType = container.containerType,
                        zoneId = container.location ?: "",
                        labelId = container.containerId ?: "",
                        isLoose = container.type == ContainerType.LOOSE_ITEM,
                        isReshop = container.isReshop(),
                        isMultiSourceOrder = actDto.isMultiSource,
                        isCustomerBagPreference = actDto.isCustomerBagPreference,
                        bagCount = container.bagCount
                    )
                    if (bagLabelSourceOfTruth.none { it.labelId == bagLabel.labelId }) {
                        bagLabelSourceOfTruth.add(bagLabel)
                        zonedBagsScannedData.add(ZonedBagsScannedData(bagLabel))
                    }
                }
            }
        }

        assignBagsToOrder(activityDtoArray)

        currentOrderNumber = activityDtoArray.getOrNull(pageEvent.value)?.customerOrderNumber.orEmpty()
        when {
            false -> previousPageEvent.value = activityDtoArray.lastIndex
            else -> Timber.d("Initial loading of data, setupScanData() call, has already been made.")
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // API Calls
    // /////////////////////////////////////////////////////////////////////////
    fun loadDetails(erIds: List<Long>?, newOrder: Boolean = false) {
        val dtoArray = mutableListOf<ActivityDto>()
        val regulatedArray = mutableListOf<ActivityDto>()
        viewModelScope.launch(dispatcherProvider.Main) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> {
                    erIds?.forEach { erId ->
                        val result = isBlockingUi.wrap { apsRepo.pickUpActivityDetails(id = erId, loadCI = true) }
                        when (result) {
                            is ApiResult.Success<ActivityDto> -> {
                                if (newOrder) {
                                    if (result.data.activityHasRegulatedItems()) {
                                        regulatedArray.add(result.data)
                                    } else {
                                        dtoArray.add(result.data)
                                        isFabVisible.postValue(true)
                                    }
                                } else {
                                    dtoArray.add(result.data)
                                    isFabVisible.postValue(true)
                                }
                                if (siteRepo.isCctEnabled) {
                                    // precache image urls
                                    val imageUrls = mutableListOf<String?>()
                                    result.data.orderSummary?.forEach {
                                        imageUrls.add(it?.imageUrl)
                                        if (it?.substitutedWith.isNotNullOrEmpty()) {
                                            it?.substitutedWith?.forEach { subSummary ->
                                                imageUrls.add(subSummary?.imageUrl)
                                            }
                                        }
                                    }
                                    imagePreCacher.preCacheImages(imageUrls.mapNotNull { getSizedImageUrl(it, ImageSizePreset.ItemDetails) })
                                } else {
                                    // adding to fix compliler error
                                }
                            }

                            is ApiResult.Failure -> {
                                if (result is ApiResult.Failure.Server) {
                                    val type = result.error?.errorCode?.resolvedType
                                    if (type == ServerErrorCode.USER_NOT_VALID) {
                                        inlineDialogEvent.postValue(
                                            CustomDialogArgDataAndTag(
                                                data = HAND_OFF_ALREADY_ASSIGNED_ARG_DATA,
                                                tag = HANDOFF_ALREADY_ASSIGNED_DIALOG_TAG,
                                            )
                                        )
                                    } else {
                                        handleApiError(result, retryAction = { loadDetails(erIds, newOrder) })
                                    }
                                } else {
                                    handleApiError(result, retryAction = { loadDetails(erIds, newOrder) })
                                }
                            }
                        }.exhaustive
                    }
                }

                false -> {
                    networkAvailabilityManager.triggerOfflineError {
                        loadDetails(erIds, newOrder)
                    }
                }
            }

            if (regulatedArray.isNotEmpty()) {
                setUpDialogForDestageRegulatedOrder(regulatedArray)
                showOfAgeVerificationDialog()
            }
            setupOrderDetails(dtoArray)
        }
    }

    private fun setUpDialogForDestageRegulatedOrder(orders: List<ActivityDto>) {
        registerCloseAction(OF_AGE_ASSOCIATE_VERIFICATION_DESTAGE_TAG) {
            closeActionFactory(
                positive = {
                    loadDetails(orders.map { it.erId ?: 0 }, false)
                },
                negative = {
                    dialogTagCloseActionListenerMap[OF_AGE_ASSOCIATE_VERIFICATION_TAG]?.onCloseAction(CloseAction.Dismiss, null)
                    exitHandoffForCancelledRegulated(orders = orders)
                }
            )
        }
    }

    private fun exitHandoffForCancelledRegulated(orders: List<ActivityDto>) {
        viewModelScope.launch {
            val cancelHandoffReqList = orders.map {
                CancelHandoffRequestDto(
                    cancelReasonCode = CancelReasonCode.WRONG_HANDOFF,
                    erId = it.erId ?: 0L,
                    siteId = it.siteId.orEmpty(),
                )
            }
            val result = isBlockingUi.wrap { apsRepo.cancelHandoffs(cancelHandoffReqList) }
            when (result) {
                is ApiResult.Success -> {}
                is ApiResult.Failure -> {
                    Timber.d("Error exiting handoff for orders: ${orders.map { "actId: " + it.actId.toString() + ", " }}")
                    handleApiError(result, retryAction = { exitHandoffForCancelledRegulated(orders) })
                }
            }.exhaustive
        }
    }

    // resultsUiList preserve the data as the viewModels is created using navGraphViewModels
    // populating ressultsUiList with the data on recreeation of viewModel will trigger the UI update twice
    // as tabs livedata is observed in the fragment and it depends on the resultsUiList
    // checking for the size of resultsUiList will prevent the UI update
    // the size will differ only initially and when we add a customer
    private fun assignBagsToOrder(list: List<ActivityDto>) {
        if (resultsUiList.value?.size != list.size) {
            resultsUiList.postValue(
                list.map {
                    val rejectedItemCount = it.getRejectedItemsByZone()
                    DestageOrderUiData(it, rejectedItemCount, DetailsHeaderUi(it, ::onStartTimer), zonedBagsScannedData)
                }
            )
            viewModelScope.launch {
                zonedBagUiData.emit(zonedBagsScannedData.toMutableList())
            }
            checkComplete()
        }
    }

    fun assignToMe(actId: Long?) {
        viewModelScope.launch(dispatcherProvider.IO) {
            val user = userRepo.user.value!!.toUserDto()

            val alreadyAssignedToMe = resultsUiList.value?.find { it.actId == actId }

            if (alreadyAssignedToMe != null) {
                handoffAlreadyAssignedToMeDialog(alreadyAssignedToMe)
            } else {
                val result = isBlockingUi.wrap {
                    apsRepo.assignUserToHandoffs(
                        AssignUserWrapperRequestDto(
                            actIds = listOf(actId ?: 0),
                            replaceOverride = false,
                            user = user
                        )
                    )
                }
                when (result) {
                    is ApiResult.Success -> {
                        val id = result.data.firstOrNull()?.erId ?: 0L
                        loadDetails(listOf(id), true)
                        showSnackBar(
                            AcupickSnackEvent(
                                message = StringIdHelper.Format(R.string.batch_handoff_assigned, result.data.firstOrNull()?.fullContactName() ?: ""),
                                type = SnackType.INFO
                            )
                        )
                    }

                    is ApiResult.Failure -> {
                        if (result is ApiResult.Failure.Server) {
                            val type = result.error?.errorCode?.resolvedType
                            when (type?.cannotAssignToOrder()) {
                                true -> {
                                    val count = tabs.value?.size ?: 1
                                    val serverErrorType =
                                        if (type == ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY) {
                                            CannotAssignToOrderDialogTypes.HANDOFF
                                        } else {
                                            CannotAssignToOrderDialogTypes.REGULAR
                                        }
                                    serverErrorCannotAssignUser(serverErrorType, count > 1)
                                }

                                else -> {
                                    handleApiError(result, retryAction = { assignToMe(actId) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Navigation
    // /////////////////////////////////////////////////////////////////////////
    private fun exitHandOff() {
        if (activityDtoArray.isNotEmpty()) {
            viewModelScope.launch(dispatcherProvider.IO) {
                cancelHandoff()
                if (isFromNotification.value.orFalse()) {
                    navigateToHome()
                } else {
                    _navigationEvent.postValue(NavigationEvent.Up)
                }
            }
        }
    }

    private suspend fun cancelHandoff() {
        val cancelHandoffReqList = activityDtoArray.map {
            CancelHandoffRequestDto(
                cancelReasonCode = CancelReasonCode.WRONG_HANDOFF,
                erId = it.erId ?: 0L,
                siteId = it.siteId.orEmpty(),
            )
        }
        when (isBlockingUi.wrap { apsRepo.cancelHandoffs(cancelHandoffReqList) }) {
            is ApiResult.Success -> Unit
            is ApiResult.Failure -> {
                withContext(dispatcherProvider.Main) {
                    showSnackBar(
                        AcupickSnackEvent(
                            message = StringIdHelper.Id(R.string.handofff_cancellation_error),
                            type = SnackType.ERROR,
                            isDismissable = true
                        )
                    )
                }
            }
        }
    }

    // Pass in a list of ActivityDto for which the /api/scanContainers call failed
    private fun navigateToHandOff(failedActivityList: List<ActivityDto>, rxPickupRequestDto: ConfirmRxPickupRequestDto? = null) {
        saveActIdsIntoAppD()
        if (!activityDtoArray.hasPharmacyServicingOrdersAndStaged()) {
            continueNavigationToHandOff(failedActivityList, null)
            return
        }

        if (apiRxBagList.value.isNotNullOrEmpty() && apiRxBagList.value?.size != incomingScannedRxBags.size) {
            isFabVisible.postValue(true)
            _showPerscriptionPickup.value = true
            return
        } else {
            continueNavigationToHandOff(failedActivityList, rxPickupRequestDto)
        }
    }

    /**
     * Validate actIds of original activityDto list and actIds of failed activityDto list
     * Sending it to AppD in order to check status of the orders.
     */
    private fun saveActIdsIntoAppD() {
        val actIdOfActivityDtoList = activityDtoArray.map { it.actId }.toList()
        val actIdOfFailedActivityDtoList = failedActivityDtoList.map { it.actId }.toList()
        acuPickLogger.i(
            "Complete destaging navigateToHandOff originalActivityList= $actIdOfActivityDtoList failedActvityList= " +
                "$actIdOfFailedActivityDtoList "
        )
    }

    fun continueToHandoff() {
        continueNavigationToHandOff(failedActivityDtoList, null)
    }

    private fun continueNavigationToHandOff(failedActivityList: List<ActivityDto>, rxPickupRequestDto: ConfirmRxPickupRequestDto?) {
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                NavGraphDirections.actionToHandOffFragment(
                    HandOffArgData(createHandOffUiList(failedActivityList, rxPickupRequestDto, latestVehicleInfo)),
                    isFromNotification.value.orFalse(),
                    pickedBagNumbers = incomingScannedRxBags.takeIf { it.isNotEmpty() }
                        ?.map { it.bagNumber.orEmpty() }
                        ?.let { PrescriptionReturnData(it) }
                )
            )
        )
    }

    fun navigateToRemoveItemsFragment() {
        lockTab.postValue(false)
        previousPageEvent.value = pageEvent.value
        val storageType = rejectedItemZone?.name ?: StorageType.AM.name
        val ui = RemoveRejectedItemUiData(destageOrderUiData)
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                DestageOrderPagerFragmentDirections.actionDestageOrderFragmentToRemoveRejectedItemsFragment(ui = ui, storageType = storageType)
            )
        )
    }

    private fun hasRxDetails() = activityDtoArray.firstOrNull { it.activityNo == currentActNo.value }?.rxDetails != null
    private fun currentOrderErid() = activityDtoArray.firstOrNull { it.activityNo == currentActNo.value }?.erId?.toString()
    private fun snackbarRemoveItemsEvent() = SnackBarEvent(prompt = null, cta = null, payload = null, action = { navigateToRemoveItemsFragment() })
    private fun navigateToManualEntry() {
        val params = ManualEntryHandoffParams(
            bagLabels = ArrayList(bagLabelSourceOfTruth.filter { it.customerOrderNumber == currentOrderNumber }),
            customerOrderNumber = currentOrderNumber,
            activityId = currentActNo.value.orEmpty(),
            isMutliSource = isCurrentOrderMultiSource,
            shortOrderId = destageOrderUiData?.detailsHeaderUi?.shortOrderNumber,
            customerName = destageOrderUiData?.customerName
        )
        if (isCurrentOrderMfc) {
            // _navigationEvent.postValue(NavigationEvent.Directions(NavGraphDirections.actionToManualEntryHandOffMfcFragment(params)))
            inlineBottomSheetEvent.postValue(
                BottomSheetArgDataAndTag(
                    data = CustomBottomSheetArgData(
                        dialogType = BottomSheetType.ManualEntryMfcDestaging,
                        title = StringIdHelper.Raw(""),
                        peekHeight = R.dimen.expanded_bottomsheet_peek_height,
                        customDataParcel = params
                    ),
                    tag = MANUAL_ENTRY_DESTAGING_BOTTOMSHEET_TAG
                )
            )
            isManualEntryBottomSheetOpen = true
        } else {
            inlineBottomSheetEvent.postValue(
                BottomSheetArgDataAndTag(
                    data = CustomBottomSheetArgData(
                        dialogType = BottomSheetType.ManualEntryDestaging,
                        title = StringIdHelper.Raw(""),
                        peekHeight = R.dimen.expanded_bottomsheet_peek_height,
                        customDataParcel = params
                    ),
                    tag = MANUAL_ENTRY_DESTAGING_BOTTOMSHEET_TAG
                )
            )
            isManualEntryBottomSheetOpen = true
        }
    }

    fun getManualEntryToolTipBottomsheetDialog() {
        inlineBottomSheetEvent.postValue(
            BottomSheetArgDataAndTag(
                data = CustomBottomSheetArgData(
                    dialogType = BottomSheetType.ManualEntryToolTip,
                    title = StringIdHelper.Raw(""),
                    isFullScreen = true,
                ),
                tag = MANUAL_ENTRY_TOOL_TIP_BOTTOMSHEET_TAG
            )
        )
    }

    fun getManualEntryBottomSheetDismissArgData() = CustomBottomSheetArgData(
        title = StringIdHelper.Raw(""),
        exit = true
    )

    fun navigateToReportMissingBagOrToteFragment(storageType: StorageType) {
        forceScanTempList.clear()
        forceScanTempList.addAll(zonedBagsScannedData.takeCurrentOrder().filter { !it.isComplete() })
        // if it is CBP and mfc it is tote else bag
        orderIssueReasonCodeTemp = if (isCurrentOrderMfc || !isCurrentOrderHasCustomerBagPreference) OrderIssueReasonCode.TOTES_MISSING else OrderIssueReasonCode.BAGS_MISSING
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                NavGraphDirections.actionToReportMissignBagsFragment(
                    reportMissingBagsParams = ReportMissingBagsParams(
                        forceScanTempList.toList(), isMissingBags = true, storageType = storageType, pageEvent.value, isCurrentOrderMfc,
                        isCurrentOrderHasCustomerBagPreference, isLooseItemLableMissing = false
                    )
                )
            )
        )
    }

    fun navigateToReportMissingLooseItemFragment(storageType: StorageType) {
        forceScanTempList.clear()
        // Filter out the bags that are complete and are loose items applicable for CBP
        forceScanTempList.addAll(zonedBagsScannedData.takeCurrentOrder().filter { !it.isComplete() && it.bagData?.isLoose.orFalse() })
        orderIssueReasonCodeTemp = OrderIssueReasonCode.LOOSE_ITEMS_MISSING
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                NavGraphDirections.actionToReportMissignBagsFragment(
                    reportMissingBagsParams = ReportMissingBagsParams(
                        forceScanTempList.toList(), isMissingBags = false, storageType = storageType, pageEvent.value, isCurrentOrderMfc,
                        isCurrentOrderHasCustomerBagPreference, isLooseItemLableMissing = false, isLooseItemMissing = true
                    )
                )
            )
        )
    }

    fun navigateToReportMissingBagOrToteLabelFragment(storageType: StorageType) {
        forceScanTempList.clear()
        forceScanTempList.addAll(zonedBagsScannedData.takeCurrentOrder().filter { !it.isComplete() })
        // if it is CBP and mfc it is tote else bag
        orderIssueReasonCodeTemp = if (isCurrentOrderMfc || !isCurrentOrderHasCustomerBagPreference) OrderIssueReasonCode.TOTE_LABELS else OrderIssueReasonCode.BAG_LABELS
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                NavGraphDirections.actionToReportMissignBagsFragment(
                    reportMissingBagsParams = ReportMissingBagsParams(
                        forceScanTempList.toList(), isMissingBags = false, storageType = storageType,
                        pageEvent.value, isCurrentOrderMfc, isCurrentOrderHasCustomerBagPreference, isLooseItemLableMissing = false
                    )
                )
            )
        )
    }

    fun navigateToReportMissingLooseItemLabelFragment(storageType: StorageType) {
        forceScanTempList.clear()
        // Filter out the bags that are complete and are loose items applicable for CBP
        forceScanTempList.addAll(zonedBagsScannedData.takeCurrentOrder().filter { !it.isComplete() && it.bagData?.isLoose.orFalse() })
        orderIssueReasonCodeTemp = OrderIssueReasonCode.LOOSE_ITEM_LABELS
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                NavGraphDirections.actionToReportMissignBagsFragment(
                    reportMissingBagsParams = ReportMissingBagsParams(
                        forceScanTempList.toList(), isMissingBags = false, storageType = storageType,
                        pageEvent.value, isCurrentOrderMfc, isCurrentOrderHasCustomerBagPreference, isLooseItemLableMissing = true
                    )
                )
            )
        )
    }

    private fun navigateToRxManualEntry() {
        inlineBottomSheetEvent.postValue(
            BottomSheetArgDataAndTag(
                data = CustomBottomSheetArgData(
                    dialogType = BottomSheetType.ManualEntryPharmacy,
                    title = StringIdHelper.Raw(""),
                    isFullScreen = true,
                    customDataParcel = ManualEntryPharmacyParams(
                        orderNumber = incomingScannedRxBags.getOrNull(0)?.orderNumber ?: "", // TODO : change for multiple bags
                        scanTarget = activeScanTarget.value ?: ScanTarget.PharmacyArrivalLabel,
                        shortOrderId = destageOrderUiData?.detailsHeaderUi?.shortOrderNumber,
                        customerName = destageOrderUiData?.detailsHeaderUi?.contactName,
                        customerOrderNumber = destageOrderUiData?.customerOrderNumber
                    )
                ),
                tag = MANUAL_ENTRY_PHARMACY_BOTTOMSHEET_TAG
            )
        )
    }

    private fun navigateToUpdateCustomerAddFragment(count: Int) {
        if (lockTab.value == false) {
            viewModelScope.launch {
                delay(500)
                _navigationEvent.postValue(
                    NavigationEvent.Directions(
                        NavGraphDirections.actionToUpdateCustomerAddFragment(count)
                    )
                )
            }
        }
    }

    private fun navigateToUpdateCustomerArrivedFragment() {
        if (lockTab.value == false) {
            viewModelScope.launch {
                delay(500)
                _navigationEvent.postValue(
                    NavigationEvent.Directions(
                        NavGraphDirections.actionToUpdateCustomerMarkArrivedFragment()
                    )
                )
            }
        }
    }

    // DUG interjection to set dug interjection state in case of BATCH FAILURE
    fun setDugInterjectionState(dugInterjectionState: DugInterjectionState) = pushNotificationsRepository.setDugInterjectionState(dugInterjectionState)

    // /////////////////////////////////////////////////////////////////////////
    // Dialogs
    // /////////////////////////////////////////////////////////////////////////
    init {
        registerCloseAction(ORDER_DETAILS_LEAVE_SCREEN_DIALOG_TAG) {
            closeActionFactory(positive = { exitHandOff() })
        }

        registerCloseAction(RETURN_PRESCRIPTION_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    viewModelScope.launch(dispatcherProvider.IO) {
                        cancelHandoff()
                        navigateToReturnFragment(fromPartialPrescriptionPickup)
                    }
                },
                negative = { fromPartialPrescriptionPickup = false },
                dismiss = { fromPartialPrescriptionPickup = false }
            )
        }

        registerCloseAction(MANUAL_ENTRY_DESTAGING_BOTTOMSHEET_TAG) {
            closeActionFactory(dismiss = { isManualEntryBottomSheetOpen = false })
        }

        registerCloseAction(REJECTED_ITEM_TAG) {
            closeActionFactory(
                positive = {
                    resetRejectedItemDialogShown()
                    lockTab.postValue(false)
                    navigateToRemoveItemsFragment()
                },
                dismiss = {
                    resetRejectedItemDialogShown()
                    clearSnackBarEvents()
                    showRejectedItemSnackbar()
                },
                negative = {
                    resetRejectedItemDialogShown()
                    clearSnackBarEvents()
                    showRejectedItemSnackbar()
                },
            )
        }

        registerCloseAction(API_FAILURE) {
            closeActionFactory(positive = { onConfirmCtaClick() })
        }

        registerCloseAction(ORDER_ISSUE_SCAN_BAGS_DIALOG_TAG) {
            closeActionFactory(positive = { showOrderIssueDialog() })
        }

        registerCloseAction(ORDER_ISSUE_SCAN_TOTES_DIALOG_TAG) {
            closeActionFactory(positive = { showMfcOrderIssueDialog() })
        }

        registerCloseAction(ORDER_ISSUE_COMPLETE_DIALOG_TAG) {
            closeActionFactory(
                positive = { completeOrderIssue() },
                negative = { cancelOrderIssue() }
            )
        }

        registerCloseAction(DESTAGING_DIALOG) {
            closeActionFactory(
                positive = { navigateToHandOff(failedActivityDtoList) }
            )
        }

        registerCloseAction(ORDER_ISSUE_DIALOG_TAG) {
            closeActionFactory(
                positive = { selection -> selection?.let { setOrderIssue(orderIssueReasonCode[it]) } },
                negative = { cancelOrderIssue() }
            )
        }

        registerCloseAction(MFC_ORDER_ISSUE_DIALOG_TAG) {
            closeActionFactory(
                positive = { selection -> selection?.let { setOrderIssue(orderIssueReasonCode[it + OrderIssueReasonCode.TOTE_LABELS.ordinal]) } },
                negative = { cancelOrderIssue() }
            )
        }

        registerCloseAction(HANDOFF_USER_NOT_VALID_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    if (activityDtoArray.isEmpty()) {
                        navigateToHome()
                    } else {
                        navigateToHandOff(failedActivityDtoList)
                    }
                }
            )
        }

        registerCloseAction(RX_PRESCRIPTION_ADD_ON_TAG) {
            closeActionFactory(
                positive = {
                    handleGiftDialog()
                }
            )
        }

        viewModelScope.launch(dispatcherProvider.IO) {
            _showPerscriptionPickup.collectLatest { showPerscriptionPickup ->
                if (showPerscriptionPickup) {
                    val promptId = R.string.scan_pharmacy_arrival_label
                    activeScanTarget.postValue(ScanTarget.PharmacyArrivalLabel)
                    dispatchSnackbarEvent(StringIdHelper.Id(promptId))
                }
            }
        }
    }

    private fun showHotDialog(hotDialogActivityBagData: StringIdHelper) =
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = getDestageOrderHotReminderArgData(hotDialogActivityBagData),
                tag = CONFIRM_ORDER_HOT_REMINDER_DIALOG_TAG
            )
        )

    fun showOrderIssueScanBagDialog() =
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = ORDER_ISSUE_SCAN_BAGS_ARG_DATA,
                tag = ORDER_ISSUE_SCAN_BAGS_DIALOG_TAG
            )
        )

    fun showOrderIssueScanToteDialog() =
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = ORDER_ISSUE_SCAN_TOTES_ARG_DATA,
                tag = ORDER_ISSUE_SCAN_TOTES_DIALOG_TAG
            )
        )

    private fun showHandOffAlreadyAssignedDialog(orderNumbers: List<String>) =
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = if (orderNumbers.size > 1) getHandOffBatchAlreadyAssignedWithOrderNumbersDialog(orderNumbers.toString())
                else
                    getHandOffAlreadyAssignedWithOrderNumberDialog(orderNumbers.toString()),
                // This close action tag is a special case used in only one place. Be careful and test it thoroughly before using it
                tag = HANDOFF_USER_NOT_VALID_DIALOG_TAG,
            )
        )

    fun showOrderIssueDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.RadioButtons,
                    title = StringIdHelper.Id(R.string.order_issue_dialog_title),
                    customData = listOf(
                        StringIdHelper.Id(R.string.bag_labels_missing),
                        StringIdHelper.Id(R.string.bags_missing),
                    ) as Serializable,
                    positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                ),
                tag = ORDER_ISSUE_DIALOG_TAG
            )
        )
    }

    private fun showMfcOrderIssueDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.RadioButtons,
                    title = StringIdHelper.Id(R.string.order_issue_dialog_title),
                    customData = listOf(
                        StringIdHelper.Id(R.string.tote_labels_missing),
                        StringIdHelper.Id(R.string.totes_missing),
                    ) as Serializable,
                    positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                ),
                tag = MFC_ORDER_ISSUE_DIALOG_TAG
            )
        )
    }

    private fun showOrderIssueCompleteDialog() =
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.complete_order_issue_dialog_title),
                    body = createOrderIssueCompleteBody(),
                    positiveButtonText = StringIdHelper.Id(R.string.complete),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel)
                ),
                tag = ORDER_ISSUE_COMPLETE_DIALOG_TAG
            )
        )

    fun showMaxAssignedDialog() =
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.max_assigned_title),
                    body = StringIdHelper.Id(R.string.max_assigned_body),
                    positiveButtonText = StringIdHelper.Id(R.string.ok)
                ),
                tag = MAX_ORDERS_ASSIGNED
            )
        )

    fun onCustomerAdded(customerName: String?) {
        showSnackBar(
            AcupickSnackEvent(
                message = StringIdHelper.Raw(customerName.orEmpty()),
                type = SnackType.SUCCESS,
                isDismissable = true,
                onDismiss = { showIndefinitePrompt() }
            )
        )
    }

    fun onCustomerUpdateStatus(tabCount: Int, orderToUpdate: MarkedArrivedUI) {
        val action = if (orderToUpdate.snackBarData?.second == 1)
            SnackAction(
                actionText = StringIdHelper.Id(R.string.add_to_handoff),
                onActionClicked = {
                    if (tabCount >= MAX_HANDOFF_COUNT) {
                        showMaxAssignedDialog()
                    } else {
                        assignToMe(orderToUpdate.markedArrived)
                    }
                }
            ) else null

        showSnackBar(
            AcupickSnackEvent(
                message = StringIdHelper.Raw(orderToUpdate.snackBarData?.first.orEmpty()),
                type = SnackType.SUCCESS,
                action = action
            )
        )
    }

    private fun handoffAlreadyAssignedToMeDialog(order: DestageOrderUiData) {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.confirm_order_customer_already_added_title),
                    body = StringIdHelper.Format(
                        idRes = R.string.confirm_order_customer_already_added_body,
                        rawString = "${order.detailsHeaderUi.contactName} (${order.detailsHeaderUi.shortOrderNumber})"
                    ),
                    positiveButtonText = StringIdHelper.Id(R.string.ok)
                ),
                tag = ORDER_ALREADY_ASSIGNED
            )
        )
    }

    private fun createOrderIssueCompleteBody(): StringIdHelper {
        val frozenCount = forceScanTempList.takeCurrentOrder()
            .filter { it.bagData?.zoneType == StorageType.FZ }
            .sumOf { it.totalBagsForZone - it.currentBagsScanned }
        val chilledCount = forceScanTempList.takeCurrentOrder()
            .filter { it.bagData?.zoneType == StorageType.CH }
            .sumOf { it.totalBagsForZone - it.currentBagsScanned }
        val ambientCount = forceScanTempList.takeCurrentOrder()
            .filter { it.bagData?.zoneType == StorageType.AM }
            .sumOf { it.totalBagsForZone - it.currentBagsScanned }
        val hotCount = forceScanTempList.takeCurrentOrder()
            .filter { it.bagData?.zoneType == StorageType.HT }
            .sumOf { it.totalBagsForZone - it.currentBagsScanned }

        val frozen: String = app.getString(R.string.frozen)
        val chilled: String = app.getString(R.string.chilled)
        val ambient: String = app.getString(R.string.ambient)
        val hot: String = app.getString(R.string.hot)

        val zonesList: MutableList<String> = mutableListOf()
        if (frozenCount > 0) zonesList.add(frozen)
        if (chilledCount > 0) zonesList.add(chilled)
        if (ambientCount > 0) zonesList.add(ambient)
        if (hotCount > 0) zonesList.add(hot)

        val zonesString: String =
            when (zonesList.size) {
                1 -> zonesList[0]
                2 -> "${zonesList[0]} and ${zonesList[1]}"
                3 -> "${zonesList[0]}, ${zonesList[1]}, and ${zonesList[2]}"
                4 -> "${zonesList[0]}, ${zonesList[1]}, ${zonesList[2]}, and ${zonesList[3]}"
                else -> ""
            }
        // TODO: Customer bag preference Venkatesh need to test this flow
        return StringIdHelper.Raw(
            app.resources.getString(
                if (isCurrentOrderMfc) R.string.complete_order_issue_mfc_dialog_body else R.string.complete_order_issue_dialog_body,
                app.resources.getQuantityString(
                    if (isCurrentOrderMfc) R.plurals.totes_plural else R.plurals.bags_plural,
                    forceScanTempList.sumOf { it.totalBagsForZone - it.currentBagsScanned },
                    forceScanTempList.sumOf { it.totalBagsForZone - it.currentBagsScanned }
                ),
                zonesString
            )
        )
    }

    /** Given a [List] of type [ZonedBagsScannedData], returns a filtered list for the currently shown order */
    private fun List<ZonedBagsScannedData>.takeCurrentOrder() = this.takeOrder(currentOrderNumber)

    private fun getShortToteLabel(labelId: String?) = if (labelId?.contains('-') == true) labelId.split('-').first() else labelId

    private fun showOfAgeVerificationDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = OF_AGE_ASSOCIATE_VERIFICATION_DATA,
                tag = OF_AGE_ASSOCIATE_VERIFICATION_DESTAGE_TAG,
            )
        )
    }

    private fun onStartTimer(job: Job) {
        timerjobs.add(job)
    }

    override fun onCleared() {
        super.onCleared()
        timerjobs.forEach { it.cancel() }
    }

    companion object {
        const val ORDER_DETAILS_LEAVE_SCREEN_DIALOG_TAG = "orderDetailsLeaveScreenDialogTag"
        const val RETURN_PRESCRIPTION_DIALOG_TAG = "returnPrescriptionDialogTag"
        const val ORDER_ISSUE_SCAN_BAGS_DIALOG_TAG = "orderIssueScanBagsDialogTag"
        const val ORDER_ISSUE_SCAN_TOTES_DIALOG_TAG = "orderIssueScanTotesDialogTag"
        const val ORDER_ISSUE_COMPLETE_DIALOG_TAG = "orderIssueCompleteDialogTag"
        const val ORDER_ISSUE_DIALOG_TAG = "orderIssueDialogTag"
        const val MFC_ORDER_ISSUE_DIALOG_TAG = "mfcOrderIssueDialogTag"
        const val CONFIRM_ORDER_HOT_REMINDER_DIALOG_TAG = "destageOrderHotReminderDialogTag"
        const val API_FAILURE = "orderDetailsApiFailureDialogTag"
        const val MAX_ORDERS_ASSIGNED = "maxOrdersAssignedToHandoff"
        const val ORDER_ALREADY_ASSIGNED = "orderAlreadyAssignedToUser"
        const val HANDOFF_ALREADY_ASSIGNED_DIALOG_TAG = "handoffAlreadyAssignedTag"
        const val HANDOFF_USER_NOT_VALID_DIALOG_TAG = "handoffUserNotValidTag"
        const val REJECTED_ITEM_TAG = "rejectedItemTag"
        const val OF_AGE_ASSOCIATE_VERIFICATION_TAG = "ofAgeAssociateVerificationTag"
        const val OF_AGE_ASSOCIATE_VERIFICATION_DESTAGE_TAG = "ofAgeAssociateVerificationDestageTag"
        const val DESTAGING_DIALOG = "destagingdialog"
        const val RX_BAG_NUMBER = "01"
        const val MANUAL_ENTRY_DESTAGING_BOTTOMSHEET_TAG = "ManualEntryDeStagingBottomSheetTag"
        const val MANUAL_ENTRY_TOOL_TIP_BOTTOMSHEET_TAG = "ManualEntryToolTipBottomSheetTag"
        const val MANUAL_ENTRY_PHARMACY_BOTTOMSHEET_TAG = "ManualEntryPharmacyBottomSheetTag"

        private const val MFC_TOTE_ID_LENGTH = 8
        private const val EBT_WARNING_DIALOG_TAG = "ebtWarningDialogTag"
        private const val CONFIRM_REMOVAL_OF_ITEMS_DIALOG = "confirmRemovalOfItemsDialog"
        private const val DESTAGING_ACTION_SHEET_DIALOG_TAG = "DestagingActionSheetDialogTag"
        const val GIFTING_DIALOG_TAG = "GiftingDialogTag"
        const val GIFTING_CONFIRMATION_DIALOG_TAG = "GiftingConfirmationDialogTag"
    }
}
