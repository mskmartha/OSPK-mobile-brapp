package com.albertsons.acupick.ui.dialog

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.OrderedByWeightDialogBinding
import com.albertsons.acupick.ui.util.TypefaceStyle
import com.albertsons.acupick.ui.util.getFontSpanWithFallback
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.Serializable
import java.text.DecimalFormat

class OrderedByWeightDialogFragment : BaseCustomDialogFragment() {
    private val fragmentVm: CustomDialogViewModel by viewModel()

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding =
        DataBindingUtil.inflate<OrderedByWeightDialogBinding>(inflater, R.layout.ordered_by_weight_dialog, container, false).apply {
            viewModel = fragmentVm
            viewData = argData.toViewData(requireContext())
            item = argData.customData as OrderedByWeightDialogData
        }
}

data class OrderedByWeightDialogData(
    val itemDescription: String,
    val plu: String,
    val imageUrl: String,
    val requestedWeight: Double?,
    val scannedWeight: Double?,
    val uom: String?,
) : Serializable

@BindingAdapter(value = ["app:orderedWeight", "app:uom"], requireAll = true)
fun TextView.formatOrderedWeightString(orderedWeight: Double?, uom: String?) {
    val extraBoldItalicFontSpan = context.getFontSpanWithFallback(R.font.nunito_sans_extrabolditalic, TypefaceStyle.BOLD_ITALIC)

    val weight = DecimalFormat("#.##").format(orderedWeight ?: 0f)
    val uomString = uom?.lowercase() ?: context.getString(R.string.uom_default)
    val weightString = "$weight$uomString"
    val wholeString = context.getString(R.string.ordered_by_weight_customer_ordered, weightString)

    val spannableString = SpannableString(wholeString)
    val startIndexOfWeight = wholeString.indexOf(weightString)
    val endIndexOfWeight = startIndexOfWeight + weightString.length

    spannableString.setSpan(extraBoldItalicFontSpan, startIndexOfWeight, endIndexOfWeight, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    text = spannableString
}

@BindingAdapter("app:scannedWeight", "app:uom")
fun TextView.formatScannedWeightString(scannedWeight: Double?, uom: String?) {
    val boldItalicFontSpan = context.getFontSpanWithFallback(R.font.nunito_sans_bold, TypefaceStyle.BOLD)
    val colorSpan = ForegroundColorSpan(context.getColor(R.color.darkestOrange))

    val weight = DecimalFormat("#.##").format(scannedWeight ?: 0f)
    val uomString = uom?.lowercase() ?: context.getString(R.string.uom_default)
    val weightString = "$weight$uomString"
    val wholeString = context.getString(R.string.ordered_by_weight_you_have_scanned, weightString)

    val spannableString = SpannableString(wholeString)
    val startIndexOfWeight = wholeString.indexOf(weightString)
    val endIndexOfWeight = startIndexOfWeight + weightString.length

    spannableString.setSpan(boldItalicFontSpan, startIndexOfWeight, endIndexOfWeight, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    spannableString.setSpan(colorSpan, startIndexOfWeight, endIndexOfWeight, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    text = spannableString
}
