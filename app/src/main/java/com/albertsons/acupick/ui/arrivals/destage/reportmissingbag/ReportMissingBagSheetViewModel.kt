package com.albertsons.acupick.ui.arrivals.destage.reportmissingbag

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import com.albertsons.acupick.FirebaseAnalyticsInterface
import org.koin.core.component.inject

class ReportMissingBagSheetViewModel(app: Application) : BaseViewModel(app) {

    val cancelClickAction = MutableSharedFlow<Unit>()
    val missingBagLabelAction = MutableSharedFlow<Unit>()
    val missingLooseItemLabelAction = MutableSharedFlow<Unit>()
    val missingLooseItemAction = MutableSharedFlow<Unit>()
    val missingBagAction = MutableSharedFlow<Unit>()
    val firebaseAnalytics: FirebaseAnalyticsInterface by inject()

    fun cancelClicked() = viewModelScope.launch {
        cancelClickAction.emit(Unit)
    }

    fun onMissingLooseItemLabelClicked() = viewModelScope.launch {
        missingLooseItemLabelAction.emit(Unit)
    }

    fun onMissingLooseItemClicked() = viewModelScope.launch {
        missingLooseItemAction.emit(Unit)
    }

    fun onMissingBagLabelClicked() = viewModelScope.launch {
        missingBagLabelAction.emit(Unit)
    }

    fun onMissingBagClicked() = viewModelScope.launch {
        missingBagAction.emit(Unit)
    }

    fun navigateUp() {
        _navigationEvent.postValue(NavigationEvent.Up)
    }
}
