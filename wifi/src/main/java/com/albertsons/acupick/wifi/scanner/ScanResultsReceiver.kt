package com.albertsons.acupick.wifi.scanner

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager

internal interface Callback {
    fun onSuccess()
}

internal class ScanResultsReceiver(private val activity: Activity, private val callback: Callback) :
    BroadcastReceiver() {
    private var registered = false

    fun register() {
        if (!registered) {
            activity.registerReceiver(this, makeIntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
            registered = true
        }
    }

    fun unregister() {
        if (registered) {
            activity.unregisterReceiver(this)
            registered = false
        }
    }

    fun makeIntentFilter(action: String): IntentFilter = IntentFilter(action)

    override fun onReceive(context: Context, intent: Intent) {
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION == intent.action) {
            if (intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)) {
                callback.onSuccess()
            }
        }
    }
}
