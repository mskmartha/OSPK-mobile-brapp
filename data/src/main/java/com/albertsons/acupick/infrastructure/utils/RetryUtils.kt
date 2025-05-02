package com.albertsons.acupick.infrastructure.utils

import com.albertsons.acupick.data.model.ApiResult
import kotlinx.coroutines.delay
import timber.log.Timber

suspend fun <R : Any> retrying(
    tryCnt: Int,
    intervalMillis: () -> Long,
    block: suspend () -> ApiResult<R>,
): ApiResult<R> {
    try {
        val retryCount = tryCnt - 1
        repeat(retryCount) { attempt ->
            try {
                when (val result = block()) {
                    is ApiResult.Success -> {
                        Timber.d("retrying Success")
                        return result
                    }
                    is ApiResult.Failure -> {
                        delay(intervalMillis())
                    }
                }
            } catch (e: Exception) {
                Timber.d("retrying repeat error")
                delay(intervalMillis())
            }
        }
        Timber.d("retrying end")
        return block()
    } catch (e: Exception) {
        return ApiResult.Failure.GeneralFailure(e.localizedMessage.orEmpty())
    }
}

suspend fun <T : Any> commonRetrying(
    block: suspend () -> ApiResult<T>,
): ApiResult<T> = retrying(3, { 1000L }, block)
