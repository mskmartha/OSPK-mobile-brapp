package com.albertsons.acupick.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.Duration
import java.time.Instant

interface GamePointsRepository : Repository {

    fun getPoints(): String

    fun canMakeApiCall(): Boolean

    fun setPoints(points: String)

    fun storeTimeStamp()

}

class GamePointsRepositoryImpl(
    private val sharedPreferences: SharedPreferences,
) : GamePointsRepository {

    override fun getPoints(): String {
        return sharedPreferences.getString(TOTAL_POINTS, null) ?: ""
    }

    override fun canMakeApiCall(): Boolean {
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

    private companion object {
        const val TOTAL_POINTS = "TOTAL_POINTS"
        const val LAST_TIME_STAMP = "LAST_TIME_STAMP"

    }
}