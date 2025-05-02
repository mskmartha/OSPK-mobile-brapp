package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.AmountDto
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.EntityReference
import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.PrePickType
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.SubstitutionCode
import com.albertsons.acupick.data.model.isNotCustomerSuggestedSubCode
import com.albertsons.acupick.data.model.request.UserDto
import com.albertsons.acupick.infrastructure.utils.orZero
import com.albertsons.acupick.infrastructure.utils.toTwoDecimalString
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.text.DecimalFormat
import java.time.ZonedDateTime

/**
 * Corresponds to the ItemActivityDto swagger api
 *
 * **To uniquely identify an item, use [id] or [itemId] + [customerOrderNumber]**
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ItemActivityDto(
    /** This flag if set indicates that corresponding customer order got cancelled  */
    @Json(name = "amount") val amountDto: AmountDto? = null,
    @Json(name = "attemptToRemove") val attemptToRemove: Boolean? = null,
    @Json(name = "completionTime") val completionTime: ZonedDateTime? = null,
    @Json(name = "conditionCode") val conditionCode: String? = null,
    /** Customer First Name */
    @Json(name = "contactFirstName") val contactFirstName: String? = null,
    /** Customer Last Name */
    @Json(name = "contactLastName") val contactLastName: String? = null,
    @Json(name = "contactPhoneNum") val contactPhoneNumber: String? = null,
    /**
     * Customer order number for the item.
     *
     * **To uniquely identify an item, use [id] or [itemId] + [customerOrderNumber]**
     */
    @Json(name = "customerOrderNumber") val customerOrderNumber: String? = null,
    @Json(name = "depId") val depId: String? = null,

    /** Department name of the item (like Snacks, Fresh etc) */
    @Json(name = "depName") val depName: String? = null,
    // specific to substitutionItemDetails API call
    @Json(name = "deptName") val deptName: String? = null,
    /** displayType = 3 signals that the item is an item that was ordered by weight, and has an orderedWeight */
    @Json(name = "displayType") val displayType: Long? = null,
    @Json(name = "entityReference") val entityReference: EntityReference? = null,
    /** Shorted quantity (due to Out of Stock, etc) */
    @Json(name = "exceptionQty") val exceptionQty: Double? = null,
    @Json(name = "expectedCompletionTime") val expectedCompletionTime: ZonedDateTime? = null,
    @Json(name = "fulfillment") val fulfillment: FulfillmentAttributeDto? = null,
    /**
     * ItemActivity db id (aka iaId)
     *
     * Unique id for the specific item/customer combination.
     * **To uniquely identify an item, use [id] or [itemId] + [customerOrderNumber]**
     */
    @Json(name = "id") val id: Long? = null,
    @Json(name = "imageURL") val imageUrl: String? = null,
    /** Holds customer instructions */
    @Json(name = "instruction") val instructionDto: InstructionDto? = null,
    @Json(name = "isSnap") val isSnap: Boolean? = null,
    @Json(name = "isSubscription") val isSubscription: Boolean? = null,
    /** Item Address holds the location of the item in the warehouse. It contains aisle, bay and level for every item */
    @Json(name = "itemAddress") val itemAddressDto: ItemAddressDto? = null,
    @Json(name = "locationDetail") val locationDetail: String? = null,
    @Json(name = "itemDescription") val itemDescription: String? = null,
    /**
     * BPN (Base Product Number)
     *
     * Unique identifier for the actual product only.
     * Not unique for a picklist since multiple customers can have ordered the same product.
     * **To uniquely identify an item, use [id] or [itemId] + [customerOrderNumber]**
     */
    @Json(name = "itemId") val itemId: String? = null,
    /** Average weight of an item */
    @Json(name = "itemWeight") val itemWeight: String? = null,
    /** Item weight Unit Of Measure */
    @Json(name = "itemWeightUom") val itemWeightUom: String? = null,
    @Json(name = "lowerWeightLimit") val lowerWeightLimit: Double? = null,
    @Json(name = "minWeight") val minWeight: Double? = null,
    @Json(name = "orderedWeight") val orderedWeight: Double? = null,
    /** Ordered weight Unit of Measure */
    @Json(name = "orderedWeightUOM") val orderedWeightUOM: String? = null,
    @Json(name = "pickedUpcCodes") val pickedUpcCodes: List<PickedItemUpcDto>? = null,
    @Json(name = "pluCode") val pluCode: String? = null,
    @Json(name = "pluList") val pluList: List<String?>? = null,
    @Json(name = "primaryUpc") val primaryUpc: String? = null,
    /** Total quantity picked for this item */
    @Json(name = "processedQty") val processedQty: Double? = null,
    /** Total qty to pick for this item */
    @Json(name = "processedWeight") val processedWeight: Double? = null,
    @Json(name = "qty") val qty: Double? = null,
    @Json(name = "routeVanNumber") val routeVanNumber: String? = null,
    @Json(name = "sellByWeightInd") val sellByWeightInd: SellByType? = null,
    @Json(name = "seqNumber") val seqNumber: Long? = null,
    @Json(name = "shortedItemUpc") val shortedItemUpc: List<ShortedItemUpcDto>? = null,
    @Json(name = "shortOrderNumber") val shortOrderNumber: String? = null,
    @Json(name = "slotEndDate") val slotEndDate: ZonedDateTime? = null,
    @Json(name = "slotStartDate") val slotStartDate: ZonedDateTime? = null,
    @Json(name = "stopNumber") val stopNumber: String? = null,
    /** Storage type of the container. This is helpful in multi order scenario where in activities are created based on the storage type */
    @Json(name = "storageType") val storageType: StorageType? = null,
    /** Flag if set indicates that substitution is allowed  */
    @Json(name = "subAllowed") val subAllowed: Boolean? = null,
    /** Substitution code */
    @Json(name = "subCode") val subCode: SubstitutionCode? = null,
    /** Substitution value. Not an enum, just a string sourced from whatever BE get from OSCO orders */
    @Json(name = "subValue") val subValue: String? = null,
    @Json(name = "uom") val uom: String? = null,
    @Json(name = "upcList") val upcList: List<String>? = null,
    @Json(name = "upperWeightLimit") val upperWeightLimit: Double? = null,
    @Json(name = "useBatch") val useBatch: Boolean? = null,
    @Json(name = "groupByName") val groupByName: String? = null,
    @Json(name = "groupBySeq") val groupBySeq: String? = null,
    @Json(name = "isPickCompleted") val isPickCompleted: Boolean = false,
    @Json(name = "basePrice") val basePrice: Double? = null,
    @Json(name = "basePricePer") val basePricePer: Double? = null,
    @Json(name = "prePickType") val prePickType: PrePickType? = null,
    @Json(name = "bulkVariantType") val bulkVariantType: BulkVariantType? = null,
    @Json(name = "pluBulkVariants") val bulkVariantList: List<BulkItemsDto?>? = null,
    @Json(name = "upcBulkVariants") val bulkVariantMap: Map<String, List<BulkItemsDto?>?>? = null,
    @Json(name = "isCustBagPreference") val isCustomerBagPreference: Boolean? = null,
) : Parcelable, Dto {
    //  Item is fully picked when either it meets normal quantity or any amount of shorts, or substitutions, are made.
    // TODO: ISSUE-SCANNING add issue scanning condition in fully picked method commented old one once testing completed will remove it
    /*fun isFullyPicked() =
        (processedQty.orZero() >= qty.orZero()) || exceptionQty.orZero() > 0 || pickedUpcCodes?.any { it.isSubstitution() } ?: false*/

    fun isFullyPicked() =
        (processedQty.orZero() >= qty.orZero()) || exceptionQty.orZero() > 0 || pickedUpcCodes?.any { it.isSubstitution() } ?: false || pickedUpcCodes?.any { it.isIssueScanning() } ?: false

    /**
     * ISSUE-SCANNING
     * During Issue Scanning we need to prevent the item moving from
     * to-do tab to picked tab until the issue scanning flow for an item is completed.
     * In this method we restrict the item in moving to picked tab by checking the flag isIssueScanningInProgress
     * false -> item will be moved to picked tab
     * true -> item will be in to-do tab
     */
    fun validateIsFullyPicked(isIssueScanningInProgress: Boolean? = false, issueReportedItemId: Long? = 0) = run {
        when (issueReportedItemId == id && isIssueScanningInProgress ?: false) {
            true -> false
            false -> isFullyPicked()
        }
    }

    fun isPartiallyPicked() = ((processedQty ?: 0.0) >= 1.0)
    fun isFullyShorted() = exceptionQty.orZero() >= qty.orZero()
    fun isFullySubstituted() = pickedUpcCodes?.all { pickedItemUpcDto -> pickedItemUpcDto.isSubstitution() } ?: false

    fun hasAnySubstitutionOtherThanCustomerSuggestedSub(): Boolean = pickedUpcCodes?.any {
        it.isSubstitutionOrIssueScanning() && (it.isSmartSubItem?.not() ?: false || subCode.isNotCustomerSuggestedSubCode())
    } == true

    fun getCustomerName() = "${contactFirstName.orEmpty()} ${contactLastName.orEmpty()}".trim()

    fun isOrderedByWeight() = sellByWeightInd == SellByType.Weight &&
        displayType == DISPLAY_TYPE_ORDERED_BY_WEIGHT &&
        orderedWeight != null

    fun isDisplayType3PW() = sellByWeightInd == SellByType.PriceWeighted &&
        displayType == DISPLAY_TYPE_ORDERED_BY_WEIGHT &&
        orderedWeight != null

    fun isOrderedByWeightFromSubstitution() =
        sellByWeightInd == SellByType.Weight &&
            displayType == DISPLAY_TYPE_ORDERED_BY_WEIGHT

    fun getWeightAndUom(): String {
        val weight = DecimalFormat("#.##").format(orderedWeight ?: 0f)
        val uom = orderedWeightUOM?.lowercase() ?: "lb"
        return "$weight $uom"
    }

    fun isPrepped() = sellByWeightInd == SellByType.Prepped

    fun isPriceEachTotaled() = sellByWeightInd == SellByType.PriceEachTotal

    fun getItemDescriptionEllipsis(charLength: Int = MAX_ITEM_DESCRIPTION_CHARACTERS): String {
        val diffLength = when (charLength) {
            MAX_ITEM_DESCRIPTION_CHARACTERS_FOR_SMALL_VIEWS -> charLength - 11
            else -> charLength - 10
        }
        val description = itemDescription.orEmpty().trim()
        if (description.length > charLength) {
            val suffix = "...${description.takeLast(10)}"
            return "${description.take(diffLength).trimEnd()}$suffix"
        }
        return description
    }

    companion object {
        const val DISPLAY_TYPE_ORDERED_BY_WEIGHT = 3L
    }

    /**
     * ISSUE-SCANNING
     * In Issue scanning flow to-do tab count should be updated only when the issue scanning flow
     * for an item is fully complete. We should not be updating it for every record pick call.
     * Calculation for to-do tab count in issue scanning
     * todoCount = OrderedQty-(ProcessedQty-IssueReportedQty)
     */
    fun todoCountWithoutIssueReportedItem(issueReportedItemId: Long?): Double {
        return if (issueReportedItemId == id) {
            qty?.minus((processedQty?.minus(pickedUpcCodes?.filter { it.isIssueScanning() }?.sumOf { item -> item.qty.orZero() }.orZero()).orZero())).orZero()
        } else 0.0
    }

    /**
     * ISSUE-SCANNING
     * In Issue scanning flow picked tab count should be updated only when the issue scanning flow
     * for an item is fully complete. We should not be updating it for every record pick call.
     * Calculation for picked tab count in issue scanning
     * totalCount = ProcessedQty-IssueReportedQty
     */
    fun pickedCountWithoutIssueReportedItem(issueReportedItemId: Long?): Double {
        return if (issueReportedItemId == id) {
            processedQty?.minus((pickedUpcCodes?.filter { it.isIssueScanning() }?.sumOf { item -> item.qty.orZero() }.orZero())).orZero()
        } else qty.orZero()
    }
}

