package com.albertsons.acupick.data.repository

import android.content.SharedPreferences
import com.albertsons.acupick.data.model.CompleteHandoffData
import com.albertsons.acupick.data.model.CompleteHandoffDataJsonAdapter
import com.squareup.moshi.Moshi
import timber.log.Timber

interface CompleteHandoffRepository : Repository {
    fun saveCompleteHandoff(completeHandoffData: CompleteHandoffData)
    fun loadCompleteHandoff(): CompleteHandoffData?
    fun clear()
}

internal class CompleteHandoffRepositoryImplementation(
    moshi: Moshi,
    private val sharedPrefs: SharedPreferences,
) : CompleteHandoffRepository {

    private val completeHandoffDataAdapter = CompleteHandoffDataJsonAdapter(moshi)

    override fun saveCompleteHandoff(completeHandoffData: CompleteHandoffData) {
        with(sharedPrefs.edit()) {
            val jsonObj = completeHandoffDataAdapter.toJson(completeHandoffData)
            val key = COMPLETE_HANDOFF_PREFS
            Timber.d("[userAuthCode] key: $key $jsonObj")
            putString(key, jsonObj)
            apply()
        }
    }

    override fun loadCompleteHandoff(): CompleteHandoffData? {
        val key = COMPLETE_HANDOFF_PREFS
        if (sharedPrefs.contains(key)) {
            val jsonObj = sharedPrefs.getString(key, String())!!
            Timber.d("[userAuthCode] key: $key $jsonObj")
            return completeHandoffDataAdapter.fromJson(jsonObj)
        }
        return null
    }

    override fun clear() = sharedPrefs.edit().clear().apply()

    companion object {
        const val COMPLETE_HANDOFF_PREFS = "completeHandoffPrefs"
    }
}
