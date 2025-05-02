package com.albertsons.acupick.ui.dialog

import android.app.Application

class AlternativeLocationViewModel(app: Application) : CustomDialogViewModel(app) {

    var path: Int? = null

    fun onItemFoundButtonClicked() {
        onNegativeButtonClick()
    }

    fun onOutOfStockButtonClicked() {
        navigation.postValue(Pair(CloseAction.Positive, path))
    }
}
