package com.albertsons.acupick.data.model

import com.albertsons.acupick.data.model.ApiResult.Failure
import com.albertsons.acupick.data.model.ApiResult.Success
import com.albertsons.acupick.data.model.response.ServerErrorDto
import timber.log.Timber
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketException
import java.net.UnknownHostException

/**
 * Wrapper class for [Success] type that also represent various [Failure]s with appropriate associated data.
 *
 * Note that a loading type at this level is unnecessary. Loading status can be handled at a higher level if needed but hasn't been very useful from past experience.
 */
sealed class ApiResult<out SUCCESS_TYPE : Any> {

    /** Indicates a successful operation (ex: successful api response) */
    data class Success<out SUCCESS_TYPE : Any>(val data: SUCCESS_TYPE) : ApiResult<SUCCESS_TYPE>()

    /** Indicates a failed operation (ex: api response error, parsing error, exception thrown, etc) */
    sealed class Failure : ApiResult<Nothing>() {
        /** Network failures due to network connection issues (no connection, airplane mode, wifi connected with no internet) or connection issues (unknown hostname when not on vpn) */
        sealed class NetworkFailure : Failure() {
            abstract val exception: Exception
            abstract val networkCallName: String

            /** Network issue is likely related to timeout */
            data class Timeout(override val exception: Exception, override val networkCallName: String = "") : NetworkFailure()

            /** Network issue is likely related to some kind of vpn error */
            data class VpnError(override val exception: Exception, override val networkCallName: String = "") : NetworkFailure()
        }

        /** Retrofit+Moshi parse failures or other errors related to the api calls */
        data class Server(val error: ServerErrorDto?, val networkCallName: String = "") : Failure()

        /** General failure type bucket */
        data class GeneralFailure(val message: String, val networkCallName: String = "") : Failure()
    }
}

/**
 * Executes the [transform] block only when receiver [ApiResult.Success] to modify the wrapped value, similar to Collection/List map function.
 *
 * Note that you need to wrap the value you return back from the [transform] block (at the call site) into an [ApiResult].
 * This gives you the ability to change the type from [ApiResult.Success] to [ApiResult.Failure] based on business logic/etc
 * Ex: Mapping a Dto to DomainModel where the mapping cannot be performed due to invalid data, etc should likely return a [ApiResult.Failure]
 *
 * Note that failure has not been thoroughly tested yet but _should_ work.
 */
fun <T : Any, R : Any> ApiResult<T>.map(transform: (T) -> ApiResult<R>): ApiResult<R> {
    return when (this) {
        is ApiResult.Success -> transform(data)
        is ApiResult.Failure -> this
    }
}

fun <T : Any> ApiResult<T>.getOrNull(): T? {
    return when (this) {
        is ApiResult.Success -> this.data
        is ApiResult.Failure -> null
    }
}

/** Converts ApiResult<T> into ApiResult<Unit> to be used when the success return type doesn't matter */
fun <T : Any> ApiResult<T>.asEmptyResult(): ApiResult<Unit> {
    return when (this) {
        is ApiResult.Success -> ApiResult.Success(Unit)
        is ApiResult.Failure -> this
    }
}

/** Syntax sugar to execute [onSuccessBlock] when this is ApiResult.Success */
inline fun <T : Any> ApiResult<T>.alsoOnSuccess(onSuccessBlock: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) {
        onSuccessBlock(data)
    }
    return this
}

inline fun <T : Any> ApiResult<T>.alsoOnFailure(onFailureBlock: (ApiResult<T>) -> Unit): ApiResult<T> {
    if (this is ApiResult.Failure) {
        onFailureBlock(this)
    }
    return this
}

/** Syntax sugar to wrap the receiver [T] into [ApiResult.Success] and support chaining */
fun <T : Any> T.asSuccess(): ApiResult.Success<T> = ApiResult.Success(this)

/** Resolves exceptions to an appropriate ApiResult.Failure subtype */
fun Exception.asAppropriateFailure(netWorkcallName: String = ""): ApiResult.Failure {
    return when {
        // covers SocketTimeoutException and ConnectTimeoutException
        this is InterruptedIOException ||
            // covers multiple applicable exceptions including ConnectException
            this is SocketException -> {
            Failure.NetworkFailure.Timeout(this, netWorkcallName)
        }
        // covers no vpn while device has no network proxy set scenario
        this is UnknownHostException ||
            // covers no vpn while device has network proxy set scenario (java.io.IOException: Unexpected response code for CONNECT: 503)
            (this as? IOException)?.message?.contains("Unexpected response code for CONNECT: 503") == true -> {
            Failure.NetworkFailure.VpnError(this, netWorkcallName)
        }
        else -> Failure.GeneralFailure(localizedMessage.orEmpty(), netWorkcallName)
    }
}

/**
 * Executes [block], converting any caught exceptions to [ApiResult.Failure.GeneralFailure] with logging
 *
 * @param className Name of calling class (for logging purposes)
 * @param methodName Name of calling function (for logging purposes)
 * @param block Lambda with logic that returns the appropriate [ApiResult] response
 *
 */
internal suspend fun <T : Any> wrapExceptions(className: String, methodName: String, block: suspend () -> ApiResult<T>): ApiResult<T> {
    return try {
        Timber.tag(className).v("[$methodName]")
        block().also { Timber.tag(className).v("[$methodName] result=$it") }
    } catch (e: Exception) {
        e.asAppropriateFailure(methodName).also { Timber.tag(className).w(e, "[$methodName wrapExceptions] exception caught and converted to failure: $it") }
    }
}
