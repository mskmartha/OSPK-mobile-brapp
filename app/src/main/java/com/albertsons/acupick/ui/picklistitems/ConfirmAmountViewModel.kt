package com.albertsons.acupick.ui.picklistitems

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.data.model.ConfirmAmountError
import com.albertsons.acupick.data.model.FulfilledQuantityResult
import com.albertsons.acupick.data.model.RequestedAmount
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.picklist.toConfirmAmountError
import com.albertsons.acupick.data.picklist.toConfirmNetWeightResult
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConfirmAmountViewModel(
    app: Application,
) : BaseViewModel(app) {

    val requestedAmount = MutableLiveData<RequestedAmount>()
    val netWeightStateFlow = MutableStateFlow("")
    val confirmAmountErrorLiveData: LiveData<ConfirmAmountError> = MutableLiveData()
    val continueEnabledLiveData = netWeightStateFlow.map { it.isNotBlank() }.asLiveData()
    val resultNetWeightSharedFlow: SharedFlow<FulfilledQuantityResult.ConfirmNetWeightResult> = MutableSharedFlow()
    var itemType: SellByType = SellByType.PriceScaled

    init {
        viewModelScope.launch {
            netWeightStateFlow.collect {
                clearConfirmAmountError()
            }
        }
    }

    private fun clearConfirmAmountError() = confirmAmountErrorLiveData.postValue(ConfirmAmountError.None)

    fun onContinueClicked() {
        viewModelScope.launch {
            val netWeightDouble = netWeightStateFlow.value.toDoubleOrNull() ?: 0.0
            requestedAmount.value?.toConfirmAmountError(netWeightDouble, itemType).let { confirmAmountError ->
                when (confirmAmountError) {
                    ConfirmAmountError.None -> sendNetWeightResult(netWeightDouble)
                    else -> confirmAmountErrorLiveData.postValue(confirmAmountError)
                }
            }
        }
    }

    private suspend fun sendNetWeightResult(resultNetWeight: Double) = resultNetWeightSharedFlow.emit(requestedAmount.value?.toConfirmNetWeightResult(resultNetWeight, itemType))

    fun navigateUp() = _navigationEvent.postValue(NavigationEvent.Up)

    fun onToolTipClicked() = _navigationEvent.postValue(
        NavigationEvent.Directions(
            ConfirmAmountFragmentDirections.actionConfirmAmountFragmentToNetWeightToolTipFragment()
        )
    )
}
