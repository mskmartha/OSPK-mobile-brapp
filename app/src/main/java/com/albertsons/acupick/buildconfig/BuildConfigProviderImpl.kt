package com.albertsons.acupick.buildconfig

import com.albertsons.acupick.BuildConfig
import com.albertsons.acupick.data.buildconfig.ApsRegion
import com.albertsons.acupick.data.buildconfig.BuildConfigProvider

class BuildConfigProviderImpl : BuildConfigProvider {
    override val isDebugOrInternalBuild: Boolean
        get() = isDebugOrInternalBuild()
    override val isProductionReleaseBuild: Boolean
        get() = isProductionReleaseBuild()
    override val buildIdentifier: String
        get() = BuildConfig.BUILD_IDENTIFIER
    override val productionApsRegion: ApsRegion?
        get() = when {
            BuildConfig.USE_PRODUCTION_CANARY -> ApsRegion.Canary
            BuildConfig.USE_PRODUCTION_EAST_REGION -> ApsRegion.East
            else -> ApsRegion.West
        }
}

private fun isProductionReleaseBuild() = !BuildConfig.DEBUG && BuildConfig.PRODUCTION

private fun isDebugOrInternalBuild() = BuildConfig.DEBUG || BuildConfig.INTERNAL
