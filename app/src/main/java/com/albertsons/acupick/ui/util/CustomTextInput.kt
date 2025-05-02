package com.albertsons.acupick.ui.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.CustomTextInputBinding
import com.albertsons.acupick.ui.auth.onImeClicked
import com.albertsons.acupick.ui.bindingadapters.setVisibilityGoneIfTrue
import com.albertsons.acupick.ui.manualentry.handoff.DRAWABLE_END
import com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
import timber.log.Timber
import java.text.DecimalFormat

class CustomTextInput @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = CustomTextInputBinding.inflate(LayoutInflater.from(context), this, false)
    val textInputLayout = binding.textInputLayout
    val textInputEditText = binding.textInputEditText
    val errorImg = binding.errorImg
    val errorTv = binding.errorTv
    var isInputTypePassword = false // This variable is used to identify inputbox is password
    private var isPassword = true
    var isError: Boolean = false
    private var isAutoDecimalEnabled = false
    private var current: String? = ""

    companion object {
        private const val MAX_AUTO_DECIMAL_TEXT_LENGTH = 6
    }

    init {
        addView(binding.root)
        textInputEditText.setOnFocusChangeListener { _, _ ->
            setupTextField()
        }
        setupTextField()
    }

    fun setupTextField(isError: Boolean = this.isError, isAutoDecimalEnabled: Boolean = this.isAutoDecimalEnabled) {
        if (isInputTypePassword) {
            textInputLayout.isEndIconVisible = textInputEditText.text?.isNotBlank() ?: false // To hide drawable when edit box is empty only for password input box
        }
        this.isError = isError
        this.isAutoDecimalEnabled = isAutoDecimalEnabled
        textInputEditText.background = when {
            isError -> ContextCompat.getDrawable(context, R.drawable.rounded_corner_edittext_background_error)
            !textInputEditText.isEnabled -> ContextCompat.getDrawable(context, R.drawable.rounded_corner_edittext_background_disabled)
            textInputEditText.isFocused -> ContextCompat.getDrawable(context, R.drawable.rounded_corner_edittext_background_active)
            !textInputEditText.isFocused && !textInputEditText.text.isNullOrBlank() ->
                ContextCompat.getDrawable(context, R.drawable.rounded_corner_edittext_background_filled)
            !textInputEditText.isFocused -> ContextCompat.getDrawable(context, R.drawable.rounded_corner_edittext_background_default)
            else -> null
        }

        if (isAutoDecimalEnabled) {
            setupAutoDecimal()
        }
    }

    fun convertNumber(number: Double): String {
        val decimalFormat = DecimalFormat("00.000")
        return decimalFormat.format(number / 1000.0)
    }

    fun isFirstTwoDigitsNonZero(number: String): Boolean {
        Timber.d("numberString: $number")
        return number.length >= 2 && number[0] != '0' && number[1] != '0'
    }
    var isOnTextChanged = false

    private fun setupAutoDecimal() {
        textInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(s: CharSequence, i: Int, i1: Int, i2: Int) {
                Timber.d("onTextChanged: $s")
                if (s.toString() != current && !isOnTextChanged) {
                    isOnTextChanged = true
                    Timber.d("onTextChanged inside: $current")
                    if (isFirstTwoDigitsNonZero(current ?: "0.0")) {
                        textInputEditText.filters = arrayOf(InputFilter.LengthFilter(MAX_AUTO_DECIMAL_TEXT_LENGTH))
                    } else {
                        textInputEditText.filters = arrayOf()
                    }
                    textInputEditText.removeTextChangedListener(this)
                    val cleanString: String = s.toString().replace("[.]".toRegex(), "").replace("\\s+".toRegex(), "")
                    if (cleanString.isNotEmpty()) {
                        try {
                            val parsed: Double = cleanString.toDouble()
                            Timber.d("parsed: $parsed")

                            val formatted = convertNumber(parsed).padStart(6, '0')
                            Timber.d("formatted: $formatted")
                            current = formatted

                            if (formatted == "00.000")
                                textInputEditText.setText("")
                            else {
                                textInputEditText.setText(formatted)
                                textInputEditText.setSelection(formatted?.length.getOrZero())
                            }
                        } catch (_: NumberFormatException) {
                        }
                    }
                    textInputEditText.addTextChangedListener(this)
                    isOnTextChanged = false
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })
    }

    fun setAsPassword() {
        setAsPassword(isPassword)
        textInputLayout.setEndIconOnClickListener {
            isPassword = !isPassword
            setAsPassword(isPassword)
        }
    }

    fun clearEditTextFocus() {
        textInputEditText.clearFocus()
    }

    private fun setAsPassword(isPasswordVisible: Boolean) {
        textInputEditText.transformationMethod = if (isPasswordVisible) PasswordTransformationMethod.getInstance() else null
        textInputLayout.endIconDrawable = ContextCompat.getDrawable(
            context,
            if (isPasswordVisible) R.drawable.ic_hide_password
            else R.drawable.ic_view_password
        )
    }
}

@BindingAdapter(value = ["app:hint"])
fun CustomTextInput.setHint(hint: String) {
    textInputLayout.hint = hint
}

