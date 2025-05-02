package com.albertsons.acupick.ui.manualentry.handoff

import android.app.Application
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.manualentry.ManualEntryStagingUi
import com.albertsons.acupick.ui.util.combineWith
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.transform
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.Locale

const val MAX_MANUAL_ENTRY_QUANTITY = 99
const val MAX_ENTRY_LENGTH_ZONES = 5

class ManualEntryStagingViewModel(app: Application) : ManualEntryBaseViewModel(app) {

    // State
    private lateinit var bagBarcode: String
    private lateinit var boxBarcode: String
    private val zoneError: LiveData<ZonesValidationError> = MutableLiveData(ZonesValidationError.NONE)
    private val bagsOrLooseError: LiveData<BagsValidationError> = MutableLiveData(BagsValidationError.NONE)
    private val toteError: LiveData<ToteIdValidationError> = MutableLiveData(ToteIdValidationError.NONE)

    // UI
    val manualEntryStagingUI = MutableLiveData<ManualEntryStagingUi>()
    val zoneEntryTypedText = MutableLiveData<String>()
    private val zoneEntryText = zoneEntryTypedText.map { it.uppercase() }
    val bagsOrLooseEntryText = MutableLiveData<String>()
    val toteEntryTypedText = MutableLiveData<String>()
    private val toteEntryText = toteEntryTypedText.map { it.uppercase() }
    val isBoxEntry = manualEntryStagingUI.transform { it?.boxList.isNotNullOrEmpty() }
    val zoneTextInputError: LiveData<Int?> = zoneError.map { it.stringId }
    val bagTextInputError: LiveData<Int?> = bagsOrLooseError.map { it.stringId }
    val toteTextInputError: LiveData<Int?> = toteError.map { it.stringId }
    val returnManualEntryStagingDataEvent = LiveEvent<ManualEntryStagingData>()
    val isCustomerPreferBag = manualEntryStagingUI.map { it.isCustomerPreferBag }
    val bagsOrLooseLabelText = manualEntryStagingUI.map {
        app.getString(if (it.isCustomerPreferBag) R.string.bag_number else R.string.manual_entry_loose_item)
    }
    val isToteEntry = bagsOrLooseEntryText.combineWith(toteEntryText) { loose, tote ->
        (loose.isNullOrBlank() && tote.isNullOrBlank()) || (loose.isNullOrBlank() && tote.isNotNullOrBlank())
    }
    val isLooseEntry = bagsOrLooseEntryText.combineWith(toteEntryText) { loose, tote ->
        (loose.isNullOrBlank() && tote.isNullOrBlank()) || (loose.isNotNullOrBlank() && tote.isNullOrBlank())
    }

    private fun getBoxLabel(boxNumber: String) = manualEntryStagingUI.value?.boxList?.firstOrNull { it.boxNumber == boxNumber }?.label ?: ""

    override val continueEnabled =
        combine(
            zoneEntryText.asFlow().filterNotNull(),
            orderNumberEntryText.asFlow().filterNotNull(),
            bagsOrLooseEntryText.asFlow().filterNotNull(),
            toteEntryText.asFlow().filterNotNull()
        ) { zones, orderNumber, bagsOrTote, tote ->
            zones.length == MAX_ENTRY_LENGTH_ZONES && orderNumber.isNotNullOrBlank() && (bagsOrTote.isNotNullOrBlank() || tote.isNotNullOrBlank())
        }.asLiveData()

    init {
        viewModelScope.launch {
            manualEntryStagingUI.asFlow().collect {
                it.defaultValue?.let { zone -> zoneEntryTypedText.set(zone) }
            }
        }
        viewModelScope.launch {
            manualEntryStagingUI.asFlow().collect {
                it.customerOrderNumber?.let { orderNumber -> orderNumberEntryText.value = orderNumber }
                it.shortOrderId?.let { orderId -> shortOrderId.value = orderId }
                it.customerName?.let { name -> customerName.value = name }
            }
        }
        viewModelScope.launch {
            zoneEntryText.asFlow().collect {
                clearZoneError()
            }
        }
        viewModelScope.launch {
            bagsOrLooseEntryText.asFlow().collect {
                clearBagError()
            }
        }
        viewModelScope.launch {
            toteEntryText.asFlow().collect {
                clearToteError()
            }
        }
    }

    fun update(param: ManualEntryStagingUi) {
        if (param.isWineOrder) {
            toteEntryTypedText.value = ""
        }
        manualEntryStagingUI.value = param
    }

