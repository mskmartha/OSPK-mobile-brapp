package com.albertsons.acupick.ui.util

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerFragment
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

class AcupickSnackbar private constructor(
    private val fragment: Fragment,
    private val message: String,
    private val type: SnackType,
    private val duration: SnackDuration,
    private val action: SnackAction?,
    private val isDismissable: Boolean,
    private val onDismiss: () -> Unit,
) {

    // snackbar shows on top of anchor view
    private var anchor: View? = null

    private val snackbar = rootView?.let { fragmentView -> Snackbar.make(fragmentView, message, duration.length) }

    init {
        // Keep track of snackbars getting shown/dismissed
        snackbar?.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
            override fun onShown(transientBottomBar: Snackbar?) {
                add(this@AcupickSnackbar)
                super.onShown(transientBottomBar)
            }

            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                if (clear(this@AcupickSnackbar)) {
                    onDismiss.invoke()
                }
                super.onDismissed(transientBottomBar, event)
            }
        })
    }

    fun setAnchorView(anchorView: View) = apply {
        anchor = anchorView
    }

    private fun isBottomSheet() = fragment is BottomSheetDialogFragment

    // For bottomsheet, view is unable to display snackbar
    private val rootView get() = when (fragment) {
        is BottomSheetDialogFragment -> fragment.dialog?.window?.decorView
        is ManualEntryPagerFragment -> fragment.view?.rootView
        else -> fragment.view
    }
    private val systemNavigationbarHeight
        get(): Int {
            val resources = fragment.resources
            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            return if (resourceId > 0) {
                resources.getDimensionPixelSize(resourceId)
            } else {
                0
            }
        }

    // To avoid showing snackbar on top of navigation keys we need padding for bottomsheet
    private val bottomPadding get() = if ((isBottomSheet() && anchor == null) || fragment is ManualEntryPagerFragment) systemNavigationbarHeight else 0

    private fun makeSnack() = snackbar?.apply {
        // To avoid snackbar floats to top of the screen we need to check if the anchor view is shown on UI
        if (anchor != null && anchor?.isShown == true) {
            anchorView = anchor
        }

        // Resets snackbars default background
        view.setBackgroundColor(Color.TRANSPARENT)

        val sbLayout = (view as Snackbar.SnackbarLayout)
        sbLayout.setPadding(0, 0, 0, bottomPadding)

        // Initialiazes Text Span if clickable action is available
        var ss: SpannableString? = null
        action?.let {
            val actionText = action.actionText.getString(fragment.requireContext())
            ss = SpannableString("$message \n$actionText")
            val start = message.length + 1
            val end = start + actionText.length.getOrZero() + 1

            val clickableSpan: ClickableSpan = object : ClickableSpan() {
                override fun onClick(p0: View) {
                    action.onActionClicked.invoke()
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    // changes the default text color of the clickable link
                    ds.color = Color.WHITE
                }
            }

            ss?.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val customView = fragment.layoutInflater.inflate(R.layout.custom_snackbar, null)
        customView.findViewById<TextView>(R.id.snackMessageTv).apply {
            text = ss ?: message
            ss.let { movementMethod = LinkMovementMethod.getInstance() }
        }
        customView.findViewById<ImageView>(R.id.snackBarStartImg).setImageResource(type.resourceId)
        customView.findViewById<ImageView>(R.id.dismissIv).apply {
            visibility = if (isDismissable) View.VISIBLE else View.GONE
            setOnClickListener { dismiss() }
        }
        sbLayout.addView(customView)
    }

    fun show() {
        makeSnack()?.show()
    }

    fun dismiss() {
        snackbar?.dismiss()
    }

    companion object {
        private val snackbars = mutableListOf<AcupickSnackbar>()

        fun make(fragment: Fragment, event: AcupickSnackEvent) = AcupickSnackbar(
            fragment = fragment,
            message = event.message.getString(fragment.requireContext()),
            type = event.type,
            duration = event.duration,
            action = event.action,
            isDismissable = event.isDismissable,
            onDismiss = event.onDismiss
        )

        fun add(snack: AcupickSnackbar) = snackbars.add(snack)

        fun clearAll() = with(snackbars) {
            forEach { it.dismiss() }
            clear()
        }

        fun clear(snack: AcupickSnackbar) = snackbars.remove(snack)
    }
}

data class SnackAction(val actionText: StringIdHelper, val onActionClicked: () -> Unit)

enum class SnackType(val resourceId: Int) {
    SUCCESS(R.drawable.ic_success),
    WARNING(R.drawable.ic_warn),
    ERROR(R.drawable.ic_error),
    INFO(R.drawable.ic_info_filled)
}

enum class SnackDuration(val length: Int) {
    LENGTH_SHORT(Snackbar.LENGTH_SHORT),
    LENGTH_LONG(Snackbar.LENGTH_LONG),
    LENGTH_INDEFINITE(Snackbar.LENGTH_INDEFINITE)
}
