package com.albertsons.acupick.ui.manualentry.pick

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.FirebaseAnalyticsInterface
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.FulfilledQuantityResult
import com.albertsons.acupick.data.model.ItemSearchResult
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.ItemDetailDto
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.models.QuantityParams
import com.albertsons.acupick.data.model.QuantitySelectionType
import com.albertsons.acupick.data.model.SubReasonCode
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.text
import com.albertsons.acupick.data.picklist.getQuantitySelectionType
import com.albertsons.acupick.data.picklist.getQuantitySelectionTypeForIssueScanning
import com.albertsons.acupick.data.picklist.getQuantitySelectionTypeForSubstitution
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetArgDataAndTag
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetType
import com.albertsons.acupick.ui.bottomsheetdialog.CustomBottomSheetArgData
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.manualentry.ManualEntryPickParams
import com.albertsons.acupick.ui.manualentry.ManualEntryType
import com.albertsons.acupick.ui.manualentry.ManualTabUI
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.models.OriginalItemParams
import com.albertsons.acupick.ui.picklistitems.getConfirmAmountArgDataAndTagForBottomSheet
import com.albertsons.acupick.ui.substitute.SubstituteViewModel.Companion.QTY_ALREADY_FULFILLED
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.getFormattedValue
import com.albertsons.acupick.ui.util.orFalse
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.io.Serializable

const val MIN_WEIGHT_LB = 0.01
const val MAX_WEIGHT_LB = 99.99
const val BY_EACH_PLU_0 = "0"
const val MAX_ENTRY_LENGTH_PLU = 5
const val MAX_ENTRY_LENGTH_UPC = 20 // RANDOM

class ManualEntryPagerViewModel(val app: Application) : BaseViewModel(app) {

    val dispatcherProvider: DispatcherProvider by inject()
    val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    val userRepo: UserRepository by inject()
    val siteRepo: SiteRepository by inject()
    val pickRepo: PickRepository by inject()
    val barcodeMapper: BarcodeMapper by inject()
    val firebaseAnalytics: FirebaseAnalyticsInterface by inject()

    var manualEntryParams: ManualEntryPickParams? = null
    val isUPC = MutableLiveData<Boolean>()
    val isPlu = MutableLiveData<Boolean>()
    val isWeighted = MutableLiveData<Boolean>()
    val weightEntry = MutableLiveData<String>()
    val tabVisibility = MutableLiveData(false)

    val quantity = MutableLiveData<Int>()
    val barcodeType = MutableLiveData<BarcodeType>()

    val selectedTabFlow = MutableStateFlow<ManualEntryType?>(null)

    val activeTab = MutableSharedFlow<String>()

    val triggerBarcodeCollection: LiveData<Unit> = LiveEvent()
    val playScanSound: LiveData<Boolean> = LiveEvent()

    var itemEnteredBarcode: BarcodeType? = null
        private set

    var enteredItemDetails: ItemDetailDto? = null
        private set

    // State
    private val isContinueEnabledUpc = MutableStateFlow(false)
    private val isContinueEnabledPlu = MutableStateFlow(false)
    private val isContinueEnabledWeight = MutableStateFlow(false)

    val isContinueEnabled = combine(isContinueEnabledUpc, isContinueEnabledPlu, isContinueEnabledWeight, selectedTabFlow) { upc, plu, weight, tab ->
        when (tab) {
            ManualEntryType.PLU -> plu
            ManualEntryType.Weight -> weight
            ManualEntryType.UPC -> upc
            else -> false
        }
    }.asLiveData()

    val tabData = MutableLiveData<List<ManualTabUI>>()

    init {
        registerCloseAction(QUANTITY_PICKER_MANUAL_ENTRY) {
            closeActionFactory(
                positive = { selection ->
                    quantity.postValue(selection)
                }
            )
        }
    }

    fun setEntryParams(manualEntryParams: ManualEntryPickParams) {
        this.manualEntryParams = manualEntryParams
        isUPC.postValue(manualEntryParams.entryType == ManualEntryType.UPC || manualEntryParams.entryType == ManualEntryType.Barcode)
        isPlu.postValue(manualEntryParams.entryType == ManualEntryType.PLU)
        isWeighted.postValue(manualEntryParams.entryType == ManualEntryType.Weight)
    }

