@file:Suppress("DeprecatedCallableAddReplaceWith")

package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.ActivityType
import com.albertsons.acupick.data.model.CartType
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.CustomerArrivalStatus
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.EntityReference
import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.FulfillmentSubType
import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.HandshakeType
import com.albertsons.acupick.data.model.OrderChatDetail
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.PickListActivity
import com.albertsons.acupick.data.model.PrePickType
import com.albertsons.acupick.data.model.RejectedItem
import com.albertsons.acupick.data.model.RejectedItemsByStorageType
import com.albertsons.acupick.data.model.RxBag
import com.albertsons.acupick.data.model.StorageLocation
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.request.UserDto
import com.albertsons.acupick.data.model.thatNeedsToBeSplit
import com.albertsons.acupick.infrastructure.utils.HOUR_MINUTE_AM_PM_TIME_FORMATTER
import com.albertsons.acupick.infrastructure.utils.SPACED_HOUR_MINUTE_AM_PM_TIME_FORMATTER
import com.albertsons.acupick.infrastructure.utils.formattedWith
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Corresponds to the ActivityDto swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ActivityDto(
    @Json(name = "actId") val actId: Long? = null,
    /** Activity type (see [ActivityType]) */
    @Json(name = "actType") val actType: ActivityType? = null,
    @Json(name = "activityNo") val activityNo: String? = null,
    @Json(name = "addnActData") val additionalActData: AdditionalActDataDto? = null,
    @Json(name = "assignedTo") val assignedTo: UserDto? = null,
    @Json(name = "authCode") val authCode: String? = null,
    @Json(name = "toteCount") val toteCount: Int? = null,
    @Json(name = "bagCount") val bagCount: Int? = null,
    @Json(name = "bagCountForHandOff") val bagCountForHandOff: Int? = null,
    @Json(name = "bagCountRequired") val bagCountRequired: Boolean? = null,
    @Json(name = "batch") val batch: String? = null,
    @Json(name = "cartType") val cartType: CartType? = null,
    @Json(name = "completionTime") val completionTime: ZonedDateTime? = null,
    /** Customer First Name */
    @Json(name = "contactFirstName") val contactFirstName: String? = null,
    /** Customer Last Name */
    @Json(name = "contactLastName") val contactLastName: String? = null,
    @Json(name = "contactPhoneNum") val contactPhoneNumber: String? = null,
    @Json(name = "containerActivities") val containerActivities: List<ContainerActivityDto>? = null,
    @Json(name = "containerItems") val containerItems: List<ErItemDto>? = null,
    @Json(name = "createdDate") val createdDate: ZonedDateTime? = null,
    @Json(name = "isFFC") val isFFC: Boolean? = null,
    @Json(name = "customerOrderNumber") val customerOrderNumber: String? = null,
    /** Delivery drive details */
    @Json(name = "driver") val driver: DriverDto? = null,
    @Json(name = "handshakeRequired") val handShakeRequired: Boolean? = null,
    @Json(name = "entityReference") val entityReference: EntityReference? = null,
    @Json(name = "erId") val erId: Long? = null,
    /** Shorted quantity (due to Out of Stock, etc) */
    @Json(name = "exceptionQty") val exceptionQty: Double? = null,
    /** Total count of item quantities across the activity */
    @Json(name = "expectedCount") val expectedCount: Int? = null,
    @Json(name = "expectedEndTime") val expectedEndTime: ZonedDateTime? = null,
    @Json(name = "fulfillment") val fulfillment: FulfillmentAttributeDto? = null,
    @Json(name = "handshakeType") val handshakeType: HandshakeType? = null,
    @Json(name = "isMultiSource") val isMultiSource: Boolean? = null,
    @Json(name = "isSnap") val isSnap: Boolean? = null,
    @Json(name = "isSubscription") val isSubscription: Boolean? = null,
    @Json(name = "itemActivities") val itemActivities: List<ItemActivityDto>? = null,
    @Json(name = "masterView") val masterView: List<ActivityDto>? = null,
    @Json(name = "itemQty") val itemQty: Double? = null,
    @Json(name = "looseItemCount") val looseItemCount: Int? = null,
    @Json(name = "minimumAgeRequired") val minimumAgeRequired: Int? = null,
    /**
     * Represents the next db activity id (often null).
     *
     * For example, if you complete a pick list you will get a response for the current activity ([ActivityType.PICK_PACK]) with [nextActivityId] being the [ActivityType.DROP_OFF] value
     */
    @Json(name = "nextActivityId") val nextActivityId: Long? = null,
    /** Time when the customer is arrived/ arriving */
    @Json(name = "nextActExpStartTime") val nextActExpStartTime: ZonedDateTime? = null,
    @Json(name = "nextActStatus") val nextActStatus: String? = null,
    @Json(name = "orderType") val orderType: OrderType? = null,
    /** Name of delivery partner ex. doordash */
    @Json(name = "partnerName") val partnerName: String? = null,
    @Json(name = "pickUpBay") val pickUpBay: String? = null,
    /** Previous activity id. For example, it will be the picking flow activity id when [actId] is the staging flow activity id. Will be null if [actId] is the picking flow activity id */
    @Json(name = "prevActivityId") val prevActivityId: Long? = null,
    /** Total number of items picked */
    @Json(name = "processedQty") val processedQty: Double? = null,
    @Json(name = "reProcess") val reProcess: Boolean? = null,
    @Json(name = "routeVanNumber") val routeVanNumber: String? = null,
    @Json(name = "rxDetails") val rxDetails: RxDetailsDto? = null,
    @Json(name = "serviceType") val serviceType: ServiceType? = null,
    @Json(name = "scheduledPickupTimestamp") val scheduledPickupTimestamp: ZonedDateTime? = null,
    @Json(name = "seqNo") val seqNo: String? = null,
    @Json(name = "shortOrderNumber") val shortOrderNumber: String? = null,
    @Json(name = "siteId") val siteId: String? = null,
    /** Indicates slot end time of customer arrival */
    @Json(name = "slotEndDate") val slotEndDate: ZonedDateTime? = null,
    /** Indicates slot start time of customer arrival */
    @Json(name = "slotStartDate") val slotStartDate: ZonedDateTime? = null,
    @Json(name = "status") val status: ActivityStatus? = null,
    @Json(name = "stopNumber") val stopNumber: String? = null,
    @Json(name = "storageTypes") val storageTypes: List<StorageType>? = null,
    @Json(name = "subStatus") val subStatus: CustomerArrivalStatus? = null,
    @Json(name = "totalSeqNumber") val totalSeqNo: String? = null,
    @Json(name = "orderCount") val orderCount: String? = null,
    @Json(name = "isCas") val isCas: Boolean? = null,
    @Json(name = "is3p") val is3p: Boolean? = null,
    @Json(name = "orderSummary") val orderSummary: List<OrderSummaryDto?>? = null,
    @Json(name = "releasedEventDateTime") val releasedEventDateTime: ZonedDateTime? = null,
    @Json(name = "prePickType") val prePickType: PrePickType? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "vehicleInfo") val vehicleInfo: VehicleInfoDto? = null,
    @Json(name = "deliveryInstruction") val deliveryInstruction: String? = null,
    @Json(name = "feScreenStatus") val feScreenStatus: String? = null,
    @Json(name = "conversationId") val conversationId: String? = null,
    @Json(name = "isCustBagPreference") val isCustomerBagPreference: Boolean? = null,
    @Json(name = "isPrepNeeded") val isPrepNeeded: Boolean? = null,
    @Json(name = "missingItemLocDisabledDepts") val missingItemLocDisabledDepts: List<String>? = null,
    @Json(name = "startTimer") val startTimer: Boolean? = null,
    @Json(name = "isGift") val isGift: Boolean? = null,
    @Json(name = "gift") val gift: GiftDto? = null,
    @Json(name = "orderChatDetails") val orderChatDetails: List<OrderChatDetail>? = null,
    @Json(name = "previousLocations") val previousLocations: List<StorageLocation>? = null,
) : Parcelable, Dto

