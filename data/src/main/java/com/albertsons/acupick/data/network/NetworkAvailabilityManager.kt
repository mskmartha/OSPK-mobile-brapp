package com.albertsons.acupick.data.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Provides apis to control network state and observe offline errors */
interface NetworkAvailabilityController {
    /** Signals events to show an offline error. Only show events on non-null Unit instances. Should only be called by code directly observing offline errors. */
    val showOfflineError: StateFlow<Unit?>

    /** Updates the current network connection state. Should only be called by code directly managing the network state. */
    suspend fun updateNetworkStatus(connected: Boolean)
}

/** Provides apis to observe current network state and signal offline errors */
interface NetworkAvailabilityManager {
    /** Represents connection state (connected or not). Observe the stream via [Flow.collect] or use first() to get the current value. */
    val isConnected: StateFlow<Boolean>

    var tryAgainLambda: (suspend () -> Unit)?

    /** Sends an event to show an offline error. */
    suspend fun triggerOfflineError(retry: (suspend () -> Unit)?)
}

class NetworkAvailabilityManagerImplementation : NetworkAvailabilityController, NetworkAvailabilityManager {

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected

    private val _showOfflineError = MutableStateFlow<Unit?>(null)
    override val showOfflineError: StateFlow<Unit?>
        get() = _showOfflineError

    private var _tryAgainLambda: (suspend () -> Unit)? = null
    override var tryAgainLambda: (suspend () -> Unit)? = null
        get() = _tryAgainLambda

    override suspend fun updateNetworkStatus(connected: Boolean) {
        _isConnected.value = connected
    }

    override suspend fun triggerOfflineError(retry: (suspend () -> Unit)?) {
        _showOfflineError.value = Unit
        _tryAgainLambda = retry
        delay(CONFLATION_PROPAGATION_DELAY_MS)
        _showOfflineError.value = null
    }
    companion object {
        /**
         * Arbitrary delay value needed to allow collectors to consume the state change.
         *
         * > ... Updates to the value are always conflated. So a slow collector skips fast updates, but always collects the most recently emitted value. ...
         * > Source: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/
         */
        private const val CONFLATION_PROPAGATION_DELAY_MS = 1L
    }
}