    fun setTabData(manualEntryParams: ManualEntryPickParams): List<ManualTabUI> {
        return listOf(
            ManualTabUI(manualEntryType = ManualEntryType.UPC, tabArguments = ManualEntryPagerFragmentArgs(manualEntryParams, ManualEntryType.UPC)),
            ManualTabUI(manualEntryType = ManualEntryType.PLU, tabArguments = ManualEntryPagerFragmentArgs(manualEntryParams, ManualEntryType.PLU)),
            ManualTabUI(manualEntryType = ManualEntryType.Weight, tabArguments = ManualEntryPagerFragmentArgs(manualEntryParams, ManualEntryType.Weight))
        )
    }

    fun setSelectedTab(input: ManualEntryType?) {
        viewModelScope.launch {
            selectedTabFlow.emit(input)
        }
    }

    fun setContinueEnabled(it: Boolean?) {
        viewModelScope.launch {
            when (selectedTabFlow.value) {
                ManualEntryType.PLU -> isContinueEnabledPlu.emit(it ?: false)
                ManualEntryType.Weight -> isContinueEnabledWeight.emit(it ?: false)
                ManualEntryType.UPC -> isContinueEnabledUpc.emit(it ?: false)
                else -> isContinueEnabled.postValue(false)
            }
        }
    }

    fun onContinueButtonClicked() = triggerBarcodeCollection.postValue(Unit)

