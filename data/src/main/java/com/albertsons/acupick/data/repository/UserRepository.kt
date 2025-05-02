package com.albertsons.acupick.data.repository

import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.ResponseToApiResultMapper
import com.albertsons.acupick.data.model.User
import com.albertsons.acupick.data.model.ValidCredentialModel
import com.albertsons.acupick.data.model.map
import com.albertsons.acupick.data.model.request.BasicAuthRequestDto
import com.albertsons.acupick.data.model.request.LogoutRequest
import com.albertsons.acupick.data.model.wrapExceptions
import com.albertsons.acupick.data.network.auth.token.TokenAuthService
import com.albertsons.acupick.data.network.logging.LoggingDataProvider
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import retrofit2.Response
import timber.log.Timber

/**
 * Provides apis around the user authentication status.
 */
interface UserRepository : Repository {
    /** Null when signed out. [User] data when signed in. */
    val user: StateFlow<User?>

    /** False when signed out. True when signed in. */
    val isLoggedIn: StateFlow<Boolean>

    suspend fun login(credentials: ValidCredentialModel): ApiResult<Unit>
    suspend fun logout()
    suspend fun updateUser(user: User)
}

internal class UserRepositoryImplementation(
    private val tokenAuthService: TokenAuthService,
    private val configRepository: ConfigRepository,
    private val chatRepository: ConversationsRepository,
    private val credentialsRepository: CredentialsRepository,
    private val pushNotificationsRepository: PushNotificationsRepository,
    private val pickRepository: PickRepository,
    private val itemProcessorRepository: ItemProcessorRepository,
    private val responseToApiResultMapper: ResponseToApiResultMapper,
    private val dispatcherProvider: DispatcherProvider,
    private val loggingDataProvider: LoggingDataProvider,
) : UserRepository {

    private val _user = MutableStateFlow(credentialsRepository.loadUser())

    private val _isLoggedIn = MutableStateFlow(_user.value != null)

    override val user: StateFlow<User?>
        get() = _user

    // Todo switch to StateFlow once we can figure out the correct way that won't lead to user having to login over and over again on relaunch when still within threshold
    override val isLoggedIn: StateFlow<Boolean>
        get() = _isLoggedIn

    init {

        val isUserNull = _user.value == null
        Timber.d("ACUPICK-1371 is user null $isUserNull")
        // TODO: Consider/test out refactoring to use AlbApplicationCoroutineScope instead of GlobalScope
        GlobalScope.launch(dispatcherProvider.Default) {
            credentialsRepository.hasAccessToken.collect { hasAccessToken ->
                if (!hasAccessToken && isLoggedIn.first()) {
                    Timber.d("[init] token not present but user repository still thinks user is logged in - proceed to logout the user")
                    logout()
                }
            }

            _user.collect {
                Timber.d("ACUPICK-1371 _user has changed $it")
            }
        }
        GlobalScope.launch(dispatcherProvider.Default) {
            user.collect {
                loggingDataProvider.storeId = it?.selectedStoreId.orEmpty()
            }
        }
    }

    override suspend fun login(credentials: ValidCredentialModel): ApiResult<Unit> {
        if (isLoggedIn.firstOrNull() == true) {
            return ApiResult.Success(Unit)
        }
        credentialsRepository.storeCredentials(credentials)
        val result = wrapExceptions("UserRepository", "login") {
            tokenAuthService.getToken(BasicAuthRequestDto(credentials.id, credentials.password)).toResult()
        }

        when (result) {
            is ApiResult.Success -> {
                val userData = result.data.user?.toUser()
                _isLoggedIn.value = userData != null
                result.data.toAccessToken().let { token ->
                    credentialsRepository.storeToken(token)
                }
                userData?.let { user ->
                    credentialsRepository.storeUser(user)
                    _user.value = user
                }
            }
            is ApiResult.Failure -> {
                // no-op
            }
        }
        // Throw away success data. Caller should observe user instead if they need user data.
        return result.map { ApiResult.Success(Unit) }
    }

    override suspend fun logout() {
        Timber.d("logout")
        val refreshToken = credentialsRepository.loadToken()?.refreshToken.orEmpty()
        val userId = user.value?.userId.orEmpty()

        // NOTE: Clearing out all internal logged in related data BEFORE making the api call (with cached values) to allow UI/etc to respond as fast as possible on a logout request
        credentialsRepository.clearStorage()
        _user.value = null
        pickRepository.clearAllData()
        itemProcessorRepository.clearItemProcessorData()
        pushNotificationsRepository.clearChatIds()

        // clear chat messages and unsubscribe from call backs and shut down conenction
        chatRepository.shutDownChat()
        _isLoggedIn.value = false

        // Now we make the api call to have the backend also handle user logout (possibly purge tokens for that user on the backend before they actually expire, etc - black box to us)
        if (refreshToken.isNotEmpty() && userId.isNotEmpty()) {
            try {
                tokenAuthService.invalidateToken(LogoutRequest(refreshToken = refreshToken, userId = userId).toLogoutRequestDto())
            } catch (e: Exception) {
                Timber.tag("UserRepository").w(e, "[logout] exception caught: $e")
            }
        }
    }

    override suspend fun updateUser(user: User) {
        user.selectedStoreId?.let { siteId ->
            configRepository.fetchConfigFlagsBySiteId(siteId)
        }
        credentialsRepository.storeUser(user)
        _user.value = user
    }

    private fun <T : Any> Response<T>.toResult(): ApiResult<T> {
        return responseToApiResultMapper.toResult(this)
    }

    private fun <T : Any> Response<T>.toEmptyResult(): ApiResult<Unit> {
        return responseToApiResultMapper.toEmptyResult(this)
    }
}
