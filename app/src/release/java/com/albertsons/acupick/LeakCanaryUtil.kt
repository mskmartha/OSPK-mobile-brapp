package com.albertsons.acupick

import timber.log.Timber

fun configureLeakCanary(isEnable: Boolean = false) {
    Timber.i("Leak Canary is disabled - state isEnabled: $isEnable")
}