    /**
     * Adds an extra parameter to the itemDetails API only for Priced item barcode to fetch basePrice, basePricePer in
     * issue scanning flow.
     * @param queryType
     * @param queryType = issueScanning if we are using itemDetails API with issue scanning flow
     * In other flow this parameter will be null
     */
    fun onBarcodeCollected(barcode: BarcodeType) {
        viewModelScope.launch(dispatcherProvider.Main) {
            val itemDetails = if (!networkAvailabilityManager.isConnected.first()) {
                if (manualEntryParams?.selectedItem?.sellByWeightInd != SellByType.Each && barcode is BarcodeType.Item.Each) {
                    ItemDetailDto.unknownItem
                } else {
                    when (val scannedSubItem = pickRepo.getItemWithoutOrderOrCustomerDetails(barcode as? BarcodeType.Item)) {
                        is ItemSearchResult.MatchedItem -> {
                            ItemDetailDto(scannedSubItem.itemActivityDto)
                        }
                        is ItemSearchResult.Error -> {
                            ItemDetailDto.unknownItem
                        }
                    }
                }
            } else {
                val siteId = userRepo.user.value!!.selectedStoreId.orEmpty()
                val is3p = pickRepo.pickList.value?.is3p.orFalse()
                val activityId = if (is3p) pickRepo.pickList.value?.actId else null
                val itemId = if (is3p) manualEntryParams?.selectedItem?.itemId else null

                val result = when (val barcodeType = barcode as? BarcodeType.Item) {
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
                        sellByWeightInd = SellByType.Prepped.code,
                        queryType = if (manualEntryParams?.isIssueScanning == true) SubReasonCode.IssueScanning.text() else null
                    )
                    else -> pickRepo.getItemDetails(
                        siteId = siteId,
                        actId = activityId,
                        originalItemId = itemId,
                        upcId = barcodeType?.catalogLookupUpc.orEmpty()
                    )
                }
                when (result) {
                    is ApiResult.Success -> {
                        result.data
                    }
                    is ApiResult.Failure ->
                        if (result is ApiResult.Failure.Server) {
                            when (result.error?.errorCode?.resolvedType) {
                                ServerErrorCode.FFC_RESTRICTION -> {
                                    showScanItemRestrictionFailure(R.string.substitute_entered_ffc_error)
                                    null
                                }
                                ServerErrorCode.BPN_RESTRICTION -> {
                                    showScanItemRestrictionFailure(R.string.substitute_entered_bpn_error)
                                    null
                                }
                                else -> {
                                    ItemDetailDto.unknownItem
                                }
                            }
                        } else if (manualEntryParams?.isSubstitution == true || manualEntryParams?.isIssueScanning == true) {
                            ItemDetailDto.unknownItem
                        } else null
                }
            }

            val isSameItem = manualEntryParams?.selectedItem?.itemId == itemDetails?.itemId

            val itemActivity = if (isSameItem) {
                pickRepo.getItem(
                    itemBpnId = itemDetails?.itemId.toString(),
                    customerOrderNumber = manualEntryParams?.selectedItem?.customerOrderNumber
                )
            } else {
                null
            }
            val requestedQty = manualEntryParams?.requestedQty ?: 0
            val enteredQty = manualEntryParams?.selectedItem?.processedQty?.toInt() ?: 0

            if (itemActivity?.sellByWeightInd == SellByType.PriceWeighted || itemActivity?.sellByWeightInd == SellByType.PriceEach || itemDetails?.bulkVariantList.isNotNullOrEmpty()) {
                val eaches = (barcode as? BarcodeType.Item.Each)
                if (itemDetails?.bulkVariantList.isNotNullOrEmpty() && eaches is BarcodeType.Item.Each &&
                    (manualEntryParams?.selectedItem?.bulkVariantList.isNullOrEmpty() || manualEntryParams?.selectedItem?.pluCode != eaches.plu)
                ) {
                    // for bulk variant of eaches type we dont set itemActivityDBid from the original
                    barcodeType.postValue(barcodeMapper.generateEachBarcode(eaches.plu, null))
                } else {
                    barcodeType.postValue(barcode)
                }
            } else if (isSameItem && requestedQty <= enteredQty) {
                // use quantity field to return an error code
                quantity.postValue(QTY_ALREADY_FULFILLED)
            } else if (itemDetails != null) {
                setManualEntryPicker(
                    barcode = barcode,
                    itemActivity = itemActivity,
                    itemDetails = itemDetails,
                    isSameItem = isSameItem,
                )
                enteredItemDetails = itemDetails
                itemEnteredBarcode = barcode
            }
        }
    }

    private suspend fun showScanItemRestrictionFailure(@StringRes id: Int) {
        playScanSound.postValue(false)
        withContext(dispatcherProvider.Main) {
            showSnackBar(
                AcupickSnackEvent(
                    message = StringIdHelper.Id(id),
                    type = SnackType.ERROR
                )
            )
        }
    }

    private fun setManualEntryPicker(barcode: BarcodeType, itemActivity: ItemActivityDto?, itemDetails: ItemDetailDto?, isSameItem: Boolean) {

        val requestedQty = manualEntryParams?.requestedQty ?: 0
        val remainingRequestedQty = manualEntryParams?.remainingRequestedQty ?: 0
        val quantitySelectionType = if (manualEntryParams?.isSubstitution == true) getQuantitySelectionTypeForSubstitution(barcode, remainingRequestedQty, itemDetails?.isOrderedByWeight())
        else if (manualEntryParams?.isIssueScanning == true) {
            getQuantitySelectionTypeForIssueScanning(barcode)
        } else
            getQuantitySelectionType(itemActivity, barcode)
        when (quantitySelectionType) {
            QuantitySelectionType.QuantityPicker -> {
                showQuantityPicker(
                    QuantityParams(
                        barcodeFormatted = barcode.getFormattedValue(context = app, hideUnits = true),
                        isPriced = barcode is BarcodeType.Item.Priced && itemActivity?.isPrepped() == true,
                        isWeighted = barcode is BarcodeType.Item.Weighted,
                        isEaches = barcode is BarcodeType.Item.Each,
                        isTotaled = itemActivity?.isPriceEachTotaled() == true,
                        itemId = getItemId(itemActivity, itemDetails),
                        description = getItemDesc(itemActivity, itemDetails),
                        image = getItemImage(itemActivity, itemDetails),
                        weightEntry = weightEntry.value,
                        requested = requestedQty,
                        entered = manualEntryParams?.selectedItem?.processedQty?.toInt() ?: 0,
                        isSubstitution = manualEntryParams?.isSubstitution,
                        isIssueScanning = manualEntryParams?.isIssueScanning ?: false,
                        storageType = itemDetails?.storageType,
                        isRegulated = itemDetails?.isRegulated,
                        isSameItem = isSameItem,
                        shouldShowOriginalItemInfo = manualEntryParams?.isIssueScanning ?: false || manualEntryParams?.isBulk.orFalse(),
                        originalItemParams = if (manualEntryParams?.isIssueScanning == true || manualEntryParams?.isBulk.orFalse()) OriginalItemParams(
                            manualEntryParams?.selectedItem?.itemDescription, manualEntryParams?.selectedItem?.itemId, manualEntryParams?.selectedItem?.imageUrl,
                            (manualEntryParams?.selectedItem?.todoCountWithoutIssueReportedItem(manualEntryParams?.selectedItem?.id)?.toInt())
                        ) else null,
                        isCustomerBagPreference = manualEntryParams?.selectedItem?.isCustomerBagPreference
                    ),
                    isFullScreen = manualEntryParams?.isIssueScanning ?: false || manualEntryParams?.isBulk.orFalse()
                )
            }
            QuantitySelectionType.ConfirmAmount -> itemActivity?.let { showConfirmAmountFragment(it) }
            else -> quantity.postValue(1)
        }
    }

    private fun showQuantityPicker(quantityParams: QuantityParams, isFullScreen: Boolean) {
        inlineBottomSheetEvent.postValue(
            BottomSheetArgDataAndTag(
                data = CustomBottomSheetArgData(
                    dialogType = BottomSheetType.QuantityPicker,
                    title = StringIdHelper.Raw(""),
                    customDataParcel = quantityParams,
                    positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
                    peekHeight = if (isFullScreen) R.dimen.expanded_bottomsheet_peek_height else R.dimen.default_bottomsheet_peek_height
                ),
                tag = QUANTITY_PICKER_MANUAL_ENTRY
            )
        )
    }

    private fun showConfirmAmountFragment(item: ItemActivityDto) {
        inlineBottomSheetEvent.postValue(getConfirmAmountArgDataAndTagForBottomSheet(item))
    }

    fun navigateUp() = _navigationEvent.postValue(NavigationEvent.Up)

    private fun getItemImage(itemActivityDto: ItemActivityDto?, itemDetailDto: ItemDetailDto?) = itemActivityDto?.imageUrl ?: itemDetailDto?.imageUrl
    private fun getItemId(itemActivityDto: ItemActivityDto?, itemDetailDto: ItemDetailDto?) = itemActivityDto?.itemId ?: itemDetailDto?.itemId
    private fun getItemDesc(itemActivityDto: ItemActivityDto?, itemDetailDto: ItemDetailDto?) = itemActivityDto?.itemDescription ?: itemDetailDto?.itemDesc

    companion object {
        const val MANUAL_ENTRY_PICK_RESULTS = "manualEntryPickResults"
        const val BARCODE_TYPE = "barcodeType"
        const val MANUAL_ENTRY_PICK = "manualEntryPick"
        const val MANUAL_ENTRY_SUBSTITUTION = "manualEntrySubstitution"
        const val QUANTITY_PICKER_MANUAL_ENTRY = "quantityPickerManualEntry"
        const val BYPASS_QUANTITY_PICKER = "byPassQuantityPicker"
    }
}

// /////////////////////////////////////////////////////////////////////////
// UI models
// /////////////////////////////////////////////////////////////////////////
enum class ValidationError(@StringRes val stringId: Int?) {
    NONE(null),
    UPC_VALIDATION(R.string.manual_upc_error),
    PLU_VALIDATION(R.string.manual_plu_error),
}

enum class WeightValidationError(@StringRes val stringId: Int?) {
    NONE(null),
    WEIGHT_VALIDATION(R.string.manual_weight_error),
    MAX_WEIGHT_VALIDATION(R.string.error_message_entered_weight_too_heavy),
}

data class ManualEntryPickResults(
    val quantity: FulfilledQuantityResult = FulfilledQuantityResult.DefaultQuantity,
    val barcode: BarcodeType? = null,
    val weightEntry: String? = null,
    val itemDetails: ItemDetailDto? = null,
) : Serializable
