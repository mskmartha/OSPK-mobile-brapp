package com.albertsons.acupick.ui.staging

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.arrivals.destage.OrderCompletionState
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.models.StagingUI
import com.albertsons.acupick.ui.models.ToteUI
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.combineWith
import com.albertsons.acupick.ui.util.getOrZero
import com.hadilq.liveevent.LiveEvent
import org.koin.core.component.inject

class StagingViewModel(
    app: Application,
) : BaseViewModel(app) {

    // DI
    val activityViewModel: MainActivityViewModel by inject()

    // Incoming values
    val stagingUi: MutableLiveData<StagingUI> = MutableLiveData()
    val toteUiList: MutableLiveData<MutableList<ToteUI>> = MutableLiveData(mutableListOf())
    val activityId = MutableLiveData<String>()

    val requestSave: LiveData<Unit> = LiveEvent()

    val isCustomerPreferBag = stagingUi.map { it.isCustomerBagPreference ?: true }
    private var isLooseItemSnackbarShown = false

    // Data
    val toteDbVms: LiveData<MutableList<StagingToteDbViewModel>> = toteUiList.combineWith(stagingUi) { newToteUiList, stagingUI ->
        newToteUiList?.map { StagingToteDbViewModel(it, stagingUI, stagingUI?.isCustomerBagPreference ?: true, ::updateCounts) }?.toMutableList() ?: mutableListOf()
    }

    // Internal event
    private val updateUiEvent = LiveEvent<Unit>()

    val orderCompletionState = updateUiEvent.combineWith(toteDbVms) { _, totes ->
        totes?.firstOrNull()?.stagingUI?.orderNumber?.let { orderNumber ->
            val isComplete = totes.firstOrNull {
                (it.bagCount.value == null || it.bagCount.value == 0) && (it.looseCount.value == null || it.looseCount.value == 0)
            } == null || (isCustomerPreferBag.value == false)
            OrderCompletionState(orderNumber, isComplete)
        }
    }

    init {
        updateUiEvent.postValue(Unit)
    }

    // UI
    val isOrderCompleted: LiveData<Boolean> = updateUiEvent.updating(toteDbVms) {
        it.filter { it.bagCount.value == null || it.looseCount.value == null }.isNullOrEmpty()
    }

    // Flags to control presence of cards in UI
    val hideAmbientCard = toteUiList.map { it -> it.filter { it.storageType == StorageType.AM }.isNullOrEmpty() }
    val hideChilledCard = toteUiList.map { it -> it.filter { it.storageType == StorageType.CH }.isNullOrEmpty() }
    val hideFrozenCard = toteUiList.map { it -> it.filter { it.storageType == StorageType.FZ }.isNullOrEmpty() }
    val hideHotCard = toteUiList.map { it -> it.filter { it.storageType == StorageType.HT }.isNullOrEmpty() }

    // Events
    val advanceEvent: LiveData<Unit> = LiveEvent()

    // Trigger toteDbVms to notify
    private fun updateCounts() {
        requestSave.postValue(Unit)
        updateUiEvent.postValue(Unit)
        handleLooseItemSnackbar()
    }

    private fun handleLooseItemSnackbar() {
        if (isCustomerPreferBag.value == false) {
            if (toteDbVms.value?.all { it.looseCount.value == null || it.looseCount.value == 0 } == true) {
                isLooseItemSnackbarShown = false
            } else if (toteDbVms.value?.any { it.looseCount.value == 1 } == true && !isLooseItemSnackbarShown) {
                isLooseItemSnackbarShown = true
                showSnackBar(
                    AcupickSnackEvent(
                        message = StringIdHelper.Id(R.string.please_only_enter_loose_item_count_for_oversized_items),
                        type = SnackType.INFO,
                        isDismissable = true
                    )
                )
            }
        }
    }

    fun getIncompleteStorageTypes() = with(mutableListOf<StorageType>()) {
        val incompleteStorageTypesSet: HashSet<StorageType> = hashSetOf()
        // filtering totes where both bag and loose item count is 0
        toteDbVms.value?.filter { it.bagCount.value.getOrZero() == 0 && it.looseCount.value.getOrZero() == 0 }
            // putting their storage types to hashset
            ?.forEach { it.item.storageType?.let(incompleteStorageTypesSet::add) }
        incompleteStorageTypesSet.toList()
    }
}
