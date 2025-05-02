package com.albertsons.acupick.infrastructure.utils

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * Creates a StateFlow with the initial the given [value]. Added as there doesn't seem to be a similar solution available in the kotlin coroutines std libs. Remove if one becomes available.
 */
fun <T> stateFlowOf(value: T): StateFlow<T> = MutableStateFlow(value).asStateFlow()

fun <T> Fragment.collectFlow(flow: Flow<T>, action: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect(action)
        }
    }
}

fun <T> Fragment.emitToFlow(flow: FlowCollector<T>, data: T) {
    viewLifecycleOwner.lifecycleScope.launch {
        flow.emit(data)
    }
}

// This function is used to combine two flows into a single flow.
// The output flow will emit a value whenever either of the input flows emit a value.
// The output flow will emit the latest value from the first flow and
// the latest value from the second flow whenever either of the input flows emit a value.
// The second flow's value is used only once per change in the first flow.
fun <T1, T2, R> Flow<T1>.combineOncePerChange(
    other: Flow<T2?>,
    transform: suspend (T1, T2) -> R
): Flow<R> = channelFlow {
    var latestValue1: T1? = null
    var latestValue2: T2? = null
    var hasUsedNonNullFromFlow2 = false

    coroutineScope {
        launch {
            other.collect { value2 ->
                if (value2 != latestValue2) {
                    latestValue2 = value2
                    latestValue1?.let { value1 ->
                        if (!hasUsedNonNullFromFlow2 && value2 != null) {
                            send(transform(value1, value2))
                            hasUsedNonNullFromFlow2 = true
                        }
                    }
                }
            }
        }
        launch {
            collect { value1 ->
                if (value1 != latestValue1) {
                    latestValue1 = value1
                    hasUsedNonNullFromFlow2 = false
                    latestValue2?.let { value2 ->
                        send(transform(value1, value2))
                        hasUsedNonNullFromFlow2 = true
                    }
                }
            }
        }
    }
}

fun <T1, R> Flow<T1>.combineOncePerChangeNoneZero(
    other: Flow<Int?>,
    transform: suspend (T1, Int) -> R
): Flow<R> = channelFlow {
    var latestValue1: T1? = null
    var latestValue2: Int? = null
    var hasUsedNonNullFromFlow2 = false

    coroutineScope {
        launch {
            other.collect { value2 ->
                if (value2 != latestValue2) {
                    latestValue2 = value2
                    latestValue1?.let { value1 ->
                        if (!hasUsedNonNullFromFlow2 && value2 != 0 && value2 != null) {
                            send(transform(value1, value2))
                            hasUsedNonNullFromFlow2 = true
                        }
                    }
                }
            }
        }
        launch {
            collect { value1 ->
                if (value1 != latestValue1) {
                    latestValue1 = value1
                    hasUsedNonNullFromFlow2 = false
                    latestValue2?.let { value2 ->
                        if (value2 != 0) {
                            send(transform(value1, value2))
                            hasUsedNonNullFromFlow2 = true
                        }
                    }
                }
            }
        }
    }
}
