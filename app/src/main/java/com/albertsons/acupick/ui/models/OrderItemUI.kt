package com.albertsons.acupick.ui.models

import android.os.Parcelable
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.CustomerArrivalStatus
import com.albertsons.acupick.data.model.ErOrderStatus
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.VanStatus
import com.albertsons.acupick.data.model.response.ErDto
import com.albertsons.acupick.data.model.response.OnePlDto
import com.albertsons.acupick.data.model.response.asFirstInitialDotLastString
import com.albertsons.acupick.infrastructure.utils.getFormattedWithAmPm
import com.albertsons.acupick.infrastructure.utils.getFormattedWithoutAmPm
import com.albertsons.acupick.ui.util.asFirstInitialDotLastString
import com.albertsons.acupick.ui.util.getOrEmpty
import com.albertsons.acupick.ui.util.toFulfillmentTypeUI
import kotlinx.android.parcel.Parcelize
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

// /////////////////////////////////////////////////////////////////////////
// UI Models
// /////////////////////////////////////////////////////////////////////////
@Parcelize
@Suppress("DataClassPrivateConstructor")
data class OrderItemUI private constructor(
    val actId: Long?,
    val siteId: String?,
    val orderNumber: String,
    val name: String,
    val contactPersonId: String,
    val nameShort: String?,
    val pickerName: String?,
    val pickerId: String?,
    val fulfillment: FulfillmentTypeUI?,
    val windowStartTime: ZonedDateTime?,
    val windowEndTime: ZonedDateTime?,
    val expectedComplete: ZonedDateTime?,
    val isYesterdaysOrder: Boolean,
    val showStatusAlert: Boolean,
    val toteCount: String?,
    val bagCount: String?,
    val looseItemCount: String?,
    val staged: Boolean,
    val erId: Long?,
    val shortOrderNumber: String,
    val stopNumber: String,
    val priority: OrderPriority,
    val orderStatus: OrderStatus?,
    val customerArrivalStatus: CustomerArrivalStatusUI?,
    val customerArrivalTime: ZonedDateTime?,
    val orderType: OrderTypeUI?,
    val source: String?,
    val fulfillmentResId: Int?,
    val isPartnerPick: Boolean?,
    val containsRegulatedItem: Boolean?,
    val isMultiSource: Boolean?,
    val feScreenStatus: String?,
    val orderCount: String? = null,
    val rejectedItemsCount: Int? = null,
    val vanStatus: VanStatus? = null,
    val vanArrivalTime: ZonedDateTime? = null,
    val nameFull: String? = null
) : UIModel, Parcelable {
    // TODO - Move this to converter - Need to consider this a bit before making refactor
    constructor(dto: ErDto) : this(
        actId = dto.pickupActId,
        siteId = dto.siteId,
        orderNumber = dto.customerOrderNumber ?: "",
        name = "${dto.contactPersonDto?.firstName} ${dto.contactPersonDto?.lastName}".trim(),
        nameShort = dto.contactPersonDto?.asFirstInitialDotLastString()?.trim(),
        pickerName = dto.assignedTo.asFirstInitialDotLastString()?.trim(),
        contactPersonId = dto.contactPersonDto?.id.orEmpty(),
        pickerId = dto.assignedTo?.userId,
        fulfillment = dto.fulfillment?.toFulfillmentTypeUI(),
        windowStartTime = dto.fulfillmentSlotDto?.startTime,
        windowEndTime = dto.fulfillmentSlotDto?.endTime,
        expectedComplete = dto.expectedCompletedDate,
        showStatusAlert = when (dto.status) {
            ErOrderStatus.NEW, ErOrderStatus.RELEASED, ErOrderStatus.READY -> isOrderWithinHighPriorityTimeThreshold(dto)
            else -> false
        },
        toteCount = dto.toteCount.getOrEmpty(),
        bagCount = dto.bagCount.getOrEmpty(),
        looseItemCount = dto.looseItemCount.getOrEmpty(),
        staged = dto.status == ErOrderStatus.DROPPED_OFF,
        erId = dto.erId,
        shortOrderNumber = dto.shortOrderNumber ?: "",
        stopNumber = dto.stopNumber ?: "",
        priority = when (dto.status) {
            ErOrderStatus.ASSIGNED, ErOrderStatus.PACKED -> OrderPriority.MEDIUM
            ErOrderStatus.NEW, ErOrderStatus.RELEASED, ErOrderStatus.READY -> {
                if (isOrderWithinHighPriorityTimeThreshold(dto)) OrderPriority.CRITICAL else OrderPriority.LOW
            }
            ErOrderStatus.DROPPED_OFF -> OrderPriority.HIGH
            else -> OrderPriority.LOW
        },
        isYesterdaysOrder = dto.isYesterday ?: false,
        orderStatus = when (dto.status) {
            ErOrderStatus.ASSIGNED, ErOrderStatus.PACKED -> OrderStatus.IN_PROGRESS
            ErOrderStatus.NEW, ErOrderStatus.RELEASED, ErOrderStatus.READY -> {
                if (dto.expectedCompletedDate?.withZoneSameInstant(ZoneId.systemDefault()) != null) {
                    OrderStatus.UNASSIGNED
                } else {
                    OrderStatus.UNAVAILABLE
                }
            }
            ErOrderStatus.DROPPED_OFF -> OrderStatus.READY
            else -> null
        },
        customerArrivalStatus = convertArrivalStatus(status = dto.subStatus, isUnclaimedOrder = dto.assignedTo == null),
        customerArrivalTime = dto.nextActExpStartTime,
        orderType = when (dto.orderType) {
            OrderType.FLASH -> OrderTypeUI.FLASH
            OrderType.FLASH3P -> OrderTypeUI.FLASH3P
            OrderType.EXPRESS -> OrderTypeUI.EXPRESS
            else -> OrderTypeUI.REGULAR
        },
        source = dto.source ?: dto.partnerName,
        fulfillmentResId = dto.fulfillment?.toFulfillmentTypeUI()?.asNameRes(),
        isPartnerPick = dto.orderType == OrderType.FLASH3P,
        containsRegulatedItem = dto.containsRegulatedItem,
        isMultiSource = dto.isMultiSource,
        feScreenStatus = dto.feScreenStatus,
        nameFull = "${dto.contactPersonDto?.firstName.orEmpty()} ${dto.contactPersonDto?.lastName.orEmpty()}"
    )

    constructor(dto: OnePlDto, selectedSiteId: String?) : this(
        actId = dto.activityId,
        customerArrivalTime = dto.plannedDepartureTime,
        source = dto.vanNumber,
        orderNumber = "${dto.activityId}",
        fulfillment = FulfillmentTypeUI.ONEPL,
        siteId = selectedSiteId,
        name = dto.vanNumber?.let { "Van $it Driver" }.orEmpty(),
        nameShort = dto.assignedUser?.asFirstInitialDotLastString()?.trim(),
        pickerName = dto.assignedUser.asFirstInitialDotLastString()?.trim(),
        contactPersonId = "",
        pickerId = dto.assignedUser?.userId,
        windowStartTime = null,
        windowEndTime = null,
        expectedComplete = null,
        showStatusAlert = false,
        toteCount = "",
        bagCount = "",
        looseItemCount = "",
        staged = false,
        erId = null,
        shortOrderNumber = "",
        stopNumber = "",
        priority = OrderPriority.LOW,
        isYesterdaysOrder = false,
        orderStatus = null,
        customerArrivalStatus = convertArrivalStatus(status = dto.status, isUnclaimedOrder = dto.assignedUser == null),
        orderType = OrderTypeUI.REGULAR,
        fulfillmentResId = FulfillmentTypeUI.ONEPL.asNameRes(),
        isPartnerPick = false,
        containsRegulatedItem = null,
        isMultiSource = null,
        feScreenStatus = null,
        orderCount = dto.orderCount.toString(),
        rejectedItemsCount = dto.rejectedItemsCount,
        vanStatus = dto.status,
        vanArrivalTime = dto.vanArrivalTime,
    )

    companion object {
        /** Amount of time before an order's stage by time that is considered high priority (in the app, not necessarily the backend) */
        private val HIGH_PRIORITY_THRESHOLD_DURATION: Duration = Duration.ofMinutes(45L)

        /**
         * True when the order within [HIGH_PRIORITY_THRESHOLD_DURATION] of stage by time. Use to make UI more prominent for things that need to be done soon.
         *
         * Example: Assume now is 1:30pm and stage by time is 2pm. This property would be set to true
         * Example: Assume now is 1:00 pm and stage by time is 2pm. This property would be set to false
         */

        fun isOrderWithinHighPriorityTimeThreshold(dto: ErDto): Boolean {
            val withZoneSameInstant = dto.expectedCompletedDate?.withZoneSameInstant(ZoneId.systemDefault())
            return if (withZoneSameInstant != null) {
                ZonedDateTime.now().isAfter(
                    withZoneSameInstant.minus(HIGH_PRIORITY_THRESHOLD_DURATION)
                        ?.withZoneSameInstant(ZoneId.systemDefault())
                )
            } else {
                false
            }
        }

        fun combineCustomerNameAndOrderNumber(item: OrderItemUI?) = "${item?.nameShort} - ${item?.orderNumber}"
    }

    // Formatting
    val formattedWindowStartTime: String? get() = getFormattedWithoutAmPm(windowStartTime)
    val formattedWindowStartTimeWithAMPM: String? get() = getFormattedWithAmPm(windowStartTime)
    val formattedWindowEndTime: String? get() = getFormattedWithAmPm(windowEndTime)
    val formattedArrivalTime: String? get() = getFormattedWithAmPm(customerArrivalTime)
}

