package com.albertsons.acupick.data.picklist

import com.albertsons.acupick.data.model.QuantitySelectionType
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.response.ItemActivityDto

const val MIN_QUANTITY = 2 // Show Quantity Picker in both Picking and Subs Flow for any item with a requested quantity of 2 or more

fun getQuantitySelectionType(
    item: ItemActivityDto?,
    barcodeType: BarcodeType
): QuantitySelectionType {
    val quantity = item?.qty?.toInt() ?: 0
    return when (barcodeType) {
        is BarcodeType.Item.Priced -> when {
            (item?.sellByWeightInd == SellByType.PriceScaled || item?.sellByWeightInd == SellByType.PriceEachTotal) -> QuantitySelectionType.ConfirmAmount
            quantity < MIN_QUANTITY -> QuantitySelectionType.None
            item?.isPrepped() != true -> QuantitySelectionType.QuantityPicker
            item.sellByWeightInd == SellByType.Each -> QuantitySelectionType.QuantityPicker
            else -> QuantitySelectionType.None
        }
        is BarcodeType.Item.Weighted -> when {
            quantity < MIN_QUANTITY -> QuantitySelectionType.None
            item?.isOrderedByWeight() != true -> QuantitySelectionType.QuantityPicker
            item.sellByWeightInd == SellByType.Each -> QuantitySelectionType.QuantityPicker
            else -> QuantitySelectionType.None
        }
        else -> when {
            quantity >= MIN_QUANTITY -> QuantitySelectionType.QuantityPicker
            else -> QuantitySelectionType.None
        }
    }
}

fun getQuantitySelectionTypeForIssueScanning(
    barcodeType: BarcodeType
): QuantitySelectionType {
    return when (barcodeType) {
        is BarcodeType.Item.Priced -> QuantitySelectionType.None
        else -> QuantitySelectionType.QuantityPicker
    }
}

fun getQuantitySelectionTypeForSubstitution(
    barcodeType: BarcodeType,
    requestedQty: Int,
    orderedByWeight: Boolean?
): QuantitySelectionType {
    return when {
        barcodeType is BarcodeType.Item.Priced -> QuantitySelectionType.None
        orderedByWeight == true -> QuantitySelectionType.None
        requestedQty >= MIN_QUANTITY -> QuantitySelectionType.QuantityPicker
        else -> QuantitySelectionType.None
    }
}
