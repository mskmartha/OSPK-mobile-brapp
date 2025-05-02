package com.albertsons.acupick.ui.util

import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.FulfillmentSubType
import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.ui.models.FulfillmentTypeUI

@DrawableRes
fun FulfillmentAttributeDto.asIcon(isToolbar: Boolean? = null): Int {
    return when (this.type) {
        FulfillmentType.DUG -> R.drawable.ic_fullfillment_dug
        FulfillmentType.SHIPPING -> R.drawable.ic_shipping
        FulfillmentType.DELIVERY -> {
            when (this.subType) {
                FulfillmentSubType.THREEPL -> R.drawable.ic_fullfillment_threepl
                else -> R.drawable.ic_fullfillment_onepl
            }
        }
        // TODO: Should there be a different fallback icon?
        else -> R.drawable.ic_fullfillment_dug
    }
}

fun FulfillmentTypeUI.asIcon(): Int {
    return when (this) {
        FulfillmentTypeUI.DUG -> R.drawable.ic_fullfillment_dug
        FulfillmentTypeUI.THREEPL -> R.drawable.ic_arrivals_fullfillment_threepl
        FulfillmentTypeUI.SHIPPING -> R.drawable.ic_shipping
        else -> R.drawable.ic_1_pl
    }
}

fun FulfillmentAttributeDto.toFulfillmentTypeUI(): FulfillmentTypeUI? {
    return when (subType) {
        FulfillmentSubType.THREEPL -> FulfillmentTypeUI.THREEPL
        FulfillmentSubType.ONEPL -> FulfillmentTypeUI.ONEPL
        else -> {
            when (type) {
                FulfillmentType.DUG -> FulfillmentTypeUI.DUG
                // used for wine shipping
                FulfillmentType.SHIPPING -> FulfillmentTypeUI.SHIPPING
                // All known delivery subtypes handled above
                FulfillmentType.DELIVERY, null -> null
            }
        }
    }
}

@BindingAdapter("app:setFulfillmentTypeText")
fun AppCompatTextView.setFulfillmentTypeText(fulfillmentTypeUI: FulfillmentTypeUI) {
    text = when (fulfillmentTypeUI) {
        FulfillmentTypeUI.THREEPL -> "${context.getString(R.string.threepl)}: "
        FulfillmentTypeUI.ONEPL -> "${context.getString(R.string.onePl)}: "
        FulfillmentTypeUI.DUG -> "${context.getString(R.string.dug)}: "
        FulfillmentTypeUI.SHIPPING -> "${context.getString(R.string.wine_shipping)}: "
    }
}
