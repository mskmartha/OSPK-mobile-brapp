package com.albertsons.acupick.ui.arrivals.complete

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.R
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isBeginVerificationState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isVerifyingCodeState
import com.albertsons.acupick.data.logic.HandOffVerificationState
import com.google.android.material.textfield.TextInputEditText

@BindingAdapter(value = ["app:isAuthDUGEnabled", "app:isUnavailableCtaEnabled", "app:availabilityCtaState"], requireAll = true)
fun TextView.handleCodeUnavailableCtaVisibility(
    isAuthDUGEnabled: Boolean,
    isUnavailableCtaEnabled: Boolean,
    handOffVerificationState: HandOffVerificationState
) {
    isVisible = (isAuthDUGEnabled && isUnavailableCtaEnabled && !handOffVerificationState.isVerifyingCodeState() && !handOffVerificationState.isBeginVerificationState())
}

@BindingAdapter(value = ["app:isAuthDUGEnabled", "app:codeVerifiedOrReportLogged"], requireAll = true)
fun View.handleAuthDugInputVisibility(
    isAuthDUGEnabled: Boolean,
    codeVerifiedOrReportLogged: Boolean
) {
    isVisible = isAuthDUGEnabled && !codeVerifiedOrReportLogged
}

@BindingAdapter(value = ["app:verificationMessage"])
fun TextView.setVerificationMessage(
    isDugOrder: Boolean
) {
    text = when (isDugOrder) {
        true -> context?.getString(R.string.pickup_persons_age_verified)
        false -> context?.getString(R.string.drivers_age_verified)
    }
}

// Function to display code into the designated area for the code instead of the edit text
fun TextInputEditText.setCodeEntryDisplay(linearLayout: LinearLayout) {
    addTextChangedListener(
        onTextChanged = { text, _, beforeCount, count ->
            text?.forEachIndexed { index, char ->
                if (text.length <= linearLayout.childCount) {
                    linearLayout.getChildTextView(index)?.text = char.toString()
                }
            }
            when {
                text.isNullOrEmpty() -> linearLayout.getChildTextViews().forEach { it.text = "" }
                count < beforeCount -> linearLayout.getChildTextView(text.length)?.text = ""
            }
        }
    )
}

fun LinearLayout.getChildTextView(index: Int) = (getChildAt(index) as? TextView)
fun LinearLayout.getChildTextViews() = children.filterIsInstance(TextView::class.java)
