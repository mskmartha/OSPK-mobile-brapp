package com.albertsons.acupick

import com.albertsons.acupick.domain.AcuPickLoggerInterface
import com.appdynamics.eumagent.runtime.CallTracker
import com.appdynamics.eumagent.runtime.ErrorSeverityLevel
import com.appdynamics.eumagent.runtime.Instrumentation
import timber.log.Timber
import java.util.Date

class AcuPickLoggerImpl(appDynamicsConfig: AppDynamicsConfig) : AcuPickLoggerInterface {

    private var callTracker: CallTracker? = null

    init {
        appDynamicsConfig.initialize()
    }

    override fun setUserData(key: String, value: Any?, debugUserData: Boolean) {
        if (value == null) {
            Timber.w("setUserData- value given was null")
            return
        }

        if (debugUserData) {
            Timber.d("setUserData- key:$key value:$value")
        }
        when (value) {
            is String -> Instrumentation.setUserData(key, value)
            is Boolean -> Instrumentation.setUserDataBoolean(key, value)
            is Double -> Instrumentation.setUserDataDouble(key, value)
            is Date -> Instrumentation.setUserDataDate(key, value)
            is Long -> Instrumentation.setUserDataLong(key, value)
            is Int -> Instrumentation.setUserDataLong(key, value.toLong())
            else -> Instrumentation.setUserData(key, value.toString())
        }
    }

    override fun e(error: String) {
        Timber.e(error)
        Instrumentation.reportError(Throwable(message = error), ErrorSeverityLevel.CRITICAL)
    }

    override fun w(warning: String) {
        Timber.w(warning)
        Instrumentation.reportError(Throwable(message = warning), ErrorSeverityLevel.WARNING)
    }

    override fun i(info: String) {
        Timber.i(info)
        Instrumentation.reportError(Throwable(message = info), ErrorSeverityLevel.INFO)
    }

    override fun v(verbose: String) {
        Timber.v(verbose)
    }

    override fun d(debug: String) {
        Timber.d(debug)
    }

    override fun startNextSession() {
        Instrumentation.startNextSession()
    }

    override fun reportMetric(value: String, count: Long, debugMetric: Boolean) {
        if (debugMetric) {
            Timber.d("reportMetric- value:$value count:$count")
        }
        Instrumentation.reportMetric(value, count)
    }

    override fun beginCall(arg1: String, arg2: String, arg3: Any, debugCall: Boolean) {
        if (debugCall) {
            Timber.d("beginCall- arg1: $arg1\n arg2:$arg2\n arg3:$arg3")
        }
        callTracker = Instrumentation.beginCall(arg1, arg2).withArguments(arg3)
    }

    override fun endCall(error: String?, debugCall: Boolean) {
        if (debugCall) {
            Timber.d("endCall- error:$error")
        }
        if (error != null) {
            callTracker?.reportCallEndedWithException(Exception(error))
        } else {
            callTracker?.reportCallEnded()
        }
        callTracker = null
    }

    override fun startTimer(value: String, debugTimer: Boolean) {
        if (debugTimer) {
            Timber.d("startTimer- value:$value")
        }
        Instrumentation.startTimer(value)
    }

    override fun stopTimer(value: String, debugTimer: Boolean) {
        if (debugTimer) {
            Timber.d("endTimer- value:$value")
        }
        Instrumentation.stopTimer(value)
    }

    override fun leaveBreadcrumb(breadcrumb: String, mode: Int) {
        Instrumentation.leaveBreadcrumb(breadcrumb, mode)
    }
}
