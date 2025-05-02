package com.albertsons.acupick.data.model.barcode

import com.albertsons.acupick.data.model.SellByType

fun BarcodeType.getUpcQty(
    fulfilledQuantity: Double,
    sellByType: SellByType? = null,
    fixedItemTypeEnabled: Boolean,
): Double = when {
    this is BarcodeType.Item.Weighted -> 1.0
    this is BarcodeType.Item.Priced && sellByType != SellByType.PriceEachUnique && fixedItemTypeEnabled -> 1.0
    sellByType == SellByType.PriceWeighted -> 1.0
    sellByType == SellByType.PriceEach -> 1.0
    else -> fulfilledQuantity
}
