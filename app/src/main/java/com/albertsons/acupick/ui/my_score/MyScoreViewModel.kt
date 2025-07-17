package com.albertsons.acupick.ui.my_score

import android.app.Application
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.response.GamesPointsDto
import com.albertsons.acupick.data.model.response.OthRule
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.GamesRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.ui.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.time.Timer
import timber.log.Timber

class MyScoreViewModel(
    private val app: Application,
    private val repo: GamesRepository,
    private val dispatcherProvider: DispatcherProvider,
) :BaseViewModel(app) {



    val isDataLoading : LiveData<Boolean> = MutableLiveData(true)

    val gamesPointsData : LiveData<GamesPointsDto?> = MutableLiveData(null)

    init {
        getRulesData()
    }

    private fun getRulesData() {
        viewModelScope.launch(dispatcherProvider.IO) {
            val result = isBlockingUi.wrap { repo.getRulesData() }
            when (result) {
                is ApiResult.Success -> {
                    isDataLoading.postValue(false)
                    gamesPointsData.postValue(result.data)

                }

                is ApiResult.Failure -> {
                    isDataLoading.postValue(false)
                    handleApiError(result, retryAction = { getRulesData() })
                }
            }.exhaustive
        }

    }

}
