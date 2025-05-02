package com.albertsons.acupick.ui.itemdetails

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.AcuPickConfig
import com.albertsons.acupick.R
import com.albertsons.acupick.data.logic.getCustomerType
import com.albertsons.acupick.data.logic.shouldShowCustomerType
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.CustomerType
import com.albertsons.acupick.data.model.ImageSizePreset
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.ShortReasonCode
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.request.ShortPickRequestDto
import com.albertsons.acupick.data.model.request.ShortRequestDto
import com.albertsons.acupick.data.model.response.CannotAssignToOrderDialogTypes
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.ItemLocationDto
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.fulfilledWeight
import com.albertsons.acupick.data.model.response.fullContactName
import com.albertsons.acupick.data.model.response.isIssueScanned
import com.albertsons.acupick.data.model.response.isShorted
import com.albertsons.acupick.data.model.response.isSubstituted
import com.albertsons.acupick.data.model.response.processedAndExceptionQty
import com.albertsons.acupick.data.model.response.requestedNetWeight
import com.albertsons.acupick.data.model.response.splitItems
import com.albertsons.acupick.data.model.textValue
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.DevOptionsRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.data.toast.Toaster
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.infrastructure.utils.orZero
import com.albertsons.acupick.infrastructure.utils.roundToLongOrZero
import com.albertsons.acupick.infrastructure.utils.toTwoDecimalString
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.dialog.CloseAction
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.SHORT_ITEM_REASON_DIALOG
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.picklistitems.FLASH_WARNING_DIALOG_TAG
import com.albertsons.acupick.ui.picklistitems.PARTNER_PICK_OUT_OF_STOCK_DIALOG_TAG
import com.albertsons.acupick.ui.picklistitems.PARTNER_PICK_PREP_NOT_READY_DIALOG_TAG
import com.albertsons.acupick.ui.picklistitems.PickListType
import com.albertsons.acupick.ui.picklistitems.getDismissUnPickBottomSheetArgDataAndTag
import com.albertsons.acupick.ui.picklistitems.getSelectedFlashWarningArgAndTag
import com.albertsons.acupick.ui.substitute.SubstituteParams
import com.albertsons.acupick.ui.util.asCustomerComments
import com.albertsons.acupick.ui.util.asItemLocation
import com.albertsons.acupick.ui.util.asStatusPillString
import com.albertsons.acupick.ui.util.asSubstitutionInfo
import com.albertsons.acupick.ui.util.asUpcOrPlu
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.sizedImageUrl
import com.albertsons.acupick.ui.util.toItemAction
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZonedDateTime

