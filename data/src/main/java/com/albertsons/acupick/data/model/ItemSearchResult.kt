package com.albertsons.acupick.data.model

import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.response.ItemActivityDto

sealed class ItemSearchResult {
    data class MatchedItem(
        val itemActivityDto: ItemActivityDto,
    ) : ItemSearchResult()

    sealed class Error : ItemSearchResult() {
        object NoItemFound : Error()
        object SecondaryUpcScannedForWeightedItem : Error()
    }
}

/** Returns a ScannedItem based on the current state of the ItemActivityDto */
fun ItemActivityDto?.toItemSearchResult(fixedItemTypeEnabled: Boolean, itemBarcodeType: BarcodeType.Item? = null): ItemSearchResult {
    return when {
        this == null -> {
            ItemSearchResult.Error.NoItemFound
        }
        sellByWeightInd == SellByType.Weight && itemBarcodeType is BarcodeType.Item.Normal && fixedItemTypeEnabled -> {
            ItemSearchResult.Error.SecondaryUpcScannedForWeightedItem
        }
        else -> {
            ItemSearchResult.MatchedItem(this)
        }
    }
}

/** Returns a copy (of the receiver type) with all order/customer specific info nulled out */
fun ItemSearchResult.copyWithCustomerSpecificInformationRemoved(fixedItemTypeEnabled: Boolean): ItemSearchResult {
    return if (this is ItemSearchResult.MatchedItem) {
        itemActivityDto.copy(
            contactFirstName = null,
            contactLastName = null,
            contactPhoneNumber = null,
            customerOrderNumber = null,
            fulfillment = null,
            stopNumber = null,
            routeVanNumber = null,
            shortOrderNumber = null,
        ).toItemSearchResult(fixedItemTypeEnabled)
    } else ItemSearchResult.Error.NoItemFound
}

/** Returns the ItemActivityDto from a ScannedItem.MatchedItem */
fun ItemSearchResult.getItemActivityDto(): ItemActivityDto? = (this as? ItemSearchResult.MatchedItem)?.itemActivityDto

fun ItemSearchResult.isMatchedItem(): Boolean = this is ItemSearchResult.MatchedItem

fun ItemSearchResult.isWeightedItem(): Boolean = this.isMatchedItem() && this.getItemActivityDto()?.sellByWeightInd == SellByType.Weight

fun ItemSearchResult.itemIdOrNull(): String? = if (this.isMatchedItem()) this.getItemActivityDto()?.itemId else null
