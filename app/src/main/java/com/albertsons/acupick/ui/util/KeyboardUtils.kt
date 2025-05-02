package com.albertsons.acupick.ui.util

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.IBinder
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import timber.log.Timber

// Inspiration from https://stackoverflow.com/questions/8991522/how-can-i-set-the-focus-and-display-the-keyboard-on-my-edittext-programmatical
/**
 * Forces the soft input keyboard to be shown on the receiver [View]. Expected usage is for [EditText].
 */
fun View?.forceShowKeyboard() {
    if (this != null) {
        val imm = context.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}

/**
 * Hides soft input keyboard from a view.
 */
fun View?.hideKeyboard() {
    if (this != null) {
        val imm: InputMethodManager? = context?.applicationContext?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(windowToken, 0)
    }
}

// Inspiration from https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
/**
 * Hides soft input keyboard from an activity. Use [Fragment.hideKeyboard] if inside a fragment.
 */
fun Activity?.hideKeyboard(clearFocus: Boolean = true) {
    if (this != null) {
        if (clearFocus) {
            clearFocusedView()
        }
        val imm = application.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val windowToken = window.decorView.rootView.windowToken
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
}

/**
 * Hides soft input keyboard from a fragment. Use [Activity.hideKeyboard] if inside an activity.
 */
fun Fragment?.hideKeyboard(clearFocus: Boolean = true) {
    if (this != null) {
        if (clearFocus) {
            activity?.clearFocusedView()
        }
        val windowToken: IBinder? = view?.rootView?.windowToken
        windowToken?.let {
            val imm: InputMethodManager? = context?.applicationContext?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(windowToken, 0)
        }
    }
}

/**
 * Clears the focused view on the activity
 */
private fun Activity?.clearFocusedView() {
    this?.currentFocus
        ?.also { Timber.v("[clearFocusedView] clearing focus on view=$it") }
        ?.clearFocus()
}

/**
 * Returns whether the software keyboard is visible
 * TODO: Replace this with something better.  Android 11 should handle this natively.
 */
fun Fragment?.isKeyboardVisible(): Boolean {
    val rootView = this?.view?.rootView ?: return false
    val softKeyboardHeight = 100
    val r = Rect()
    rootView.getWindowVisibleDisplayFrame(r)
    val dm = rootView.resources.displayMetrics
    val heightDiff = rootView.bottom - r.bottom
    return heightDiff > softKeyboardHeight * dm.density
}

/**
 * Returns whether the software keyboard is visible
 * TODO: Replace this with something better.  Android 11 should handle this natively.
 */
fun isKeyboardVisible(view: View): Boolean {
    val softKeyboardHeight = 100
    val r = Rect()
    view.getWindowVisibleDisplayFrame(r)
    val dm = view.resources.displayMetrics
    val heightDiff = view.bottom - r.bottom
    return heightDiff > softKeyboardHeight * dm.density
}

fun View.addKeyboardListener(keyboardCallback: (visible: Boolean) -> Unit) {
    doOnLayout {
        // get init state of keyboard
        var keyboardVisible = isKeyboardVisible(it)

        // callback as soon as the layout is set with whether the keyboard is open or not
        keyboardCallback(keyboardVisible)

        // whenever the layout resizes/changes, callback with the state of the keyboard.
        viewTreeObserver.addOnGlobalLayoutListener {
            val keyboardUpdateCheck = isKeyboardVisible(it)
            // since the observer is hit quite often, only callback when there is a change.
            if (keyboardUpdateCheck != keyboardVisible) {
                keyboardCallback(keyboardUpdateCheck)
                keyboardVisible = keyboardUpdateCheck
            }
        }
    }
}

fun View.removeKeyboardListener(keyboardCallback: (visible: Boolean) -> Unit) {
    doOnLayout {
        viewTreeObserver.removeOnGlobalLayoutListener {
            if (isKeyboardVisible(it)) {
                keyboardCallback(false)
            }
        }
    }
}
