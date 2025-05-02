package com.albertsons.acupick.ui.manualentry.handoff

import android.view.MotionEvent
import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputEditText

const val DRAWABLE_START = 0
const val DRAWABLE_TOP = 1
const val DRAWABLE_END = 2
const val DRAWABLE_BOTTOM = 3

@BindingAdapter("app:drawableAction")
fun TextInputEditText.drawableAction(unit: () -> Unit?) {
    setOnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            if (event.rawX >= right - compoundDrawables[DRAWABLE_END].bounds.width()) {
                // your action here
                v.performClick()
                unit.invoke()
            }
        }

        v.onTouchEvent(event)
    }
}
