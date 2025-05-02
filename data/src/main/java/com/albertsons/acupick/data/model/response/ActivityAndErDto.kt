package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.ActivityType
import com.albertsons.acupick.data.model.CartType
import com.albertsons.acupick.data.model.CustomerArrivalStatus
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.EntityReference
import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.FulfillmentSubType
import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.HandshakeType
import com.albertsons.acupick.data.model.OrderChatDetail
import com.albertsons.acupick.data.model.PrePickType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.ToteEstimate
import com.albertsons.acupick.data.model.request.UserDto
import com.albertsons.acupick.infrastructure.utils.HOUR_MINUTE_AM_PM_TIME_FORMATTER
import com.albertsons.acupick.infrastructure.utils.formattedWith
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Corresponds to the ActivityAndErDto swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ActivityAndErDto(
    @Json(name = "actId") val actId: Long? = null,
    /** Activity type (see [ActivityType]) */
    @Json(name = "actType") val actType: ActivityType? = null,
    @Json(name = "activityNo") val activityNo: String? = null,
    @Json(name = "addnActData") val additionalActData: AdditionalActDataDto? = null,
    @Json(name = "assignedTo") val assignedTo: UserDto? = null,
    @Json(name = "bagCount") val bagCount: Int? = null,
    @Json(name = "bagCountRequired") val bagCountRequired: Boolean? = null,
    @Json(name = "batch") val batch: String? = null,
    @Json(name = "completionTime") val completionTime: ZonedDateTime? = null,
    /** Customer First Name */
    @Json(name = "contactFirstName") val contactFirstName: String? = null,
    /** Customer Last Name */
    @Json(name = "contactLastName") val contactLastName: String? = null,
    @Json(name = "isSubscription") val isSubscription: Boolean? = null,
    @Json(name = "isSnap") val isSnap: Boolean? = null,
    @Json(name = "shortOrderNumber") val shortOrderNumber: String? = null,
    @Json(name = "customerOrderNumber") val customerOrderNumber: String? = null,
    @Json(name = "entityIds") val entityIds: List<String>? = null,
    @Json(name = "entityReference") val entityReference: EntityReference? = null,
    @Json(name = "erId") val erId: Long? = null,
    /** Shorted quantity (due to Out of Stock, etc) */
    @Json(name = "exceptionQty") val exceptionQty: Long? = null,
    /** Total count of item quantities across the activity */
    @Json(name = "expectedCount") val expectedCount: Int? = null,
    @Json(name = "expectedEndTime") val expectedEndTime: ZonedDateTime? = null,
    @Json(name = "fulfillment") val fulfillment: FulfillmentAttributeDto? = null,
    @Json(name = "handshakeType") val handshakeType: HandshakeType? = null,
    @Json(name = "isPrepNeeded") val isPrepNeeded: Boolean? = null,
    @Json(name = "itemQty") val itemQty: Double? = null,
    @Json(name = "looseItemCount") val looseItemCount: Int? = null,
    @Json(name = "toteCount") val toteCount: Int? = null,
    /** Time when the customer is arrived/ arriving */
    @Json(name = "nextActExpStartTime") val nextActExpStartTime: ZonedDateTime? = null,
    @Json(name = "orderType") val orderType: OrderType? = null,
    @Json(name = "pickUpBay") val pickUpBay: String? = null,
    /** Total number of items picked */
    /** Previous activity id. For example, it will be the picking flow activity id when [actId] is the staging flow activity id. Will be null if [actId] is the picking flow activity id */
    @Json(name = "prevActivityId") val prevActivityId: Long? = null,
    @Json(name = "processedQty") val processedQty: Long? = null,
    @Json(name = "reProcess") val reProcess: Boolean? = null,
    @Json(name = "releasedEventDateTime") val releasedEventDateTime: ZonedDateTime? = null,
    @Json(name = "routeVanNumber") val routeVanNumber: String? = null,
    @Json(name = "seqNo") val seqNo: String? = null,
    @Json(name = "siteId") val siteId: String? = null,
    /** Indicates slot end time of customer arrival */
    @Json(name = "slotEndDate") val slotEndDate: ZonedDateTime? = null,
    /** Indicates slot start time of customer arrival */
    @Json(name = "slotStartDate") val slotStartDate: ZonedDateTime? = null,
    @Json(name = "status") val status: ActivityStatus? = null,
    @Json(name = "stopNumber") val stopNumber: String? = null,
    @Json(name = "storageTypes") val storageTypes: List<StorageType>? = null,
    /** Customer arrival status */
    @Json(name = "subStatus") val subStatus: CustomerArrivalStatus? = null,
    @Json(name = "totalSeqNo") val totalSeqNo: String? = null,
    @Json(name = "cartType") val cartType: CartType? = null,
    @Json(name = "prePickType") val prePickType: PrePickType? = null,
    @Json(name = "is3p") val is3p: Boolean? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "vehicleInfo") val vehicleInfo: VehicleInfoDto? = null,
    @Json(name = "toteEstimate") val toteEstimate: ToteEstimate? = null,
    @Json(name = "isMultiSource") val isMultiSource: Boolean? = null,
    @Json(name = "feScreenStatus") val feScreenStatus: String? = null,
    @Json(name = "vanDepartureTime") val vanDepartureTime: ZonedDateTime? = null,
    @Json(name = "itemQtyToRemove") val itemQtyToRemove: Int? = null,
    @Json(name = "orderChatDetails") val orderChatDetails: List<OrderChatDetail>? = null,
) : Parcelable, Dto {
    fun getPickListType(): PickListBatchingType {
        return if (erId == null) PickListBatchingType.Batch else PickListBatchingType.SingleOrder
    }

    fun getOrderCount(): Int = entityIds?.size ?: 1

    fun stageByTime() = this.expectedEndTime?.withZoneSameInstant(ZoneId.systemDefault()).formattedWith(HOUR_MINUTE_AM_PM_TIME_FORMATTER)
}

