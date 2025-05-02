package com.albertsons.acupick.ui.staging

import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.R

// TODO - Move these to binding adapter files
@BindingAdapter("totalCount")
fun TextView.setTotalCount(count: Int) {
    text = context.getString(R.string.total_format, count)
}

@BindingAdapter("scannedCount")
fun TextView.setScannedCount(count: Int) {
    text = context.getString(R.string.scanned_format, count)
}

@BindingAdapter(value = ["scannedCount", "totalCount"])
fun TextView.setProportionMarker(scannedCount: Int, totalCount: Int) {
    text = context.getString(R.string.proportion_marker_format, scannedCount, totalCount)
}

@BindingAdapter(value = ["isShowingScanPrompt", "isScanSuccess"])
fun TextView.setStatusMessageColor(isShowingScanPrompt: Boolean, isScanSuccess: Boolean) {
    setTextColor(
        ContextCompat.getColor(
            context,
            when {
                isShowingScanPrompt -> R.color.grey_600
                isScanSuccess -> R.color.statusGreen
                else -> R.color.error
            }
        )
    )
    val drawable = when {
        isShowingScanPrompt -> null
        isScanSuccess -> ContextCompat.getDrawable(context, R.drawable.ic_confirm_green)
        else -> ContextCompat.getDrawable(context, R.drawable.ic_cancel_red)
    }
    setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
}
