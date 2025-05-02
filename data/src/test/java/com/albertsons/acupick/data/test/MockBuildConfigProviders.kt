package com.albertsons.acupick.data.test

import com.albertsons.acupick.data.buildconfig.ApsRegion
import com.albertsons.acupick.data.buildconfig.BuildConfigProvider

/** Preconfigured [BuildConfigProvider] to simplify usage of common configurations for easier testing  */
object MockBuildConfigProviders {
    val DEV = object : BuildConfigProvider {
        override val isDebugOrInternalBuild = true
        override val isProductionReleaseBuild = !isDebugOrInternalBuild
        override val buildIdentifier = ""
        override val productionApsRegion: ApsRegion? = null
    }

    val PROD_RELEASE = object : BuildConfigProvider {
        override val isDebugOrInternalBuild = false
        override val isProductionReleaseBuild = !isDebugOrInternalBuild
        override val buildIdentifier = ""
        override val productionApsRegion = ApsRegion.West
    }

    val PROD_EAST_RELEASE = object : BuildConfigProvider {
        override val isDebugOrInternalBuild = false
        override val isProductionReleaseBuild = !isDebugOrInternalBuild
        override val buildIdentifier = ""
        override val productionApsRegion = ApsRegion.East
    }
}
