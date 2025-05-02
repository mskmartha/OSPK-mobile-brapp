package com.albertsons.acupick.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.albertsons.acupick.data.model.request.UserActivityRequestDto
import com.albertsons.acupick.data.network.ApsService
import timber.log.Timber
import java.time.Instant

/**
 * Provides apis around the user authentication status.
 */
interface LoginLogoutAnalyticsRepository : Repository {
    suspend fun onLogin(siteId: String)
    suspend fun logLogoutTime(logLogoutDto: UserActivityRequestDto)
    suspend fun sendStoredLogoutData()
    suspend fun sendOrSaveLogoutData(logoutReason: String)
    fun onUserInteraction()
}

internal class LoginLogoutAnalyticsRepositoryImplementation(
    private val apsService: ApsService,
    private val logoutLocalDataStorage: LogoutLocalDataStorage,
    private val sharedPreferences: SharedPreferences,
    private val userRepository: UserRepository,
) : LoginLogoutAnalyticsRepository {

    private var lastInteractionTimestamp: Instant? = null
    private var loginTimestamp: Instant? = null

    init {
        // After an app restart (force close, crash, etc), load last saved timestamp value (if it exists) into lastInteractionEpochMs to support autologout (if criteria are met))
        val lastInteractionEpochMs = sharedPreferences.getLong(KEY_LAST_INTERACTION_TIMESTAMP_EPOCH_MS, TIMESTAMP_NOT_PRESENT_VALUE)
        val loginEpochMs = sharedPreferences.getLong(KEY_LOGIN_TIMESTAMP_EPOCH_MS, TIMESTAMP_NOT_PRESENT_VALUE)
        if (lastInteractionEpochMs != TIMESTAMP_NOT_PRESENT_VALUE) {
            lastInteractionTimestamp = Instant.ofEpochMilli(lastInteractionEpochMs)
            Timber.d("ACUPICK-1213 last interaction timestamp set to $lastInteractionTimestamp")
        }
        if (loginEpochMs != TIMESTAMP_NOT_PRESENT_VALUE) {
            loginTimestamp = Instant.ofEpochMilli(loginEpochMs)
        }
    }

    override fun onUserInteraction() {
        Timber.d("ACUPICK-1213 recordInteractionTimestamp")
        val now = Instant.now()
        lastInteractionTimestamp = now
        sharedPreferences.edit { putLong(KEY_LAST_INTERACTION_TIMESTAMP_EPOCH_MS, now.toEpochMilli()) }
    }

    override suspend fun onLogin(siteId: String) {
        val now = Instant.now()
        loginTimestamp = now
        sharedPreferences.edit { putLong(KEY_LOGIN_TIMESTAMP_EPOCH_MS, now.toEpochMilli()) }
        sendStoredLogoutData()
        val result = apsService.logUserActivityLogin(
            UserActivityRequestDto.generateLoginDto(
                loginTime = now,
                siteId = siteId,
                user = userRepository.user.value
            )
        )
        if (result.isSuccessful) {
            userRepository.user.value?.let { userRepository.updateUser(it.copy(userActivityId = result.body()?.userActivityId)) }
        }
    }

    override suspend fun logLogoutTime(logLogoutDto: UserActivityRequestDto) {
        Timber.d("ACUPICK-1213 in logLogoutTime in LoginLogoutAnalytics $logLogoutDto")
        try {
            val result = apsService.logUserActivityLogout(logLogoutDto)
            if (!result.isSuccessful) {
                logoutLocalDataStorage.saveLogoutData(logLogoutDto)
                Timber.d("ACUPICK-1213 failed to send data saved to local storage $logLogoutDto")
            } else {
                Timber.d("ACUPICK-1213 log out data sent successfully clearing local storage $logLogoutDto")
                logoutLocalDataStorage.clear()
            }
        } catch (e: Exception) {
            Timber.d("ACUPICK-1213 Caught exception trying to send logout data. Saving to local storage $e")
            logoutLocalDataStorage.saveLogoutData(logLogoutDto)
        }
    }

    override suspend fun sendStoredLogoutData() {
        Timber.d("ACUPICK-1213 Trying to send stored logout data in LoginLogoutAnalytics")
        val logData = logoutLocalDataStorage.loadLogoutData()
        if (logData != null) {
            logLogoutTime(logData)
            logoutLocalDataStorage.clear()
        } else {
            Timber.d("ACUPICK-1213 stored logout data was null so not sent")
        }
    }

    override suspend fun sendOrSaveLogoutData(logoutReason: String) {
        Timber.d("ACUPICK-1213 autoLogoutLogin in onLogout")
        Timber.d("ACUPICK-1213 AutoLogoutLogic loginTimestamp: $loginTimestamp")
        Timber.d("ACUPICK-1213 lastInteractionTimestamp: $lastInteractionTimestamp")

        loginTimestamp?.let { loginTime ->
            lastInteractionTimestamp?.let { logoutTime ->
                Timber.d("ACUPICK-1213 AutoLogoutLogic saving data")
                logLogoutTime(
                    UserActivityRequestDto.generateLogoutDto(
                        loginTime = loginTime,
                        logoutTime = logoutTime,
                        user = userRepository.user.value,
                        logoutReason = logoutReason
                    )
                )
                Timber.d("ACUPICK-1213 set lastInteractionTimestamp to null in autologout")
                loginTimestamp = null
                sharedPreferences.edit { remove(KEY_LOGIN_TIMESTAMP_EPOCH_MS) }
                sharedPreferences.edit { remove(KEY_LAST_INTERACTION_TIMESTAMP_EPOCH_MS) }
            }
        }
    }

    companion object {
        private const val KEY_LAST_INTERACTION_TIMESTAMP_EPOCH_MS = "logout_repo_last_interaction_timestamp_epoch_ms"
        private const val KEY_LOGIN_TIMESTAMP_EPOCH_MS = "login_timestamp_epoch_ms"
        private const val TIMESTAMP_NOT_PRESENT_VALUE = -1L
    }
}
