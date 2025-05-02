package com.albertsons.acupick.ui.substitute

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.SellByType

@BindingAdapter("app:sellByTypeInstruction")
fun TextView.setSellByTypeInstruction(sellByType: SellByType?) {
    text = when (sellByType) {
        SellByType.Weight -> context.getString(R.string.substitute_instruction_weighted)
        SellByType.Each -> context.getString(R.string.substitute_instruction_each)
        else -> context.getString(R.string.substitute_instruction_upc)
    }
}

@BindingAdapter("app:isDisplayType3PW", "app:isOrderedByWeight", "app:requestedWeightAndUnits", "app:remainingQtyCount", "app:remainingWeight")
fun TextView.setQuantityOrWeightSubstituiton(
    isDisplayType3PW: Boolean?,
    isOrderedByWeight: Boolean?,
    requestedWeightAndUnits: String?,
    remainingQtyCount: Int,
    remainingWeight: String?,
) {
    text = if (isDisplayType3PW == true) remainingWeight
    else if (isOrderedByWeight == true) requestedWeightAndUnits
    else remainingQtyCount.toString()
}
