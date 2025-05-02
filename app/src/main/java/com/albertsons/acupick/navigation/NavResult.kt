package com.albertsons.acupick.navigation

import android.os.Parcelable

/**
 * Encapsulates data to be passed with [NavigationEvent.Back] to the destination on the backstack
 */
data class NavResult(
    val key: String,
    val data: Parcelable,
)
