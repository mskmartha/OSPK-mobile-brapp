package com.albertsons.acupick.ui.dialog

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.albertsons.acupick.ui.models.QuantityParams
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.manualentry.handoff.MAX_MANUAL_ENTRY_QUANTITY
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.flow.combine

class QuantityPickerDialogViewModel(
    val app: Application,
) : BaseViewModel(app) {

    val navigation: LiveData<Pair<CloseAction, Int?>> = LiveEvent()

    val quantity = MutableLiveData(0)

    val quantityParams = MutableLiveData<QuantityParams>()

    val plusEnabled = combine(quantityParams.asFlow(), quantity.asFlow()) { params, quantity ->
        val requested = params.requested ?: 0
        val entered = params.entered ?: 0
        val maxQuantity = requested - entered
        when {
            params.isSubstitution == true && params.isSameItem -> quantity < requested - entered
            params.isSubstitution == true -> quantity <= MAX_MANUAL_ENTRY_QUANTITY
            params.isSubstitution == false && entered == 0 -> quantity < requested
            params.isTotaled -> quantity < maxQuantity && quantity <= MAX_MANUAL_ENTRY_QUANTITY
            else -> quantity < maxQuantity
        }
    }.asLiveData()

    // /////////////////////////////////////////////////////////////////////////
    // UI Callbacks
    // /////////////////////////////////////////////////////////////////////////
    fun onMinusButtonClicked() {
        quantity.postValue(quantity.value?.minus(1)?.coerceAtLeast(0))
    }

    fun onPlusButtonClicked() {
        quantity.postValue(
            quantity.value?.plus(1)
                ?.coerceAtMost(
                    when (quantityParams.value?.isSubstitution == true) {
                        true -> MAX_MANUAL_ENTRY_QUANTITY
                        else -> quantityParams.value?.requested ?: 0
                    }
                ) ?: 0
        )
    }

    fun onContinueButtonClicked() {
        navigation.postValue(Pair(CloseAction.Positive, quantity.value ?: 0))
    }

    val maxQuantity = quantityParams.map { param ->
        val requested = param.requested ?: 0
        val entered = param.entered ?: 0
        val maxQuantity = requested - entered

        when {
            param.isSubstitution == true && param.isSameItem -> maxQuantity
            param.isSubstitution == true || param.isIssueScanning -> MAX_MANUAL_ENTRY_QUANTITY
            param.isSubstitution == false && entered == 0 -> maxQuantity
            param.isTotaled -> MAX_MANUAL_ENTRY_QUANTITY // TODO: Need to check this usecase
            else -> maxQuantity
        }
    }
}
