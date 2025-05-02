package com.albertsons.acupick.ui.manualentry.handoff

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.manualentry.ManualEntryHandOffUi
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.orTrue
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.io.Serializable

class ManualEntryHandOffMfcViewModel(app: Application) : ManualEntryBaseViewModel(app) {

    // State
    private val toteIdError: LiveData<ToteIdValidationError> = MutableLiveData(ToteIdValidationError.NONE)

    // UI
    val manualEntryHandOffUI = MutableLiveData<ManualEntryHandOffUi>()
    val toteIdEntryText = MutableLiveData<String>()
    val toteIdTextInputError: LiveData<Int?> = toteIdError.map { it.stringId }
    private var currentStorageTypes: List<StorageType?>? = null
    private var currentToteId = ""
    var toteIdsByStorageType: List<Pair<StorageType?, String?>>? = listOf()
    val isCustomerBagPreference = MutableLiveData<Boolean>()
    val inlineStorageTypeDialogEvent: LiveData<CustomDialogArgDataAndTag> = LiveEvent()
    val manualEntryTooltipEvent = MutableSharedFlow<Unit>()

    override val continueEnabled =
        combine(orderNumberEntryText.asFlow().filterNotNull(), toteIdEntryText.asFlow().filterNotNull()) { _, toteInput ->
            toteInput.length == MAX_MFC_TOTE_ID_LENGTH || toteInput.length == MAX_RESHOP_TOTE_ID_LENGTH
        }.asLiveData()

    val returnMfcToteDataEvent = LiveEvent<ManualEntryHandOffBag>()

    override fun navigateBackWithResults() {
        val toteId = toteIdEntryText.value.orEmpty()
        val customerOrderNumber = orderNumberEntryText.value.orEmpty()
        // The first 6 digits are not entered, so we will fill them in with 0's for now.  The comparison
        // with containerId in DestageOrderPagerViewModel will ignore these 6 digits.

        val totesFound = manualEntryHandOffUI.value?.bagLabels?.filter { label ->
            (label.labelId?.endsWith(toteId) == true) && !label.isScanned
        }
        currentStorageTypes = totesFound?.map { it.zoneType } ?: emptyList()

        when (totesFound?.size) {
            0 -> {
                currentToteId = if (toteId.length == MAX_RESHOP_TOTE_ID_LENGTH) {
                    "$customerOrderNumber-$toteId"
                } else {
                    "000000$toteId-$customerOrderNumber"
                }
                sendManualDataUp()
            }

            1 -> {
                currentToteId = if (toteId.length == MAX_RESHOP_TOTE_ID_LENGTH) {
                    "$customerOrderNumber-$toteId"
                } else {
                    "${totesFound.firstOrNull()?.labelId.orEmpty()}-$customerOrderNumber"
                }
                sendManualDataUp()
            }

            else -> {
                toteIdsByStorageType = totesFound?.map { totes -> Pair(totes.zoneType, "${totes.labelId.orEmpty()}-$customerOrderNumber") }
                showChooseStorageTypeDialog()
            }
        }
    }

    private fun sendManualDataUp() {
        val isNoToteIdError = toteIdError.value == ToteIdValidationError.NONE
        val isNoOrderNumError = orderNumberError.value == OrderNumberValidationError.NONE

        val barcodeType = barcodeMapper.inferBarcodeType(currentToteId, enableLogging = true)

        if (isNoToteIdError && isNoOrderNumError) {
            returnMfcToteDataEvent.postValue(ManualEntryHandOffBag(barcodeType))
            _navigationEvent.postValue(NavigationEvent.Up)
        }
    }

    private fun showChooseStorageTypeDialog() {
        val storageTypeStrings = currentStorageTypes?.map {
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
                    title = StringIdHelper.Id(R.string.associated_mfc_zone_title),
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
        //             title = StringIdHelper.Id(R.string.associated_mfc_zone_title),
        //             customData = storageTypeStrings as Serializable,
        //             positiveButtonText = StringIdHelper.Id(R.string.confirm),
        //             negativeButtonText = StringIdHelper.Id(R.string.cancel),
        //         ),
        //         tag = MANUAL_ENTRY_STORAGE_TYPE
        //     )
        // )
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
        viewModelScope.launch {
            // FIXME - this could be source of error when third entry is used for bags?
            //  Secondary could be customer order number?
            toteIdEntryText.asFlow().collect {
                clearToteIdError()
            }
        }
        // registerCloseAction(MANUAL_ENTRY_STORAGE_TYPE) {
        //     closeActionFactory(
        //         positive = { selection ->
        //             selection?.let { type ->
        //                 currentToteId = toteIdsByStorageType?.find { totePair ->
        //                     totePair.first == currentStorageTypes?.get(type)
        //                 }?.second.orEmpty()
        //                 sendManualDataUp()
        //             }
        //         },
        //         negative = { continueEnabled.postValue(true) },
        //         dismiss = { continueEnabled.postValue(true) }
        //     )
        // }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Validation
    // /////////////////////////////////////////////////////////////////////////
    override fun validateEntries() {
        validateOrderNumber()
        validateToteId()
    }
    fun navigateToToolTip() = viewModelScope.launch {
        manualEntryTooltipEvent.emit(Unit)
    }

    private fun validateToteId() {
        val toteId = toteIdEntryText.value.orEmpty()
        val newToteIdError = if (toteId.length == MAX_MFC_TOTE_ID_LENGTH || toteId.length == MAX_RESHOP_TOTE_ID_LENGTH) {
            ToteIdValidationError.NONE
        } else {
            ToteIdValidationError.TOTE_ID_VALIDATION
        }
        if (toteIdError.value != newToteIdError) {
            toteIdError.set(newToteIdError)
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Helpers
    // /////////////////////////////////////////////////////////////////////////
    override fun isValidOrderNumber(orderNumber: String): Boolean {
        return manualEntryHandOffUI.value?.customerOrderNumber == orderNumber
    }

    private fun clearToteIdError() = toteIdError.postValue(ToteIdValidationError.NONE)

    fun sendSelection(selection: Int?) {
        selection?.let { type ->
            currentToteId = toteIdsByStorageType?.find { totePair ->
                totePair.first == currentStorageTypes?.get(type)
            }?.second.orEmpty()
            sendManualDataUp()
        }
    }

    companion object {
        private const val MAX_RESHOP_TOTE_ID_LENGTH = 6
        private const val MAX_MFC_TOTE_ID_LENGTH = 8
        const val MANUAL_ENTRY_TOOL_TIP_TAG_REQUEST_KEY = "missing_manual_entry_tool_tip_key"
    }
}
