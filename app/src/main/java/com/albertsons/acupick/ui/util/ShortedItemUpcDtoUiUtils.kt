package com.albertsons.acupick.ui.util

import android.app.Application
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.ShortedItemUpcDto
import com.albertsons.acupick.infrastructure.utils.roundToIntOrZero
import com.albertsons.acupick.ui.itemdetails.ItemAction
import com.albertsons.acupick.ui.itemdetails.ItemActionBackingType

fun ShortedItemUpcDto.toItemAction(app: Application, item: ItemActivityDto): ItemAction {
    val quantity = exceptionQty.roundToIntOrZero().toString()
    val exceptionReasonCodeWithFallback = exceptionReasonCode?.displayString(app) ?: app.getString(R.string.fallback_short_exception_copy)
    val containerId = app.getString(R.string.dash)

    return ItemAction(
        qty = quantity,
        description = item.itemDescription.orEmpty(),
        containerId = containerId,
        isSubstitution = false,
        backingType = ItemActionBackingType.Short(this),
        upcPlu = exceptionReasonCodeWithFallback,
        imageUrl = item.imageUrl.orEmpty(),
        sellByType = item.sellByWeightInd ?: SellByType.RegularItem,
        isPWItem = item.sellByWeightInd == SellByType.PriceWeighted
    )
}
