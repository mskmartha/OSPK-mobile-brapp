package com.albertsons.acupick.ui.arrivals.complete

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.lang.ref.WeakReference

class GhostTextWatcher(
    private val ghostText: String,
    editTextIn: EditText?,
) : TextWatcher {
    val editText = WeakReference<EditText>(editTextIn)

    private var isUserDeleting = false
    private var edited = false
    private var formattedString = ""
    override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
        if (edited) return
        isUserDeleting = count - after == 1
        if (charSequence.toString() == ghostText) editText.clear()
    }

    override fun onTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
        when {
            edited || charSequence.contentEquals(formattedString) -> {
                return
            }
            isUserDeleting && charSequence != "" -> {
                formattedString = charSequence.toString()
            }
            !charSequence.isNullOrEmpty() -> {
                formattedString = charSequence.toString().replace(ghostText, "")
            }
        }
    }

    override fun afterTextChanged(editable: Editable?) {
        if (edited) return

        // If the picker has deleted all the entered text, display the ghost text
        if (isUserDeleting && formattedString.isEmpty()) {
            formattedString = ghostText
        }

        if (editable.toString() == ghostText) return
        edited = true
        editable?.clear()
        editable?.insert(0, formattedString)
        edited = false
    }
}