class ItemDetailsViewModel(
    private val app: Application,
    private val pickRepository: PickRepository,
    private val activityViewModel: MainActivityViewModel,
    private val dispatcherProvider: DispatcherProvider,
    private val toaster: Toaster,
    private val userRepository: UserRepository,
    private val barcodeMapper: BarcodeMapper,
    private val devOptionsRepository: DevOptionsRepository,
    private val networkAvailabilityManager: NetworkAvailabilityManager,
    private val siteRepository: SiteRepository
) : BaseViewModel(app) {

    // Incoming state
    var actId: Long = 0
    val iaId = MutableLiveData<Long>()
    val pickNumber = MutableLiveData<String>()
    val altLocations = MutableLiveData<List<ItemLocationDto>>()
    var pickListType = PickListType.Todo

    private lateinit var itemActivityDto: ItemActivityDto

    private var shortSelection = 0

    // Data binding
    val pickList = pickRepository.pickList.asLiveData()
    val isFromSubstitutionFlow = MutableLiveData(false)
    private val item = combine(iaId.asFlow(), pickList.asFlow(), isFromSubstitutionFlow.asFlow()) { iaId, pickList, isFromSub ->
        if (isFromSub) {
            pickRepository.getSubstitutionItemDetails(iaId)?.let { substituionDetails ->
                substituionDetails.copy(
                    locationDetail = substituionDetails.locationDetail ?: pickList?.itemActivities?.find { pickList -> pickList.itemId == substituionDetails.itemId }?.locationDetail,
                    itemAddressDto = substituionDetails.itemAddressDto ?: pickList?.itemActivities?.find { pickList -> pickList.itemId == substituionDetails.itemId }?.itemAddressDto
                )
            }
        } else
            pickList?.itemActivities?.find { it.id == iaId }
    }.asLiveData()
    private val isRepickOriginalItemAllowed = siteRepository.twoWayCommsFlags?.allowRepickOriginalItem.orFalse() // Re-pick original item: added safe call ? to avoid test cases failure
    val orderNumber = item.map { it?.customerOrderNumber }
    val fulfillmentType = item.map { it?.fulfillment }
    val itemAddress = item.map { it.asItemLocation(app.applicationContext) }
    val isFullyPicked = item.map { it?.isFullyPicked() == true }
    val processedQty = item.map {
        if (pickListType == PickListType.Todo || pickListType == PickListType.Picked) {
            it?.processedQty.roundToLongOrZero().toString()
        } else {
            it?.exceptionQty
                .roundToLongOrZero().toString()
        }
    }
    val totalQty = item.map { it?.qty.roundToLongOrZero().toString() }
    val imageUrl = item.map { it?.sizedImageUrl(ImageSizePreset.ItemDetails).orEmpty() }
    val description = item.map { it?.itemDescription.orEmpty() }

    val hideUnpick = item.map {
        (it?.isSubstituted.orFalse() || it?.isIssueScanned.orFalse()) && siteRepository.twoWayCommsFlags.realTimeSubstitutions.orFalse() &&
            isRepickOriginalItemAllowed.not()
    }
    val shouldRepickOriginalItemAllow = item.map {
        isRepickOriginalItemAllowed && (it?.isSubstituted.orFalse() || it?.isIssueScanned.orFalse())
    }
    val upc = item.map { it.asUpcOrPlu(app.applicationContext, barcodeMapper) }
    val fullContactName = item.map { it?.fullContactName() }
    val isCustomerCommented = item.map { it?.instructionDto?.text.isNotNullOrEmpty() }
    val customerComments = item.map { it.asCustomerComments(app) }
    val substitutionInfo = item.map { it?.asSubstitutionInfo(app).orEmpty() }
    val shortQty = item.map { app.getString(R.string.number_short_format, it?.exceptionQty.roundToLongOrZero().toString()) }
    val routeVanNumber: LiveData<String> = MutableLiveData()
    val noScansOrExceptions = item.map { it?.pickedUpcCodes.isNullOrEmpty() && it?.shortedItemUpc.isNullOrEmpty() }
    val itemActionList = item.map { item ->
        val pickedItemActionDbViewModels = item?.pickedUpcCodes?.let { pickedItems ->
            pickedItems.splitItems(barcodeMapper, siteRepository.fixedItemTypesEnabled).map { pickedItem ->
                ItemActionDbViewModel(itemAction = pickedItem.toItemAction(app, item, barcodeMapper), onCheckedChange = ::updateUnpickButtonStatus)
            }
        }.orEmpty()

        pickedItemActionDbViewModels
    }
    val isUnpickButtonEnabled = itemActionList.map { itemActionList -> itemActionList.any { it.isChecked.value == true || it.itemAction.isPWItem || it.itemAction.sellByType == SellByType.PriceEach } }
    val barcodeType: LiveData<BarcodeType> = MutableLiveData()
    val isShowWeightIndicator = item.map { it?.sellByWeightInd == SellByType.Weight }
    val isOrderedByWeight = item.map { it?.isOrderedByWeight() == true }
    val weightString = item.map { it?.getWeightAndUom().orEmpty() }
    val isEbt = item.map { it?.isSnap.orFalse() }
    val isCattEnabled = AcuPickConfig.cattEnabled.asLiveData()
    private val isSubscription = item.map { it?.isSubscription.orFalse() }
    val customerTypeIcon: LiveData<CustomerType?> = combine(isEbt.asFlow(), isSubscription.asFlow()) { isEbt, isFreshPass ->
        getCustomerType(isEbt, isFreshPass)
    }.asLiveData()
    val showCustomerType = combine(AcuPickConfig.cattEnabled, customerTypeIcon.asFlow()) { cattEnabled, customerType ->
        shouldShowCustomerType(cattEnabled, customerType)
    }.asLiveData()

    // Start PW item variables
    val fulfilledWeight = item.map { it?.fulfilledWeight }
    val orderedWeight = item.map { it?.orderedWeight }
    val totalWeight = item.map {
        (if ((it?.requestedNetWeight ?: 0.0) % 1 != 0.0) it?.requestedNetWeight ?: 0.0 else it?.requestedNetWeight).toTwoDecimalString()
    }
    // As per figma ACURED_REDESIGN picked item less than < 49% will not falls under the error case
    val showError = item.map { item ->
        item?.processedWeight.orZero() > ((item?.requestedNetWeight ?: 0.0) + ((item?.requestedNetWeight ?: 0.0) * 0.1))
    }
    val showWeightIndicator = item.map {
        it?.sellByWeightInd == SellByType.PriceWeighted
    }
    val showCompletePickButton = item.map {
        pickListType == PickListType.Todo && it?.sellByWeightInd == SellByType.PriceWeighted
    }
    val isDisplayType3Pw = item.map { siteRepository.isDisplayType3PWEnabled && it?.isDisplayType3PW() == true }
    private val maxWeightLimit = item.map { it?.orderedWeight?.plus((siteRepository.weightedItemThreshold * it.minWeight.orZero())).orZero() }
    private val minWeightLimit = item.map { it?.orderedWeight?.minus((siteRepository.weightedItemThreshold * it.minWeight.orZero())).orZero() }
    val enableCompletePickButton =
        combine(item.asFlow(), processedQty.asFlow(), isDisplayType3Pw.asFlow(), maxWeightLimit.asFlow(), minWeightLimit.asFlow()) { item, processedQty, isPw, maxWeight, minWeight ->
            val inRange = if (isPw) item?.fulfilledWeight.orZero() in minWeight..maxWeight else processedQty == item?.qty.roundToLongOrZero().toString()
            inRange && pickListType == PickListType.Todo && item?.sellByWeightInd == SellByType.PriceWeighted
        }.asLiveData()

    val completePickCTAClicked = MutableLiveData<ItemActivityDto>()
    // End PW variables

    // Events
    val showItemPhotoDialog: LiveData<String> = LiveEvent()

    val hasExceptions = item.map { it?.isFullyPicked() == true && (it.isShorted || it.isSubstituted) }
    val itemComplete = item.map { it?.isFullyPicked() == true || pickListType == PickListType.Picked || pickListType == PickListType.Short }
    val status = item.map { it?.asStatusPillString(app.applicationContext, pickListType) }
    // TODO Redesign We can change it to variable as scond parameter is not used.
    val navigation: LiveData<Pair<CloseAction, Int?>> = LiveEvent()

    val isAllItemsSelected = MutableLiveData(false)
    lateinit var checkedItems: List<ItemActionBackingType>
    val sellByType = item.map { it?.sellByWeightInd }
    // Unpick variables
    val hideUnpickAllCheckBox = combine(sellByType.asFlow(), itemActionList.asFlow()) { sellByType, itemActionList ->
        itemActionList.size > 1 && sellByType != SellByType.Prepped && sellByType != SellByType.Weight && sellByType != SellByType.PriceWeighted && sellByType != SellByType.PriceEach
    }.asLiveData()

    private val isPluCodeAvailable = item.map {
        val selectedItemPluCode = it?.pluList?.getOrNull(0)
        selectedItemPluCode?.toIntOrNull() == null || selectedItemPluCode.toIntOrNull() == 0
    }
    val showPluCta = item.map { it?.sellByWeightInd == SellByType.Each && pickListType == PickListType.Todo }
    val pluCtaText = upc.map {
        val resId = if (it.isBlank()) {
            R.string.enter_the_plu_number
        } else {
            R.string.select_quantity
        }
        app.getString(resId)
    }
    val pluCtaEvent = LiveEvent<Unit>()
    val addLocationEvent = LiveEvent<Unit>()
    val locationCtaEvent = LiveEvent<Unit>()
    val locationNoteDetails = item.map { it?.locationDetail }
    val enableNoteLocationCta = item.map { it?.locationDetail.isNotNullOrEmpty() }
    val enableAddLocationCta = item.map { item ->
        val isItemAddressDtoNull = item?.itemAddressDto == null
        val isMissingLocationEnabledInStoreLeveL = siteRepository.siteDetails.value?.storeLevelTempFlags?.missingLocationEnabledApp.toBoolean()
        val isDepNameNotInMissingItemLocDisabledDepts = pickList.value
            ?.missingItemLocDisabledDepts
            ?.map { it.lowercase() }
            ?.contains(item?.depName?.lowercase()) == false
        isItemAddressDtoNull && isMissingLocationEnabledInStoreLeveL && isDepNameNotInMissingItemLocDisabledDepts
    }

    init {
        registerCloseAction(SUGGEST_SUBSTITUTION_DIALOG_TAG) { closeActionFactory(positive = { navigateToShortItemDialog() }) }

        registerCloseAction(OVERRIDE_SUBSTITUTION_DIALOG_TAG) {
            closeActionFactory(positive = { navigateToSubstitution() })
        }

        registerCloseAction(SHORT_ITEM_REASON_TAG) {
            closeActionFactory(
                positive = { selection ->
                    if (pickList.value?.orderType == OrderType.FLASH) {
                        shortSelection = selection ?: 0
                        getSelectedFlashWarningArgAndTag(selection)?.let {
                            inlineDialogEvent.postValue(it)
                        } ?: run { makeShortReasonSelection(selection) }
                    } else {
                        makeShortReasonSelection(selection)
                    }
                }
            )
        }

        // TODO ACURED_Redesign Need to remove this un used listener
        registerCloseAction(FLASH_WARNING_DIALOG_TAG) {
            closeActionFactory(positive = { makeShortReasonSelection(shortSelection) })
        }

        registerCloseAction(PARTNER_PICK_PREP_NOT_READY_DIALOG_TAG) {
            closeActionFactory(positive = { makeShortReasonSelection(shortSelection) })
        }

        registerCloseAction(PARTNER_PICK_OUT_OF_STOCK_DIALOG_TAG) {
            closeActionFactory(positive = { makeShortReasonSelection(shortSelection) })
        }
    }

    /** A backdoor for unit tests */
    fun setItem(item: ItemActivityDto) {
        this.item.set(item)
    }

    /** Entry point for a barcode received from the scanner/DataWedge */
    fun onScannerBarcodeReceived(barcodeType: BarcodeType) {
        handleScannedItem(barcodeType)
    }

    private fun handleScannedItem(inputBarcode: BarcodeType) {
        viewModelScope.launch(dispatcherProvider.IO) {
            val fetchedItem =
                if (inputBarcode is BarcodeType.Item && pickRepository.getItem(inputBarcode, item.value?.customerOrderNumber)?.id == item.value?.id) {
                    item
                } else {
                    Timber.w("[handleScannedItem] barcodeType is not an item barcode")
                    null
                }
            val foundItem = fetchedItem != null
            val itemFullyPicked = fetchedItem?.value?.isFullyPicked() == true
            val isScanSuccess = foundItem && !itemFullyPicked

            barcodeType.postValue(
                if (isScanSuccess || foundItem && itemFullyPicked) {
                    inputBarcode
                } else {
                    BarcodeType.Unknown("")
                }
            )
            _navigationEvent.postValue(NavigationEvent.Up)
        }
    }

    private fun makeShortReasonSelection(selection: Int?) {
        when (selection) {
            0 -> completeShort(ShortReasonCode.OUT_OF_STOCK)
            1 -> completeShort(ShortReasonCode.TOTE_FULL)
            2 -> completeShort(ShortReasonCode.PREP_NOT_READY)
            else -> Unit
        }
    }

    fun onSelectAllClicked() {
        itemActionList.value?.forEach {
            it.isChecked.value = !(isAllItemsSelected.value ?: false)
        }
        isUnpickButtonEnabled.postValue(!(isAllItemsSelected.value ?: false))
        isAllItemsSelected.value = !(isAllItemsSelected.value ?: false)
    }

    fun onLabelClicked() {
        navigation.postValue(Pair(CloseAction.Positive, 0))
    }

    fun onCommentClick() {
        locationCtaEvent.postValue(Unit)
    }

    fun onManualEntryButtonClicked() {
        navigation.postValue(Pair(CloseAction.Negative, 0))
    }

    fun undoItemActions() {
        itemActionList.value?.let { activityViewModel.bottomSheetRecordPickArgData.postValue(getDismissUnPickBottomSheetArgDataAndTag(it)) }
    }

    private fun updateUnpickButtonStatus() {
        val isChecked = itemActionList.value?.any { it.isChecked.value == true } == true
        // Check Select all check box if all the items are checked individualy
        isAllItemsSelected.value = itemActionList.value?.all { it.isChecked.value == true } == true
        isUnpickButtonEnabled.postValue(isChecked || this.item.value?.sellByWeightInd == SellByType.PriceWeighted || this.item.value?.sellByWeightInd == SellByType.PriceEach)
    }

    fun onImageClicked() {
        Timber.v("[onImageClicked]")
        viewModelScope.launch {
            showItemPhotoDialog.postValue(
                item.value?.sizedImageUrl(
                    if (networkAvailabilityManager.isConnected.first()) ImageSizePreset.ItemZoom else ImageSizePreset.PickList
                )
            )
        }
    }

    fun onClickPluCta() {
        pluCtaEvent.postValue(Unit)
    }

    fun onCompletePickClicked() {
        acuPickLogger.v("[onCompletePickClicked] item=${item.value}")
        completePickCTAClicked.postValue(item.value)
    }

    fun onLocationClicked() {
        locationCtaEvent.postValue(Unit)
    }

    private fun navigateToShortItemDialog() {
        item.value?.let {
            itemActivityDto = it
            inlineDialogEvent.postValue(CustomDialogArgDataAndTag(data = SHORT_ITEM_REASON_DIALOG, tag = SHORT_ITEM_REASON_TAG))
        }
    }

    private fun completeShort(selectedReason: ShortReasonCode) {
        viewModelScope.launch {
            activityViewModel.setLoadingState(isLoading = true, blockUi = true)
            val result = isBlockingUi.wrap {
                pickRepository.recordShortage(
                    when (selectedReason) {
                        ShortReasonCode.OUT_OF_STOCK -> createShortDto(selectedReason)
                        ShortReasonCode.TOTE_FULL -> createShortDto(selectedReason)
                        ShortReasonCode.PREP_NOT_READY -> createShortDto(selectedReason)
                        else -> ShortPickRequestDto()
                    }.exhaustive
                )
            }

            when (result) {
                is ApiResult.Success -> {
                    // todo green toast like on pickListItems, maybe a navigation arg to show on previous screen?
                    val actId = pickRepository.pickList.first()?.actId.toString().also {
                        it.logError(
                            "Null Activity Id. ItemDetailsViewModel(completeShort), Order Id-${pickRepository.pickList.first()?.customerOrderNumber}, User Id-${
                            userRepository.user.value?.userId
                            }, storeId-${siteRepository.siteDetails.value?.siteId}"
                        )
                    }
                    // TODO: Move into PickRepository (reference approach used in recordPick)
                    if (!devOptionsRepository.useOnlineInMemoryPickListState) {
                        pickRepository.getActivityDetails(actId)
                    }
                }

                is ApiResult.Failure -> {
                    if (result is ApiResult.Failure.Server) {
                        val type = result.error?.errorCode?.resolvedType
                        if (type?.cannotAssignToOrder() == true) {
                            val serverErrorType = if (type == ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY) CannotAssignToOrderDialogTypes.PICKLIST else CannotAssignToOrderDialogTypes.REGULAR
                            serverErrorCannotAssignUser(serverErrorType, pickRepository.pickList.value?.erId == null)
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
                userId = userRepository.user.value?.userId
            )
        )
        return shortRequestList.toList()
    }

    private fun navigateToSubstitution() {
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                ItemDetailsFragmentDirections.actionItemDetailsFragmentToSubstituteFragment(
                    SubstituteParams(
                        iaId = iaId.value,
                        pickListId = actId.toString()
                    )
                )
            )
        )
    }

    fun onAddLocationClicked() {
        addLocationEvent.postValue(Unit)
    }

    companion object {
        const val RETRY_AUTO_SHORT_DIALOG = "itemDetailsRetryAutoShortDialogTag"
        private const val SUGGEST_SUBSTITUTION_DIALOG_TAG = "suggestSubstitutionDialogTag"
        private const val SHORT_ITEM_REASON_TAG = "shortItemDialog"

        // private const val CONFIRMATION_DIALOG_TAG = "confirmationDialog"
        private const val OVERRIDE_SUBSTITUTION_DIALOG_TAG = "overrideSubstitutionDialog"
        const val CONFIRMATION_UNDO_SHORT_DIALOG_TAG = "confirmationUndoShortDialog"
        const val ITEM_DETAILS_PLU_REQUEST_KEY = "itemDetailsPluRequestKey"
        const val ADD_LOCATION_REQUEST_KEY = "addLocationRequestKey"
        const val UNPICK_RESULT_DATA_KEY = "unpickresultdatakey"
        const val UNPICK_RESULT_KEY = "unpickresultkey"
        const val COMPLETE_PICK_REQUEST_KEY = "completePickRequestKey"
        const val COMPLETE_PICK_RESULTS = "completePickResult"
    }
}
