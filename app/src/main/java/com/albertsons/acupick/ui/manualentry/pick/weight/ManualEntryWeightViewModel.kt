package com.albertsons.acupick.ui.manualentry.pick.weight

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.getWeightedItemMaxWeight
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.albertsons.acupick.infrastructure.utils.orZero
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.manualentry.ManualEntryWeightUi
import com.albertsons.acupick.ui.manualentry.pick.BY_EACH_PLU_0
import com.albertsons.acupick.ui.manualentry.pick.MAX_WEIGHT_LB
import com.albertsons.acupick.ui.manualentry.pick.MIN_WEIGHT_LB
import com.albertsons.acupick.ui.manualentry.pick.ValidationError
import com.albertsons.acupick.ui.manualentry.pick.WeightValidationError
import kotlinx.coroutines.flow.combine
import timber.log.Timber

class ManualEntryWeightViewModel(val app: Application) : BaseViewModel(app) {

    val manualEntryWeightUI = MutableLiveData<ManualEntryWeightUi>()
    val weightPluEntryText = MutableLiveData("")
    val weightEntryText = MutableLiveData("")

    val pickListItem = MutableLiveData<ItemActivityDto?>()
    val validationError: LiveData<ValidationError> = MutableLiveData(ValidationError.NONE)
    val weightError: LiveData<WeightValidationError> = MutableLiveData(WeightValidationError.NONE)

    val pluTextInputError = validationError.map { it.stringId }
    val weightTextInputError = weightError.map { it.stringId }

    val quantity = MutableLiveData<Int>()
    val continueEnabled = combine(weightPluEntryText.asFlow(), weightEntryText.asFlow()) { plu, weight ->
        plu.length in 4..5 && isPluValid(plu) && isWeightValid(weight)
    }.asLiveData()
    // Events
    val closeKeyboard: LiveData<Unit> = MutableLiveData()

    fun validatePluTextEntry() {
        val pluEntered = weightPluEntryText.value.orEmpty()
        Timber.v("[validatePlu] plu entered=$pluEntered")

        val error = if (isPluValid(pluEntered) || pluEntered.isEmpty()) ValidationError.NONE else ValidationError.PLU_VALIDATION
        // Only set value if it's different from the current one--to prevent UI jankiness
        if (validationError.value != error) {
            validationError.set(error)
        }
    }

    fun validateWeight() {
        val weightEntered = weightEntryText.value.orEmpty()
        Timber.v("[validateWeight] weight entered=$weightEntered")
        val error = when {
            !isWeightValid(weightEntered) && weightEntered.isNotEmpty() -> WeightValidationError.WEIGHT_VALIDATION
            isMaxWeightValidationRequired() && validateMaxWeight(weightEntered) -> WeightValidationError.MAX_WEIGHT_VALIDATION
            else -> WeightValidationError.NONE
        }
        if (weightError.value != error) {
            weightError.postValue(error)
        }
    }

    private fun isPluValid(plu: String): Boolean {
        val item = pickListItem.value
        val isEachWithPlu0 = item?.sellByWeightInd == SellByType.Each && item.pluList?.contains(BY_EACH_PLU_0) == true

        return when (manualEntryWeightUI.value?.isSubstitution == true || manualEntryWeightUI.value?.isIssueScanning == true || isEachWithPlu0) {
            true -> plu.isNotNullOrBlank() && plu.all { it.isDigit() }
            false -> pickListItem.value?.pluList?.contains(plu) == true
        }
    }

    private fun isWeightValid(weight: String): Boolean =
        (weight.toDoubleOrNull() != null) &&
            (weight.toDouble() >= MIN_WEIGHT_LB) &&
            (weight.toDouble() <= MAX_WEIGHT_LB)

    fun validateMaxWeight(weight: String): Boolean {
        return (weight.toDoubleOrNull() != null) && (weight.toDouble() > pickListItem.value?.getWeightedItemMaxWeight().orZero())
    }

    fun isMaxWeightValidationRequired(): Boolean = (manualEntryWeightUI.value?.isFromPicking == true) && (pickListItem.value?.isOrderedByWeight() == false)

    companion object {
        const val MAX_WEIGHT_ERROR_RESULTS = "maxWeightErrorResult"
        const val MAX_WEIGHT_ERROR_REQUEST_KEY = "maxWeightErrorRequestKey"
    }
}
