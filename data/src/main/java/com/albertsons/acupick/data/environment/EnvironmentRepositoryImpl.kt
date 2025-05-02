package com.albertsons.acupick.data.environment

import android.content.SharedPreferences
import androidx.core.content.edit
import com.albertsons.acupick.data.buildconfig.ApsRegion
import com.albertsons.acupick.data.buildconfig.BuildConfigProvider
import com.albertsons.acupick.data.model.ApsEnvironmentConfig
import com.albertsons.acupick.data.model.AuthEnvironmentConfig
import com.albertsons.acupick.data.model.ConfigEnvironmentConfig
import com.albertsons.acupick.data.model.ComprehensiveEnvironmentConfig
import com.albertsons.acupick.data.model.OsccEnvironmentConfig
import com.albertsons.acupick.data.model.ItemProcessorEnvironmentConfig
import timber.log.Timber

@Suppress("PrivatePropertyName")
class EnvironmentRepositoryImpl(private val sharedPrefs: SharedPreferences, private val buildConfigProvider: BuildConfigProvider) : EnvironmentRepository {

    private val CONFIG_ENVIRONMENT_QA1 = ConfigEnvironmentConfig(ConfigEnvironmentType.QA1, QA1_BASE_CONFIG_URL, false)
    private val CONFIG_ENVIRONMENT_PROD = ConfigEnvironmentConfig(ConfigEnvironmentType.PROD, PROD_BASE_CONFIG_URL, true)

    private val APS_ENVIRONMENT_DEV = ApsEnvironmentConfig(ApsEnvironmentType.DEV, DEV_BASE_APS_URL, false)
    private val APS_ENVIRONMENT_QA = ApsEnvironmentConfig(ApsEnvironmentType.QA, QA_BASE_APS_URL, false)
    private val APS_ENVIRONMENT_QA3 = ApsEnvironmentConfig(ApsEnvironmentType.QA3, QA3_BASE_APS_URL, false)
    private val APS_ENVIRONMENT_QA7 = ApsEnvironmentConfig(ApsEnvironmentType.QA7, QA7_BASE_APS_URL, false)
    private val APS_ENVIRONMENT_QA2 = ApsEnvironmentConfig(ApsEnvironmentType.QA2, QA2_BASE_APS_URL, false)

    // private val APS_ENVIRONMENT_APIM_QA3_EAST = ApsEnvironmentConfig(ApsEnvironmentType.APIM_QA3_EAST, APIM_QA3_EAST_BASE_APS_URL, false)
    private val APS_ENVIRONMENT_APIM_QA1 = ApsEnvironmentConfig(ApsEnvironmentType.APIM_QA1, APIM_QA1_WEST_BASE_APS_URL, false)
    private val APS_ENVIRONMENT_APIM_QA2 = ApsEnvironmentConfig(ApsEnvironmentType.APIM_QA2, APIM_QA2_WEST_BASE_APS_URL, false)
    private val APS_ENVIRONMENT_APIM_QA3 = ApsEnvironmentConfig(ApsEnvironmentType.APIM_QA3, APIM_QA3_WEST_BASE_APS_URL, false)
    private val APS_ENVIRONMENT_APIM_QA4 = ApsEnvironmentConfig(ApsEnvironmentType.APIM_QA4, APIM_QA4_WEST_BASE_APS_URL, false)
    private val APS_ENVIRONMENT_APIM_QA5 = ApsEnvironmentConfig(ApsEnvironmentType.APIM_QA5, APIM_QA5_WEST_BASE_APS_URL, false)
    private val APS_ENVIRONMENT_APIM_PERF = ApsEnvironmentConfig(ApsEnvironmentType.APIM_PERF, APIM_PERF_WEST_BASE_APS_URL, false)
    private val APS_ENVIRONMENT_CANARY = ApsEnvironmentConfig(ApsEnvironmentType.CANARY, CANARY_BASE_APS_URL, false)
    private val APS_ENVIRONMENT_PROD_CANARY = ApsEnvironmentConfig(ApsEnvironmentType.PRODUCTION_CANARY, PROD_CANARY_BASE_APS_URL, true)
    private val APS_ENVIRONMENT_PROD = ApsEnvironmentConfig(ApsEnvironmentType.PRODUCTION, PROD_BASE_APS_URL, true)
    private val APS_ENVIRONMENT_APIM_PROD = ApsEnvironmentConfig(ApsEnvironmentType.APIM_PRODUCTION, APIM_PROD_BASE_APS_URL, true)
    private val APS_ENVIRONMENT_APIM_PROD_EAST = ApsEnvironmentConfig(ApsEnvironmentType.APIM_PRODUCTION_EAST, APIM_PROD_EAST_BASE_APS_URL, true)

    private val AUTH_ENVIRONMENT_QA = AuthEnvironmentConfig(AuthEnvironmentType.QA, QA_BASE_AUTH_URL)
    private val AUTH_ENVIRONMENT_QA2 = AuthEnvironmentConfig(AuthEnvironmentType.QA2, QA2_BASE_AUTH_URL)
    private val AUTH_ENVIRONMENT_QA3 = AuthEnvironmentConfig(AuthEnvironmentType.QA3, QA3_BASE_AUTH_URL)
    private val AUTH_ENVIRONMENT_QA4 = AuthEnvironmentConfig(AuthEnvironmentType.QA4, QA4_BASE_AUTH_URL)
    private val AUTH_ENVIRONMENT_QA5 = AuthEnvironmentConfig(AuthEnvironmentType.QA5, QA5_BASE_AUTH_URL)
    private val AUTH_ENVIRONMENT_PERF = AuthEnvironmentConfig(AuthEnvironmentType.PERF, PERF_BASE_AUTH_URL)
    // private val AUTH_ENVIRONMENT_PROD = AuthEnvironmentConfig(AuthEnvironmentType.PRODUCTION, PROD_BASE_AUTH_URL)
    private val AUTH_ENVIRONMENT_APIM_PROD = AuthEnvironmentConfig(AuthEnvironmentType.APIM_PRODUCTION, APIM_PROD_BASE_AUTH_URL)
    private val AUTH_ENVIRONMENT_PROD_CANARY = AuthEnvironmentConfig(AuthEnvironmentType.PRODUCTION_CANARY, APIM_PROD_BASE_AUTH_URL_CANARY)