fun ActivityAndErDto.isWineOrder() = fulfillment?.type == FulfillmentType.SHIPPING && cartType == CartType.WINE

fun ActivityAndErDto.fullContactName() = "${contactFirstName.orEmpty()} ${contactLastName.orEmpty()}".trim()

fun ActivityAndErDto.customerNameForHomeCard() = StringBuilder().apply {
    val firstName = contactFirstName?.firstOrNull()?.let { "$it. " }.orEmpty()
    append(firstName)
    val limit = MAX_CUSTOMER_NAME_HOME
    val lastName = contactLastName?.take(limit).orEmpty()
    append(lastName)
    if ((contactLastName?.length ?: 0) > limit) append("...")
}.trim().toString()

fun ActivityAndErDto.customerNameBasedOnAssociate() = StringBuilder().apply {
    val firstName = contactFirstName?.firstOrNull()?.let { "$it. " }.orEmpty()
    append(firstName)
    val limit = if (assignedTo != null) NAME_LENGTH_PICKLIST else NAME_LENGTH_PICKLIST_ASSOCIATE_AVAILABLE
    val lastName = contactLastName?.take(limit).orEmpty()
    append(lastName)
    if ((contactLastName?.length ?: 0) > limit) append("...")
}.trim().toString()

fun ActivityAndErDto.associateFirstNameDotLastInitial() = StringBuilder().apply {
    assignedTo?.let { user ->
        val firstName = user.firstName?.take(NAME_LENGTH_PICKLIST).orEmpty()
        append(firstName)
        if ((user.firstName?.length ?: 0) > NAME_LENGTH_PICKLIST) append("...")
        val lastName = user.lastName?.firstOrNull()?.let { " $it." }
        append(lastName)
    }
}.trim().toString()

fun ActivityAndErDto.getListOfConversationSid(): List<String>? = orderChatDetails?.mapNotNull {
    it.conversationSid
}

fun String?.toVanNumber(): String? {
    val finalString = this?.subSequence(3, 6).toString().substringAfter('-')
    if (this?.subSequence(3, 6).toString() == finalString) {
        return null
    }
    return finalString
}

fun String.toDeliveryType(): FulfillmentSubType? {
    if (this.substringBefore("-") == "1PL") {
        return FulfillmentSubType.ONEPL
    }
    return null
}

enum class PickListBatchingType {
    SingleOrder, Batch
}

// Combine the Express and Regular orders into one group,
// this is just for the sticky headers on Open PickLists page
fun OrderType.combinePickListStatuses() =
    if (this == OrderType.EXPRESS) {
        OrderType.REGULAR
    } else {
        this
    }

const val NAME_LENGTH_PICKLIST = 11
const val NAME_LENGTH_PICKLIST_ASSOCIATE_AVAILABLE = 22
const val MAX_CUSTOMER_NAME_HOME = 9