@JsonClass(generateAdapter = false)
enum class BulkVariantType {
    @Json(name = "UPC") UPC,
    @Json(name = "PLU") PLU,
}

fun ActivityDto.toPickListActivity(items: Map<String, List<ItemActivityDto>?>) = PickListActivity(
    actId = actId,
    customerOrderNumber = customerOrderNumber,
    itemActivitiesMap = items,
    listOfOrderNumber = getListOfOrderNumber()
)

/** Set of all fulfillment types from children item activities. */
fun ActivityDto.fulfillmentTypes(): Set<FulfillmentAttributeDto> = itemActivities?.mapNotNull { item -> item.fulfillment }?.toSet() ?: emptySet()

fun ActivityDto.fullContactName() = "${contactFirstName.orEmpty()} ${contactLastName.orEmpty()}".trim()

fun ActivityDto.customerNameForHomeCard() = StringBuilder().apply {
    val firstName = contactFirstName?.firstOrNull()?.let { "$it. " }.orEmpty()
    append(firstName)
    val limit = NAME_LENGTH_PICKLIST_ASSOCIATE_AVAILABLE
    val lastName = contactLastName?.take(limit).orEmpty()
    append(lastName)
    if ((contactLastName?.length ?: 0) > limit) append("...")
}.trim().toString()

