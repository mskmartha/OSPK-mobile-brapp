package com.albertsons.acupick.ui.util

import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.widget.TextView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout

/**
 * Extension function wrapper around String.equals(other, ignoreCase = true) to be less verbose and easier to add/remove.
 *
 * Using String.equals(other, ignoreCase = true) requires changing from string1 == string 2 to string1.equals(string2, ignoreCase = true).
 * It is simpler/more readable in some instances to just change to string1.equalsIgnoreCase(string2).
 *
 * @see [String.equals]
 */
fun String.equalsIgnoreCase(other: String?): Boolean = this.equals(other, ignoreCase = true)

/**
 * Extension function for getting the text view from a tab
 *
 * searches all views and finds the views that are text and contain this.text
 * returns first as a textView or null
 */
fun TabLayout.Tab?.getTabTextView(): TextView? {
    val views = arrayListOf<View>()
    this?.view?.findViewsWithText(views, this.text, View.FIND_VIEWS_WITH_TEXT)
    return views.firstOrNull() as? TextView
}

//  TODO - Challenge here: Rewrite to into tailrec function.
val Context.lifeCycleScope: LifecycleCoroutineScope?
    get() {
        var context: Context = this
        while (context !is LifecycleOwner) {
            context = (context as ContextWrapper).baseContext
        }
        return (context as? LifecycleOwner)?.lifecycleScope
    }

/** Extension function to turn a null boolean into a false */
fun Boolean?.orFalse(): Boolean = this ?: false

/** Extension function to turn a null boolean into a true */
fun Boolean?.orTrue(): Boolean = this ?: true

/** Check if a String is null or equal to another String */
fun String?.isNullOrEqualTo(other: String?) = this.isNullOrEmpty() || this == other

fun Int?.notZeroOrNull() = this != null && this > 0

fun Int?.zeroOrNull() = this == null || this == 0

fun Int?.getOrZero() = this ?: 0

fun Int?.getOrEmpty() = if (this == 0) "" else this?.toString() ?: ""

fun String.annotateBoldWord(word: String?): AnnotatedString {
    return buildAnnotatedString {
        if (word.isNullOrEmpty()) {
            append(this@annotateBoldWord)
        } else {
            val startIndex = this@annotateBoldWord.indexOf(word)
            if (startIndex != -1) {
                append(this@annotateBoldWord.substring(0, startIndex))
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(word)
                }
                append(this@annotateBoldWord.substring(startIndex + word.length))
            } else {
                append(this@annotateBoldWord)
            }
        }
    }
}
