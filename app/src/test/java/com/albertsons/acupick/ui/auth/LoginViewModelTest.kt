package com.albertsons.acupick.ui.auth

import android.widget.Toast
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.albertsons.acupick.R
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.User
import com.albertsons.acupick.data.model.response.ServerErrorDto
import com.albertsons.acupick.data.model.response.SiteDto
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.data.toast.Toaster
import com.albertsons.acupick.infrastructure.utils.stateFlowOf
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.getPrivateProperty
import com.albertsons.acupick.test.loginViewModelFactory
import com.albertsons.acupick.test.runPrivateMethod
import com.albertsons.acupick.test.setPrivateProperty
import com.albertsons.acupick.ui.auth.LoginViewModel.Companion.INCORRECT_USERNAME_OR_PASSWORD_ERROR_MESSAGE
import com.albertsons.acupick.ui.auth.LoginViewModel.Companion.NO_USER_DETAILS_FOR_USER
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import java.time.LocalDateTime

class LoginViewModelTest : BaseTest() {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    @get:Rule
    val koinRule = KoinTestRule(TestModule.generateMockedTestModule())

    companion object {
        private val MOCK_NO_STORE_USER = User(firstName = "Test", lastName = "Tester", userId = "id1234", sites = listOf(), selectedStoreId = "1234")
        private val MOCK_SINGLE_STORE_USER = MOCK_NO_STORE_USER.copy(sites = listOf(SiteDto("1234", true)))
        private val MOCK_MULTI_STORE_USER = MOCK_NO_STORE_USER.copy(sites = listOf(SiteDto("1234", false), SiteDto("5678", false)))
        private val MOCK_MULTI_STORE_USER_MULTI_DEFAULT = MOCK_NO_STORE_USER.copy(sites = listOf(SiteDto("1234", true), SiteDto("5678", true)))
    }

    @Test
    fun loginViewModel_shouldEnableLogin_withValidCredentials() {
        val loginViewModel = loginViewModelFactory()
        loginViewModel.email.postValue("test@example.com")
        loginViewModel.password.postValue("Password1!")
        assertThat(loginViewModel.loginEnabled.value).isTrue()
    }

    @Test
    fun loginViewModel_shouldDisableLogin_withInvalidCredentials() {
        val loginViewModel = loginViewModelFactory()
        loginViewModel.email.postValue("")
        loginViewModel.password.postValue("")
        assertThat(loginViewModel.loginEnabled.value).isFalse()
    }

    @Test
    fun loginViewModel_shouldClearObservers_whenDoClearCalled() {
        val loginViewModel = loginViewModelFactory()
        assertThat(loginViewModel.password.hasActiveObservers()).isTrue()
        assertThat(loginViewModel.email.hasActiveObservers()).isTrue()
        assertThat(loginViewModel.textWatcher).isNotNull()
        loginViewModel.doClear()
        assertThat(loginViewModel.password.hasActiveObservers()).isFalse()
        assertThat(loginViewModel.email.hasActiveObservers()).isFalse()
    }

    @Test
    fun `WHEN the login fails THEN nothing will happen after login`() {
        val mockObserver: Observer<NavigationEvent> = mock()
        val userRepository = mock<UserRepository> {
            onBlocking { login(any()) } doReturn ApiResult.Failure.GeneralFailure("")
            on { user } doReturn stateFlowOf(null)
        }
        val sut = loginViewModelFactory(
            userRepository = userRepository,
        )
        sut.navigationEvent.observeForever(mockObserver)
        sut.email.postValue("test@example.com")
        sut.password.postValue("Password1!")

        sut.onLoginClicked()

        verifyNoInteractions(mockObserver)
    }

    @Test
    fun `WHEN the logged-in user has access to no stores THEN nothing will happen after login`() {
        val mockObserver: Observer<NavigationEvent> = mock()
        val userRepository = mock<UserRepository> {
            onBlocking { login(any()) } doReturn ApiResult.Success(Unit)
            on { user } doReturn stateFlowOf(MOCK_NO_STORE_USER)
        }
        val sut = loginViewModelFactory(
            userRepository = userRepository,
        )
        sut.navigationEvent.observeForever(mockObserver)
        sut.email.postValue("test@example.com")
        sut.password.postValue("Password1!")

        sut.onLoginClicked()

        verifyNoInteractions(mockObserver)
    }

    @Test @Ignore // TODO fix this test
    fun `WHEN the logged-in user only has access to one store THEN the home screen will be shown after login`() {
        val userRepository = mock<UserRepository> {
            onBlocking { login(any()) } doReturn ApiResult.Success(Unit)
            on { user } doReturn stateFlowOf(MOCK_SINGLE_STORE_USER)
        }
        val sut = loginViewModelFactory(
            userRepository = userRepository
        )
        val enterStoreActionMock = sut.enterStoreAction.mock()
        sut.email.postValue("test@example.com")
        sut.password.postValue("Password1!")

        sut.onLoginClicked()
        assertThat(enterStoreActionMock.verifyWithCapture(times(1))).isEqualTo(Unit)
    }

