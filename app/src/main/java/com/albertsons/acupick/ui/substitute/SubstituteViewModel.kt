package com.albertsons.acupick.ui.substitute

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
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
import com.albertsons.acupick.data.logic.getCustomerType
import com.albertsons.acupick.data.logic.shouldShowCustomerType
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.ImageSizePreset
import com.albertsons.acupick.data.model.ItemSearchResult
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.QuantitySelectionType
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.ShortReasonCode
import com.albertsons.acupick.data.model.SubReasonCode
import com.albertsons.acupick.data.model.SubstitutePickRequest
import com.albertsons.acupick.data.model.SubstitutedItem
import com.albertsons.acupick.data.model.SubstitutionCode
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.barcode.PickingContainer
import com.albertsons.acupick.data.model.barcode.asBarcodeType
import com.albertsons.acupick.data.model.barcode.getUpcQty
import com.albertsons.acupick.data.model.isAdvancePickOrPrePick
import com.albertsons.acupick.data.model.request.ShortPickRequestDto
import com.albertsons.acupick.data.model.request.ShortRequestDto
import com.albertsons.acupick.data.model.request.UndoPickLocalDto
import com.albertsons.acupick.data.model.request.UndoPickRequestDto
import com.albertsons.acupick.data.model.response.BulkVariantType
import com.albertsons.acupick.data.model.response.CannotAssignToOrderDialogTypes
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.ItemDetailDto
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.asListOfItemIds
import com.albertsons.acupick.data.model.response.fullContactName
import com.albertsons.acupick.data.model.response.isShorted
import com.albertsons.acupick.data.model.response.netWeight
import com.albertsons.acupick.data.model.response.processedAndExceptionQty
import com.albertsons.acupick.data.model.response.remainingWeight
import com.albertsons.acupick.data.model.response.stageByTime
import com.albertsons.acupick.data.model.response.substitutedQtyMasterView
import com.albertsons.acupick.data.model.textValue
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.picklist.getQuantitySelectionTypeForSubstitution
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.data.toast.Toaster
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.infrastructure.utils.orZero
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.bottomsheetdialog.ActionSheetDetails
import com.albertsons.acupick.ui.bottomsheetdialog.ActionSheetOptions
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetArgDataAndTag
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetType
import com.albertsons.acupick.ui.bottomsheetdialog.CustomBottomSheetArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.manualentry.ManualEntryPickParams
import com.albertsons.acupick.ui.manualentry.ManualEntryType
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.models.OriginalItemParams
import com.albertsons.acupick.ui.models.QuantityParams
import com.albertsons.acupick.ui.picklistitems.FLASH_WARNING_OOS_DIALOG_TAG
import com.albertsons.acupick.ui.picklistitems.FLASH_WARNING_PREP_NOT_READY_DIALOG_TAG
import com.albertsons.acupick.ui.picklistitems.ITEM_DETAIL_BOTTOMSHEET_TAG
import com.albertsons.acupick.ui.picklistitems.OVERRIDE_SUBSTITUTION_DIALOG_TAG
import com.albertsons.acupick.ui.picklistitems.PARTNER_PICK_OUT_OF_STOCK_DIALOG_TAG
import com.albertsons.acupick.ui.picklistitems.PARTNER_PICK_PREP_NOT_READY_DIALOG_TAG
import com.albertsons.acupick.ui.picklistitems.PickListItemsBottomPrompt
import com.albertsons.acupick.ui.picklistitems.PickListType
import com.albertsons.acupick.ui.picklistitems.ScanTarget
import com.albertsons.acupick.ui.picklistitems.TOTE_SCAN_BOTTOMSHEET_TAG
import com.albertsons.acupick.ui.picklistitems.TOTE_UI_COUNT
import com.albertsons.acupick.ui.picklistitems.getFlashWarningOutOfStockArgAndTag
import com.albertsons.acupick.ui.picklistitems.getFlashWarningPrepNotReadyArgAndTag
import com.albertsons.acupick.ui.picklistitems.getItemDetailsArgDataAndTagForBottomSheet
import com.albertsons.acupick.ui.picklistitems.getPartnerPickOutOfStockWarningArgAndTag
import com.albertsons.acupick.ui.picklistitems.getPartnerPickPrepNotReadyWarningArgAndTag
import com.albertsons.acupick.ui.picklistitems.getShortItemConfirmationDialogArgData
import com.albertsons.acupick.ui.picklistitems.getToteScanArgDataAndTagForBottomSheet
import com.albertsons.acupick.ui.util.EventAction
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StateHandler
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.asCustomerComments
import com.albertsons.acupick.ui.util.asItemLocation
import com.albertsons.acupick.ui.util.asSubstitutionInfo
import com.albertsons.acupick.ui.util.asSuggestedItemHeader
import com.albertsons.acupick.ui.util.asUpcOrPlu
import com.albertsons.acupick.ui.util.getFormattedValue
import com.albertsons.acupick.ui.util.getOrEmpty
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.sizedImageUrl
import com.albertsons.acupick.ui.util.transform
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import timber.log.Timber
import java.io.Serializable
import java.time.ZonedDateTime

