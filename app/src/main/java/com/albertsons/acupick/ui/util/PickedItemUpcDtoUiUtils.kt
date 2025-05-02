package com.albertsons.acupick.ui.util

import android.app.Application
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ImageSizePreset
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.PickedItemUpcDto
import com.albertsons.acupick.data.model.response.isIssueScanning
import com.albertsons.acupick.data.model.response.isSubstitution
import com.albertsons.acupick.infrastructure.utils.roundToIntOrZero
import com.albertsons.acupick.ui.itemdetails.ItemAction
import com.albertsons.acupick.ui.itemdetails.ItemActionBackingType
import java.text.DecimalFormat

fun PickedItemUpcDto.toItemAction(app: Application, item: ItemActivityDto, barcodeMapper: BarcodeMapper): ItemAction {

    val barcodeType = barcodeMapper.inferBarcodeType(upc.orEmpty())
    val weight = (barcodeType as? BarcodeType.Item.Weighted)?.weight ?: 0.0
    val weightString = DecimalFormat(".##").format(weight)
    val itemWeightUom = item.itemWeightUom ?: app.resources.getString(R.string.uom_default)
    val quantity = this.qty.roundToIntOrZero().toString()

    val quantityAndWeight = when (barcodeType) {
        is BarcodeType.Item.Normal, is BarcodeType.Item.Short -> app.getString(R.string.item_details_1_item)
        is BarcodeType.Item.Each, is BarcodeType.Item.Priced -> quantity
        is BarcodeType.Item.Weighted -> {
            val weightAndUom = "$weightString $itemWeightUom"
            "$quantity ($weightAndUom)"
        }
        else -> ""
    }

    val upcPlu = when (barcodeType) {
        is BarcodeType.Item.Normal, is BarcodeType.Item.Short -> barcodeType.rawBarcode
        is BarcodeType.Item.Weighted -> barcodeType.plu
        is BarcodeType.Item.Each -> barcodeType.plu
        is BarcodeType.Item.Priced -> barcodeType.plu
        else -> ""
    }

    val tote = containerId ?: ""

    val isSubstitiution = this.isSubstitution()
    val isIssueScanned = this.isIssueScanning()

    /**
     * In unpick screen we have to shown substituted and issue reported item details
     */
    val imageUrl = if (isSubstitiution || isIssueScanned) {
        this.substituteItemImageUrl.orEmpty()
    } else item.sizedImageUrl(ImageSizePreset.ItemDetails)

    val description = if (isSubstitiution || isIssueScanned) {
        this.substituteItemDesc.orEmpty()
    } else item.itemDescription.orEmpty()

    val pickedItemUpcOrPlu = if (isSubstitiution || isIssueScanned) {
        barcodeType.getFormattedValue(context = app, hideUnits = true)
    } else {
        item.asUpcOrPlu(app, barcodeMapper)
    }

    return ItemAction(
        qty = quantityAndWeight,
        description = description,
        upcPlu = pickedItemUpcOrPlu,
        containerId = tote,
        isSubstitution = isSubstitiution,
        isIssueScanned = isIssueScanned,
        backingType = if (isSubstitution == true) ItemActionBackingType.Substitution(this) else ItemActionBackingType.Pick(this),
        imageUrl = imageUrl,
        sellByType = item.sellByWeightInd ?: SellByType.RegularItem,
        isPWItem = item.sellByWeightInd == SellByType.PriceWeighted
    )
}
