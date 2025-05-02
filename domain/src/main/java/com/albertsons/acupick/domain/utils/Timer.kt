package com.albertsons.acupick.domain.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Create Flow that emits every N milliseconds
fun timer(interval: Long): Flow<Unit> = flow {
    do {
        emit(Unit)
        delay(interval)
    } while (true)
}
