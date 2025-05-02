package com.albertsons.acupick.ui.fieldservices

import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.R
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.arrivals.complete.DateTextWatcher
import com.albertsons.acupick.ui.arrivals.complete.GhostTextFocusChangeListener
import com.albertsons.acupick.ui.arrivals.complete.GhostTextWatcher
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

@BindingAdapter("app:fieldServiceOperationState")
fun TextView.setFieldServiceOperationState(fieldServiceOperationState: FieldServiceOperationState) {
    when (fieldServiceOperationState) {
        FieldServiceOperationState.Unknown -> {
            text = ""
            setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
        FieldServiceOperationState.Success -> {
            text = context.getString(R.string.test_success)
            setTextColor(ContextCompat.getColor(context, R.color.statusGreen))
            TextViewCompat.setCompoundDrawableTintList(this, ContextCompat.getColorStateList(context, R.color.statusGreen))
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_confirm_16dp, 0, 0, 0)
        }
        FieldServiceOperationState.Failure -> {
            text = context.getString(R.string.test_failure)
            setTextColor(ContextCompat.getColor(context, R.color.error))
            TextViewCompat.setCompoundDrawableTintList(this, ContextCompat.getColorStateList(context, R.color.error))
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_alert_16dp, 0, 0, 0)
        }
    }
}

@BindingAdapter("app:fieldServiceOperationState")
fun TextInputLayout.setFieldServiceOperationState(fieldServiceOperationState: FieldServiceOperationState) {
    // manually update hint text/box colors to look like an error in order to allow the status textview to be positioned visually directly underneath the TextInputLayout
    // without a gap where the error message would be displayed
    when (fieldServiceOperationState) {
        FieldServiceOperationState.Unknown,
        FieldServiceOperationState.Success -> {
            hintTextColor = ContextCompat.getColorStateList(context, R.color.text_input_active_state_selector)
            ContextCompat.getColorStateList(context, R.color.text_input_active_state_selector)?.let { setBoxStrokeColorStateList(it) }

            // Note: Alt way to change TextInputLayout is to set the error BUT doing so leaves extra empty bottom since we would have to show a space character to force the error UI
            // error = null
            // setErrorIconDrawable(android.R.color.transparent)
            // isErrorEnabled = false
        }
        FieldServiceOperationState.Failure -> {
            hintTextColor = ContextCompat.getColorStateList(context, R.color.error)
            boxStrokeColor = ContextCompat.getColor(context, R.color.error)

            // Note: Alt way to change TextInputLayout is to set the error BUT doing so leaves extra empty bottom since we would have to show a space character to force the error UI
            // Not using the TextInputLayout error message BUT have to display something to get the rest of the UI in the error state (red outline) so just display a space (empty string won't work)
            // error = ""
            // setErrorIconDrawable(android.R.color.transparent)
            // isErrorEnabled = true
        }
    }
}

@BindingAdapter(value = ["app:setStyleByState", "app:isViewEnabled"], requireAll = false)
fun TextInputLayout.setStyleByState(string: String?, isViewEnabled: Boolean?) {
    defaultHintTextColor = when {
        isViewEnabled == false -> context.getColorStateList(R.color.disabledGrey)
        string.isNotNullOrEmpty() -> context.getColorStateList(R.color.darkBlue)
        else -> context.getColorStateList(R.color.text_input_active_text_state_selector)
    }
    if (this.editText?.text.toString() == context.getString(R.string.first_and_last_name_ghost_text)) {
        this.editText?.apply {
            setTextColor(context.getColorStateList(R.color.text_input_ghost_text_selector))
            setSelection(0)
        }
    } else editText?.setTextColor(context.getColorStateList(R.color.text_input_active_text_state_selector))
}

@BindingAdapter("app:setFirstAndLastNameGhostText")
fun TextInputEditText.addFirstAndLastNameGhostText(ghostText: String) {
    addTextChangedListener(GhostTextWatcher(ghostText, this))
    onFocusChangeListener = GhostTextFocusChangeListener(ghostText, this)
}

@BindingAdapter("app:setDobGhostText")
fun TextInputEditText.addDOBWatcher(ghostText: String?) {
    addTextChangedListener(DateTextWatcher(this))
    onFocusChangeListener = GhostTextFocusChangeListener(ghostText, this)
}

@BindingAdapter("isDOBFormatCorrect", "app:setDobStyle", "app:isDobEnabled")
fun TextInputLayout.isDOBFormatCorrect(isDobError: Boolean?, string: String?, isViewEnabled: Boolean?) {
    when {
        this.editText?.text.toString() == context.getString(R.string.date_of_birth_ghost_text) -> {
            this.editText?.apply {
                setTextColor(context.getColorStateList(R.color.text_input_ghost_text_selector))
                setSelection(0)
            }
        }
        isDobError != false && string.isNotNullOrEmpty() -> {
            defaultHintTextColor = context.getColorStateList(R.color.all_state_error_selector)
            setBoxStrokeColorStateList(context.getColorStateList(R.color.all_state_error_selector))
        }
        else -> {
            setBoxStrokeColorStateList(context.getColorStateList(R.color.text_input_active_state_selector))
            setStyleByState(string, isViewEnabled)
        }
    }
}

@BindingAdapter("app:verificationInputStyle")
fun TextInputLayout.setVerificationInputStyle(string: String?) {
    defaultHintTextColor = context.getColorStateList(R.color.darkBlue)
    editText?.setTextColor(context.getColorStateList(R.color.age_verification_text_input_state_selector))
}
