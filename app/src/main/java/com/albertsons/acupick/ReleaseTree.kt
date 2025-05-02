package com.albertsons.acupick

import android.util.Log
import timber.log.Timber

class ReleaseTree : Timber.Tree() {
    fun isLoggable(tag: String?, priority: Int, message: String?): Boolean {
        // Don't log VERBOSE, DEBUG and INFO
        return if (priority != Log.ERROR) {
            false
        } else return message?.contains("FCM") == true

        // Log only ERROR, WARN and WTF and if the message has FCM in it
    }

    protected override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (isLoggable(tag, priority)) {
            Log.e(tag, message.take(500))
        }
    }
}