class SubstituteViewModel(
    stateHandle: SavedStateHandle,
    private val app: Application,
    private val barcodeMapper: BarcodeMapper,
    private val pickRepo: PickRepository,
    private val userRepo: UserRepository,
    private val networkAvailabilityManager: NetworkAvailabilityManager,
    private val dispatcherProvider: DispatcherProvider,
    private val toaster: Toaster,
    private val activityViewModel: MainActivityViewModel,
) : BaseViewModel(app) {

    // DI
    private val siteRepo: SiteRepository by inject()
    private val conversationRepo: ConversationsRepository by inject()
    private val fireBaseAnalytics: FirebaseAnalyticsInterface by inject()

    val iaIdBeforeSubstitution = MutableLiveData<Long>()
    private val itemJustSubstituted = MutableStateFlow(false)
    private val iaIdAfterSwapSubstitution = pickRepo.iaIdAfterSwapSubstitution.asLiveData()

    val swapSubReason = MutableLiveData<SwapSubstitutionReason?>()
    val messageSid = MutableLiveData<String?>()
    val substitutionPath = MutableLiveData<SubstitutionPath>(null)

    private val myPickList = pickRepo.pickList.asLiveData()
    private val masterPickList = pickRepo.masterPickList.asLiveData()

    val iaId = combine(swapSubReason.asFlow(), iaIdBeforeSubstitution.asFlow(), iaIdAfterSwapSubstitution.asFlow(), itemJustSubstituted) { swapSubReason, oldIaId,
        swapSubIaId, itemSubstituted ->
        if (swapSubReason?.isSwapSubstitutionForOtherPicker().orFalse() && itemSubstituted)
            swapSubIaId
        else oldIaId
    }.asLiveData()

    private val pickList = combine(swapSubReason.asFlow(), myPickList.asFlow(), masterPickList.asFlow(), itemJustSubstituted) { swapSubReason, myPicklist, masterPicklist, itemSubstituted ->
        if (swapSubReason?.isSwapSubstitutionForOtherPicker().orFalse() && itemSubstituted.not())
            masterPicklist?.masterView?.first()
        else myPicklist
    }.asLiveData()

    val item = combine(iaId.asFlow(), pickList.asFlow()) { iaId, pickList ->
        pickList?.itemActivities?.find { it.id == iaId }
    }.asLiveData()
    private lateinit var itemActivityDto: ItemActivityDto
    private val substitutedItem: LiveData<ItemDetailDto?> = MutableLiveData()

    val remainingQtyCount = MutableLiveData<Int>()
    val requestedCount = MutableLiveData<Int>()

    val is3p = pickList.map { it?.is3p ?: false }
    val isFFC = pickList.map { it?.isFFC ?: false }

    private var selectedVariant: BulkItem? = null
    val bulkVariants = ArrayList<BulkItem>()
    val isOrderedByWeight = item.map { it?.isOrderedByWeight() }
    val isDisplayType3PW = item.map { siteRepo.isDisplayType3PWEnabled && it?.isDisplayType3PW().orFalse() }
    val requestedWeightAndUnits = item.map { it?.getWeightAndUom() } as MutableLiveData<String?>
    val substitutedCount = item.map { item ->
        item?.pickedUpcCodes?.filter { it.isSubstitution == true }?.sumOf { it.qty ?: 0.0 }?.toInt() ?: 0
    }
    val imageUrl = item.map { it?.sizedImageUrl(ImageSizePreset.ItemDetails) }
    val description = item.map { it?.getItemDescriptionEllipsis() }
    val isOriginalItemCBP = item.map { it?.isCustomerBagPreference }
    val upc = item.map { it?.asUpcOrPlu(app.applicationContext, barcodeMapper) }

    val suggestedItem = item.map {
        pickRepo.getSubstitutionItemDetails(it?.id ?: 0L)?.let { substituionDetails ->
            val itemPickActivity = pickList.value?.itemActivities?.find { pickList -> pickList.itemId == substituionDetails.itemId }
            substituionDetails.copy(
                locationDetail = substituionDetails.locationDetail ?: itemPickActivity?.locationDetail,
                itemAddressDto = substituionDetails.itemAddressDto ?: itemPickActivity?.itemAddressDto,
            )
        }
    }

    val isCustomerChosenItemAvailable = combine(item.asFlow(), suggestedItem.asFlow()) { requested, suggested ->
        requested?.subCode == SubstitutionCode.ONLY_USE_SUGGESTED_SUB && suggested != null
    }.asLiveData()

    val isSuggestedItemVisible = combine(item.asFlow(), suggestedItem.asFlow()) { requested, suggested ->
        requested?.subCode != SubstitutionCode.NOT_ALLOWED && suggested == null
    }.asLiveData()
    val isSystemSuggestedItemAvailable = combine(item.asFlow(), suggestedItem.asFlow()) { requested, suggested ->
        requested?.subCode == SubstitutionCode.USE_SUGGESTED_SUB && suggested != null
    }.asLiveData()
    val suggestedItemImageUrl = suggestedItem.map { it?.sizedImageUrl(ImageSizePreset.ItemDetails) }
    val suggestedItemDescription = suggestedItem.map { it?.getItemDescriptionEllipsis().orEmpty() }
    val suggestedItemUpc = suggestedItem.map { it?.asUpcOrPlu(app.applicationContext, barcodeMapper) }

    val substitutionType: LiveData<SellByType> = combine(item.asFlow(), suggestedItem.asFlow()) { item, suggestedItem ->
        suggestedItem?.sellByWeightInd ?: item?.sellByWeightInd ?: SellByType.RegularItem
    }.asLiveData()

    val customerComments = item.map { it?.asCustomerComments(app.applicationContext) }
    val isCustomerCommented = item.map { it?.instructionDto?.text.isNotNullOrEmpty() }
    val isCustomerNotesTextVisible = combine(isCustomerChosenItemAvailable.asFlow(), customerComments.asFlow()) { isCustomerChosenItemAvailable, customerComment ->
        customerComment.isNotNullOrEmpty() || !isCustomerChosenItemAvailable
    }.asLiveData()
    val customerTypeIcon = item.transform { getCustomerType(it?.isSnap.orFalse(), it?.isSubscription.orFalse()) }
    val isCattEnabled = AcuPickConfig.cattEnabled.asLiveData()
    val showCustomerType = combine(AcuPickConfig.cattEnabled, customerTypeIcon.asFlow()) { cattEnabled, customerType ->
        shouldShowCustomerType(cattEnabled, customerType)
    }.asLiveData()
    val substitutionInfo = combine(item.asFlow(), customerComments.asFlow()) { item, customerComments ->
        if (customerComments.isNullOrEmpty() && item?.subCode == SubstitutionCode.USE_SUGGESTED_SUB) {
            app.getString(R.string.substitute_no_notes)
        } else {
            item?.asSubstitutionInfo(app.applicationContext)
        }
    }.asLiveData()
    val isSubstitutionInfoVisible = combine(
        item.asFlow(), isCustomerChosenItemAvailable.asFlow(), substitutionInfo.asFlow(), customerComments.asFlow()
    ) { item, isCustomerChosenItemAvailable, subInfo, customerComments ->
        !isCustomerChosenItemAvailable && subInfo.isNotNullOrEmpty() && (item?.subCode != SubstitutionCode.USE_SUGGESTED_SUB || customerComments.isNullOrEmpty())
    }.asLiveData()
    val isSubstitutionNotAllowed = item.map {
        it?.subCode == SubstitutionCode.NOT_ALLOWED
    }
    val isSubstitutionAllowed = item.map {
        it?.subAllowed == true || (!siteRepo.areIssueScanningFeaturesEnabled && it?.subCode != SubstitutionCode.NOT_ALLOWED)
    }
    val customerName = item.map { it?.fullContactName() }
    val itemAddress = suggestedItem.map {
        it.asItemLocation(app, false).takeIf { it.isNotEmpty() } ?: suggestedItem.value?.locationDetail.orEmpty()
    }
    val prompt: LiveData<PickListItemsBottomPrompt> = MutableLiveData(PickListItemsBottomPrompt.None)

    // TODO: All the above LiveData need unit tests

    val suggestedItemHeader = item.map { it?.asSuggestedItemHeader(app.applicationContext) }
    val pluOrUpc = MutableLiveData("")
    val quantity: LiveData<Int> = MutableLiveData(0)
    val weight = MutableLiveData("")
    private val unitOfMeasure = MutableLiveData<String?>()
    val isComplete: LiveData<Boolean> = MutableLiveData(false)
    val isFromSwapSubstitution = MutableLiveData<Boolean>(null)
    private val isFirstSubstitute: LiveData<Boolean> = MutableLiveData(true)
    val isEachItem = item.map { it?.sellByWeightInd == SellByType.Each }
    private val activeScanTarget: LiveData<ScanTarget> = MutableLiveData(ScanTarget.Item)
    val isManualEnabled: LiveData<Boolean> = MutableLiveData(true)

    val price = item.map {
        it?.amountDto?.let { amountDto ->
            "$${String.format("%.2f", amountDto.amount)}" + if (item.value?.isOrderedByWeight() == true) item.value?.orderedWeightUOM?.lowercase() ?: " lb" else " ea"
        }
    }

    val isThresholdPriceEnabled = AcuPickConfig.isThresholdPriceEnabledAsFlow()

    val isCompleteEnabled = combine(isComplete.asFlow(), isDisplayingSnackbar.asFlow()) { complete, snack ->
        complete && !snack
    }.asLiveData()

    val entryType = combine(item.map { it?.sellByWeightInd }.asFlow(), suggestedItem.map { it?.sellByWeightInd }.asFlow()) { regularSellBy, suggestedSellBy ->
        when (suggestedSellBy ?: regularSellBy) {
            SellByType.RegularItem, SellByType.Prepped, SellByType.PriceEachUnique, SellByType.PriceEach, SellByType.PriceScaled,
            SellByType.PriceEachTotal, SellByType.PriceWeighted, null,
            -> ManualEntryType.UPC

            SellByType.Weight -> ManualEntryType.Weight
            SellByType.Each -> ManualEntryType.PLU
        }
    }.asLiveData()

    // Manual Entry / Quantity Picker
    private var isFromQuantityPicker = false
    private var isFromManualEntry = false

    // Value must be retained and not cleared as regular quantity is
    private val quantityForDesc: LiveData<Int> = MutableLiveData(1)

    private val subListHolder = mutableListOf<SubstitutionLocalItem>()
    val subListItemUi: LiveData<List<SubstitutionLocalItem>> = MutableLiveData(mutableListOf())

    val isSuggestedItemPicked: LiveData<Boolean> = combine(subListItemUi.asFlow(), suggestedItem.asFlow()) { pickedItems, suggestedItem ->
        pickedItems.any { it.item.itemId == suggestedItem?.itemId || pickRepo.getItemId(it.itemBarcodeType) == suggestedItem?.itemId }
    }.asLiveData()

    fun setIsFromManualEntry(isFromManualEntry: Boolean) {
        this.isFromManualEntry = isFromManualEntry
    }

    private fun isMultiSource(): Boolean {
        return pickList.value?.isMultiSource ?: false
    }

    val isDataLoading: LiveData<Boolean> = MutableLiveData(false)
    private lateinit var shortSelection: ShortReasonCode

    // State
    lateinit var pickListId: String
    private var suggestionNotTakenReason: SubReasonCode? = null
    var canAcceptScan = true

    // substitute quantity for both manual and scanned items
    private var itemPickUpcQty = 1
    var lastItemBarcodeScanned: BarcodeType.Item? = null
    private var lastToteBarcodeScanned: BarcodeType? = null
    private var savedToteBarcodeScanned: BarcodeType? = null
    private var lastItemManuallyEnteredQty: Int? = null
    var lastScannedItem: ItemDetailDto? = null
    private var itemToRemove: SubstitutionLocalItem? = null

    // Events
    val playScanSound: LiveData<Boolean> = LiveEvent()
    val snackBarMessageOnNavigateUp = MutableLiveData<StringIdHelper>()
    val showUnreadMessages = MutableLiveData<Boolean>(false)
    val remainingWeight = MutableLiveData<String>()

    // Dialog callback registration
    init {
        clearToolbarEvent.postValue(Unit)
        changeToolbarTitleEvent.postValue(app.getString(R.string.toolbar_title_substitute))

        viewModelScope.launch {
            /**
             * To show qty indicator
             * In normal substitution and swap substitution for myItem flow it would be OrderedQty minus processedQty
             * In swap substitution flow for other picker of substituted item: it would be OrderedQty minus Originally pickedQty (not substituted qty)
             * In swap substitution flow for other picker of OOS item: it would be exceptionQty only
             */
            item.asFlow().first().let {
                remainingQtyCount.set(
                    when {
                        it?.isShorted.orFalse() -> it?.exceptionQty.orZero().toInt()
                        swapSubReason.value?.isSwapSubstitutionForOtherPicker().orFalse() -> it?.substitutedQtyMasterView.orZero().toInt()
                        else -> (it?.qty?.toInt() ?: 0) - (it?.processedQty?.toInt() ?: 0)
                    }
                )
                // To show ordered quantity of item
                requestedCount.set(it?.qty?.toInt() ?: 0)
                remainingWeight.set(it?.remainingWeight)
            }
        }

        /**
         * Master order view: To validate substitution has mad for other picker's item at the very first time
         */
        viewModelScope.launch(dispatcherProvider.IO) {
            itemJustSubstituted.collect {
                if (swapSubReason.value?.isSwapSubstitutionForOtherPicker().orFalse() && it) {
                    getMasterViewSubstitutedItemDetail()
                }
            }
        }

        // Post the corresponding instruction based on sellByType to scan an item
        viewModelScope.launch {
            substitutionType.asFlow().first().let { sellByType ->
                when (sellByType) {
                    SellByType.Weight -> prompt.postValue(PickListItemsBottomPrompt.Weight(R.string.substitute_instruction_weighted, ::onManualEntryButtonClicked))
                    SellByType.Each -> {
                        val data = PickListItemsBottomPrompt.Eaches(R.string.substitute_instruction_each, ::onManualEntryButtonClicked)
                        prompt.postValue(data)
                    }

                    else -> prompt.postValue(PickListItemsBottomPrompt.Default(R.string.substitute_instruction_upc, ::onManualEntryButtonClicked))
                }
            }
        }

        registerCloseAction(CONTAINER_REASSIGNMENT_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    reAssignContainer(
                        savedItemBarcodeType ?: return@closeActionFactory,
                        savedToteBarcodeType ?: return@closeActionFactory
                    )
                    canAcceptScan = true
                },
                negative = { handleContainerReassignmentRefusal() },
                dismiss = { handleContainerReassignmentRefusal() },
            )
        }

        registerCloseAction(CONCURRENT_SUBSTITUTION_ERROR_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    viewModelScope.launch(dispatcherProvider.IO) {
                        exitFromSwapSubstitutionForOtherPickersItem()
                    }
                },
                dismiss = { exitFromSwapSubstitutionForOtherPickersItem() },
            )
        }

        registerCloseAction(RELOAD_MASTER_VIEW_ITEM_DETAIL_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    viewModelScope.launch(dispatcherProvider.IO) {
                        getMasterViewSubstitutedItemDetail()
                    }
                }
            )
        }

        registerCloseAction(EXIT_SUBSTITUTION_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    val listPair = listOf(Pair(EventKey.ORDER_ID, pickRepo.pickList.value?.customerOrderNumber ?: ""))
                    fireBaseAnalytics.logEvent(EventCategory.SUBSTITUTION, EventAction.CLICK, EventLabel.SUBSTITUTION_CONFIRM_EXIT, listPair)
                    unpickSubs()
                }
            )
        }
        registerCloseAction(EXIT_SWAP_SUBSTITUTION_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    val listPair = listOf(Pair(EventKey.ORDER_ID, pickRepo.pickList.value?.customerOrderNumber ?: ""))
                    fireBaseAnalytics.logEvent(EventCategory.SUBSTITUTION, EventAction.CLICK, EventLabel.SWAP_SUBSTITUTION_CONFIRM_EXIT, listPair)
                    snackBarMessageOnNavigateUp.postValue(StringIdHelper.Id(R.string.swap_substitute_cancelled_message))
                    _navigationEvent.postValue(NavigationEvent.Up)
                },
                negative = { onScanSubstitutionCardClicked() }
            )
        }
        registerCloseAction(SUBSTITUTION_COMPLETE_CONFIRMATION_DIALOG_TAG) {
            closeActionFactory(positive = { _navigationEvent.postValue(NavigationEvent.Up) })
        }
        registerCloseAction(SUGGESTED_ITEM_NOT_CHOSEN_DIALOG_TAG) {
            closeActionFactory(
                positive = { selection ->
                    canAcceptScan = true
                    suggestionNotTakenReason = getSuggestionNotTakenReason((selection as Int))
                    handleOverridenScannedItem()
                },
                negative = {
                    canAcceptScan = true
                    playScanSound.postValue(true)
                    setManualEnabledAndScan()
                },
                dismiss = {
                    canAcceptScan = true
                    playScanSound.postValue(true)
                    setManualEnabledAndScan()
                },
            )
        }
        registerCloseAction(DELETE_SUBSTITUTED_ITEM_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    deleteScannedItem()
                    canAcceptScan = false
                },
                negative = { canAcceptScan = false },
                dismiss = { canAcceptScan = false },
            )
        }
        registerCloseAction(QUANTITY_PICKER_SUBS_DIALOG_TAG) {
            closeActionFactory(
                positive = { result ->
                    canAcceptScan = true
                    handleManualEntryData(result ?: 0, lastItemBarcodeScanned)
                },
                dismiss = {
                    canAcceptScan = true
                    isManualEnabled.postValue(true)
                },
                negative = {
                    canAcceptScan = true
                    isManualEnabled.postValue(true)
                }
            )
        }

        registerCloseAction(BULK_VARIANT_BOTTOM_SHEET) {
            closeActionFactory(
                positive = { result ->
                    canAcceptScan = true
                    selectedVariant = bulkVariants.find { it.itemId == result.toString() }
                    lastItemBarcodeScanned?.let { showQuantityPicker(lastScannedItem, it) }
                },
                dismiss = {
                    canAcceptScan = true
                    selectedVariant = null
                    isManualEnabled.postValue(true)
                },
                negative = {
                    canAcceptScan = true
                    selectedVariant = null
                    isManualEnabled.postValue(true)
                }
            )
        }
        registerCloseAction(ITEM_DETAIL_BOTTOMSHEET_TAG) {
            closeActionFactory(
                negative = {
                    viewModelScope.launch(dispatcherProvider.Main) {
                        delay(500) // Delay for smooth transition of bottomsheet
                        onManualEntryButtonClicked()
                    }
                }
            )
        }
        registerCloseAction(SUBSTITUTE_CONFIRM_BOTTOM_SHEET_TAG) {
            closeActionFactory(
                // Positive action received on substitution completion
                positive = {
                    // Substitution complete back to picklist item screen
                    viewModelScope.launch(dispatcherProvider.Main) {
                        delay(500)
                        snackBarMessageOnNavigateUp.postValue(
                            if (siteRepo.twoWayCommsFlags.chatBeta == true) {
                                StringIdHelper.Id(R.string.substitution_completed_customer_notified)
                            } else {
                                StringIdHelper.Id(R.string.substitution_completed)
                            }
                        )
                        _navigationEvent.postValue(NavigationEvent.Up)
                    }
                },
                // Negative action received to add multiple substitution
                negative = {
                    // Handle add another substitution click open scan an item bottom sheet
                    canAcceptScan = true
                    onScanSubstitutionCardClicked()
                },
                // Dismiss action received on remove all the substituted item
                dismiss = {
                    returnToInitialState()
                },
            )
        }

        registerCloseAction(TOTE_SCAN_BOTTOMSHEET_TAG) {
            closeActionFactory(
                positive = {
                    viewModelScope.launch(dispatcherProvider.Main) {
                        delay(500)
                        activityViewModel.bottomSheetRecordPickArgData.postValue(getSubstituteConfirmationArgData()) // Send the live data event to substitute confirmation bottomsheet
                    }
                },
                dismiss = {
                    returnToInitialState()
                }
            )
        }

        registerCloseAction(SUBSTITTUE_ACTION_SHEET_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    canAcceptScan = false
                    viewModelScope.launch(dispatcherProvider.Main) {
                        delay(500)
                        // TODO ACURED_Redesign Need to optimize action sheet bottom sheet click action handling.
                        when ((substituteOptions[it as Int].settingsString)) {
                            R.string.substitue_different_item -> {
                                suggestedItem.value?.itemId?.let { showNotChosenSubDialog() } ?: run {
                                    canAcceptScan = true
                                    onScanSubstitutionCardClicked() // Do not show reason code dialog when suggested item is not available
                                }
                            }

                            R.string.short_prep_not_ready -> {
                                if (pickList.value?.orderType == OrderType.FLASH) {
                                    inlineDialogEvent.postValue(getFlashWarningPrepNotReadyArgAndTag(item.value?.subAllowed))
                                } else if (pickList.value?.orderType == OrderType.FLASH3P) {
                                    inlineDialogEvent.postValue(getPartnerPickPrepNotReadyWarningArgAndTag())
                                } else {
                                    shortSelection = ShortReasonCode.PREP_NOT_READY
                                    inlineDialogEvent.postValue(
                                        CustomDialogArgDataAndTag(
                                            data = getShortItemConfirmationDialogArgData(R.string.mark_as_not_ready),
                                            tag = SHORT_ITEM_REASON_TAG,
                                        )
                                    )
                                }
                            }

                            R.string.short_tote_full -> {
                                shortSelection = ShortReasonCode.TOTE_FULL
                                inlineDialogEvent.postValue(
                                    CustomDialogArgDataAndTag(
                                        data = getShortItemConfirmationDialogArgData(R.string.mark_as_tote_full),
                                        tag = SHORT_ITEM_REASON_TAG,
                                    )
                                )
                            }

                            R.string.short_out_of_stock -> {
                                if (pickList.value?.orderType == OrderType.FLASH) {
                                    inlineDialogEvent.postValue(getFlashWarningOutOfStockArgAndTag(item.value?.subAllowed))
                                } else if (pickList.value?.orderType == OrderType.FLASH3P) {
                                    inlineDialogEvent.postValue(getPartnerPickOutOfStockWarningArgAndTag())
                                } else {
                                    shortSelection = ShortReasonCode.OUT_OF_STOCK
                                    inlineDialogEvent.postValue(
                                        CustomDialogArgDataAndTag(
                                            data = if (shouldShowCustomerNotifyMessage())
                                                getShortItemConfirmationDialogArgData(
                                                    R.string.do_you_want_to_mark_this_item_as_out_of_stock,
                                                    R.string.short_item_dialog_body_text
                                                ) else getShortItemConfirmationDialogArgData(
                                                R.string.do_you_want_to_mark_this_item_as_out_of_stock,
                                            ),
                                            tag = SHORT_ITEM_REASON_TAG,
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }

        registerCloseAction(FLASH_WARNING_OOS_DIALOG_TAG) {
            closeActionFactory(
                positive = { completeShort(ShortReasonCode.OUT_OF_STOCK) },
                dismiss = { canAcceptScan = true },
                negative = { canAcceptScan = true }
            )
        }

        registerCloseAction(FLASH_WARNING_PREP_NOT_READY_DIALOG_TAG) {
            closeActionFactory(
                positive = { completeShort(ShortReasonCode.PREP_NOT_READY) },
                dismiss = { canAcceptScan = true },
                negative = { canAcceptScan = true }
            )
        }

        registerCloseAction(PARTNER_PICK_PREP_NOT_READY_DIALOG_TAG) {
            closeActionFactory(
                positive = { completeShort(ShortReasonCode.PREP_NOT_READY) },
                dismiss = { canAcceptScan = true },
                negative = { canAcceptScan = true }
            )
        }

        registerCloseAction(PARTNER_PICK_OUT_OF_STOCK_DIALOG_TAG) {
            closeActionFactory(
                positive = { completeShort(ShortReasonCode.OUT_OF_STOCK) },
                dismiss = { canAcceptScan = true },
                negative = { canAcceptScan = true }
            )
        }

        registerCloseAction(SHORT_ITEM_REASON_TAG) {
            closeActionFactory(
                positive = {
                    when (shortSelection) {
                        ShortReasonCode.OUT_OF_STOCK -> completeShort(ShortReasonCode.OUT_OF_STOCK)
                        ShortReasonCode.TOTE_FULL -> completeShort(ShortReasonCode.TOTE_FULL)
                        ShortReasonCode.PREP_NOT_READY -> completeShort(ShortReasonCode.PREP_NOT_READY)
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

        registerCloseAction(SCAN_SUBSTITUTE_ITEM_BOTTOM_SHEET) {
            closeActionFactory(
                negative = {
                    viewModelScope.launch(dispatcherProvider.Main) {
                        delay(500)
                        onManualEntryButtonClicked()
                    }
                },
                dismiss = {
                    // If something is substituted which means we are showing scan item bottom sheet on top of substitution confirmation bottom sheet
                    if (isSomethingSubstituted()) {
                        canAcceptScan = false
                    } else {
                        returnToInitialState()
                    }
                }
            )
        }

        /**
         * Collect the value of substitution path wheather its Normal substitution and Swap substitution flow
         */
        viewModelScope.launch {
            isFromSwapSubstitution.asFlow().collect { swapSubstituted ->
                swapSubstituted.takeIf { it.orFalse() }?.apply { onScanSubstitutionCardClicked() }
            }
        }

        /**
         * Customer approved sub dialog will not be shown in case of swap substitution flow
         */
        viewModelScope.launch(dispatcherProvider.Main) {
            isCustomerChosenItemAvailable.asFlow().first().let { isCustomerChosen ->
                if (isCustomerChosen && isFromSwapSubstitution.value.orFalse().not()) {
                    inlineDialogEvent.postValue(
                        CustomDialogArgDataAndTag(
                            data = getCustomerSubstitutionAlertDialogArgData(suggestedItem.value),
                            tag = CUSTOMER_SUBSTITUTION_ALERT_DIALOG,
                        )
                    )
                }
            }
        }

        /**
         * Do not sub dialog will not be shown in case of swap substitution flow
         */
        viewModelScope.launch(dispatcherProvider.Main) {
            isSubstitutionAllowed.asFlow().first().let { isSubstitutionAllowed ->
                if (!isSubstitutionAllowed && isFromSwapSubstitution.value.orFalse().not()) {
                    handleSubsitutionNotAllowed()
                    // TODO ACURED Redesign we would revisit the changes when working with issue scan item
                    // substituteOptions.removeFirst()
                }
            }
        }
    }

    private val substituteOptions = mutableListOf(
        ActionSheetOptions(R.drawable.ic_swap, R.string.substitue_different_item),
        ActionSheetOptions(R.drawable.ic_prepnotready, R.string.short_prep_not_ready),
        ActionSheetOptions(R.drawable.ic_tote_full, R.string.short_tote_full),
        ActionSheetOptions(R.drawable.ic_outofstock, R.string.short_out_of_stock)
    )

    fun onChatClicked(orderNumber: String) {
        viewModelScope.launch {
            pickRepo.pickList.value?.let { picklist ->
                picklist.orderChatDetails?.firstOrNull { orderChatDetail ->
                    orderNumber == orderChatDetail.customerOrderNumber
                }?.let {
                    _navigationEvent.postValue(
                        NavigationEvent.Directions(
                            SubstituteFragmentDirections.actionSubstituteFragmentToChatFragment(
                                orderNumber = orderNumber,
                                convetsationId = it.conversationSid.orEmpty(),
                                fulfullmentOrderNumber = it.referenceEntityId.orEmpty()
                            )
                        )
                    )
                }
            }
        }
    }

    private fun returnToInitialState() {
        canAcceptScan = true
        isFromQuantityPicker = false
        activeScanTarget.set(ScanTarget.Item)
        suggestionNotTakenReason = null
        isManualEnabled.postValue(true)
        lastItemBarcodeScanned = null
        lastScannedItem = null
        if (subListHolder.isNotEmpty()) {
            subListHolder.removeLast()
            subListItemUi.postValue(subListHolder)
        }
        selectedVariant = null
        val shouldReenableCompleteButton = (subListItemUi.value?.size ?: 0) > 0
        isComplete.postValue(shouldReenableCompleteButton)
        setIsFromManualEntry(false)
    }

    private fun handleOverridenScannedItem() {
        if (isFromQuantityPicker) {
            if (lastItemBarcodeScanned != null && lastItemManuallyEnteredQty != null) {
                addItemFromManualEntryOrQuantityPicker(lastItemBarcodeScanned!!, lastItemManuallyEnteredQty!!)
            }
        } else {
            // Validating substitute override dialog shown if item not scanned first
            lastScannedItem?.let {
                viewModelScope.launch(dispatcherProvider.Main) {
                    addScannedItem()
                }
            } ?: run {
                onScanSubstitutionCardClicked()
            }
        }
    }

    private fun getSuggestionNotTakenReason(selection: Int?): SubReasonCode? {
        val isCustomerChosen = suggestedItemHeader.value == app.applicationContext.getString(R.string.substitute_suggested_header_customer_chosen)
        return if (selection == null) {
            null
        } else {
            if (isCustomerChosen) {
                suggestedChosenItemNotChosenList[selection]
            } else suggestedItemNotChosenList[selection]
        }
    }

    /** If the item scanned is not in the pick list, try to fetch info about item */
    private suspend fun fetchItemInfo(barcodeType: BarcodeType.Item): ItemDetailDto? =
        if (!networkAvailabilityManager.isConnected.first()) {
            ItemDetailDto.unknownItem
        } else {
            val siteId = userRepo.user.value!!.selectedStoreId.orEmpty()
            val activityId = if (is3p.value.orFalse()) pickList.value?.actId else null
            val itemId = if (is3p.value.orFalse()) item.value?.itemId else null
            val result = isDataLoading.wrap {
                when (barcodeType) {
                    is BarcodeType.Item.Each -> pickRepo.getItemDetails(
                        siteId = siteId,
                        upcId = barcodeType.catalogLookupUpc,
                        pluCode = barcodeType.plu,
                        actId = activityId,
                        originalItemId = itemId,
                        sellByWeightInd = SellByType.Each.code
                    )

                    is BarcodeType.Item.Weighted -> pickRepo.getItemDetails(
                        siteId = siteId,
                        upcId = barcodeType.catalogLookupUpc,
                        pluCode = barcodeType.plu,
                        actId = activityId,
                        originalItemId = itemId,
                        sellByWeightInd = SellByType.Weight.code
                    )

                    is BarcodeType.Item.Priced -> pickRepo.getItemDetails(
                        siteId = siteId,
                        upcId = barcodeType.catalogLookupUpc,
                        actId = activityId,
                        originalItemId = itemId,
                        sellByWeightInd = SellByType.Prepped.code
                    )

                    else -> pickRepo.getItemDetails(
                        siteId = siteId,
                        actId = activityId,
                        originalItemId = itemId,
                        upcId = barcodeType.catalogLookupUpc
                    )
                }
            }

            when (result) {
                is ApiResult.Success -> result.data
                is ApiResult.Failure -> {
                    isManualEnabled.postValue(true)
                    if (result is ApiResult.Failure.Server) {
                        when (result.error?.errorCode?.resolvedType) {
                            ServerErrorCode.FFC_RESTRICTION -> {
                                showScanItemRestrictionFailure(R.string.substitute_scanned_ffc_error)
                                null
                            }

                            ServerErrorCode.BPN_RESTRICTION -> {
                                showScanItemRestrictionFailure(R.string.substitute_scanned_bpn_error)
                                null
                            }

                            else -> {
                                ItemDetailDto.unknownItem
                            }
                        }
                    } else {
                        ItemDetailDto.unknownItem
                    }
                }

                else -> {
                    ItemDetailDto.unknownItem
                }
            }
        }

    private suspend fun checkEntryType(itemBarcodeType: BarcodeType.Item, toteBarcodeType: PickingContainer, disableContainerValidation: Boolean = false) {
        if (isFromQuantityPicker) {
            recordSubstitution(itemBarcodeType, toteBarcodeType, disableContainerValidation, isFromQuantityPicker = true)
        } else {
            recordSubstitution(itemBarcodeType, toteBarcodeType, disableContainerValidation, isFromQuantityPicker = false)
        }
    }

    /** When the item & tote are scanned, send successful substitution to backend */
    private suspend fun recordSubstitution(itemBarcodeType: BarcodeType.Item, toteBarcodeType: PickingContainer, disableContainerValidation: Boolean = false, isFromQuantityPicker: Boolean) {
        activeScanTarget.postValue(ScanTarget.None) // we will not accept any scans at this point
        val substituteItem = SubstitutedItem(
            itemId = if (selectedVariant != null) {
                selectedVariant.let { it?.itemId }
            } else substitutedItem.value?.itemId,
            description = if (selectedVariant != null) {
                selectedVariant.let { it?.itemDes }
            } else substitutedItem.value?.itemDesc,
            modifiedUpc = itemBarcodeType.getBarcodeToSendToBackend(),
            storageType = substitutedItem.value?.storageType,
        )
        val fulfilledQuantity =
            if (isFromQuantityPicker) {
                quantity.value?.toDouble()?.coerceAtLeast(1.0)
            } else {
                itemPickUpcQty.toDouble()
            } ?: 1.0

        val upcQuantity = itemBarcodeType.getUpcQty(fulfilledQuantity, fixedItemTypeEnabled = siteRepo.fixedItemTypesEnabled)

        val subItemId = substituteItem.itemId
        val originalItemId = item.value!!.itemId
        val result = isBlockingUi.wrap {
            pickRepo.recordSubstitution(
                request = SubstitutePickRequest(
                    itemBarcodeType = itemBarcodeType,
                    toteBarcodeType = toteBarcodeType,
                    fulfilledQuantity = fulfilledQuantity,
                    upcQuantity = upcQuantity,
                    originalItem = item.value!!,
                    sellByWeightInd = if (subItemId == originalItemId) { item.value?.sellByWeightInd } else null, // ACIP-187230: Will send SBWI if same item sub is true
                    regulated = substitutedItem.value?.isRegulated,
                    substituteItem = substituteItem,
                    userId = userRepo.user.filterNotNull().first().userId,
                    disableContainerValidation = disableContainerValidation,
                    isSmartSubItem = suggestedItem.value?.itemId != null &&
                        suggestedItem.value?.itemId == if (bulkVariants.isEmpty()) pickRepo.getItemId(itemBarcodeType) else selectedVariant?.itemId,
                    subReasonCode = suggestionNotTakenReason,
                    sameItemSubbed = if (bulkVariants.isNullOrEmpty()) subItemId == originalItemId else false,
                    substitutionReason = swapSubReason.value?.getSubstituteRejectionReasonValue(),
                    exceptionDetailsId = substitutedItem.value?.exceptionDetailsId,
                    scannedPrice = if (itemBarcodeType is BarcodeType.Item.Priced) (itemBarcodeType as? BarcodeType.Item.Priced)?.price else null,
                    messageSid = if (swapSubReason.value?.isSwapSubstitutionForOtherPicker() == true) messageSid.value else null,
                    isManuallyEntered = isFromManualEntry
                )
            )
        }

        when (result) {
            is ApiResult.Success -> {
                itemJustSubstituted.value = true
                delay(500) // To get new iaId in swap substitution flow
                openSubstitueConfirmationBottomSheet()
                showScanToteSuccess(toteBarcodeType.asBarcodeType())
                // Reset
                setIsFromManualEntry(false)
                itemPickUpcQty = 1
                this.quantity.postValue(0)
                clearSubItemView()
            }

            is ApiResult.Failure -> {
                playScanSound.postValue(false)
                activeScanTarget.postValue(ScanTarget.Tote)
                withContext(dispatcherProvider.Main) {
                    if (result is ApiResult.Failure.Server) {
                        when (val type = result.error?.errorCode?.resolvedType) {
                            ServerErrorCode.CONCURRENT_SWAP_SUBSTITUTION -> showConcurrentSubstitutionErrorDialog()
                            ServerErrorCode.CONTAINER_ATTACHED_TO_ENTITY_ID -> showContainerReassignmentDialog(itemBarcodeType, toteBarcodeType)
                            ServerErrorCode.CONTAINER_ADDED_DIFF_PICKLIST_SAME_ORDER -> showContainerCannotReassignDialog(itemBarcodeType, toteBarcodeType)
                            ServerErrorCode.STORAGE_TYPE_IS_DIFFERENT -> showWrongTote()
                            else -> {
                                if (type?.cannotAssignToOrder() == true) {
                                    val serverErrorType =
                                        if (type == ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY) CannotAssignToOrderDialogTypes.PICKLIST else CannotAssignToOrderDialogTypes.REGULAR
                                    serverErrorCannotAssignUser(serverErrorType, pickRepo.pickList.value?.erId == null)
                                } else {
                                    handleApiError(result)
                                }
                            }
                        }
                    } else {
                        handleApiError(result)
                    }
                }
            }
        }
    }

    /**
     * Master order view: After making swap substitution of other picker's item Acupick app loads the required data such as
     * upc's, substitution detail and alternate location of that ordered item.
     */
    private fun getMasterViewSubstitutedItemDetail() {
        viewModelScope.launch(dispatcherProvider.IO) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> {
                    val upcRequest = async { loadUpcCodes() }
                    val substitutionItemRequest = async { loadSubstitutionItemDetails() }
                    val alternateLocationRequest = async { loadAlternateLocations() }
                    try {
                        awaitAll(upcRequest, substitutionItemRequest, alternateLocationRequest)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        delay(500) // To show error dialog on top of bottomsheet
                        showMasterViewItemDetailApiErrorDialog()
                    }
                }

                false -> {
                    networkAvailabilityManager.triggerOfflineError { getMasterViewSubstitutedItemDetail() }
                }
            }
        }
    }

    private suspend fun loadSubstitutionItemDetails() {
        val result = pickRepo.getSubstitutionItemDetailList(actId = pickListId)
        when (result) {
            is ApiResult.Success -> {
                // Nothing to do here.  PickRepository holds the response data.
            }

            is ApiResult.Failure -> {
                delay(500) // To show error dialog on top of bottomsheet
                handleApiError(result, retryAction = { getMasterViewSubstitutedItemDetail() })
            }
        }.exhaustive
    }

    private suspend fun loadAlternateLocations() {
        val itemIds = pickList.value?.asListOfItemIds() ?: listOf()
        if (pickList.value?.itemActivities != null && itemIds.isNotNullOrEmpty()) {
            val result = pickRepo.getAllItemLocations(
                siteId = pickList.value?.siteId ?: "",
                itemId = itemIds
            )
            when (result) {
                is ApiResult.Success -> {
                    // Nothing to do here.  PickRepository holds the response data.
                }

                is ApiResult.Failure -> {
                    delay(500) // To show error dialog on top of bottomsheet
                    handleApiError(result, retryAction = { getMasterViewSubstitutedItemDetail() })
                }
            }.exhaustive
        }
    }

    private suspend fun loadUpcCodes() {
        pickList.value?.itemActivities?.let { itemActivities ->
            val siteId = userRepo.user.value.also { siteId ->
                if (siteId == null) acuPickLogger.w("[loadUpcCodes] user is null - unable to retrieve siteId")
            }?.selectedStoreId.orEmpty()
            val itemIds = itemActivities.map { it.itemId.orEmpty() }.distinct()
            acuPickLogger.v("[loadUpcCodes] siteId=$siteId, itemIds=[${itemIds.joinToString(separator = ", ")}]")
            val result = pickRepo.getItemUpcList(siteId, itemIds, itemActivities)
            when (result) {
                is ApiResult.Success -> {
                    // Nothing to do here.  PickRepository holds the response data.
                }

                is ApiResult.Failure -> {
                    delay(500) // To show error dialog on top of bottomsheet
                    handleApiError(result, retryAction = { getMasterViewSubstitutedItemDetail() })
                }
            }.exhaustive
        }
    }
    private fun reAssignContainer(savedReassignmentBarcodeTemp: BarcodeType.Item, savedReassignmentToteTemp: PickingContainer) {
        viewModelScope.launch(dispatcherProvider.IO) {
            // Call recordSubstitution again, this time with disableContainerValidation true to bypass container/tote issues
            checkEntryType(savedReassignmentBarcodeTemp, savedReassignmentToteTemp, disableContainerValidation = true)
        }
    }

    private fun handleContainerReassignmentRefusal() {
        activeScanTarget.postValue(ScanTarget.Tote)
        lastToteBarcodeScanned = savedToteBarcodeScanned
        canAcceptScan = true
    }

    /** Entry point for a barcode received from the scanner/DataWedge */
    fun onScannerBarcodeReceived(barcodeType: BarcodeType) {
        if (canAcceptScan) {
            viewModelScope.launch(dispatcherProvider.Default) {
                when (activeScanTarget.value) {
                    ScanTarget.Item -> handleScannedItem(barcodeType)
                    ScanTarget.Tote -> handleScannedTote(barcodeType)
                    else -> Unit // cannot handle a scan right now
                }.exhaustive
            }
        }
    }

    private fun clearSubItemView() {
        substitutedItem.postValue(null)
        suggestionNotTakenReason = null
        pluOrUpc.postValue("")
        quantityForDesc.postValue(1)
        isFromQuantityPicker = false
        selectedVariant = null
    }

    // used to determine same item sub flow for bulk items
    var isFromCache = true
    private suspend fun handleScannedItem(barcodeType: BarcodeType) {
        acuPickLogger.v("[handleScannedItem] item barcode scanned ${barcodeType.rawBarcode}")

        if (barcodeType is BarcodeType.Item) {
            isManualEnabled.postValue(false)
            pluOrUpc.postValue(
                when (barcodeType) {
                    is BarcodeType.Item.Short -> barcodeType.upcA
                    is BarcodeType.Item.Each -> barcodeType.plu
                    is BarcodeType.Item.Weighted -> barcodeType.plu
                    is BarcodeType.Item.Priced -> barcodeType.plu
                    else -> barcodeType.rawBarcode
                }
            )

            weight.postValue((barcodeType as? BarcodeType.Item.Weighted)?.weight.toString())

            // Either original barcode or Eaches adjusted barcode (possibly added itemActivityDbId)
            var adjustedBarcodeType: BarcodeType.Item = barcodeType

            // We use getItemWithoutOrderOrCustomerDetails as we only need item specific information (don't need the specific customer order or related info here)
            val fetchedItemDetailDto = when (val scannedSubItem = pickRepo.getItemWithoutOrderOrCustomerDetails(barcodeType)) {
                is ItemSearchResult.MatchedItem -> {
                    isFromCache = true
                    unitOfMeasure.postValue(scannedSubItem.itemActivityDto.orderedWeightUOM?.uppercase())
                    ItemDetailDto(scannedSubItem.itemActivityDto)
                }

                is ItemSearchResult.Error -> {
                    isFromCache = false
                    suggestedItem.value?.let {
                        if (it.itemId == pickRepo.getItemId(barcodeType)) ItemDetailDto(it) else null
                    } ?: run {
                        // lookup item info as the item is not present in the pick list
                        acuPickLogger.w("[handleScannedItem] item barcode scanned is not in this pick list")
                        fetchItemInfo(barcodeType)
                    }
                }
            }

            val processedQty = item.value?.processedQty?.toInt() ?: 0
            if ((requestedCount.value ?: 0) <= processedQty && fetchedItemDetailDto?.itemId == item.value?.itemId) {
                showSubstituteFulfilledFailure()
                isManualEnabled.postValue(true)
                return
            }

            // Need to add the itemActivityDbId to the Eaches barcode IF the item is known within the picklist
            if (barcodeType is BarcodeType.Item.Each) {
                // Find matching item for the given customer order number
                val matchingPickListItem = pickRepo.getItem(fetchedItemDetailDto?.itemId, item.value?.customerOrderNumber)
                acuPickLogger.v("[handleScannedItem] fetchedItem=$fetchedItemDetailDto, matchingPickListItem=$matchingPickListItem")
                adjustedBarcodeType = if (matchingPickListItem?.id == iaId.value) {
                    // Add the known itemActivityDbId so that all code handling the barcodeType from this point is aware of the item being present in the pick list
                    barcodeType.copy(itemActivityDbId = this@SubstituteViewModel.item.value?.id).also {
                        acuPickLogger.d("[handleScannedItem] Each barcode item matches the original item - adding itemActivityDbId to Each barcode - $it")
                    }
                } else {
                    acuPickLogger.d("[handleScannedItem] Unable to find matching item for given each barcode in pick list - leaving each barcode alone - $barcodeType")
                    // If unable to find item, leave the barcode alone (likely indicates substitution for an item not found in the original pick list)
                    barcodeType
                }
            }

            isFromQuantityPicker = false
            lastItemBarcodeScanned = barcodeType
            lastScannedItem = fetchedItemDetailDto

            fun shouldShowbottomSheet(): Boolean {
                val result = when (item.value?.sellByWeightInd) {
                    SellByType.RegularItem, SellByType.PriceEach -> item.value?.bulkVariantType == BulkVariantType.UPC
                    SellByType.Each -> true
                    else -> false
                }
                return if (isFromCache) result else true
            }

            val showBulk = (
                fetchedItemDetailDto?.bulkVariantMap?.getOrDefault(barcodeType.catalogLookupUpc, null) != null ||
                    fetchedItemDetailDto?.bulkVariantList.isNotNullOrEmpty()
                ) && shouldShowbottomSheet()
            acuPickLogger.d("bulk catalog ${barcodeType.catalogLookupUpc} -${fetchedItemDetailDto?.bulkVariantMap}  ${shouldShowbottomSheet()} ")
            // clearing the bulkvariants list and any selected bulk variant for every item scan
            bulkVariants.clear()
            selectedVariant = null
            if (showBulk) {
                val bulkItems = if (fetchedItemDetailDto?.bulkVariantList.isNotNullOrEmpty()) {
                    fetchedItemDetailDto?.bulkVariantList?.map {
                        BulkItem(
                            itemDes = it?.itemDesc,
                            imageUrl = it?.imageURL,
                            itemId = it?.itemId,
                            isSystemSuggested = false,
                            customerChosen = false,
                        )
                    }?.toMutableList()?.apply {
                        if (isFromCache && isSystemSuggestedItemAvailable.value.orFalse()) {
                            add(
                                0,
                                BulkItem(
                                    itemDes = suggestedItemDescription.value,
                                    imageUrl = suggestedItemImageUrl.value,
                                    itemId = suggestedItem.value?.itemId,
                                    isSystemSuggested = true,
                                    customerChosen = false
                                )
                            )
                        } else if (isFromCache && isCustomerChosenItemAvailable.value.orFalse()) {
                            add(
                                0,
                                BulkItem(
                                    itemDes = suggestedItemDescription.value,
                                    imageUrl = suggestedItemImageUrl.value,
                                    itemId = suggestedItem.value?.itemId,
                                    isSystemSuggested = false,
                                    customerChosen = true
                                )
                            )
                        } // when we add customer choice to the list it was already there in the bulk variant list, so removing duplicates if any
                    }?.distinctBy { it.itemId } ?: emptyList()
                } else {
                    fetchedItemDetailDto?.bulkVariantMap?.getOrDefault(barcodeType.catalogLookupUpc, null)?.map {
                        BulkItem(
                            itemDes = it?.itemDesc,
                            imageUrl = it?.imageURL,
                            itemId = it?.itemId,
                            isSystemSuggested = false,
                            customerChosen = false
                        )
                    }?.toMutableList()?.apply {
                        if (isFromCache && isSystemSuggestedItemAvailable.value.orFalse()) {
                            add(
                                0,
                                BulkItem(
                                    itemDes = suggestedItemDescription.value,
                                    imageUrl = suggestedItemImageUrl.value,
                                    itemId = suggestedItem.value?.itemId,
                                    isSystemSuggested = true,
                                    customerChosen = false
                                )
                            )
                        } else if (isFromCache && isCustomerChosenItemAvailable.value.orFalse()) {
                            add(
                                0,
                                BulkItem(
                                    itemDes = suggestedItemDescription.value,
                                    imageUrl = suggestedItemImageUrl.value,
                                    itemId = suggestedItem.value?.itemId,
                                    isSystemSuggested = false,
                                    customerChosen = true
                                )
                            )
                        }
                    }?.distinctBy { it.itemId } ?: emptyList()
                }
                bulkVariants.addAll(bulkItems)
                bulkItems.let { launchBulkVariantSelection(it) }
            } else {
                fetchedItemDetailDto?.let { handleQuantitySelectionTypeForScan(adjustedBarcodeType, fetchedItemDetailDto) }
            }
        } else {
            pluOrUpc.postValue("")
            showScanItemFailure()
        }
    }

    private suspend fun handleQuantitySelectionTypeForScan(barcodeType: BarcodeType.Item, itemDetailDto: ItemDetailDto) {
        val orderedByWeight = when (lastScannedItem?.itemId) {
            suggestedItem.value?.itemId -> suggestedItem.value?.isOrderedByWeightFromSubstitution() == true
            else -> itemDetailDto.isOrderedByWeight()
        }

        val quantitySelectionType = getQuantitySelectionTypeForSubstitution(barcodeType, remainingQtyCount.value ?: 0, orderedByWeight)
        when {
            quantitySelectionType == QuantitySelectionType.QuantityPicker -> showQuantityPicker(itemDetailDto, barcodeType)
            /**
             * Reason dialog will not be shown in case of swap substitution flow
             */
            suggestedItem.value?.itemId != null &&
                suggestedItem.value?.itemId != itemDetailDto.itemId &&
                suggestedItem.value?.itemId != pickRepo.getItemId(barcodeType) &&
                isFromSwapSubstitution.value.orFalse().not() ->
                // ACURED_REDESIGN Checking if override dialog already selected
                suggestionNotTakenReason?.let {
                    withContext(dispatcherProvider.Main) {
                        handleOverridenScannedItem()
                    }
                } ?: run {
                    showNotChosenSubDialog()
                }

            else -> {
                substitutedItem.postValue(itemDetailDto)
                // Substitute Item collector
                addSubbedItemToUiList(itemDetailDto)
                playScanSound.postValue(true)
                isComplete.postValue(false)
                showScanItemSuccess(barcodeType)
            }
        }
    }

    private suspend fun addScannedItem() {
        isComplete.postValue(false)
        substitutedItem.postValue(lastScannedItem)

        // Substitute Item collector
        addSubbedItemToUiList(lastScannedItem)

        playScanSound.postValue(true)
        showScanItemSuccess(lastItemBarcodeScanned!!)
        lastScannedItem = null
        canAcceptScan = true
    }

    private fun addSubbedItemToUiList(subsitutedItemIn: ItemDetailDto?) {
        // Quantity collector
        quantity.postValue(quantityForDesc.value)

        subsitutedItemIn?.apply {
            subListHolder.add(
                SubstitutionLocalItem(
                    item = this,
                    selectedVariant = selectedVariant,
                    itemBarcodeType = lastItemBarcodeScanned,
                    toteBarcodeType = null,
                    quantity = quantityForDesc.value?.toDouble() ?: 1.0,
                    itemWeight = weight.value ?: "",
                    unitOfMeasure = unitOfMeasure.value,
                    orderedByWeight = isOrderedByWeight.value ?: false,
                    isCustomerChosenItemAvailable = isCustomerChosenItemAvailable.value == true && (
                        suggestedItem.value?.itemId == subsitutedItemIn.itemId || suggestedItem.value?.itemId == selectedVariant?.itemId
                        ),
                    isDisplayType3Pw = siteRepo.isDisplayType3PWEnabled && subsitutedItemIn.isDisplayType3PW(),
                    orderedWeightWithUom = ""
                )
            )
        }
        subListItemUi.postValue(subListHolder.reversed())
    }

    private fun addItemFromManualEntryOrQuantityPicker(barcodeType: BarcodeType.Item, quantity: Int) {
        isManualEnabled.set(false)
        quantityForDesc.set(quantity)
        val itemDetails = lastScannedItem?.copy()
        substitutedItem.set(itemDetails)

        // Add SubbedItemToUiList
        addSubbedItemToUiList(itemDetails)
        viewModelScope.launch {
            pluOrUpc.postValue(barcodeType.rawBarcode)
            isComplete.postValue(false)
            delay(1000)
            playScanSound.postValue(true)
            showScanItemSuccess(barcodeType)
        }
        canAcceptScan = true
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

    private fun showQuantityPicker(subItem: ItemDetailDto?, barcode: BarcodeType.Item) {
        lastItemBarcodeScanned = barcode
        canAcceptScan = false
        viewModelScope.launch(dispatcherProvider.IO) {
            inlineBottomSheetEvent.postValue(
                BottomSheetArgDataAndTag(
                    data = CustomBottomSheetArgData(
                        dialogType = BottomSheetType.QuantityPicker,
                        title = StringIdHelper.Raw(""),
                        peekHeight = R.dimen.expanded_bottomsheet_peek_height,
                        customDataParcel = QuantityParams(
                            barcodeFormatted = barcode.getFormattedValue(context = app, hideUnits = true),
                            isPriced = false,
                            isWeighted = barcode is BarcodeType.Item.Weighted,
                            isEaches = barcode is BarcodeType.Item.Each,
                            isTotaled = false,
                            itemId = if (selectedVariant != null) selectedVariant?.itemId else subItem?.itemId,
                            description = if (selectedVariant != null) selectedVariant?.itemDes else subItem?.itemDesc,
                            image = if (selectedVariant != null) selectedVariant?.imageUrl else subItem?.imageUrl,
                            weightEntry = weight.value,
                            requested = requestedCount.value ?: 0,
                            entered = if (item.value?.itemId == subItem?.itemId) item.value?.processedQty?.toInt() else 0,
                            isSubstitution = true,
                            isIssueScanning = false,
                            storageType = subItem?.storageType,
                            isRegulated = subItem?.isRegulated,
                            isSameItem = item.value?.itemId == subItem?.itemId,
                            shouldShowOriginalItemInfo = bulkVariants.isNotNullOrEmpty(),
                            originalItemParams = if (bulkVariants.isNotNullOrEmpty()) OriginalItemParams(
                                itemDesc = item.value?.itemDescription,
                                itemId = item.value?.itemId,
                                itemImage = item?.value?.imageUrl,
                                orderedQty = item.value?.qty?.toInt()
                            ) else null,
                            isCustomerBagPreference = isOriginalItemCBP.value
                        ),
                    ),
                    tag = QUANTITY_PICKER_SUBS_DIALOG_TAG
                )
            )
        }
    }

    private suspend fun handleScannedTote(barcodeType: BarcodeType) {
        val lastItemBarcodeScannedTemp = lastItemBarcodeScanned // hold onto immutable value for proper null usage below
        when {
            barcodeType !is PickingContainer || lastItemBarcodeScannedTemp == null -> {
                // Tote/item mismatch - tell the user
                acuPickLogger.w("[handleScannedTote] invalid tote scanned")
                showWrongTote(R.string.invalid_tote_scanned)
            }

            !pickRepo.isItemIntoPickingContainerValid(
                item = item.value!!.copy(storageType = getExpectedStorageType()), toteBarcodeType = barcodeType,
                isMultiSource()
            ) -> {
                // wrong type of tote
                acuPickLogger.w("[handleScannedTote] wrong tote scanned")
                showWrongTote()
            }

            else -> {
                savedToteBarcodeScanned = lastToteBarcodeScanned // save it in case tote reassignment is needed, and the user decides not to reassign
                lastToteBarcodeScanned = barcodeType
                subListHolder.lastOrNull()?.toteBarcodeType = barcodeType
                subListHolder.lastOrNull()?.quantity = quantityForDesc.value?.toDouble() ?: 1.0
                checkEntryType(lastItemBarcodeScannedTemp, toteBarcodeType = barcodeType)
            }
        }
    }

    // Try to figure out what kind of storage type we should be using. If we can't get the type of the substituted item use the type of the original item
    private fun getExpectedStorageType() = substitutedItem.value?.storageType ?: suggestedItem.value?.storageType ?: item.value?.storageType

    private suspend fun showScanItemSuccess(itemBarcodeType: BarcodeType.Item) {
        withContext(dispatcherProvider.Main) {
            // TODO: ACURED_REDESIGN Remove commented code after complete testing
            /*showSnackBar(
                SnackBarEvent(
                    isSuccess = true,
                    prompt = when (itemBarcodeType) {
                        is BarcodeType.Item.Short -> StringIdHelper.Format(R.string.item_scanned_upc_format, itemBarcodeType.upcA)
                        is BarcodeType.Item.Each -> StringIdHelper.Format(R.string.item_scanned_plu_format, itemBarcodeType.plu)
                        is BarcodeType.Item.Weighted -> StringIdHelper.Format(R.string.item_scanned_plu_format, itemBarcodeType.plu)
                        is BarcodeType.Item.Priced -> StringIdHelper.Format(R.string.item_scanned_plu_format, itemBarcodeType.plu)
                        else -> StringIdHelper.Format(R.string.item_scanned_upc_format, itemBarcodeType.rawBarcode)
                    },
                    onDismissEventCallback = {
                        viewModelScope.launch(dispatcherProvider.Main) {
                            showTotePrompt(lastItemBarcodeScanned)
                        }
                    }
                )
            )*/
            showTotePrompt(lastItemBarcodeScanned)
            activeScanTarget.postValue(ScanTarget.Tote)
        }
    }

    private suspend fun showScanToteSuccess(itemBarcodeType: BarcodeType?) {
        itemBarcodeType?.let {
            canAcceptScan = false
            withContext(dispatcherProvider.Main) {
                clearSnackBarEvents()
                playScanSound.postValue(true)
                delay(500)
                showSnackBar(
                    AcupickSnackEvent(
                        message = StringIdHelper.Format(R.string.tote_scanned_format_redesign, itemBarcodeType.rawBarcode.takeLast(TOTE_UI_COUNT)),
                        type = SnackType.SUCCESS
                    )
                )
                // TODO: ACURED_REDESIGN Remove commented code after complete testing
                /*showSnackBar(
                    SnackBarEvent(
                        prompt = StringIdHelper.Format(R.string.tote_scanned_format, itemBarcodeType.rawBarcode.takeLast(TOTE_UI_COUNT)),
                        onDismissEventCallback = {
                            setManualEnabledAndScan()
                        },
                        isSuccess = true
                    )
                )*/
                // setManualEnabledAndScan() // ACURED_REDESIGN to disable scan on substitute confirmation bottom sheet
                activeScanTarget.set(ScanTarget.Item)
            }
        }
    }

    private fun setManualEnabledAndScan() {
        lastScannedItem = null // ACURED_REDESIGN to clear the last scanned item if reason dialog dismissed after scanning an item
        selectedVariant = null
        isManualEnabled.set(true)
        canAcceptScan = true
        isFirstSubstitute.postValue(false)
        isComplete.postValue(true)
    }

    private suspend fun showScanItemFailure() {
        playScanSound.postValue(false)
        withContext(dispatcherProvider.Main) {
            showSnackBar(
                AcupickSnackEvent(
                    message = StringIdHelper.Id(R.string.wrong_substitution_scanned),
                    type = SnackType.ERROR
                )
            )
        }
    }

    private suspend fun showScanItemRestrictionFailure(@StringRes id: Int) {
        playScanSound.postValue(false)
        withContext(dispatcherProvider.Main) {
            showAnchoredSnackBar(
                AcupickSnackEvent(
                    message = StringIdHelper.Id(id),
                    type = SnackType.ERROR
                )
            )
        }
    }

    private suspend fun showSubstituteFulfilledFailure() {
        playScanSound.postValue(false)
        withContext(dispatcherProvider.Main) {
            showSnackBar(AcupickSnackEvent(message = StringIdHelper.Id(R.string.requested_already_fulfilled), type = SnackType.ERROR))
        }
    }

    private suspend fun showWrongTote(@StringRes customWrongToteStringId: Int? = null) {
        playScanSound.postValue(false)

        withContext(dispatcherProvider.Main) {
            clearSnackBarEvents()
            showSnackBar(
                AcupickSnackEvent(
                    message = StringIdHelper.Id(customWrongToteStringId ?: R.string.wrong_tote_scanned),
                    type = SnackType.ERROR
                )
            )
            // TODO: ACURED_REDESIGN Remove commented code after complete testing
            /*showSnackBar(
                SnackBarEvent(
                    prompt = StringIdHelper.Id(customWrongToteStringId ?: R.string.wrong_tote_scanned),
                    isError = true,
                    onDismissEventCallback = {
                        viewModelScope.launch(dispatcherProvider.Main) {
                            showTotePrompt(lastItemBarcodeScanned)
                        }
                    }
                )
            )*/
        }
    }

    private suspend fun showTotePrompt(itemBarcodeType: BarcodeType.Item?) {
        val suggestedTote = when {
            itemBarcodeType != null -> {
                pickRepo.findExistingValidToteForItem(item.value?.copy(storageType = getExpectedStorageType()) ?: ItemActivityDto())
            }

            else -> null
        }
        inlineBottomSheetEvent.postValue(getToteScanArgDataAndTagForBottomSheet(suggestedTote?.containerId, item.value?.isCustomerBagPreference))
    }

    //  Find a way to scope these to the close action run block.
    private var savedItemBarcodeType: BarcodeType.Item? by StateHandler(stateHandle)
    private var savedToteBarcodeType: PickingContainer? by StateHandler(stateHandle)

    private fun showContainerReassignmentDialog(itemBarcodeType: BarcodeType.Item, toteBarcodeType: PickingContainer) {
        canAcceptScan = false
        savedItemBarcodeType = itemBarcodeType
        savedToteBarcodeType = toteBarcodeType

        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.container_reassignment_title),
                    body = StringIdHelper.Id(R.string.container_reassignment_body),
                    positiveButtonText = StringIdHelper.Id(R.string.yes),
                    negativeButtonText = StringIdHelper.Id(R.string.no),
                    cancelOnTouchOutside = false
                ),
                tag = CONTAINER_REASSIGNMENT_DIALOG_TAG,
            )
        )
    }

    private fun showConcurrentSubstitutionErrorDialog() {
        canAcceptScan = false
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.concurrent_substitution_error_dialog_title),
                    body = StringIdHelper.Id(R.string.concurrent_substitution_error_dialog_body),
                    bodyWithBold = app.getString(R.string.remove_your_item),
                    positiveButtonText = StringIdHelper.Id(R.string.close),
                    cancelOnTouchOutside = false
                ),
                tag = CONCURRENT_SUBSTITUTION_ERROR_DIALOG_TAG,
            )
        )
    }

    /**
     * Master order view: To handle retry option when any exception will occur.
     */
    private fun showMasterViewItemDetailApiErrorDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.something_went_wrong),
                    body = StringIdHelper.Id(R.string.something_wrong_body),
                    positiveButtonText = StringIdHelper.Id(R.string.ok_cta),
                    cancelOnTouchOutside = false
                ),
                tag = RELOAD_MASTER_VIEW_ITEM_DETAIL_DIALOG_TAG,
            )
        )
    }

    private fun showContainerCannotReassignDialog(itemBarcodeType: BarcodeType.Item, toteBarcodeType: PickingContainer) {
        savedItemBarcodeType = itemBarcodeType
        savedToteBarcodeType = toteBarcodeType

        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.container_cannot_reassign_title),
                    body = StringIdHelper.Id(R.string.container_cannot_reassign_body_primary),
                    secondaryBody = StringIdHelper.Id(R.string.container_cannot_reassign_body_secondary),
                    positiveButtonText = StringIdHelper.Id(R.string.ok),
                    cancelOnTouchOutside = true
                ),
                tag = CONTAINER_CANNOT_REASSIGN_DIALOG_TAG,
            )
        )
    }

    fun onManualEntryButtonClicked() {
        acuPickLogger.v("[onManualEntryButtonClicked]")
        val entryType =
            when (substitutionType.value) {
                SellByType.RegularItem, SellByType.Prepped,
                SellByType.PriceEachUnique, SellByType.PriceScaled,
                SellByType.PriceEachTotal, SellByType.PriceWeighted, SellByType.PriceEach, null,
                -> ManualEntryType.UPC

                SellByType.Weight -> ManualEntryType.Weight
                SellByType.Each -> ManualEntryType.PLU
            }

        _navigationEvent.postValue(
            NavigationEvent.Directions(
                SubstituteFragmentDirections.actionSubstituteFragmentToManualEntryPickFragment(
                    ManualEntryPickParams(
                        selectedItem = item.value,
                        requestedQty = requestedCount.value ?: 0,
                        remainingRequestedQty = remainingQtyCount.value ?: 0,
                        stageByTime = pickList.value?.stageByTime(),
                        isSubstitution = true,
                        entryType = entryType,
                        substitutedCount = substitutedCount.value,
                        isBulk = item.value?.bulkVariantList.isNotNullOrEmpty()
                    ),
                    entryType
                )
            )
        )
    }

    fun handleManualEntryData(quantity: Int, barcode: BarcodeType.Item?) {
        viewModelScope.launch(dispatcherProvider.IO) {
            isComplete.postValue(false)
            item.asFlow().launchIn(this)
            weight.postValue((barcode as? BarcodeType.Item.Weighted)?.weight.toString())

            if (quantity == QTY_CANCELLED || quantity == QTY_ALREADY_FULFILLED) {
                withContext(dispatcherProvider.Main) {
                    activeScanTarget.set(ScanTarget.Item)
                    canAcceptScan = true
                    lastItemBarcodeScanned = null
                    lastScannedItem = null
                    selectedVariant = null

                    // ACUPICK-978 restore the Complete button if applicable upon canceling picker
                    val subbedCt = subListItemUi.value?.size ?: 0
                    val shouldReenableCompleteButton = subbedCt > 0
                    isComplete.postValue(shouldReenableCompleteButton)

                    if (quantity == QTY_ALREADY_FULFILLED) {
                        delay(1000) // To show message after smooth transition of manual entry bottomsheet
                        showSubstituteFulfilledFailure()
                    }
                }
            } else {
                quantityForDesc.postValue(quantity)

                lastItemManuallyEnteredQty = quantity
                isFromQuantityPicker = true
                /**
                 * Reason dialog will not be shown in case of swap substitution flow
                 */
                if (suggestedItem.value?.itemId != null && suggestedItem.value?.itemId != lastScannedItem?.itemId && bulkVariants.isNullOrEmpty() && isFromSwapSubstitution.value.orFalse().not()) {
                    // need to wait for quantity picker to dismiss and for SubstituteFragment to be in focus before showing dialog
                    delay(500)
                    // ACURED_REDESIGN Checking if override dialog already selected
                    suggestionNotTakenReason?.let {
                        withContext(dispatcherProvider.Main) {
                            handleOverridenScannedItem()
                        }
                    } ?: run {
                        showNotChosenSubDialog()
                    }
                } else {
                    withContext(dispatcherProvider.Main) {
                        if (barcode != null) {
                            addItemFromManualEntryOrQuantityPicker(barcode, quantity)
                        }
                    }
                }
            }
        }
    }

    fun onCompleteButtonClicked() {
        showCompleteConfirmationDialog()
    }

    fun onScanSubstitutionCardClicked() {
        inlineBottomSheetEvent.postValue(
            BottomSheetArgDataAndTag(
                data = CustomBottomSheetArgData(
                    dialogType = BottomSheetType.ScanItem,
                    titleIcon = R.drawable.ic_scan_item,
                    title = StringIdHelper.Id(R.string.scan_item),
                    body = when (substitutionPath.value.isRepickOriginalItem()) {
                        true -> StringIdHelper.Id(R.string.repick_original_item_scan_hint)
                        else -> StringIdHelper.Id(R.string.scan_different_substitution)
                    },
                ),
                tag = SCAN_SUBSTITUTE_ITEM_BOTTOM_SHEET
            )
        )
    }

    private fun showCompleteConfirmationDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.substitute_complete_confirmation_dialog_title),
                    body = StringIdHelper.Id(R.string.substitute_complete_confirmation_dialog_body),
                    positiveButtonText = StringIdHelper.Id(R.string.ok),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelOnTouchOutside = false
                ),
                tag = SUBSTITUTION_COMPLETE_CONFIRMATION_DIALOG_TAG
            )
        )
    }

    fun showExitSubstitutionDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.substitute_exit_title),
                    body = StringIdHelper.Id(R.string.substitute_exit_body),
                    positiveButtonText = StringIdHelper.Id(R.string.confirm),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelable = false,
                    cancelOnTouchOutside = false
                ),
                tag = EXIT_SUBSTITUTION_DIALOG_TAG
            )
        )
    }

    /**
     * This is confirmation exit dialog in swap substitution flow
     */
    fun showExitSwapSubstitutionDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.substitute_exit_title),
                    body = StringIdHelper.Id(R.string.swap_substitute_exit_dialog_body),
                    positiveButtonText = StringIdHelper.Id(R.string.confirm),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelable = false,
                    cancelOnTouchOutside = false
                ),
                tag = EXIT_SWAP_SUBSTITUTION_DIALOG_TAG
            )
        )
    }

    private fun showNotChosenSubDialog() {
        val isCustomerChosen = suggestedItemHeader.value == app.applicationContext.getString(R.string.substitute_suggested_header_customer_chosen)
        val dialogArgs = if (isCustomerChosen)
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.CustomRadioButtons,
                    title = StringIdHelper.Id(R.string.override_substitution),
                    // TODO: ACURED_REDESIGN Will remov body after confirmation from UX team
                    // body = StringIdHelper.Id(R.string.customer_choosen_substitution_reason_description),
                    customData = listOf(
                        StringIdHelper.Id(R.string.customer_selected_out_of_stock),
                        StringIdHelper.Id(R.string.substitute_suggested_not_chosen_reason_3),
                    ) as Serializable,
                    positiveButtonText = StringIdHelper.Id(R.string.confirm),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelOnTouchOutside = false
                ),
                tag = SUGGESTED_ITEM_NOT_CHOSEN_DIALOG_TAG
            ) else
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.CustomRadioButtons,
                    title = StringIdHelper.Id(R.string.override_substitution),
                    customData = listOf(
                        StringIdHelper.Id(R.string.substitute_suggested_not_chosen_reason_2),
                        StringIdHelper.Id(R.string.system_suggestion_out_of_stock),
                        StringIdHelper.Id(R.string.substitute_suggested_not_chosen_reason_3),
                        StringIdHelper.Id(R.string.substitute_reason_price_high),
                    ) as Serializable,
                    positiveButtonText = StringIdHelper.Id(R.string.confirm),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelOnTouchOutside = false
                ),
                tag = SUGGESTED_ITEM_NOT_CHOSEN_DIALOG_TAG
            )

        inlineDialogEvent.postValue(dialogArgs)
    }

    private fun getSubstituteConfirmationArgData(): CustomBottomSheetArgData {
        return CustomBottomSheetArgData(
            dialogType = BottomSheetType.SubstitutionConfirmation,
            draggable = false,
            peekHeight = R.dimen.expanded_bottomsheet_peek_height,
            title = StringIdHelper.Id(R.string.confirm_substitution),
            positiveButtonText = StringIdHelper.Id(R.string.confirm),
            negativeButtonText = StringIdHelper.Id(R.string.add_another_substitution),
            exit = subListHolder.isEmpty(), // Exit substitute confimation bottom sheet if all subtituted item removed
            customDataParcel = SubstituteConfirmationParam(
                subListHolder.reversed(),
                imageUrl.value,
                description.value,
                if (isDisplayType3PW.value == true) remainingWeight.value.orEmpty() else remainingQtyCount.value.getOrEmpty(),
                isOrderedByWeight.value ?: false,
                isDisplayType3PW.value.orFalse(),
                requestedWeightAndUnits.value,
                iaId = iaId.value,
                siteId = siteRepo.siteDetails.value?.siteId,
                messageSid = messageSid.value,
                isCustomerBagPreference = item.value?.isCustomerBagPreference
            )
        )
    }

    private fun openSubstitueConfirmationBottomSheet() {
        if (subListHolder.size > 1) {
            // Send the live data event to tote bottomsheet
            activityViewModel.bottomSheetRecordPickArgData.postValue(
                CustomBottomSheetArgData(
                    dialogType = BottomSheetType.ToteScan,
                    exit = true,
                    title = StringIdHelper.Raw("")
                )
            )
            // activityViewModel.bottomSheetRecordPickArgData.postValue(getSubstituteConfirmationArgData()) // Send the live data event to substitute confirmation bottomsheet
        } else {
            // Open substitute confirmation bottomsheet for the first time
            inlineBottomSheetEvent.postValue(BottomSheetArgDataAndTag(data = getSubstituteConfirmationArgData(), tag = SUBSTITUTE_CONFIRM_BOTTOM_SHEET_TAG))
        }
    }

    private fun unpickSubs() {
        clearSnackBarEvents()
        if (subListHolder.isEmpty()) {
            _navigationEvent.postValue(NavigationEvent.Up)
        } else {
            viewModelScope.launch {
                val undoPicks = mutableListOf<UndoPickLocalDto>()
                subListHolder.forEach {
                    if (it.toteBarcodeType != null) {
                        undoPicks.add(
                            UndoPickLocalDto(
                                containerId = it.toteBarcodeType?.rawBarcode,
                                undoPickRequestDto = UndoPickRequestDto(
                                    actId = pickListId.toLong(),
                                    iaId = item.value?.id,
                                    pickedUpcId = item.value?.pickedUpcCodes?.find { pickedItemUpcDto ->
                                        (
                                            pickedItemUpcDto.upc == it.itemBarcodeType?.rawBarcode ||
                                                pickedItemUpcDto.upc == it.itemBarcodeType?.catalogLookupUpc ||
                                                pickedItemUpcDto.upc == it.itemBarcodeType?.getBarcodeToSendToBackend()
                                            ) && pickedItemUpcDto.containerId == it.toteBarcodeType?.rawBarcode
                                    }?.upcId,
                                    qty = it.quantity
                                )
                            )
                        )
                    }
                }
                if (undoPicks.isEmpty()) {
                    _navigationEvent.postValue(NavigationEvent.Up)
                } else {
                    val results = isBlockingUi.wrap {
                        pickRepo.undoPicks(undoPicks.toList())
                    }

                    if (results is ApiResult.Failure) {
                        withContext(dispatcherProvider.Main) {
                            toaster.toast(app.getString(R.string.item_details_undo_error))
                        }
                    } else {
                        // Close substitute confirmation bottomsheet only
                        withContext(dispatcherProvider.Main) {
                            showSnackBar(
                                AcupickSnackEvent(
                                    message = StringIdHelper.Id(R.string.substitution_deleted),
                                    type = SnackType.SUCCESS
                                )
                            )
                            subListHolder.clear()
                            subListItemUi.postValue(subListHolder)
                            activityViewModel.bottomSheetRecordPickArgData.postValue(getSubstituteConfirmationArgData())
                        }
                    }
                }
            }
        }
    }

    fun handlePluCtaResult() {
        suggestedItem.value?.let {
            val selectedItemPluCode = it.pluList?.getOrNull(0)
            if (selectedItemPluCode?.toIntOrNull() == null || selectedItemPluCode.toIntOrNull() == 0) {
                onManualEntryButtonClicked()
            } else {
                val barcodeType = barcodeMapper.generateEachBarcode(selectedItemPluCode, it.id)
                lastScannedItem = ItemDetailDto(it)
                showQuantityPicker(lastScannedItem, barcodeType)
            }
        }
    }

    fun onSubstitutionItemClicked() {
        Timber.v("[onSubstitutionItemClicked]")
        val altLocationsList = pickRepo.getAlternateLocations(suggestedItem.value?.itemId ?: "")
        item.value?.id?.let { iaId ->
            inlineBottomSheetEvent.postValue(
                getItemDetailsArgDataAndTagForBottomSheet(
                    iaId, pickList.value?.actId ?: 0, pickList.value?.activityNo.orEmpty(), altLocationsList, item.value,
                    PickListType.Todo, true
                )
            )
        }
    }

    fun onOtherOptionsClicked() {
        // TODO ACURED_Redesign canAcceptScan flag should be handled for all action sheet options
        inlineBottomSheetEvent.postValue(getSubstituteActionSheetArgsDataAndTagForBottomSheet())
    }

    fun isSomethingSubstituted() = subListHolder.isNotNullOrEmpty()

    private fun handleSubsitutionNotAllowed() {
        // TODO ACURED Redesign we would revisit the changes when working with issue scan item
        /*     if (siteRepo.areIssueScanningFeaturesEnabled) {
                 canAcceptScan = false // Substitution not allowed
                 inlineDialogEvent.postValue(
                     CustomDialogArgDataAndTag(
                         data = getNoSubstitutionAllowedDialogArgData(),
                         tag = SUBSTITUTION_NOT_ALLOWED_DIALOG_TAG,
                     )
                 )
             } else {*/
        // Substitution allowed
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = getNoSubstitutionAllowedDialogArgData(),
                tag = OVERRIDE_SUBSTITUTION_DIALOG_TAG,
            )
        )
        // }
    }

    fun showDeleteSubItemDialog(item: SubstitutionLocalItem) {
        canAcceptScan = false
        itemToRemove = item
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = getShortItemConfirmationDialogArgData(R.string.substitute_delete_item_dialog_title),
                tag = DELETE_SUBSTITUTED_ITEM_DIALOG_TAG
            )
        )
    }

    fun getCustomerSubstitutionAlertDialogArgData(suggestedItem: ItemActivityDto?): CustomDialogArgData {
        return CustomDialogArgData(
            dialogType = DialogType.SubbedItemAlert,
            title = StringIdHelper.Id(R.string.substitute_customer_selection_alert_title),
            imageUrl = suggestedItem?.imageUrl,
            body = StringIdHelper.Id(R.string.substitute_customer_selection_alert_description),
            positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
            cancelOnTouchOutside = true
        )
    }

    private fun getNoSubstitutionAllowedDialogArgData(): CustomDialogArgData {
        return CustomDialogArgData(
            dialogType = DialogType.ModalFiveConfirmation,
            title = StringIdHelper.Id(R.string.not_substitution_dialog_title),
            titleIcon = R.drawable.ic_no_substitute_item,
            body = StringIdHelper.Id(R.string.do_not_substitute_dialog_body),
            positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
            cancelOnTouchOutside = true
        )
    }

    private fun deleteScannedItem() {
        // If a tote was not scanned, we only need to remove the scanned item from the UI
        val needToUndoPick = itemToRemove?.toteBarcodeType != null

        // Otherwise, we need to call undoPick
        if (needToUndoPick) {
            viewModelScope.launch {
                val undoPickLocalDtoList = listOf(
                    UndoPickLocalDto(
                        containerId = itemToRemove?.toteBarcodeType?.rawBarcode,
                        undoPickRequestDto = UndoPickRequestDto(
                            actId = pickListId.toLong(),
                            iaId = item.value?.id,
                            netWeight = if (item.value?.sellByWeightInd == SellByType.PriceScaled) item.value?.netWeight else null,
                            pickedUpcId = item.value?.pickedUpcCodes?.find { pickedItemUpcDto ->
                                (
                                    pickedItemUpcDto.upc == itemToRemove?.itemBarcodeType?.rawBarcode ||
                                        pickedItemUpcDto.upc == itemToRemove?.itemBarcodeType?.catalogLookupUpc ||
                                        pickedItemUpcDto.upc == itemToRemove?.itemBarcodeType?.getBarcodeToSendToBackend()
                                    ) && pickedItemUpcDto.containerId == itemToRemove?.toteBarcodeType?.rawBarcode
                            }?.upcId,
                            qty = itemToRemove?.quantity
                        )
                    )
                )
                when (isBlockingUi.wrap { pickRepo.undoPicks(undoPickLocalDtoList) }) {
                    is ApiResult.Success -> {
                        resetAfterDeleteScannedItem()
                    }

                    is ApiResult.Failure -> Unit
                    else -> {}
                }
            }
        } else {
            resetAfterDeleteScannedItem()
        }
    }

    private fun completeShort(selectedReason: ShortReasonCode) {
        item.value?.let { itemActivityDto = it }
        val message = when (selectedReason) {
            ShortReasonCode.OUT_OF_STOCK -> if (shouldShowCustomerNotifyMessage()) {
                StringIdHelper.Id(R.string.the_item_has_been_marked_out_of_stock_customer_notified)
            } else {
                StringIdHelper.Id(R.string.the_item_has_been_market_out_of_stock)
            }
            ShortReasonCode.TOTE_FULL -> StringIdHelper.Plural(R.plurals.tote_full, remainingQtyCount.value ?: 0)
            ShortReasonCode.PREP_NOT_READY -> StringIdHelper.Plural(R.plurals.prep_not_ready, remainingQtyCount.value ?: 0)
            else -> null
        }
        viewModelScope.launch {
            val result = isBlockingUi.wrap {
                pickRepo.recordShortage(
                    when (selectedReason) {
                        ShortReasonCode.OUT_OF_STOCK -> createShortDto(selectedReason)
                        ShortReasonCode.TOTE_FULL -> createShortDto(selectedReason)
                        ShortReasonCode.PREP_NOT_READY -> createShortDto(selectedReason)
                        else -> ShortPickRequestDto()
                    }.exhaustive
                )
            }
            canAcceptScan = true
            when (result) {
                is ApiResult.Success -> {
                    snackBarMessageOnNavigateUp.postValue(message)
                    _navigationEvent.postValue(NavigationEvent.Up)
                }

                is ApiResult.Failure -> {
                    if (result is ApiResult.Failure.Server) {
                        val type = result.error?.errorCode?.resolvedType
                        if (type?.cannotAssignToOrder() == true) {
                            val serverErrorType = if (type == ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY) CannotAssignToOrderDialogTypes.PICKLIST else CannotAssignToOrderDialogTypes.REGULAR
                            serverErrorCannotAssignUser(serverErrorType, pickRepo.pickList.value?.erId == null)
                        } else {
                            handleApiError(result, retryAction = { completeShort(selectedReason) })
                        }
                    } else {
                        handleApiError(result, retryAction = { completeShort(selectedReason) })
                        acuPickLogger.d("ShortItemViewModel: onCompleteCtaClick $result")
                    }
                }
            }
        }
    }

    private suspend fun createShortDto(selectedReason: ShortReasonCode): ShortPickRequestDto {
        return ShortPickRequestDto(
            actId = pickRepo.pickList.first()?.actId,
            shortReqDto = createShortRequestDto(calculateQtyToShort(), selectedReason)
        )
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

    private fun calculateQtyToShort(): Double {
        // Factoring in processedAndExceptionQty due to the following scenario:
        // Item has 3 qty, picker picks 1 qty, shorts remaining (2) qty, undoes pick of 1 qty, shorts that remaining 1 qty
        return (itemActivityDto.qty.orZero()).minus(itemActivityDto.processedAndExceptionQty.orZero())
    }

    private fun resetAfterDeleteScannedItem() {
        isManualEnabled.postValue(true)
        clearSnackBarEvents()
        activeScanTarget.postValue(ScanTarget.Item)
        subListHolder.remove(itemToRemove)
        isComplete.set(subListHolder.isNotEmpty())
        subListItemUi.postValue(subListHolder.reversed())
        // Post updated list to substitute confirmation bottomsheet
        showSnackBar(
            AcupickSnackEvent(
                message = StringIdHelper.Id(R.string.substitution_deleted),
                type = SnackType.SUCCESS
            )
        )
        activityViewModel.bottomSheetRecordPickArgData.postValue(getSubstituteConfirmationArgData())
    }

    fun onSubstituteDifferentItemCardClicked() {
        showNotChosenSubDialog()
    }

    private fun getSubstituteActionSheetArgsDataAndTagForBottomSheet(): BottomSheetArgDataAndTag {
        return BottomSheetArgDataAndTag(
            data = CustomBottomSheetArgData(
                dialogType = BottomSheetType.ActionSheet,
                draggable = false,
                title = StringIdHelper.Raw(""),
                peekHeight = /*if (isSubstitutionAllowed.value == true) */R.dimen.actionsheet_peek_height /*else R.dimen.actionsheet_peek_height_sub_not_allowed*/,
                customDataParcel = ActionSheetDetails(substituteOptions)
            ),
            tag = SUBSTITTUE_ACTION_SHEET_DIALOG_TAG
        )
    }

    private fun shouldShowCustomerNotifyMessage() = pickList.value?.prePickType.isAdvancePickOrPrePick().not() && siteRepo.twoWayCommsFlags.chatBeta == true
    fun exitFromSwapSubstitutionForOtherPickersItem() = _navigationEvent.postValue(NavigationEvent.Up)
    fun shouldShowExitSwapSubstitionDialog() = swapSubReason.value?.isSwapSubstitutionForMyItems().orFalse()
    fun validateSwapSubstitutionForOtherPicker() = swapSubReason.value?.isSwapSubstitutionForOtherPicker().orFalse()
    fun shouldShowExitSwapSubstitutionDialogForOtherPicker() = validateSwapSubstitutionForOtherPicker() && itemJustSubstituted.value

    companion object {
        private const val CONTAINER_REASSIGNMENT_DIALOG_TAG = "containerReassignmentDialog"
        private const val CONTAINER_CANNOT_REASSIGN_DIALOG_TAG = "containerCannotReassignDialog"
        private const val EXIT_SUBSTITUTION_DIALOG_TAG = "exitSubstitutionDialog"
        private const val SUBSTITUTION_COMPLETE_CONFIRMATION_DIALOG_TAG = "substitutionCompleteConfirmationDialog"
        private const val DELETE_SUBSTITUTED_ITEM_DIALOG_TAG = "deleteSubstitutedItemDialog"
        private const val SUGGESTED_ITEM_NOT_CHOSEN_DIALOG_TAG = "suggestedItemNotChosenDialog"
        const val SUBSTITUTION_SUGGESTED_ITEM_DETAILS = "substitutionSuggestedItemDetails"
        private const val CUSTOMER_SUBSTITUTION_ALERT_DIALOG = "customerSubstitutionAlertDialog"
        private const val RELOAD_MASTER_VIEW_ITEM_DETAIL_DIALOG_TAG = "reloadMasterViewItemDetailDialogTag"
        private val suggestedItemNotChosenList = listOf(SubReasonCode.NotRelevant, SubReasonCode.OutOfStock, SubReasonCode.BadQuality, SubReasonCode.PriceDifferenceIsTooHigh)
        private val suggestedChosenItemNotChosenList = listOf(SubReasonCode.OutOfStock, SubReasonCode.BadQuality)
        private const val QUANTITY_PICKER_SUBS_DIALOG_TAG = "quanitityPickerSubsDialogTag"
        private const val BULK_VARIANT_BOTTOM_SHEET = "bulkVariantSelectionTag"
        private const val SUBSTITUTE_CONFIRM_BOTTOM_SHEET_TAG = "substituteConfirmBottomSheetTag"
        private const val SCAN_SUBSTITUTE_ITEM_BOTTOM_SHEET = "scanSubstituteItemBottomSheetTag"
        private const val SUBSTITTUE_ACTION_SHEET_DIALOG_TAG = "SubstituteActionSheetDialogTag"
        private const val EXIT_SWAP_SUBSTITUTION_DIALOG_TAG = "exitSwapSubstitutionDialogTag"
        private const val CONCURRENT_SUBSTITUTION_ERROR_DIALOG_TAG = "concurrentSubstitutionErrorDialog"
        const val KEY_ITEM_HAS_UPDATED = "itemHasUpdated"
        const val NAVIGATE_BACK_FROM_SUBSTITUTION_UI = "navigateBackFromSubstitutionUi"
        const val QTY_CANCELLED = 0
        const val QTY_ALREADY_FULFILLED = -1
        const val REMOVE_SUBSTITUTION_RESULTS = "removeSubstitutionResult"
        const val REMOVE_SUBSTITUTION_REQUEST_KEY = "removeSubstitutionRequestKey"
        const val SHORT_ITEM_REASON_TAG = "shortItemDialog"
    }
}
