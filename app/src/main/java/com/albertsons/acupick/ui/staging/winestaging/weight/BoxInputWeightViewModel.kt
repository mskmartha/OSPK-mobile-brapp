package com.albertsons.acupick.ui.staging.winestaging.weight

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ConfirmAmountError
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import java.lang.Exception

class BoxInputWeightViewModel(val app: Application) : BaseViewModel(app) {

    val weight = MutableLiveData<String>()
    val label = MutableLiveData<String>()
    val weightSet = MutableLiveData<Pair<String, String>>()
    val enableConfirmLabel = weight.map { isValidWeight(weight = weight.value ?: "") }
    val confirmAmountErrorLiveData: LiveData<ConfirmAmountError> = MutableLiveData()

    init {
        clear()
        changeToolbarTitleEvent.postValue(app.getString(R.string.input_weight))
    }

    fun updateScreen(boxLabel: String) {
        label.value = boxLabel
    }

    fun clear() {
        weight.set("")
    }

    fun onConfirmClciked() {
        val capturedWeight = weight.value?.toDouble() ?: 0.0
        when {
            capturedWeight >= 150 -> {
                confirmAmountErrorLiveData.postValue(ConfirmAmountError.TooHeavy)
                return
            }
            capturedWeight < 0.5 -> {
                confirmAmountErrorLiveData.postValue(ConfirmAmountError.TooLight)
                return
            }
            else -> {
                confirmAmountErrorLiveData.postValue(ConfirmAmountError.None)
                (capturedWeight in 0.5..150.0)
                weightSet.value = Pair(weight.value ?: "", label.value ?: "")
            }
        }
    }

    private fun isValidWeight(weight: String): Boolean {
        return try {
            // If entered weight valid double, clear error & enable cta
            weight.toDouble()
            confirmAmountErrorLiveData.postValue(ConfirmAmountError.None)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun navigateUp() {
        _navigationEvent.postValue(NavigationEvent.Up)
    }
}
