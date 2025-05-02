package com.albertsons.acupick.ui.arrivals.destage

import android.app.Application
import androidx.lifecycle.LiveData
import com.albertsons.acupick.FirebaseAnalyticsInterface
import com.albertsons.acupick.ui.BaseViewModel
import com.hadilq.liveevent.LiveEvent
import org.koin.core.component.inject

class DestageBottomSheetViewModel(app: Application) : BaseViewModel(app) {
    val reportIssueClickAction: LiveData<Unit> = LiveEvent()
    val abandonPartialPrescriptionPickupClickAction: LiveData<Unit> = LiveEvent()
    val cancelClickAction: LiveData<Unit> = LiveEvent()
    val firebaseAnalytics: FirebaseAnalyticsInterface by inject()

    fun reportIssueClicked() {
        reportIssueClickAction.postValue(Unit)
    }

    fun abandonPartialPrescriptionPickupClicked() {
        abandonPartialPrescriptionPickupClickAction.postValue(Unit)
    }
    fun cancelClicked() {
        cancelClickAction.postValue(Unit)
    }
}