    @Test
    fun `WHEN the logged-in user only has access to more than one store THEN the stores screen will be shown after login`() {
        val userRepository = mock<UserRepository> {
            onBlocking { login(anyOrNull()) } doReturn ApiResult.Success(Unit)
            on { user } doReturn stateFlowOf(MOCK_MULTI_STORE_USER)
        }
        val sut = loginViewModelFactory(
            userRepository = userRepository,
        )
        val navigationEventMock = sut.navigationEvent.mock()
        sut.email.postValue("test@example.com")
        sut.password.postValue("Password1!")

        sut.onLoginClicked()
        assertThat(navigationEventMock.verifyWithCapture(times(1))).isEqualTo(NavigationEvent.Action(R.id.action_loginFragment_to_storesFragment))
    }

    @Test
    fun loginViewModel_authenticate_serverError() {
        val emptyFailureResult = ServerErrorDto()
        var mockUserRepo: UserRepository = mock {
            onBlocking { login(any()) } doReturn ApiResult.Failure.Server(error = emptyFailureResult)
            on { user } doReturn stateFlowOf(null)
        }
        var vm = loginViewModelFactory(userRepository = mockUserRepo)
        val apiErrorEventMock = vm.apiErrorEvent.mock()

        vm.email.postValue("test@example.com")
        vm.password.postValue("Password1!")
        vm.onLoginClicked()
        assertThat(apiErrorEventMock.verifyWithCapture(atLeastOnce())).isEqualTo(Pair("", ApiResult.Failure.Server(error = emptyFailureResult)))

        mockUserRepo = mock {
            onBlocking { login(any()) } doReturn ApiResult.Failure.Server(error = ServerErrorDto(message = NO_USER_DETAILS_FOR_USER))
            on { user } doReturn stateFlowOf(null)
        }
        vm = loginViewModelFactory(userRepository = mockUserRepo)
        var passwordErrorMock = vm.passwordError.mock()
        var usernameErrorMock = vm.userNameError.mock()
        vm.email.postValue("test@example.com")
        vm.password.postValue("Password1!")
        vm.onLoginClicked()
        verifyNoMoreInteractions(apiErrorEventMock)
        assertThat(passwordErrorMock.verifyWithCapture(atLeastOnce())).isNull()
        assertThat(usernameErrorMock.verifyWithCapture(atLeastOnce())).isEqualTo(R.string.user_name_error)

        mockUserRepo = mock {
            onBlocking { login(any()) } doReturn ApiResult.Failure.Server(error = ServerErrorDto(message = INCORRECT_USERNAME_OR_PASSWORD_ERROR_MESSAGE))
            on { user } doReturn stateFlowOf(null)
        }
        vm = loginViewModelFactory(userRepository = mockUserRepo)
        passwordErrorMock = vm.passwordError.mock()
        usernameErrorMock = vm.userNameError.mock()
        vm.email.postValue("test@example.com")
        vm.password.postValue("Password1!")
        vm.onLoginClicked()
        verifyNoMoreInteractions(apiErrorEventMock)
        assertThat(passwordErrorMock.verifyWithCapture(atLeastOnce())).isEqualTo(R.string.password_error)
        assertThat(usernameErrorMock.verifyWithCapture(atLeastOnce())).isNull()
    }

    @Test
    fun loginViewModel_defaultSites_moreThanOne() {
        val mockUserRepo = mock<UserRepository> {
            onBlocking { login(any()) } doReturn ApiResult.Success(Unit)
            on { user } doReturn stateFlowOf(MOCK_MULTI_STORE_USER_MULTI_DEFAULT)
        }
        val vm = loginViewModelFactory(userRepository = mockUserRepo)
        val navigationEventMock = vm.navigationEvent.mock()
        vm.email.postValue("test@example.com")
        vm.password.postValue("Password1!")

        vm.onLoginClicked()
        assertThat(navigationEventMock.verifyWithCapture(atLeastOnce())).isEqualTo(NavigationEvent.Action(R.id.action_loginFragment_to_storesFragment))
    }

    @Test
    fun loginViewModel_onCleared() {
        val vm = loginViewModelFactory()
        assertThat(vm.email.hasObservers()).isTrue()
        assertThat(vm.password.hasObservers()).isTrue()
        vm.runPrivateMethod("onCleared")
        assertThat(vm.email.hasObservers()).isFalse()
        assertThat(vm.password.hasObservers()).isFalse()
    }

