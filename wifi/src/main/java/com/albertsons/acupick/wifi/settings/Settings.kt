package com.albertsons.acupick.wifi.settings

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import com.albertsons.acupick.wifi.R
import com.albertsons.acupick.wifi.utils.buildMinVersionQ
import com.albertsons.acupick.wifi.utils.buildVersionP
import com.albertsons.acupick.wifi.utils.findOne
import com.albertsons.acupick.wifi.utils.findSet
import com.albertsons.acupick.wifi.utils.ordinals
import com.albertsons.acupick.wifi.band.WiFiBand
import com.albertsons.acupick.wifi.model.GroupBy
import com.albertsons.acupick.wifi.model.Security
import com.albertsons.acupick.wifi.model.SortBy
import com.albertsons.acupick.wifi.model.Strength

class Settings(private val repository: Repository) {

    fun registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener: OnSharedPreferenceChangeListener): Unit =
        repository.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)

    fun scanSpeed(): Int {
        val defaultValue = repository.stringAsInteger(R.string.scan_speed_default, SCAN_SPEED_DEFAULT)
        val scanSpeed = repository.stringAsInteger(R.string.scan_speed_key, defaultValue)
        return if (versionP()) {
            if (wiFiThrottleDisabled()) scanSpeed.coerceAtLeast(SCAN_SPEED_DEFAULT) else scanSpeed
        } else scanSpeed
    }

    fun wiFiThrottleDisabled(): Boolean {
        if (versionP()) {
            val defaultValue = repository.resourceBoolean(R.bool.wifi_throttle_disabled_default)
            return repository.boolean(R.string.wifi_throttle_disabled_key, defaultValue)
        }
        return false
    }

    fun cacheOff(): Boolean =
        repository.boolean(R.string.cache_off_key, repository.resourceBoolean(R.bool.cache_off_default))

    fun countryCode(): String = repository.string(R.string.country_code_key, "US")

    fun sortBy(): SortBy = find(SortBy.values(), R.string.sort_by_key, SortBy.STRENGTH)

    fun groupBy(): GroupBy = find(GroupBy.values(), R.string.group_by_key, GroupBy.NONE)

    fun wiFiBand(): WiFiBand = find(WiFiBand.values(), R.string.wifi_band_key, WiFiBand.GHZ2)

    fun wiFiOffOnExit(): Boolean =
        if (minVersionQ()) {
            false
        } else {
            repository.boolean(R.string.wifi_off_on_exit_key, repository.resourceBoolean(R.bool.wifi_off_on_exit_default))
        }

    fun keepScreenOn(): Boolean = repository.boolean(R.string.keep_screen_on_key, repository.resourceBoolean(R.bool.keep_screen_on_default))

    fun findSSIDs(): Set<String> = repository.stringSet(R.string.filter_ssid_key, setOf())

    fun saveSSIDs(values: Set<String>): Unit = repository.saveStringSet(R.string.filter_ssid_key, values)

    fun findWiFiBands(): Set<WiFiBand> = findSet(WiFiBand.values(), R.string.filter_wifi_band_key, WiFiBand.GHZ2)

    fun saveWiFiBands(values: Set<WiFiBand>): Unit = saveSet(R.string.filter_wifi_band_key, values)

    fun findStrengths(): Set<Strength> = findSet(Strength.values(), R.string.filter_strength_key, Strength.FOUR)

    fun saveStrengths(values: Set<Strength>): Unit = saveSet(R.string.filter_strength_key, values)

    fun findSecurities(): Set<Security> = findSet(Security.values(), R.string.filter_security_key, Security.NONE)

    fun saveSecurities(values: Set<Security>): Unit = saveSet(R.string.filter_security_key, values)

    private fun <T : Enum<T>> find(values: Array<T>, key: Int, defaultValue: T): T {
        val value = repository.stringAsInteger(key, defaultValue.ordinal)
        return findOne(values, value, defaultValue)
    }

    private fun <T : Enum<T>> findSet(values: Array<T>, key: Int, defaultValue: T): Set<T> {
        val ordinalDefault = ordinals(values)
        val ordinalSaved = repository.stringSet(key, ordinalDefault)
        return findSet(values, ordinalSaved, defaultValue)
    }

    private fun <T : Enum<T>> saveSet(key: Int, values: Set<T>): Unit = repository.saveStringSet(key, ordinals(values))

    fun minVersionQ(): Boolean = buildMinVersionQ()

    fun versionP(): Boolean = buildVersionP()

    companion object {
        private const val SCAN_SPEED_DEFAULT = 300
    }
}