    private val OSCC_ENVIRONMENT_QA = OsccEnvironmentConfig(OsccEnvironmentType.QA, OSCC_QA_BASE_URL)
    private val OSCC_ENVIRONMENT_QA2 = OsccEnvironmentConfig(OsccEnvironmentType.QA2, OSCC_QA2_BASE_URL)
    private val OSCC_ENVIRONMENT_PROD = OsccEnvironmentConfig(OsccEnvironmentType.PRODUCTION, OSCC_PROD_BASE_URL)

    private val ITEM_PROCESSOR_ENVIRONMENT_QA = ItemProcessorEnvironmentConfig(ItemProcessorEnvironmentType.QA, ITEM_PROCESSOR_QA_BASE_URL)
    private val ITEM_PROCESSOR_ENVIRONMENT_QA3 = ItemProcessorEnvironmentConfig(ItemProcessorEnvironmentType.QA3, ITEM_PROCESSOR_QA3_BASE_URL)
    private val ITEM_PROCESSOR_ENVIRONMENT_QA7 = ItemProcessorEnvironmentConfig(ItemProcessorEnvironmentType.QA7, ITEM_PROCESSOR_QA7_BASE_URL)
    private val ITEM_PROCESSOR_ENVIRONMENT_APIM_QA1 = ItemProcessorEnvironmentConfig(ItemProcessorEnvironmentType.APIM_QA1, ITEM_PROCESSOR_APIM_QA1_BASE_URL)
    private val ITEM_PROCESSOR_ENVIRONMENT_APIM_QA2 = ItemProcessorEnvironmentConfig(ItemProcessorEnvironmentType.APIM_QA2, ITEM_PROCESSOR_APIM_QA2_BASE_URL)
    private val ITEM_PROCESSOR_ENVIRONMENT_APIM_QA3 = ItemProcessorEnvironmentConfig(ItemProcessorEnvironmentType.APIM_QA3, ITEM_PROCESSOR_APIM_QA3_BASE_URL)
    private val ITEM_PROCESSOR_ENVIRONMENT_APIM_QA4 = ItemProcessorEnvironmentConfig(ItemProcessorEnvironmentType.APIM_QA4, ITEM_PROCESSOR_APIM_QA4_BASE_URL)
    private val ITEM_PROCESSOR_ENVIRONMENT_APIM_QA5 = ItemProcessorEnvironmentConfig(ItemProcessorEnvironmentType.APIM_QA5, ITEM_PROCESSOR_APIM_QA5_BASE_URL)
    private val ITEM_PROCESSOR_ENVIRONMENT_APIM_PERF = ItemProcessorEnvironmentConfig(ItemProcessorEnvironmentType.APIM_PERF, ITEM_PROCESSOR_APIM_PERF_BASE_URL)
    private val ITEM_PROCESSOR_ENVIRONMENT_PROD_CANARY = ItemProcessorEnvironmentConfig(ItemProcessorEnvironmentType.PRODUCTION_CANARY, ITEM_PROCESSOR_PROD_CANARY_BASE_URL)
    private val ITEM_PROCESSOR_ENVIRONMENT_PROD = ItemProcessorEnvironmentConfig(ItemProcessorEnvironmentType.PRODUCTION, ITEM_PROCESSOR_PROD_BASE_URL)
    private val ITEM_PROCESSOR_ENVIRONMENT_PROD_EAST = ItemProcessorEnvironmentConfig(ItemProcessorEnvironmentType.PRODUCTION_EAST, ITEM_PROCESSOR_PROD_EAST_BASE_URL)

    /** Represents environment to use as a fallback in case any issues (ex: sharedPref loading issue, selected environment no longer in environment list) */
    private val FALLBACK_CONFIG_ENVIRONMENT = CONFIG_ENVIRONMENT_QA1
    private val FALLBACK_APS_ENVIRONMENT = APS_ENVIRONMENT_APIM_QA3
    private val FALLBACK_AUTH_ENVIRONMENT = AUTH_ENVIRONMENT_QA3
    private val FALLBACK_OSCC_ENVIRONMENT = OSCC_ENVIRONMENT_QA
    private val FALLBACK_ITEM_PROCESSOR_ENVIRONMENT = ITEM_PROCESSOR_ENVIRONMENT_QA3

    override val apsEnvironments: List<ApsEnvironmentConfig> =
        if (buildConfigProvider.isProductionReleaseBuild) {
            when (buildConfigProvider.productionApsRegion) {
                ApsRegion.West, null -> listOf(APS_ENVIRONMENT_APIM_PROD)
                ApsRegion.East -> listOf(APS_ENVIRONMENT_APIM_PROD_EAST)
                ApsRegion.Canary -> listOf(APS_ENVIRONMENT_PROD_CANARY)
            }
        } else {
            listOf(
                APS_ENVIRONMENT_DEV,
                APS_ENVIRONMENT_QA,
                APS_ENVIRONMENT_QA3,
                APS_ENVIRONMENT_QA7,
                APS_ENVIRONMENT_QA2,
                APS_ENVIRONMENT_APIM_QA1,
                APS_ENVIRONMENT_APIM_QA2,
                APS_ENVIRONMENT_APIM_QA3,
                APS_ENVIRONMENT_APIM_QA4,
                APS_ENVIRONMENT_APIM_QA5,
                APS_ENVIRONMENT_APIM_PERF,
                APS_ENVIRONMENT_CANARY,
                APS_ENVIRONMENT_PROD_CANARY,
                APS_ENVIRONMENT_PROD,
                APS_ENVIRONMENT_APIM_PROD,
                APS_ENVIRONMENT_APIM_PROD_EAST
            )
        }

    override val authEnvironments: List<AuthEnvironmentConfig> =
        if (buildConfigProvider.isProductionReleaseBuild) {
            when (buildConfigProvider.productionApsRegion) {
                ApsRegion.West, null -> listOf(AUTH_ENVIRONMENT_APIM_PROD)
                ApsRegion.East -> listOf(AUTH_ENVIRONMENT_APIM_PROD)
                ApsRegion.Canary -> listOf(AUTH_ENVIRONMENT_PROD_CANARY)
            }
        } else {
            listOf(
                AUTH_ENVIRONMENT_QA,
                AUTH_ENVIRONMENT_QA2,
                AUTH_ENVIRONMENT_QA3,
                AUTH_ENVIRONMENT_QA4,
                AUTH_ENVIRONMENT_QA5,
                AUTH_ENVIRONMENT_PERF,
                // AUTH_ENVIRONMENT_PROD,
                AUTH_ENVIRONMENT_APIM_PROD,
                AUTH_ENVIRONMENT_PROD_CANARY
            )
        }

