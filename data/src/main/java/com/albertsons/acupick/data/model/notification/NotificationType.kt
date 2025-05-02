package com.albertsons.acupick.data.model.notification

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class NotificationType(vararg val values: String) {
    ARRIVED("ARRIVED", "STORE-NOTIFIED"),
    ARRIVING("GEO-FENCE-BROKEN"),
    DISMISS("DISMISS"),
    PICKING("PICKING"),
    ARRIVED_INTERJECTION("ARRIVED_INTERJECTION"),
    ARRIVED_INTERJECTION_ALL_USER("ARRIVED_INTERJECTION_ALL_USER"),
    CHAT("CHAT"),
    CHAT_PICKER("CHAT_PICKER"),
    UNKNOWN("");

    companion object {
        fun getByValue(value: String?): NotificationType {
            for (notificationType in values()) {
                if (notificationType.values.contains(value?.uppercase())) {
                    return notificationType
                }
            }
            return UNKNOWN
        }
    }
}
