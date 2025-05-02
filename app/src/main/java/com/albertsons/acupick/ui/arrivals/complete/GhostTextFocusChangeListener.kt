package com.albertsons.acupick.ui.arrivals.complete

import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.EditText
import java.lang.ref.WeakReference

class GhostTextFocusChangeListener(
    private val ghostText: String?,
    editTextIn: EditText?,
) : OnFocusChangeListener {
    val editText = WeakReference<EditText>(editTextIn)

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (hasFocus) {
            if (editText.get()?.text.isNullOrEmpty()) {
                editText.get()?.apply {
                    setText(ghostText)
                }
            }
        } else {
            if (editText.get()?.text.toString() == ghostText) {
                editText.get()?.setText("")
            }
        }
    }
}
