package com.albertsons.acupick.data.environment

import com.albertsons.acupick.data.model.ApsEnvironmentConfig
import com.albertsons.acupick.data.model.AuthEnvironmentConfig
import com.albertsons.acupick.data.model.ConfigEnvironmentConfig
import com.albertsons.acupick.data.model.ComprehensiveEnvironmentConfig
import com.albertsons.acupick.data.model.OsccEnvironmentConfig
import com.albertsons.acupick.data.model.ItemProcessorEnvironmentConfig
import com.albertsons.acupick.data.repository.Repository

/**
 * Provides client environment related values.
 */
interface EnvironmentRepository : Repository {
    /** List of config flag environments. Only contains production on production release build. */
    val configEnvironments: List<ConfigEnvironmentConfig>

    /** Currently selected environment. Always production data on production release build. */
    val selectedConfig: ComprehensiveEnvironmentConfig

    /** List of aps environments. Only contains production on production release build. */
    val apsEnvironments: List<ApsEnvironmentConfig>

    /** List of auth environments. Only contains production on production release build. */
    val authEnvironments: List<AuthEnvironmentConfig>

    /** List of oscc environments. Only contains production on production release build. */
    val osccEnvironments: List<OsccEnvironmentConfig>

    /** List of item processor environments. Only contains production on production release build. */
    val itemProcessorEnvironments: List<ItemProcessorEnvironmentConfig>

    /** Modifies the current config flag environment to [configEnvironmentType]. No-op if called on production release build. */
    fun changeConfigEnvironment(configEnvironmentType: ConfigEnvironmentType)

    /** Modifies the current APS environment to [apsEnvironmentType]. No-op if called on production release build. */
    fun changeApsEnvironment(apsEnvironmentType: ApsEnvironmentType)

    /** Modifies the current OSCC environment to [osccEnvironmentType]. No-op if called on production release build. */
    fun changeOsccEnvironment(osccEnvironmentType: OsccEnvironmentType)

    /** Modifies the current Auth environment to [authEnvironmentType]. No-op if called on production release build. */
    fun changeAuthEnvironment(authEnvironmentType: AuthEnvironmentType)

    /** Modifies the current ItemProcessor environment to [itemProcessorEnvironmentType]. No-op if called on production release build. */
    fun changeItemProcessorEnvironment(itemProcessorEnvironmentType: ItemProcessorEnvironmentType)

    /** Override for forcing environment switcher to use hand entered base URL. */
    fun overrideConfigEnvironment(configOverride: String)

    /** Override for forcing environment switcher to use hand entered base URL. */
    fun overrideApsEnvironment(apsOverride: String)

    /** Override for forcing environment switcher to use hand entered base URL. */
    fun overrideAuthEnvironment(authOverride: String)
    fun overrideOsccEnvironment(osccOverride: String)
    fun overrideItemProcessorEnvironment(itemProcessorOverride: String)

    /** pre override values */
    val preOverrideConfig: ComprehensiveEnvironmentConfig
}
