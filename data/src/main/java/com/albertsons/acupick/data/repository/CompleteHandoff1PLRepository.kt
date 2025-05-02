package com.albertsons.acupick.data.repository

import android.content.SharedPreferences
import com.albertsons.acupick.data.model.Complete1PLHandoffData
import com.albertsons.acupick.data.model.Complete1PLHandoffDataJsonAdapter
import com.squareup.moshi.Moshi
import timber.log.Timber

interface CompleteHandoff1PLRepository : Repository {
    fun saveCompleteHandoff(completeHandoffData: Complete1PLHandoffData)
    fun loadCompleteHandoff(): Complete1PLHandoffData?
    fun clear()
}

internal class CompleteHandoff1PLRepositoryImplementation(
    moshi: Moshi,
    private val sharedPrefs: SharedPreferences,
) : CompleteHandoff1PLRepository {

    private val completeHandoffDataAdapter = Complete1PLHandoffDataJsonAdapter(moshi)

    override fun saveCompleteHandoff(completeHandoffData: Complete1PLHandoffData) {
        with(sharedPrefs.edit()) {
            val jsonObj = completeHandoffDataAdapter.toJson(completeHandoffData)
            val key = COMPLETE_HANDOFF_PREFS
            Timber.d("[CompleteHandoff1PLData] key: $key $jsonObj")
            putString(key, jsonObj)
            apply()
        }
    }

    override fun loadCompleteHandoff(): Complete1PLHandoffData? {
        val key = COMPLETE_HANDOFF_PREFS
        if (sharedPrefs.contains(key)) {
            val jsonObj = sharedPrefs.getString(key, String())!!
            Timber.d("[CompleteHandoff1PLData] key: $key $jsonObj")
            return completeHandoffDataAdapter.fromJson(jsonObj)
        }
        return null
    }

    override fun clear() = sharedPrefs.edit().clear().apply()

    companion object {
        const val COMPLETE_HANDOFF_PREFS = "completeHandoff1PLPrefs"
    }
}
