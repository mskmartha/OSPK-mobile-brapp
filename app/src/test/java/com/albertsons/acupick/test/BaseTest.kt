package com.albertsons.acupick.test

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asFlow
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertWithMessage
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.mockito.kotlin.times
import org.junit.Before
import org.koin.core.context.loadKoinModules
import org.koin.core.scope.Scope
import org.koin.dsl.module
import org.mockito.verification.VerificationMode
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

open class BaseTest {
    @Before
    fun plantTimber() {
        Timber.plant(SystemOutPrintlnTree())
    }

    // /////////////////////////////////////////////////////////////////////////
    // Utils
    // /////////////////////////////////////////////////////////////////////////

    // useful for getting values that are set once not values that change multiple times
    fun <T> LiveData<T>.getOrAwaitValue(
        time: Long = 2,
        timeUnit: TimeUnit = TimeUnit.SECONDS
    ): T {
        var data: T? = null
        val latch = CountDownLatch(1)
        val observer = object : Observer<T> {
            override fun onChanged(value: T) {
                data = value
                latch.countDown()
                this@getOrAwaitValue.removeObserver(this)
            }
        }

        this.observeForever(observer)

        // Don't wait indefinitely if the LiveData is not set.
        if (!latch.await(time, timeUnit)) {
            throw TimeoutException("LiveData value was never set.")
        }

        @Suppress("UNCHECKED_CAST")
        return data as T
    }

    // when flowing as live data this@getOrAwaitValue.removeObserver(this) stops us from actually collecting the data so below is needed
    suspend fun <T> LiveData<T>.getFlowAsLiveDataValue(timeMillis: Long = 2_000): T? =
        withTimeoutOrNull(timeMillis) {
            this@getFlowAsLiveDataValue.asFlow().first()
        }

    // Used to bracket mock block so generic types can be fully inferred.
    protected fun <T> LiveData<T>.mock(): Observer<T> = org.mockito.kotlin.mock { observeForever(mock) }

    // Combine assertThat, custom messaging template, and verifyWithCapture
    // FIXME - This is broken until we make specific overrides per type.  Compiler isn't inferring the correct that()
    protected inline fun <reified T : Any> Observer<T>.assertThatCapturing(mode: VerificationMode = times(1)): Subject {
        val captor = argumentCaptor<T>()
        verify(this, mode).onChanged(captor.capture())
        return assertWithMessage("${this::class.qualifiedName}").that(captor as T)
    }

    // Pulls last value from mocked observable
    protected inline fun <reified T : Any> Observer<T>.verifyWithCapture(mode: VerificationMode = times(1)): T {
        val captor = argumentCaptor<T>()
        verify(this, mode).onChanged(captor.capture())
        return captor.lastValue
    }

    // // Nullable version - Pulls last value from mocked observable
    protected inline fun <reified T : Any> Observer<T?>.verifyWithNullableCapture(mode: VerificationMode = times(1)): T? {
        val captor = argumentCaptor<T>()
        verify(this, mode).onChanged(captor.capture())
        return captor.lastValue
    }

    /* Used to declare a Koin single within a module and load immediately */
    inline fun <reified T> inlineKoinSingle(override: Boolean = false, crossinline block: Scope.() -> T) =
        loadKoinModules(module(override = override) { single { block() } })
}
