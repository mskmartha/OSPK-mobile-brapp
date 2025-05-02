package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.notification.NotificationType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to the RecordNotificationReq swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class RecordNotificationRequestDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "deviceId") val deviceId: String? = null,
    @Json(name = "notificationType") val notificationType: NotificationType? = null,
    @Json(name = "notificationCounter") val notificationCounter: Int? = null,
    @Json(name = "receivedTimeStamp") val receivedTimeStamp: ZonedDateTime? = null,
    @Json(name = "failureReasonCode") val failureReasonCode: String? = null, // DUG interjection to record exception due to multiple interjection and batch failure
) : Parcelable, Dto
