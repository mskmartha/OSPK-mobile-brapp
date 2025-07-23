package com.albertsons.acupick.ui.dialog.firstlaunch

import android.os.Build
import android.text.Html
import android.text.Spanned
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.viewpager2.widget.ViewPager2

@BindingAdapter("currentPage")
fun ViewPager2.bindCurrentPage(currentPage: Int?) {
    if (currentPage != null && currentItem != currentPage) {
        setCurrentItem(currentPage, true)
    }
}

@InverseBindingAdapter(attribute = "currentPage", event = "currentPageAttrChanged")
fun ViewPager2.getCurrentPage(): Int = currentItem

@BindingAdapter("currentPageAttrChanged")
fun ViewPager2.setCurrentPageListener(attrChange: InverseBindingListener) {
    registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            attrChange.onChange()
        }
    })
}

fun String.fromHtml(): Spanned {
    return Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
}
