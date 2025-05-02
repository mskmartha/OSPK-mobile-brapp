package com.albertsons.acupick.data.network.auth.token

import com.albertsons.acupick.data.repository.CredentialsRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.net.HttpURLConnection

internal class TokenAuthInterceptor(private val tokenAuthService: TokenAuthService, private val credentialsRepo: CredentialsRepository) : Interceptor {

    /**
     * Use to lock critical code blocks that should never be accessed simultaneously (such as refresh token reload read/write).
     * See https://kotlinlang.org/docs/reference/coroutines/shared-mutable-state-and-concurrency.html#mutual-exclusion
     */
    private val refreshTokenMutex = Mutex()

    override fun intercept(chain: Interceptor.Chain): Response {
        return interceptWorker(chain)
    }

    private fun interceptWorker(chain: Interceptor.Chain): Response {
        val accessToken = credentialsRepo.loadToken()?.accessToken
        return if (accessToken.isNullOrEmpty()) {
            chain.proceed(chain.request())
        } else {
            val request = chain.request()
            val newRequest = request.newBuilder()
                .header("Authorization", getTokenAuthHeader(accessToken))
                .build()
            var chainResult = chain.proceed(newRequest)
            // Re-authenticating on a 403 is abnormal but required to be added after discussion with the backend teams ended up the need to support both 401 and 403
            if (chainResult.code == HttpURLConnection.HTTP_UNAUTHORIZED || chainResult.code == HttpURLConnection.HTTP_FORBIDDEN) {
                Timber.v("auth failure - code=${chainResult.code}, message=${chainResult.message}")
                runBlocking {
                    // Prevents reading/writing stale refresh token values when multiple requests are in flight by forcing sequential execution, one by one
                    refreshTokenMutex.withLock {
                        credentialsRepo.loadToken()?.refreshToken?.let { refreshToken ->
                            // Load the refresh token here to guarantee it is the latest value (was previously retrieved from variable captured at function start)
                            val refreshResponse = tokenAuthService.refreshToken(LdapRefreshTokenRequestDto(refreshToken = refreshToken))
                            val newToken = refreshResponse.body()?.toAccessToken()

                            if (newToken == null) {
                                Timber.w("[interceptWorker] unable to retrieve updated access token - logging the user out")
                                refreshResponse.errorBody()?.string()?.also { Timber.w("refreshToken error=$it") }
                                // Cannot include user repository due to circular dependencies so clear out storage here which will be observed by user repository to log the user out
                                credentialsRepo.clearStorage()
                            } else {
                                Timber.v("[interceptWorker] new access token retrieved - repeating previously failed api call")
                                credentialsRepo.storeToken(newToken)

                                // TODO: If we receive another 401/403 on the new call, should we log the user out?
                                val newestRequest = request.newBuilder()
                                    .header("Authorization", getTokenAuthHeader(newToken?.accessToken ?: ""))
                                    .build()
                                chainResult = chain.proceed(newestRequest)
                            }
                        } ?: run { Timber.w("[interceptWorker] refresh token is null - bypass refreshToken api call as well as corresponding api call retry") }
                    }
                }
            }
            chainResult
        }
    }
}

private fun getTokenAuthHeader(token: String): String {
    return "Bearer $token"
}
