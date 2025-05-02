package com.albertsons.acupick.data.autologout

import android.content.SharedPreferences
import androidx.core.content.edit
import com.albertsons.acupick.data.model.request.UserActivityRequestDto
import com.albertsons.acupick.data.repository.DevOptionsRepositoryWriter
import com.albertsons.acupick.data.repository.LoginLogoutAnalyticsRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.infrastructure.utils.prettyPrint
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.Duration
import java.time.Instant

/** Defines how and when auto logout applies. Actual logout and navigation to be handled elsewhere. */
interface AutoLogoutLogic {
    /**
     * Call when user interacts with app (likely Activity.onUserInteraction).
     *
     * Necessary to track active user interactions to support autologout (when criteria are met) in the event of:
     * 1. an app crash, force close, or app kill by system in the background *then*
     * 2. app re-launch
     */
    fun onUserInteraction()

    /** Call when application is paused. */
    fun applicationPaused()

    /** Call when application is resumed. The [autoLogoutBlock] is only invoked if the auto logout criteria has been met. Proceed to log the user out and perform navigation back to login. */
    suspend fun performAutoLogoutOnApplicationResumed(autoLogoutBlock: () -> Unit)
}

class AutoLogoutLogicImpl(
    private val userRepository: UserRepository,
    private val loginAnalyticsRepository: LoginLogoutAnalyticsRepository,
    devOptionsRepositoryWriter: DevOptionsRepositoryWriter,
    private val sharedPreferences: SharedPreferences
) : AutoLogoutLogic {

    private var lastInteractionTimestamp: Instant? = null
    private val autoLogoutTime = Duration.ofMinutes(devOptionsRepositoryWriter.autoLogoutTime)

    init {
        // After an app restart (force close, crash, etc), load last saved timestamp value (if it exists) into lastInteractionEpochMs to support autologout (if criteria are met))
        val lastInteractionEpochMs = sharedPreferences.getLong(KEY_LAST_INTERACTION_TIMESTAMP_EPOCH_MS, TIMESTAMP_NOT_PRESENT_VALUE)
        if (lastInteractionEpochMs != TIMESTAMP_NOT_PRESENT_VALUE) {
            lastInteractionTimestamp = Instant.ofEpochMilli(lastInteractionEpochMs)
            Timber.d("ACUPICK-1213 last interaction timestamp set to $lastInteractionTimestamp")
            sharedPreferences.edit { remove(KEY_LAST_INTERACTION_TIMESTAMP_EPOCH_MS) }
        }
    }

    override fun onUserInteraction() {
        recordInteractionTimestamp()
    }

    override fun applicationPaused() {
        recordInteractionTimestamp()
    }

    override suspend fun performAutoLogoutOnApplicationResumed(autoLogoutBlock: () -> Unit) {
        if (userRepository.isLoggedIn.first() && isPastAutoLogoutThreshold()) {
            loginAnalyticsRepository.sendOrSaveLogoutData(UserActivityRequestDto.ACTIVITY_LOGOUT_REASON_APP_TIMEOUT)
            Timber.d("ACUPICK-1213 before autoLogoutBlock")
            autoLogoutBlock()
            Timber.d("ACUPICK-1213 in logout MainActivityViewModel")
        }
        Timber.d("ACUPICK-1213 set lastInteractionTimestamp to null in applicationResumed AutoLoginLogic")
        lastInteractionTimestamp = null
    }

    private fun recordInteractionTimestamp() {
        Timber.d("ACUPICK-1213 recordInteractionTimestamp")
        val now = Instant.now()
        lastInteractionTimestamp = now
        sharedPreferences.edit { putLong(KEY_LAST_INTERACTION_TIMESTAMP_EPOCH_MS, now.toEpochMilli()) }
    }

    private fun isPastAutoLogoutThreshold(): Boolean {
        Timber.d("ACUPICK-1213 is in isPastAutoLogoutThreshold ")
        if (lastInteractionTimestamp == null) return false

        val now = Instant.now()
        val autoLogoutThreshold = lastInteractionTimestamp?.plus(autoLogoutTime)
        val timeLeftBeforeAutoLogout = Duration.between(now, autoLogoutThreshold).prettyPrint()
        Timber.v("[isPastAutoLogoutThreshold] \nnow                =$now, \nautoLogoutThreshold=$autoLogoutThreshold, \ntime left before auto logout=$timeLeftBeforeAutoLogout")
        val shouldAutoLogout = now.isAfter(autoLogoutThreshold)
        Timber.d("ACUPICK-1213 isPastAutoLogoutThreshold value: $shouldAutoLogout")
        return shouldAutoLogout
    }

    companion object {
        private const val KEY_LAST_INTERACTION_TIMESTAMP_EPOCH_MS = "last_interaction_timestamp_epoch_ms"
        private const val TIMESTAMP_NOT_PRESENT_VALUE = -1L
    }
}