fun ActivityDto.stageByTime() = this.expectedEndTime?.withZoneSameInstant(ZoneId.systemDefault()).formattedWith(HOUR_MINUTE_AM_PM_TIME_FORMATTER)

fun ActivityDto.stageByTimeInSpacedFormat() = this.expectedEndTime?.withZoneSameInstant(ZoneId.systemDefault()).formattedWith(SPACED_HOUR_MINUTE_AM_PM_TIME_FORMATTER)

fun ActivityDto.getFulfillmentTypeDescriptions(): String =
    when {
        fulfillment?.type == FulfillmentType.DUG -> "DUG-$shortOrderNumber"
        fulfillment?.subType == FulfillmentSubType.THREEPL -> "3PL-$stopNumber"
        fulfillment?.subType == FulfillmentSubType.ONEPL -> "$routeVanNumber-$stopNumber"
        fulfillment?.type == FulfillmentType.DELIVERY -> "DEL-$stopNumber"
        else -> ""
    }

fun ArrayList<ActivityDto>.hasPharmacyServicingOrders() = this.any { it.rxDetails?.pharmacyServicingOrders == true }

fun ArrayList<ActivityDto>.hasPharmacyServicingOrdersAndStaged() = this.any { it.rxDetails?.pharmacyServicingOrders == true && it.rxDetails.orderStatus == OrderStatus.READY_FOR_PU }

fun ActivityDto.asFirstInitialDotLastString() =
    "${this.contactFirstName?.take(1)}. $contactLastName"

fun ActivityDto.asFirstNameLastInitial() =
    "${contactFirstName.orEmpty()} ${this.contactLastName?.take(1).orEmpty()}."

fun ActivityDto.asListOfItemIds(): List<String> {
    val itemIdList = mutableListOf<String?>()
    this.itemActivities?.forEach { itemIdList.add(it.itemId) }
    return itemIdList.filterNotNull()
}

fun ActivityDto.isWineOrder() = fulfillment?.type == FulfillmentType.SHIPPING && cartType == CartType.WINE

fun ActivityDto.containsSnap() = itemActivities.isNotNullOrEmpty() &&
    itemActivities?.any { it.isSnap == true } ?: false

fun ActivityDto.bagAndLooseItemTotal(): Int {
    val totalBags = this.containerActivities?.filter { it.type == ContainerType.BAG }?.size ?: 0
    val totalLoose = this.containerActivities?.filter { it.type == ContainerType.LOOSE_ITEM }?.size ?: 0
    return totalBags + totalLoose
}

fun ActivityDto.isRxDug(siteFlag: Boolean = true) = this.rxDetails != null && this.rxDetails.rxOrderId.isNotNullOrEmpty() && siteFlag

fun ActivityDto.getRxBag(siteFlag: Boolean = true): List<RxBag> {
    return rxDetails?.rxOrderId?.map { RxBag(rxDetails?.orderId.orEmpty(), it) } ?: emptyList()
}