    @Test
    fun loginViewModel_onDevOptionsClicked() {
        val vm = loginViewModelFactory()
        vm.onDevOptionsClicked()
        assertThat(vm.navigationEvent.value).isEqualTo(NavigationEvent.Action(R.id.action_loginFragment_to_devOptionsFragment))
    }

    @Test
    fun loginViewModel_imeLoginClick() {
        val networkAvailabilityManagerMock: NetworkAvailabilityManager = mock {
            onBlocking { isConnected } doReturn stateFlowOf(false)
            onBlocking { triggerOfflineError { } } doAnswer { }
        }

        val vm = loginViewModelFactory(networkAvailabilityManager = networkAvailabilityManagerMock)
        vm.imeLoginClick()

        vm.email.postValue("a@b.com")
        vm.password.postValue("abcdefg")
        vm.imeLoginClick()
        runBlocking {
            verify(networkAvailabilityManagerMock, times(1)).triggerOfflineError(any())
        }
    }

    @Test
    fun loginViewModel_setLoginErrors() {
        var didToast = false
        val toasterMock: Toaster = mock {
            on {
                toast(R.string.incorrect_credentials_error, Toast.LENGTH_LONG)
            } doAnswer { didToast = true }
        }
        var vm = loginViewModelFactory(toaster = toasterMock)
        var passwordErrorMock = vm.passwordError.mock()
        var usernameErrorMock = vm.userNameError.mock()
        vm.setLoginErrors()
        assertThat(didToast).isTrue()
        assertThat(passwordErrorMock.verifyWithCapture(atLeastOnce())).isEqualTo(R.string.password_error)
        assertThat(usernameErrorMock.verifyWithCapture(atLeastOnce())).isNull()
        didToast = false

        vm = loginViewModelFactory(toaster = toasterMock)
        passwordErrorMock = vm.passwordError.mock()
        usernameErrorMock = vm.userNameError.mock()
        vm.setLoginErrors(isUserNameError = true)
        assertThat(didToast).isTrue()
        assertThat(passwordErrorMock.verifyWithCapture(atLeastOnce())).isNull()
        assertThat(usernameErrorMock.verifyWithCapture(atLeastOnce())).isEqualTo(R.string.user_name_error)
    }

    @Test
    fun loginViewModel_handshakePointOneOnClick() {
        val vm = loginViewModelFactory()
        vm.handshakePointOneOnClick()
        assertThat(vm.getPrivateProperty("handshakePointOneClicked")).isEqualTo(true)
        assertThat(vm.getPrivateProperty("handshakeExpireTime")).isNotNull()
    }

    @Test
    fun loginViewModel_handshakePointTwoOnClick() {
        val vm = loginViewModelFactory()
        vm.handshakePointTwoOnClick()
        confirmViewModelResetPoints(vm)

        vm.setPrivateProperty("handshakePointOneClicked", true)
        vm.handshakePointTwoOnClick()
        assertThat(vm.getPrivateProperty("handshakePointTwoClicked")).isEqualTo(true)
    }

    @Test
    fun loginViewModel_handshakePointThreeOnClick() {
        val vm = loginViewModelFactory()
        vm.handshakePointThreeOnClick()
        confirmViewModelResetPoints(vm)

        vm.setPrivateProperty("handshakePointOneClicked", true)
        vm.setPrivateProperty("handshakePointTwoClicked", true)
        vm.handshakePointThreeOnClick()
        assertThat(vm.navigationEvent.value).isNull()

        vm.setPrivateProperty("handshakeExpireTime", LocalDateTime.now().plusMinutes(5))
        vm.setPrivateProperty("handshakePointOneClicked", true)
        vm.setPrivateProperty("handshakePointTwoClicked", true)
        vm.handshakePointThreeOnClick()
        assertThat(vm.navigationEvent.value).isEqualTo(NavigationEvent.Action(R.id.action_loginFragment_to_fieldServicesFragment))
    }

    @Test
    fun loginViewModel_resetHandshakePoints() {
        val vm = loginViewModelFactory()
        vm.setPrivateProperty("handshakePointOneClicked", true)
        vm.setPrivateProperty("handshakePointTwoClicked", true)
        vm.setPrivateProperty("handshakePointThreeClicked", true)
        vm.setPrivateProperty("handshakeExpireTime", LocalDateTime.now())
        vm.runPrivateMethod("resetHandshakePoints")
        confirmViewModelResetPoints(vm)
    }

    private fun confirmViewModelResetPoints(vm: LoginViewModel) {
        assertThat(vm.getPrivateProperty("handshakePointOneClicked")).isEqualTo(false)
        assertThat(vm.getPrivateProperty("handshakePointTwoClicked")).isEqualTo(false)
        assertThat(vm.getPrivateProperty("handshakePointThreeClicked")).isEqualTo(false)
        assertThat(vm.getPrivateProperty("handshakeExpireTime")).isNull()
    }
}
