package com.albertsons.acupick.ui.util

import android.content.Context
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.barcode.BarcodeType

fun BarcodeType.getFormattedValue(context: Context, hideUnits: Boolean = false): String {
    return when (this) {
        is BarcodeType.Item.Normal -> context.getString(R.string.item_details_upc_format, rawBarcode)
        is BarcodeType.Item.Short -> context.getString(R.string.item_details_upc_format, upcA)
        is BarcodeType.Item.Weighted -> {
            if (hideUnits) {
                context.getString(R.string.item_details_plu_format, plu)
            } else {
                context.getString(R.string.item_details_plu_weighted_format, plu, context.getString(R.string.uom_default))
            }
        }
        is BarcodeType.Item.Priced -> context.getString(R.string.item_details_slu_format, plu)
        is BarcodeType.Item.Each -> context.getString(R.string.item_details_plu_format, plu)
        else -> rawBarcode
    }
}
