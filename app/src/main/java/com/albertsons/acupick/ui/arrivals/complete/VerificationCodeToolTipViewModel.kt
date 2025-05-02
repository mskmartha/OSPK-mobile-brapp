package com.albertsons.acupick.ui.arrivals.complete

import android.app.Application
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.ui.BaseViewModel
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import kotlinx.coroutines.launch
import android.text.Spannable

import android.text.style.BackgroundColorSpan

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.core.text.toSpannable

class VerificationCodeToolTipViewModel(app: Application) : BaseViewModel(app) {
    val firstHighlightedText = app.resources.getString(R.string.email_emaple_body)
    val secondHighlightedText = app.resources.getString(R.string.text_example_snippet)
    init {
        viewModelScope.launch {
            changeToolbarTitleEvent.postValue(app.getString(R.string.verification_code))
        }
    }
}

@BindingAdapter("textToHighlight")
fun TextView.higlightCode(text: String) {
    text.toSpannable()
    val textToHighlight = "0337"
    val urlToHighlight = "http://safewayqa.onelink.me/ZdH8/1jfzt7wd"

    var ufe: Int = text.indexOf(urlToHighlight, 0)
    val wordToSpan: Spannable = SpannableString(text)
    var ufs = 0
    while (ufs < text.length && ufe != -1) {
        ufe = text.indexOf(urlToHighlight, ufs)
        if (ufe == -1) break else {
            // set color here
            wordToSpan.setSpan(ForegroundColorSpan(context.getColor(R.color.linkBlue)), ufe, ufe + urlToHighlight.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            wordToSpan.setSpan(UnderlineSpan(), ufe, ufe + urlToHighlight.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        ufs = ufe + 1
    }

    var ofe: Int = text.indexOf(textToHighlight, 0)
    var ofs = 0
    while (ofs < text.length && ofe != -1) {
        ofe = text.indexOf(textToHighlight, ofs)
        if (ofe == -1) break else {
            // set color here
            wordToSpan.setSpan(BackgroundColorSpan(context.getColor(R.color.highlight)), ofe, ofe + textToHighlight.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            wordToSpan.setSpan(StyleSpan(R.style.NunitoSansExtraBold14), ofe, ofe + textToHighlight.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        ofs = ofe + 1
    }
    this.setText(wordToSpan, TextView.BufferType.SPANNABLE)
}
