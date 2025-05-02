package com.albertsons.acupick.ui.manualentry.pharmacy

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.arrivals.pharmacy.ManualEntryPharmacyData
import com.albertsons.acupick.ui.manualentry.ManualEntryPharmacyUi
import com.albertsons.acupick.ui.picklistitems.ScanTarget
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.transform
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.util.Locale

class ManualEntryPharmacyViewModel(app: Application) : BaseViewModel(app) {

    val barcodeMapper: BarcodeMapper by inject()

    private lateinit var collectedBarcode: String
    private val arrivalLabelError: LiveData<ArrivalLabelError> = MutableLiveData(ArrivalLabelError.NONE)
    private val returnLabelError: LiveData<ReturnLabelError> = MutableLiveData(ReturnLabelError.NONE)
    private val bagsError: LiveData<BagError> = MutableLiveData(BagError.NONE)

    val data = MutableStateFlow<ManualEntryPharmacyUi?>(null)
    val scanTarget = data.map { data.value?.scanTarget ?: ScanTarget.PharmacyArrivalLabel }.asLiveData()
    val manualEntryData = MutableLiveData("")
    val continueEnabled = manualEntryData.transform { manualEntryData ->
        manualEntryData.isNotNullOrBlank()
    }
    val hint = scanTarget.transform {
        when (it) {
            ScanTarget.PharmacyArrivalLabel -> app.getString(R.string.pharmacy_location_label_no)
            ScanTarget.PharmacyReturnLabel -> app.getString(R.string.pharmacy_return_label_no)
            ScanTarget.Bag -> app.getString(R.string.pharmacy_rx_bag_label_no)
            else -> ""
        }
    }

    val error = MutableLiveData(R.string.empty)
    val returnManualEntryDataEvent = LiveEvent<ManualEntryPharmacyData>()

    fun update(manualEntryPharmacyData: ManualEntryPharmacyUi?) {
        data.value = manualEntryPharmacyData
    }

    fun onContinueButtonClicked() {
        continueEnabled.set(false)
        viewModelScope.launch {
            validateEntries()
            navigateBackWithResults()
        }
    }

    fun onTexChanged(comment: String) {
        viewModelScope.launch {
            manualEntryData.postValue(comment)
        }
    }

    fun validateEntries() {
        when (scanTarget.value) {
            ScanTarget.PharmacyArrivalLabel -> {
                collectedBarcode = manualEntryData.value?.trim() ?: ""
                validateArrivalLabel()
            }
            ScanTarget.PharmacyReturnLabel -> {
                collectedBarcode = manualEntryData.value?.trim() ?: ""
                validateReturnLabel()
            }
            ScanTarget.Bag -> {
                val customerOrderNumber = data.value?.orderNumber
                val bagNumber = manualEntryData.value?.trim()
                collectedBarcode = bagNumber ?: ""
                validateBagNumber()
            }

            else -> {}
        }
    }

    private fun validateBagNumber() {
        val bagNumber = manualEntryData.value?.trim() ?: ""
        val newBagNumberError = if (bagNumber.length == 15) {
            BagError.NONE
        } else {
            BagError.BAG_ERROR
        }

        if (bagsError.value != newBagNumberError) {
            bagsError.set(newBagNumberError)
            error.value = newBagNumberError.stringId
        }
    }

    private fun validateArrivalLabel() {
        val label = manualEntryData.value?.trim() ?: ""
        val newError = if (label.length == 5) {
            ArrivalLabelError.NONE
        } else {
            ArrivalLabelError.ARRIVAL_LABEL_ERROR
        }

        if (arrivalLabelError.value != newError) {
            arrivalLabelError.set(newError)
            error.value = newError.stringId
        }
    }

    private fun validateReturnLabel() {
        val label = manualEntryData.value?.trim() ?: ""
        val newError = if (label.length == 13) {
            ReturnLabelError.NONE
        } else {
            ReturnLabelError.RETURN_LABEL_ERROR
        }

        if (returnLabelError.value != newError) {
            returnLabelError.set(newError)
            error.value = newError.stringId
        }
    }

    private fun navigateBackWithResults() {
        val isNoBagError = bagsError.value == BagError.NONE
        val isNoArrivalLabelError = arrivalLabelError.value == ArrivalLabelError.NONE
        val isNoReturnLabelError = returnLabelError.value == ReturnLabelError.NONE

        if (isNoBagError && isNoArrivalLabelError && isNoReturnLabelError) {
            returnManualEntryDataEvent.postValue(getCompleteData())
        }
    }

    private fun getCompleteData(): ManualEntryPharmacyData {
        val data = if (manualEntryData.value?.isNotNullOrEmpty().orFalse()) {
            barcodeMapper.inferBarcodeType(collectedBarcode.uppercase(Locale.getDefault()), enableLogging = true)
        } else {
            null
        }
        return ManualEntryPharmacyData(
            scanTarget = scanTarget.value ?: ScanTarget.PharmacyArrivalLabel,
            stagingContainer = data
        )
    }

    companion object {
        const val MANUAL_ENTRY_PHARMACY_RESULTS = "manualEntryPharmacyResults"
        const val MANUAL_ENTRY_PHARMACY = "manualEntryPharmacy"
    }
}

enum class ArrivalLabelError(@StringRes val stringId: Int?) {
    NONE(null),
    ARRIVAL_LABEL_ERROR(R.string.pharmacy_arrival_label_error),
}

enum class BagError(@StringRes val stringId: Int?) {
    NONE(null),
    BAG_ERROR(R.string.manual_handoff_bags_error),
}

enum class ReturnLabelError(@StringRes val stringId: Int?) {
    NONE(null),
    RETURN_LABEL_ERROR(R.string.pharmacy_return_label_error),
}
