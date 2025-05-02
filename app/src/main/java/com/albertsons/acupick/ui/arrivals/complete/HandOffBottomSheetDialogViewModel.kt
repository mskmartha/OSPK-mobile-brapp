package com.albertsons.acupick.ui.arrivals.complete

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.albertsons.acupick.FirebaseAnalyticsInterface
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.CloseAction
import com.hadilq.liveevent.LiveEvent
import org.koin.core.component.inject

class HandOffBottomSheetDialogViewModel(app: Application) : BaseViewModel(app) {
    val isCompleteEnabled = MutableLiveData<Boolean>()
    val completeClickAction: LiveData<CloseAction> = LiveEvent()
    val headerClickAction: LiveData<Unit> = LiveEvent()
    val isExpanded = MutableLiveData(false)
    val firebaseAnalytics: FirebaseAnalyticsInterface by inject()

    fun onHeaderClicked() {
        headerClickAction.postValue(Unit)
    }

    fun onCompleteClicked() {
        completeClickAction.postValue(CloseAction.Positive)
    }
}
