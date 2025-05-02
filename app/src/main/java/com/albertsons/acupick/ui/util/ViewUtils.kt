package com.albertsons.acupick.ui.util

import android.content.ContextWrapper
import android.content.res.Resources
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner

/** Returns the string name for the resource id or "no-id" if there is no id set on the resource. */
fun View?.idName(): String {
    if (this == null) return ""
    return id.resourceIdName(resources)
}

/** Returns the string name for the resource id or "no-id" if there is no id set on the resource. */
fun Int.resourceIdName(resources: Resources): String {
    return if (this == -0x1) {
        "no-id"
    } else {
        resources.getResourceEntryName(this)
    }
}

fun View.setStartMargin(dimenPx: Int) {
    val layoutParams = this.layoutParams as ViewGroup.MarginLayoutParams
    layoutParams.marginStart = dimenPx
    this.layoutParams = layoutParams
}

fun View.setTopMargin(dimenPx: Int) {
    val layoutParams = this.layoutParams as ViewGroup.MarginLayoutParams
    layoutParams.topMargin = dimenPx
    this.layoutParams = layoutParams
}

fun View.lifecycleOwner(): LifecycleOwner? {
    var curContext = this.context
    var maxDepth = 20
    while (maxDepth-- > 0 && curContext !is LifecycleOwner) {
        curContext = (curContext as ContextWrapper).baseContext
    }
    return if (curContext is LifecycleOwner) {
        curContext as LifecycleOwner
    } else {
        null
    }
}

val Int.dpToPx: Int
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()

/**
Since TypefaceSpan only supports Android P, CustomTypefaceSpan is used for applying fonts via SpannableString pre Android P
see: https://youtu.be/x-FcOX6ErdI?t=486
**/
class CustomTypefaceSpan(val font: Typeface?) : MetricAffectingSpan() {
    override fun updateMeasureState(textPaint: TextPaint) = update(textPaint)
    override fun updateDrawState(textPaint: TextPaint?) = update(textPaint)

    private fun update(tp: TextPaint?) {
        tp.apply {
            val old = this!!.typeface
            val oldStyle = old?.style ?: 0
            val font = Typeface.create(font, oldStyle)
            typeface = font
        }
    }
}