/** Combination of processedQty and exceptionQty. Only non null if both values are non null. */
val ItemActivityDto.processedAndExceptionQty: Double?
    get() = if (exceptionQty != null) processedQty?.plus(exceptionQty) else null

/** True when there is a non zero [ItemActivityDto.exceptionQty] */
val ItemActivityDto.isShorted: Boolean
    get() = exceptionQty.orZero() != 0.0

/** True when any [ItemActivityDto.pickedUpcCodes] are substitutions. */
val ItemActivityDto.isSubstituted: Boolean
    get() = pickedUpcCodes?.any { it.isSubstitution() } == true

// Used for to-do and picked tab count and chip label
/** True when any [ItemActivityDto.pickedUpcCodes] are issue scanned. */
val ItemActivityDto.isIssueScanned: Boolean
    get() = pickedUpcCodes?.any { it.isIssueScanning() } == true

val ItemActivityDto.netWeight: Double
    get() = pickedUpcCodes?.sumOf { it.netWeight.orZero() }.orZero()

val ItemActivityDto.requestedNetWeight: Double
    get() = itemWeight?.toDoubleOrNull().orZero() * qty.orZero()

val ItemActivityDto.substitutedQty: Double
    get() = qty.orZero() - (pickedUpcCodes?.filter { it.isSubstitutionOrIssueScanning().not() }?.sumBy { it.qty.orZero().toInt() }?.toDouble().orZero()) - exceptionQty.orZero()

