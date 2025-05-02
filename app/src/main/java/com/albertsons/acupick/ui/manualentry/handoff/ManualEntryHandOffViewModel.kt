package com.albertsons.acupick.ui.manualentry.handoff

import android.app.Application
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.manualentry.ManualEntryHandOffUi
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.combineWith
import com.albertsons.acupick.ui.util.orTrue
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.io.Serializable

class ManualEntryHandOffViewModel(app: Application) : ManualEntryBaseViewModel(app) {

    // State
    private val bagsError: LiveData<BagsValidationError> = MutableLiveData(BagsValidationError.NONE)
    private val toteIdError: LiveData<ToteIdValidationError> = MutableLiveData(ToteIdValidationError.NONE)

    // UI
    val toteIdTextInputError: LiveData<Int?> = toteIdError.map { it.stringId }
    val toteIdCapText = MutableLiveData<String>()
    val toteIdEntryText = toteIdCapText.map { it.uppercase() }
    val manualEntryHandOffUI = MutableLiveData<ManualEntryHandOffUi>()
    val inlineStorageTypeDialogEvent: LiveData<CustomDialogArgDataAndTag> = LiveEvent()
    val isCustomerBagPreference = MutableLiveData<Boolean>()
    val bagsEntryText = MutableLiveData<String>()
    val bagTextInputError: LiveData<Int?> = bagsError.map { it.stringId }
    private var currentStorageType: List<StorageType?>? = null
    private var bagCountWithActNum = ""
    private var currentToteId = ""
    private var toteIdsByStorageType: List<Pair<StorageType?, String?>>? = listOf()
    val returnNonMfcToteDataEvent = LiveEvent<ManualEntryHandOffBag>()
    private var entredContainerType = MutableLiveData(ContainerType.BAG)

    override val continueEnabled =
        combine(orderNumberEntryText.asFlow().filterNotNull(), toteIdEntryText.asFlow().filterNotNull(), bagsEntryText.asFlow().filterNotNull()) { orderNumberInput, toteInput, bagInput ->
            // Commenting out ordernumberInput length until we get a confirmation that it is no longer needed
            (bagInput.isNotNullOrBlank() || toteInput.isNotNullOrBlank()) /*&& (orderNumberInput.length == 8)*/
        }.asLiveData()

    val returnBagDataEvent = LiveEvent<ManualEntryHandOffBag>()

    val isToteEntry = bagsEntryText.combineWith(toteIdCapText) { loose, tote ->
        (loose.isNullOrBlank() && tote.isNullOrBlank()) || (loose.isNullOrBlank() && tote.isNotNullOrBlank())
    }
    val isLooseEntry = bagsEntryText.combineWith(toteIdCapText) { loose, tote ->
        (loose.isNullOrBlank() && tote.isNullOrBlank()) || (loose.isNotNullOrBlank() && tote.isNullOrBlank())
    }

    init {
        viewModelScope.launch {
            manualEntryHandOffUI.asFlow().collect {
                it.customerOrderNumber?.let { orderNumber -> orderNumberEntryText.value = orderNumber }
                it.shortOrderId?.let { orderId -> shortOrderId.value = orderId }
                it.customerName?.let { name -> customerName.value = name }
                isCustomerBagPreference.postValue(it.bagLabels?.any { bagLabel -> bagLabel.isCustomerBagPreference.orTrue() })
            }
        }

        // registerCloseAction(MANUAL_ENTRY_STORAGE_TYPE) {
        //     closeActionFactory(
        //         positive = { selection ->
        //             selection?.let {
        //                 when (entredContainerType.value) {
        //                     ContainerType.TOTE -> {
        //                         currentToteId = toteIdsByStorageType?.find { totePair ->
        //                             totePair.first == currentStorageType?.get(it)
        //                         }?.second.orEmpty()
        //                         sendManualNonMfcToteDataUp()
        //                     }
        //                     ContainerType.BAG, ContainerType.LOOSE_ITEM -> {
        //                         sendManualDataUp(currentStorageType?.get(it))
        //                     }
        //                     else -> Unit
        //                 }
        //             }
        //         },
        //         negative = { continueEnabled.postValue(true) },
        //         dismiss = { continueEnabled.postValue(true) }
        //     )
        // }

        viewModelScope.launch {
            bagsEntryText.asFlow().collect {
                clearBagError()
            }
        }
        viewModelScope.launch {
            toteIdEntryText.asFlow().collect {
                clearToteIdError()
            }
        }
    }

