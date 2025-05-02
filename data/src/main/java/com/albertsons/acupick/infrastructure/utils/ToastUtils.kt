package com.albertsons.acupick.infrastructure.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

/** Syntax sugar extension function to simplify showing a Toast */
fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/** Syntax sugar extension function to simplify showing a Toast */
fun Context.toast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, this.resources.getText(resId), duration).show()
}