    override val osccEnvironments: List<OsccEnvironmentConfig> =
        if (buildConfigProvider.isProductionReleaseBuild) {
            when (buildConfigProvider.productionApsRegion) {
                ApsRegion.West, null -> listOf(OSCC_ENVIRONMENT_PROD)
                ApsRegion.East -> listOf(OSCC_ENVIRONMENT_PROD)
                ApsRegion.Canary -> listOf(OSCC_ENVIRONMENT_PROD)
            }
        } else {
            listOf(
                OSCC_ENVIRONMENT_PROD,
                OSCC_ENVIRONMENT_QA,
                OSCC_ENVIRONMENT_QA2
            )
        }

    override val itemProcessorEnvironments: List<ItemProcessorEnvironmentConfig> =
        if (buildConfigProvider.isProductionReleaseBuild) {
            when (buildConfigProvider.productionApsRegion) {
                ApsRegion.West, null -> listOf(ITEM_PROCESSOR_ENVIRONMENT_PROD)
                ApsRegion.East -> listOf(ITEM_PROCESSOR_ENVIRONMENT_PROD_EAST)
                ApsRegion.Canary -> listOf(ITEM_PROCESSOR_ENVIRONMENT_PROD_CANARY)
            }
        } else {
            listOf(
                ITEM_PROCESSOR_ENVIRONMENT_QA,
                ITEM_PROCESSOR_ENVIRONMENT_QA3,
                ITEM_PROCESSOR_ENVIRONMENT_QA7,
                ITEM_PROCESSOR_ENVIRONMENT_APIM_QA1,
                ITEM_PROCESSOR_ENVIRONMENT_APIM_QA2,
                ITEM_PROCESSOR_ENVIRONMENT_APIM_QA3,
                ITEM_PROCESSOR_ENVIRONMENT_APIM_QA4,
                ITEM_PROCESSOR_ENVIRONMENT_APIM_QA5,
                ITEM_PROCESSOR_ENVIRONMENT_APIM_PERF,
                ITEM_PROCESSOR_ENVIRONMENT_PROD,
                ITEM_PROCESSOR_ENVIRONMENT_PROD_EAST,
                ITEM_PROCESSOR_ENVIRONMENT_PROD_CANARY
            )
        }

    override val configEnvironments: List<ConfigEnvironmentConfig> =
        if (buildConfigProvider.isProductionReleaseBuild) {
            listOf(CONFIG_ENVIRONMENT_PROD)
        } else {
            listOf(
                CONFIG_ENVIRONMENT_QA1,
                CONFIG_ENVIRONMENT_PROD
            )
        }

    override fun changeApsEnvironment(apsEnvironmentType: ApsEnvironmentType) {
        if (buildConfigProvider.isProductionReleaseBuild) {
            // no-op
            return
        }
        Timber.v("[changeApsEnvironment] new apsEnvironmentType=$apsEnvironmentType")
        sharedPrefs.edit {
            putString(KEY_SELECTED_APS_ENVIRONMENT_TYPE, apsEnvironmentType.asEnvironmentPrefValue())
        }
    }

    override fun changeOsccEnvironment(osccEnvironmentType: OsccEnvironmentType) {
        if (buildConfigProvider.isProductionReleaseBuild) {
            // no-op
            return
        }
        Timber.v("[changeOsccEnvironment] new osccEnvironmentType=$osccEnvironmentType")
        sharedPrefs.edit {
            putString(KEY_SELECTED_OSCC_ENVIRONMENT_TYPE, osccEnvironmentType.asEnvironmentPrefValue())
        }
    }

    override fun changeAuthEnvironment(authEnvironmentType: AuthEnvironmentType) {
        if (buildConfigProvider.isProductionReleaseBuild) {
            // no-op
            return
        }
        Timber.v("[changeAuthEnvironment] new authEnvironmentType=$authEnvironmentType")
        sharedPrefs.edit {
            putString(KEY_SELECTED_AUTH_ENVIRONMENT_TYPE, authEnvironmentType.asEnvironmentPrefValue())
        }
    }

    override fun changeItemProcessorEnvironment(itemProcessorEnvironmentType: ItemProcessorEnvironmentType) {
        if (buildConfigProvider.isProductionReleaseBuild) {
            // no-op
            return
        }
        Timber.v("[changeItemProcessorEnvironment] new itemProcessorEnvironmentType=$itemProcessorEnvironmentType")
        sharedPrefs.edit {
            putString(KEY_SELECTED_ITEM_PROCESSOR_ENVIRONMENT_TYPE, itemProcessorEnvironmentType.asEnvironmentPrefValue())
        }
    }

