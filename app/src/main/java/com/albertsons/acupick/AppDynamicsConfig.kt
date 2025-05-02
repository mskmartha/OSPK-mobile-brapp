package com.albertsons.acupick

import android.app.Application
import com.albertsons.acupick.data.buildconfig.BuildConfigProvider
import com.appdynamics.eumagent.runtime.AgentConfiguration
import com.appdynamics.eumagent.runtime.Instrumentation

class AppDynamicsConfig(private val app: Application, buildConfigProvider: BuildConfigProvider) {

    private val eumAppKey: String = if (buildConfigProvider.isProductionReleaseBuild) {
        EUM_APP_KEY_PRODUCTION
    } else {
        EUM_APP_KEY_TEST
    }

    fun initialize() {
        Instrumentation.start(
            AgentConfiguration.builder()
                .withAppKey(eumAppKey)
                .withContext(app)
                .withCollectorURL(COLLECTOR_URL)
                .withScreenshotURL(SCREENSHOT_URL)
                .withExcludedUrlPatterns(
                    setOf(
                        ".*col.eum-appdynamics.com.*",
                        ".*emucollector.*"
                    )
                )
                .build()
        )
    }

    companion object {
        private const val EUM_APP_KEY_PRODUCTION = "AD-AAB-ABA-CTM"
        private const val EUM_APP_KEY_TEST = "AD-AAB-AAZ-NYD"
        const val COLLECTOR_URL = "https://col.eum-appdynamics.com"
        const val SCREENSHOT_URL = "https://image.eum-appdynamics.com/"
    }
}
