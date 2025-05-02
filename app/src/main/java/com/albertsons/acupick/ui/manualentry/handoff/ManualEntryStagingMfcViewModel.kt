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
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.manualentry.ManualEntryStagingUi
import com.albertsons.acupick.ui.staging.StagingPart2PagerViewModel.Companion.MAX_MFC_TOTE_ID_LENGTH
import com.albertsons.acupick.ui.util.StringIdHelper
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.io.Serializable

class ManualEntryStagingMfcViewModel(app: Application) : ManualEntryBaseViewModel(app) {

    // State
    private val zoneError: LiveData<ZonesValidationError> = MutableLiveData(ZonesValidationError.NONE) // State
    private val toteIdError: LiveData<ToteIdValidationError> = MutableLiveData(ToteIdValidationError.NONE)

    // UI
    val manualEntryStagingUI = MutableLiveData<ManualEntryStagingUi>()
    val zoneEntryText = MutableLiveData<String>()
    val toteIdEntryText = MutableLiveData<String>()
    val zoneTextInputError: LiveData<Int?> = zoneError.map { it.stringId }
    val toteIdTextInputError: LiveData<Int?> = toteIdError.map { it.stringId }

    private var currentStorageTypes: List<StorageType?>? = null
    private var currentToteId = ""
    var toteIdsByStorageType: List<Pair<StorageType?, String?>>? = listOf()

    override val continueEnabled =
        combine(orderNumberEntryText.asFlow().filterNotNull(), toteIdEntryText.asFlow().filterNotNull(), zoneEntryText.asFlow()) { _, toteInput, zone ->
            toteInput.length == MAX_MFC_TOTE_ID_LENGTH && zone.isNotNullOrEmpty()
        }.asLiveData()

    val returnMfcToteDataEvent = LiveEvent<ManualEntryStagingData>()

    override fun navigateBackWithResults() {
        val toteId = toteIdEntryText.value.orEmpty()
        val customerOrderNumber = orderNumberEntryText.value.orEmpty()

        val totesFound = manualEntryStagingUI.value?.scannedBagUiList?.filter { label -> (label.bagId.endsWith(toteId)) }

        currentStorageTypes = totesFound?.map { it.zoneType } ?: emptyList()

        when (totesFound?.size) {
            0 -> {
                currentToteId = "000000$toteId-$customerOrderNumber"
                sendManualDataUp()
            }
            1 -> {
                currentToteId = "${totesFound.firstOrNull()?.bagId.orEmpty()}-$customerOrderNumber"
                sendManualDataUp()
            }
            else -> {
                toteIdsByStorageType = totesFound?.map { totes -> Pair(totes.zoneType, "${totes.bagId.orEmpty()}-$customerOrderNumber") }
                showChooseStorageTypeDialog()
            }
        }
    }

    private fun sendManualDataUp() {
        val isNoToteIdError = toteIdError.value == ToteIdValidationError.NONE
        val isNoOrderNumError = orderNumberError.value == OrderNumberValidationError.NONE

        val zone = barcodeMapper.inferBarcodeType(zoneEntryText.value.orEmpty(), enableLogging = true)
        val tote = barcodeMapper.inferBarcodeType(currentToteId, enableLogging = true)

        if (isNoToteIdError && isNoOrderNumError) {
            returnMfcToteDataEvent.postValue(
                ManualEntryStagingData(
                    zone = zone,
                    stagingContainer = tote
                )
            )
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

        inlineDialogEvent.postValue(
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
    }

    init {
        viewModelScope.launch {
            manualEntryStagingUI.asFlow().collect {
                it.customerOrderNumber?.let { orderNumber -> orderNumberEntryText.value = orderNumber }
            }
        }
        viewModelScope.launch {
            // FIXME - this could be source of error when third entry is used for bags?
            //  Secondary could be customer order number?
            toteIdEntryText.asFlow().collect {
                clearToteIdError()
            }
        }
        registerCloseAction(MANUAL_ENTRY_STORAGE_TYPE) {
            closeActionFactory(
                positive = { selection ->
                    selection?.let { type ->
                        currentToteId = toteIdsByStorageType?.find { totePair ->
                            totePair.first == currentStorageTypes?.get(type)
                        }?.second.orEmpty()
                        sendManualDataUp()
                    }
                },
                negative = { continueEnabled.postValue(true) },
                dismiss = { continueEnabled.postValue(true) }
            )
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Validation
    // /////////////////////////////////////////////////////////////////////////
    override fun validateEntries() {
        validateOrderNumber()
        validateToteId()
    }

    private fun validateToteId() {
        val toteId = toteIdEntryText.value.orEmpty()
        val newToteIdError = if (toteId.length == MAX_MFC_TOTE_ID_LENGTH) {
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
        return manualEntryStagingUI.value?.customerOrderNumber == orderNumber
    }

    private fun clearToteIdError() = toteIdError.postValue(ToteIdValidationError.NONE)
}
