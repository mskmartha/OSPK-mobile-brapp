package com.albertsons.acupick.ui.dialog

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import timber.log.Timber

/**
 * Show the dialog in a fragment.
 *
 * @param tag identifier which can be used to tell which dialog is clicked (via [CloseActionListenerProvider.provide])
 */
fun <T> DialogFragment.showWithFragment(fragment: T, tag: String) where T : Fragment, T : CloseActionListenerProvider {
    if (fragment.isDetached) {
        Timber.w("[showWithFragment] cannot add dialog fragment when fragment is detached: $fragment")
        return
    }
    val childFragmentManager = fragment.childFragmentManager
    if (childFragmentManager.isStateSaved) {
        Timber.d("[showWithFragment] state saved, skip showing dialog fragment (should be reshown automatically)")
        return
    }

    val transaction = childFragmentManager.beginTransaction()
    val customDialogFragment = childFragmentManager.findFragmentByTag(tag)
    if (customDialogFragment != null) {
        transaction.remove(customDialogFragment)
    }

    show(transaction, tag)
}

/**
 * Show the dialog in an activity.
 *
 * @param tag identifier which can be used to tell which dialog is clicked (via [CloseActionListenerProvider.provide])
 */
fun <T> DialogFragment.showWithActivity(activity: T, tag: String) where T : FragmentActivity, T : CloseActionListenerProvider {
    val fragmentManager = activity.supportFragmentManager
    val transaction = fragmentManager.beginTransaction()
    val customDialogFragment = fragmentManager.findFragmentByTag(tag)
    if (customDialogFragment != null) {
        transaction.remove(customDialogFragment)
    }
    show(transaction, tag)
}

/** Retrieves the [CloseActionListener] specified in the host (parent) fragment/activity */
fun DialogFragment.findDialogListener(): CloseActionListener? {
    return (parentFragment as? CloseActionListenerProvider)?.provide(tag) ?: (activity as? CloseActionListenerProvider)?.provide(tag)
}
