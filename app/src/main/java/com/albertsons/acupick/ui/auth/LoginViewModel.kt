package com.albertsons.acupick.ui.auth

import android.app.Application
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.buildconfig.BuildConfigProvider
import com.albertsons.acupick.data.crashreporting.ForceCrashLogic
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.CredentialModel
import com.albertsons.acupick.data.model.ValidCredentialModel
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ConversationsClientWrapper
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.DevOptionsRepository
import com.albertsons.acupick.data.repository.DevOptionsRepositoryWriter
import com.albertsons.acupick.data.repository.LoginLogoutAnalyticsRepository
import com.albertsons.acupick.data.repository.PushNotificationsRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.TokenizedLdapRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.data.toast.Toaster
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.NO_STORES_ASSIGNED_ARG_DATA
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.util.AnalyticsHelper.EventKey
import com.albertsons.acupick.ui.util.hideKeyboard
import com.hadilq.liveevent.LiveEvent
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import timber.log.Timber
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class LoginViewModel(
    private val app: Application,
    private val userRepo: UserRepository,
    buildConfigProvider: BuildConfigProvider,
    private val forceCrashLogic: ForceCrashLogic,
    private val dispatcherProvider: DispatcherProvider,
    private val activityViewModel: MainActivityViewModel,
    private val networkAvailabilityManager: NetworkAvailabilityManager,
    private val toaster: Toaster
) : BaseViewModel(app) {

    // DI
    private val siteRepository: SiteRepository by inject()
    private val pushNotificationsRepository: PushNotificationsRepository by inject()
    private val loginLogoutAnalyticsRepo by inject<LoginLogoutAnalyticsRepository>()
    private val devOptionsRepository: DevOptionsRepository by inject()
    private val devOptionsRepositoryWriter: DevOptionsRepositoryWriter by inject()
    private val conversationsClientWrapper: ConversationsClientWrapper by inject()
    private val conversationsRepository: ConversationsRepository by inject()
    private val tokenizedLdapRepository: TokenizedLdapRepository by inject()

    private val _isHoliday = MutableLiveData<Boolean>()
    val isHoliday: LiveData<Boolean> get() = _isHoliday

    val textWatcher = Observer<String> {
        loginEnabled.postValue(CredentialModel(email.value, password.value).valid)
    }

    val email = MutableLiveData<String>()
    val emailEnabled = email.map { it.isNotNullOrEmpty() }
    val password = MutableLiveData<String>()
    val passwordEnabled = password.map { it.isNotNullOrEmpty() }
    val devOptionsEnabled = buildConfigProvider.isDebugOrInternalBuild

    val enterStoreAction = LiveEvent<Unit>()

    private var handshakeExpireTime: LocalDateTime? = null
    private var handshakePointOneClicked = false
    private var handshakePointTwoClicked = false
    private var handshakePointThreeClicked = false

    val loginEnabled: LiveData<Boolean> = MutableLiveData()
    val userNameError: LiveData<Int> = MutableLiveData()
    val passwordError: LiveData<Int> = MutableLiveData()
    val hideKeyboard: LiveData<Unit> = LiveEvent<Unit>()

    init {
        _isHoliday.value = checkIfHoliday()
        loginEnabled.postValue(false)
        email.observeForever(textWatcher)
        password.observeForever(textWatcher)
        registerCloseAction(NO_SITES_ASSIGNED_TAG) {
            closeActionFactory(
                // Log out user so that they are not stuck here having to restart app
                negative = { activityViewModel.manualLogout() }
            )
        }
        registerCloseAction(SITE_DETAILS_ERROR_TAG) {
            closeActionFactory { onLoginClicked() }
        }
    }

    private fun checkIfHoliday(): Boolean {
        val today = LocalDate.now()
        val start = LocalDate.of(today.year, 11, 15)
        val end = LocalDate.of(today.year + 1, 1, 1)
        return !today.isBefore(start) && !today.isAfter(end)
    }

    override fun onCleared() {
        super.onCleared()
        doClear()
    }

    fun doClear() {
        email.removeObserver(textWatcher)
        password.removeObserver(textWatcher)
    }

    fun onRestartAppClicked() {
        acuPickLogger.v("[LoginViewMode] restarting app now...")
        ProcessPhoenix.triggerRebirth(app)
    }

    fun onDevOptionsClicked() {
        _navigationEvent.postValue(NavigationEvent.Action(R.id.action_loginFragment_to_devOptionsFragment))
    }

    fun onLoginClicked() {
        // special logic for testing crash reports
        forceCrashLogic.forceCrashOnMatch(email.value)
        val creds = CredentialModel(email.value ?: "", password.value ?: "")
        creds.validCredentials?.let {
            authenticate(it)
        }
    }

    fun imeLoginClick() {
        if (loginEnabled.value == true) {
            onLoginClicked()
        }
    }

    private fun authenticate(credentials: ValidCredentialModel) {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { authenticate(credentials) }
            } else {
                val result = isBlockingUi.wrap { userRepo.login(credentials) }
                when (result) {
                    is ApiResult.Success -> {
                        val sites = userRepo.user.value!!.sites
                        acuPickLogger.setUserData(EventKey.USER_ID, userRepo.user.value!!.userId)
                        when (sites.size) {
                            0 -> {
                                acuPickLogger.d("No stores returned.")
                                inlineDialogEvent.postValue(CustomDialogArgDataAndTag(NO_STORES_ASSIGNED_ARG_DATA, NO_SITES_ASSIGNED_TAG))
                            }
                            1 -> {
                                sites.first().siteId?.let {
                                    enterStore(it)
                                }
                            }
                            else -> {
                                hideKeyboard.postValue(Unit)
                                if (devOptionsRepository.autoChooseLastSite && devOptionsRepository.lastSiteId.isNotNullOrBlank()) {
                                    enterStore(devOptionsRepository.lastSiteId)
                                } else {
                                    _navigationEvent.postValue(NavigationEvent.Action(R.id.action_loginFragment_to_storesFragment))
                                }
                            }
                        }.exhaustive
                    }
                    is ApiResult.Failure.Server -> {
                        acuPickLogger.d("${result.error}")
                        when {
                            result.error?.message?.contains(NO_USER_DETAILS_FOR_USER, ignoreCase = true) == true -> {
                                withContext(dispatcherProvider.Main) {
                                    setLoginErrors(isUserNameError = true)
                                }
                            }
                            result.error?.message?.contains(INCORRECT_USERNAME_OR_PASSWORD_ERROR_MESSAGE, ignoreCase = true) == true -> {
                                withContext(dispatcherProvider.Main) {
                                    setLoginErrors(isUserNameError = false)
                                }
                            }
                            else -> {
                                handleApiError(result, retryAction = { authenticate(credentials) })
                            }
                        }
                    }
                    is ApiResult.Failure -> {
                        handleApiError(result, retryAction = { authenticate(credentials) })
                    }
                }.exhaustive
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun setupChat() {
        userRepo.user.value?.userId?.let {
            GlobalScope.async { conversationsRepository.initializeChat(it) }
        }
    }

    private suspend fun enterStore(siteId: String) {
        val result = isBlockingUi.wrap { siteRepository.getSiteDetails(siteId) }
        when (result) {
            is ApiResult.Success -> {
                val user = userRepo.user.value!!
                userRepo.updateUser(user.copy(selectedStoreId = siteId))
                acuPickLogger.setUserData(EventKey.STORE_ID, siteId)
                loginLogoutAnalyticsRepo.onLogin(siteId = siteId)
                devOptionsRepositoryWriter.storeSiteId(siteId)
                sendFcmRegistrationToken()
                hideKeyboard.postValue(Unit)
                if (!conversationsClientWrapper.isClientCreated) {
                    Timber.d("setup chat")
                    setupChat()
                }
                tokenizedLdapRepository.getSitePickersLdapDetails(siteId)
                enterStoreAction.postValue(Unit)
            }
            is ApiResult.Failure -> handleApiError(result, retryAction = { viewModelScope.launch { enterStore(siteId) } })
        }
    }

    // If Firebase Cloud Messaging resgistration token is available, send it, along with site ID to AcuPick backend
    private fun sendFcmRegistrationToken() {
        viewModelScope.launch(dispatcherProvider.IO) {
            withContext(NonCancellable) {
                pushNotificationsRepository.sendFcmTokenToBackend()
            }

            // TODO: Not sure if error handling is needed.  If sending the FCM token to the AcuPick backend fails,
            // I'm not sure what the user experience should be.
        }
    }

    fun setLoginErrors(isUserNameError: Boolean = false) {
        if (isUserNameError) {
            userNameError.set(R.string.user_name_error)
            passwordError.set(null)
        } else {
            passwordError.set(R.string.password_error)
            userNameError.set(null)
        }
        toaster.toast(R.string.incorrect_credentials_error, Toast.LENGTH_LONG)
    }

    // Handshake to Connectivity Test Page
    fun handshakePointOneOnClick() {
        resetHandshakePoints()

        handshakePointOneClicked = true

        // Set expire time as now + allowed completion duration
        handshakeExpireTime = LocalDateTime.now().plus(HANDSHAKE_TIME_LIMIT_DURATION)
    }

    fun handshakePointTwoOnClick() {
        // Confirm that this is second point pressed
        if (handshakePointOneClicked && !handshakePointTwoClicked) {
            handshakePointTwoClicked = true
        } else {
            resetHandshakePoints()
        }
    }

    fun handshakePointThreeOnClick() {
        handshakePointThreeClicked = true
        // Verify that this is the third point clicked
        if (handshakePointOneClicked && handshakePointTwoClicked && handshakePointThreeClicked) {
            // If so, check that the timer is not expired
            if (handshakeExpireTime?.isAfter(LocalDateTime.now()) == true) {
                _navigationEvent.postValue(NavigationEvent.Action(R.id.action_loginFragment_to_fieldServicesFragment))
            }
        }
        resetHandshakePoints()
    }

    private fun resetHandshakePoints() {
        handshakePointOneClicked = false
        handshakePointTwoClicked = false
        handshakePointThreeClicked = false
        handshakeExpireTime = null
    }

    companion object {
        const val INCORRECT_USERNAME_OR_PASSWORD_ERROR_MESSAGE = "Bad Credentials"
        const val NO_USER_DETAILS_FOR_USER = "User details not available for user"
        private const val NO_SITES_ASSIGNED_TAG = "noSitesAssigned"
        private const val SITE_DETAILS_ERROR_TAG = "siteDetailsError"

        /** Maximum amount of time allowed for a handshake to be considered valid */
        private val HANDSHAKE_TIME_LIMIT_DURATION: Duration = Duration.ofMillis(3000)
    }
}

@BindingAdapter("onImeClick")
fun EditText.onImeClicked(function: (() -> Unit)?) {

    if (function == null) setOnEditorActionListener(null)
    else setOnEditorActionListener { _, actionId, event ->

        val imeAction = when (actionId) {
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_GO -> true
            else -> false
        }

        val actionDown = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN

        if (imeAction || actionDown) {
            hideKeyboard()
            true.also {
                function.invoke()
            }
        } else false
    }
}
