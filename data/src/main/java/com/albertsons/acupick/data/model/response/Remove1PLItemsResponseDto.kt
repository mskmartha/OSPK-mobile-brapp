package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.EntityReference
import com.albertsons.acupick.data.model.RejectedItem
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.thatNeedsToBeSplit
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class Remove1PLItemsResponseDto(
    @Json(name = "activityId") val activityId: Long? = null,
    @Json(name = "totalOrders") val totalOrders: Int = 1,
    @Json(name = "vanArrivalTime") val vanArrivalTime: String? = null,
    @Json(name = "vanId") val vanId: String? = null,
    @Json(name = "ordersPerZone") val ordersPerZone: List<RejectedItemsByZone>? = emptyList(),
    @Json(name = "giftOrderCount") val giftOrderCount: Int? = null,
    @Json(name = "giftOrderErIds") val giftOrderErIds: List<Long>? = null
) : Parcelable, Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class RejectedItemsByZone(
    @Json(name = "zone") val zone: StorageType = StorageType.AM,
    @Json(name = "rejectedItems") val rejectedItems: Int = 10,
    @Json(name = "orderDetails") val orderDetails: List<RejectedOrderDetails> = emptyList(),
) : Parcelable, Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class RejectedOrderDetails(
    @Json(name = "orderNumber") val orderNumber: String,
    @Json(name = "shortOrderNumber") val shortOrderNumber: String? = null,
    @Json(name = "stopNumber") val stopNumber: String,
    @Json(name = "entityReference") val entityReference: EntityReference? = null,
    @Json(name = "totesPerZone") val totesPerZone: List<TotesPerZone> = emptyList(),
) : Parcelable, Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class TotesPerZone(
    @Json(name = "isSubstitution") val isSubstitution: Boolean? = null,
    @Json(name = "qty") val qty: Double? = null,
    @Json(name = "isRegulated") val isRegulated: Boolean? = null,
    @Json(name = "isRejected") val isRejected: Boolean? = null,
    @Json(name = "isRemovedInEdit") val isRemovedInEdit: Boolean? = null,
    @Json(name = "sellByWeightInd") val sellByWeightInd: SellByType? = null,
    @Json(name = "substituteItemImageUrl") val substituteItemImageUrl: String? = null,
    @Json(name = "substituteItemDesc") val substituteItemDesc: String? = null,
    @Json(name = "substituteItemId") val substituteItemId: String? = null,
    @Json(name = "upc") val upc: String? = null,
    @Json(name = "zone") val zone: StorageType? = null,
    @Json(name = "containerId") val containerId: String? = null,
    @Json(name = "location") val location: String,
    @Json(name = "upcQty") val upcQty: Double? = null,
    @Json(name = "netWeight") val netWeight: Double? = null,
    @Json(name = "displayType") val displayType: Int? = null,
    @Json(name = "orderedItemImageUrl") val orderedItemImageUrl: String,
    @Json(name = "orderedItemDesc") val orderedItemDesc: String,
    @Json(name = "orderedItemId") val orderedItemId: String,
) : Parcelable, Dto

fun build1PLRejectedList(listOfRejectedItems: List<TotesPerZone>): List<RejectedItem> {
    val rejectedItems = arrayListOf<RejectedItem>()
    listOfRejectedItems.forEach { totePerZone ->
        rejectedItems.addAll(totePerZone.toRejectedItems())
    }
    return rejectedItems
}

fun TotesPerZone.toRejectedItems(): MutableList<RejectedItem> {
    val rejectedItemList = mutableListOf<RejectedItem>()
    if (isNeededToBeSplit()) {
        repeat(getQty()?.toInt() ?: 0) {
            rejectedItemList.add(createRejectedItem(splitId = it))
        }
    } else {
        rejectedItemList.add(createRejectedItem())
    }
    return rejectedItemList
}

fun TotesPerZone.createRejectedItem(splitId: Int? = null): RejectedItem {
    return RejectedItem(
        upcOrPlu = upc,
        itemType = sellByWeightInd,
        itemDesc = if (isSubstitution == true) substituteItemDesc else orderedItemDesc,
        itemId = if (isSubstitution == true) substituteItemId else orderedItemId,
        imageUrl = if (isSubstitution == true) substituteItemImageUrl else orderedItemImageUrl,
        originalItemId = orderedItemId,
        qty = if (splitId != null) 1 else getQty()?.toInt(),
        displayType = displayType,
        weight = getWeightFromUpc(),
        regulated = isRegulated,
        splitId = splitId,
        containerId = containerId,
        isRemovedInEdit = isRemovedInEdit
    )
}

fun TotesPerZone.isNeededToBeSplit(): Boolean =
    this.sellByWeightInd?.thatNeedsToBeSplit() == true && (this.getQty()?.toInt() ?: 1) > 1

fun TotesPerZone.getQty(): Double? {
    return when (sellByWeightInd) {
        SellByType.Each -> qty
        else -> upcQty
    }
}

fun TotesPerZone.getWeightFromUpc(): Double? {
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
