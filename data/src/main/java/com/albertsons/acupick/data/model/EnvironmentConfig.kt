package com.albertsons.acupick.data.model

import com.albertsons.acupick.data.environment.ApsEnvironmentType
import com.albertsons.acupick.data.environment.AuthEnvironmentType
import com.albertsons.acupick.data.environment.ConfigEnvironmentType
import com.albertsons.acupick.data.environment.OsccEnvironmentType
import com.albertsons.acupick.data.environment.ItemProcessorEnvironmentType

data class ConfigEnvironmentConfig(
    val configEnvironmentType: ConfigEnvironmentType,
    /** Base url for the APS environment (APS is the current system providing all of the Albertson's apis) */
    val baseConfigUrl: String,
    val isProd: Boolean
) : DomainModel

/** Contains all environment specific data. Globally unique data should live outside of this config. */
data class ComprehensiveEnvironmentConfig(
    val configEnvironmentConfig: ConfigEnvironmentConfig,
    val apsEnvironmentConfig: ApsEnvironmentConfig,
    val osccEnvironmentConfig: OsccEnvironmentConfig,
    val itemProcessorEnvironmentConfig: ItemProcessorEnvironmentConfig,
    val authEnvironmentConfig: AuthEnvironmentConfig,
) : DomainModel

data class ApsEnvironmentConfig(
    val apsEnvironmentType: ApsEnvironmentType,
    /** Base url for the APS environment (APS is the current system providing all of the Albertson's apis) */
    val baseApsUrl: String,
    val isProd: Boolean
) : DomainModel

data class AuthEnvironmentConfig(
    val authEnvironmentType: AuthEnvironmentType,
    /** Base authentication url for the environment */
    val baseAuthUrl: String,
) : DomainModel

data class OsccEnvironmentConfig(
    val osccEnvironmentType: OsccEnvironmentType,
    val baseOsccUrl: String,
) : DomainModel

data class ItemProcessorEnvironmentConfig(
    val itemProcessorEnvironmentType: ItemProcessorEnvironmentType,
    val baseItemProcessorUrl: String,
) : DomainModel
