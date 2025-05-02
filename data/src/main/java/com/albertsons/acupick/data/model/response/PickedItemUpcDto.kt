package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.SubReasonCode
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to the PickedItemUpcDto swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class PickedItemUpcDto(
    @Json(name = "containerId") val containerId: String? = null,
    @Json(name = "isSmartSubItem") val isSmartSubItem: Boolean? = null,
    @Json(name = "isSubstitution") val isSubstitution: Boolean? = null,
    @Json(name = "pickedTime") val pickedTime: ZonedDateTime? = null,
    @Json(name = "qty") val qty: Double? = null,
    @Json(name = "regulated") val regulated: Boolean? = null,
    @Json(name = "reshop") val reshop: Boolean? = null,
    @Json(name = "sameItemSubbed") val sameItemSubbed: Boolean? = null,
    @Json(name = "sellByWeightInd") val sellByWeightInd: SellByType? = null,
    @Json(name = "subReasonCode") val subReasonCode: SubReasonCode? = null,
    @Json(name = "substituteItemDesc") val substituteItemDesc: String? = null,
    @Json(name = "substituteItemId") val substituteItemId: String? = null,
    @Json(name = "minimumAgeRequired") val minimumAgeRequired: Int? = null,
    @Json(name = "isRejected") val isRejected: Boolean? = null,
    @Json(name = "isRemovedInEdit") val isRemoved: Boolean? = null,
    @Json(name = "zone") val zone: StorageType? = null,
    @Json(name = "substituteItemImageUrl") val substituteItemImageUrl: String? = null,
    @Json(name = "upc") val upc: String? = null,
    @Json(name = "upcId") val upcId: Long? = null,
    @Json(name = "upcQty") val upcQty: Double? = null,
    @Json(name = "userId") val userId: String? = null,
    @Json(name = "netWeight") val netWeight: Double? = null,
    @Json(name = "displayType") val displayType: Int? = null,
    @Json(name = "netPromotionAmount") val netPromotionAmount: Double? = null,
    @Json(name = "unitPrice") val unitPrice: Double? = null
) : Parcelable, Dto

/** Give a list of picked items, return another list with all non-1-quantity items with barcodeType of Normal, Short, or Priced split into multiple items */
fun Iterable<PickedItemUpcDto>?.splitItems(barcodeMapper: BarcodeMapper, fixedItemTypesEnabled: Boolean): List<PickedItemUpcDto> {
    val splitItems = mutableListOf<PickedItemUpcDto>()
    this?.forEach { item ->
        val itemBarcodeType = barcodeMapper.inferBarcodeType(item.upc.orEmpty(), enableLogging = true)
        val isItemTypeSplittable =
            itemBarcodeType is BarcodeType.Item.Normal ||
                itemBarcodeType is BarcodeType.Item.Short ||
                itemBarcodeType is BarcodeType.Item.Weighted ||
                (
                    itemBarcodeType is BarcodeType.Item.Priced &&
                        item.sellByWeightInd in listOf(SellByType.Prepped, SellByType.PriceEachUnique, SellByType.PriceEach, SellByType.RegularItem) &&
                        fixedItemTypesEnabled
                    )
        val qty = item.qty?.toInt() ?: 0
        if (qty > 1 && isItemTypeSplittable) {
            val newItem = item.copy(qty = 1.0, upcQty = 1.0)
            repeat(qty) {
                splitItems.add(newItem)
            }
        } else {
            splitItems.add(item)
        }
    }
    return splitItems
}

fun PickedItemUpcDto.getWeightFromUpc(): Double? {
    return if (upc?.startsWith("04") == true) {
        val rawResult = (upc.subSequence(7, 12).toString().toDoubleOrNull())
        var result: Double? = null
        if (rawResult != null) {
            result = rawResult / 100
        }
        result
    } else {
        null
    }
}

fun PickedItemUpcDto.getQty(): Double? {
    return when (sellByWeightInd) {
        SellByType.Each -> qty
        else -> upcQty
    }
}

/** Give a list of picked items, return another list with all identical items combined into single items with updated qty */
fun Iterable<PickedItemUpcDto>?.combineIdenticalItems(): List<PickedItemUpcDto> {
    val combinedItems = mutableListOf<PickedItemUpcDto>()
    this?.distinct()?.forEach { item ->
        val qty = this.count { it == item }
        if (qty > 1) {
            val newItem = item.copy(qty = qty.toDouble(), upcQty = qty.toDouble())
            combinedItems.add(newItem)
        } else {
            combinedItems.add(item)
        }
    }
    return combinedItems
}

fun PickedItemUpcDto.isSubstitution() = this.isSubstitution == true && this.subReasonCode != SubReasonCode.IssueScanning

fun PickedItemUpcDto.isSubstitutionOrIssueScanning() = this.isSubstitution == true || this.subReasonCode == SubReasonCode.IssueScanning

// ISSUE-SCANNING Used to show only issue reported item
fun PickedItemUpcDto.isIssueScanning() = this.subReasonCode == SubReasonCode.IssueScanning

fun PickedItemUpcDto.toSwapItem(): SwapItem? {
    return SwapItem(
        itemId = substituteItemId,
        imageUrl = substituteItemImageUrl,
        itemDesc = substituteItemDesc,
        qty = qty,
        price = netPromotionAmount,
        upcId = upcId,
        containerId = containerId,
        netWeight = netWeight
    )
}