    override fun navigateBackWithResults() {
        if (toteIdEntryText.value.isNotNullOrBlank()) {
            entredContainerType.set(ContainerType.TOTE)
            val toteId = toteIdEntryText.value.orEmpty()
            val customerOrderNumber = orderNumberEntryText.value.orEmpty()
            // We are validating toteId for non loose item because it might be chance to match last 2 digit toteId with bagNumber for example (toteId=TTA20, bagNumber=20)
            val totesFound = manualEntryHandOffUI.value?.bagLabels?.filter { label ->
                (label.isLoose != true && label.labelId?.endsWith(toteId) == true) && !label.isScanned
            }
            currentStorageType = totesFound?.map { it.zoneType } ?: emptyList()

            when (totesFound?.size) {
                0 -> {
                    currentToteId = "$customerOrderNumber-$toteId"
                    sendManualNonMfcToteDataUp()
                }
                1 -> {
                    currentToteId = "$customerOrderNumber-${totesFound.firstOrNull()?.labelId.orEmpty()}"
                    sendManualNonMfcToteDataUp()
                }
                else -> {
                    toteIdsByStorageType = totesFound?.map { totes -> Pair(totes.zoneType, "$customerOrderNumber-${totes.labelId.orEmpty()}") }
                    showChooseStorageTypeDialog(totesFound?.map { it.zoneType }.orEmpty())
                }
            }
        } else {
            val bagNumber = bagsEntryText.value.orEmpty().padStart(2, '0')

            when (isCustomerBagPreference.value.orTrue()) {
                true -> entredContainerType.set(ContainerType.BAG)
                else -> entredContainerType.set(ContainerType.LOOSE_ITEM)
            }
            // We are validating bagNumber for loose item because it might be chance to match last 2 digit toteId with bagNumber for example (toteId=TTA20, bagNumber=20)
            val bagsFound = if (isCustomerBagPreference.value.orTrue()) {
                manualEntryHandOffUI.value?.bagLabels?.filter { label ->
                    (label.labelId?.endsWith(bagNumber) == true)
                }
            } else {
                manualEntryHandOffUI.value?.bagLabels?.filter { label ->
                    (label.isLoose == true && label.labelId?.endsWith(bagNumber) == true)
                }
            }

            currentStorageType = bagsFound?.map { it.zoneType } ?: emptyList()

            when (bagsFound?.size) {
                0 -> {
                    bagCountWithActNum = "${manualEntryHandOffUI.value?.activityId}$bagNumber"
                    val zoneByOrderNumber = manualEntryHandOffUI.value?.bagLabels?.find { label ->
                        label.customerOrderNumber == orderNumberEntryText.value
                    }?.zoneType
                    sendManualDataUp(zoneByOrderNumber)
                }
                1 -> {
                    bagCountWithActNum = bagsFound.firstOrNull()?.labelId ?: "${manualEntryHandOffUI.value?.activityId}$bagNumber"
                    sendManualDataUp(bagsFound.firstOrNull()?.zoneType)
                }
                else -> showChooseStorageTypeDialog(bagsFound?.map { it.zoneType }.orEmpty())
            }
        }
    }

