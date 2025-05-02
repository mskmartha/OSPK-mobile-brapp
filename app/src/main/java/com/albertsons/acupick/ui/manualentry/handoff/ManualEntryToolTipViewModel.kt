package com.albertsons.acupick.ui.manualentry.handoff

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class ManualEntryToolTipViewModel(app: Application) : BaseViewModel(app) {
    val shouldPlayAnimation = MutableLiveData(true)
    val hideReshopBarcode = MutableLiveData<Boolean>()
    val backButtonEvent = MutableSharedFlow<Unit>()
    fun onBackPressHandle() =
        viewModelScope.launch {
            backButtonEvent.emit(Unit)
        }
}
