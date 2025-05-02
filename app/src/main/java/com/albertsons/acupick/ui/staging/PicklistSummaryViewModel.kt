package com.albertsons.acupick.ui.staging

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.totes.TotesUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class PicklistSummaryViewModel(
    val app: Application
) : BaseViewModel(app) {

    private val pickRepository: PickRepository by inject()
    private val dispatcherProvider: DispatcherProvider by inject()

    /** Source of truth */
    val pickList: LiveData<ActivityDto?> = pickRepository.pickList.asLiveData()
    var pickListId: Long = 0
    var customerOrderNumber: String? = null
    val activityDto: MutableStateFlow<ActivityDto?> = MutableStateFlow(null)
    val toteInfo = activityDto.map {
        TotesUi(customerOrderNumber, it?.itemActivities?.first(), it)
    }

    init {
        changeToolbarTitleEvent.postValue(app.applicationContext.getString(R.string.pick_list_info_header))
    }

    fun loadData(picklistId: Long) {
        viewModelScope.launch(dispatcherProvider.IO) {
            when (val result = isBlockingUi.wrap { pickRepository.getActivityDetails(picklistId.toString(), false) }) {
                is ApiResult.Success<ActivityDto> -> activityDto.emit(result.data)
                else -> null
            }
        }
    }
}
