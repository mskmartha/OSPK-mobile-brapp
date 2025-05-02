package com.albertsons.acupick.ui.manualentry.pick

import androidx.databinding.BindingAdapter
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.manualentry.ManualEntryType
import com.albertsons.acupick.ui.manualentry.ManualEntryUpcUi
import com.albertsons.acupick.ui.util.CustomTextInput
import com.albertsons.acupick.ui.util.setHint
import com.google.android.material.textfield.TextInputLayout

@BindingAdapter("upcHint")
fun TextInputLayout.setUpcHint(upcUI: ManualEntryUpcUi?) {
    upcUI?.let {
        hint = context.getString(
            when (upcUI.entryType) {
                ManualEntryType.Barcode -> R.string.manual_barcode_hint
                else -> R.string.manual_upc_hint
            }
        )
    }
}
@BindingAdapter("isBoxEntry")
fun TextInputLayout.setisBoxEntry(isBoxEntry: Boolean?) {
    isBoxEntry?.let {
        hint = context.getString(if (it) R.string.manual_box_hint else R.string.manual_bag_hint)
    }
}
@BindingAdapter("upcHint")
fun CustomTextInput.setUpcHint(upcUI: ManualEntryUpcUi?) {
    upcUI?.let {
        setHint(
            context.getString(
                when (upcUI.entryType) {
                    ManualEntryType.Barcode -> R.string.manual_barcode_hint
                    else -> R.string.manual_upc_hint
                }
            )
        )
    }
}
