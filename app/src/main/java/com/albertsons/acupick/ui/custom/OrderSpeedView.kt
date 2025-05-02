package com.albertsons.acupick.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.databinding.OrderSpeedViewBinding
import com.albertsons.acupick.ui.bindingadapters.setVisibilityGoneIfTrue

class OrderSpeedView@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyle, defStyleRes) {

    val binding: OrderSpeedViewBinding

    init {
        val inflater = LayoutInflater.from(context)
        binding = OrderSpeedViewBinding.inflate(inflater, this, true)
    }
}

@BindingAdapter("app:orderTypeIcon")
fun OrderSpeedView.setOrderSpeedIcon(orderType: OrderType?) {
    when (orderType) {
        OrderType.FLASH -> R.drawable.ic_flash
        OrderType.EXPRESS -> R.drawable.ic_express
        else -> null
    }?.let { binding.orderSpeedPillImage.setImageResource(it) }
    setVisibilityGoneIfTrue(orderType == OrderType.REGULAR || orderType == null)
}

@BindingAdapter("app:orderTypeText")
fun OrderSpeedView.setOrderSpeedText(text: String) {
    binding.orderSpeedPillTv.text = text
}
