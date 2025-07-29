package com.albertsons.acupick.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import timber.log.Timber
import java.time.Duration
import java.time.Instant

interface GamePointsRepository : Repository {

    fun getPoints(): String

    fun canMakeApiCall(): Boolean

    fun setPoints(points: String)

    fun storeTimeStamp()

    fun isFirstTimeLaunch() : Boolean

    fun updateFirstLaunchStatus(isOpen:Boolean)

    fun isShowCaseOnTimerDisplayed() : Boolean

    fun updateShowCaseOnTimerDisplayed(isOpen:Boolean)

}

class GamePointsRepositoryImpl(
    private val sharedPreferences: SharedPreferences,
) : GamePointsRepository {

    override fun getPoints(): String {
        return sharedPreferences.getString(TOTAL_POINTS, null) ?: ""
    }

    override fun canMakeApiCall(): Boolean {
        if (getPoints().isEmpty()) return true
        val lastTimeStampMillis = sharedPreferences.getLong(
            LAST_TIME_STAMP, -1L
        )

        if (lastTimeStampMillis == 0L) {
            return true
        }
        // Convert the stored milliseconds to an Instant
        val lastInstant = Instant.ofEpochMilli(lastTimeStampMillis)

        val currentInstant = Instant.now()

        // Calculate the duration between the two instants
        val duration = Duration.between(lastInstant, currentInstant)

        // Define the minimum required duration (10 minutes)
        val tenMinutes = Duration.ofMinutes(10)

        // Check if the duration is 10 minutes or more
        return duration >= tenMinutes

    }

    override fun setPoints(points: String) {
        sharedPreferences.edit { putString(TOTAL_POINTS, points) }
    }

    override fun storeTimeStamp() {
        val now = Instant.now()
        sharedPreferences.edit { putLong(LAST_TIME_STAMP, now.toEpochMilli()) }
    }

    override fun isFirstTimeLaunch(): Boolean {
        return sharedPreferences.getBoolean(IS_FIRST_LAUNCH_DIALOG, false)
    }

    override fun updateFirstLaunchStatus(isOpen: Boolean) {
        sharedPreferences.edit{
            putBoolean(IS_FIRST_LAUNCH_DIALOG,isOpen)
        }
    }

    override fun isShowCaseOnTimerDisplayed(): Boolean {
        return sharedPreferences.getBoolean(IS_TIMER_SHOW_CASE_ALREADY_DISPLAYED, false)
    }

    override fun updateShowCaseOnTimerDisplayed(isOpen: Boolean) {
        sharedPreferences.edit{
            putBoolean(IS_TIMER_SHOW_CASE_ALREADY_DISPLAYED,isOpen)
        }
    }

    private companion object {
        const val TOTAL_POINTS = "TOTAL_POINTS"
        const val LAST_TIME_STAMP = "LAST_TIME_STAMP"
        const val IS_FIRST_LAUNCH_DIALOG = "IS_FIRST_LAUNCH_DIALOG"
        const val IS_TIMER_SHOW_CASE_ALREADY_DISPLAYED = "IS_TIMER_SHOW_CASE_ALREADY_DISPLAYED"

    }
}