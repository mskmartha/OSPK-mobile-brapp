package com.albertsons.acupick.data.repository

import android.content.SharedPreferences
import com.albertsons.acupick.data.model.response.WineStagingData
import com.albertsons.acupick.data.model.response.WineStagingDataJsonAdapter
import com.squareup.moshi.Moshi
import timber.log.Timber

interface WineShippingStageStateRepository : Repository {
    fun saveStagingPartOne(stagingOneData: WineStagingData, activityId: String)
    fun loadStagingPartOne(activityId: String): WineStagingData?
    fun clear()
}

internal class WineShippingStageStateRepositoryImplementation(
    moshi: Moshi,
    private val sharedPrefs: SharedPreferences,
) : WineShippingStageStateRepository {

    private enum class StagingType {
        StagingPartOne, StagingPartTwo, StagingPartThree;

        fun typedPrefKey(key1: String, key2: String = String()): String {
            return "$key1$key2-$name"
        }
    }

    private val stagingPartOneAdapter = WineStagingDataJsonAdapter(moshi)
    private val stagingPartTwoAdapter = WineStagingDataJsonAdapter(moshi)

    override fun saveStagingPartOne(stagingOneData: WineStagingData, activityId: String) {
        with(sharedPrefs.edit()) {
            val jsonObj = stagingPartOneAdapter.toJson(stagingOneData)
            val key = StagingType.StagingPartOne.typedPrefKey(activityId)
            Timber.d("[saveStagingOne] key: $key")
            putString(key, jsonObj)
            apply()
        }
    }

    override fun loadStagingPartOne(activityId: String): WineStagingData? {
        val key = StagingType.StagingPartOne.typedPrefKey(activityId)
        if (sharedPrefs.contains(key)) {
            val jsonObj = sharedPrefs.getString(key, String())!!
            Timber.d("[loadStagingOne] key: $key")
            return stagingPartOneAdapter.fromJson(jsonObj)
        }
        return null
    }

    override fun clear() = sharedPrefs.edit().clear().apply()
}
