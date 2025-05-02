package com.albertsons.acupick.ui.util

import android.graphics.drawable.Drawable
import android.text.InputFilter
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.google.android.material.textfield.TextInputLayout
import java.util.regex.Pattern

@BindingAdapter(value = ["errorMessageByInt", "editTextDrawable"], requireAll = false)
fun setErrorMessageByInt(textInputLayout: TextInputLayout, @StringRes message: Int?, drawable: Drawable?) {
    if (message == null && textInputLayout.error == null) return

    message?.let { message ->
        textInputLayout.error = ""
        textInputLayout.error = textInputLayout.context.getString(message)
        textInputLayout.editText?.let {
            it.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null, null, drawable, null
            )
        }
    } ?: run {
        textInputLayout.error = null
        textInputLayout.editText?.let {
            it.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null, null, null, null
            )
        }
    }
}

@BindingAdapter("requestFocus")
fun requestFocus(view: EditText, requestFocus: Boolean) {
    if (requestFocus) {
        view.apply {
            isFocusableInTouchMode = true
            requestFocus()
            forceShowKeyboard()
        }
    }
}

@BindingAdapter("setFilter")
fun setFilter(view: EditText, setFilter: Boolean) {
    if (setFilter) view.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(5, 1))
}

internal class DecimalDigitsInputFilter(digitsBeforeZero: Int, digitsAfterZero: Int) : InputFilter {
    private val mPattern: Pattern
    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        val matcher = mPattern.matcher(dest)
        return if (!matcher.matches()) "" else null
    }

    init {
        mPattern = Pattern.compile("[0-9]{0," + (digitsBeforeZero - 1) + "}+((\\.[0-9]{0," + (digitsAfterZero - 1) + "})?)||(\\.)?")
    }
}

@BindingAdapter(value = ["errorMessageByString", "editTextDrawableByString"], requireAll = false)
fun setErrorMessageByString(textInputLayout: TextInputLayout, message: String?, drawable: Drawable?) {
    textInputLayout.error = message
    textInputLayout.editText?.setCompoundDrawablesRelativeWithIntrinsicBounds(
        null, null, if (message.isNotNullOrEmpty()) drawable else null, null
    )
}

@BindingAdapter(value = ["startDrawable"])
fun AppCompatTextView.setDrawable(drawable: Drawable?) {
    setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
}

@BindingAdapter(value = ["startDrawableInt"])
fun AppCompatTextView.setDrawable(drawable: Int?) {
    setCompoundDrawablesRelativeWithIntrinsicBounds(drawable?.let { ContextCompat.getDrawable(this.context, it) }, null, null, null)
}

fun TextInputLayout.setErrorMessageWithIcon(errorMessage: String, errorIcon: Drawable) {
    error = SpannableStringBuilder("  $errorMessage").apply {
        setSpan(ImageSpan(errorIcon), 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    }
}
