package com.albertsons.acupick.ui.util

import android.content.Context
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ShortReasonCode

/** Returns the display string for the given enum value */
fun ShortReasonCode.displayString(context: Context): String {
    return when (this) {
        ShortReasonCode.OUT_OF_STOCK -> context.getString(R.string.short_out_of_stock)
        ShortReasonCode.TOTE_FULL -> context.getString(R.string.short_tote_full)
        ShortReasonCode.PREP_NOT_READY -> context.getString(R.string.short_prep_not_ready)
        ShortReasonCode.PICK_LATER, ShortReasonCode.PRE_PICK_ISSUE_SCANNING -> context.getString(R.string.pick_later_label)
    }
}

/** Returns the display string for the given enum value */
fun ShortReasonCode.displayStringWithColor(context: Context): Pair<String, Int> {
    return when (this) {
        ShortReasonCode.OUT_OF_STOCK -> context.getString(R.string.short_out_of_stock) to R.color.semiLightRed
        ShortReasonCode.TOTE_FULL -> context.getString(R.string.short_tote_full) to R.color.semiLightRed
        ShortReasonCode.PREP_NOT_READY -> context.getString(R.string.short_prep_not_ready) to R.color.semiLightRed
        ShortReasonCode.PICK_LATER, ShortReasonCode.PRE_PICK_ISSUE_SCANNING -> context.getString(R.string.pick_later_label) to R.color.semiLightRed
    }
}