val ItemActivityDto.substitutedQtyMasterView: Double
    get() = qty.orZero() - ((processedQty.orZero() - (pickedUpcCodes?.sumBy { it.qty.orZero().toInt() }?.toDouble().orZero())) - exceptionQty.orZero())
fun ItemActivityDto.fullContactName() = "${contactFirstName.orEmpty()} ${contactLastName.orEmpty()}".trim()

fun ItemActivityDto.asFirstInitialDotLastString() =
    "${this.contactFirstName?.take(1)}. $contactLastName"

val ItemActivityDto.totalWeight: Double
    get() = (if ((requestedNetWeight ?: 0.0) % 1 != 0.0) requestedNetWeight ?: 0.0 else requestedNetWeight)

val ItemActivityDto.fulfilledWeight: Double
    get() = (if ((processedWeight.orZero()) % 1 != 0.0) processedWeight.orZero() else processedWeight).orZero()
val ItemActivityDto.remainingWeight: String
    get() = with(this) {
        val weight = (orderedWeight.orZero() - fulfilledWeight).toTwoDecimalString()
        "$weight ${itemWeightUom ?: "lb"}"
    }

/** Number of orders in batch picklist or 1 for single order picklist */
fun List<ItemActivityDto>.getOrderCount(): Int = groupingBy { it.customerOrderNumber }.eachCount().count()

