package com.albertsons.acupick.ui.manualentry.handoff

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.core.component.inject

/**
 * A base class from which view model classes for manual entry screens that accepts the customer order number can extend
 */
abstract class ManualEntryBaseViewModel(app: Application) : BaseViewModel(app) {

    // DI
    val barcodeMapper: BarcodeMapper by inject()

    // State
    protected val orderNumberError: LiveData<OrderNumberValidationError> = MutableLiveData(OrderNumberValidationError.NONE)

    // UI
    val orderNumberEntryText = MutableLiveData<String>()
    val shortOrderId = MutableLiveData<String>()
    val customerName = MutableLiveData<String>()
    val orderNumberTextInputError: LiveData<Int?> = orderNumberError.map { it.stringId }
    abstract val continueEnabled: LiveData<Boolean>

    // Events
    private val closeKeyboard: LiveData<Unit> = LiveEvent()

    init {
        viewModelScope.launch {
            // FIXME - this could be source of error when third entry is used for bags?
            //  Secondary could be customer order number?
            orderNumberEntryText.asFlow().collect {
                clearOrderNumberError()
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // UI Callbacks
    // /////////////////////////////////////////////////////////////////////////
    fun onContinueButtonClicked() {
        // continueEnabled.set(false)
        viewModelScope.launch {
            validateEntries()
            closeKeyboard.postValue(Unit)
            navigateBackWithResults()
        }
    }

    /**
     * This function should use [NavigationEvent.Back] to return the results to the caller.
     */
    abstract fun navigateBackWithResults()

    // /////////////////////////////////////////////////////////////////////////
    // Validation
    // /////////////////////////////////////////////////////////////////////////
    /**
     * This function should validate all the inputs and set the values for the corresponding validation error LiveData.
     * An example is [orderNumberError] and [OrderNumberValidationError].
     */
    abstract fun validateEntries()

    protected fun validateOrderNumber() {
        val customerOrderNumber = orderNumberEntryText.value.orEmpty()
        val newOrderNumberError = if (isValidOrderNumber(customerOrderNumber) || customerOrderNumber.isEmpty()) {
            OrderNumberValidationError.NONE
        } else {
            OrderNumberValidationError.ORDER_NUMBER_VALIDATION
        }
        if (orderNumberError.value != newOrderNumberError) {
            orderNumberError.set(newOrderNumberError)
        }
    }

    /**
     * This function should return whether the customer order number is valid
     */
    abstract fun isValidOrderNumber(orderNumber: String): Boolean

    // /////////////////////////////////////////////////////////////////////////
    // Helpers
    // /////////////////////////////////////////////////////////////////////////
    private fun clearOrderNumberError() = orderNumberError.postValue(OrderNumberValidationError.NONE)

    // /////////////////////////////////////////////////////////////////////////
    // UI models
    // /////////////////////////////////////////////////////////////////////////
    enum class OrderNumberValidationError(@StringRes val stringId: Int?) {
        NONE(null),
        ORDER_NUMBER_VALIDATION(R.string.manual_order_num_error)
    }

    enum class ToteIdValidationError(@StringRes val stringId: Int?) {
        NONE(null),
        TOTE_ID_VALIDATION(R.string.manual_handoff_tote_id_error),
    }

    companion object {
        const val MANUAL_ENTRY_HANDOFF_RESULTS = "manualEntryHandoffResults"
        const val MANUAL_ENTRY_HANDOFF = "manualEntryHandoff"
        const val MANUAL_ENTRY_STORAGE_TYPE = "manualEntryStorageType"
    }
}
