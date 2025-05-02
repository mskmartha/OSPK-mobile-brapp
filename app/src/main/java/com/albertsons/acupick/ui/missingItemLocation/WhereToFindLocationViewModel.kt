package com.albertsons.acupick.ui.missingItemLocation

import android.app.Application
import com.albertsons.acupick.ui.BaseViewModel
import com.hadilq.liveevent.LiveEvent

class WhereToFindLocationViewModel(app: Application) : BaseViewModel(app) {
    val navigation = LiveEvent<Boolean>()
    fun onBackPressHandle() {
        navigation.postValue(true)
    }
}