@BindingAdapter(value = ["app:typedText", "app:isEnabled"], requireAll = false)
fun CustomTextInput.setTypedText(value: String?, isEnabled: Boolean? = true) {
    if (textInputEditText.text.toString() != value) {
        textInputEditText.setText(value)
    }
    textInputEditText.isEnabled = isEnabled ?: true
    setupTextField()
}

@BindingAdapter("app:isPassword")
fun CustomTextInput.setPassword(isPassword: Boolean? = false) {
    textInputLayout.endIconMode = END_ICON_PASSWORD_TOGGLE
    isPassword?.let {
        isInputTypePassword = isPassword
    }
    setAsPassword()
}

@InverseBindingAdapter(attribute = "app:typedText")
fun CustomTextInput.getTypedText(): String {
    return textInputEditText.text.toString()
}

@BindingAdapter("app:typedTextAttrChanged")
fun CustomTextInput.setTypedTextListener(listener: InverseBindingListener?) {
    if (listener != null) {
        textInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                listener.onChange()
            }
        })
    }
}

@BindingAdapter(value = ["app:errorDrawable", "app:errorTextResId"], requireAll = true)
fun CustomTextInput.setError(errorDrawable: Drawable?, @StringRes errorTextResId: Int?) {
    errorImg.setImageDrawable(errorDrawable)
    errorTv.text = if (errorTextResId != null) context.getString(errorTextResId) else null
    errorTv.setVisibilityGoneIfTrue(errorTextResId == null)
    errorImg.setVisibilityGoneIfTrue(errorTextResId == null)
    setupTextField(errorTextResId != null)
}

@BindingAdapter(value = ["app:onImeClick", "app:imeClickEnabled"], requireAll = false)
fun CustomTextInput.setOnImeClicked(imeClickEvent: () -> Unit, imeClickEnabled: Boolean?) {
    textInputEditText.imeOptions = EditorInfo.IME_ACTION_DONE

    textInputEditText.onImeClicked {
        if (imeClickEnabled == null || imeClickEnabled == true) imeClickEvent.invoke()
    }
}

@BindingAdapter("app:nextFocus")
fun CustomTextInput.setNextIme(nextFocusForwardId: Int) {
    textInputEditText.imeOptions = EditorInfo.IME_ACTION_NEXT
    textInputEditText.nextFocusForwardId = nextFocusForwardId
}

@BindingAdapter("app:maxLength")
fun CustomTextInput.setMaxLength(maxLength: Int?) {
    textInputEditText.filters = arrayOf(InputFilter.LengthFilter(maxLength.getOrZero()))
}

@BindingAdapter("app:isNumber")
fun CustomTextInput.setIsNumber(isNumber: Boolean?) {
    isNumber?.let { textInputEditText.inputType = InputType.TYPE_CLASS_NUMBER }
}

@BindingAdapter("app:isDobText")
fun CustomTextInput.setisDobText(isDobText: Boolean?) {
    isDobText?.let {
        textInputEditText.inputType = InputType.TYPE_CLASS_NUMBER
        textInputEditText.keyListener = DigitsKeyListener.getInstance("MDY0123456789-")
    }
}

@BindingAdapter("app:isFloatingNumber")
fun CustomTextInput.setIsFloatingNumber(isFloatingNumber: Boolean = false) {
    textInputEditText.inputType = if (isFloatingNumber) InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_NUMBER
}

@BindingAdapter("app:isLastView")
fun CustomTextInput.setIsLastView(isLastView: Boolean?) {
    if (isLastView != false) {
        textInputEditText.imeOptions = EditorInfo.IME_ACTION_DONE
    } else {
        textInputEditText.imeOptions = EditorInfo.IME_ACTION_NEXT
    }
}

@BindingAdapter("app:imeOptionDone")
fun CustomTextInput.setImeOptionDone(imeOptionDone: Boolean?) {
    imeOptionDone?.let {
        textInputEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                textInputEditText.imeOptions = EditorInfo.IME_ACTION_DONE
            }
        }
    }
}

@BindingAdapter("app:isAutoDecimalEnabled")
fun CustomTextInput.setIsAutoDecimalEnabled(isAutoDecimalEnabled: Boolean) {
    setupTextField(isAutoDecimalEnabled = isAutoDecimalEnabled)
}

@BindingAdapter("app:requestFocus")
fun CustomTextInput.setRequestFocus(isFocusRequested: Boolean?) {
    if (isFocusRequested == true) {
        textInputEditText.postDelayed({
            textInputEditText.isFocusableInTouchMode = true
            textInputEditText.requestFocus()
            textInputEditText.forceShowKeyboard()
        }, 100)
    }
}

@BindingAdapter("app:drawbleEndIcon")
fun CustomTextInput.setDrawableEndIcon(drawableEndIcon: Drawable?) {
    drawableEndIcon?.apply {
        val paddingBottom = 20
        val insetDrawable = InsetDrawable(this, 0, 0, 0, paddingBottom)
        textInputEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, insetDrawable, null)
    }
}

@BindingAdapter("app:drawableEndAction")
fun CustomTextInput.setDrawableEndAction(endIconAction: () -> Unit) {
    textInputEditText.setOnTouchListener { view, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            if (event.rawX >= right - textInputEditText.compoundDrawables[DRAWABLE_END].bounds.width() * 2) {
                view.performClick()
                endIconAction.invoke()
            }
        }
        view.onTouchEvent(event)
    }
}
