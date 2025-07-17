package com.albertsons.acupick.ui.how_to_win

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.response.OthRule
import com.albertsons.acupick.data.repository.GamesRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.ui.BaseViewModel
import kotlinx.coroutines.launch

class HowToWinViewModel(
    private val app: Application,
    private val repo: GamesRepository,
    private val dispatcherProvider: DispatcherProvider,
) : BaseViewModel(app) {

    val BASE_POINTS = "0"
    val OTH_RULE = "1"
    val OTH_STORE = "2"

    val isDataLoading : LiveData<Boolean> = MutableLiveData(true)

    val basePoints : LiveData<List<OthRule?>?> = MutableLiveData(arrayListOf())
    val othRule5 : LiveData<List<OthRule>> = MutableLiveData(arrayListOf())
    val othStore : LiveData<List<OthRule>> = MutableLiveData(arrayListOf())

    init {
        getMyRewards()
    }

    private fun getMyRewards() {
        viewModelScope.launch(dispatcherProvider.IO) {
            val result = isBlockingUi.wrap { repo.getGameRewardsPoint() }
            when (result) {
                is ApiResult.Success -> {
                    isDataLoading.postValue(false)
                    val basePointsRules = result.data.basePointsRules ?: arrayListOf()
                    val userOTH = result.data.userOTH ?: arrayListOf()
                    val storeOTH = result.data.storeOTH ?: arrayListOf()
                    basePoints.postValue(basePointsRules)
                    othRule5.postValue(userOTH)
                    othStore.postValue(storeOTH)
                }

                is ApiResult.Failure -> {
                    isDataLoading.postValue(false)
                    handleApiError(result, retryAction = { getMyRewards() })
                }
            }.exhaustive
        }

    }
}