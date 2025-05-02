package com.albertsons.acupick.ui.picklistitems

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.models.ConfirmAmountUIData
import com.albertsons.acupick.data.model.ConfirmAmountError
import com.albertsons.acupick.data.picklist.enteredWeightString
import com.albertsons.acupick.data.picklist.isPartialPick
import com.albertsons.acupick.data.picklist.netWeightString
import com.albertsons.acupick.ui.util.CustomTextInput
import com.albertsons.acupick.ui.util.dpToPx
import com.albertsons.acupick.ui.util.setError
import com.albertsons.acupick.ui.util.setErrorMessageWithIcon
import com.google.android.material.textfield.TextInputLayout

private fun String.orDefaultUOM(context: Context): String = ifEmpty { context.getString(R.string.uom_default) }

@BindingAdapter("enteredNetWeightText")
fun TextView.setEnteredNetWeightText(uiData: ConfirmAmountUIData?) {
    uiData?.let {
        isVisible = uiData.requestedAmount.isPartialPick
        text = context.getString(
            R.string.entered_amount_entered_weight,
            uiData.requestedAmount.enteredWeightString,
            uiData.requestedAmount.weightUOM.orDefaultUOM(context)
        )
    } ?: run { isVisible = false }
}

@BindingAdapter("requestedNetWeightText")
fun TextView.setRequestedNetWeightText(uiData: ConfirmAmountUIData?) {
    uiData?.let {
        text = context.getString(
            R.string.confirm_amount_requested_weight,
            uiData.requestedAmount.netWeightString,
            uiData.requestedAmount.weightUOM.orDefaultUOM(context)
        )
    }
}

@BindingAdapter("confirmAmountError")
fun CustomTextInput.setConfirmAmountError(confirmAmountError: ConfirmAmountError?) {
    when (confirmAmountError) {
        ConfirmAmountError.TooLight -> setError(context.getDrawable(R.drawable.ic_red_warning), R.string.confirm_amount_too_light_error_msg)
        ConfirmAmountError.TooHeavy -> setError(context.getDrawable(R.drawable.ic_red_warning), R.string.confirm_amount_too_heavy_error_msg)
        ConfirmAmountError.ExceedsMaxLimit -> setError(context.getDrawable(R.drawable.ic_red_warning), R.string.quantity_entered_exceeds_max_limit)
        ConfirmAmountError.InvalidAmountOrQty -> setError(context.getDrawable(R.drawable.ic_red_warning), R.string.amount_entered_is_invalid)
        else -> setError(null, null)
    }
}

@BindingAdapter("confirmWeightError")
fun TextInputLayout.setConfirmWeightError(confirmAmountError: ConfirmAmountError?) {
    when (confirmAmountError) {
        ConfirmAmountError.TooLight -> setConfirmAmountError(R.string.box_too_light)
        ConfirmAmountError.TooHeavy -> setConfirmAmountError(R.string.box_too_heavy)
        else -> error = null
    }
}

private fun TextInputLayout.setConfirmAmountError(@StringRes errorStringId: Int) {
    val errorMessage = context.getString(errorStringId)
    AppCompatResources.getDrawable(context, R.drawable.ic_alert_error_red)?.let { errorIcon ->
        errorIcon.apply {
            setBounds(0, 0, 13.dpToPx, 15.dpToPx)
            setTint(context.getColor(R.color.snackbarError))
        }
        removePaddingFromErrorMsgLayout()
        setErrorMessageWithIcon(errorMessage, errorIcon)
    }
}

// Hack to remove padding from error message text view
private fun TextInputLayout.removePaddingFromErrorMsgLayout() =
    children
        .find { it is LinearLayout }
        ?.let {
            it.setPadding(0, it.paddingTop, 0, it.paddingBottom)
        }
