package com.albertsons.acupick.data.model.notification

import android.content.Intent
import android.os.Parcelable
import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.infrastructure.utils.getFormattedWithAmPm
import com.albertsons.acupick.infrastructure.utils.toZonedDateTimeFromFormattedUtcTime
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

const val NOTIFICATION_DATA = "notificationData"
const val FINAL_NOTIFICATION_COUNTER = 3

@Parcelize
data class NotificationData(
    val actId: Long?,
    val customerFirstName: String?,
    val customerLastName: String?,
    val customerOrderNumber: String?,
    val fulfillmentType: FulfillmentType,
    val notificationCounter: Int?,
    val notificationType: NotificationType,
    val serviceLevel: OrderType,
    val siteId: Long?,
    val stageByTime: String?,
    val isShowingHybridMessage: Boolean,
    val customerName: String?,
    val pickerName: String?,
    val pickerJoined: Boolean?,
    val customerMessage: String?,
    val customerArrivedTime: ZonedDateTime?,
) : Parcelable

fun Map<String, String>.toNotificationData() = NotificationData(
    actId = this["actId"]?.toLongOrNull(),
    customerFirstName = this["customerFirstName"],
    customerLastName = this["customerLastName"],
    customerOrderNumber = this["orderNumber"],
    fulfillmentType = enumValueOrNull(this["fulfillmentType"]) ?: FulfillmentType.DUG,
    notificationCounter = this["notificationCounter"]?.toIntOrNull(),
    notificationType = NotificationType.getByValue(this["notificationType"]),
    serviceLevel = enumValueOrNull(this["serviceLevel"]) ?: OrderType.REGULAR,
    siteId = this["siteId"]?.toLongOrNull(),
    stageByTime = getFormattedWithAmPm(
        try {
            ZonedDateTime.parse(this["stageByTime"])
        } catch (ex: Exception) {
            try {
                this["stageByTime"]?.toZonedDateTimeFromFormattedUtcTime()
            } catch (ex: Exception) {
                null
            }
        }
    ),
    isShowingHybridMessage = this["showHybridMessage"]?.toBoolean() ?: false,
    customerArrivedTime = try {
        ZonedDateTime.parse(this["customerArrivedTime"])
    } catch (ex: Exception) {
        null
    },
    customerName = this["customerName"],
    pickerName = this["pickerName"],
    customerMessage = this["customerMessage"],
    pickerJoined = this["pickerJoined"]?.toBoolean() ?: false
)

fun Intent.getNotificationData() = extras?.get(NOTIFICATION_DATA) as? NotificationData

fun NotificationData.asFirstInitialDotLastString() =
    "${customerFirstName?.take(1)}. $customerLastName"

fun NotificationData.asFirstNameLastInitialDotString() =
    "${customerFirstName?.replaceFirstChar{ if (it.isLowerCase()) it.uppercase() else it.toString() }} ${customerLastName?.take(1)?.uppercase()}."

fun NotificationData.isFinalNotification() = notificationCounter ?: 0 >= FINAL_NOTIFICATION_COUNTER

fun NotificationData.getNotificationReceivedCount() = if (notificationType == NotificationType.DISMISS) 0 else notificationCounter

// TODO: DUG2.0 Once customerArrivalTime available in notification payload will remove the below block of code.
/**
 * This method perform check wheather we required customerArrivedTime from API response or not.
 *
 */
fun NotificationData.isCustomerArrivalTimeRequired(): Boolean {
    return notificationType == NotificationType.ARRIVED_INTERJECTION && customerArrivedTime == null
}
fun NotificationData.isCustomerArrivalTimeRequiredForAllUser(): Boolean {
    return notificationType == NotificationType.ARRIVED_INTERJECTION_ALL_USER && customerArrivedTime == null
}
fun NotificationData.isChatNotification(): Boolean {
    return notificationType == NotificationType.CHAT
}

fun NotificationData.isChatPickerNotification(): Boolean {
    return notificationType == NotificationType.CHAT_PICKER
}

inline fun <reified T : Enum<*>> enumValueOrNull(name: String?): T? =
    T::class.java.enumConstants.firstOrNull { it.name == name }
