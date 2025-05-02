package com.albertsons.acupick.ui

import android.annotation.SuppressLint
import android.provider.Settings
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.BuildConfig
import com.albertsons.acupick.R
import com.facebook.shimmer.ShimmerFrameLayout

@BindingAdapter("app:showAppVersion")
fun TextView.showAppVersion(showAppVersion: Boolean) {
    check(showAppVersion) { "passing 'false' to showAppVersion is not supported - 'true' must be used" }
    val buildVersion = BuildConfig.VERSION_NAME.substringBefore("-")
    val region = when {
        BuildConfig.USE_PRODUCTION_CANARY == true -> "Canary"
        BuildConfig.USE_PRODUCTION_EAST_REGION == true -> "E"
        else -> "W"
    }
    text = context.getString(R.string.build_version, buildVersion, region)
}

@SuppressLint("HardwareIds")
@BindingAdapter("app:showDeviceId")
fun TextView.showDeviceId(showDeviceId: Boolean) {
    check(showDeviceId) { "passing 'false' to showDeviceId is not supported - 'true' must be used" }
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    text = context.getString(R.string.device_id, deviceId)
}

@BindingAdapter("app:setShimmerStartStop")
fun ShimmerFrameLayout.setShimmerStartStop(isDataLoading: Boolean) {
    if (isDataLoading) {
        startShimmer()
    } else {
        stopShimmer()
    }
}
