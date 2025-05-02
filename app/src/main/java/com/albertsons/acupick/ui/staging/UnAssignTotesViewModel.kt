package com.albertsons.acupick.ui.staging

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.StagingTwoData
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.repository.StagingStateRepository
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.models.ToteUI
import org.koin.core.component.inject

class UnAssignTotesViewModel(val app: Application) : BaseViewModel(app) {

    private val stagingStateRepo: StagingStateRepository by inject()

    private val currentDiscardedTotes = MutableLiveData<Set<String>>(emptySet())

    val inputTotes = MutableLiveData<List<ToteUI>>()
    var shortOrderId: String? = null
    var customerName: String? = null
    var customerOrderNumber: String = ""
    var activityId = ""
    var isMultiSource = false

    val activity = MutableLiveData<UnAssignToteParams>()

    val amTotes = inputTotes.map { totes -> totes.filter { it.storageType == StorageType.AM } }.map { totes ->
        totes.forEach {
            inputTotes ->
            inputTotes.isChecked = currentDiscardedTotes.value?.any { it == inputTotes.toteId } ?: false
        }
        totes
    }
    val htTotes = inputTotes.map { totes -> totes.filter { it.storageType == StorageType.HT } }.map { totes ->
        totes.forEach {
            inputTotes ->
            inputTotes.isChecked = currentDiscardedTotes.value?.any { it == inputTotes.toteId } ?: false
        }
        totes
    }
    val chTotes = inputTotes.map { totes -> totes.filter { it.storageType == StorageType.CH } }.map { totes ->
        totes.forEach {
            inputTotes ->
            inputTotes.isChecked = currentDiscardedTotes.value?.any { it == inputTotes.toteId } ?: false
        }
        totes
    }
    val fzTotes = inputTotes.map { totes -> totes.filter { it.storageType == StorageType.FZ } }.map { totes ->
        totes.forEach {
            inputTotes ->
            inputTotes.isChecked = currentDiscardedTotes.value?.any { it == inputTotes.toteId } ?: false
        }
        totes
    }

    init {
        changeToolbarTitleEvent.postValue(app.applicationContext.getString(R.string.staging_unassign_tote))
    }

    fun setParams(params: UnAssignToteParams) {
        activityId = params.stagingActivityId.toString()
        customerOrderNumber = params.customerOrderNumber.orEmpty()
        isMultiSource = params.isMultiSource ?: false
        currentDiscardedTotes.set(
            stagingStateRepo.loadStagingPartTwo(
                custId = params.customerOrderNumber.orEmpty(),
                activityId = params.stagingActivityId.toString(),
            )?.unassignedToteIdList?.toSet() ?: emptySet()
        )
    }

    fun updateDiscardedToteList(toteId: String, isAdd: Boolean) {
        val updatedList = currentDiscardedTotes.value?.toMutableSet()?.apply {
            if (isAdd) add(toteId) else remove(toteId)
        }
        currentDiscardedTotes.set(updatedList)
        updateStagingStateRepo()
    }

    fun onConfirmClicked() = _navigationEvent.postValue(NavigationEvent.Up)

    private fun updateStagingStateRepo() {
        var stagingPartTwoData = stagingStateRepo.loadStagingPartTwo(customerOrderNumber, activityId)
        if (stagingPartTwoData == null) {
            stagingPartTwoData = StagingTwoData(
                customerOrderNumber = customerOrderNumber,
                scannedBagList = emptyList(),
                activityId = activityId.toLong(),
                unassignedToteIdList = currentDiscardedTotes.value?.toList() ?: emptyList(),
                isMultiSource = isMultiSource
            )
            stagingStateRepo.saveStagingPartTwo(stagingPartTwoData, customerOrderNumber, activityId)
        } else {
            stagingStateRepo.saveStagingPartTwo(
                StagingTwoData(
                    scannedBagList = stagingPartTwoData.scannedBagList,
                    customerOrderNumber = stagingPartTwoData.customerOrderNumber,
                    activityId = stagingPartTwoData.activityId,
                    unassignedToteIdList = currentDiscardedTotes.value?.toList() ?: emptyList(),
                    isMultiSource = stagingPartTwoData.isMultiSource
                ),
                customerOrderNumber,
                activityId
            )
        }
    }
}