fun ActivityDto.hasAddOnPrescription(siteFlag: Boolean = true): Boolean {
    return rxDetails?.pharmacyServicingOrders == true && siteFlag && rxDetails.orderStatus == OrderStatus.READY_FOR_PU
}

fun OrderSummaryDto.toOrderSummary(): OrderSummary {
    return OrderSummary(
        imageUrl = imageUrl,
        itemDesc = itemDesc,
        qty = qty,
        price = price,
        title = title,
        substitutedWith = substitutedWith?.mapNotNull { it?.toOrderSummary() }
    )
}

fun ActivityDto.getRejectedItemsByZone(): List<RejectedItemsByStorageType> {

    val rejectedItemsByStorageTypeList: MutableList<RejectedItemsByStorageType> = arrayListOf()

    val amRejectedItems = buildErItemDtoList(containerItems ?: arrayListOf(), StorageType.AM)
    val chRejectedItems = buildErItemDtoList(containerItems ?: arrayListOf(), StorageType.CH)
    val fzRejectedItems = buildErItemDtoList(containerItems ?: arrayListOf(), StorageType.FZ)
    val htRejectedItem = buildErItemDtoList(containerItems ?: arrayListOf(), StorageType.HT)

    if (amRejectedItems.isNotEmpty()) {
        rejectedItemsByStorageTypeList.add(
            RejectedItemsByStorageType(
                customerOrderNumber = customerOrderNumber,
                storageType = StorageType.AM,
                rejectedItems = buildRejectedList(amRejectedItems)
            )
        )
    }
    if (fzRejectedItems.isNotEmpty()) {
        rejectedItemsByStorageTypeList.add(
            RejectedItemsByStorageType(
                customerOrderNumber = customerOrderNumber,
                storageType = StorageType.FZ,
                rejectedItems = buildRejectedList(fzRejectedItems)
            )
        )
    }
    if (chRejectedItems.isNotEmpty()) {
        rejectedItemsByStorageTypeList.add(
            RejectedItemsByStorageType(
                customerOrderNumber = customerOrderNumber,
                storageType = StorageType.CH,
                rejectedItems = buildRejectedList(chRejectedItems)
            )
        )
    }
    if (htRejectedItem.isNotEmpty()) {
        rejectedItemsByStorageTypeList.add(
            RejectedItemsByStorageType(
                customerOrderNumber = customerOrderNumber,
                storageType = StorageType.HT,
                rejectedItems = buildRejectedList(htRejectedItem)
            )
        )
    }

    return rejectedItemsByStorageTypeList.filter { it.rejectedItems?.isNotNullOrEmpty() == true }
}

private fun buildRejectedList(containerItems: List<ErItemDto>): List<RejectedItem> {
    val rejectedItems = arrayListOf<RejectedItem>()
    containerItems.forEach { erItemDto ->
        erItemDto.pickedUpcCodes?.forEach {
            if (it.isRejected == true || it.isRemoved == true) {
                rejectedItems.addAll(erItemDto.toRejectedItems(it))
            }
        }
    }
    return rejectedItems
}

private fun buildErItemDtoList(containerItems: List<ErItemDto>, storageType: StorageType): List<ErItemDto> {
    return containerItems.filter { erItemDto ->
        erItemDto.pickedUpcCodes?.any {
            it.zone == storageType
        } ?: false
    }
}

fun ErItemDto.toRejectedItems(pickedItemUpcDto: PickedItemUpcDto): MutableList<RejectedItem> {
    val rejectedItemList = mutableListOf<RejectedItem>()
    if (pickedItemUpcDto.isNeededToBeSplit()) {
        repeat(pickedItemUpcDto.getQty()?.toInt() ?: 0) {
            rejectedItemList.add(this.createRejectedItem(pickedItemUpcDto, splitId = it))
        }
    } else {
        rejectedItemList.add(this.createRejectedItem(pickedItemUpcDto))
    }
    return rejectedItemList
}

