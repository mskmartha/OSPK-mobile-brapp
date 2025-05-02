package com.albertsons.acupick.ui.custom

import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.QuantityPickerViewBinding

class QuantityPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: QuantityPickerViewBinding
    val valueQuantity: TextView
    val minusQuantity: ImageView
    val plusQuantity: ImageView
    var minValue: Int = 0
        private set
    var maxValue: Int = 99
        private set

    init {
        binding = QuantityPickerViewBinding.inflate(LayoutInflater.from(context), this, false)
        background = ContextCompat.getDrawable(context, R.drawable.oval_background)
        addView(binding.root)
        minusQuantity = binding.minusQuantity
        plusQuantity = binding.plusQuantity
        valueQuantity = binding.valueQuantity
        setQuantity(0)
    }

    fun incrementQuantity() {
        setQuantity(valueQuantity.text.toString().toInt() + 1)
    }

    fun setMin(value: Int) {
        minValue = value
    }

    fun setMax(value: Int) {
        maxValue = value
    }

    fun decrementQuantity() {
        setQuantity(valueQuantity.text.toString().toInt() - 1)
    }

    fun setMinusDisabledIfNeeded(quantity: Int) {
        if (quantity <= minValue) {
            minusQuantity.let {
                it.setColorFilter(ContextCompat.getColor(context, R.color.semiLightGray), PorterDuff.Mode.SRC_IN)
                it.isClickable = false
            }
        } else {
            minusQuantity.let {
                it.setColorFilter(ContextCompat.getColor(context, R.color.semiLightBlue), PorterDuff.Mode.SRC_IN)
                it.isClickable = true
            }
        }
    }

    fun setPlusDisabledIfNeeded(quantity: Int) {
        if (quantity >= maxValue) {
            plusQuantity.let {
                it.setColorFilter(ContextCompat.getColor(context, R.color.semiLightGray), PorterDuff.Mode.SRC_IN)
                it.isClickable = false
            }
        } else {
            plusQuantity.let {
                it.setColorFilter(ContextCompat.getColor(context, R.color.semiLightBlue), PorterDuff.Mode.SRC_IN)
                it.isClickable = true
            }
        }
    }
}

@BindingAdapter(value = ["app:quantity", "app:maxCount", "app:minCount"], requireAll = false)
fun QuantityPickerView.setQuantity(quantity: Int?, maxCount: Int? = null, minCount: Int? = null) {
    maxCount?.let(::setMax)
    minCount?.let(::setMin)

    if (quantity == null) {
        setQuantity(0, maxCount, minCount)
        return
    }
    if (valueQuantity.text.toString() != quantity.toString() && quantity.toInt() in minValue..maxValue) {
        valueQuantity.text = quantity.toString()
    }
    quantity.toInt().let {
        setMinusDisabledIfNeeded(it)
        setPlusDisabledIfNeeded(it)
    }
}

@InverseBindingAdapter(attribute = "app:quantity")
fun QuantityPickerView.getQuantity(): Int {
    return valueQuantity.text.toString().toInt()
}

@BindingAdapter("app:quantityAttrChanged")
fun QuantityPickerView.setQuantityChangeListener(listener: InverseBindingListener?) {
    if (listener != null) {
        plusQuantity.setOnClickListener {
            incrementQuantity()
            listener.onChange()
        }
        minusQuantity.setOnClickListener {
            decrementQuantity()
            listener.onChange()
        }
    }
}
