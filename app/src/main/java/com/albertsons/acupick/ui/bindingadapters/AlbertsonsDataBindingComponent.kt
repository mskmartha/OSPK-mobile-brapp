package com.albertsons.acupick.ui.bindingadapters

import androidx.databinding.DataBindingComponent

/**
 * Approach used to inject values (via koin) into BindingAdapters. Note that this extends [androidx.databinding.DataBindingComponent].
 *
 * Approach modified from https://philio.me/using-android-data-binding-adapters-with-dagger-2/, taking the concepts and applying to Koin :)
 */
class AlbertsonsDataBindingComponent(private val koinBindingAdaptersArg: KoinBindingAdapters) : DataBindingComponent {
    override fun getKoinBindingAdapters(): KoinBindingAdapters = koinBindingAdaptersArg
}
