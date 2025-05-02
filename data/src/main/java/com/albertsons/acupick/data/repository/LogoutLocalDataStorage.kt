package com.albertsons.acupick.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.albertsons.acupick.data.model.request.UserActivityRequestDto
import com.albertsons.acupick.data.model.request.UserActivityRequestDtoJsonAdapter
import com.squareup.moshi.Moshi
import timber.log.Timber

interface LogoutLocalDataStorage {
    fun saveLogoutData(userActivityRequestDto: UserActivityRequestDto)
    fun loadLogoutData(): UserActivityRequestDto?
    fun clear()
}
class LogoutLocalDataStorageImpl(
    moshi: Moshi,
    private val sharedPreferences: SharedPreferences,
) : LogoutLocalDataStorage {

    private val userActivityAdapter = UserActivityRequestDtoJsonAdapter(moshi)

    override fun saveLogoutData(userActivityRequestDto: UserActivityRequestDto) {
        val userActivityRequestDtoString = userActivityRequestDto.toString()
        Timber.d("ACUPICK-1213 saving logout data $userActivityRequestDtoString")
        sharedPreferences.edit {
            val jsonObj = userActivityAdapter.toJson(userActivityRequestDto)
            val key = USER_ACTIVITY_KEY
            Timber.d("[saveLogoutData] key: $key $jsonObj")
            putString(key, jsonObj)
        }
    }

    override fun loadLogoutData(): UserActivityRequestDto? {
        Timber.d("ACUPICK-1213 trying to load stored logout data")
        val key = USER_ACTIVITY_KEY
        if (sharedPreferences.contains(key)) {
            val jsonObj = sharedPreferences.getString(key, String())!!
            Timber.d("[loadLogoutData] key: $key $jsonObj")
            val fromSharedPrefs = userActivityAdapter.fromJson(jsonObj)
            Timber.d("ACUPICK-1213 data loaded fromSharedPrefs $fromSharedPrefs")
            return fromSharedPrefs
        }
        Timber.d("ACUPICK-1213 sharedPrefsDidNotContainKey")
        return null
    }

    override fun clear() = sharedPreferences.edit().clear().apply()

    companion object {
        const val USER_ACTIVITY_KEY = "user_activity_key"
    }
}
