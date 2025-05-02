package com.albertsons.acupick.ui.dialog

import android.app.Application

class DestagingDialogViewmodel(app: Application) : CustomDialogViewModel(app) {

    fun okCtaClicked() {
        navigation.postValue(Pair(CloseAction.Positive, null))
    }
}
