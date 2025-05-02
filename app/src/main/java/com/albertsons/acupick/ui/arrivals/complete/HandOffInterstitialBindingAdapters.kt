package com.albertsons.acupick.ui.arrivals.complete

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.HandOffAction

@BindingAdapter("app:action")
fun TextView.setAction(action: HandOffAction) {
    text = context.getString(
        when (action) {
            HandOffAction.COMPLETE -> R.string.interstitial_action_completed
            HandOffAction.CANCEL -> R.string.interstitial_action_canceled
            HandOffAction.COMPLETE_WITH_EXCEPTION -> R.string.empty
        }
    )
}

@BindingAdapter("app:rxaction")
fun TextView.setRxAction(action: HandOffAction) {
    text = context.getString(
        when (action) {
            HandOffAction.COMPLETE -> R.string.interstitial_action_completed
            HandOffAction.CANCEL -> R.string.interstitial_action_canceled
            HandOffAction.COMPLETE_WITH_EXCEPTION -> R.string.interstitial_action_completed
        }
    )
}