    override fun changeConfigEnvironment(configEnvironmentType: ConfigEnvironmentType) {
        if (buildConfigProvider.isProductionReleaseBuild) {
            // no-op
            return
        }
        Timber.v("[changeConfigEnvironment] new ConfigEnvironmentType=$configEnvironmentType")
        sharedPrefs.edit {
            putString(KEY_SELECTED_CONFIG_ENVIRONMENT_TYPE, configEnvironmentType.asEnvironmentPrefValue())
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Override logic
    // /////////////////////////////////////////////////////////////////////////
    override fun overrideConfigEnvironment(configOverride: String) = sharedPrefs.edit { putString(KEY_OVERRIDE_CONFIG_ENVIRONMENT, ccmOverride) }
    override fun overrideApsEnvironment(apsOverride: String) = sharedPrefs.edit { putString(KEY_OVERRIDE_APS_ENVIRONMENT, apsOverride) }
    override fun overrideAuthEnvironment(authOverride: String) = sharedPrefs.edit { putString(KEY_OVERRIDE_AUTH_ENVIRONMENT, authOverride) }
    override fun overrideOsccEnvironment(osccOverride: String) = sharedPrefs.edit { putString(KEY_OVERRIDE_OSCC_ENVIRONMENT, osccOverride) }
    override fun overrideItemProcessorEnvironment(itemProcessorOverride: String) = sharedPrefs.edit { putString(KEY_OVERRIDE_ITEM_PROCESSOR_ENVIRONMENT, itemProcessorOverride) }

    /**
     * This value is set on class initialization for the lifetime of the application and won't reflect changes made until an app restart.
     * App **MUST** be restarted for environment overrides to take effect
     */
    private val ccmOverride = sharedPrefs.getString(KEY_OVERRIDE_CONFIG_ENVIRONMENT, "")

    /**
     * This value is set on class initialization for the lifetime of the application and won't reflect changes made until an app restart.
     * App **MUST** be restarted for environment overrides to take effect
     */
    private val apsOverride = sharedPrefs.getString(KEY_OVERRIDE_APS_ENVIRONMENT, "")

    /**
     * This value is set on class initialization for the lifetime of the application and won't reflect changes made until an app restart.
     * App **MUST** be restarted for environment overrides to take effect
     */
    private val authOverride = sharedPrefs.getString(KEY_OVERRIDE_AUTH_ENVIRONMENT, "")

    /**
     * This value is set on class initialization for the lifetime of the application and won't reflect changes made until an app restart.
     * App **MUST** be restarted for environment overrides to take effect
     */
    private val osccOverride = sharedPrefs.getString(KEY_OVERRIDE_OSCC_ENVIRONMENT, "")

    /**
     * This value is set on class initialization for the lifetime of the application and won't reflect changes made until an app restart.
     * App **MUST** be restarted for environment overrides to take effect
     */
    private val itemProcessorOverride = sharedPrefs.getString(KEY_OVERRIDE_ITEM_PROCESSOR_ENVIRONMENT, "")
    private fun applyConfigOverride(config: ConfigEnvironmentConfig) =
        when (val override = ccmOverride) {
            null, "" -> config
            else -> ConfigEnvironmentConfig(config.configEnvironmentType, override, config.isProd)
        }

    private fun applyApsOverride(config: ApsEnvironmentConfig) =
        when (val override = apsOverride) {
            null, "" -> config
            else -> ApsEnvironmentConfig(config.apsEnvironmentType, override, config.isProd)
        }

    private fun applyAuthOverride(config: AuthEnvironmentConfig) =
        when (val override = authOverride) {
            null, "" -> config
            else -> AuthEnvironmentConfig(config.authEnvironmentType, override)
        }

    private fun applyOsccOverride(config: OsccEnvironmentConfig) =
        when (val override = osccOverride) {
            null, "" -> config
            else -> OsccEnvironmentConfig(config.osccEnvironmentType, override)
        }

    private fun applyItemProcessorOverride(config: ItemProcessorEnvironmentConfig) =
        when (val override = itemProcessorOverride) {
            null, "" -> config
            else -> ItemProcessorEnvironmentConfig(config.itemProcessorEnvironmentType, override)
        }

    private fun applyOverrides(config: ComprehensiveEnvironmentConfig) =
        ComprehensiveEnvironmentConfig(
            applyConfigOverride(config.configEnvironmentConfig),
            applyApsOverride(config.apsEnvironmentConfig),
            applyOsccOverride(config.osccEnvironmentConfig),
            applyItemProcessorOverride(config.itemProcessorEnvironmentConfig),
            applyAuthOverride(config.authEnvironmentConfig)
        )

    // /////////////////////////////////////////////////////////////////////////

    override val selectedConfig: ComprehensiveEnvironmentConfig
        get() = applyOverrides(preOverrideConfig)

    override val preOverrideConfig: ComprehensiveEnvironmentConfig
        get() = if (buildConfigProvider.isProductionReleaseBuild) {
            when (buildConfigProvider.productionApsRegion) {
                ApsRegion.West, null -> ComprehensiveEnvironmentConfig(
                    configEnvironmentConfig = CONFIG_ENVIRONMENT_PROD,
                    apsEnvironmentConfig = APS_ENVIRONMENT_APIM_PROD,
                    authEnvironmentConfig = AUTH_ENVIRONMENT_APIM_PROD,
                    osccEnvironmentConfig = OSCC_ENVIRONMENT_PROD,
                    itemProcessorEnvironmentConfig = ITEM_PROCESSOR_ENVIRONMENT_PROD
                )
                ApsRegion.East -> ComprehensiveEnvironmentConfig(
                    configEnvironmentConfig = CONFIG_ENVIRONMENT_PROD,
                    apsEnvironmentConfig = APS_ENVIRONMENT_APIM_PROD_EAST,
                    authEnvironmentConfig = AUTH_ENVIRONMENT_APIM_PROD,
                    osccEnvironmentConfig = OSCC_ENVIRONMENT_PROD,
                    itemProcessorEnvironmentConfig = ITEM_PROCESSOR_ENVIRONMENT_PROD_EAST
                )
                ApsRegion.Canary -> ComprehensiveEnvironmentConfig(
                    configEnvironmentConfig = CONFIG_ENVIRONMENT_PROD,
                    apsEnvironmentConfig = APS_ENVIRONMENT_PROD_CANARY,
                    authEnvironmentConfig = AUTH_ENVIRONMENT_PROD_CANARY,
                    osccEnvironmentConfig = OSCC_ENVIRONMENT_PROD,
                    itemProcessorEnvironmentConfig = ITEM_PROCESSOR_ENVIRONMENT_PROD_CANARY
                )
            }
        } else {
            val selectedConfigEnvironmentType = sharedPrefs.getString(KEY_SELECTED_CONFIG_ENVIRONMENT_TYPE, "").orEmpty().asConfigEnvironmentType()
            val selectedApsEnvironmentType = sharedPrefs.getString(KEY_SELECTED_APS_ENVIRONMENT_TYPE, "").orEmpty().asApsEnvironmentType()
            val selectedAuthEnvironmentType = sharedPrefs.getString(KEY_SELECTED_AUTH_ENVIRONMENT_TYPE, "").orEmpty().asAuthEnvironmentType()
            val selectedOsccEnvironmentType = sharedPrefs.getString(KEY_SELECTED_OSCC_ENVIRONMENT_TYPE, "").orEmpty().asOsccEnvironmentType()
            val selectedItemProcessorEnvironmentType = sharedPrefs.getString(KEY_SELECTED_ITEM_PROCESSOR_ENVIRONMENT_TYPE, "").orEmpty().asItemProcessorEnvironmentType()
            val selectedConfigEnvironmentConfig = configEnvironments.firstOrNull { it.configEnvironmentType == selectedConfigEnvironmentType } ?: FALLBACK_CONFIG_ENVIRONMENT
            val selectedApsEnvironmentConfig = apsEnvironments.firstOrNull { it.apsEnvironmentType == selectedApsEnvironmentType } ?: FALLBACK_APS_ENVIRONMENT
            val selectedAuthEnvironmentConfig = authEnvironments.firstOrNull { it.authEnvironmentType == selectedAuthEnvironmentType } ?: FALLBACK_AUTH_ENVIRONMENT
            val selectedOsccEnvironmentConfig = osccEnvironments.firstOrNull { it.osccEnvironmentType == selectedOsccEnvironmentType } ?: FALLBACK_OSCC_ENVIRONMENT
            val selectedItemProcessorEnvironmentConfig = itemProcessorEnvironments.firstOrNull { it.itemProcessorEnvironmentType == selectedItemProcessorEnvironmentType }
                ?: FALLBACK_ITEM_PROCESSOR_ENVIRONMENT
            ComprehensiveEnvironmentConfig(
                configEnvironmentConfig = selectedConfigEnvironmentConfig,
                apsEnvironmentConfig = selectedApsEnvironmentConfig,
                authEnvironmentConfig = selectedAuthEnvironmentConfig,
                osccEnvironmentConfig = selectedOsccEnvironmentConfig,
                itemProcessorEnvironmentConfig = selectedItemProcessorEnvironmentConfig
            )
        }

    private fun ConfigEnvironmentType.asEnvironmentPrefValue(): String {
        return when (this) {
            ConfigEnvironmentType.QA1 -> SP_CONFIG_ENVIRONMENT_VALUE_QA1
            ConfigEnvironmentType.PROD -> SP_CONFIG_ENVIRONMENT_VALUE_PROD
        }
    }

    private fun ApsEnvironmentType.asEnvironmentPrefValue(): String {
        return when (this) {
            ApsEnvironmentType.DEV -> SP_APS_ENVIRONMENT_VALUE_DEV
            ApsEnvironmentType.QA -> SP_APS_ENVIRONMENT_VALUE_QA
            ApsEnvironmentType.QA3 -> SP_APS_ENVIRONMENT_VALUE_QA3
            ApsEnvironmentType.QA7 -> SP_APS_ENVIRONMENT_VALUE_QA7
            ApsEnvironmentType.APIM_QA1 -> SP_APS_ENVIRONMENT_VALUE_APIM_QA1
            ApsEnvironmentType.APIM_QA2 -> SP_APS_ENVIRONMENT_VALUE_APIM_QA2
            ApsEnvironmentType.APIM_QA3 -> SP_APS_ENVIRONMENT_VALUE_APIM_QA3
            ApsEnvironmentType.APIM_QA4 -> SP_APS_ENVIRONMENT_VALUE_APIM_QA4
            ApsEnvironmentType.APIM_QA5 -> SP_APS_ENVIRONMENT_VALUE_APIM_QA5
            ApsEnvironmentType.APIM_PERF -> SP_APS_ENVIRONMENT_VALUE_APIM_PERF
            // ApsEnvironmentType.APIM_QA3_EAST -> SP_APS_ENVIRONMENT_VALUE_APIM_QA3_EAST
            ApsEnvironmentType.CANARY -> SP_APS_ENVIRONMENT_VALUE_CANARY
            ApsEnvironmentType.PRODUCTION_CANARY -> SP_APS_ENVIRONMENT_VALUE_PRODUCTION_CANARY
            ApsEnvironmentType.PRODUCTION -> SP_APS_ENVIRONMENT_VALUE_PRODUCTION
            ApsEnvironmentType.APIM_PRODUCTION -> SP_APS_ENVIRONMENT_VALUE_APIM_PRODUCTION
            ApsEnvironmentType.APIM_PRODUCTION_EAST -> SP_APS_ENVIRONMENT_VALUE_APIM_PRODUCTION_EAST
            ApsEnvironmentType.QA2 -> SP_APS_ENVIRONMENT_VALUE_QA2
        }
    }

    private fun AuthEnvironmentType.asEnvironmentPrefValue(): String {
        return when (this) {
            AuthEnvironmentType.QA -> SP_AUTH_ENVIRONMENT_VALUE_QA
            AuthEnvironmentType.QA2 -> SP_AUTH_ENVIRONMENT_VALUE_QA2
            AuthEnvironmentType.QA3 -> SP_AUTH_ENVIRONMENT_VALUE_QA3
            AuthEnvironmentType.QA4 -> SP_AUTH_ENVIRONMENT_VALUE_QA4
            AuthEnvironmentType.QA5 -> SP_AUTH_ENVIRONMENT_VALUE_QA5
            AuthEnvironmentType.PERF -> SP_AUTH_ENVIRONMENT_VALUE_PERF
            // AuthEnvironmentType.PRODUCTION -> SP_AUTH_ENVIRONMENT_VALUE_PRODUCTION
            AuthEnvironmentType.APIM_PRODUCTION -> SP_AUTH_ENVIRONMENT_VALUE_APIM_PRODUCTION
            AuthEnvironmentType.PRODUCTION_CANARY -> SP_AUTH_ENVIRONMENT_VALUE_APIM_PRODUCTION_CANARY
        }
    }

    private fun OsccEnvironmentType.asEnvironmentPrefValue(): String {
        return when (this) {
            OsccEnvironmentType.QA -> SP_OSCC_ENVIRONMENT_VALUE_QA
            OsccEnvironmentType.QA2 -> SP_OSCC_ENVIRONMENT_VALUE_QA2
            OsccEnvironmentType.PRODUCTION -> SP_OSCC_ENVIRONMENT_VALUE_PROD
        }
    }

    private fun ItemProcessorEnvironmentType.asEnvironmentPrefValue(): String {
        return when (this) {
            ItemProcessorEnvironmentType.QA -> SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_QA
            ItemProcessorEnvironmentType.QA3 -> SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_QA3
            ItemProcessorEnvironmentType.QA7 -> SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_QA7
            ItemProcessorEnvironmentType.APIM_QA1 -> SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_QA1
            ItemProcessorEnvironmentType.APIM_QA2 -> SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_QA2
            ItemProcessorEnvironmentType.APIM_QA3 -> SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_QA3
            ItemProcessorEnvironmentType.APIM_QA4 -> SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_QA4
            ItemProcessorEnvironmentType.APIM_QA5 -> SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_QA5
            ItemProcessorEnvironmentType.APIM_PERF -> SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_PERF
            ItemProcessorEnvironmentType.PRODUCTION -> SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_PROD
            ItemProcessorEnvironmentType.PRODUCTION_CANARY -> SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_PRODUCTION_CANARY
            ItemProcessorEnvironmentType.PRODUCTION_EAST -> SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_PRODUCTION_EAST
        }
    }

    private fun String?.asConfigEnvironmentType(): ConfigEnvironmentType? {
        return when (this) {
            SP_CONFIG_ENVIRONMENT_VALUE_QA1 -> ConfigEnvironmentType.QA1
            SP_CONFIG_ENVIRONMENT_VALUE_PROD -> ConfigEnvironmentType.PROD
            else -> null
        }
    }

    private fun String?.asApsEnvironmentType(): ApsEnvironmentType? {
        return when (this) {
            SP_APS_ENVIRONMENT_VALUE_DEV -> ApsEnvironmentType.DEV
            SP_APS_ENVIRONMENT_VALUE_QA -> ApsEnvironmentType.QA
            SP_APS_ENVIRONMENT_VALUE_QA3 -> ApsEnvironmentType.QA3
            SP_APS_ENVIRONMENT_VALUE_QA7 -> ApsEnvironmentType.QA7
            SP_APS_ENVIRONMENT_VALUE_APIM_QA1 -> ApsEnvironmentType.APIM_QA1
            SP_APS_ENVIRONMENT_VALUE_APIM_QA2 -> ApsEnvironmentType.APIM_QA2
            SP_APS_ENVIRONMENT_VALUE_APIM_QA3 -> ApsEnvironmentType.APIM_QA3
            SP_APS_ENVIRONMENT_VALUE_APIM_QA4 -> ApsEnvironmentType.APIM_QA4
            SP_APS_ENVIRONMENT_VALUE_APIM_QA5 -> ApsEnvironmentType.APIM_QA5
            SP_APS_ENVIRONMENT_VALUE_APIM_PERF -> ApsEnvironmentType.APIM_PERF
            // SP_APS_ENVIRONMENT_VALUE_APIM_QA3_EAST -> ApsEnvironmentType.APIM_QA3_EAST
            SP_APS_ENVIRONMENT_VALUE_QA2 -> ApsEnvironmentType.QA2
            SP_APS_ENVIRONMENT_VALUE_CANARY -> ApsEnvironmentType.CANARY
            SP_APS_ENVIRONMENT_VALUE_PRODUCTION_CANARY -> ApsEnvironmentType.PRODUCTION_CANARY
            SP_APS_ENVIRONMENT_VALUE_PRODUCTION -> ApsEnvironmentType.PRODUCTION
            SP_APS_ENVIRONMENT_VALUE_APIM_PRODUCTION -> ApsEnvironmentType.APIM_PRODUCTION
            SP_APS_ENVIRONMENT_VALUE_APIM_PRODUCTION_EAST -> ApsEnvironmentType.APIM_PRODUCTION_EAST
            else -> null
        }
    }

    private fun String?.asAuthEnvironmentType(): AuthEnvironmentType? {
        return when (this) {
            SP_AUTH_ENVIRONMENT_VALUE_QA -> AuthEnvironmentType.QA
            SP_AUTH_ENVIRONMENT_VALUE_QA2 -> AuthEnvironmentType.QA2
            SP_AUTH_ENVIRONMENT_VALUE_QA3 -> AuthEnvironmentType.QA3
            SP_AUTH_ENVIRONMENT_VALUE_QA4 -> AuthEnvironmentType.QA4
            SP_AUTH_ENVIRONMENT_VALUE_QA5 -> AuthEnvironmentType.QA5
            SP_AUTH_ENVIRONMENT_VALUE_PERF -> AuthEnvironmentType.PERF
            // SP_AUTH_ENVIRONMENT_VALUE_PRODUCTION -> AuthEnvironmentType.PRODUCTION
            SP_AUTH_ENVIRONMENT_VALUE_APIM_PRODUCTION -> AuthEnvironmentType.APIM_PRODUCTION
            SP_AUTH_ENVIRONMENT_VALUE_APIM_PRODUCTION_CANARY -> AuthEnvironmentType.PRODUCTION_CANARY
            else -> null
        }
    }

    private fun String?.asOsccEnvironmentType(): OsccEnvironmentType? {
        return when (this) {
            SP_OSCC_ENVIRONMENT_VALUE_QA -> OsccEnvironmentType.QA
            SP_OSCC_ENVIRONMENT_VALUE_PROD -> OsccEnvironmentType.PRODUCTION
            SP_OSCC_ENVIRONMENT_VALUE_QA2 -> OsccEnvironmentType.QA2
            else -> null
        }
    }

    private fun String?.asItemProcessorEnvironmentType(): ItemProcessorEnvironmentType? {
        return when (this) {
            SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_QA -> ItemProcessorEnvironmentType.QA
            SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_QA3 -> ItemProcessorEnvironmentType.QA3
            SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_QA7 -> ItemProcessorEnvironmentType.QA7
            SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_QA1 -> ItemProcessorEnvironmentType.APIM_QA1
            SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_QA2 -> ItemProcessorEnvironmentType.APIM_QA2
            SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_QA3 -> ItemProcessorEnvironmentType.APIM_QA3
            SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_QA4 -> ItemProcessorEnvironmentType.APIM_QA4
            SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_QA5 -> ItemProcessorEnvironmentType.APIM_QA5
            SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_PERF -> ItemProcessorEnvironmentType.APIM_PERF
            SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_PROD -> ItemProcessorEnvironmentType.PRODUCTION
            SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_PRODUCTION_CANARY -> ItemProcessorEnvironmentType.PRODUCTION_CANARY
            SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_PRODUCTION_EAST -> ItemProcessorEnvironmentType.PRODUCTION_EAST
            else -> null
        }
    }
    companion object {
        private const val KEY_SELECTED_CONFIG_ENVIRONMENT_TYPE = "selected_ccm_environment_type"
        private const val KEY_OVERRIDE_CONFIG_ENVIRONMENT = "override_ccm_environment"
        private const val SP_CONFIG_ENVIRONMENT_VALUE_QA1 = "ccm_env_qa1"
        private const val SP_CONFIG_ENVIRONMENT_VALUE_PROD = "ccm_env_prod"

        private const val KEY_SELECTED_APS_ENVIRONMENT_TYPE = "selected_aps_environment_type"
        private const val KEY_OVERRIDE_APS_ENVIRONMENT = "override_aps_environment"
        private const val SP_APS_ENVIRONMENT_VALUE_DEV = "aps_env_dev"
        private const val SP_APS_ENVIRONMENT_VALUE_QA = "aps_env_qa"
        private const val SP_APS_ENVIRONMENT_VALUE_QA3 = "aps_env_qa3"
        private const val SP_APS_ENVIRONMENT_VALUE_QA7 = "aps_env_qa7"
        private const val SP_APS_ENVIRONMENT_VALUE_APIM_QA1 = "aps_env_apim_qa1"
        private const val SP_APS_ENVIRONMENT_VALUE_APIM_QA2 = "aps_env_apim_qa2"
        private const val SP_APS_ENVIRONMENT_VALUE_APIM_QA3 = "aps_env_apim_qa3"
        private const val SP_APS_ENVIRONMENT_VALUE_APIM_QA4 = "aps_env_apim_qa4"
        private const val SP_APS_ENVIRONMENT_VALUE_APIM_QA5 = "aps_env_apim_qa5"
        private const val SP_APS_ENVIRONMENT_VALUE_APIM_PERF = "aps_env_apim_perf"
        // private const val SP_APS_ENVIRONMENT_VALUE_APIM_QA3_EAST = "aps_env_apim_qa3_east"

        private const val SP_APS_ENVIRONMENT_VALUE_QA2 = "aps_env_qa2"
        private const val SP_APS_ENVIRONMENT_VALUE_CANARY = "aps_env_canary"
        private const val SP_APS_ENVIRONMENT_VALUE_PRODUCTION_CANARY = "aps_env_production_canary"
        private const val SP_APS_ENVIRONMENT_VALUE_PRODUCTION = "aps_env_production"
        private const val SP_APS_ENVIRONMENT_VALUE_APIM_PRODUCTION = "aps_env_apim_production"
        private const val SP_APS_ENVIRONMENT_VALUE_APIM_PRODUCTION_EAST = "aps_env_apim_production_east"

        private const val KEY_SELECTED_AUTH_ENVIRONMENT_TYPE = "selected_auth_environment_type"
        private const val KEY_OVERRIDE_AUTH_ENVIRONMENT = "override_auth_environment"
        private const val SP_AUTH_ENVIRONMENT_VALUE_QA = "auth_env_qa"
        private const val SP_AUTH_ENVIRONMENT_VALUE_QA2 = "auth_env_qa2"
        private const val SP_AUTH_ENVIRONMENT_VALUE_QA3 = "auth_env_qa3"
        private const val SP_AUTH_ENVIRONMENT_VALUE_QA4 = "auth_env_qa4"
        private const val SP_AUTH_ENVIRONMENT_VALUE_QA5 = "auth_env_qa5"
        private const val SP_AUTH_ENVIRONMENT_VALUE_PERF = "auth_env_perf"
        // private const val SP_AUTH_ENVIRONMENT_VALUE_PRODUCTION = "auth_env_production"
        private const val SP_AUTH_ENVIRONMENT_VALUE_APIM_PRODUCTION = "auth_env_apim_production"
        private const val SP_AUTH_ENVIRONMENT_VALUE_APIM_PRODUCTION_CANARY = "auth_env_apim_production_canary"

        private const val KEY_SELECTED_OSCC_ENVIRONMENT_TYPE = "selected_oscc_environment_type"
        private const val KEY_OVERRIDE_OSCC_ENVIRONMENT = "override_oscc_environment"
        private const val SP_OSCC_ENVIRONMENT_VALUE_QA = "oscc_qa"
        private const val SP_OSCC_ENVIRONMENT_VALUE_QA2 = "oscc_qa3"
        private const val SP_OSCC_ENVIRONMENT_VALUE_PROD = "oscc_prod"

        private const val KEY_SELECTED_ITEM_PROCESSOR_ENVIRONMENT_TYPE = "selected_item_processor_environment_type"
        private const val KEY_OVERRIDE_ITEM_PROCESSOR_ENVIRONMENT = "override_item_processor_environment"
        private const val SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_QA = "item_processor_qa"
        private const val SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_QA3 = "item_processor_qa3"
        private const val SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_QA7 = "item_processor_qa7"
        private const val SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_QA1 = "item_processor_apim_qa1"
        private const val SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_QA2 = "item_processor_apim_qa2"
        private const val SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_QA3 = "item_processor_apim_qa3"
        private const val SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_QA4 = "item_processor_apim_qa4"
        private const val SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_QA5 = "item_processor_apim_qa5"
        private const val SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_APIM_PERF = "item_processor_apim_perf"
        private const val SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_PROD = "item_processor_prod"
        private const val SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_PRODUCTION_CANARY = "item_processor_production_canary"
        private const val SP_ITEM_PROCESSOR_ENVIRONMENT_VALUE_PRODUCTION_EAST = "item_processor_production_east"
    }
}

// Base Aps Urls per environment
private const val QA_BASE_APS_URL = "https://ospk.qa1.westus.aks.az.albertsons.com/ospk-services/" // "https://ospk-services-qa.apps.np.stratus.albertsons.com/"
private const val QA3_BASE_APS_URL = "https://ospk.qa3.westus.aks.az.albertsons.com/ospk-services/" // "https://ospk-services-qa3.apps.np.stratus.albertsons.com/"
private const val QA7_BASE_APS_URL = "https://ospk.qa5.westus.aks.az.albertsons.com/ospk-services/" // "https://ospk-services-qa7.apps.np.stratus.albertsons.com/"
private const val QA2_BASE_APS_URL = "https://ospk.qa2.westus.aks.az.albertsons.com/ospk-services/" // "https://ospk-services-acceptance.apps.np.stratus.albertsons.com/"

// Technically is the west url (and the one we used in the beginning for MLP launch)
// private const val APIM_QA3_BASE_APS_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa3int/ospkwu/pickservice" // "https://apim-dev-01.albertsons.com/abs/qa3/pickservice/"

// The west APIM  url for pickservice // TODO we don't the east url for APIM QA3
private const val APIM_QA1_WEST_BASE_APS_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa1int/ospkwu/pickservice/" // "https://apim-dev-01.albertsons.com/abs/qa/pickservice/"
private const val APIM_QA2_WEST_BASE_APS_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa2int/ospkwu/pickservice/" // "https://apim-dev-02.albertsons.com/abs/qa3/pickservice/"
private const val APIM_QA3_WEST_BASE_APS_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa3int/ospkwu/pickservice/" // "https://apim-dev-02.albertsons.com/abs/qa3/pickservice/"
private const val APIM_QA4_WEST_BASE_APS_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa4int/ospkwu/pickservice/"
private const val APIM_QA5_WEST_BASE_APS_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa5int/ospkwu/pickservice/"
private const val APIM_PERF_WEST_BASE_APS_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/perfint/ospkwu/pickservice/"

private const val QA1_BASE_CONFIG_URL = "https://www-qa1.albertsons.com/"
private const val PROD_BASE_CONFIG_URL = "https://www.safeway.com/"

private const val DEV_BASE_APS_URL = "https://ospk.dev.westus.aks.az.albertsons.com/lospk-services/" // "https://osco-pick-services-dev.apps.np.stratus.albertsons.com/"
private const val PROD_BASE_APS_URL = "https://osco-pick-services-prod.apps.prod.stratus.albertsons.com/"

// Canary URLs
private const val CANARY_BASE_APS_URL = "https://apim-dev-01.albertsons.com/abs/perf/pickservicecanary/"
private const val PROD_CANARY_BASE_APS_URL =
    "https://esag-intgw-prod-westus-01.albertsons.com/abs/pilotint/ospkwu/pickservice/" // "https://apim-prod-01.albertsons.com/abs/prod/pickservicecanary/"

// Technically is the west url (and the one we used in the beginning for MLP launch)
private const val APIM_PROD_BASE_APS_URL = "https://esag-intgw-prod-westus-01.albertsons.com/abs/int/ospkwu/pickservice/" // "https://apim-prod-01.albertsons.com/abs/prod/pickservice/"

// The east url
private const val APIM_PROD_EAST_BASE_APS_URL = "https://esag-intgw-prod-eastus-01.albertsons.com/abs/int/ospkeu/pickservice/" // "https://apim-prod-02.albertsons.com/abs/prod/pickservice/"

// Base authentication Urls per environment - Note that authentication is common (across both east/west)
// NOTE: Dev auth requires a different api contract and is not supported in the codebase at this time. Also, authentication endpoints for the QA env won't be used.
private const val QA_BASE_AUTH_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa1int/ospkwu/authservice/"
private const val QA2_BASE_AUTH_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa2int/ospkwu/authservice/ "
private const val QA3_BASE_AUTH_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa3int/ospkwu/authservice/"
private const val QA4_BASE_AUTH_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa4int/ospkwu/authservice/"
private const val QA5_BASE_AUTH_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa5int/ospkwu/authservice/"
private const val PERF_BASE_AUTH_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/perf1int/ospkwu/authservice/"
// private const val PROD_BASE_AUTH_URL = "https://authentication-service-prod.apps.prod.stratus.albertsons.com/"
private const val APIM_PROD_BASE_AUTH_URL = "https://esag-intgw-prod-westus-01.albertsons.com/abs/int/ospkwu/authservice/"
private const val APIM_PROD_BASE_AUTH_URL_CANARY = "https://esag-intgw-prod-westus-01.albertsons.com/abs/pilotint/ospkwu/authservice/"

private const val OSCC_QA_BASE_URL = "https://esap-share-nonprod-apim-01-west-az.albertsons.com/abs/qaint/oscc-processor/"
private const val OSCC_QA2_BASE_URL = "https://esap-share-nonprod-apim-01-west-az.albertsons.com/abs/acceptanceint/oscc-processor/"
private const val OSCC_PROD_BASE_URL = "https://esap-apim-prod-01.albertsons.com/abs/int/oscc-processor/"

private const val ITEM_PROCESSOR_QA_BASE_URL = "https://ospk.qa1.westus.aks.az.albertsons.com/ospk-item-processor/" // "https://ospk-item-processor-qa.apps.np.stratus.albertsons.com/"
private const val ITEM_PROCESSOR_QA3_BASE_URL = "https://ospk.qa3.westus.aks.az.albertsons.com/ospk-item-processor/" // "https://ospk-item-processor-qa3.apps.np.stratus.albertsons.com/"
private const val ITEM_PROCESSOR_QA7_BASE_URL = "https://ospk.qa5.westus.aks.az.albertsons.com/ospk-item-processor/" // "https://ospk-item-processor-qa7.apps.np.stratus.albertsons.com/"
private const val ITEM_PROCESSOR_APIM_QA1_BASE_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa1int/ospkwu/pickitemprocessor/"
// "https://apim-dev-01.albertsons.com/abs/qa/ospk-item-processor/"
private const val ITEM_PROCESSOR_APIM_QA2_BASE_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa2int/ospkwu/pickitemprocessor/"
private const val ITEM_PROCESSOR_APIM_QA3_BASE_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa3int/ospkwu/pickitemprocessor/"
// "https://apim-dev-02.albertsons.com/abs/qa3/ospk-item-processor/"
private const val ITEM_PROCESSOR_APIM_QA4_BASE_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa4int/ospkwu/pickitemprocessor/"
private const val ITEM_PROCESSOR_APIM_QA5_BASE_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/qa5int/ospkwu/pickitemprocessor/"
private const val ITEM_PROCESSOR_APIM_PERF_BASE_URL = "https://esag-intgw-nonprod-westus-01.albertsons.com/abs/perfint/ospkwu/pickitemprocessor/"
private const val ITEM_PROCESSOR_CANARY_BASE_URL = "https://apim-dev-01.albertsons.com/abs/perf/ospk-item-processorcanary/"
private const val ITEM_PROCESSOR_PROD_CANARY_BASE_URL =
    "https://esag-intgw-prod-westus-01.albertsons.com/abs/pilotint/ospkwu/pickitemprocessor/" // "https://apim-prod-01.albertsons.com/abs/prod/ospk-item-processorcanary/"

// Technically is the west url (and the one we used in the beginning for MLP launch)
private const val ITEM_PROCESSOR_PROD_BASE_URL =
    "https://esag-intgw-prod-westus-01.albertsons.com/abs/int/ospkwu/pickitemprocessor/" // "https://apim-prod-01.albertsons.com/abs/prod/ospk-item-processor/"

// The east url
private const val ITEM_PROCESSOR_PROD_EAST_BASE_URL = "https://esag-intgw-prod-eastus-01.albertsons.com/abs/int/ospkeu/pickitemprocessor/" //  "https://apim-prod-02.albertsons
// .com/abs/prod/ospk-item-processor/"
