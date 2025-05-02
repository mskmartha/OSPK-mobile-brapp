package com.albertsons.acupick.data.repository

import android.content.SharedPreferences

interface ArrivalsRepository : Repository {
    val is1plDialogShown: Boolean

    fun showDialog()

    fun clear(): Boolean
}

class ArrivalsRepositoryImplementation(
    private val sharedPrefs: SharedPreferences,
) : ArrivalsRepository {

    companion object {
        private const val ARRIVAL_DETAILS = "ArrivalDetails"
    }

    override val is1plDialogShown: Boolean
        get() = sharedPrefs.getBoolean(ARRIVAL_DETAILS, false)

    override fun showDialog() {
        sharedPrefs.edit().putBoolean(ARRIVAL_DETAILS, true).apply()
    }

    override fun clear() = sharedPrefs.edit().clear().commit()
}
