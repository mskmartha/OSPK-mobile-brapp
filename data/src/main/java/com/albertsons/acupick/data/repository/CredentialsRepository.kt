package com.albertsons.acupick.data.repository

import androidx.security.crypto.EncryptedSharedPreferences
import com.albertsons.acupick.data.model.User
import com.albertsons.acupick.data.model.ValidCredentialModel
import com.albertsons.acupick.data.network.auth.token.AccessToken
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber.Forest.e

internal class CredentialsRepository(private val encryptedSharedPrefs: EncryptedSharedPreferences, private val moshi: Moshi) {
    companion object {
        private const val CREDENTIALS = "Credentials"
        private const val TOKEN = "Token"
        private const val USER = "User"
    }

    private val CREDENTIAL_ADAPTER: JsonAdapter<ValidCredentialModel> by lazy { moshi.adapter(ValidCredentialModel::class.java) }
    private val TOKEN_ADAPTER: JsonAdapter<AccessToken> by lazy { moshi.adapter(AccessToken::class.java) }
    private val USER_ADAPTER: JsonAdapter<User> by lazy { moshi.adapter(User::class.java) }

    private val _hasAccessToken: MutableStateFlow<Boolean> = MutableStateFlow(loadToken() != null)
    /** True when access token is present (assume logged in) and false when it is not present (assume logged out) */
    val hasAccessToken: StateFlow<Boolean>
        get() = _hasAccessToken

    fun clearStorage() {
        encryptedSharedPrefs.edit().clear().apply()
        _hasAccessToken.value = false
    }

    fun storeCredentials(credentials: ValidCredentialModel) {
        encryptedSharedPrefs.edit().putString(CREDENTIALS, CREDENTIAL_ADAPTER.toJson(credentials)).apply()
    }

    fun loadCredentials(): ValidCredentialModel? {
        val credentialsJson = encryptedSharedPrefs.getString(CREDENTIALS, null)
        return if (credentialsJson.isNotNullOrEmpty()) {
            try {
                CREDENTIAL_ADAPTER.fromJson(credentialsJson)
            } catch (e: JsonDataException) {
                e("[loadCredentials] Error when de-serializing credentials from JSON")
                null
            }
        } else {
            e("[loadCredentials] Credentials repository could not load credentials")
            null
        }
    }

    fun storeToken(token: AccessToken) {
        encryptedSharedPrefs.edit().putString(TOKEN, TOKEN_ADAPTER.toJson(token)).apply()
        _hasAccessToken.value = true
    }

    fun loadToken(): AccessToken? {
        val tokenJson = encryptedSharedPrefs.getString(TOKEN, null)
        return if (tokenJson.isNotNullOrEmpty()) {
            try {
                TOKEN_ADAPTER.fromJson(tokenJson)
            } catch (e: JsonDataException) {
                e("[loadToken] Error when de-serializing token from JSON")
                null
            }
        } else {
            e("[loadToken] Credentials repository could not load token")
            null
        }
    }
    fun storeUser(user: User) {
        encryptedSharedPrefs.edit().putString(USER, USER_ADAPTER.toJson(user)).apply()
    }

    fun loadUser(): User? {
        val userJson = encryptedSharedPrefs.getString(USER, null)
        return if (userJson.isNotNullOrEmpty()) {
            try {
                USER_ADAPTER.fromJson(userJson)
            } catch (e: JsonDataException) {
                e("[loadUser] Error when de-serializing user from JSON")
                null
            }
        } else {
            e("[loadUser] Credentials repository could not load user")
            null
        }
    }
}
