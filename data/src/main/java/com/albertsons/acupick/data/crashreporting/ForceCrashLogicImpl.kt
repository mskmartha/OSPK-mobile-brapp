package com.albertsons.acupick.data.crashreporting

import com.albertsons.acupick.data.buildconfig.BuildConfigProvider

class ForceCrashLogicImpl(private val buildConfigProvider: BuildConfigProvider) : ForceCrashLogic {
    override fun forceCrashOnMatch(input: String?) {
        if (input == CRASH_TRIGGER_TEXT) {
            throw RuntimeException("forcing crash for testing purposes")
        }
    }

    override fun forceCrashNow() {
        if (buildConfigProvider.isDebugOrInternalBuild) {
            forceCrashOnMatch(CRASH_TRIGGER_TEXT)
        }
    }

    companion object {
        private const val CRASH_TRIGGER_TEXT = "crashthisapp!"
    }
}
