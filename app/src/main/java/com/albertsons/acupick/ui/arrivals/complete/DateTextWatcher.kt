package com.albertsons.acupick.ui.arrivals.complete

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.albertsons.acupick.ui.arrivals.complete.DateTextWatcher.Companion.DOB_GHOST_TEXT
import java.lang.StringBuilder
import java.lang.ref.WeakReference

class DateTextWatcher(
    editTextIn: EditText?,
    private val onChange: (string: String) -> Unit = {}
) : TextWatcher {
    val editText = WeakReference<EditText>(editTextIn)

    private var isUserDeleting = false
    private var editted = false
    private var formattedString = ""

    override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
        if (editted) return
        isUserDeleting = count - after == 1
        if (charSequence.toString() == DOB_GHOST_TEXT) editText.clear()
    }

    override fun onTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
        when {
            editted || charSequence.contentEquals(formattedString) -> return
            (!charSequence.isNullOrEmpty() && !charSequence.all { it.isDigit() || it == '-' }) -> {
                formattedString = charSequence.filter { it.isDigit() }.toString()
            }
            ((charSequence?.length ?: 0) > 10) -> {
                charSequence?.let {
                    formattedString = it.dropLast(1).toString()
                }
            }
            !editted -> {
                charSequence?.let {
                    formattedString = addDashes(it)
                }
            }
        }
        onChange(formattedString)
    }

    override fun afterTextChanged(editable: Editable?) {
        if (editted) return

        // If the picker has deleted all the entered text, display the ghost text
        if (isUserDeleting && formattedString.isEmpty()) {
            formattedString = DOB_GHOST_TEXT ?: ""
        }

        if (editable.toString() == DOB_GHOST_TEXT) return
        editted = true
        editable?.clear()
        editable?.insert(0, formattedString)
        editted = false
    }

    private fun addDashes(charSequence: CharSequence): String {
        val newSequence = StringBuilder(charSequence.filterNot { it == '-' })
        if (newSequence.length > 2) { newSequence.insert(2, "-") }
        if (newSequence.length > 5) { newSequence.insert(5, '-') }
        return newSequence.toString()
    }

    companion object {
        const val DOB_GHOST_TEXT = "MM-DD-YYYY"
    }
}

/* Is this entry either valid or it could become valid by typing numbers */
fun String?.isEntryPartiallyValid(): Boolean {
    try {
        if (this == DOB_GHOST_TEXT) return true
        if (isNullOrEmpty()) return true
        if (this[0].digitToInt() > 1) return false
        if (length > 1 && substring(0..1).toInt() > 12) return false
        if (length >= 4 && this[3].digitToInt() > 3) return false
        if (length >= 5 && substring(3..4).toInt() > 31) return false
        if (length in 7..9) return false
        return true
    } catch (e: Exception) {
        return false
    }
}
