package com.albertsons.acupick.ui.bottomsheetdialog

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.CloseAction
import com.albertsons.acupick.ui.substitute.BulkItem
import com.albertsons.acupick.ui.substitute.BulkSubstitutionDbViewModel
import com.hadilq.liveevent.LiveEvent

class BulkSubstituteConfirmationViewModel(
    val app: Application,
) : BaseViewModel(app) {

    val navigation: LiveData<Pair<CloseAction, String?>> = LiveEvent()
    val bulkSubList = MutableLiveData<List<BulkSubstitutionDbViewModel>>()
    val isIssueScanning = MutableLiveData<Boolean>()
    val isConfirmEnabled = MutableLiveData(false)

    private fun onItemClicked(bulkItem: BulkItem) {
        val newData = bulkSubList.value?.map {
            it.copy(selected = it.itemId == bulkItem.itemId)
        }
        bulkSubList.postValue(newData)
        isConfirmEnabled.value = true
    }

    fun setData(bulkItem: List<BulkItem>) {
        bulkSubList.value = bulkItem.map {
            BulkSubstitutionDbViewModel(false, ::onItemClicked, it)
        }
    }

    fun onCancel() {
        navigation.postValue(Pair(CloseAction.Negative, null))
    }

    fun onConfirmButtonClick() {
        navigation.postValue(Pair(CloseAction.Positive, bulkSubList.value?.find { it.selected }?.itemId))
    }
}
