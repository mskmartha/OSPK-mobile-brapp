package com.albertsons.acupick

import leakcanary.LeakCanary

fun configureLeakCanary(isEnable: Boolean = false) {
    LeakCanary.config = LeakCanary.config.copy(dumpHeap = isEnable)
    LeakCanary.showLeakDisplayActivityLauncherIcon(isEnable)
}
