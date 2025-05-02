package com.albertsons.acupick.ui.arrivals.destage

import android.app.Application
import androidx.lifecycle.LiveData
import com.albertsons.acupick.FirebaseAnalyticsInterface
import com.albertsons.acupick.ui.BaseViewModel
import com.hadilq.liveevent.LiveEvent
import org.koin.core.component.inject

class ArrivalsOptionsViewModel(app: Application) : BaseViewModel(app) {

    val cancelClickAction: LiveData<Unit> = LiveEvent()
    val markAsNotHereAction: LiveData<Unit> = LiveEvent()
    val firebaseAnalytics: FirebaseAnalyticsInterface by inject()

    fun cancelClicked() {
        cancelClickAction.postValue(Unit)
    }

    fun onMarkAsNotHereLabelClicked() {
        markAsNotHereAction.postValue(Unit)
    }
}
