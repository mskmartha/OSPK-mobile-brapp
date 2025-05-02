package com.albertsons.acupick

import com.albertsons.acupick.ui.util.EventAction

interface FirebaseAnalyticsInterface {
    /**
     * Logs an app event to Firebase.
     * @param eventCategory - event category at screen level
     * @param eventAction - event action performed
     * @param eventLabel - label associated to the event
     * @param valuePairList - list of custom variables associated
     */
    fun logEvent(
        eventCategory: String,
        eventAction: EventAction,
        eventLabel: String,
        valuePairList: List<Pair<String, String>>? = null
    )

    /**
     * set user properties for Firebase Analytics
     * @param key - property Key
     * @param value - property value
     */
    fun setUserPropertyValue(key: String, value: String)

    fun setuserId(userId: String)
}
