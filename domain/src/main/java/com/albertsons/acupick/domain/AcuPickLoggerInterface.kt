package com.albertsons.acupick.domain

interface AcuPickLoggerInterface {

    /**
     * sets userData for appDynamics
     * @param string value of the key, must be unqiue across the application
     * @param value any however appDynamics only accepts string, boolean, double, date, long, will be converted to string if no match
     * @param debugUserData boolean, auto set to true for debugging
     */
    fun setUserData(key: String, value: Any?, debugUserData: Boolean = true)

    /**
     * log error to Timber and appDynamics
     * ErrorSeverityLevel.CRITICAL
     */
    fun e(error: String)

    /**
     * log verbose to Timber
     * appDynamics does not seem to have any good way to log verbose statements
     */
    fun v(verbose: String)

    /**
     * log warning to Timber and appDynamics
     * ErrorSeverityLevel.WARNING
     */
    fun w(warning: String)

    /**
     * log info to Timber and appDynamics
     * ErrorSeverityLevel.INFO
     */
    fun i(info: String)

    /**
     * log debug to Timber
     * appDynamics does not seem to have any good way to log debug statements
     */
    fun d(debug: String)

    /**
     * start next session in appDynamics
     */
    fun startNextSession()

    /**
     * reporting metric to appDynamics
     * @param value string metric you want to log
     * @param count long metric you want to log, default = 1
     * @param debugMetric boolean for debugging metric
     */
    fun reportMetric(value: String, count: Long = 1, debugMetric: Boolean = false)

    /**
     * logging designed for api calls
     * @param arg1 string value for look up such as package name
     * @param arg2 string value for more refined look up such as function name
     * @param arg3 any value to add such as an argument such as url, request object, etc
     */
    fun beginCall(arg1: String, arg2: String, arg3: Any, debugCall: Boolean = false)

    /**
     * end call logging with or without exception
     * @param error string error that occurred if try catching
     * @param debugCall boolean to debug the end call
     */
    fun endCall(error: String? = null, debugCall: Boolean = false)

    /**
     * start timer for app dynamic for sequences of events
     * @param value string for the timer
     * @param debugTimer debug the timer
     */
    fun startTimer(value: String, debugTimer: Boolean = false)

    /**
     * stop timer for app dynamic for sequences of events
     * @param value string for the timer
     * @param debugTimer debug the timer
     */
    fun stopTimer(value: String, debugTimer: Boolean = false)

    /**
     * set a breadcrumb when you want to capture a series of events
     * @param breadcrumb string for breadcrumb (truncated if over 2048 characters)
     * @param mode integer for mode (CRASHES_ONLY = 0, CRASHES_AND_SESSIONS = 1)
     */
    fun leaveBreadcrumb(breadcrumb: String, mode: Int = 1)
}
