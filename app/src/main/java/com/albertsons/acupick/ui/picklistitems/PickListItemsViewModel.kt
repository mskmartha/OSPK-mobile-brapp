package com.albertsons.acupick.ui.picklistitems

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.AcuPickConfig
import com.albertsons.acupick.EventCategory
import com.albertsons.acupick.EventKey
import com.albertsons.acupick.EventLabel
import com.albertsons.acupick.FirebaseAnalyticsInterface
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.EndPickReasonCode
import com.albertsons.acupick.data.model.FulfilledQuantityResult
import com.albertsons.acupick.data.model.ImageSizePreset
import com.albertsons.acupick.data.model.ItemSearchResult
import com.albertsons.acupick.data.model.ItemSearchResult.Error
import com.albertsons.acupick.data.model.ItemSearchResult.MatchedItem
import com.albertsons.acupick.data.model.PickListActivity
import com.albertsons.acupick.data.model.PickRequest
import com.albertsons.acupick.data.model.PrePickType
import com.albertsons.acupick.data.model.QuantitySelectionType
import com.albertsons.acupick.data.model.ScannedPickItem
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.ShortReasonCode
import com.albertsons.acupick.data.model.SubReasonCode
import com.albertsons.acupick.data.model.SubstitutePickRequest
import com.albertsons.acupick.data.model.SubstitutedItem
import com.albertsons.acupick.data.model.SubstitutionCode
import com.albertsons.acupick.data.model.ToteEstimate
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.barcode.PickingContainer
import com.albertsons.acupick.data.model.barcode.asBarcodeType
import com.albertsons.acupick.data.model.barcode.getUpcQty
import com.albertsons.acupick.data.model.chat.DisplayType
import com.albertsons.acupick.data.model.chat.MessageSource
import com.albertsons.acupick.data.model.getItemActivityDto
import com.albertsons.acupick.data.model.isAdvancePick
import com.albertsons.acupick.data.model.isAdvancePickOrPrePick
import com.albertsons.acupick.data.model.isMatchedItem
import com.albertsons.acupick.data.model.itemActivities
import com.albertsons.acupick.data.model.request.ChatErrorData
import com.albertsons.acupick.data.model.request.MissingItemLocationRequestDto
import com.albertsons.acupick.data.model.request.NetworkCalls
import com.albertsons.acupick.data.model.request.PickCompleteRequestDto
import com.albertsons.acupick.data.model.request.ShortPickRequestDto
import com.albertsons.acupick.data.model.request.ShortRequestDto
import com.albertsons.acupick.data.model.request.SubstitutionRejectedReason
import com.albertsons.acupick.data.model.request.UndoPickLocalDto
import com.albertsons.acupick.data.model.request.UndoPickRequestDto
import com.albertsons.acupick.data.model.request.UndoShortRequestDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.CannotAssignToOrderDialogTypes
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.ItemDetailDto
import com.albertsons.acupick.data.model.response.PickedItemUpcDto
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.asListOfItemIds
import com.albertsons.acupick.data.model.response.containsSnap
import com.albertsons.acupick.data.model.response.fullContactName
import com.albertsons.acupick.data.model.response.getListOfOrderNumber
import com.albertsons.acupick.data.model.response.isIssueScanned
import com.albertsons.acupick.data.model.response.isShorted
import com.albertsons.acupick.data.model.response.isSubstituted
import com.albertsons.acupick.data.model.response.isSubstitution
import com.albertsons.acupick.data.model.response.isSubstitutionOrIssueScanning
import com.albertsons.acupick.data.model.response.isWineOrder
import com.albertsons.acupick.data.model.response.netWeight
import com.albertsons.acupick.data.model.response.processedAndExceptionQty
import com.albertsons.acupick.data.model.response.remainingWeight
import com.albertsons.acupick.data.model.response.requestedNetWeight
import com.albertsons.acupick.data.model.response.stageByTime
import com.albertsons.acupick.data.model.response.toPickListActivity
import com.albertsons.acupick.data.model.response.toSwapItem
import com.albertsons.acupick.data.model.text
import com.albertsons.acupick.data.model.textValue
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.picklist.InvalidItemScanTracker
import com.albertsons.acupick.data.picklist.getQuantitySelectionType
import com.albertsons.acupick.data.picklist.getQuantitySelectionTypeForIssueScanning
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.DevOptionsRepository
import com.albertsons.acupick.data.repository.ItemProcessorRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.data.toast.Toaster
import com.albertsons.acupick.image.ImagePreCacher
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.combineOncePerChangeNoneZero
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.indexOfOrNull
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.infrastructure.utils.isValidActivityId
import com.albertsons.acupick.infrastructure.utils.orZero
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.bottomsheetdialog.ActionSheetOptions
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetArgDataAndTag
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetType
import com.albertsons.acupick.ui.bottomsheetdialog.CustomBottomSheetArgData
import com.albertsons.acupick.ui.dialog.ALREADY_ASSIGNED_PICKLIST_ARG_DATA
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.dialog.SHORT_ITEM_REASON_DIALOG
import com.albertsons.acupick.ui.dialog.SHORT_WINE_ITEM_REASON_DIALOG
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.dialog.getEbtNoBagsBatchWarningDialog
import com.albertsons.acupick.ui.dialog.getEbtNoBagsSingleWarningDialog
import com.albertsons.acupick.ui.dialog.getEbtWarningDialog
import com.albertsons.acupick.ui.dialog.getNoBagsBatchWarningDialog
import com.albertsons.acupick.ui.dialog.getNoBagsSingleWarningDialog
import com.albertsons.acupick.ui.itemdetails.ItemActionBackingType
import com.albertsons.acupick.ui.itemdetails.ItemDetailsViewModel
import com.albertsons.acupick.ui.manualentry.ManualEntryPickParams
import com.albertsons.acupick.ui.manualentry.ManualEntryType
import com.albertsons.acupick.ui.missingItemLocation.MissingItemLocationResultParams
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.models.AlternateLocationPath.Short
import com.albertsons.acupick.ui.models.AlternateLocationPath.Substitute
import com.albertsons.acupick.ui.models.MissingItemLocationParams
import com.albertsons.acupick.ui.models.PickListScannedData
import com.albertsons.acupick.ui.staging.winestaging.WineStagingParams
import com.albertsons.acupick.ui.substitute.BulkItem
import com.albertsons.acupick.ui.substitute.BulkSubstituteConfirmationParam
import com.albertsons.acupick.ui.substitute.SubstituteConfirmationParam
import com.albertsons.acupick.ui.substitute.SubstituteParams
import com.albertsons.acupick.ui.substitute.SubstitutionLocalItem
import com.albertsons.acupick.ui.substitute.SubstitutionPath
import com.albertsons.acupick.ui.substitute.SwapSubstitutionReason
import com.albertsons.acupick.ui.util.AnalyticsHelper
import com.albertsons.acupick.ui.util.DrawableIdHelper
import com.albertsons.acupick.ui.util.EventAction
import com.albertsons.acupick.ui.util.SnackAction
import com.albertsons.acupick.ui.util.SnackDuration
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.asItemLocation
import com.albertsons.acupick.ui.util.asUpcOrPlu
import com.albertsons.acupick.ui.util.formattedWeight
import com.albertsons.acupick.ui.util.getOrZero
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.sizedImageUrl
import com.hadilq.liveevent.LiveEvent
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.time.ZonedDateTime
import kotlin.math.round
import kotlin.math.roundToInt

const val SCAN_STATUS_MESSAGE_DURATION_MS = 2000L
const val TOTE_UI_COUNT = 5
const val locationReasonCode = "MISSING"