    override fun navigateBackWithResults() {
        val isNoBagOrLooseError = bagsOrLooseError.value == BagsValidationError.NONE
        val isNoZoneError = zoneError.value == ZonesValidationError.NONE
        val isNoOrderNumError = orderNumberError.value == OrderNumberValidationError.NONE
        val isNoToteError = toteError.value == ToteIdValidationError.NONE

        if ((isNoBagOrLooseError || isNoToteError) && isNoZoneError && isNoOrderNumError) {
            returnManualEntryStagingDataEvent.postValue(getCompleteEventForZoneAndBag())
            _navigationEvent.postValue(NavigationEvent.Up)
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Validation
    // /////////////////////////////////////////////////////////////////////////
    override fun validateEntries() {
        val customerOrderNumber = orderNumberEntryText.value.orEmpty()
        val bagNumber = bagsOrLooseEntryText.value.orEmpty().padStart(2, '0')
        val bagCountWithActNum = "${manualEntryStagingUI.value?.activityId}$bagNumber"
        val boxNumber = bagsOrLooseEntryText.value.orEmpty()

        val toteId = "TTA00"
        bagBarcode = if (isLooseEntry.value == true) "$customerOrderNumber-$toteId-$bagCountWithActNum" else "$customerOrderNumber-${toteEntryText.value.orEmpty()}"
        boxBarcode = getBoxLabel(boxNumber)

        if (manualEntryStagingUI.value?.isWineOrder.orFalse()) {
            validateBoxNumber()
        } else {
            if (isToteEntry.value == true) validateToteId() else validateBagOrLooseNumber()
        }

        validateZone()
        validateOrderNumber()
    }

    private fun validateBagOrLooseNumber() {
        val bagNumber = bagsOrLooseEntryText.value.orEmpty().padStart(2, '0')
        val newBagNumberError = try {
            if (bagNumber.all { it.isDigit() } && bagNumber.toInt() in 1..99) {
                BagsValidationError.NONE
            } else if (manualEntryStagingUI.value?.isCustomerPreferBag == true) {
                BagsValidationError.BAG_VALIDATION
            } else {
                BagsValidationError.LOOSE_VALIDATION
            }
        } catch (e: NumberFormatException) {
            if (manualEntryStagingUI.value?.isCustomerPreferBag == true) {
                BagsValidationError.BAG_VALIDATION
            } else {
                BagsValidationError.LOOSE_VALIDATION
            }
        }

        if (bagsOrLooseError.value != newBagNumberError) {
            bagsOrLooseError.set(newBagNumberError)
        }
    }

    private fun validateToteId() {
        val toteId = toteEntryText.value.orEmpty()
        val newToteIdError = if (toteId.length == MAX_NON_MFC_TOTE_ID_LENGTH) {
            ToteIdValidationError.NONE
        } else {
            ToteIdValidationError.TOTE_ID_VALIDATION
        }
        if (toteError.value != newToteIdError) {
            toteError.set(newToteIdError)
        }
    }

    private fun validateBoxNumber() {
        val boxNumber = bagsOrLooseEntryText.value.orEmpty()
        val newBagNumberError = if (boxNumber.all { it.isDigit() }) {
            BagsValidationError.NONE
        } else {
            BagsValidationError.BOX_VALIDATION
        }

        if (bagsOrLooseError.value != newBagNumberError) {
            bagsOrLooseError.set(newBagNumberError)
        }
    }

    private fun validateZone() {
        val zone = zoneEntryText.value.orEmpty()
        val newZoneError = when {
            zone.isNotEmpty() -> ZonesValidationError.NONE
            isZoneValid(zone) -> ZonesValidationError.NONE
            else -> ZonesValidationError.ZONE_VALIDATION
        }
        if (zoneError.value != newZoneError) {
            zoneError.set(newZoneError)
        }
    }

    override fun isValidOrderNumber(orderNumber: String): Boolean {
        return manualEntryStagingUI.value?.customerOrderNumber == orderNumber
    }

    private fun isZoneValid(zone: String): Boolean {
        return barcodeMapper.inferBarcodeType(zone, enableLogging = true) is BarcodeType.Zone
    }

    // /////////////////////////////////////////////////////////////////////////
    // Helpers
    // /////////////////////////////////////////////////////////////////////////
    private fun getCompleteEventForZoneAndBag(): ManualEntryStagingData {
        val zone = barcodeMapper.inferBarcodeType(zoneEntryText.value.orEmpty().uppercase(Locale.getDefault()), enableLogging = true)
        val barcode = if (isBoxEntry.value.orFalse()) boxBarcode else bagBarcode
        val bag = if (bagsOrLooseEntryText.value?.isNotNullOrEmpty() == true || toteEntryText.value?.isNotNullOrEmpty() == true) {
            barcodeMapper.inferBarcodeType(barcode, enableLogging = true)
        } else {
            null
        }
        return ManualEntryStagingData(
            zone = zone,
            stagingContainer = bag
        )
    }

    private fun clearZoneError() = zoneError.postValue(ZonesValidationError.NONE)
    private fun clearBagError() = bagsOrLooseError.postValue(BagsValidationError.NONE)
    private fun clearToteError() = toteError.postValue(ToteIdValidationError.NONE)

    companion object {
        const val MANUAL_ENTRY_STAGING_RESULTS = "manualEntryStagingResults"
        const val MANUAL_ENTRY_STAGING = "manualEntryStaging"
        const val MANUAL_ENTRY_STAGING_REQUEST_KEY = "manualEntryStagingRequestKey"
        private const val MAX_NON_MFC_TOTE_ID_LENGTH = 5
    }
}

@Parcelize
data class ManualEntryStagingData(
    val zone: BarcodeType? = null,
    val stagingContainer: BarcodeType? = null, // This could be a bag or an MFCTote
) : Parcelable
