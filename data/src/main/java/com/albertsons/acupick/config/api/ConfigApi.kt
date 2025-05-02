package com.albertsons.acupick.config.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * ConfigApi provides a mechanism for fetching primitive server side config values for Android modules.
 * The primitive returned from the accessor methods depends on the existence on the server side config
 * and any constraints that are applied to the flag's value (e.g. min app version)
 *
 * ```
 * can be retrieved via the [Boolean] accessor method:
 * ```
 * configApi.getBoolean("coreXX.booleanConfig")
 * ```
 *
 * ### Observing live changes
 * To observe live changes associated with a config flag/key, use the associated `getXAsFlow` method and supply
 * a default value for when the flag/key does not exist in server side config. The collected value is guaranteed
 * to be distinct until changed.
 *
 * For example:
 * ```
 * configApi.getBooleanAsFlow("coreXX.booleanConfig", false).collect { value ->
 *     // do stuff
 * }
 * ```
 */
interface ConfigApi {

    /**
     * Attempts to get a boolean value from the server side config
     *
     * @param flagKey - identifier for the flag
     * @return flag's value or null if flag key does not exist
     */
    fun getBoolean(flagKey: String): Boolean?

    /**
     * Attempts to get a boolean value from the server side config
     *
     * @param flagKey - identifier for the flag
     * @param default - return value if no flag was found for flagKey
     * @return flag's value or default if flag does not exist
     */
    fun getBoolean(flagKey: String, default: Boolean): Boolean

    /**
     * Gets observable for [Boolean] value changes associated with [flagKey]
     *
     * @param flagKey - identified for the flag
     * @param default - return value if not flag was found for flagKey
     * @return [Flow] for observing flag changes, distinct until changed
     */
    fun getBooleanAsFlow(flagKey: String, default: Boolean): StateFlow<Boolean>
}
