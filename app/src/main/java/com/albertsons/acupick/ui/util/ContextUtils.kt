package com.albertsons.acupick.ui.util

import android.content.Context
import android.graphics.Typeface
import android.text.style.MetricAffectingSpan
import android.text.style.StyleSpan
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import com.albertsons.acupick.ui.text.TypefaceSpan

enum class TypefaceStyle(val value: Int) {
    NORMAL(Typeface.NORMAL),
    BOLD(Typeface.BOLD),
    ITALIC(Typeface.ITALIC),
    BOLD_ITALIC(Typeface.BOLD_ITALIC)
}

fun Context.getFontSpanWithFallback(@FontRes fontResId: Int, fallbackTypefaceStyle: TypefaceStyle): MetricAffectingSpan {
    return ResourcesCompat.getFont(this, fontResId)?.let { TypefaceSpan(it) } ?: run {
        StyleSpan(fallbackTypefaceStyle.value)
    }
}
