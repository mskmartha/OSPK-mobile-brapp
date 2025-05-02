package com.albertsons.acupick.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.albertsons.acupick.data.buildconfig.BuildConfigProvider

/** Write/update dev options apis/values */
interface DevOptionsRepositoryWriter : DevOptionsRepository {
    fun updateUseOnlineInMemoryPickListState(useOnlineInMemoryPickListState: Boolean)
    fun updateUseLeakCanary(useLeakCanary: Boolean)
    fun updateAutoChooseLastSite(autoChooseLastSite: Boolean)
    fun updateAutoLogoutTime(timeInMinutes: Long)
    fun storeSiteId(siteId: String)
}

/** Provides dev options apis/values */
interface DevOptionsRepository : Repository {

    /** True if online in memory pick list state should be used. If false, api calls to reload activity details should be made after appropriate successful pick actions */
    val useOnlineInMemoryPickListState: Boolean
    val useLeakCanary: Boolean
    val autoChooseLastSite: Boolean
    val autoLogoutTime: Long
    val lastSiteId: String
}

internal class DevOptionsRepositoryImplementation(
    private val sharedPrefs: SharedPreferences,
    private val buildConfigProvider: BuildConfigProvider
) : DevOptionsRepository, DevOptionsRepositoryWriter {

    override val useOnlineInMemoryPickListState: Boolean
        get() = if (buildConfigProvider.isProductionReleaseBuild) {
            false // FIXME: Once testing is good, enable this
        } else {
            sharedPrefs.getBoolean(KEY_USE_ONLINE_IN_MEMORY_PICK_LIST_STATE, false) // FIXME: Once testing shows this is good, default to true
        }

    override val useLeakCanary: Boolean
        get() = if (buildConfigProvider.isProductionReleaseBuild) {
            false
        } else {
            sharedPrefs.getBoolean(KEY_USE_LEAK_CANARY, false)
        }

    override val autoChooseLastSite: Boolean
        get() = if (buildConfigProvider.isProductionReleaseBuild) {
            false
        } else {
            sharedPrefs.getBoolean(KEY_AUTO_CHOOSE_LAST_SITE, false)
        }

    override val lastSiteId: String
        get() = sharedPrefs.getString(KEY_LAST_SITE_ID, String())!!

    override val autoLogoutTime: Long
        get() = if (buildConfigProvider.isProductionReleaseBuild) {
            DEFAULT_AUTO_LOUGOUT_TIME
        } else {
            sharedPrefs.getLong(AUTO_LOGOUT_TIME_KEY, DEFAULT_AUTO_LOUGOUT_TIME)
        }

    override fun updateUseOnlineInMemoryPickListState(useOnlineInMemoryPickListState: Boolean) {
        if (buildConfigProvider.isProductionReleaseBuild) {
            // no-op
            return
        }
        sharedPrefs.edit {
            putBoolean(KEY_USE_ONLINE_IN_MEMORY_PICK_LIST_STATE, useOnlineInMemoryPickListState)
        }
    }

    override fun updateUseLeakCanary(useLeakCanary: Boolean) {
        if (buildConfigProvider.isProductionReleaseBuild) {
            // no-op
            return
        }
        sharedPrefs.edit {
            putBoolean(KEY_USE_LEAK_CANARY, useLeakCanary)
        }
    }

    override fun updateAutoChooseLastSite(autoChooseLastSite: Boolean) {
        if (buildConfigProvider.isProductionReleaseBuild) {
            // no-op
            return
        }
        sharedPrefs.edit {
            putBoolean(KEY_AUTO_CHOOSE_LAST_SITE, autoChooseLastSite)
        }
    }

    override fun updateAutoLogoutTime(timeInMinutes: Long) {
        if (buildConfigProvider.isProductionReleaseBuild) {
            // no-op
            return
        }
        sharedPrefs.edit {
            putLong(AUTO_LOGOUT_TIME_KEY, timeInMinutes)
        }
    }

    override fun storeSiteId(siteId: String) {
        sharedPrefs.edit {
            putString(KEY_LAST_SITE_ID, siteId)
        }
    }

    companion object {
        private const val KEY_USE_ONLINE_IN_MEMORY_PICK_LIST_STATE = "use_online_in_memory_pick_list_state"
        private const val KEY_USE_LEAK_CANARY = "use_leak_canary"
        private const val KEY_AUTO_CHOOSE_LAST_SITE = "auto_choose_last_site"
        private const val KEY_LAST_SITE_ID = "last_site_id"
        private const val AUTO_LOGOUT_TIME_KEY = "auto_logout_time"
        private const val DEFAULT_AUTO_LOUGOUT_TIME = 15L
    }
}
