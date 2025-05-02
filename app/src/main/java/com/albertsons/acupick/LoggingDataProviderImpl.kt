package com.albertsons.acupick

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings
import com.albertsons.acupick.data.network.logging.LoggingDataProvider

class LoggingDataProviderImpl(
    private val app: Application,
) : LoggingDataProvider {

    override val appVersion: String
        get() = BuildConfig.VERSION_NAME.substringBefore('-')

    override val deviceId: String
        @SuppressLint("HardwareIds")
        get() = Settings.Secure.getString(app.contentResolver, Settings.Secure.ANDROID_ID)

    override var storeId: String = ""
}
