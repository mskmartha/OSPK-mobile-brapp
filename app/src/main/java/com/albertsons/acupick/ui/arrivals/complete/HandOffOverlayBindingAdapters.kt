package com.albertsons.acupick.ui.arrivals.complete

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.R

@BindingAdapter("app:overlayColor")
fun FrameLayout.overlayColor(isCancelled: Boolean) {
    backgroundTintList = if (isCancelled) {
        ContextCompat.getColorStateList(context, R.color.verificationRed)
    } else {
        ContextCompat.getColorStateList(context, R.color.darkBlue)
    }
}

@BindingAdapter("app:iconSetup")
fun View.iconSetup(isCancelled: Boolean) {
    foreground = if (isCancelled) {
        ContextCompat.getDrawable(this.context, R.drawable.ic_handoff_cancel)
    } else {
        ContextCompat.getDrawable(this.context, R.drawable.handoff_complete)
    }
}

@BindingAdapter("overlayTextColor")
fun TextView.overlayTextColor(isCancelled: Boolean) {
    if (isCancelled) {
        this.setTextColor(this.context.getColor(R.color.darkestOrange))
    } else {
        this.setTextColor(this.context.getColor(R.color.darkBlue))
    }
}

@BindingAdapter("overlayText")
fun TextView.overlayText(isCancelled: Boolean) {
    text = if (isCancelled) {
        this.context.getString(R.string.interstitial_action_canceled)
    } else {
        this.context.getString(R.string.handoff_completed)
    }
}

@BindingAdapter("app:iconHeight")
fun View.iconHeight(height: Float) {
    val layoutParams = this.layoutParams
    layoutParams.height = height.toInt()
    this.layoutParams = layoutParams
}

@BindingAdapter("app:iconWidth")
fun View.iconWidth(height: Float) {
    val layoutParams = this.layoutParams
    layoutParams.width = height.toInt()
    this.layoutParams = layoutParams
}
