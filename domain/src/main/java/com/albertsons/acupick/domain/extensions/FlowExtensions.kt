package com.albertsons.acupick.domain.extensions

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow

/**
 *  Throttle first will emit the first item it receives and then wil not emit anything until after the period expires.
 */

fun <T> Flow<T>.throttleFirst(periodMillis: Long): Flow<T> {
    require(periodMillis > 0) { "period should be positive" }
    return flow {
        var lastTime = 0L
        collect { value ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTime >= periodMillis) {
                lastTime = currentTime
                emit(value)
            }
        }
    }
}

/**
 *  Throttle last will emit the first and last elements within a time period and ignore those in between.
 *
 *  This is especially useful for debouncing API and hitting final state.
 */

fun <T> Flow<T>.throttleLatest(periodMillis: Long): Flow<T> {
    require(periodMillis > 0) { "period should be positive" }

    return channelFlow {
        var lastTime = 0L

        // collectLatest will cancel previous actions if they are still running
        collectLatest {
            System.currentTimeMillis().let { currentTime ->
                // Delay until periodMillis has elapsed since last emit
                (currentTime - lastTime).let { delta ->
                    if (delta < periodMillis) delay(periodMillis - delta)
                }

                send(it)
                lastTime = currentTime
            }
        }
    }
}