    // Handled non-mfc toteId
    private fun sendManualNonMfcToteDataUp() {
        val isNoToteIdError = toteIdError.value == ToteIdValidationError.NONE
        val isNoOrderNumError = orderNumberError.value == OrderNumberValidationError.NONE
        acuPickLogger.d("ManualEntry destaging: Non-mfc totebarcode $currentToteId")
        val barcodeType = barcodeMapper.inferBarcodeType(currentToteId, enableLogging = true)

        if (isNoToteIdError && isNoOrderNumError) {
            returnNonMfcToteDataEvent.postValue(ManualEntryHandOffBag(barcodeType))
            _navigationEvent.postValue(NavigationEvent.Up)
        }
    }
    private fun sendManualDataUp(storageType: StorageType?) {
        val bagNumber = bagsEntryText.value.orEmpty().padStart(2, '0')
        val isNoBagError = bagsError.value == BagsValidationError.NONE
        val isNoOrderNumError = orderNumberError.value == OrderNumberValidationError.NONE

        val bag = if (isCustomerBagPreference.value.orTrue()) {
            manualEntryHandOffUI.value?.bagLabels?.find { it.zoneType == storageType && it.labelId?.endsWith(bagNumber) == true }
        } else {
            manualEntryHandOffUI.value?.bagLabels?.find { it.zoneType == storageType && it.isLoose == true && it.labelId?.endsWith(bagNumber) == true }
        }

        val customerOrderNumber = orderNumberEntryText.value.orEmpty()
        // Constructing a bag barcode from customerOrderNumber and containerId/labelId.
        // The tote ID is not verified by the app, so we can just insert a generic toteID "TTA00"
        val bagBarcode = "$customerOrderNumber-TTA00-${bag?.labelId ?: bagCountWithActNum}"
        acuPickLogger.d("ManualEntry destaging: bag/looseItem barcode $bagBarcode")
        val barcodeType = barcodeMapper.inferBarcodeType(bagBarcode, enableLogging = true)

        if (isNoBagError && isNoOrderNumError) {
            returnBagDataEvent.postValue(
                ManualEntryHandOffBag(
                    bag = barcodeType
                )
            )
            _navigationEvent.postValue(NavigationEvent.Up)
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Validation
    // /////////////////////////////////////////////////////////////////////////
    override fun validateEntries() {
        validateOrderNumber()
        if (toteIdEntryText.value.isNotNullOrBlank()) {
            validateToteId()
        } else {
            validateBagNumber()
        }
    }

    private fun validateBagNumber() {
        val bagNumber = bagsEntryText.value.orEmpty()
        val newBagNumberError = try {
            if (bagNumber.all { it.isDigit() } && bagNumber.toInt() in 1..99) {
                BagsValidationError.NONE
            } else {
                when (isCustomerBagPreference.value.orTrue()) {
                    true -> BagsValidationError.BAG_VALIDATION
                    else -> BagsValidationError.LOOSE_ITEM_VALIDATION
                }
            }
        } catch (e: NumberFormatException) {
            when (isCustomerBagPreference.value.orTrue()) {
                true -> BagsValidationError.BAG_VALIDATION
                else -> BagsValidationError.LOOSE_ITEM_VALIDATION
            }
        }
        if (bagsError.value != newBagNumberError) {
            bagsError.set(newBagNumberError)
        }
    }

    private fun validateToteId() {
        val toteId = toteIdEntryText.value.orEmpty()
        val newToteIdError = if (toteId.length == MAX_NON_MFC_TOTE_ID_LENGTH) {
            ToteIdValidationError.NONE
        } else {
            ToteIdValidationError.TOTE_ID_VALIDATION
        }
        if (toteIdError.value != newToteIdError) {
            toteIdError.set(newToteIdError)
        }
    }

    private fun validateBoxNumber() {
        val bagNumber = bagsEntryText.value.orEmpty()
        val newBagNumberError = if (bagNumber.all { it.isDigit() } && bagNumber.toInt() in 1..99) {
            BagsValidationError.NONE
        } else {
            BagsValidationError.BAG_VALIDATION
        }
        if (bagsError.value != newBagNumberError) {
            bagsError.set(newBagNumberError)
        }
    }

    override fun isValidOrderNumber(orderNumber: String): Boolean {
        return manualEntryHandOffUI.value?.customerOrderNumber == orderNumber
    }

    private fun showChooseStorageTypeDialog(storageTypes: List<StorageType?>) {
        val storageTypeStrings = storageTypes.map {
            StringIdHelper.Id(
                when (it) {
                    StorageType.AM -> R.string.storage_type_ambient
                    StorageType.CH -> R.string.storage_type_chilled
                    StorageType.FZ -> R.string.storage_type_frozen
                    else -> R.string.storage_type_hot
                }
            )
        }

        inlineStorageTypeDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.RadioButtons,
                    title = StringIdHelper.Id(R.string.associated_zone_title),
                    customData = storageTypeStrings as Serializable,
                    positiveButtonText = StringIdHelper.Id(R.string.confirm),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                ),
                tag = MANUAL_ENTRY_STORAGE_TYPE
            )
        )

        // inlineDialogEvent.postValue(
        //     CustomDialogArgDataAndTag(
        //         data = CustomDialogArgData(
        //             dialogType = DialogType.RadioButtons,
        //             title = StringIdHelper.Id(R.string.associated_zone_title),
        //             customData = storageTypeStrings as Serializable,
        //             positiveButtonText = StringIdHelper.Id(R.string.confirm),
        //             negativeButtonText = StringIdHelper.Id(R.string.cancel),
        //         ),
        //         tag = MANUAL_ENTRY_STORAGE_TYPE
        //     )
        // )
    }

    fun sendSelection(selection: Int?) {
        selection?.let {
            when (entredContainerType.value) {
                ContainerType.TOTE -> {
                    currentToteId = toteIdsByStorageType?.find { totePair ->
                        totePair.first == currentStorageType?.get(0)
                    }?.second.orEmpty()
                    sendManualNonMfcToteDataUp()
                }
                ContainerType.BAG, ContainerType.LOOSE_ITEM -> {
                    sendManualDataUp(currentStorageType?.get(0))
                }
                else -> Unit
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Helpers
    // /////////////////////////////////////////////////////////////////////////
    fun clearBagError() = bagsError.postValue(BagsValidationError.NONE)
    fun clearToteIdError() = toteIdError.postValue(ToteIdValidationError.NONE)

    companion object {
        private const val MAX_NON_MFC_TOTE_ID_LENGTH = 5
    }
}

// /////////////////////////////////////////////////////////////////////////
// UI models
// /////////////////////////////////////////////////////////////////////////
enum class BagsValidationError(@StringRes val stringId: Int?) {
    NONE(null),
    BAG_VALIDATION(R.string.manual_handoff_bags_error),
    LOOSE_ITEM_VALIDATION(R.string.manual_handoff_loose_item_error),
    BOX_VALIDATION(R.string.manual_handoff_box_error),
    LOOSE_VALIDATION(R.string.manual_handoff_loose_item_error)
}

enum class ZonesValidationError(@StringRes val stringId: Int?) {
    NONE(null),
    ZONE_VALIDATION(R.string.manual_handoff_zone_error),
}

@Parcelize
data class ManualEntryHandOffBag(
    val bag: BarcodeType? = null,
) : Parcelable