class PickListItemsViewModel(
    val app: Application,
    val activityViewModel: MainActivityViewModel
) : BaseViewModel(app) {

    // DI
    private val analyticsHelper: AnalyticsHelper by inject()
    val pickRepository: PickRepository by inject()
    private val apsRepo: ApsRepository by inject()
    private val userRepo: UserRepository by inject()
    private val itemProcessorRepo: ItemProcessorRepository by inject()
    private val conversationRepo: ConversationsRepository by inject()
    val siteRepo: SiteRepository by inject()
    private val devOptionsRepository: DevOptionsRepository by inject()
    private val imagePreCacher: ImagePreCacher by inject()
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    val dispatcherProvider: DispatcherProvider by inject()
    private val toaster: Toaster by inject()
    val barcodeMapper: BarcodeMapper by inject()
    private val invalidItemScanTracker: InvalidItemScanTracker by inject()
    private val firebaseAnalytics: FirebaseAnalyticsInterface by inject()
    private val moshi: Moshi by inject()

    // Data binding
    /** Source of truth */
    val pickList: LiveData<ActivityDto?> = pickRepository.pickList.asLiveData()

    private lateinit var itemActivityDto: ItemActivityDto

    private lateinit var shortSelection: ShortReasonCode

    /** item ID of the item that has just been picked--item complete animation is possibly playing */
    private val justCompletedItemIaId = MutableStateFlow<Long?>(null)

    /** Item ID of the item that has just been shorted--item complete animation is possibly playing */
    private val justShortedItemIaId = MutableStateFlow<Long?>(null)

    //  ISSUE-SCANNING variable to check issue scanning is in progress
    private val isIssueScanningInProgress = MutableStateFlow(false)

    val pendingSubstitutionCount = MutableStateFlow<String>("")
    val approvedSubstitutionCount = MutableStateFlow<String>("")
    val declinedOosSubstitutionCount = MutableStateFlow<String>("")
    private var pendingSubCountIntFormat = 0
    private var reviewChatClickCount = 0
    private var showTypingCount = 0
    private var isManuallyEntered = false

    /** List of items shown under To-do tab */
    val todoPickList =
        combine(pickRepository.pickList, justCompletedItemIaId, justShortedItemIaId, isIssueScanningInProgress) { pickList, justCompletedItemIaId, justShortedItemIaId, isIssueScanningInProgress ->
            pickList?.toPickListActivity(
                pickList.itemActivities?.filter { item ->
                    if (item.sellByWeightInd == SellByType.PriceWeighted) {
                        !item.isPickCompleted && !item.isShorted && !item.pickedUpcCodes?.any { it.isSubstitution() }.orFalse()
                    } else {
                        item.validateIsFullyPicked(isIssueScanningInProgress, justCompletedItemIaId).not()
                    } || item.id == justCompletedItemIaId || item.id == justShortedItemIaId
                }?.groupBy { it.groupBySeq.orEmpty() }.orEmpty()
            )
        }.asLiveData()

    /** List of items shown under Picked tab */
    val pickedPickList = pickRepository.pickList.combine(justCompletedItemIaId) { pickList, justCompletedItemIaId ->
        pickList?.toPickListActivity(
            pickList.itemActivities?.filter { item ->
                item.isPartiallyPicked() && item.id != justCompletedItemIaId
            }?.let { items ->
                items.filter { it.isIssueScanned }.groupBy { ITEM_ISSUE_REPORTED } +
                    items.filter { it.isSubstituted }.groupBy { ITEM_SUBSTITUTIONS } +
                    items.filter { !it.isSubstituted && !it.isIssueScanned }.groupBy { it.groupBySeq.orEmpty() }
            }.orEmpty()
        )
    }.asLiveData()

    /** List of items shown under Short tab */
    val shortPickList = pickList.map { activity ->
        activity?.toPickListActivity(
            activity.itemActivities?.filter { item ->
                item.isShorted
            }?.groupBy { it.groupBySeq.orEmpty() }.orEmpty()
        )
    }

    fun isBatch(): Boolean {
        return pickList.value?.erId == null
    }

    val todoItemCount = combine(pickList.asFlow(), isIssueScanningInProgress) { picklist, isIssueScanningInProgress ->
        picklist?.itemActivities?.sumOf { item ->
            when {
                item.isSubstituted || item.isShorted -> 0.0
                // ISSUE-SCANNING to-do count updates only after issue scanning completed
                item.isIssueScanned -> if (isIssueScanningInProgress) item.todoCountWithoutIssueReportedItem(currentItem?.id) else 0.0
                item.sellByWeightInd == SellByType.PriceWeighted -> if (!item.isPickCompleted) item.qty.orZero() - item.processedQty.orZero() else 0.0
                else -> item.qty.orZero() - item.processedQty.orZero()
            }
        }.orZero().roundToInt().toString()
    }.asLiveData()

    val pickedItemCount = combine(pickList.asFlow(), isIssueScanningInProgress) { picklist, isIssueScanningInProgress ->
        picklist?.itemActivities?.sumOf { item ->
            when {
                item.isSubstituted -> item.qty.orZero()
                // ISSUE-SCANNING picked count updates only after issue scanning completed
                item.isIssueScanned -> if (isIssueScanningInProgress) item.pickedCountWithoutIssueReportedItem(currentItem?.id) else item.qty.orZero()
                item.sellByWeightInd == SellByType.PriceWeighted -> if (item.isPickCompleted) item.processedQty.orZero() else 0.0
                else -> item.processedQty.orZero()
            }
        }.orZero().roundToInt().toString()
    }.asLiveData()

    /** Unit count in Short tab */
    val shortItemCount = pickList.map {
        it?.itemActivities?.sumOf { item ->
            item.exceptionQty.orZero()
        }.orZero().roundToInt().toString()
    }

    /** Current assignedUserId */
    private val assignedUserId = pickList.map {
        it?.assignedTo?.userId
    }

    fun isMultiSource(): Boolean {
        return pickList.value?.isMultiSource ?: false
    }

    fun isWineShipping(): Boolean = pickList.value?.isWineOrder() ?: false

    val isDataLoading: LiveData<Boolean> = MutableLiveData(true)
    val currentTab: LiveData<PickListType> = MutableLiveData(PickListType.Todo)
    var currentItem: ItemActivityDto? = null
    val isListView: LiveData<Boolean> = MutableLiveData(true)
    var isItemDetailBottomSheetShowing = false
    val prompt: LiveData<PickListItemsBottomPrompt> = MutableLiveData(PickListItemsBottomPrompt.None)
    val isShowingEmptyState = combine(
        currentTab.asFlow(),
        todoPickList.asFlow(),
        pickedPickList.asFlow(),
        shortPickList.asFlow()
    ) { currentTab, todoPickList, pickedPickList, shortPickList ->
        when (currentTab) {
            PickListType.Todo -> todoPickList
            PickListType.Picked -> pickedPickList
            PickListType.Short -> shortPickList
        }?.itemActivities?.isEmpty() == true
    }.asLiveData()

    val emptyStateText = currentTab.map { currentTab ->
        when (currentTab) {
            PickListType.Todo -> app.getString(R.string.no_todos)
            PickListType.Picked -> app.getString(R.string.no_picked)
            PickListType.Short -> app.getString(R.string.no_shorted)
        }
    }

    /**
     * When true, informational prompts for item/tote are shown. When false, a transient success/error action is being displayed
     *
     * ### Example Informational messages/prompts
     * * Scan your first item / Weigh item and scan barcode / Tap keyboard to enter quantity
     * * Scan next item
     * * Scan tote/Scan empty tote
     * * Scan Tote TTC01 or new tote
     * * Invalid Tote Scanned
     *
     * ### Example Success/Error Actions
     * * Item 012345678901 scanned (Scan item success)
     * * Wrong item scanned (Scan item failure)
     * * Added to Tote TTC01 (Scan tote success)
     * * Wrong tote scanned (Scan tote failure)
     */
    private val isShowingScanPrompt: LiveData<Boolean> = MutableLiveData(true)
    private val isScanSuccess: LiveData<Boolean> = MutableLiveData(true)

    // Events
    val scrollToPosition: LiveData<Int> = LiveEvent()
    val playScanSound: LiveData<Boolean> = LiveEvent()
    val unAssignSuccessfulAction: LiveData<Unit> = LiveEvent()
    val assignedToWrongUserAction: LiveData<Unit> = LiveEvent()

    // State
    private lateinit var lastItemShorted: ItemActivityDto
    private var fulfilledQtyResult: FulfilledQuantityResult = FulfilledQuantityResult.DefaultQuantity

    private lateinit var pickListId: String
    private var savedReassignmentScannedItem: ScannedPickItem? = null
    private var savedReassignmentTote: PickingContainer? = null
    private var isPrintingSuccessful = false
    private var lastScannedItem: ScannedPickItem? = null
    var lastItemBarcodeScanned: BarcodeType.Item? = null
    private var lastToteBarcodeScanned: PickingContainer? = null
    private var savedToteBarcodeScanned: PickingContainer? = null

    // private var lastSubstitutedItem: ItemActivityDto? = null // TODO: There is no use of this variable will remove after QA
    private var toteEstimate: ToteEstimate? = null // Tote estimate for MFC order used to show required tote types
    private var suggestedToteId: String? = null
    var isFromManualEntry = false
    var canAcceptScan = true
    private var hadScanIssue = false
    private var lastItemInTodoListShorted = false
    val scannedItemFailure = MutableLiveData(false)
    var lastScannedItemDetails: ItemDetailDto? = null

    private var selectedVariant: BulkItem? = null
    val bulkVariants = ArrayList<BulkItem>()

    // ISSUE-SCANNING issue scanning scanned item list
    private val issueScannedListHolder = mutableListOf<SubstitutionLocalItem>()
    private var issueScannedItemToRemove: SubstitutionLocalItem? = null

    /** True when code paths to execute the complete pick api currently being run. False when outside that code block/logic */
    private var completeCallInProgress = false

    /** True when the complete pick api call returns a successful result */
    private var completeCallSuccessful = false

    /** True when missing item location updated through scanning flow */
    private var isLocationUpdated = false

    /** Tracks whether an item or tote is expected to be scanned next */
    private val activeScanTarget: LiveData<ScanTarget> = MutableLiveData(ScanTarget.Item)

    /** if scan target is not equal to ITEM, pick list is not enabled **/
    val pickListEnabled = activeScanTarget.map { it == ScanTarget.Item }

    /** Only show the Manual Entry Fab if on the to-do page and the scan targe is item */
    val isManualEntryVisible = combine(currentTab.asFlow(), activeScanTarget.asFlow()) { currentTab, activeScanTarget ->
        currentTab == PickListType.Todo && activeScanTarget == ScanTarget.Item
    }.asLiveData()

    val printLableButtonEnabled = pickRepository.countDownTimer
        .map { it == 0 }
        .asLiveData()

    val printLableText = combine(pickRepository.countDownTimer, areAllItemsShortedLiveData.asFlow()) { timer, allShorted ->
        val min = timer / 60
        val sec = timer % 60
        if (timer == 0) {
            if (allShorted) {
                app.getString(R.string.exit_pick)
            } else {
                app.getString(R.string.print_tote_labels)
            }
        } else {
            if (allShorted) {
                app.getString(R.string.chat_timer_exit, min.toString(), sec.toString())
            } else {
                app.getString(R.string.chat_timer, min.toString(), sec.toString())
            }
        }
    }.asLiveData()

    /** Represents the index of the item currently shown/selected in To-do list */
    private val _todoSelectedItemIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val todoSelectedItemIndex: StateFlow<Int>
        get() = _todoSelectedItemIndex

    /** Represents the index of the item currently shown/selected in Picked list */
    private val _pickedSelectedItemIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val pickedSelectedItemIndex: StateFlow<Int>
        get() = _pickedSelectedItemIndex

    /** Represents the index of the item currently shown/selected in Short list */
    private val _shortSelectedItemIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val shortSelectedItemIndex: StateFlow<Int>
        get() = _shortSelectedItemIndex

    private val isAnyItemPicked: Boolean
        get() = pickList.value?.itemActivities?.any { item -> item.isPartiallyPicked() } == true

    private val areAllItemsPicked: Boolean
        get() = pickList.value?.itemActivities?.isNotEmpty() ?: false &&
            (
                pickList.value?.itemActivities?.all { item ->
                    if (item.sellByWeightInd == SellByType.PriceWeighted) item.isPickCompleted
                    else item.validateIsFullyPicked(isIssueScanningInProgress.value, justCompletedItemIaId.value)
                } ?: false
                )
    val isTwoWayCommsEnabled = siteRepo.twoWayCommsFlags?.realTimeSubstitutions.orFalse()
    val isRepickOriginalItemAllowed = siteRepo.twoWayCommsFlags?.allowRepickOriginalItem.orFalse() // Re-pick original item: added safe call ? to avoid test cases failure
    val isDisplayType3PWEnabled = siteRepo.isDisplayType3PWEnabled
    val areAllItemsShorted: Boolean
        get() = pickList.value?.itemActivities?.all { item -> item.isFullyShorted() } == true

    val areAllItemsShortedLiveData: LiveData<Boolean>
        get() = pickList.map { it?.itemActivities?.all { item -> item.isFullyShorted() } == true }

    private val pickListObserver: Observer<ActivityDto?> = Observer<ActivityDto?> {
        if (it != null) {
            showStagingTimeOnTitle(it.stageByTime(), it.orderType, siteRepo.concernTime, siteRepo.warningTime, it.releasedEventDateTime, it.expectedEndTime, it.prePickType.isAdvancePickOrPrePick())
            updatePickingStatus()
            updateSubstitutionCount()
        }
    }

    private var checkApprovalJob: Job? = null

    /** Select first item of the To-do list if last item was selected and was removed (after being completely picked) */
    private val todoPickListObserver: Observer<PickListActivity?> = Observer<PickListActivity?> {
        viewModelScope.launch {
            if (todoSelectedItemIndex.value >= (it?.itemActivities?.lastIndex ?: 0)) {
                if (currentTab.value == PickListType.Todo) {
                    if (it?.itemActivities?.lastOrNull()?.validateIsFullyPicked() == true &&
                        it.itemActivities.lastOrNull()?.sellByWeightInd != SellByType.PriceWeighted
                    ) {
                        delay(SCAN_STATUS_MESSAGE_DURATION_MS)
                        if (lastItemInTodoListShorted) {
                            lastItemInTodoListShorted = false
                        }
                        scrollToPosition.set(0)
                    }
                } else {
                    _todoSelectedItemIndex.value = 0
                }
            }
        }
    }

    /** Select last item of the Picked list if last item was selected and was removed (after being completely unpicked) */
    private val pickedPickListObserver: Observer<PickListActivity?> = Observer<PickListActivity?> {
        if (pickedSelectedItemIndex.value >= (it?.itemActivities?.size ?: 0)) {
            _pickedSelectedItemIndex.value = it?.itemActivities?.size ?: 0
        }
    }

    /** Select last item of the Short list if last item was selected and was removed (after being un-shorted) */
    private val shortPickListObserver: Observer<PickListActivity?> = Observer<PickListActivity?> {
        if (shortSelectedItemIndex.value >= (it?.itemActivities?.size ?: 0)) {
            _shortSelectedItemIndex.value = it?.itemActivities?.size ?: 0
        }
    }

    private val selectedItemIndexObserver: Observer<Int> = Observer<Int> {
        val selectedChanged = todoPickList.value?.itemActivities?.getOrNull(it) != lastScannedItem?.item
        showPersistentSnackbarPrompt(fromSelectedItemChange = selectedChanged)
    }

    private val viewSwitcherObserver = Observer(::changeRightToolbarImage)

    private fun changeRightToolbarImage(isList: Boolean = isListView.value == true) {
        val drawable = if (isList) R.drawable.ic_view_switcher_list else R.drawable.ic_view_switcher_card
        changeToolbarRightFirstExtraImageEvent.postValue(DrawableIdHelper.Id(drawable))
    }

    val showUnreadMessages = MutableLiveData<Boolean>(false)
    val showChatButton = MutableLiveData<Boolean>(false)

    init {
        clearToolbarEvent.postValue(Unit)
        pickList.observeForever(pickListObserver)
        todoPickList.observeForever(todoPickListObserver)
        pickedPickList.observeForever(pickedPickListObserver)
        shortPickList.observeForever(shortPickListObserver)
        isListView.observeForever(viewSwitcherObserver)

        changeToolbarRightSecondExtraImageEvent.postValue(DrawableIdHelper.Id(R.drawable.ic_picking_info))

        viewModelScope.launch(dispatcherProvider.IO) {
            toolbarRightFirstImageEvent.asFlow().collect {
                changeView(!(isListView.value ?: true))
            }
        }

        viewModelScope.launch(dispatcherProvider.IO) {
            toolbarRightSecondImageEvent.asFlow().collect {
                onTotesCtaClicked()
            }
        }

        viewModelScope.launch {
            printLableButtonEnabled.asFlow().distinctUntilChanged().combine(isShowingEmptyState.asFlow().distinctUntilChanged()) { enabled, isEmptyScreenShown ->
                Timber.d("approve isShowingEmptyStateFlow: $enabled == $isEmptyScreenShown")
                if (!enabled && isEmptyScreenShown && currentTab.value == PickListType.Todo) {
                    scheduleApiCall()
                }
            }.collect()
        }

        viewModelScope.launch(dispatcherProvider.IO) {
            conversationRepo.showChatButton.collectLatest {
                showChatButton.postValue(it)
            }
        }
        viewModelScope.launch(dispatcherProvider.IO) {
            // for the first trigger of todoPickList when empty the value of pickRepository.countDownTimer is always
            // null due to the delay, so wait for a non null value of countDownTimer when the picklist is complete
            // to capture the remaining staging blocking time
            todoPickList.asFlow().combineOncePerChangeNoneZero(pickRepository.countDownTimer) { pickList, countDownTimer ->
                if (pickList?.itemActivities?.isEmpty() == true) {
                    val errorData = ChatErrorData(
                        stagingBlockedTime = countDownTimer,
                        orderNumbers = pickList.listOfOrderNumber
                    ).toJsonString(moshi)
                    conversationRepo.sendLog(
                        ApiResult.Failure.GeneralFailure(errorData, networkCallName = NetworkCalls.SUBSTITUTION_OSS_BLOCK_TIMER_STARTED.value),
                        false
                    )
                }
            }.collect()
        }

        // Whenever the item list is scrolled then selectedItemIndex is changed.
        // If the scan prompt is showing, show scan target.
        // If the scan prompt is NOT showing, then do nothing here to prevent prematurely hiding the success/error action pill being displayed.
        // The success/error actions show a short period of time then show the scan prompt afterwards already.
        todoSelectedItemIndex.filter { isShowingScanPrompt.value == true }.asLiveData().observeForever(selectedItemIndexObserver)

        registerCloseAction(RELOAD_LOAD_PICKLIST_DIALOG_TAG) { closeActionFactory(positive = { loadPickList(pickListId) }) }
        registerCloseAction(COMPLETE_PICKING_EARLY_EXIT_DIALOG_TAG) { closeActionFactory(positive = { showEndPickReasonDialog() }) }

        registerCloseAction(END_PICK_WITH_EXCEPTIONS_DIALOG_TAG) { closeActionFactory(positive = { showEndPickReasonDialog() }) }

        registerCloseAction(END_PICK_REASON_DIALOG_TAG) {
            closeActionFactory(positive = { selection ->
                val endPickReasonCode = getSelectedEndPickReason((selection as Int))
                when (isWineShipping()) {
                    true -> completeWinePicking(completeWithExceptions = true, endPickReasonCode = endPickReasonCode)
                    else -> completePicking(completeWithExceptions = true, endPickReasonCode = endPickReasonCode)
                }
            })
        }

        registerCloseAction(COMPLETE_WINE_PICKING_EARLY_EXIT_DIALOG_TAG) { closeActionFactory(positive = { showEndPickReasonDialog() }) }
        registerCloseAction(END_PICK_DIALOG_TAG) { closeActionFactory(positive = { unAssignPicker() }) }
        registerCloseAction(LABEL_SENT_TO_PRINTER_DIALOG_TAG) { closeActionFactory(positive = { onLabelSentToPrinter(activityId = pickListId) }) }
        registerCloseAction(COMPLETED_PICK_DIALOG_TAG) { closeActionFactory(positive = { completePicking(completeWithExceptions = false) }) }
        registerCloseAction(COMPLETE_PICK_ERROR_DIALOG_TAG) { closeActionFactory() }
        registerCloseAction(CONTAINER_REASSIGNMENT_DIALOG_TAG) {
            closeActionFactory(
                positive = { reAssignContainer() },
                dismiss = { handleContainerReassignmentRefusal() },
                negative = { handleContainerReassignmentRefusal() }
            )
        }
        registerCloseAction(RETRY_UNASSIGN_PICKER_DIALOG_TAG) { closeActionFactory(positive = { unAssignPicker() }) }
        registerCloseAction(COMMENT_DIALOG_TAG) { closeActionFactory() }
        registerCloseAction(RETRY_RECORD_PICK_DIALOG_TAG) { closeActionFactory(positive = { handleRecordPickRetry() }) }
        registerCloseAction(CONTAINER_CANNOT_REASSIGN_DIALOG_TAG) {
            closeActionFactory(positive = { showPersistentSnackbarPrompt() })
        }
        registerCloseAction(PICK_ASSIGNED_TO_DIFFERENT_USER_TAG) {
            closeActionFactory(positive = { assignedToWrongUserAction.postValue(Unit) })
        }
        registerCloseAction(QUANTITY_PICKER_PICK_DIALOG_TAG) {
            closeActionFactory(
                positive = { result ->
                    handleFulfilledQuantityResult(
                        FulfilledQuantityResult.QuantityPicker(result ?: 0)
                    )
                },
                dismiss = {
                    returnToInitialState()
                },
                negative = {
                    returnToInitialState()
                }
            )
        }

        registerCloseAction(BULK_VARIANT_BOTTOM_SHEET) {
            closeActionFactory(
                positive = { result ->
                    canAcceptScan = true
                    selectedVariant = bulkVariants.find { it.itemId == result.toString() }
                    lastItemBarcodeScanned?.let { barcode ->
                        lastScannedItem?.let { scanned ->
                            canAcceptScan = false
                            selectedVariant?.let { inlineBottomSheetEvent.postValue(getQuantityPickerArgDataAndTagForBulkBottomSheet(app, scanned, barcode, it, hadScanIssue)) }
                        }
                    }
                },
                dismiss = {
                    returnToInitialState()
                },
                negative = {
                    returnToInitialState()
                }
            )
        }
        registerCloseAction(SCAN_ISSUE_REPORTED_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    viewModelScope.launch {
                        hadScanIssue = true
                        canAcceptScan = true
                        lastItemBarcodeScanned?.let {
                            handleScannedItem(it)
                        }
                    }
                },
                dismiss = { returnToInitialState() },
                negative = { returnToInitialState() }
            )
        }
        registerCloseAction(ALTERNATIVE_LOCATION_DIALOG_TAG) {
            closeActionFactory(
                positive = { selection ->
                    canAcceptScan = true
                    when (selection) {
                        0 -> recordShortage(ShortReasonCode.OUT_OF_STOCK)
                        1 -> continueToSubstitution()
                        else -> Unit
                    }
                },
                dismiss = { canAcceptScan = true },
                negative = { canAcceptScan = true }
            )
        }

        registerCloseAction(EBT_WARNING_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    navigateToSubstitution()
                    canAcceptScan = true
                }
            )
        }

        registerCloseAction(INITIAL_EBT_NO_BAGS_WARNING_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    canAcceptScan = true
                }
            )
        }

        registerCloseAction(SYNC_FAILED_DIALOG_TAG) {
            serverErrorListener
        }

        registerCloseAction(SHORT_ITEM_OOS_WARNING) {
            closeActionFactory(
                positive = {
                    makeShortReasonSelection(0)
                    canAcceptScan = true
                },
                dismiss = { canAcceptScan = true },
                negative = { canAcceptScan = true }
            )
        }

        registerCloseAction(CONFIRM_AMOUNT_BOTTOMSHEET_TAG) {
            closeActionFactory(
                dismiss = { returnToInitialState() }
            )
        }

        registerCloseAction(ORDERED_BY_WEIGHT_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    viewModelScope.launch(dispatcherProvider.Default) {
                        canAcceptScan = true
                        lastItemBarcodeScanned?.let {
                            handleScannedItem(it, false)
                        }
                    }
                },
                negative = { returnToInitialState() },
                dismiss = { returnToInitialState() }
            )
        }

        registerCloseAction(ORDERED_BY_WEIGHT_MANUAL_ENTRY_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    handleManualEntryResults(fulfilledQtyResult, lastItemBarcodeScanned, false)
                },
                negative = { returnToInitialState() },
                dismiss = { returnToInitialState() }
            )
        }
        registerCloseAction(TOTE_SCAN_BOTTOMSHEET_TAG) {
            closeActionFactory(
                /**
                 * Handle tote scan action in case of continue issue scanning to post updated issue scanning list
                 * to the issue scanning confirmation bottom after swip down the tote bottom sheet
                 */
                positive = {
                    if (hadScanIssue) {
                        viewModelScope.launch(dispatcherProvider.Main) {
                            // delay(500)
                            // Send the live data event to issue scanning confirmation bottomsheet
                            activityViewModel.bottomSheetRecordPickArgData.postValue(getIssueScanningConfirmationArgData())
                        }
                    }
                },
                dismiss = {
                    returnToInitialState()
                }
            )
        }

        registerCloseAction(ITEM_DETAIL_BOTTOMSHEET_TAG) {
            closeActionFactory(
                positive = {
                    isItemDetailBottomSheetShowing = false
                    onLabelClicked()
                },
                negative = {
                    isItemDetailBottomSheetShowing = false
                    viewModelScope.launch {
                        delay(500)
                        onManualEntryCtaClicked()
                    }
                },
                dismiss = { isItemDetailBottomSheetShowing = false }
            )
        }

        registerCloseAction(ItemDetailsViewModel.CONFIRMATION_UNDO_SHORT_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    activityViewModel.bottomSheetRecordPickArgData.postValue(getDismissItemDetailBottomSheetArgDataAndTag())
                    viewModelScope.launch(dispatcherProvider.IO) {
                        isBlockingUi.wrap {
                            undoShorts()
                        }
                    }
                },
                dismiss = { }
            )
        }

        registerCloseAction(ItemDetailsViewModel.RETRY_AUTO_SHORT_DIALOG) {
            closeActionFactory(positive = { viewModelScope.launch(dispatcherProvider.IO) { handleItemsWithShorts() } })
        }

        registerCloseAction(IS_TROUBLE_SCANNING_DIALOG) {
            closeActionFactory(
                positive = {
                    viewModelScope.launch(dispatcherProvider.Main) {
                        delay(500)
                        currentItem?.let { inlineDialogEvent.postValue(getConfirmItemSameArgDataAndTag(it, siteRepo.isDisplayType3PWEnabled, it.asUpcOrPlu(app, barcodeMapper))) }
                    }
                },
                negative = { returnToInitialState() },
                dismiss = { returnToInitialState() }
            )
        }

        registerCloseAction(CONFIRM_ITEM_SAME_DIALOG_TAG) {
            closeActionFactory(
                positive = { onConfirmItemScanErrorCtaClicked() },
                negative = { returnToInitialState() },
                dismiss = { returnToInitialState() }
            )
        }

        // ISSUE-SCANNING receive event from delete issue scanning dialog
        registerCloseAction(DELETE_ISSUE_SCANNING_ITEM_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    deleteIssueScannedItem()
                }
            )
        }

        registerCloseAction(ISSUE_SCANNING_CONFIRMATION_BOTTOM_SHEET_TAG) {
            closeActionFactory(
                // Positive action received on confirm issue scanning completion
                positive = {
                    // Issue scanning complete swipe down the confirm pick bottom sheet
                    clearIssueScanningData()
                },
                // Negative action received to add multiple issue scanning item
                negative = {
                    // Handle add another issue scanning to open scan an item bottom sheet
                    canAcceptScan = true
                    openScanItemBottomSheet()
                },
                // Dismiss action received on remove all the issue scanned item
                dismiss = {
                    returnToInitialState()
                    // On exit of issue scanning bottomsheet, open the last clicked item detail bottom sheet in listview mode only as same as coming back from substitution screen
                    viewModelScope.launch(dispatcherProvider.Main) {
                        delay(500)
                        openLastSelectedItemDetailBottomSheet()
                    }
                },
            )
        }

        // Handle actions from issue scanning item bottom sheet
        registerCloseAction(ISSUE_SCAN_ITEM_BOTTOM_SHEET_TAG) {
            closeActionFactory(
                negative = {
                    viewModelScope.launch(dispatcherProvider.Main) {
                        delay(500)
                        onManualEntryCtaClicked()
                    }
                },
                dismiss = {
                    canAcceptScan = false
                }
            )
        }
        // Handle action from exit issue scanning dialog to remove all the issue scanned items
        registerCloseAction(EXIT_ISSUE_SCANNING_DIALOG_TAG) {
            closeActionFactory(positive = { unpickAllIssueScannedItems() })
        }
        /**
         * Pre-Pick handled Picker can mark short of any item in pre pick list without navigating
         * to substitute screen therefore we are handling short reason code here from action sheet.
         */
        registerCloseAction(SHORT_ITEM_ACTION_SHEET_TAG) {
            closeActionFactory(
                positive = {
                    viewModelScope.launch(dispatcherProvider.Main) {
                        delay(100)
                        val dialogArgData = when ((shortReasonOptions[it as Int].settingsString)) {
                            R.string.short_prep_not_ready -> {
                                shortSelection = ShortReasonCode.PREP_NOT_READY
                                getShortItemConfirmationDialogArgData(R.string.mark_as_not_ready)
                            }

                            R.string.short_tote_full -> {
                                shortSelection = ShortReasonCode.TOTE_FULL
                                getShortItemConfirmationDialogArgData(R.string.mark_as_tote_full)
                            }

                            R.string.short_out_of_stock -> {
                                shortSelection = ShortReasonCode.OUT_OF_STOCK
                                if (shouldShowCustomerNotifyMessage()) {
                                    getShortItemConfirmationDialogArgData(
                                        R.string.do_you_want_to_mark_this_item_as_out_of_stock,
                                        R.string.short_item_dialog_body_text
                                    )
                                } else {
                                    getShortItemConfirmationDialogArgData(
                                        R.string.do_you_want_to_mark_this_item_as_out_of_stock,
                                    )
                                }
                            }

                            else -> null
                        }
                        dialogArgData?.let { inlineDialogEvent.postValue(CustomDialogArgDataAndTag(data = dialogArgData, tag = SHORT_ITEM_REASON_TAG)) }
                    }
                },
                negative = {
                    canAcceptScan = true
                    openLastSelectedItemDetailBottomSheet()
                },
                dismiss = { canAcceptScan = true }
            )
        }

        registerCloseAction(SHORT_ITEM_REASON_TAG) {
            closeActionFactory(
                positive = {
                    when (shortSelection) {
                        ShortReasonCode.OUT_OF_STOCK -> recordShortage(ShortReasonCode.OUT_OF_STOCK)
                        ShortReasonCode.TOTE_FULL -> recordShortage(ShortReasonCode.TOTE_FULL)
                        ShortReasonCode.PREP_NOT_READY -> recordShortage(ShortReasonCode.PREP_NOT_READY)
                        else -> Unit
                    }
                },
                negative = {
                    canAcceptScan = true
                },
                dismiss = {
                    canAcceptScan = true
                }
            )
        }

        registerCloseAction(PICK_LATER_DIALOG) {
            closeActionFactory(
                positive = {
                    recordShortage(ShortReasonCode.PICK_LATER)
                },
                negative = {
                    canAcceptScan = true
                    openLastSelectedItemDetailBottomSheet() // To open item detail bottom sheet if it was opened.
                },
                dismiss = { canAcceptScan = true }
            )
        }

        registerCloseAction(PICK_LATER_ISSUE_SCANNING_DIALOG) {
            closeActionFactory(
                positive = {
                    recordShortage(ShortReasonCode.PRE_PICK_ISSUE_SCANNING)
                },
                negative = {
                    canAcceptScan = true
                    openLastSelectedItemDetailBottomSheet() // To open item detail bottom sheet if it was opened.
                },
                dismiss = { canAcceptScan = true }
            )
        }

        registerCloseAction(PRINT_TOTE_LABELS_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    reviewChatClickCount++
                    activityViewModel.blockStagingHandleEvent.value = true
                },
                negative = { startEndPick(true) }
            )
        }

        registerCloseAction(BLOCK_STAGING_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    activityViewModel.blockStagingHandleEvent.value = true
                },
                negative = {}
            )
        }
        registerCloseAction(CUSTOMER_TYPING_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    activityViewModel.blockStagingHandleEvent.value = true
                },
                negative = {}
            )
        }

        registerCloseAction(REMOVE_SUBSTITUTION_DIALOG_TAG) {
            closeActionFactory(
                positiveWithData = {
                    viewModelScope.launch(dispatcherProvider.IO) {
                        activityViewModel.bottomSheetRecordPickArgData.postValue(getDismissItemDetailBottomSheetArgDataAndTag())
                        undoPickSubstitutedItem()
                    }
                }
            )
        }
    }

    private fun scheduleApiCall() {
        if (checkApprovalJob?.isActive == true) return

        Timber.d("approve scheduleApiCall")
        checkApprovalJob = viewModelScope.launch {
            while (pickRepository.countDownTimer.value != 0) {
                delay(30000) // 30 seconds
                Timber.d("approve delay outside")
                if (isShowingEmptyState.value == true) {
                    Timber.d("approve delay inside")
                    pickRepository.getActivityDetails(pickListId)
                }
            }
        }
    }

    fun clearIssueScanningData() {
        issueScannedListHolder.clear()
        justCompletedItemIaId.value = null
        returnToInitialState()
        viewModelScope.launch(dispatcherProvider.Main) {
            delay(500)
            showSnackBar(
                AcupickSnackEvent(
                    message = if (siteRepo.twoWayCommsFlags.chatBeta == true) {
                        StringIdHelper.Id(R.string.issue_scanning_completed_customer_notified)
                    } else {
                        StringIdHelper.Id(R.string.issue_scanning_completed)
                    },
                    type = SnackType.SUCCESS
                )
            )
        }
    }

    private fun showSnackBar(message: StringIdHelper, type: SnackType) {
        showAnchoredSnackBar(AcupickSnackEvent(message = message, type = type))
    }

    private suspend fun undoShorts() {
        // bail early on empty list
        if (currentItem?.shortedItemUpc.isNullOrEmpty()) return

        val requests = currentItem?.shortedItemUpc?.map { shortedItemUpcDto ->
            UndoShortRequestDto(
                actId = pickList.value?.actId ?: 0,
                iaId = currentItem?.id,
                shortedItemId = shortedItemUpcDto.shortedId,
                qty = shortedItemUpcDto.exceptionQty
            )
        }
        activityViewModel.setLoadingState(true)
        val result = requests?.let { pickRepository.undoShortages(it) }
        activityViewModel.setLoadingState(false)

        delay(500)
        if (result is ApiResult.Failure) {
            showAnchoredSnackBar(
                AcupickSnackEvent(
                    message = StringIdHelper.Id(R.string.item_details_undo_error),
                    type = SnackType.ERROR
                )
            )
        } else {
            showAnchoredSnackBar(
                AcupickSnackEvent(
                    message = StringIdHelper.Plural(R.plurals.items_moved_to_todo, currentItem?.shortedItemUpc?.sumOf { it.exceptionQty?.toInt() ?: 0 } ?: 1),
                    type = SnackType.SUCCESS
                )
            )
        }
    }

    fun onChatClicked(orderNumber: String) {
        viewModelScope.launch {
            val picklist = pickRepository.pickList.value ?: return@launch
            val orderChatDetail = picklist.orderChatDetails?.firstOrNull { it.customerOrderNumber == orderNumber } ?: return@launch

            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    PickListItemsFragmentDirections.actionPickListItemsFragmentToChatFragment(
                        orderNumber = orderNumber,
                        convetsationId = orderChatDetail.conversationSid ?: conversationRepo.getConversationId(orderNumber),
                        fulfullmentOrderNumber = orderChatDetail.referenceEntityId.orEmpty()
                    )
                )
            )
        }
    }

    fun prepareForUndoPick(checkedItems: List<ItemActionBackingType>?) {
        val undoPickList = checkedItems.orEmpty().filterIsInstance<ItemActionBackingType.Pick>().map { it.pickedItemUpcDto }
        val undoSubList = checkedItems.orEmpty().filterIsInstance<ItemActionBackingType.Substitution>().map { it.pickedItemUpcDto }
        viewModelScope.launch(dispatcherProvider.IO) {
            isBlockingUi.wrap {
                undoPicks(undoPickList + undoSubList)
            }
        }
    }

    // ISSUE-SCANNING show confirmation dialog before delete issue scanned item
    fun showDeleteIssueScannedItemDialog(item: SubstitutionLocalItem) {
        issueScannedItemToRemove = item
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = getShortItemConfirmationDialogArgData(R.string.issue_scanning_remove_item_dialog_title),
                tag = DELETE_ISSUE_SCANNING_ITEM_DIALOG_TAG
            )
        )
    }

    fun showExitIssueScanningConfirmationBottomSheet() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.exit_issue_scanning_dialog_title),
                    body = StringIdHelper.Id(R.string.exit_issue_scanning_dialog_body),
                    positiveButtonText = StringIdHelper.Id(R.string.confirm),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelable = false,
                    cancelOnTouchOutside = false
                ),
                tag = EXIT_ISSUE_SCANNING_DIALOG_TAG
            )
        )
    }

    // Unpick selected issue scanned item from confirm pick bottom sheet
    private fun deleteIssueScannedItem() {
        issueScannedItemToRemove?.let {
            viewModelScope.launch {
                val undoPickLocalDtoList = listOf(
                    getUndoPickLocalDTORequest(it)
                )
                when (isBlockingUi.wrap { pickRepository.undoPicks(undoPickLocalDtoList) }) {
                    is ApiResult.Success -> {
                        resetAfterDeleteIssueScannedItem()
                    }

                    is ApiResult.Failure -> Unit
                    else -> {}
                }
            }
        }
    }

    // Unpick all the issue scanned item from confirm pick bottom sheet
    private fun unpickAllIssueScannedItems() {
        viewModelScope.launch {
            val undoPicks = mutableListOf<UndoPickLocalDto>()
            issueScannedListHolder.forEach {
                if (it.toteBarcodeType != null) {
                    undoPicks.add(getUndoPickLocalDTORequest(it))
                }
            }
            val results = isBlockingUi.wrap {
                pickRepository.undoPicks(undoPicks.toList())
            }

            if (results is ApiResult.Failure) {
                withContext(dispatcherProvider.Main) {
                    toaster.toast(app.getString(R.string.item_details_undo_error))
                }
            } else {
                // Close issue scanning confirmation bottomsheet only
                withContext(dispatcherProvider.Main) {
                    issueScannedListHolder.clear()
                    activityViewModel.bottomSheetRecordPickArgData.postValue(getIssueScanningConfirmationArgData())
                }
            }
        }
    }

    private fun getUndoPickLocalDTORequest(item: SubstitutionLocalItem): UndoPickLocalDto {
        return UndoPickLocalDto(
            containerId = item.toteBarcodeType?.rawBarcode,
            undoPickRequestDto = UndoPickRequestDto(
                actId = pickListId.toLong(),
                iaId = currentItem?.id,
                // Only PS item type required weighted while undoPick. Follow same process as substitution flow during undoPick
                netWeight = if (currentItem?.sellByWeightInd == SellByType.PriceScaled) currentItem?.netWeight else null,
                pickedUpcId = pickList.value?.itemActivities?.find { it.id == currentItem?.id }?.pickedUpcCodes?.find { pickedItemUpcDto ->
                    (
                        pickedItemUpcDto.upc == item.itemBarcodeType?.rawBarcode ||
                            pickedItemUpcDto.upc == item.itemBarcodeType?.catalogLookupUpc ||
                            pickedItemUpcDto.upc == item.itemBarcodeType?.getBarcodeToSendToBackend()
                        ) && pickedItemUpcDto.containerId == item.toteBarcodeType?.rawBarcode
                }?.upcId,
                qty = item.quantity
            )
        )
    }

    private fun resetAfterDeleteIssueScannedItem() {
        issueScannedListHolder.remove(issueScannedItemToRemove)
        activityViewModel.bottomSheetRecordPickArgData.postValue(getIssueScanningConfirmationArgData())
    }

    private suspend fun undoPicks(pickedItemUpcDtoList: List<PickedItemUpcDto>) {
        // bail early on empty list
        if (pickedItemUpcDtoList.isEmpty()) return

        val requests = pickedItemUpcDtoList.map { pickedItem ->
            UndoPickLocalDto(
                containerId = pickedItem.containerId,
                undoPickRequestDto = UndoPickRequestDto(
                    actId = pickList.value?.actId ?: 0,
                    iaId = currentItem?.id,
                    netWeight = pickedItem.netWeight,
                    pickedUpcId = pickedItem.upcId,
                    qty = pickedItem.qty
                )
            )
        }

        activityViewModel.setLoadingState(true)
        val results = pickRepository.undoPicks(requests)
        activityViewModel.setLoadingState(false)

        if (results is ApiResult.Failure) {
            showSnackBar(StringIdHelper.Id(R.string.item_details_undo_error), SnackType.ERROR)
        } else {
            handleItemsWithShorts(pickedItemUpcDtoList.sumOf { it.qty?.toInt() ?: 0 })
        }
    }

    private suspend fun handleItemsWithShorts(numberOfItemsUnPicked: Int = 1) {
        if (currentItem?.isShorted == true) {
            // Getting the updated value(including currently unpicked) of current item from picklist
            val currentItem = pickList.value?.itemActivities?.find { it.id == currentItem?.id }
            val shortRequesCount = currentItem?.qty.orZero().minus(currentItem?.processedAndExceptionQty.orZero())

            activityViewModel.setLoadingState(true)
            val results = pickRepository.recordShortage(
                ShortPickRequestDto(
                    actId = pickRepository.pickList.first()?.actId,
                    shortReqDto = createShortRequestDto(shortRequesCount.orZero())
                )
            )
            activityViewModel.setLoadingState(false)
            when (results) {
                is ApiResult.Success -> {
                    showSnackBar(StringIdHelper.Id(R.string.item_details_auto_short_toast), SnackType.SUCCESS)
                }

                is ApiResult.Failure -> {
                    acuPickLogger.e("autoShort failed API: $results")
                    showFailedToAutoShortDialog()
                }
            }.exhaustive
        } else {
            showSnackBar(StringIdHelper.Plural(R.plurals.items_moved_to_picklist, numberOfItemsUnPicked), SnackType.SUCCESS)
        }
    }

    private fun shouldShowCustomerNotifyMessage() = pickList.value?.prePickType.isAdvancePickOrPrePick().not() && siteRepo.twoWayCommsFlags.chatBeta == true

    private fun showFailedToAutoShortDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.item_details_short_error_title),
                    body = StringIdHelper.Id(R.string.item_details_short_error_body),
                    positiveButtonText = StringIdHelper.Id(R.string.try_again),
                    negativeButtonText = StringIdHelper.Id(R.string.ok),
                    cancelOnTouchOutside = false,
                ),
                tag = ItemDetailsViewModel.RETRY_AUTO_SHORT_DIALOG
            )
        )
    }

    private fun returnToInitialState() {
        /**
         * This condition is valid if we are inside the issue scanning flow and user clicks back button
         * on any of the screen (such as quantity picker, item and tote scan).
         */
        if (isIssueScanItemAvailable()) {
            canAcceptScan = false
            hadScanIssue = true
            isIssueScanningInProgress.value = true
        } else {
            /**
             * Else condition is valid when user is in normal picking flow and
             * also in issue scanning flow until the user has not picked any item
             * in issue scanning flow / not reached to the confirm pick bottom sheet
             */
            canAcceptScan = true
            hadScanIssue = false
            isIssueScanningInProgress.value = false
        }
        selectedVariant = null
        lastItemBarcodeScanned = null
        lastScannedItem = null
        activeScanTarget.set(ScanTarget.Item)
        showPersistentSnackbarPrompt()
        isFromManualEntry = false
        isManuallyEntered = false
    }

    fun getSelectedItemIndex(listType: PickListType? = currentTab.value) =
        when (listType) {
            PickListType.Todo -> todoSelectedItemIndex
            PickListType.Picked -> pickedSelectedItemIndex
            PickListType.Short -> shortSelectedItemIndex
            else -> todoSelectedItemIndex
        }

    private fun setSelectedItemIndex(value: Int) {
        when (currentTab.value) {
            PickListType.Todo -> _todoSelectedItemIndex
            PickListType.Picked -> _pickedSelectedItemIndex
            PickListType.Short -> _shortSelectedItemIndex
            else -> _todoSelectedItemIndex
        }.value = value
    }

    override fun onCleared() {
        super.onCleared()
        pickList.removeObserver(pickListObserver)
        todoPickList.removeObserver(todoPickListObserver)
        pickedPickList.removeObserver(pickedPickListObserver)
        shortPickList.removeObserver(shortPickListObserver)
        todoSelectedItemIndex.asLiveData().removeObserver(selectedItemIndexObserver)
    }

    fun setCurrentTab(index: Int?) {
        currentTab.set(PickListType.values().first { it.value == index })
        if (index.getOrZero() > 0) prompt.postValue(PickListItemsBottomPrompt.None)
    }

    /** Loads the details for the current pick list activity */
    fun loadPickList(pickListId: String, endPick: Boolean = false, toteEstimate: ToteEstimate? = null) {
        this.pickListId = pickListId
        acuPickLogger.v("[loadPickList] pickList=$pickList")
        this.toteEstimate = toteEstimate
        viewModelScope.launch(dispatcherProvider.IO) {
            // When offline with saved offline data, don't attempt to make the network calls
            val skipLoadingPickList = !networkAvailabilityManager.isConnected.first() && pickRepository.hasOfflinePickListData()
            if (!skipLoadingPickList) {
                val result = pickRepository.getActivityDetails(
                    id = pickListId.also {
                        it.logError(
                            "Activity Id is empty. PickListItemsViewModel(loadPickList), Order Id-${pickList.value?.customerOrderNumber}, User Id-${userRepo.user.value?.userId}, " +
                                "storeId-${pickList.value?.siteId}"
                        )
                    }
                )
                when (result) {
                    // TODO if a single item is fully shorted this will return a error with no debug message, error code, no httperrorcode, message and only a status of 404 : ticket 287
                    is ApiResult.Success -> {
                        // TODO: Consider moving to repository layer if called from other places (which would require moving the interface to :data as well, likely leave picasso/implementation in :app)
                        showToteEstimateBottomSheet(toteEstimate)
                        preCacheImages(result.data)
                        loadSubstitutionItemDetails()
                        loadAlternateLocations()
                        loadUpcCodes(result.data.itemActivities)
                        showEbtDialog(isSubstitution = false)
                    }

                    is ApiResult.Failure -> {
                        handleApiError(result, tag = RELOAD_LOAD_PICKLIST_DIALOG_TAG, retryAction = { loadPickList(pickListId) })
                    }
                }.exhaustive
                isDataLoading.postValue(false)
            }
            isDataLoading.postValue(false)
            if (endPick) onEndPickCtaClicked(true)
            val listPair = listOf(Pair(EventKey.ACTIVITY_ID, pickList.value?.actId.toString()), Pair(EventKey.PICKLIST_ID, pickListId))
            firebaseAnalytics.logEvent(EventCategory.PICKING, EventAction.SCREEN_VIEW, if (isListView.value == true) EventLabel.PICKLIST_LISTVIEW else EventLabel.PICKLIST_CARDVIEW, listPair)
        }
    }

    private fun showToteEstimateBottomSheet(toteEstimate: ToteEstimate? = null) {
        // This bottom sheet should be shown only if the current order is a mfc order.
        if (isMultiSource() && toteEstimate != null && ((toteEstimate.ambient ?: 0) > 0 || (toteEstimate.chilled ?: 0) > 0)) {
            inlineBottomSheetEvent.postValue(getTotesNeededArgDataAndTagForBottomSheet(toteEstimate))
        }
    }

    private fun showEbtDialog(isSubstitution: Boolean = true) {
        if (pickList.value?.containsSnap() == true) {
            canAcceptScan = false
            if (pickList.value?.isCustomerBagPreference == false) {
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = if (isBatch()) getEbtNoBagsBatchWarningDialog() else getEbtNoBagsSingleWarningDialog(),
                        tag = if (isSubstitution) EBT_WARNING_DIALOG_TAG else INITIAL_EBT_NO_BAGS_WARNING_DIALOG_TAG
                    )
                )
            } else {
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = getEbtWarningDialog(AcuPickConfig.cattEnabled.value),
                        tag = if (isSubstitution) EBT_WARNING_DIALOG_TAG else INITIAL_EBT_NO_BAGS_WARNING_DIALOG_TAG
                    )
                )
            }
        } else {
            if (pickList.value?.isCustomerBagPreference == false) {
                canAcceptScan = false
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = if (isBatch()) getNoBagsBatchWarningDialog() else getNoBagsSingleWarningDialog(),
                        tag = INITIAL_EBT_NO_BAGS_WARNING_DIALOG_TAG
                    )
                )
            }
        }
    }

    private fun makeShortReasonSelection(selection: Int?) {
        canAcceptScan = true
        when (selection) {
            0 -> completeShort(ShortReasonCode.OUT_OF_STOCK)
            1 -> completeShort(ShortReasonCode.TOTE_FULL)
            2 -> completeShort(ShortReasonCode.PREP_NOT_READY)
            else -> Unit
        }
    }

    fun updateSelectedItemIndex(index: Int) {
        invalidItemScanTracker.reset()
        viewModelScope.launch(dispatcherProvider.Main) {
            setSelectedItemIndex(index)
        }
    }

    private fun handleContainerReassignmentRefusal() {
        activeScanTarget.postValue(ScanTarget.Tote)
        lastToteBarcodeScanned = savedToteBarcodeScanned
        showPersistentSnackbarPrompt()
    }

    private fun handleRecordPickRetry() {
        viewModelScope.launch(dispatcherProvider.IO) {
            lastToteBarcodeScanned?.let { handleScannedTote(it.asBarcodeType()) }
        }
    }

    /** Loads UPC codes associated with the items in the pick list */
    private suspend fun loadUpcCodes(itemActivities: List<ItemActivityDto>?) {
        itemActivities?.let {
            val siteId = userRepo.user.value.also {
                if (it == null) acuPickLogger.w("[loadUpcCodes] user is null - unable to retrieve siteId")
            }?.selectedStoreId.orEmpty()
            val itemIds = itemActivities.map { it.itemId.orEmpty() }.distinct()
            acuPickLogger.v("[loadUpcCodes] siteId=$siteId, itemIds=[${itemIds.joinToString(separator = ", ")}]")
            val result = pickRepository.getItemUpcList(siteId, itemIds, itemActivities)
            when (result) {
                is ApiResult.Success -> {
                    // Nothing to do here.  PickRepository holds the response data.
                }

                is ApiResult.Failure -> {
                    handleApiError(result, tag = RELOAD_LOAD_PICKLIST_DIALOG_TAG)
                }
            }.exhaustive
            isDataLoading.postValue(false)
        }
    }

    /** Loads suggested substitution items associated with items in the pick list */
    private suspend fun loadSubstitutionItemDetails() {
        val result = pickRepository.getSubstitutionItemDetailList(actId = pickListId)
        when (result) {
            is ApiResult.Success -> {
                // Nothing to do here.  PickRepository holds the response data.
            }

            is ApiResult.Failure -> {
                // Also nothing to do here.  API is apparently returning an empty body when there are no suggested items?
            }
        }.exhaustive
        isDataLoading.postValue(false)
    }

    private suspend fun loadAlternateLocations() {
        val itemIds = pickList.value?.asListOfItemIds() ?: listOf()
        if (pickList.value?.itemActivities != null && itemIds.isNotNullOrEmpty())
            pickRepository.getAllItemLocations(
                siteId = pickList.value?.siteId ?: "",
                itemId = itemIds
            )
        isDataLoading.postValue(false)
    }

    /** Notifies the backend the user wishes to end the current picking activity */
    private fun completePicking(completeWithExceptions: Boolean, endPickReasonCode: EndPickReasonCode? = null) {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { completePicking(completeWithExceptions) }
                acuPickLogger.v("[completePicking] offline - unable to complete picking")
            } else {
                // TODO handle auto short/skip/shortage code
                val result = isBlockingUi.wrap {
                    pickRepository.completePickForStaging(
                        getPickCompleteRequestDto(completeWithExceptions, endPickReasonCode), (userRepo.user.value?.tokenizedLdapId ?: "")
                    )
                }
                when (result) {
                    is ApiResult.Success -> {
                        completeCallSuccessful = true
                        if (result.data.nextActivityId != null) {
                            printToteLabel(activityId = pickList.value?.actId.toString())
                        } else {
                            _navigationEvent.postValue(NavigationEvent.Up)
                        }
                    }

                    is ApiResult.Failure -> {
                        if (result is ApiResult.Failure.Server) {
                            val type = result.error?.errorCode?.resolvedType
                            if (type?.cannotAssignToOrder() == true) {
                                val serverErrorType = if (type == ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY) CannotAssignToOrderDialogTypes.PICKLIST else CannotAssignToOrderDialogTypes.REGULAR
                                serverErrorCannotAssignUser(serverErrorType, pickRepository.pickList.value?.erId == null, true)
                            } else {
                                handleApiError(result, tag = RELOAD_LOAD_PICKLIST_DIALOG_TAG, retryAction = { completePicking(completeWithExceptions) })
                            }
                        } else {
                            inlineDialogEvent.postValue(getCompletePickErrorArgDataAndTag())
                        }
                    }
                }.exhaustive
            }
            completeCallInProgress = false
        }
    }

    /** Notifies the backend the user wishes to end the current picking activity */
    private fun completeWinePicking(completeWithExceptions: Boolean, endPickReasonCode: EndPickReasonCode? = null) {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { completePicking(completeWithExceptions, endPickReasonCode) }
                acuPickLogger.v("[completePicking] offline - unable to complete picking")
            } else {
                // TODO handle auto short/skip/shortage code
                val result = isBlockingUi.wrap {
                    pickRepository.completePickForStaging(
                        getPickCompleteRequestDto(completeWithExceptions = completeWithExceptions, endPickReasonCode = endPickReasonCode),
                        (
                            userRepo
                                .user.value?.tokenizedLdapId ?: ""
                            )
                    )
                }
                when (result) {
                    is ApiResult.Success -> {
                        completeCallSuccessful = true
                        if (result.data.nextActivityId != null) {
                            navigateToWineShipping()
                        } else {
                            _navigationEvent.postValue(NavigationEvent.Up)
                        }
                    }

                    is ApiResult.Failure -> {
                        if (result is ApiResult.Failure.Server) {
                            val type = result.error?.errorCode?.resolvedType
                            if (type?.cannotAssignToOrder() == true) {
                                val serverErrorType = if (type == ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY) CannotAssignToOrderDialogTypes.PICKLIST else CannotAssignToOrderDialogTypes.REGULAR
                                serverErrorCannotAssignUser(serverErrorType, pickRepository.pickList.value?.erId == null, true)
                            } else {
                                handleApiError(result, tag = RELOAD_LOAD_PICKLIST_DIALOG_TAG, retryAction = { completePicking(completeWithExceptions) })
                            }
                        } else {
                            inlineDialogEvent.postValue(getCompletePickErrorArgDataAndTag())
                        }
                    }
                }.exhaustive
            }
            completeCallInProgress = false
        }
    }

    private fun getPickCompleteRequestDto(completeWithExceptions: Boolean, endPickReasonCode: EndPickReasonCode? = null): PickCompleteRequestDto {
        val shortageReasonCode = if (pickList.value?.prePickType.isAdvancePick()) ShortReasonCode.PICK_LATER else ShortReasonCode.TOTE_FULL
        return PickCompleteRequestDto(
            actId = pickList.value?.actId ?: 0,
            autoShortage = completeWithExceptions,
            shortageReasonCode = shortageReasonCode.takeIf { completeWithExceptions },
            endPickReasonCode = endPickReasonCode,
            skipCompleteValidation = completeWithExceptions,
            userId = userRepo.user.value?.userId
        )
    }

    private suspend fun sendPickRequest(scannedItem: ScannedPickItem, toteBarcodeType: PickingContainer, disableContainerValidation: Boolean = false): ApiResult<Unit> {
        val item = scannedItem.item
        justCompletedItemIaId.value = item.id

        val fulfilledQuantity = fulfilledQtyResult.toQuantity().toDouble()
        val upcQuantity = when (hadScanIssue) {
            true -> scannedItem.barcodeType.getUpcQty(fulfilledQuantity, sellByType = scannedItem.itemDetails?.sellByWeightInd, siteRepo.fixedItemTypesEnabled)
            false -> scannedItem.barcodeType.getUpcQty(fulfilledQuantity, sellByType = scannedItem.item.sellByWeightInd, siteRepo.fixedItemTypesEnabled)
        }

        return if (hadScanIssue) {
            pickRepository.recordSubstitution(
                request = SubstitutePickRequest(
                    itemBarcodeType = scannedItem.barcodeType,
                    toteBarcodeType = toteBarcodeType,
                    fulfilledQuantity = fulfilledQuantity,
                    upcQuantity = upcQuantity,
                    originalItem = currentItem!!,
                    substituteItem = SubstitutedItem(
                        itemId = if (selectedVariant != null) selectedVariant?.itemId else scannedItem.itemDetails?.itemId,
                        description = if (selectedVariant != null) selectedVariant?.itemDes else scannedItem.itemDetails?.itemDesc,
                        modifiedUpc = scannedItem.barcodeType.getBarcodeToSendToBackend(),
                        storageType = scannedItem.itemDetails?.storageType,
                    ),
                    userId = userRepo.user.filterNotNull().first().userId,
                    disableContainerValidation = disableContainerValidation,
                    isSmartSubItem = false,
                    subReasonCode = SubReasonCode.IssueScanning,
                    sameItemSubbed = false,
                    scannedPrice = if (scannedItem.barcodeType is BarcodeType.Item.Priced) (scannedItem.barcodeType as? BarcodeType.Item.Priced)?.price else null,
                    regulated = scannedItem.itemDetails?.isRegulated,
                    isManuallyEntered = isManuallyEntered
                )
            )
        } else {
            pickRepository.recordPick(
                request = PickRequest(
                    itemBpnId = item.itemId,
                    customerOrderNumber = item.customerOrderNumber,
                    itemBarcodeType = scannedItem.barcodeType,
                    toteBarcodeType = toteBarcodeType,
                    fulfilledQuantity = fulfilledQuantity,
                    upcQuantity = upcQuantity,
                    userId = userRepo.user.value!!.userId,
                    disableContainerValidation = disableContainerValidation,
                    netWeight = (fulfilledQtyResult as? FulfilledQuantityResult.ConfirmNetWeightResult)?.netWeight,
                    scannedPrice = if (scannedItem.barcodeType is BarcodeType.Item.Priced) (scannedItem.barcodeType as? BarcodeType.Item.Priced)?.price else null,
                    sellByWeightInd = item.sellByWeightInd,
                    storageType = item.storageType,
                    isManuallyEntered = isManuallyEntered
                )
            )
        }
    }

    /** When the item & tote are scanned, send successful pick to backend */
    private suspend fun recordPick(scannedItem: ScannedPickItem, toteBarcodeType: PickingContainer, disableContainerValidation: Boolean = false) {
        activeScanTarget.postValue(ScanTarget.None) // we will not accept any scans at this point
        val result = isBlockingUi.wrap {
            sendPickRequest(scannedItem, toteBarcodeType, disableContainerValidation)
        }
        when (result) {
            is ApiResult.Success -> {
                if (hadScanIssue) {
                    // handle confirmPick bottom sheet for issue scanning
                    addIssueScannedItemToList()
                    canAcceptScan = false
                    openIssueScanningConfirmationBottomSheet()
                } else {
                    when (scannedItem.item.sellByWeightInd == SellByType.PriceWeighted) {
                        true -> pickList.value?.itemActivities?.find { it.id == scannedItem.item.id }?.let { onDetailsCtaClicked(it, false) }
                        false -> activityViewModel.bottomSheetRecordPickArgData.postValue(CustomBottomSheetArgData(dialogType = BottomSheetType.ToteScan, exit = true, title = StringIdHelper.Raw("")))
                    }
                }
                playScanSound.postValue(true)
                withContext(dispatcherProvider.Main) {
                    // delay(500)
                    if (hadScanIssue)
                        showSnackBar(displayToteScanSuccess(toteBarcodeType.rawBarcode))
                    else showSnackBar(displayItemPickedSnackbar(scannedItem, fulfilledQtyResult, isLocationUpdated))

                    // Reset quantity after recording pick
                    isLocationUpdated = false
                    fulfilledQtyResult = FulfilledQuantityResult.DefaultQuantity
                    lastItemBarcodeScanned = null
                    lastScannedItem = null
                    analyticsHelper.scannedItemNoLongerRelevent()
                    activeScanTarget.postValue(ScanTarget.Item)
                    //  ISSUE-SCANNING in case of issue scanning it will be true once issue scanning completed
                    if (!hadScanIssue)
                        justCompletedItemIaId.value = null
                    isFromManualEntry = false
                    isManuallyEntered = false
                }
            }

            is ApiResult.Failure -> {
                playScanSound.postValue(false)
                if (result is ApiResult.Failure.Server) {
                    val type = result.error?.errorCode?.resolvedType
                    withContext(dispatcherProvider.Main) {
                        when (type) {
                            ServerErrorCode.CONTAINER_ATTACHED_TO_ENTITY_ID -> {
                                activeScanTarget.set(ScanTarget.Tote)
                                savedReassignmentScannedItem = scannedItem
                                savedReassignmentTote = toteBarcodeType
                                inlineDialogEvent.postValue(getContainerReassignmentArgDataAndTag())
                            }

                            ServerErrorCode.CONTAINER_ADDED_DIFF_PICKLIST_SAME_ORDER -> {
                                activeScanTarget.set(ScanTarget.Tote)
                                savedReassignmentScannedItem = scannedItem
                                savedReassignmentTote = toteBarcodeType
                                inlineDialogEvent.postValue(getContainerCannotReassignArgDataAndTag())
                            }

                            else -> {
                                if (type?.cannotAssignToOrder() == true) {
                                    val serverErrorType =
                                        if (type == ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY) CannotAssignToOrderDialogTypes.PICKLIST else CannotAssignToOrderDialogTypes.REGULAR
                                    serverErrorCannotAssignUser(serverErrorType, pickRepository.pickList.value?.erId == null, true)
                                } else {
                                    handleApiError(result, tag = RETRY_RECORD_PICK_DIALOG_TAG)
                                    activeScanTarget.postValue(ScanTarget.Tote)
                                }
                            }
                        }
                    }
                } else {
                    handleApiError(result, tag = RETRY_RECORD_PICK_DIALOG_TAG)
                    activeScanTarget.postValue(ScanTarget.Tote)
                }
            }
        }
    }

    // ISSUE-SCANNING open issue scanning confirmation bottom sheet
    private fun openIssueScanningConfirmationBottomSheet() {
        if (issueScannedListHolder.size > 1) {
            // Send the live data event to tote bottomsheet
            activityViewModel.bottomSheetRecordPickArgData.postValue(
                CustomBottomSheetArgData(
                    dialogType = BottomSheetType.ToteScan,
                    exit = true,
                    title = StringIdHelper.Raw("")
                )
            )
        } else {
            // Open issue scanning confirm pick bottomsheet for the first time
            inlineBottomSheetEvent.postValue(getIssueScanningConfirmationArgData()?.let { BottomSheetArgDataAndTag(data = it, tag = ISSUE_SCANNING_CONFIRMATION_BOTTOM_SHEET_TAG) })
        }
    }

    // To check if issue scanned item available in the list
    private fun isIssueScanItemAvailable() = issueScannedListHolder.isNotNullOrEmpty()

    // Open scan item bottom sheet for mulitple issue scanning item
    private fun openScanItemBottomSheet() {
        inlineBottomSheetEvent.postValue(
            BottomSheetArgDataAndTag(
                data = CustomBottomSheetArgData(
                    dialogType = BottomSheetType.ScanItem,
                    titleIcon = R.drawable.ic_scan_item,
                    title = StringIdHelper.Id(R.string.scan_item),
                    body = StringIdHelper.Id(R.string.please_scan_your_item),
                ),
                tag = ISSUE_SCAN_ITEM_BOTTOM_SHEET_TAG
            )
        )
    }

    private fun getIssueScanningConfirmationArgData() =
        currentItem?.let { currentItem ->
            val itemTotalWeight = when {
                currentItem.isOrderedByWeight() -> formattedWeight(currentItem.orderedWeight)
                currentItem.sellByWeightInd == SellByType.PriceWeighted || currentItem.sellByWeightInd == SellByType.Weight ->
                    formattedWeight(currentItem.requestedNetWeight)

                else -> ""
            }
            CustomBottomSheetArgData(
                dialogType = BottomSheetType.SubstitutionConfirmation,
                peekHeight = R.dimen.expanded_bottomsheet_peek_height,
                title = StringIdHelper.Id(R.string.confirm_pick),
                positiveButtonText = StringIdHelper.Id(R.string.complete_pick),
                negativeButtonText = StringIdHelper.Id(R.string.continue_scanning),
                exit = issueScannedListHolder.isEmpty(), // Exit issue scanning confimation bottom sheet if all scanned item removed
                customDataParcel = SubstituteConfirmationParam(
                    substituteItemList = issueScannedListHolder.reversed(),
                    imageUrl = currentItem.imageUrl,
                    description = currentItem.itemDescription,
                    requestedCount = if (siteRepo.isDisplayType3PWEnabled && currentItem.isDisplayType3PW()) currentItem.remainingWeight
                    else ((currentItem.qty?.toInt()?.minus(currentItem.processedQty?.toInt() ?: 0)) ?: 0).toString(),
                    isOrderedByWeight = currentItem.isOrderedByWeight() ?: false,
                    isDisplayType3PW = currentItem.isDisplayType3PW(),
                    requestedWeightAndUnits = itemTotalWeight,
                    hadIssueScanning = hadScanIssue,
                    iaId = justCompletedItemIaId.value,
                    sellByType = currentItem.sellByWeightInd,
                    isBulk = bulkVariants.isNotNullOrEmpty(),
                    isCustomerBagPreference = currentItem.isCustomerBagPreference
                )
            )
        }

    private fun addIssueScannedItemToList() {
        lastScannedItem?.let { scannedItem ->
            val scannedWeight = when (scannedItem.barcodeType) {
                is BarcodeType.Item.Weighted -> formattedWeight((scannedItem.barcodeType as BarcodeType.Item.Weighted).weight)
                is BarcodeType.Item.Priced -> formattedWeight(fulfilledQtyResult.toWeight())
                else -> ""
            }
            scannedItem.itemDetails?.apply {
                issueScannedListHolder.add(
                    SubstitutionLocalItem(
                        item = this,
                        selectedVariant = selectedVariant,
                        itemBarcodeType = lastItemBarcodeScanned,
                        toteBarcodeType = lastToteBarcodeScanned?.asBarcodeType(),
                        quantity = fulfilledQtyResult.toQuantity().toDouble(),
                        itemWeight = scannedWeight,
                        unitOfMeasure = scannedItem.item.itemWeightUom?.uppercase(),
                        orderedByWeight = scannedItem.item.isOrderedByWeight() ?: false,
                        isIssueScanned = true,
                        isDisplayType3Pw = siteRepo.isDisplayType3PWEnabled && currentItem?.isDisplayType3PW() == true,
                        orderedWeightWithUom = currentItem?.getWeightAndUom().orEmpty(),
                    )
                )
            }
        }
    }

    private fun reAssignContainer() {
        val savedReassignmentToteTemp = savedReassignmentTote
        val savedReassignmentScannedItemTemp = savedReassignmentScannedItem
        if (savedReassignmentToteTemp != null && savedReassignmentScannedItemTemp != null) {
            viewModelScope.launch(dispatcherProvider.IO) {
                // Call recordPick again, this time with disableContainerValidation true to bypass container/tote issues
                recordPick(savedReassignmentScannedItemTemp, savedReassignmentToteTemp, disableContainerValidation = true)
            }
        } else {
            acuPickLogger.w(
                "[reAssignContainer] unable to reassign container - savedReassignmentScannedItemTemp=$savedReassignmentScannedItemTemp, " +
                    "savedReassignmentToteTemp=$savedReassignmentToteTemp"
            )
        }
    }

    private fun printToteLabel(activityId: String) {
        acuPickLogger.v("[printToteLabel]")
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                isPrintingSuccessful = false
                networkAvailabilityManager.triggerOfflineError { printToteLabel(activityId) }
            } else {
                val result = isBlockingUi.wrap { apsRepo.printToteLabel(activityId) }
                isPrintingSuccessful = when (result) {
                    is ApiResult.Success -> true
                    is ApiResult.Failure -> false
                }.exhaustive
            }
            onLabelSentToPrinter(activityId)
        }
    }

    /** Entry point for a barcode received from the scanner/DataWedge */
    fun onScannerBarcodeReceived(barcodeType: BarcodeType) {
        if (canAcceptScan) {
            viewModelScope.launch(dispatcherProvider.Default) {
                when (activeScanTarget.value) {
                    ScanTarget.Item -> handleScannedItem(barcodeType, showMissingItemLocation = true)
                    ScanTarget.Tote -> handleScannedTote(barcodeType)
                    else -> Unit // cannot handle a scan right now
                }.exhaustive
            }
        }
    }

    /** User Interaction */
    fun onDetailsCtaClicked(item: ItemActivityDto, isMoveToLocation: Boolean) {
        acuPickLogger.v("[onDetailsCtaClicked] item=$item")
        isItemDetailBottomSheetShowing = true
        currentItem = item
        todoPickList.value?.itemActivities?.indexOfFirst {
            it.itemId == item.itemId && it.customerOrderNumber == item.customerOrderNumber
        }?.let(::updateSelectedItemIndex)
        val altLocationsList = pickRepository.getAlternateLocations(item.itemId ?: "")
        item.id?.let { iaId ->
            inlineBottomSheetEvent.postValue(
                getItemDetailsArgDataAndTagForBottomSheet(
                    iaId, pickList.value?.actId ?: 0, pickList.value?.activityNo.orEmpty(), altLocationsList, item,
                    currentTab.value ?: PickListType.Todo, isMoveToLocation = isMoveToLocation
                )
            )
        }
    }

    fun onCompletePickClicked(item: ItemActivityDto) {
        acuPickLogger.v("[onCompletePickClicked] item=$item")
        viewModelScope.launch {
            pickRepository.modifyPickList(item)
            pickRepository.completeClickedCall(item.id.toString())
            if (isLocationUpdated) {
                showSnackBar(StringIdHelper.Id(R.string.item_complete_location_updated), SnackType.SUCCESS)
            } else {
                showSnackBar(StringIdHelper.Id(R.string.item_complete_picked), SnackType.SUCCESS)
            }
        }
    }

    fun onCompleteButtonShown(item: ItemActivityDto) {
        acuPickLogger.v("[onCompletePickClicked] item=$item")
        // TODO: ACURED_REDESIGN Show snackbar message on every tote scanned Will remove commented line after QA
        // displayItemPersistentSnackbar(false)
    }

    /**
     * Substitution/Issue-Scanning will be allowed if order is not prepick/advancepick
     * Substitution/Issue-Scanning will be allowed if order is prepick or advance pick and site level flag is true.
     */
    private fun shouldAllowSubstitutionOrIssueScanning(): Boolean = pickList.value?.prePickType.isAdvancePickOrPrePick().not() ||
        (pickList.value?.prePickType.isAdvancePickOrPrePick() && siteRepo.prePickFeatureFlag.allowSubstitution.toBoolean().orFalse())

    fun onSubstituteCtaClicked(item: ItemActivityDto) {
        currentItem = item
        when (currentTab.value) {
            PickListType.Todo -> {
                acuPickLogger.v("[onSubstituteCtaClicked] item=$item")
                when {
                    shouldAllowSubstitutionOrIssueScanning() ->
                        // General picking flow
                        if (isSubstitutionAllowed(item)) {
                            val altLocationsList = pickRepository.getAlternateLocations(item.itemId ?: "")
                            if (altLocationsList.isNotNullOrEmpty()) {
                                canAcceptScan = false
                                inlineDialogEvent.postValue(
                                    getAlternativeLocationsArgDataAndTag(
                                        context = app.applicationContext, lastItemShorted = null, lastSubstitutedItem = currentItem, altLocationsList = altLocationsList, path = Substitute
                                    )
                                )
                            } else {
                                showEbtWarningOrNavigateToSubstitution()
                            }
                        } else {
                            invalidItemScanTracker.reset()
                            showEbtWarningOrNavigateToSubstitution()
                        }

                    else ->
                        // Prepick and Advance pick flow
                        when (pickList.value?.prePickType) {
                            PrePickType.ADVANCE_PICK -> {
                                canAcceptScan = false
                                inlineDialogEvent.postValue(getPickLaterDialogArgData())
                            }

                            PrePickType.PRE_PICK -> {
                                canAcceptScan = false
                                openShorItemActionSheet()
                            }

                            else -> Unit
                        }
                }
            }

            PickListType.Picked -> handleLabelClickedOnPickedTab()

            PickListType.Short -> {
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = CustomDialogArgData(
                            titleIcon = null,
                            title = StringIdHelper.Id(R.string.item_details_undo_short_title),
                            body = StringIdHelper.Id(R.string.item_details_undo_short_body),
                            imageUrl = item.sizedImageUrl(ImageSizePreset.ItemDetails),
                            secondaryBody = StringIdHelper.Raw(item.itemDescription ?: ""),
                            questionBody = StringIdHelper.Raw(item.asUpcOrPlu(app.applicationContext, barcodeMapper)),
                            orderedWeightOrRemainingQty =
                            if (siteRepo.isDisplayType3PWEnabled && currentItem?.isDisplayType3PW() == true) currentItem?.getWeightAndUom()
                            else currentItem?.exceptionQty.orZero().toString(),
                            positiveButtonText = StringIdHelper.Id(R.string.confirm),
                            negativeButtonText = StringIdHelper.Id(R.string.cancel),
                            dialogType = DialogType.ConfirmItem
                        ),
                        tag = ItemDetailsViewModel.CONFIRMATION_UNDO_SHORT_DIALOG_TAG
                    )
                )
            }

            else -> {}
        }
    }

    private fun continueToSubstitution() {
        canAcceptScan = true
        viewModelScope.launch(Main) {
            // This delay allows for the AlternativeLocationDialog to close before navigating to the SubstituionFragment
            showEbtWarningOrNavigateToSubstitution()
        }
    }

    private fun showEbtWarningOrNavigateToSubstitution() {
        if (pickList.value?.containsSnap() == true) {
            showEbtDialog()
        } else {
            navigateToSubstitution()
        }
    }

    private fun navigateToSubstitution(substitutionRemovedQty: Int? = null, path: SubstitutionPath? = null, swapSubstitutionReason: SwapSubstitutionReason? = null) {
        acuPickLogger.v("[navigateToSubstitution] item=$currentItem")
        swapSubstitutionReason?.let { acuPickLogger.v("[navigateToSubstitution] swapSubstitutionReason=$it") }
        activeScanTarget.postValue(ScanTarget.Item)
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                PickListItemsFragmentDirections.actionPickListItemsFragmentToSubstituteFragment(
                    SubstituteParams(
                        iaId = currentItem?.id,
                        pickListId = pickListId,
                        path = path,
                        swapSubstitutionReason = swapSubstitutionReason,
                        substitutionRemovedQty = substitutionRemovedQty
                    )
                )
            )
        )
    }

    /**
     * Pre-pick open short item reason bottom sheet
     */
    private fun openShorItemActionSheet() {
        inlineBottomSheetEvent.postValue(
            BottomSheetArgDataAndTag(
                data = getShortItemReasonArgDataForActionSheet(shortReasonOptions),
                tag = SHORT_ITEM_ACTION_SHEET_TAG,
            )
        )
    }

    private val shortReasonOptions = mutableListOf(
        ActionSheetOptions(R.drawable.ic_prepnotready, R.string.short_prep_not_ready),
        ActionSheetOptions(R.drawable.ic_tote_full, R.string.short_tote_full),
        ActionSheetOptions(R.drawable.ic_outofstock, R.string.short_out_of_stock)
    )

    fun onShortCtaClicked(item: ItemActivityDto) {
        lastItemShorted = item
        if (isSubstitutionAllowed(item)) {
            canAcceptScan = false
            inlineDialogEvent.postValue(getSuggestSubstitutionArgDataAndTag())
        } else {
            goToShortItemDialog()
        }
    }

    private fun goToShortItemDialog() {
        canAcceptScan = false
        itemActivityDto = lastItemShorted
        when (isWineShipping()) {
            true -> inlineDialogEvent.postValue(CustomDialogArgDataAndTag(data = SHORT_WINE_ITEM_REASON_DIALOG, tag = SHORT_ITEM_REASON_TAG))
            false -> inlineDialogEvent.postValue(CustomDialogArgDataAndTag(data = SHORT_ITEM_REASON_DIALOG, tag = SHORT_ITEM_REASON_TAG))
        }
    }

    private fun completeShort(selectedReason: ShortReasonCode) {
        val altLocationsList = pickRepository.getAlternateLocations(lastItemShorted.itemId ?: "")
        if (selectedReason == ShortReasonCode.OUT_OF_STOCK && altLocationsList.isNotNullOrEmpty()) {
            canAcceptScan = false
            inlineDialogEvent.postValue(
                getAlternativeLocationsArgDataAndTag(
                    context = app.applicationContext,
                    lastItemShorted = lastItemShorted,
                    lastSubstitutedItem = null,
                    altLocationsList = altLocationsList,
                    path = Short
                )
            )
        } else {
            recordShortage(selectedReason)
        }
    }

    private fun recordShortage(selectedReason: ShortReasonCode) {
        currentItem?.let { itemActivityDto = it }
        // Before recording the shortage, check if this is the final item in the recyclerview,
        // if so delay for the success animation to play before scrolling to first item again
        (todoSelectedItemIndex.value == (todoPickList.value?.itemActivities?.lastIndex)).also { lastItemInTodoListShorted = it }
        val message = when (selectedReason) {
            ShortReasonCode.OUT_OF_STOCK -> if (shouldShowCustomerNotifyMessage()) {
                StringIdHelper.Id(R.string.the_item_has_been_marked_out_of_stock_customer_notified)
            } else {
                StringIdHelper.Id(R.string.the_item_has_been_market_out_of_stock)
            }

            ShortReasonCode.TOTE_FULL -> StringIdHelper.Plural(R.plurals.tote_full, calculateQtyToShort().toInt())
            ShortReasonCode.PREP_NOT_READY -> StringIdHelper.Plural(R.plurals.prep_not_ready, calculateQtyToShort().toInt())
            ShortReasonCode.PICK_LATER, ShortReasonCode.PRE_PICK_ISSUE_SCANNING -> StringIdHelper.Id(R.string.the_item_has_been_marked_pick_later)
        }
        viewModelScope.launch {
            val result = isBlockingUi.wrap {
                pickRepository.recordShortage(
                    when (selectedReason) {
                        ShortReasonCode.OUT_OF_STOCK -> createShortDto(selectedReason)
                        ShortReasonCode.TOTE_FULL -> createShortDto(selectedReason)
                        ShortReasonCode.PREP_NOT_READY -> createShortDto(selectedReason)
                        ShortReasonCode.PICK_LATER, ShortReasonCode.PRE_PICK_ISSUE_SCANNING -> createShortDto(selectedReason)
                    }.exhaustive
                )
            }
            canAcceptScan = true
            when (result) {
                is ApiResult.Success -> {
                    justShortedItemIaId.value = itemActivityDto.id
                    // todo green toast like on pickListItems, maybe a navigation arg to show on previous screen?
                    val actId = pickRepository.pickList.first()?.actId.toString()
                    // TODO: Move into PickRepository (reference approach used in recordPick)
                    if (!devOptionsRepository.useOnlineInMemoryPickListState) {
                        isBlockingUi.wrap {
                            pickRepository.getActivityDetails(
                                actId.also {
                                    it.logError(
                                        "Activity ID Is Empty. PickListItemsViewModel(recordShortage), Order Id-${pickRepository.pickList.first()?.customerOrderNumber}, User Id-${
                                        userRepo.user
                                            .value?.userId
                                        }, " +
                                            "storeId-${pickRepository.pickList.first()?.siteId}"
                                    )
                                }
                            )
                        }
                    }
                    // The below delay is to allow for the full lottie 'Item Complete'
                    // animation to play before moving to the 'Short' tab
                    showSnackBar(AcupickSnackEvent(message = message, SnackType.SUCCESS))
                    delay(100)
                    justShortedItemIaId.value = null
                    showPersistentSnackbarPrompt()
                }

                is ApiResult.Failure -> {
                    if (result is ApiResult.Failure.Server) {
                        val type = result.error?.errorCode?.resolvedType
                        if (type?.cannotAssignToOrder() == true) {
                            val serverErrorType = if (type == ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY) CannotAssignToOrderDialogTypes.PICKLIST else CannotAssignToOrderDialogTypes.REGULAR
                            serverErrorCannotAssignUser(serverErrorType, pickRepository.pickList.value?.erId == null, true)
                        } else {
                            handleApiError(result, retryAction = { recordShortage(selectedReason) })
                        }
                    } else {
                        handleApiError(result, retryAction = { recordShortage(selectedReason) })
                        acuPickLogger.d("ShortItemViewModel: onCompleteCtaClick $result")
                    }
                    lastItemInTodoListShorted = false
                }
            }
        }
    }

    private suspend fun createShortDto(selectedReason: ShortReasonCode): ShortPickRequestDto {
        return ShortPickRequestDto(
            actId = pickRepository.pickList.first()?.actId,
            shortReqDto = createShortRequestDto(calculateQtyToShort(), selectedReason)
        )
    }

    private fun calculateQtyToShort(): Double {
        // Factoring in processedAndExceptionQty due to the following scenario:
        // Item has 3 qty, picker picks 1 qty, shorts remaining (2) qty, undoes pick of 1 qty, shorts that remaining 1 qty
        return (itemActivityDto.qty.orZero()).minus(itemActivityDto.processedAndExceptionQty.orZero())
    }

    private fun createShortRequestDto(count: Double, selectedReason: ShortReasonCode): List<ShortRequestDto> {
        val shortRequestList = arrayListOf<ShortRequestDto>()
        shortRequestList.add(
            ShortRequestDto(
                iaId = itemActivityDto.id,
                itemId = itemActivityDto.itemId,
                qty = count,
                shortageReasonText = selectedReason.textValue(),
                shortageReasonCode = selectedReason,
                shortedTime = ZonedDateTime.now(),
                userId = userRepo.user.value?.userId
            )
        )
        return shortRequestList.toList()
    }

    // For auto short through un pick
    private fun createShortRequestDto(count: Double): List<ShortRequestDto> {
        val shortRequestList = arrayListOf<ShortRequestDto>()
        shortRequestList.add(
            ShortRequestDto(
                iaId = currentItem?.id,
                itemId = currentItem?.itemId,
                qty = count,
                shortageReasonText = currentItem?.shortedItemUpc?.first()?.exceptionReasonText,
                shortageReasonCode = currentItem?.shortedItemUpc?.first()?.exceptionReasonCode,
                shortedTime = ZonedDateTime.now(),
                userId = currentItem?.shortedItemUpc?.first()?.userId
            )
        )
        return shortRequestList.toList()
    }

    fun onEndPickCtaClicked(skipConfirmationDialog: Boolean = false) {
        viewModelScope.launch {
            val isChatButtonVisible = showChatButton.value == true
            val hasPendingSubsOrUnreadMsgs = pendingSubCountIntFormat > 0 || showUnreadMessages.value == true
            val messages = conversationRepo.messages.value

            val blockStaging = if (isBatch()) {
                // Block staging for batch order
                isChatButtonVisible && pickList.value?.orderChatDetails?.any { orderChatDetail ->
                    val messagesForSid = messages?.get(conversationRepo.getConversationId(orderChatDetail.customerOrderNumber.orEmpty()))
                    val hasUmaOrWebText = messagesForSid?.any {
                        (it.attributes?.messageSource == MessageSource.UMA || it.attributes?.messageSource == MessageSource.WEB) &&
                            it.attributes?.messageType == DisplayType.TEXT
                    } ?: false
                    val hasPickerNonInternal = messagesForSid?.any {
                        it.attributes?.messageSource == MessageSource.PICKER && (it.attributes?.messageType == DisplayType.INTERNAL).not()
                    }?.not() ?: false
                    hasUmaOrWebText && hasPickerNonInternal
                } ?: false
            } else {
                // Block staging for non batch order
                val messagesForOrder = messages?.get(conversationRepo.getConversationId(pickList.value?.customerOrderNumber.orEmpty()))
                val hasUmaOrWebText = messagesForOrder?.any {
                    (it.attributes?.messageSource == MessageSource.UMA || it.attributes?.messageSource == MessageSource.WEB) &&
                        it.attributes?.messageType == DisplayType.TEXT
                } ?: false
                val hasPickerNonInternal = messagesForOrder?.any {
                    it.attributes?.messageSource == MessageSource.PICKER && (it.attributes?.messageType == DisplayType.INTERNAL).not()
                }?.not() ?: false
                isChatButtonVisible && hasUmaOrWebText && hasPickerNonInternal
            }

            val conversationsTyping = conversationRepo.onTyping.asLiveData().value?.filter { it.value }.orEmpty()
            val isOnlyPickerPicking = conversationRepo.checkIfOnlyPickerPicking(conversationsTyping, false)
            val showTyping = isChatButtonVisible && isOnlyPickerPicking &&
                showTypingCount < 3 &&
                siteRepo.twoWayCommsFlags.customerTyping.orFalse()
            val showUnreadMessage = isChatButtonVisible && hasPendingSubsOrUnreadMsgs && reviewChatClickCount <= 2

            fun sendLogs(networkCalls: NetworkCalls) {
                val errorData = ChatErrorData(
                    orderNumbers = pickList.value?.getListOfOrderNumber()
                ).toJsonString(moshi)
                conversationRepo.sendLog(
                    ApiResult.Failure.GeneralFailure(errorData, networkCallName = networkCalls.value),
                    false
                )
            }

            if (blockStaging) {
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = CustomDialogArgData(
                            titleIcon = null,
                            title = StringIdHelper.Id(R.string.chat_block_staging_message),
                            positiveButtonText = StringIdHelper.Id(R.string.go_to_chat),
                            dialogType = DialogType.Informational,
                            closeIconVisibility = true
                        ),
                        tag = BLOCK_STAGING_DIALOG_TAG
                    )
                )
                sendLogs(NetworkCalls.NO_REPLY_DIALOG_DISPLAYED)
            } else if (showTyping) {
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = CustomDialogArgData(
                            titleIcon = null,
                            title = StringIdHelper.Id(R.string.chat_customer_typing_message),
                            body = StringIdHelper.Id(R.string.chat_typing_message),
                            positiveButtonText = StringIdHelper.Id(R.string.go_to_chat),
                            negativeButtonText = StringIdHelper.Id(R.string.close),
                            dialogType = DialogType.Informational,
                            closeIconVisibility = false
                        ),
                        tag = CUSTOMER_TYPING_DIALOG_TAG
                    )
                )
                showTypingCount++
                sendLogs(NetworkCalls.CUSTOMER_TYPING_DIALOG_DISPLAYED)
            } else if (showUnreadMessage) {
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = CustomDialogArgData(
                            titleIcon = null,
                            title = StringIdHelper.Id(R.string.open_actions_remaining),
                            body = StringIdHelper.Id(R.string.end_pick_dialog_body),
                            positiveButtonText = StringIdHelper.Id(R.string.review_chat),
                            negativeButtonText = StringIdHelper.Id(R.string.continue_to_staging),
                            dialogType = DialogType.Informational,
                            closeIconVisibility = true
                        ),
                        tag = PRINT_TOTE_LABELS_DIALOG_TAG
                    )
                )
                sendLogs(NetworkCalls.UNREAD_MESSAGE_DIALOG_DISPLAYED)
            } else if (areAllItemsShorted) {
                completePicking(completeWithExceptions = true)
            } else {
                startEndPick(skipConfirmationDialog)
            }
        }
    }

    private fun startEndPick(skipConfirmationDialog: Boolean = false) {
        displayItemPersistentSnackbar(false)
        viewModelScope.launch(dispatcherProvider.Main) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> if (isWineShipping()) {
                    startEndWinePickingProcess()
                } else {
                    startEndPickProcess(skipConfirmationDialog)
                }

                false -> networkAvailabilityManager.triggerOfflineError { onEndPickCtaClicked(skipConfirmationDialog) }
            }.exhaustive
        }
    }

    private fun navigateToWineShipping() {
        val params = WineStagingParams(
            contactName = pickList.value?.fullContactName().orEmpty(),
            activityId = pickList.value?.actId?.toString().orEmpty(),
            entityId = pickList.value?.entityReference?.entityId.orEmpty(),
            shortOrderNumber = pickList.value?.shortOrderNumber.orEmpty(),
            customerOrderNumber = pickList.value?.customerOrderNumber.orEmpty(),
            stageByTime = pickList.value?.stageByTime().orEmpty(),
            pickedUpBottleCount = pickedItemCount.value.orEmpty()
        )
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                PickListItemsFragmentDirections.actionToWineStagingFragment(
                    wineStagingParams = params
                )
            )
        )
    }

    fun changeView(listView: Boolean) {
        val listPair = listOf(Pair(EventKey.ACTIVITY_ID, pickList.value?.actId.toString()), Pair(EventKey.PICKLIST_ID, pickListId))
        firebaseAnalytics.logEvent(EventCategory.PICKING, EventAction.CLICK, if (listView) EventLabel.PICKLIST_LISTVIEW else EventLabel.PICKLIST_CARDVIEW, listPair)
        isListView.postValue(listView)
    }

    fun onTotesCtaClicked() {
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                PickListItemsFragmentDirections.actionPickListItemsFragmentToTotesFragment(
                    picklistid = pickListId, toteEstimate = toteEstimate
                )
            )
        )
        acuPickLogger.v("[onTotesCtaClicked]")
    }

    fun onManualEntryCtaClicked() {
        canAcceptScan = false
        viewModelScope.launch {
            acuPickLogger.v("[onManualEntryCtaClicked]")
            val item = getSelectedItem()
            val entryType = when (item?.sellByWeightInd) {
                SellByType.RegularItem, SellByType.Prepped, SellByType.PriceEachUnique,
                SellByType.PriceEachTotal, SellByType.PriceEach, null,
                -> ManualEntryType.UPC

                SellByType.Weight -> ManualEntryType.Weight
                SellByType.Each -> ManualEntryType.PLU
                SellByType.PriceScaled -> ManualEntryType.Barcode
                SellByType.PriceWeighted -> ManualEntryType.UPC
            }.exhaustive

            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    PickListItemsFragmentDirections.actionPickListItemsFragmentToManualEntryPagerFragment(
                        ManualEntryPickParams(
                            selectedItem = item,
                            requestedQty = item?.qty?.toInt() ?: 0,
                            stageByTime = pickList.value?.stageByTime(),
                            entryType = entryType,
                            isSubstitution = false,
                            isIssueScanning = hadScanIssue // Manual entry through issue scanning
                        ),
                        entryType
                    )
                )
            )
        }
    }

    private fun onItemScanErrorCtaClicked(selectedItem: ItemActivityDto?) {
        canAcceptScan = false
        currentItem = selectedItem
        when {
            shouldAllowSubstitutionOrIssueScanning() -> selectedItem?.let {
                inlineDialogEvent.postValue(getIssueScanningArgDataAndTag())
            }

            else -> when (pickList.value?.prePickType) {
                PrePickType.ADVANCE_PICK, PrePickType.PRE_PICK -> {
                    viewModelScope.launch(dispatcherProvider.Main) {
                        if (isItemDetailBottomSheetShowing) {
                            activityViewModel.bottomSheetRecordPickArgData.postValue(getDismissItemDetailBottomSheetArgDataAndTag()) // To dismiss item detail bottom sheet
                            delay(100)
                        }
                        inlineDialogEvent.postValue(getPickLaterDialogArgData(pickList.value?.prePickType == PrePickType.PRE_PICK))
                    }
                }

                else -> Unit
            }
        }
    }

    private fun onConfirmItemScanErrorCtaClicked() {
        viewModelScope.launch {
            hadScanIssue = true
            canAcceptScan = true
            isIssueScanningInProgress.value = true
            lastItemBarcodeScanned?.let {
                handleScannedItem(it)
            }
        }
    }

    fun handleManualEntryResults(quantityResult: FulfilledQuantityResult, barcode: BarcodeType.Item?, doOrderedByWeightCheck: Boolean = true) {
        viewModelScope.launch(dispatcherProvider.Default) {
            isManuallyEntered = true
            val quantity = quantityResult.toQuantity()
            // Handling manual entry result with issue scanning flow
            if (hadScanIssue) {
                if (quantity == 0 || barcode == null || currentItem == null) {
                    returnToInitialState()
                } else {
                    currentItem?.let {
                        lastScannedItemDetails?.let { lastScannedItemDetails ->
                            when (lastScannedItemDetails.sellByWeightInd) {
                                // Need to check for the PW and PW here and remove them if not necessary used for issuescan
                                SellByType.PriceWeighted -> handleIssueScannedItemSuccessforPW(it, barcode as BarcodeType.Item.Priced, lastScannedItemDetails)
                                SellByType.PriceEach -> handleIssueScannedItemSuccessforPE(it, barcode as BarcodeType.Item.Priced, lastScannedItemDetails)
                                else -> {
                                    fulfilledQtyResult = quantityResult
                                    lastItemBarcodeScanned = barcode
                                    playScanSound.postValue(true)
                                    showScanItemSuccess(ScannedPickItem(barcode, it, lastScannedItemDetails))
                                    canAcceptScan = true
                                }
                            }
                        }
                    }
                }
            } else {
                // Handling manual entry result with normal picking flow
                when (val scannedItem = pickRepository.getNextItemToSelectForScan(barcode, getSelectedItem())) {
                    is MatchedItem -> {
                        if (quantity == 0 || barcode == null) {
                            returnToInitialState()
                        } else {
                            // Calculate scanned weight and quantity for W type item irrespective of display type to send in recordPick API to get processed weight
                            fulfilledQtyResult = if (scannedItem.itemActivityDto.sellByWeightInd == SellByType.Weight) {
                                (barcode as? BarcodeType.Item.Weighted)?.weight?.let { enteredWeight ->
                                    Timber.v("Manually entered weight and qty", "weight $enteredWeight , qty: ${quantityResult.toQuantity()}")
                                    FulfilledQuantityResult.ConfirmNetWeightResult(enteredWeight, quantityResult.toQuantity(), SellByType.Weight)
                                } ?: run {
                                    showSnackBar(StringIdHelper.Id(R.string.incorrect_item_scanned), SnackType.ERROR)
                                    return@launch
                                }
                            } else {
                                quantityResult
                            }
                            lastItemBarcodeScanned = barcode
                            if (doOrderedByWeightCheck && isScannedWeightOutOfRange(scannedItem.itemActivityDto, barcode, true)) {
                                return@launch
                            } else {
                                playScanSound.postValue(true)
                                manualEntryComplete(ScannedPickItem(barcode, scannedItem.itemActivityDto))
                                canAcceptScan = true
                            }
                        }
                    }

                    else -> {
                    }
                }
            }
        }
    }

    fun handleManualEntryScan(barcodeType: BarcodeType) {
        viewModelScope.launch {
            isManuallyEntered = true
            handleScannedItem(barcodeType)
        }
    }

    fun handleFulfilledQuantityResult(quantityResult: FulfilledQuantityResult, item: ItemActivityDto? = null) {
        val quantity = quantityResult.toQuantity()
        if (quantity == 0 && item?.sellByWeightInd != SellByType.PriceWeighted) {
            // quantity == 0 means the quantity picker was cancelled, and scanned item should be cleared
            returnToInitialState()
        } else {
            lastScannedItem?.let {
                viewModelScope.launch(dispatcherProvider.Default) {
                    // Calculate scanned weight and quantity for W type item to send in recordPick API to get processed weight
                    fulfilledQtyResult = if (it.item.sellByWeightInd == SellByType.Weight && it.item.isOrderedByWeight().not()) {
                        (it.barcodeType as? BarcodeType.Item.Weighted)?.weight?.let { scannedWeight ->
                            Timber.v("Scanned weight and qty", "weight $scannedWeight , qty: ${quantityResult.toQuantity()}")
                            FulfilledQuantityResult.ConfirmNetWeightResult(scannedWeight, quantityResult.toQuantity(), SellByType.Weight)
                        } ?: run {
                            showSnackBar(StringIdHelper.Id(R.string.incorrect_item_scanned), SnackType.ERROR)
                            return@launch
                        }
                    } else {
                        quantityResult
                    }
                    if (it.barcodeType is BarcodeType.Item.Each) {
                        playScanSound.postValue(true)
                    }
                    showScanItemSuccess(it, quantityResult.toWeight())
                }
            }
            canAcceptScan = true
        }
    }

    fun onLabelSentToPrinter(activityId: String) {
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                PickListItemsFragmentDirections.actionPickListItemsFragmentToStagingFragment(
                    activityId = activityId,
                    isPreviousPrintSuccessful = isPrintingSuccessful,
                    shouldClearData = true
                )
            )
        )
    }

    /** Call from associated fragment's onCreateView and onDestroyView functions to update UI active state */
    override fun updateUiLifecycle(active: Boolean) {
        super.updateUiLifecycle(active)
        if (active) {
            viewModelScope.launch(dispatcherProvider.Default) {
                pickRepository.pickList.first()?.let {
                    clearToolbarEvent.postValue(Unit)
                    changeRightToolbarImage()
                    changeToolbarRightSecondExtraImageEvent.postValue(DrawableIdHelper.Id(R.drawable.ic_picking_info))
                    showStagingTimeOnTitle(
                        it.stageByTime(),
                        it.orderType,
                        siteRepo.concernTime,
                        siteRepo.warningTime,
                        it.releasedEventDateTime,
                        it.expectedEndTime,
                        it.prePickType.isAdvancePickOrPrePick()
                    )
                }
            }
        }
    }

    private fun startEndWinePickingProcess(skipConfirmationDialog: Boolean = true) {
        acuPickLogger.v("[onCompletePickListCtaClicked]")
        if (skipConfirmationDialog) {
            when {
                areAllItemsPicked ->
                    if (areAllItemsShorted) {
                        inlineDialogEvent.postValue(getEarlyExitWineArgDataAndTag())
                    } else {
                        completeWinePicking(completeWithExceptions = areAllItemsShorted)
                    }

                isAnyItemPicked -> showEndPickReasonDialog()
                else -> unAssignPicker()
            }
        } else {
            when {
                areAllItemsPicked && areAllItemsShorted -> inlineDialogEvent.postValue(getEarlyExitWineArgDataAndTag())
            }
        }
        completeCallInProgress = false
    }

    private fun startEndPickProcess(skipConfirmationDialog: Boolean) {
        acuPickLogger.v("[onCompletePickListCtaClicked]")
        if (skipConfirmationDialog) {
            when {
                areAllItemsPicked -> {
                    if (areAllItemsShorted) {
                        showEndPickReasonDialog()
                    } else {
                        completePicking(completeWithExceptions = false)
                    }
                }

                isAnyItemPicked -> {
                    showEndPickReasonDialog()
                }
                else -> unAssignPicker()
            }
        } else {
            when {
                areAllItemsPicked ->
                    inlineDialogEvent.postValue(
                        if (areAllItemsShorted) {
                            // All item shorted end pick reason code required
                            getEarlyExitArgDataAndTag()
                        } else {
                            // No items present in to-do list, end pick reason code not required
                            getCompletePickArgDataAndTag()
                        }
                    )

                isAnyItemPicked -> {
                    val allItems = pickList.value?.itemActivities
                    val itemsNotPicked = allItems?.filter { item -> !item.validateIsFullyPicked() }?.size ?: 0
                    // Partially picked end pick reason code required
                    inlineDialogEvent.postValue(getEndPickWithExceptionsArgDataAndTag(itemsNotPicked))
                }

                else -> inlineDialogEvent.postValue(getEndPickConfirmationArgDataAndTag()) // No any items picked
            }
        }
    }

    private fun showEndPickReasonDialog() {
        inlineDialogEvent.postValue(getEndPickReasonConfirmationDialogArgData())
    }

    private fun getSelectedEndPickReason(selection: Int?): EndPickReasonCode? {
        return selection?.let { endPickReasonCodeList[it] }
    }

    private suspend fun getFreshAssignedId(): String? {
        return when (
            val result = pickRepository.getActivityDetails(
                id = pickListId.also {
                    it.logError(
                        "Null Activity Id. PickListItemsViewModel(getFreshAssignedId), Order Id-${pickRepository.pickList.first()?.customerOrderNumber}, User Id-${
                        userRepo.user
                            .value?.userId
                        }, " +
                            "storeId-${pickRepository.pickList.first()?.siteId}"
                    )
                }
            )
        ) {
            is ApiResult.Success -> {
                result.data.assignedTo?.userId
            }

            is ApiResult.Failure -> {
                assignedUserId.value
            }
        }
    }

    fun unAssignPicker() {
        if (!pickListId.isValidActivityId()) {
            return
        }
        viewModelScope.launch(dispatcherProvider.IO) {
            val assignedUserId = getFreshAssignedId()
            if (userRepo.user.value?.userId == assignedUserId) {
                // Assigned User ID is your User ID.  Continue as normal.
                val activityNo = pickList.value?.activityNo.orEmpty()
                val result = isBlockingUi.wrap {
                    pickRepository.unAssignUser(
                        actId = pickListId,
                        userId = userRepo.user.value?.userId,
                        orderIds = pickList.value?.getListOfOrderNumber(),
                        tokenizedLdap = (userRepo.user.value?.tokenizedLdapId ?: "")
                    )
                }
                when (result) {
                    is ApiResult.Success -> {
                        withContext(dispatcherProvider.Main) {
                            toaster.toast(app.getString(R.string.un_assign_success_format, activityNo))
                            delay(2000)
                            unAssignSuccessfulAction.postValue(Unit)
                        }
                    }

                    is ApiResult.Failure -> {
                        // retryAction = { unAssignPicker() }
                        handleApiError(result, tag = RETRY_UNASSIGN_PICKER_DIALOG_TAG)
                    }
                }.exhaustive
            } else {
                // Assigned User Id is different. Show toast.
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(ALREADY_ASSIGNED_PICKLIST_ARG_DATA, PICK_ASSIGNED_TO_DIFFERENT_USER_TAG)
                )
            }
        }
    }

    private suspend fun handleScannedTote(barcodeType: BarcodeType?) {
        invalidItemScanTracker.reset()
        val lastScannedItem = lastScannedItem // hold onto immutable value for proper null usage below
        when {
            barcodeType !is PickingContainer -> showWrongTote(isPickingContainer = false)
            !pickRepository.isItemIntoPickingContainerValid(lastScannedItem?.item, toteBarcodeType = barcodeType, isMultiSource()) -> showWrongTote(isPickingContainer = true)
            else -> {
                savedToteBarcodeScanned = lastToteBarcodeScanned // save it in case tote reassignment is needed, and the user decides not to reassign
                lastToteBarcodeScanned = barcodeType
                recordPick(lastScannedItem!!, toteBarcodeType = barcodeType)
            }
        }
    }

    private suspend fun handleScannedItem(barcodeType: BarcodeType, doOrderedByWeightCheck: Boolean = true, showMissingItemLocation: Boolean = false) {
        if (hadScanIssue) {
            handleIssueScannedItem(barcodeType)
        } else {
            val itemSearchResult: ItemSearchResult = when {
                barcodeType is BarcodeType.Item -> {
                    lastItemBarcodeScanned = barcodeType
                    pickRepository.getNextItemToSelectForScan(barcodeType, getSelectedItem()).also { scannedItem ->
                        scrollToScannedItem(scannedItem, barcodeType)
                    }
                }

                else -> {
                    acuPickLogger.w("[handleScannedItem] barcodeType is not an item barcode")
                    Error.NoItemFound
                }
            }

            when (itemSearchResult) {
                is MatchedItem -> {
                    val item = itemSearchResult.itemActivityDto
                    if (shouldShowMissingItemLocationScreen(item)) {
                        openMissingItemLocationBottomSheet(
                            PickListScannedData(item, barcodeType)
                        )
                    } else {
                        handleMatchedItem(item, barcodeType, doOrderedByWeightCheck)
                    }
                    // TODO remove this code in next release
                    /*                   if (item.sellByWeightInd == SellByType.PriceWeighted) {
                                           if (item.isFullyPicked() && item.isPickCompleted) {
                                               acuPickLogger.w("[handleScannedItem] all qty for an item has been picked already - cannot pick any more")
                                               handleScannedItemScanFailure(barcodeType)
                                           } else {
                                               val barcode = barcodeType as? BarcodeType.Item.Priced
                                               if (barcode != null) {
                                                   if (siteRepo.isDisplayType3PWEnabled && itemSearchResult.itemActivityDto.isDisplayType3PW()) {
                                                       handleScannedItemSuccessforDisplayType3PW(itemSearchResult.itemActivityDto, barcodeType)
                                                   } else {
                                                       handleScannedItemSuccessforPW(itemSearchResult.itemActivityDto, barcodeType)
                                                   }
                                               } else {
                                                   showSnackBar(StringIdHelper.Id(R.string.incorrect_item_scanned), SnackType.ERROR)
                                               }
                                           }
                                       } else {
                                           if (item.isFullyPicked()) {
                                               acuPickLogger.w("[handleScannedItem] all qty for an item has been picked already - cannot pick any more")
                                               handleScannedItemScanFailure(barcodeType)
                                           } else {
                                               if (item.sellByWeightInd == SellByType.PriceEach) {
                                                   val barcode = barcodeType as? BarcodeType.Item.Priced
                                                   if (barcode != null) {
                                                       handleScannedItemSuccessforPE(itemSearchResult.itemActivityDto, barcode)
                                                   } else {
                                                       showSnackBar(StringIdHelper.Id(R.string.incorrect_item_scanned), SnackType.ERROR)
                                                   }
                                               } else {
                                                   handleScannedItemSuccess(itemSearchResult.itemActivityDto, barcodeType, doOrderedByWeightCheck, showMissingItemLocation)
                                               }
                                           }
                                       }
                   */
                }

                is Error.NoItemFound -> handleScannedItemScanFailure(barcodeType)
                is Error.SecondaryUpcScannedForWeightedItem -> handleScannedItemUpcWeightedFailure()
            }.exhaustive
        }
    }

    fun onMissingItemLocationAdded(missingItemLocationParams: MissingItemLocationResultParams, scannedData: PickListScannedData?) {
        viewModelScope.launch(dispatcherProvider.IO) {
            activityViewModel.setLoadingState(true)
            val result = itemProcessorRepo.captureMissingItemLocation(
                MissingItemLocationRequestDto(
                    pickList.value?.siteId,
                    pickList.value?.actId, scannedData?.item?.id,
                    scannedData?.item?.itemId, scannedData?.item?.primaryUpc,
                    missingItemLocationParams.aisleLocation?.takeIf { it.isNotEmpty() },
                    missingItemLocationParams.section?.takeIf { it.isNotEmpty() },
                    missingItemLocationParams.shelf?.takeIf { it.isNotEmpty() },
                    missingItemLocationParams.locationComment?.takeIf { it.isNotEmpty() },
                    locationReasonCode
                )
            )
            activityViewModel.setLoadingState(false)
            when (result) {
                is ApiResult.Success -> {
                    activityViewModel.bottomSheetRecordPickArgData.postValue(
                        CustomBottomSheetArgData(
                            title = StringIdHelper.Raw(""),
                            exit = true
                        )
                    )
                    if (networkAvailabilityManager.isConnected.first()) {
                        if (scannedData?.scannedBarcodeResult != null || isFromManualEntry) {
                            isLocationUpdated = true
                        } else {
                            showSnackBar(StringIdHelper.Id(R.string.item_location_added), SnackType.SUCCESS)
                        }
                    } else {
                        showSnackBar(StringIdHelper.Id(R.string.item_location_added_offline), SnackType.WARNING)
                    }
                    pickRepository.getActivityDetails(pickListId)
                    scannedData?.let {
                        // Handled manual entry flow
                        when (isFromManualEntry) {
                            true -> openToteScanBottomSheet()
                            else -> it.scannedBarcodeResult?.let { barcodeResult ->
                                handleMatchedItem(it.item, barcodeResult)
                            }
                        }
                    }
                }

                is ApiResult.Failure -> {
                    activityViewModel.bottomSheetRecordPickArgData.postValue(
                        CustomBottomSheetArgData(
                            title = StringIdHelper.Raw(""),
                            exit = false
                        )
                    )
                    if (result is ApiResult.Failure.Server) {
                        if (result.error?.httpErrorCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                            // To do: Show error message from server
                        }
                    }
                }
            }
        }
    }

    private suspend fun scrollToScannedItem(
        itemSearchResult: ItemSearchResult,
        barcodeType: BarcodeType,
    ) {
        if (barcodeType is BarcodeType.Item && (itemSearchResult is MatchedItem || itemSearchResult is Error.SecondaryUpcScannedForWeightedItem)) {
            // Scroll to the scanned item--may have to change tabs
            if (currentTab.value != PickListType.Todo) {
                currentTab.postValue(PickListType.Todo)
            }

            // get the itemActivityDto to find the item in the list to move to
            val itemActivityDto = if (itemSearchResult.isMatchedItem()) {
                itemSearchResult.getItemActivityDto()
            } else {
                pickRepository.getItem(itemBarcodeType = barcodeType, customerOrderNumber = null)
            }

            scrollToPosition.postValue(
                getTodoListItemPos(
                    ScannedPickItem(
                        barcodeType = barcodeType,
                        item = itemActivityDto!!,
                        itemDetails = if (hadScanIssue) fetchItemInfo(barcodeType) else null
                    )
                )
            )

            // The scrollToPosition needs a headstart on showScanItemSuccess.
            // Without this delay, the success snack bar will not trigger in showScanItemSuccess
            delay(250)
        }
    }

    // Handling issue scanned item through scanning
    private suspend fun handleIssueScannedItem(scannedBarcodeResult: BarcodeType) {
        currentItem?.let {
            val isAnItem = scannedBarcodeResult is BarcodeType.Item
            if (!isAnItem) {
                showSnackBar(
                    AcupickSnackEvent(
                        message = StringIdHelper.Id(R.string.no_item_scanned),
                        type = SnackType.ERROR
                    )
                )
                return
            }
            isScanSuccess.postValue(true)
            playScanSound.postValue(true)
            isItemDetailBottomSheetShowing = false
            val itemDetails = fetchItemInfo(scannedBarcodeResult as BarcodeType.Item)
            val scannedPickItem = ScannedPickItem(scannedBarcodeResult, it, itemDetails)
            lastItemBarcodeScanned = scannedBarcodeResult
            lastScannedItem = scannedPickItem

            val showBulk = (itemDetails.bulkVariantList.isNotNullOrEmpty())
            acuPickLogger.d("bulk catalog ${scannedBarcodeResult.catalogLookupUpc}")
            if (showBulk) {
                val bulkItems = itemDetails.bulkVariantList?.map {
                    BulkItem(
                        itemDes = it?.itemDesc,
                        imageUrl = it?.imageURL,
                        itemId = it?.itemId,
                        isSystemSuggested = false
                    )
                }?.toMutableList()?.apply {
                    currentItem?.let { item ->
                        add(
                            0,
                            BulkItem(
                                itemDes = item.itemDescription,
                                imageUrl = item.imageUrl,
                                itemId = item.itemId,
                                isSystemSuggested = false
                            )
                        )
                    }
                } ?: emptyList()

                bulkVariants.clear()
                bulkVariants.addAll(bulkItems)
                bulkItems?.let { launchBulkVariantSelection(it) }
            } else {
                val quantitySelectionType = if (itemDetails == ItemDetailDto.unknownItem) {
                    QuantitySelectionType.None
                } else {
                    getQuantitySelectionTypeForIssueScanning(scannedBarcodeResult)
                }

                when (quantitySelectionType) {
                    QuantitySelectionType.QuantityPicker -> {
                        canAcceptScan = false
                        inlineBottomSheetEvent.postValue(getQuantityPickerArgDataAndTagForBottomSheet(app, scannedPickItem, scannedBarcodeResult, hadScanIssue))
                    }

                    QuantitySelectionType.None ->
                        when (itemDetails.sellByWeightInd) {
                            SellByType.PriceWeighted -> handleIssueScannedItemSuccessforPW(scannedPickItem.item, scannedBarcodeResult as BarcodeType.Item.Priced, itemDetails)
                            SellByType.PriceEach -> handleIssueScannedItemSuccessforPE(scannedPickItem.item, scannedBarcodeResult as BarcodeType.Item.Priced, itemDetails)
                            else -> showScanItemSuccess(scannedPickItem)
                        }

                    else -> {}
                }
            }
        }
    }

    private fun launchBulkVariantSelection(bulkItems: List<BulkItem>) {
        inlineBottomSheetEvent.postValue(
            BottomSheetArgDataAndTag(
                data = CustomBottomSheetArgData(
                    dialogType = BottomSheetType.BulkSubstitution,
                    title = StringIdHelper.Id(R.string.bulk_variant_title),
                    customDataParcel = BulkSubstituteConfirmationParam(
                        bulkItems = bulkItems
                    ),
                    peekHeight = R.dimen.expanded_bottomsheet_peek_height,
                    positiveButtonText = StringIdHelper.Id(R.string.confirm),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                ),
                tag = BULK_VARIANT_BOTTOM_SHEET
            )
        )
    }

    /**
     * Handling PW item success for issue scanning
     */
    private fun handleIssueScannedItemSuccessforPW(item: ItemActivityDto, scannedBarcodeResult: BarcodeType.Item.Priced, itemDetails: ItemDetailDto) {
        val scannedPrice = scannedBarcodeResult.price
        val weight = BigDecimal((scannedPrice) / (itemDetails.basePricePer ?: 1.00)).setScale(2, RoundingMode.HALF_EVEN).toDouble()
        val fulfilledQuantity = 1
        val scannedPickItem = ScannedPickItem(scannedBarcodeResult as BarcodeType.Item, item, itemDetails)
        lastItemBarcodeScanned = scannedBarcodeResult
        lastScannedItem = scannedPickItem
        analyticsHelper.setLastScannedItem(lastScannedItem)

        handleFulfilledQuantityResult(
            FulfilledQuantityResult.ConfirmNetWeightResult(weight, fulfilledQuantity, SellByType.PriceWeighted),
            item
        )
    }

    /**
     * Handling PE item success for issue scanning
     */
    private suspend fun handleIssueScannedItemSuccessforPE(item: ItemActivityDto, scannedBarcodeResult: BarcodeType.Item.Priced, itemDetails: ItemDetailDto) {
        val scannedPrice = scannedBarcodeResult.price.toBigDecimal()
        val quantity = (scannedPrice) / (itemDetails.basePrice?.toBigDecimal() ?: BigDecimal.ONE)
        if ((quantity.toDouble() % 1) != 0.0) { // if quantity is a fraction show error
            acuPickLogger.w(
                "[showErrorInQuantity] quantity: $quantity,scannedPrice: $scannedPrice, basePrice: ${itemDetails.basePrice}, rawbasePrice: ${scannedBarcodeResult.rawBarcode}," +
                    "sellByWeightInd: ${itemDetails.sellByWeightInd}, itemID: ${itemDetails.itemId}"
            )
            showErrorInQuantity()
            delay(250)
            /**
             * If we are in continue issue scanning flow then do not call returnToInitialState()
             * returnToInitialState() will call only if we have not scanned any item with issue scanning.
             */
            if (isIssueScanItemAvailable().not()) {
                withContext(dispatcherProvider.Main) {
                    returnToInitialState()
                }
            }
        } else {
            val scannedPickItem = ScannedPickItem(scannedBarcodeResult as BarcodeType.Item, item, itemDetails)
            lastItemBarcodeScanned = scannedBarcodeResult
            lastScannedItem = scannedPickItem
            analyticsHelper.setLastScannedItem(lastScannedItem)
            handleFulfilledQuantityResult(
                FulfilledQuantityResult.QuantityPicker(
                    quantity.toInt()
                ),
                item
            )
        }
    }

    private suspend fun handleScannedItemSuccessforPW(item: ItemActivityDto, scannedBarcodeResult: BarcodeType.Item.Priced) {
        val scannedPrice = scannedBarcodeResult.price
        val weight = BigDecimal((scannedPrice) / (item.basePricePer ?: 1.00)).setScale(2, RoundingMode.HALF_EVEN).toDouble()
        // val weight = .4

        // Fresh Scan : fulQty = (round(scanedWt/itemWt))<=OrderQty? round(scaneedWt/itemWt):Ordered quantity
        // Next scan : fulQty= ((round((processWt+scanedWt)/itemWt))<=(OrderQty))? (round(processWt+/scanedWt)-proceQty) : (OrderQty-proceQty)
        val scannedQuantity = round(weight / (item.itemWeight?.toDoubleOrNull() ?: 1.0)).toInt()
        val processedWeight = item.processedWeight ?: 0.0
        val totalWeight = processedWeight.plus(weight)
        val orderedQantity = (item.qty ?: 0.0).toInt()
        val processedQuantity = (item.processedQty ?: 0.0).toInt()
        val itemWeight = item.itemWeight?.toDoubleOrNull() ?: 1.0

        val fulfilledQuantity =
            if (processedWeight == 0.0) {
                if (scannedQuantity < orderedQantity) {
                    scannedQuantity
                } else {
                    orderedQantity
                }
            } else {
                Timber.v(
                    "orderedQantity num ${((item.processedWeight ?: 0.0) + weight)}" +
                        "\ndem ${round((item.processedWeight ?: 0.0) + weight / (item.itemWeight?.toDoubleOrNull() ?: 1.0)).toInt()}"
                )

                if (round((totalWeight) / (itemWeight)) <= orderedQantity) {
                    round((totalWeight) / (itemWeight)).toInt() - processedQuantity
                } else {
                    orderedQantity - processedQuantity
                }
            }

        Timber.v("orderedQantity-- $orderedQantity\n + scannedQuantity= $fulfilledQuantity")

        if ((totalWeight ?: 1.0) > item.requestedNetWeight * 2) {
            showScanHeavy()
        } else {
            isScanSuccess.postValue(true)
            playScanSound.postValue(true)

            val itemDetails = if (hadScanIssue) {
                fetchItemInfo(scannedBarcodeResult as BarcodeType.Item)
            } else null

            val scannedPickItem = ScannedPickItem(scannedBarcodeResult as BarcodeType.Item, item, itemDetails)
            lastItemBarcodeScanned = scannedBarcodeResult
            lastScannedItem = scannedPickItem
            analyticsHelper.setLastScannedItem(lastScannedItem)

            handleFulfilledQuantityResult(
                FulfilledQuantityResult.ConfirmNetWeightResult(weight, fulfilledQuantity, SellByType.PriceWeighted),
                item
            )
        }
    }

    private suspend fun handleScannedItemSuccessforDisplayType3PW(item: ItemActivityDto, scannedBarcodeResult: BarcodeType.Item.Priced) {
        val weight = BigDecimal((scannedBarcodeResult.price) / (item.basePricePer ?: 1.00)).setScale(2, RoundingMode.HALF_EVEN).toDouble()
        val processedWeight = item.processedWeight ?: 0.0
        val orderedQantity = (item.qty ?: 0.0).toInt()
        val totalScannedWeight = processedWeight.plus(weight)
        val maxWeight = item.orderedWeight?.plus((siteRepo.weightedItemThreshold * item.minWeight.orZero())).orZero()
        val scannedQuantity = round(weight / (item.minWeight ?: 1.0)).toInt()
        val itemWeight = item.minWeight ?: 1.0
        val processedQuantity = (item.processedQty ?: 0.0).toInt()

        val fulfilledQuantity =
            if (processedWeight == 0.0) {
                if (scannedQuantity < orderedQantity) {
                    scannedQuantity
                } else {
                    orderedQantity
                }
            } else {
                if (round((totalScannedWeight) / (itemWeight)) <= orderedQantity) {
                    round((totalScannedWeight) / (itemWeight)).toInt() - processedQuantity
                } else {
                    orderedQantity - processedQuantity
                }
            }

        if (totalScannedWeight > maxWeight) {
            showScanHeavy(maxWeight)
        } else {
            isScanSuccess.postValue(true)
            playScanSound.postValue(true)

            val itemDetails = if (hadScanIssue) {
                fetchItemInfo(scannedBarcodeResult as BarcodeType.Item)
            } else null

            val scannedPickItem = ScannedPickItem(scannedBarcodeResult as BarcodeType.Item, item, itemDetails)
            lastItemBarcodeScanned = scannedBarcodeResult
            lastScannedItem = scannedPickItem
            analyticsHelper.setLastScannedItem(lastScannedItem)

            handleFulfilledQuantityResult(
                FulfilledQuantityResult.ConfirmNetWeightResult(weight, fulfilledQuantity, SellByType.PriceWeighted),
                item
            )
        }
    }

    private suspend fun handleScannedItemSuccessforPE(item: ItemActivityDto, scannedBarcodeResult: BarcodeType.Item.Priced) {
        // https://stackoverflow.com/questions/68044639/how-to-format-numbers-with-two-decimal-places
        val scannedPrice = scannedBarcodeResult.price.toBigDecimal()
        val quantity = (scannedPrice) / (item.basePrice?.toBigDecimal() ?: BigDecimal.ONE)
        val orderedQuantity = item.qty?.toInt() ?: 1
        val totalQuantity = quantity.toDouble().plus(item.processedQty ?: 0.0).toInt()

        if ((quantity.toDouble() % 1) != 0.0) { // if quantity is a fraction show error
            acuPickLogger.w(
                "[showErrorInQuantity] quantity: $quantity,scannedPrice: $scannedPrice,basePrice: ${item.basePrice}," +
                    " rawbasePrice: ${scannedBarcodeResult.rawBarcode}, sellByWeightInd: ${item.sellByWeightInd}, itemID: ${item.itemId}"
            )
            showErrorInQuantity()
            delay(250)
            showPersistentSnackbarPrompt(showOrderIssue = true)
        } else if (totalQuantity > orderedQuantity) {
            showQuantityExceedsError()
        } else {
            isScanSuccess.postValue(true)
            playScanSound.postValue(true)

            val itemDetails = if (hadScanIssue) {
                fetchItemInfo(scannedBarcodeResult as BarcodeType.Item)
            } else null

            val scannedPickItem = ScannedPickItem(scannedBarcodeResult as BarcodeType.Item, item, itemDetails)

            lastItemBarcodeScanned = scannedBarcodeResult
            lastScannedItem = scannedPickItem
            analyticsHelper.setLastScannedItem(lastScannedItem)

            handleFulfilledQuantityResult(
                FulfilledQuantityResult.QuantityPicker(
                    if (totalQuantity > orderedQuantity)
                        orderedQuantity - (item.processedQty?.toInt() ?: 0)
                    else {
                        quantity.toInt()
                    }
                ),
                item
            )
        }
    }

    private suspend fun handleScannedItemSuccess(item: ItemActivityDto, scannedBarcodeResult: BarcodeType, doOrderedByWeightCheck: Boolean) {

        if (doOrderedByWeightCheck && isScannedWeightOutOfRange(item, scannedBarcodeResult, false)) return

        isScanSuccess.postValue(true)
        playScanSound.postValue(true)

        continueScannedItemSuceess(item, scannedBarcodeResult)
    }

    suspend fun handleMatchedItem(item: ItemActivityDto, barcodeType: BarcodeType, doOrderedByWeightCheck: Boolean = true) {
        if (item.sellByWeightInd == SellByType.PriceWeighted) {
            if (item.isFullyPicked() && item.isPickCompleted) {
                acuPickLogger.w("[handleScannedItem] all qty for an item has been picked already - cannot pick any more")
                handleScannedItemScanFailure(barcodeType)
            } else {
                val barcode = barcodeType as? BarcodeType.Item.Priced
                if (barcode != null) {
                    if (siteRepo.isDisplayType3PWEnabled && item.isDisplayType3PW()) {
                        handleScannedItemSuccessforDisplayType3PW(item, barcodeType)
                    } else {
                        handleScannedItemSuccessforPW(item, barcodeType)
                    }
                } else {
                    showSnackBar(StringIdHelper.Id(R.string.incorrect_item_scanned), SnackType.ERROR)
                }
            }
        } else {
            if (item.isFullyPicked()) {
                acuPickLogger.w("[handleScannedItem] all qty for an item has been picked already - cannot pick any more")
                handleScannedItemScanFailure(barcodeType)
            } else {
                if (item.sellByWeightInd == SellByType.PriceEach) {
                    val barcode = barcodeType as? BarcodeType.Item.Priced
                    if (barcode != null) {
                        handleScannedItemSuccessforPE(item, barcode)
                    } else {
                        showSnackBar(StringIdHelper.Id(R.string.incorrect_item_scanned), SnackType.ERROR)
                    }
                } else {
                    handleScannedItemSuccess(item, barcodeType, doOrderedByWeightCheck)
                }
            }
        }
    }

    suspend fun continueScannedItemSuceess(item: ItemActivityDto, scannedBarcodeResult: BarcodeType?) {
        val itemDetails = if (hadScanIssue) {
            isItemDetailBottomSheetShowing = false
            fetchItemInfo(scannedBarcodeResult as BarcodeType.Item)
        } else null
        val scannedPickItem = ScannedPickItem(scannedBarcodeResult as BarcodeType.Item, item, itemDetails)
        val quantitySelectionType = if (itemDetails == ItemDetailDto.unknownItem) {
            QuantitySelectionType.None
        } else {
            getQuantitySelectionType(item, scannedBarcodeResult)
        }

        // Reset pick quantity
        fulfilledQtyResult = FulfilledQuantityResult.DefaultQuantity

        when (quantitySelectionType) {
            QuantitySelectionType.QuantityPicker -> {
                canAcceptScan = false
                inlineBottomSheetEvent.postValue(getQuantityPickerArgDataAndTagForBottomSheet(app, scannedPickItem, scannedBarcodeResult, hadScanIssue))
            }

            QuantitySelectionType.ConfirmAmount -> {
                canAcceptScan = false
                showConfirmAmountBottomSheet(item)
            }

            QuantitySelectionType.None -> {
                /* ACIP-355128 - Preparing ConfirmNetWeightResult object only for W item and display type 3. */
                handleWeightedItemDisplayType3(scannedPickItem)
                showScanItemSuccess(scannedPickItem)
            }
        }

        lastItemBarcodeScanned = scannedBarcodeResult
        lastScannedItem = scannedPickItem
        analyticsHelper.setLastScannedItem(lastScannedItem)
    }

    private fun handleWeightedItemDisplayType3(scannedPickItem: ScannedPickItem) {
        if (scannedPickItem.item.sellByWeightInd == SellByType.Weight && scannedPickItem.item.isOrderedByWeight()) {
            fulfilledQtyResult = (scannedPickItem.barcodeType as? BarcodeType.Item.Weighted)?.weight?.let { scannedWeight ->
                FulfilledQuantityResult.ConfirmNetWeightResult(scannedWeight, fulfilledQtyResult.toQuantity(), SellByType.Weight)
            } ?: FulfilledQuantityResult.DefaultQuantity
        }
    }

    private fun showConfirmAmountBottomSheet(item: ItemActivityDto) {
        inlineBottomSheetEvent.postValue(getConfirmAmountArgDataAndTagForBottomSheet(item))
    }

    private fun isScannedWeightOutOfRange(item: ItemActivityDto, barcodeType: BarcodeType, isManualEntry: Boolean): Boolean {
        if (item.isOrderedByWeight()) {
            val scannedItemWeight = (barcodeType as BarcodeType.Item.Weighted).weight
            when {
                scannedItemWeight < (item.lowerWeightLimit ?: Double.MAX_VALUE) -> true
                scannedItemWeight > (item.upperWeightLimit ?: Double.MIN_VALUE) -> false
                else -> null
            }?.let { isTooLight ->
                inlineDialogEvent.postValue(
                    getOrderedByWeightArgAndData(
                        item = item,
                        scannedItemWeight = scannedItemWeight,
                        isScannedItemTooLight = isTooLight,
                        isManualEntry = isManualEntry,
                    )
                )
                return true
            }
        }
        return false
    }

    /** Try to fetch info about item for the Scan Item Issue flow */
    private suspend fun fetchItemInfo(itemBarcodeType: BarcodeType.Item): ItemDetailDto =
        if (!networkAvailabilityManager.isConnected.first()) {
            ItemDetailDto.unknownItem
        } else {
            when (
                val result = isBlockingUi.wrap {
                    pickRepository.getItemDetails(
                        siteId = userRepo.user.value!!.selectedStoreId.orEmpty(),
                        upcId = itemBarcodeType.catalogLookupUpc,
                        queryType = if (hadScanIssue) SubReasonCode.IssueScanning.text() else null
                    )
                }
            ) {
                is ApiResult.Success -> result.data
                is ApiResult.Failure -> ItemDetailDto.unknownItem
            }
        }

    private suspend fun showScanItemSuccess(scannedItem: ScannedPickItem, weight: Double = 0.0) {
        withContext(dispatcherProvider.Main) {
            delay(50)
            // showScanSuccessSnackbar(scannedItem.barcodeType, scannedItem.item.sellByWeightInd ?: SellByType.RegularItem, weight)
            activeScanTarget.postValue(ScanTarget.Tote)
            lastScannedItem = scannedItem
            analyticsHelper.setLastScannedItem(lastScannedItem)
            suggestedToteId = null
            findSuggestedTote(scannedItem.item)
            // Validating to open missing item location bottomsheet in manual entry flow
            if (isFromManualEntry && shouldShowMissingItemLocationScreen(scannedItem.item)) {
                openMissingItemLocationBottomSheet(
                    PickListScannedData(scannedItem.item, scannedItem.barcodeType)
                )
            } else {
                openToteScanBottomSheet()
            }
        }
    }

    /**
     * Validating to open missing item location bottomsheet based on
     * [ItemActivityDto.itemAddressDto] [ItemActivityDto.locationDetail] and store level flag.
     */
    private fun shouldShowMissingItemLocationScreen(item: ItemActivityDto): Boolean {
        val itemLocation = item.itemAddressDto?.asItemLocation(app).isNullOrEmpty()
        val locationDetail = item.locationDetail.isNullOrEmpty()
        val isFullyPicked = item.isFullyPicked().not()
        val isMissingLocationEnabledInStoreLeveL = siteRepo.siteDetails.value?.storeLevelTempFlags?.missingLocationEnabledApp.toBoolean()
        val isDepNameNotInMissingItemLocDisabledDepts = pickList.value?.missingItemLocDisabledDepts?.map { it.lowercase() }?.contains(item.depName?.lowercase()) == false
        return itemLocation && locationDetail && isFullyPicked && isMissingLocationEnabledInStoreLeveL && isDepNameNotInMissingItemLocDisabledDepts
    }
    fun openToteScanBottomSheet() {
        inlineBottomSheetEvent.postValue(getToteScanArgDataAndTagForBottomSheet(suggestedToteId, currentItem?.isCustomerBagPreference))
    }

    private suspend fun handleScannedItemScanFailure(scannedBarcodeResult: BarcodeType) {

        if (scannedBarcodeResult is BarcodeType.Item) {
            acuPickLogger.reportMetric("[handleScannedItem] item barcode scanned is not in this pick list")
            invalidItemScanTracker.trackInvalidItemScan(scannedBarcodeResult)
        }

        isScanSuccess.postValue(false)
        playScanSound.postValue(false)

        // Commenting this feature when the number of wrong scan exceeds more than two
        /*        if (invalidItemScanTracker.isInvalidItemScanLimitReached()) {
                    viewModelScope.launch {
                        val selectedItem = getSelectedItem()
                        if (selectedItem != null && !isSubstitutionAllowed(selectedItem)) {
                            onSubstituteCtaClicked(selectedItem)
                        }
                    }
                }*/

        isShowingScanPrompt.postValue(false)
        val isAnItem = scannedBarcodeResult is BarcodeType.Item
        scannedItemFailure.postValue(isAnItem)
        if (isAnItem && siteRepo.areIssueScanningFeaturesEnabled && !pickList.value?.is3p.orFalse()) {
            showAnchoredSnackBar(
                AcupickSnackEvent(
                    message = StringIdHelper.Id(R.string.wrong_item_scanned),
                    type = SnackType.ERROR,
                    duration = SnackDuration.LENGTH_LONG,
                    action = SnackAction(StringIdHelper.Id(R.string.issue_scanning_an_item)) {
                        if (isListView.value == true && !isItemDetailBottomSheetShowing) {
                            inlineDialogEvent.postValue(getSelectSpecificItemArgDataAndTag())
                        } else {
                            onItemScanErrorCtaClicked(getSelectedItem())
                        }
                    }
                )
            )
        } else showSnackBar(displayItemScanFailureSnackbar(isAnItem)) // ISSUE-SCANNING if issue scanning feature is NOT supported in a store
    }

    private suspend fun handleScannedItemUpcWeightedFailure() {
        isScanSuccess.postValue(false)
        playScanSound.postValue(false)

        // show the error snackbar
        isShowingScanPrompt.postValue(false)
        scannedItemFailure.postValue(false)
        showSnackBar(StringIdHelper.Id(R.string.incorrect_item_weighted), SnackType.ERROR)
        withContext(dispatcherProvider.Main) {
            delay(SCAN_STATUS_MESSAGE_DURATION_MS)
            showPersistentSnackbarPrompt(showOrderIssue = true)
        }
    }

    private suspend fun showWrongTote(isPickingContainer: Boolean) {
        playScanSound.postValue(false)
        isShowingScanPrompt.postValue(false)
        isScanSuccess.postValue(false)
        showAnchoredSnackBar(displayToteScanFailureSnackbar(isPickingContainer))
        withContext(dispatcherProvider.Main) {
            delay(SCAN_STATUS_MESSAGE_DURATION_MS)
            showPersistentSnackbarPrompt()
        }
    }

    private suspend fun showScanHeavy(maxWeight: Double? = null) {
        playScanSound.postValue(false)
        isShowingScanPrompt.postValue(false)
        isScanSuccess.postValue(false)
        showAnchoredSnackBar(if (maxWeight == null) displayScanHeavyFailureSnackbar() else displayScanHeavyDsiplayType3FailureSnackbar(maxWeight))
        withContext(dispatcherProvider.Main) {
            delay(SCAN_STATUS_MESSAGE_DURATION_MS)
            showPersistentSnackbarPrompt()
        }
    }

    private suspend fun showErrorInQuantity() {
        playScanSound.postValue(false)
        isShowingScanPrompt.postValue(false)
        isScanSuccess.postValue(false)
        showAnchoredSnackBar(displayErrorInQuanntityFailureSnackbar())
        withContext(dispatcherProvider.Main) {
            delay(SCAN_STATUS_MESSAGE_DURATION_MS)
            showPersistentSnackbarPrompt()
        }
    }

    private suspend fun showQuantityExceedsError() {
        playScanSound.postValue(false)
        isShowingScanPrompt.postValue(false)
        isScanSuccess.postValue(false)
        showAnchoredSnackBar(displayQuanntityExceedsSnackbar())
        withContext(dispatcherProvider.Main) {
            delay(SCAN_STATUS_MESSAGE_DURATION_MS)
            showPersistentSnackbarPrompt()
        }
    }

    private suspend fun findSuggestedTote(item: ItemActivityDto?) {
        val suggestedTote = item?.let { pickRepository.findExistingValidToteForItem(it) }
        if (suggestedTote != null) {
            val suggestedContainerId = suggestedTote.containerId ?: ""
            acuPickLogger.v("[showTotePrompt] containerId=$suggestedContainerId")
            suggestedToteId = suggestedContainerId
        }
    }

    private fun getTodoListItemPos(scannedItem: ScannedPickItem): Int {
        return todoPickList.value?.itemActivities?.indexOfOrNull(scannedItem.item) ?: 0
    }

    private fun getSelectedItem(): ItemActivityDto? {
        acuPickLogger.v("[getSelectedItem] selected item index=${todoSelectedItemIndex.value}")
        return todoPickList.value?.itemActivities?.getOrNull(todoSelectedItemIndex.value)
    }

    /** Do any kind of updates needed after recording/undoing a pick */
    fun updatePickingStatus() {
        // Gate changes to pick complete state when UI is not active to prevent completePicking api calls when fragment is in background (and viewmodel is still alive)
        if (isUiActive && !isFromManualEntry) {
            showPersistentSnackbarPrompt()
        }

        // TODO Need to refactor after re designing early exit flow.
        if (areAllItemsPicked && isUiActive) {

            // Only make 1 call at a time - don't call again once successful
            val attemptCompleteCall = !completeCallInProgress && !completeCallSuccessful
            when {
                attemptCompleteCall -> {
                    completeCallInProgress = true
                    if (areAllItemsShorted) {
                        if (isWineShipping()) {
                            inlineDialogEvent.postValue(getEarlyExitWineArgDataAndTag())
                        }
                    }
                }

                completeCallSuccessful -> acuPickLogger.w("[updatePickingStatus] complete picking api already returned successful result - no need to call again")
                completeCallInProgress -> acuPickLogger.w("[updatePickingStatus] complete picking api call in progress - bypass making another api call")
            }
        }
    }

    private fun preCacheImages(activityDto: ActivityDto) {
        // Not pre-caching ItemZoom, when offline the zoom screen will use the PickList image.
        imagePreCacher.preCacheImages(activityDto.itemActivities.orEmpty().map { it.sizedImageUrl(ImageSizePreset.ItemDetails) })
        imagePreCacher.preCacheImages(activityDto.itemActivities.orEmpty().map { it.sizedImageUrl(ImageSizePreset.PickList) })
    }

    private fun manualEntryComplete(scannedItem: ScannedPickItem) {
        viewModelScope.launch {
            showScanItemSuccess(scannedItem)
            if (scannedItem.barcodeType !is BarcodeType.Item.Each) {
                _todoSelectedItemIndex.value = getTodoListItemPos(scannedItem)
            }
        }
    }

    fun clearInvalidItemScanTracker() {
        invalidItemScanTracker.reset()
        scannedItemFailure.postValue(false)
    }

    // /////////////////////////////////////////////////////////////////////////
    // Snackbar Messages
    // /////////////////////////////////////////////////////////////////////////

    // Persistent Snackbars
    fun showPersistentSnackbarPrompt(fromSelectedItemChange: Boolean = false, showOrderIssue: Boolean = false) {
        if (isDataLoading.value == false) {
            clearSnackBarEvents()
            when (activeScanTarget.value) {
                ScanTarget.Item -> displayItemPersistentSnackbar(showOrderIssue)
                ScanTarget.Tote -> displayTotePersistentSnackbar(fromSelectedItemChange)
                else -> Unit
            }.exhaustive
        }
    }

    private fun displayItemPersistentSnackbar(showOrderIssue: Boolean) {
        // TODO: Remove Picking Snackbar Code when all Sell By Types are covered after integration
        // TODO: Cover Item Scan Error Cta flow
        // TODO: Will remove commented code after complete testing of issue scanning feature
        viewModelScope.launch(dispatcherProvider.Main) {
            if (areAllItemsPicked) {
                // showSnackBar(showEndPickSnackBar(action = { onEndPickCtaClicked() }))
                prompt.postValue(PickListItemsBottomPrompt.None)
            } else {
                if (currentTab.value == PickListType.Todo) {
                    val selectedItem = getSelectedItem()
                    when (selectedItem?.sellByWeightInd) {
                        SellByType.Weight ->
                            PickListItemsBottomPrompt.Weight { onManualEntryCtaClicked() }.let { prompt.postValue(it) }
                        // showSnackBar(
                        //     showWeightedItemScanbar(
                        //         selectedItem,
                        //         showOrderIssue,
                        //         siteRepo.areIssueScanningFeaturesEnabled,
                        //         action = { onItemScanErrorCtaClicked(selectedItem) }
                        //     )
                        // )
                        SellByType.Each,
                        -> {
                            val selectedItemPluCode = selectedItem.pluList?.getOrNull(0)
                            if (selectedItemPluCode?.toIntOrNull() == null || selectedItemPluCode.toIntOrNull() == 0) {
                                // The item has no plu number, Picker enters PLU on Manual Entry Page
                                // showSnackBar(showEachesItemNoPLUSnackbar(action = { onManualEntryCtaClicked() }))
                                val data = PickListItemsBottomPrompt.Eaches(R.string.enter_the_plu_number, ::onManualEntryCtaClicked)
                                prompt.postValue(data)
                            } else {
                                // We know the items PLU number, take the user to Count page of Manual Entry Page
                                val barcodeType = barcodeMapper.generateEachBarcode(selectedItemPluCode, selectedItem.id)
                                // showSnackBar(
                                //     showEachesItemPLUSnackbar(
                                //         selectedItem,
                                //         action = {
                                //             lastItemBarcodeScanned = barcodeType
                                //             lastScannedItem = ScannedPickItem(barcodeType, selectedItem)
                                //             analyticsHelper.setLastScannedItem(lastScannedItem)
                                //             inlineDialogEvent.postValue(
                                //                 getQuantityPickerArgDataAndTag(
                                //                     app, ScannedPickItem(barcodeType = barcodeType, item = selectedItem), barcodeType, hadScanIssue
                                //                 )
                                //             )
                                //         }
                                //     )
                                // )
                                val data = PickListItemsBottomPrompt.Eaches(R.string.select_quantity) {
                                    openSelectQuantityBottomSheet(barcodeType, selectedItem)
                                }
                                prompt.postValue(data)
                            }
                        }

                        SellByType.PriceScaled, SellByType.PriceEachTotal ->
                            // showSnackBar(
                            //     showPricedItemSnackbar(
                            //         siteRepo.areIssueScanningFeaturesEnabled,
                            //         showOrderIssue,
                            //         action = { onItemScanErrorCtaClicked(selectedItem) }
                            //     )
                            // )
                            PickListItemsBottomPrompt.Default { onManualEntryCtaClicked() }.let { prompt.postValue(it) }
                        // TODO: ACURED_REDESIGN Showing only instruction message as per figma will implement issue scanning for PW item
                        SellByType.PriceWeighted -> {
                            PickListItemsBottomPrompt.Default { onManualEntryCtaClicked() }.let { prompt.postValue(it) }
                            /*val processedQty = selectedItem.processedQty.roundToLongOrZero().toString()
                            val totalQty = selectedItem.qty.roundToLongOrZero().toString()
                            if (processedQty == totalQty) {
                                showSnackBar(
                                    showPricedWeightedItemSnackbar(
                                        siteRepo.areIssueScanningFeaturesEnabled,
                                        showOrderIssue,
                                        action = { onItemScanErrorCtaClicked(selectedItem) }
                                    )
                                )
                            } else {
                                showSnackBar(
                                    showGenericItemSnackbar(
                                        siteRepo.areIssueScanningFeaturesEnabled,
                                        showOrderIssue,
                                        action = { onItemScanErrorCtaClicked(selectedItem) }
                                    )
                                )
                            }*/
                        }
                        // TODO: ACURED_REDESIGN Showing only instruction message as per figma will implement issue scanning for PE item
                        SellByType.PriceEach -> {
                            PickListItemsBottomPrompt.Default { onManualEntryCtaClicked() }.let { prompt.postValue(it) }
                            /* showSnackBar(
                                 showPricedEachItemSnackbar(
                                     siteRepo.areIssueScanningFeaturesEnabled,
                                     showOrderIssue,
                                     action = { onItemScanErrorCtaClicked(selectedItem) }
                                 )
                             )*/
                        }

                        else -> {
                            // The item is being picked by UPC or is a Priced Item
                            /*showSnackBar(
                                showGenericItemSnackbar(
                                    siteRepo.areIssueScanningFeaturesEnabled,
                                    showOrderIssue,
                                    action = { onItemScanErrorCtaClicked(selectedItem) }
                                )
                            )*/
                            PickListItemsBottomPrompt.Default { onManualEntryCtaClicked() }.let { prompt.postValue(it) }
                        }
                    }
                }
            }
        }
    }

    private fun openSelectQuantityBottomSheet(barcodeType: BarcodeType.Item, selectedItem: ItemActivityDto) {
        lastItemBarcodeScanned = barcodeType
        lastScannedItem = ScannedPickItem(barcodeType, selectedItem)
        analyticsHelper.setLastScannedItem(lastScannedItem)
        canAcceptScan = false
        val (scannedItem, barcode) = lastScannedItem to lastItemBarcodeScanned
        if (scannedItem != null && barcode != null) {
            inlineBottomSheetEvent.postValue(getQuantityPickerArgDataAndTagForBottomSheet(app, scannedItem, barcode, hadScanIssue))
        }
    }

    fun handlePluCtaResult() {
        val selectedItem = getSelectedItem()
        val selectedItemPluCode = selectedItem?.pluList?.getOrNull(0)
        if (selectedItemPluCode?.toIntOrNull() == null || selectedItemPluCode.toIntOrNull() == 0) {
            onManualEntryCtaClicked()
        } else {
            val barcodeType = barcodeMapper.generateEachBarcode(selectedItemPluCode, selectedItem.id)
            openSelectQuantityBottomSheet(barcodeType, selectedItem)
        }
    }

    fun onAddLocationClicked() {
        openMissingItemLocationBottomSheet(currentItem?.let { PickListScannedData(it, null) })
    }

    private fun openMissingItemLocationBottomSheet(scannedData: PickListScannedData? = null) {
        inlineBottomSheetEvent.postValue(
            BottomSheetArgDataAndTag(
                data = CustomBottomSheetArgData(
                    dialogType = BottomSheetType.MissingItemLocation,
                    title = StringIdHelper.Id(R.string.add_location),
                    isFullScreen = true,
                    customDataParcel = MissingItemLocationParams(
                        itemDescription = scannedData?.item?.itemDescription.orEmpty(),
                        itemImage = scannedData?.item?.sizedImageUrl(ImageSizePreset.PickList),
                        itemUpcId = scannedData?.item?.asUpcOrPlu(app.applicationContext, barcodeMapper).orEmpty(),
                        itemLocation = scannedData?.item?.asItemLocation(app.applicationContext).orEmpty(),
                        scannedData = scannedData
                    )
                ),
                tag = MISSING_ITEM_LOCATION_DIALOG_TAG
            )
        )
    }

    fun onWhereToFindLocationCode() {
        inlineBottomSheetEvent.postValue(
            BottomSheetArgDataAndTag(
                data = CustomBottomSheetArgData(
                    dialogType = BottomSheetType.WhereToFindLocationCode,
                    title = StringIdHelper.Id(R.string.where_to_find_location_code),
                    isFullScreen = true,
                ),
                tag = WHERE_TO_FIND_LOCATION_DIALOG_TAG
            )
        )
    }

    private fun displayTotePersistentSnackbar(fromSelectedItemChange: Boolean = false) {
        if (currentTab.value == PickListType.Todo && !fromSelectedItemChange) {
            // showSnackBar(showToteSnackbar(suggestedToteId))
        }
    }

    // Successfull Messages
    private fun showScanSuccessSnackbar(barcodeType: BarcodeType?, sellByType: SellByType, weight: Double = 0.0) {
        isScanSuccess.postValue(true)
        when (barcodeType) {
            is BarcodeType.Item -> showSnackBar(displayItemScanSuccessfulSnackbar(sellByType, barcodeType.rawBarcode, isFromManualEntry, weight))
            is PickingContainer -> {
                suggestedToteId = null
                showSnackBar(displayToteScanSuccessfulSnackbar(barcodeType.rawBarcode))
            }

            else -> Unit
        }.exhaustive
    }

    // true  || (false && true)  -> enabled // When server send sub allowed, and issueScanningFeatures is enabled
    // false || (false && false) -> disabled //  When server send sub allowed not allowed and issueScanningFeatures is enabled
    // false || (true  && false) -> disabled (greyed out)// greyed out but takes user to sub screen to override server falg and issueScanningFeatures is disabled
    fun isSubstitutionAllowed(item: ItemActivityDto) =
        item.subAllowed == true || (!siteRepo.areIssueScanningFeaturesEnabled && item.subCode != SubstitutionCode.NOT_ALLOWED)

    private fun onLabelClicked() {
        when (currentTab.value) {
            PickListType.Short -> {
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = CustomDialogArgData(
                            titleIcon = null,
                            title = StringIdHelper.Id(R.string.item_details_undo_short_title),
                            body = StringIdHelper.Id(R.string.item_details_undo_short_body),
                            imageUrl = currentItem?.sizedImageUrl(ImageSizePreset.ItemDetails).orEmpty(),
                            secondaryBody = StringIdHelper.Raw(currentItem?.itemDescription ?: ""),
                            questionBody = StringIdHelper.Raw(currentItem.asUpcOrPlu(app.applicationContext, barcodeMapper)),
                            orderedWeightOrRemainingQty =
                            if (siteRepo.isDisplayType3PWEnabled && currentItem?.isDisplayType3PW() == true) currentItem?.getWeightAndUom()
                            else currentItem?.exceptionQty.orZero().toString(),
                            positiveButtonText = StringIdHelper.Id(R.string.confirm),
                            negativeButtonText = StringIdHelper.Id(R.string.cancel),
                            dialogType = DialogType.ConfirmItem
                        ),
                        tag = ItemDetailsViewModel.CONFIRMATION_UNDO_SHORT_DIALOG_TAG
                    )
                )
            }

            PickListType.Picked -> handleLabelClickedOnPickedTab()

            PickListType.Todo -> {
                viewModelScope.launch {
                    // wait for the bottom sheet to dismiss
                    delay(500)
                    currentItem?.let { onSubstituteCtaClicked(it) }
                }
            }

            else -> {}
        }
    }

    /**
     * Handle can't find, unpick, re-pick-original CTA clicks from list item and item detail bottomsheet
     * at common method for [PickListType.Picked]
     */
    private fun handleLabelClickedOnPickedTab() {
        when (shouldRepickOriginalItemAllowed()) {
            true -> showRemoveSubstitutedItemDialog()
            else -> {
                currentItem?.id?.let { iaId ->
                    inlineBottomSheetEvent.postValue(
                        getUnPickArgDataAndTagForBottomSheet(
                            iaId = iaId,
                            actId = pickList.value?.actId ?: 0,
                            activityNo = pickList.value?.activityNo.orEmpty(),
                            item = currentItem,
                            pickListType = currentTab.value ?: PickListType.Todo
                        )
                    )
                }
            }
        }
    }

    // To open confirmation dialog of substituted item only.
    private fun showRemoveSubstitutedItemDialog() = currentItem?.toSwapItem()?.substitutedWith?.let {
        inlineDialogEvent.postValue(getRemoveSubstitutionDialogArgDataAndTag(it))
    }

    // To validate re-pick original item flow should be allowed
    private fun shouldRepickOriginalItemAllowed() = siteRepo.twoWayCommsFlags.allowRepickOriginalItem.orFalse() && (currentItem?.isSubstituted.orFalse() || currentItem?.isIssueScanned.orFalse())

    // Undopick perform on substituted item only in re-pick original item flow
    private fun undoPickSubstitutedItem() {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { undoPickSubstitutedItem() }
            } else {
                currentItem?.toSwapItem()?.substitutedWith?.let { substitutionList ->
                    if (substitutionList.isEmpty()) return@launch
                    val substitutionRemovedQty = substitutionList.sumBy { it.qty?.toInt().getOrZero() }
                    val requests =
                        substitutionList.map {
                            UndoPickLocalDto(
                                containerId = it.containerId,
                                undoPickRequestDto = UndoPickRequestDto(
                                    actId = pickRepository.pickList.first()?.actId ?: 0,
                                    iaId = currentItem?.id,
                                    netWeight = it.netWeight,
                                    pickedUpcId = it.upcId,
                                    qty = it.qty,
                                    rejectionReason = SubstitutionRejectedReason.SWAP,
                                )
                            )
                        }
                    val results =
                        isBlockingUi.wrap {
                            pickRepository.undoPicks(requests)
                        }
                    if (results is ApiResult.Failure) {
                        withContext(dispatcherProvider.Main) {
                            toaster.toast(app.getString(R.string.item_details_undo_error))
                        }
                    } else {
                        handleNavigationOfRepickOriginalItem(substitutionRemovedQty)
                    }
                }
            }
        }
    }

    /**
     * Repick original item: Validating if chat has not initialised item will moved to to-do tab, picker can able to re-pick it.
     * If chat has initialised item will moved to to-do tab, picker will moved to swap substitution flow where they can able to pick original item.
     */
    private suspend fun handleNavigationOfRepickOriginalItem(substitutionRemovedQty: Int? = null) {
        if (currentTab.value != PickListType.Todo) {
            currentTab.postValue(PickListType.Todo)
        }
        when (showChatButton.value.orFalse()) {
            true -> {
                navigateToSubstitution(
                    substitutionRemovedQty = substitutionRemovedQty,
                    path = SubstitutionPath.REPICK_ORIGINAL_ITEM,
                    swapSubstitutionReason = SwapSubstitutionReason.SWAP,
                )
            }

            else -> {
                delay(500)
                openLastSelectedItemDetailBottomSheet()
            }
        }
    }

    fun getLocationCount(itemId: String?): String {
        if (itemId == null) return ""
        val count = pickRepository.getAlternateLocations(itemId)?.size.getOrZero()
        return if (count > 0) "+$count" else ""
    }

    // Open last item clicked bottom sheet to show item detail
    fun openLastSelectedItemDetailBottomSheet() {
        // Show bottom sheet only in listview mode
        if (isListView.value == true) {
            currentItem?.let { onDetailsCtaClicked(it, false) }
        }
    }

    private fun updateSubstitutionCount() {
        viewModelScope.launch {
            var pendingSubCount = 0.0
            var approvedSubCount = 0.0
            var declinedSubCount = 0.0
            var outOfStockCount = 0.0
            pickList.value?.itemActivities?.forEach { item ->
                if (item.isShorted) {
                    outOfStockCount++
                }
                item.pickedUpcCodes?.firstOrNull { it.isSubstitutionOrIssueScanning() }?.let { substitutedList ->
                    when (substitutedList.isRejected) {
                        null -> pendingSubCount++
                        false -> approvedSubCount++
                        true -> declinedSubCount++
                    }
                }
            }
            pendingSubstitutionCount.value = pendingSubCount.toInt().toString()
            approvedSubstitutionCount.value = approvedSubCount.toInt().toString()
            declinedOosSubstitutionCount.value = (declinedSubCount + outOfStockCount).toInt().toString()
            pendingSubCountIntFormat = pendingSubCount.toInt()
        }
    }

    companion object {

        const val BULK_VARIANT_BOTTOM_SHEET = "bulkVariantSelectionTag"
        val RELOAD_LOAD_PICKLIST_DIALOG_TAG = "reloadLoadPicklistDialogTag"
        const val RETRY_UNASSIGN_PICKER_DIALOG_TAG = "retryUnassignPickerDialogTag"
        const val COMMENT_DIALOG_TAG = "commentDialogTag"
        const val RETRY_RECORD_PICK_DIALOG_TAG = "retryRecordPickDialogTag"
        const val SHORT_ITEM_REASON_TAG = "shortItemDialog"
        const val SHORT_ITEM_OOS_WARNING = "shortItemOosWarningDialog"
        const val SYNC_FAILED_DIALOG_TAG = "syncFailedDialog"
        const val PICK_ASSIGNED_TO_DIFFERENT_USER_TAG = "pickAssignedToDifferentUserDialogTag"
        const val QUANTITY_PICKER_PICK_DIALOG_TAG = "quanitityPickerPickDialogTag"
        const val ALTERNATIVE_LOCATION_DIALOG_TAG = "alternativeLocationDialogTag"
        private const val ISSUE_SCANNING_CONFIRMATION_BOTTOM_SHEET_TAG = "issueScanningConfirmationBottomSheetTag"
        private const val CONFIRM_ITEM_SAME_DIALOG_TAG = "confirmSameItemDialogTag"
        private const val SCAN_ISSUE_REPORTED_DIALOG_TAG = "scanIssueReportedDialogTag"
        private const val EBT_WARNING_DIALOG_TAG = "ebtWarningDialogTag"
        private const val INITIAL_EBT_NO_BAGS_WARNING_DIALOG_TAG = "initialEbtWarningDialogTag"
        private const val DELETE_ISSUE_SCANNING_ITEM_DIALOG_TAG = "deleteIssueScanningItemDialog"
        private const val ISSUE_SCAN_ITEM_BOTTOM_SHEET_TAG = "issueScanItemBottomSheetTag"
        private const val EXIT_ISSUE_SCANNING_DIALOG_TAG = "exitIssueScanningDialogTag"
        private const val SHORT_ITEM_ACTION_SHEET_TAG = "shortItemActionSheetTag"
        private const val PRINT_TOTE_LABELS_DIALOG_TAG = "PrintToteLabelsDialogTag"
        private const val BLOCK_STAGING_DIALOG_TAG = "blockStagingDialogTag"
        private const val CUSTOMER_TYPING_DIALOG_TAG = "CustomerTypingDialogTag"
        private val endPickReasonCodeList =
            listOf(EndPickReasonCode.PICKING_ANOTHER_ORDER, EndPickReasonCode.HANDOFF_CUSTOMER, EndPickReasonCode.TOTE_FULL, EndPickReasonCode.OTHER)
    }
}

enum class PickListType(val value: Int) {
    Todo(0),
    Picked(1),
    Short(2),
}
