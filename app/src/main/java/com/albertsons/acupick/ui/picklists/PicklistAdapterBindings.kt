package com.albertsons.acupick.ui.picklists

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.R
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.infrastructure.utils.toTwoDecimalString

@BindingAdapter("app:remainingWeight", "app:totalQuantity")
fun TextView.setTotalWeightOrQuantity(remainingWeight: String?, totalQuantity: Int) {
    text = if (remainingWeight.isNotNullOrEmpty()) remainingWeight else totalQuantity.toString()
}

@BindingAdapter("app:isDisplayType3Enabled", "app:fulfilledWeight", "app:totalWeight", "app:orderedWeight")
fun TextView.setOutOfQuantity(isDisplayType3Enabled: Boolean, fulfilledWeight: Double?, totalWeight: String?, orderedWeight: Double?) {
    val total = if (isDisplayType3Enabled) orderedWeight.toTwoDecimalString() else totalWeight
    text = context.getString(R.string.fit_weight, fulfilledWeight.toTwoDecimalString(), total)
}
