package com.albertsons.acupick.data.repository

import android.content.SharedPreferences
import com.albertsons.acupick.data.model.StagingOneData
import com.albertsons.acupick.data.model.StagingOneDataJsonAdapter
import com.albertsons.acupick.data.model.StagingTwoData
import com.albertsons.acupick.data.model.StagingTwoDataJsonAdapter
import com.albertsons.acupick.data.model.WineStagingTwoDataJsonAdapter
import com.squareup.moshi.Moshi
import timber.log.Timber

interface StagingStateRepository : Repository {
    fun saveStagingPartOne(stagingOneData: StagingOneData, activityId: String)
    fun saveStagingPartTwo(stagingTwoData: StagingTwoData, custId: String, activityId: String)

    fun loadStagingPartOne(activityId: String): StagingOneData?
    fun loadStagingPartTwo(custId: String, activityId: String): StagingTwoData?
    fun clear()
}

internal class StagingStateRepositoryImplementation(
    moshi: Moshi,
    private val sharedPrefs: SharedPreferences,
) : StagingStateRepository {

    private enum class StagingType {
        StagingPartOne, StagingPartTwo;

        fun typedPrefKey(key1: String, key2: String = String()): String {
            return "$key1$key2-$name"
        }
    }

    private val stagingPartOneAdapter = StagingOneDataJsonAdapter(moshi)
    private val stagingPartTwoAdapter = StagingTwoDataJsonAdapter(moshi)
    private val wineStagingTwoAdapter = WineStagingTwoDataJsonAdapter(moshi)

    override fun saveStagingPartOne(stagingOneData: StagingOneData, activityId: String) {
        with(sharedPrefs.edit()) {
            val jsonObj = stagingPartOneAdapter.toJson(stagingOneData)
            val key = StagingType.StagingPartOne.typedPrefKey(activityId)
            Timber.d("[saveStagingOne] key: $key $jsonObj")
            putString(key, jsonObj)
            apply()
        }
    }

    override fun saveStagingPartTwo(stagingTwoData: StagingTwoData, custId: String, activityId: String) {
        with(sharedPrefs.edit()) {
            val jsonObj = stagingPartTwoAdapter.toJson(stagingTwoData)
            val key = StagingType.StagingPartTwo.typedPrefKey(custId, activityId)
            Timber.d("[saveStagingTwo] key: $key $jsonObj")
            putString(key, jsonObj)
            apply()
        }
    }

    override fun loadStagingPartOne(activityId: String): StagingOneData? {
        val key = StagingType.StagingPartOne.typedPrefKey(activityId)
        if (sharedPrefs.contains(key)) {
            val jsonObj = sharedPrefs.getString(key, String())!!
            Timber.d("[loadStagingOne] key: $key $jsonObj")
            return stagingPartOneAdapter.fromJson(jsonObj)
        }
        return null
    }

    override fun loadStagingPartTwo(custId: String, activityId: String): StagingTwoData? {
        val key = StagingType.StagingPartTwo.typedPrefKey(custId, activityId)
        if (sharedPrefs.contains(key)) {
            val jsonObj = sharedPrefs.getString(key, String())!!
            Timber.d("[loadStagingTwo] key: $key $jsonObj")
            return stagingPartTwoAdapter.fromJson(jsonObj)
        }
        return null
    }

    override fun clear() = sharedPrefs.edit().clear().apply()
}
