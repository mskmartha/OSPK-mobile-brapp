package com.albertsons.acupick.ui.manualentry.pick.upc

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.manualentry.ManualEntryUpcUi
import com.albertsons.acupick.ui.manualentry.pick.ValidationError
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject
import timber.log.Timber

class ManualEntryUpcViewModel(val app: Application) : BaseViewModel(app) {

    // DI
    val pickRepository: PickRepository by inject()
    val barcodeMapper: BarcodeMapper by inject()

    val pickListItem = MutableLiveData<ItemActivityDto?>()
    val manualEntryUPCUI = MutableLiveData<ManualEntryUpcUi>()
    val upcEntryText = MutableLiveData("")

    val validationError: LiveData<ValidationError> = MutableLiveData(ValidationError.NONE)
    val upcTextInputError: LiveData<Int?> = validationError.map { it.stringId }

    val continueEnabled = upcEntryText.map { it.length == 8 || it.length in 12..13 }

    val quantity = MutableLiveData<Int>()
    // Events
    val closeKeyboard: LiveData<Unit> = MutableLiveData()

    fun validateUpcTextEntry() {
        val upcEntered = upcEntryText.value.orEmpty()
        Timber.v("[validateUpc] upc entered=$upcEntered")

        val error = if (isUpcValid(upcEntered)) ValidationError.NONE else ValidationError.UPC_VALIDATION

        // Only set value if it's different from the current one--to prevent UI jankiness
        if (validationError.value != error) {
            validationError.set(error)
        }
    }

    private fun isUpcValid(upc: String) =
        // False if UPC is empty
        upc.isNotNullOrEmpty() && (
            // True is substitution
            manualEntryUPCUI.value?.isSubstitution == true || manualEntryUPCUI.value?.isIssueScanning == true ||
                // Otherwise, check picklist for matching UPC and verify match has same ID.
                runBlocking {
                    pickRepository.getItemId(barcodeMapper.inferBarcodeType(upc, enableLogging = true) as? BarcodeType.Item)
                }?.let { pickListItem.value?.itemId == it } ?: false
            )

    fun clearUpcError() = validationError.postValue(ValidationError.NONE)

    fun noActiveUpcErrors() = validationError.value == ValidationError.NONE
}
