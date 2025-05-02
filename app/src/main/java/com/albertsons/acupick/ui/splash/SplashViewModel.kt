package com.albertsons.acupick.ui.splash

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.configureLeakCanary
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ConversationsClientWrapper
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.DevOptionsRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.util.AnalyticsHelper.EventKey
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import timber.log.Timber
import java.time.LocalDate

private const val SPLASH_DELAY_MS = 1500L

class SplashViewModel(app: Application, userRepo: UserRepository, dispatcherProvider: DispatcherProvider) : BaseViewModel(app) {

    private val devOptionsRepo: DevOptionsRepository by inject()
    private val conversationsClientWrapper: ConversationsClientWrapper by inject()
    private val conversationsRepository: ConversationsRepository by inject()
    private val userRepo: UserRepository by inject()
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()

    private val _isHoliday = MutableLiveData<Boolean>()
    val isHoliday: LiveData<Boolean> get() = _isHoliday

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun setupChat() {
        userRepo.user.value?.userId?.let {
            GlobalScope.async { conversationsRepository.initializeChat(it) }
        }
    }

    init {
        configureLeakCanary(devOptionsRepo.useLeakCanary)
        _isHoliday.value = checkIfHoliday()
        viewModelScope.launch(dispatcherProvider.IO) {
            delay(SPLASH_DELAY_MS)
            initializeApp(userRepo)
        }
    }

    private fun checkIfHoliday(): Boolean {
        val today = LocalDate.now()
        val start = LocalDate.of(today.year, 11, 15)
        val end = LocalDate.of(today.year + 1, 1, 1)
        return !today.isBefore(start) && !today.isAfter(end)
    }

    private suspend fun initializeApp(userRepo: UserRepository) {
        Timber.d("networkAvailabilityManager ${networkAvailabilityManager.isConnected.first().not()}")
        if (networkAvailabilityManager.isConnected.first().not()) {
            networkAvailabilityManager.triggerOfflineError {
                initializeApp(userRepo)
            }
        } else {
            delay(SPLASH_DELAY_MS)
            if (userRepo.isLoggedIn.first()) {
                // If logged in user only has one store, or has selected a store already, go to home.
                val userInfo = userRepo.user.value!!
                acuPickLogger.setUserData(EventKey.USER_ID, userInfo.userId)
                when {
                    // No stores to select
                    userInfo.sites.isEmpty() -> {
                        acuPickLogger.setUserData(EventKey.STORE_ID, "")
                        Timber.d("No stores returned.")
                    }

                    // Store already selected
                    userInfo.selectedStoreId.isNotNullOrEmpty() -> {
                        acuPickLogger.setUserData(EventKey.STORE_ID, userInfo.selectedStoreId)
                        if (!conversationsClientWrapper.isClientCreated) {
                            Timber.d("Store already selected")
                            setupChat()
                        }
                        _navigationEvent.postValue(NavigationEvent.Action(R.id.action_splashFragment_to_homeFragment))
                    }

                    // Only 1 store, pick it and go.
                    userInfo.sites.size == 1 -> {
                        acuPickLogger.setUserData(EventKey.STORE_ID, userInfo.sites.firstOrNull() ?: "")
                        if (!conversationsClientWrapper.isClientCreated) {
                            Timber.d("Setting up chat")
                            setupChat()
                        }
                        _navigationEvent.postValue(NavigationEvent.Action(R.id.action_splashFragment_to_homeFragment))
                    }

                    // Otherwise, go to stores list
                    else -> _navigationEvent.postValue(NavigationEvent.Action(R.id.action_splashFragment_to_storesFragment))
                }.exhaustive
            } else {
                _navigationEvent.postValue(NavigationEvent.Action(R.id.action_splashFragment_to_loginFragment))
            }
        }
    }
}