fun ErItemDto.createRejectedItem(incomingPickedItemUpcDto: PickedItemUpcDto, splitId: Int? = null): RejectedItem {
    return RejectedItem(
        upcOrPlu = incomingPickedItemUpcDto.upc,
        itemType = incomingPickedItemUpcDto.sellByWeightInd,
        itemDesc = if (incomingPickedItemUpcDto.isSubstitutionOrIssueScanning()) incomingPickedItemUpcDto.substituteItemDesc else itemDesc,
        itemId = if (incomingPickedItemUpcDto.isSubstitutionOrIssueScanning()) incomingPickedItemUpcDto.substituteItemId else itemId,
        imageUrl = if (incomingPickedItemUpcDto.isSubstitutionOrIssueScanning()) incomingPickedItemUpcDto.substituteItemImageUrl else imageUrl,
        originalItemId = itemId,
        qty = if (splitId != null) 1 else incomingPickedItemUpcDto.getQty()?.toInt(),
        displayType = incomingPickedItemUpcDto.displayType,
        weight = incomingPickedItemUpcDto.getWeightFromUpc(),
        regulated = incomingPickedItemUpcDto.regulated,
        splitId = splitId,
        containerId = incomingPickedItemUpcDto.containerId,
        isRemovedInEdit = incomingPickedItemUpcDto.isRemoved
    )
}

fun PickedItemUpcDto.isNeededToBeSplit(): Boolean =
    this.sellByWeightInd?.thatNeedsToBeSplit() == true && (this.getQty()?.toInt() ?: 1) > 1

fun ActivityDto.toMySwapSubstitutedItem(): List<SwapItem?> {
    val swapItems = mutableListOf<SwapItem>()
    getSubstitutedItems()?.map { itemActivityDto ->
        swapItems.add(itemActivityDto.toSwapItem())
    }
    getShortedItems()?.map { itemActivityDto ->
        swapItems.add(itemActivityDto.toSwapItem(isOutOfStock = true))
    }
    return swapItems
}

fun ActivityDto.getSubstitutedItems(): List<ItemActivityDto>? {
    return this.itemActivities?.filter { it.isSubstituted || it.isIssueScanned }
}

fun ActivityDto.getSubstitutedItemsForMasterView(): List<ItemActivityDto>? {
    return this.itemActivities?.filter { it.pickedUpcCodes.isNotNullOrEmpty() }
}

fun ActivityDto.getShortedItems(): List<ItemActivityDto>? {
    return this.itemActivities?.filter { it.isShorted }
}

fun ActivityDto.toOtherPickerSwapSubstitutedItem(): List<SwapItem?> {
    val swapItems = mutableListOf<SwapItem>()
    this.masterView?.mapNotNull { masterView ->
        masterView.getSubstitutedItemsForMasterView()?.map { itemActivityDto ->
            swapItems.add(itemActivityDto.toSwapItem(isMasterOrderView = true, assignedTo = masterView.assignedTo))
        }
        masterView.getShortedItems()?.map { itemActivityDto ->
            swapItems.add(itemActivityDto.toSwapItem(isOutOfStock = true, isMasterOrderView = true, assignedTo = masterView.assignedTo))
        }
    }
    return swapItems
}

fun ActivityDto.getListOfOrderNumber(): List<String>? = orderChatDetails?.mapNotNull {
    it.customerOrderNumber
}

fun ActivityDto.getListOfConversationSid(): List<String>? = orderChatDetails?.mapNotNull {
    it.conversationSid
}

/**
 * Prepare list of customerOrderNumber from orderChatDetails based on the startChatBlockTimer attribute is true.
 */
fun ActivityDto.getListOfStartTimerOrderNumber(): List<String>? = orderChatDetails
    ?.filter { it.startChatBlockTimer == true }
    ?.mapNotNull { it.customerOrderNumber }

//  To support backward compatibility we have to validating old startTimer attribute as well
fun ActivityDto.isStartTimerEnabled(): Boolean? = orderChatDetails?.let { true } ?: startTimer

fun ActivityDto.isBatchOrder(): Boolean = erId == null

const val FE_SCREEN_STATUS_STORE_NOTIFIED = "STORE-NOTIFIED"
