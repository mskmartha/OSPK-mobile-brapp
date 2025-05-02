package com.albertsons.acupick.ui.dialog

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.BaseViewModel
import com.hadilq.liveevent.LiveEvent

open class CustomDialogViewModel(app: Application) : BaseViewModel(app) {

    val navigation: LiveData<Pair<CloseAction, Int?>> = LiveEvent()
    val navigationWithData: LiveData<Pair<CloseAction, Pair<String, String>?>> = LiveEvent()
    val radioChecked = MutableLiveData<Int>()
    val checkBoxChecked = MutableLiveData(false)
    val selection = radioChecked.map { radioChecked ->
        val choice = arrayListOf(
            R.id.radioButton1,
            R.id.radioButton2,
            R.id.radioButton3,
            R.id.radioButton4,
            R.id.radioButton5,
        ).indexOf(radioChecked)
        if (choice == -1) null else choice
    }

    fun onPositiveButtonClick() {
        navigation.postValue(Pair(CloseAction.Positive, selection.value))
    }

    fun onPositiveButtonClick(itemIdAndMessageSid: Pair<String, String>?) {
        navigationWithData.postValue(Pair(CloseAction.Positive, itemIdAndMessageSid))
    }

    fun onNegativeButtonClick() {
        navigation.postValue(Pair(CloseAction.Negative, null))
    }

    fun onCloseIconClick() {
        navigation.postValue(Pair(CloseAction.Dismiss, null))
    }
}
