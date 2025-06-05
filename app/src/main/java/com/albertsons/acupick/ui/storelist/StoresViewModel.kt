package com.albertsons.acupick.ui.storelist

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.EventLabel
import com.albertsons.acupick.FirebaseAnalyticsInterface
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.request.UserActivityRequestDto
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.DevOptionsRepositoryWriter
import com.albertsons.acupick.data.repository.LoginLogoutAnalyticsRepository
import com.albertsons.acupick.data.repository.PushNotificationsRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.TokenizedLdapRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.util.AnalyticsHelper.EventKey
import com.albertsons.acupick.ui.util.StateHandler
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class StoresViewModel(
    val stateHandle: SavedStateHandle,
    app: Application,
    private val userRepo: UserRepository,
    private val dispatcherProvider: DispatcherProvider,
) : BaseViewModel(app) {

    // DI
    private val siteRepository: SiteRepository by inject()
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    private val pushNotificationsRepository: PushNotificationsRepository by inject()
    private val loginLogoutLogicRepo: LoginLogoutAnalyticsRepository by inject()
    private val devOptionsRepositoryWriter: DevOptionsRepositoryWriter by inject()
    private val conversationsRepository: ConversationsRepository by inject()
    private val firebaseAnalytics: FirebaseAnalyticsInterface by inject()
    private val tokenizedLdapRepository: TokenizedLdapRepository by inject()

    // Managed state
    var store: String? by StateHandler(stateHandle)

    // UI
    val stores: LiveData<List<String>> = MutableLiveData(userRepo.user.value?.getStoreIds())
    val confirmActive: LiveData<Boolean> = MutableLiveData(store != null && store != userRepo.user.value!!.selectedStoreId)
    val searchText = MutableLiveData("")
    val filteredStores = combine(stores.asFlow(), searchText.asFlow()) { stores, searchText ->
        stores.filter {
            it.contains(searchText) || searchText.isEmpty()
        }
    }.asLiveData()
    private val changingSite: Boolean = !userRepo.user.value!!.selectedStoreId.isNullOrEmpty()

    // Events
    val storeClickAction: LiveData<Unit> = LiveEvent()
    val storeSelectionCompleteAction: LiveData<Unit> = LiveEvent()

    init {
        // if changing site, select the current one
        if (changingSite) {
            onStoreClicked(storeValue = userRepo.user.value!!.selectedStoreId ?: "")
        }
        registerCloseAction(SITE_DETAILS_ERROR_TAG) {
            closeActionFactory { onConfirmClick() }
        }
    }

    fun onStoreClicked(storeValue: String) {
        store = storeValue
        // If choosing the same store, the button will not become active
        confirmActive.postValue(storeValue != userRepo.user.value!!.selectedStoreId)
        stores.value?.let { stores.postValue(it) } // Trigger UI to render now that selection has been changed.
        storeClickAction.set(Unit)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun onConfirmClick() {
      //  storeSelectionCompleteAction.postValue(Unit)
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { onConfirmClick() }
            } else {
                store?.let { siteId ->
                    val result = isBlockingUi.wrap { siteRepository.getSiteDetails(siteId) }
                    when (result) {
                        is ApiResult.Success -> {
                            firebaseAnalytics.setUserPropertyValue(EventLabel.STORE_ID, siteId)
                            firebaseAnalytics.setuserId(userRepo.user.value?.userId ?: "")
                            loginLogoutLogicRepo.sendOrSaveLogoutData(UserActivityRequestDto.ACTIVITY_LOGOUT_REASON_CHANGED_STORE)
                            val user = userRepo.user.value!!
                            val newUser = user.copy(selectedStoreId = store)
                            userRepo.updateUser(newUser)
                            acuPickLogger.setUserData(EventKey.STORE_ID, store)
                            loginLogoutLogicRepo.onLogin(siteId = siteId)
                            devOptionsRepositoryWriter.storeSiteId(siteId)
                            sendFcmRegistrationToken()

                            userRepo.user.value?.userId?.let {
                                // initialise the sdk in non blocking way and
                                // Golbal scope to prevent premature cancellation
                                GlobalScope.async {
                                    conversationsRepository.initializeChat(it)
                                }
                            }

                            tokenizedLdapRepository.getSitePickersLdapDetails(siteId)
                            storeSelectionCompleteAction.postValue(Unit)
                        }

                        is ApiResult.Failure -> {
                            handleApiError(result, retryAction = { onConfirmClick() })
                        }
                    }.exhaustive
                }
            }
        }
    }

    fun onSearchClearClick() {
        searchText.postValue("")
    }

    // If Firebase Cloud Messaging resgistration token is available, send it, along with site ID to AcuPick backend
    private suspend fun sendFcmRegistrationToken() {
        pushNotificationsRepository.sendFcmTokenToBackend()
    }

    companion object {
        private const val SITE_DETAILS_ERROR_TAG = "siteDetailsError"
    }
}