enum class FulfillmentTypeUI {
    THREEPL, ONEPL, DUG, SHIPPING;
}

enum class OrderPriority {
    LOW, MEDIUM, HIGH, CRITICAL;
}

enum class OrderStatus {
    IN_PROGRESS, UNASSIGNED, UNAVAILABLE, READY
}

enum class CustomerArrivalStatusUI {
    ARRIVED, ARRIVING, EN_ROUTE, PICKUP_READY, ARRIVED_NOT_STARTED
}

enum class OrderTypeUI {
    REGULAR, FLASH, FLASH3P, EXPRESS
}

fun convertArrivalStatus(status: CustomerArrivalStatus?, isUnclaimedOrder: Boolean? = null) =
    when (status) {
        CustomerArrivalStatus.STORE_NOTIFIED, CustomerArrivalStatus.ARRIVED -> {
            if (isUnclaimedOrder == true) {
                CustomerArrivalStatusUI.ARRIVED_NOT_STARTED
            } else {
                CustomerArrivalStatusUI.ARRIVED
            }
        }
        CustomerArrivalStatus.ETA_SHARED, CustomerArrivalStatus.ON_THE_WAY -> CustomerArrivalStatusUI.EN_ROUTE
        CustomerArrivalStatus.GEO_FENCE_BROKEN -> CustomerArrivalStatusUI.ARRIVING
        CustomerArrivalStatus.UNARRIVED -> CustomerArrivalStatusUI.PICKUP_READY
        else -> null
    }

fun convertArrivalStatus(status: VanStatus?, isUnclaimedOrder: Boolean? = null) =
    when (status) {
        VanStatus.IN_PROGRESS -> {
            if (isUnclaimedOrder == true) {
                CustomerArrivalStatusUI.ARRIVED_NOT_STARTED
            } else {
                CustomerArrivalStatusUI.ARRIVED
            }
        }
        VanStatus.ARRIVING -> CustomerArrivalStatusUI.ARRIVING
        else -> null
    }

fun CustomerArrivalStatusUI.combineArriveStatuses() =
    if (this == CustomerArrivalStatusUI.ARRIVED_NOT_STARTED) {
        CustomerArrivalStatusUI.ARRIVED
    } else {
        this
    }

fun FulfillmentTypeUI.asNameRes() = when (this) {
    FulfillmentTypeUI.ONEPL -> R.string.fulfillment_one_pl
    FulfillmentTypeUI.THREEPL -> R.string.fulfillment_three_pl
    FulfillmentTypeUI.DUG -> R.string.fulfillment_dug
    else -> R.string.empty
}
