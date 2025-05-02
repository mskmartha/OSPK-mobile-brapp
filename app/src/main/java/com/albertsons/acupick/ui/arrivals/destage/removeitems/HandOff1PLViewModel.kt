package com.albertsons.acupick.ui.arrivals.destage.removeitems

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.data.model.HandOff1PLAction
import com.albertsons.acupick.data.model.HandOff1PLInterstitialParams
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.HAND_OFF_ALREADY_ASSIGNED_ARG_DATA
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.usecase.handoff.CompleteHandoff1PLUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class HandOff1PLViewModel(app: Application, private val completeHandoff1PLUseCase: CompleteHandoff1PLUseCase) : BaseViewModel(app) {

    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()

    val isShowingTroubleMessage: LiveData<Boolean> = MediatorLiveData<Boolean>().apply { postValue(false) }
    val showBackToHomeButton = MutableLiveData(false)

    fun navigateToHomeScreen() = _navigationEvent.postValue(NavigationEvent.Up)
    private var isReassigned: Boolean = false
    var handOffAction: LiveData<HandOff1PLAction> = MutableLiveData()

    init {

        registerCloseAction(SINGLE_ORDER_ERROR_DIALOG_TAG) {
            closeActionFactory(
                positive = { navigateToHome() }
            )
        }

        viewModelScope.launch {
            completeHandoff1PLUseCase.handOffReassigned.collect {
                isReassigned = it
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = HAND_OFF_ALREADY_ASSIGNED_ARG_DATA,
                        tag = SINGLE_ORDER_ERROR_DIALOG_TAG
                    )
                )
            }
        }
    }

    fun handleHandoffCompletion(params: HandOff1PLInterstitialParams) {

        handOffAction.set(params.handOffAction)
        viewModelScope.launch {
            if (!networkAvailabilityManager.isConnected.first()) {
                isShowingTroubleMessage.postValue(true)
            }
            completeHandoff1PLUseCase()
            isShowingTroubleMessage.postValue(false)
            showBackToHomeButton.postValue(true)
        }
    }
}
