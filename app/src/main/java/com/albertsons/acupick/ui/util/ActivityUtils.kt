package com.albertsons.acupick.ui.util

import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import com.albertsons.acupick.ui.MainActivity

internal fun MainActivity.keepScreenOn() =
    if (settings.keepScreenOn()) {
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        this.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

internal fun makeIntent(action: String): Intent = Intent(action)

@TargetApi(Build.VERSION_CODES.Q)
internal fun MainActivity.startWiFiSettings() =
    this.startActivityForResult(makeIntent(Settings.Panel.ACTION_WIFI), 0)

internal fun MainActivity.startLocationSettings() =
    this.startActivity(makeIntent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
