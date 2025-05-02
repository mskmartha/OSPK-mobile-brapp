package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.notification.NotificationType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to the notificationReq swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class NotificationRequestDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "deviceId") val deviceId: String? = null,
    @Json(name = "notificationType") val notificationType: NotificationType? = null,
    @Json(name = "notificationCounter") val notificationCounter: Int? = null,
    @Json(name = "acceptedTimeStamp") val acceptedTimeStamp: ZonedDateTime? = null,
    @Json(name = "userAction") val userAction: String? = null,
    @Json(name = "isAutoAssign") val isAutoAssign: Boolean? = null,
    @Json(name = "userId") val userId: String? = null
) : Parcelable, Dto