// to be used for swap substitution ui
fun ItemActivityDto.toSwapItem(isMasterOrderView: Boolean = false, isOutOfStock: Boolean = false, assignedTo: UserDto? = null): SwapItem {
    return SwapItem(
        id = id,
        itemId = itemId,
        imageUrl = imageUrl,
        itemDesc = itemDescription,
        qty = if (isOutOfStock) exceptionQty else when (isMasterOrderView) {
            true -> substitutedQtyMasterView
            else -> substitutedQty
        },
        price = amountDto?.netPromotionAmount,
        upcId = primaryUpc?.toLong(),
        subApprovalStatus = if (isOutOfStock) SubApprovalStatus.OUT_OF_STOCK else
            when (pickedUpcCodes?.firstOrNull()?.isRejected) {
                true -> SubApprovalStatus.DECLINED_SUB
                false -> SubApprovalStatus.APPROVED_SUB
                else -> SubApprovalStatus.PENDING_SUB
            },
        substitutedWith = if (isMasterOrderView) pickedUpcCodes?.mapNotNull { it.toSwapItem() } // BE only sending substituted/oos item for masterView no need to filter it at FE side
        else pickedUpcCodes?.filter { it.isSubstitution() || it.isIssueScanning() }?.mapNotNull { it.toSwapItem() },
        shortedItemUpc = shortedItemUpc,
        assignedTo = assignedTo,
        customerOrderNumber = customerOrderNumber
    )
}
// Calculate max weight limit ACIP-192856
fun ItemActivityDto.getWeightedItemMaxWeight(): Double = (qty.orZero() * itemWeight?.toDouble().orZero() * MAX_WEIGHT_FACTOR_FOR_WEIGHTED_ITEM) - processedWeight.orZero()

private const val MAX_ITEM_DESCRIPTION_CHARACTERS = 60
private const val MAX_WEIGHT_FACTOR_FOR_WEIGHTED_ITEM = 3
const val MAX_ITEM_DESCRIPTION_CHARACTERS_FOR_SMALL_VIEWS = 45
