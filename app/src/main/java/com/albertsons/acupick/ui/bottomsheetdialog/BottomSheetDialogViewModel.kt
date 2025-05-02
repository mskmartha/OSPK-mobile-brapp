package com.albertsons.acupick.ui.bottomsheetdialog

import android.app.Application
import androidx.lifecycle.LiveData
import com.albertsons.acupick.FirebaseAnalyticsInterface
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.CloseAction
import com.hadilq.liveevent.LiveEvent
import org.koin.core.component.inject

const val BOTTOM_SHEET_DISMISS_DELAY_DURATION_MS = 2000L
open class BottomSheetDialogViewModel(app: Application) : BaseViewModel(app) {

    val navigation: LiveData<Pair<CloseAction, Int?>> = LiveEvent()
    val dispatcherProvider: DispatcherProvider by inject()

    val firebaseAnalytics: FirebaseAnalyticsInterface by inject()

    fun dismissBottomSheet() {
        navigation.postValue(Pair(CloseAction.Positive, null))
    }

    fun dismissBottomSheetWithNegativeAction() {
        navigation.postValue(Pair(CloseAction.Negative, null))
    }
}
