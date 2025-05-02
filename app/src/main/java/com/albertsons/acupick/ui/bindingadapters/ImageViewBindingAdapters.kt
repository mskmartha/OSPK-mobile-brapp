package com.albertsons.acupick.ui.bindingadapters

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.ui.models.FulfillmentTypeUI
import com.albertsons.acupick.ui.models.OrderItemUI
import com.albertsons.acupick.ui.util.asIcon

/** Shows appropriate icon per [value]. [View.GONE] visibility when null. */
@BindingAdapter("updateFulfillmentIcon", "app:isPartnerPickOrder")
fun AppCompatImageView.updateFulfillmentIcon(value: FulfillmentAttributeDto?, isPartnerPickOrder: Boolean?) {
    when {
        isPartnerPickOrder == true -> {
            visibility = View.VISIBLE
            setImageResource(R.drawable.ic_partnerpick)
        }
        value != null -> {
            visibility = View.VISIBLE
            setImageResource(value.asIcon())
            setColorFilter(ContextCompat.getColor(context, R.color.grey_600))
        }
        else -> {
            setImageResource(android.R.color.transparent)
            View.INVISIBLE
        }
    }
}

@BindingAdapter("updateOrderTypeIcon")
fun ImageView.updateOrderTypeIcon(value: OrderType?) {
    visibility = if (value.asIcon() != 0) {
        value?.asIcon()?.let { setImageResource(it) }
        View.VISIBLE
    } else {
        setImageResource(android.R.color.transparent)
        View.GONE
    }
}

@BindingAdapter("updateFulfillmentUIIcon")
fun ImageView.updateFulfillmentUIIcon(value: FulfillmentTypeUI?) {
    visibility = if (value != null) {
        setImageResource(value.asIcon())
        View.VISIBLE
    } else {
        setImageResource(android.R.color.transparent)
        View.GONE
    }
}

@BindingAdapter(value = ["showFulfillment3plIcon", "app:isPartnerPickOrder"])
fun ImageView.showFulfillment3plIcon(activityFulfillmentTypes: Set<FulfillmentTypeUI>, isPartnerPickOrder: Boolean) {
    this.visibility = if (!isPartnerPickOrder && activityFulfillmentTypes.contains(FulfillmentTypeUI.THREEPL)) View.VISIBLE else View.GONE
}

@BindingAdapter(value = ["showFulfillmentDugIcon", "app:isPartnerPickOrder"])
fun ImageView.showFulfillmentDugIcon(activityFulfillmentTypes: Set<FulfillmentTypeUI>, isPartnerPickOrder: Boolean) {
    this.visibility = if (!isPartnerPickOrder && activityFulfillmentTypes.contains(FulfillmentTypeUI.DUG)) View.VISIBLE else View.GONE
}

@BindingAdapter(value = ["showFulfillment1plIcon", "app:isPartnerPickOrder"])
fun ImageView.showFulfillment1plIcon(activityFulfillmentTypes: Set<FulfillmentTypeUI>, isPartnerPickOrder: Boolean) {
    this.visibility = if (!isPartnerPickOrder && activityFulfillmentTypes.contains(FulfillmentTypeUI.ONEPL)) View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("app:showPartnerPickIcon")
fun ImageView.showPartnerPickIcon(isPartnerPickOrder: Boolean) {
    if (isPartnerPickOrder) this.setImageResource(R.drawable.ic_partnerpick)
    this.visibility = if (isPartnerPickOrder) View.VISIBLE else View.GONE
}

@BindingAdapter("app:fullFillmentType")
fun ImageView.showfullFillmentTypeIcon(fullFillmentType: FulfillmentTypeUI?) {
    val fullFillmentTypeImage = when (fullFillmentType) {
        FulfillmentTypeUI.DUG -> {
            ContextCompat.getDrawable(context, R.drawable.ic_arrivals_fullfillment_dug) as Drawable
        }
        FulfillmentTypeUI.ONEPL -> {
            ContextCompat.getDrawable(context, R.drawable.ic_fullfillment_onepl) as Drawable
        }
        FulfillmentTypeUI.THREEPL -> {
            ContextCompat.getDrawable(context, R.drawable.ic_arrivals_fullfillment_threepl) as Drawable
        }
        else -> {
            ContextCompat.getDrawable(context, android.R.color.transparent)
        }
    }
    setImageDrawable(fullFillmentTypeImage)
}

@BindingAdapter("showFulfillmentWineShippingIcon")
fun ImageView.showFulfillmentWineShippingIcon(activityFulfillmentTypes: Set<FulfillmentTypeUI>) {
    this.visibility = if (activityFulfillmentTypes.contains(FulfillmentTypeUI.SHIPPING)) View.VISIBLE else View.GONE
}

@BindingAdapter("app:item", "app:is1Pl")
fun AppCompatTextView.setOrderInfoArrivals(item: OrderItemUI?, is1pl: Boolean) {
    text = if (is1pl) {
        context.resources.getQuantityString(
            R.plurals.orders_count_plural, item?.orderCount?.toInt() ?: 0,
            item?.orderCount?.toInt() ?: 0
        )
    } else {
        item?.orderNumber?.let { "#$it" }
    }
}

@DrawableRes
fun OrderType?.asIcon(): Int {
    return when (this) {
        OrderType.FLASH, OrderType.FLASH3P -> R.drawable.ic_flash
        OrderType.EXPRESS -> R.drawable.ic_express
        else -> 0
    }
}
