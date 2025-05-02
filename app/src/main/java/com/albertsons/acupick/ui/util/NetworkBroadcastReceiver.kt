package com.albertsons.acupick.ui.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import timber.log.Timber

class NetworkBroadcastReceiver(context: Context) : BroadcastReceiver() {

    interface ConnectionListener {
        fun onNetworkConnectionChanged(isConnected: Boolean)
    }

    private val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val listeners: MutableSet<ConnectionListener> = mutableSetOf()

    override fun onReceive(context: Context, intent: Intent?) {
        listeners.forEach { notifyState(it) }
    }

    private fun notifyState(listener: ConnectionListener) {
        val isConnected = isInternetConnected()
        Timber.v("[notifyState] isConnected=$isConnected")
        listener.onNetworkConnectionChanged(isConnected)
    }

    fun addListener(listener: ConnectionListener) {
        listeners.add(listener)
        notifyState(listener)
    }

    fun removeListener(listener: ConnectionListener) {
        listeners.remove(listener)
    }

    private fun isInternetConnected(): Boolean {
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }
}
