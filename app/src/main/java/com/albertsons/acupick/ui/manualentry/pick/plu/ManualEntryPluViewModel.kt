package com.albertsons.acupick.ui.manualentry.pick.plu

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.manualentry.ManualEntryPluUi
import com.albertsons.acupick.ui.manualentry.pick.BY_EACH_PLU_0
import com.albertsons.acupick.ui.manualentry.pick.ValidationError
import kotlinx.coroutines.flow.combine
import timber.log.Timber

class ManualEntryPluViewModel(val app: Application) : BaseViewModel(app) {

    val pickListItem = MutableLiveData<ItemActivityDto?>()
    val manualEntryPLUUI = MutableLiveData<ManualEntryPluUi>()

    val pluEntryText = MutableLiveData("")
    val validationError: LiveData<ValidationError> = MutableLiveData(ValidationError.NONE)
    val pluTextInputError: LiveData<Int?> = validationError.map { it.stringId }

    val quantity = MutableLiveData<Int>()
    val isPluEntryEditable: LiveData<Boolean> = combine(manualEntryPLUUI.asFlow(), pickListItem.asFlow()) { plu, item ->
        plu.isSubstitution || plu.isIssueScanning || (
            item?.sellByWeightInd == SellByType.Each && (
                item.pluList?.getOrNull(0)?.toIntOrNull() == null || item.pluList?.getOrNull(0)?.toIntOrNull
                () == 0
                )
            )
    }.asLiveData()

    val continueEnabled = combine(pluEntryText.asFlow(), manualEntryPLUUI.asFlow()) { plu, entryUi ->
        (plu.length in 4..5 || !entryUi.isSubstitution) && isPluValid(plu)
    }.asLiveData()

    // Events
    val closeKeyboard: LiveData<Unit> = MutableLiveData()

    fun setDefaultPlu(plu: String?) {
        if (plu?.toIntOrNull() != null && plu.toIntOrNull() != 0) {
            pluEntryText.postValue(plu)
        }
    }

    fun validatePluTextEntry() {
        val pluEntered = pluEntryText.value.orEmpty()
        Timber.v("[validatePlu] plu entered=$pluEntered")

        val error = if (isPluValid(pluEntered) || pluEntered.isEmpty()) ValidationError.NONE else ValidationError.PLU_VALIDATION

        // Only set value if it's different from the current one--to prevent UI jankiness
        if (validationError.value != error) {
            validationError.set(error)
        }
    }

    private fun isPluValid(plu: String): Boolean {
        val item = pickListItem.value
        val isEachWithPlu0 = item?.sellByWeightInd == SellByType.Each && item.pluList?.contains(BY_EACH_PLU_0) == true

        return when (manualEntryPLUUI.value?.isSubstitution == true || manualEntryPLUUI.value?.isIssueScanning == true || isEachWithPlu0) {
            true -> plu.isNotNullOrBlank() && plu.all { it.isDigit() }
            false -> pickListItem.value?.pluList?.contains(plu) == true
        }
    }
}
