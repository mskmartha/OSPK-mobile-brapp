package com.albertsons.acupick.ui.arrivals.destage.updatecustomers

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.albertsons.acupick.R
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.ContentViewCallback

class MarkedArrivedSnackBar(
    parent: ViewGroup,
    content: MarkedArrivedSnackBarView,
) : BaseTransientBottomBar<MarkedArrivedSnackBar>(parent, content, content) {

    init {
        getView().setBackgroundColor(ContextCompat.getColor(view.context, android.R.color.transparent))
        getView().setPadding(0, 0, 0, 0)
    }

    companion object {

        fun make(view: View, string: String, showCta: Boolean, unit: () -> Unit?, onDismissed: () -> Unit?): MarkedArrivedSnackBar {

            // First we find a suitable parent for our custom view
            val parent = view.findSuitableParent() ?: throw IllegalArgumentException(
                "No suitable parent found from the given view. Please provide a valid view."
            )

            // We inflate our custom view
            val customView = LayoutInflater.from(view.context).inflate(
                R.layout.update_customer_snackbar_layout,
                parent,
                false
            ) as MarkedArrivedSnackBarView

            customView.findViewById<AppCompatTextView>(R.id.message).text = string
            val addToHandoffCta = customView.findViewById<AppCompatTextView>(R.id.snackbarCta)
            addToHandoffCta.visibility = if (showCta) View.VISIBLE else View.GONE
            addToHandoffCta.setOnClickListener { unit.invoke() }

            // We create and return our Snackbar
            return MarkedArrivedSnackBar(
                parent,
                customView
            ).addCallback(
                object : BaseTransientBottomBar.BaseCallback<MarkedArrivedSnackBar>() {
                    override fun onShown(transientBottomBar: MarkedArrivedSnackBar?) {
                        super.onShown(transientBottomBar)
                    }

                    override fun onDismissed(transientBottomBar: MarkedArrivedSnackBar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        onDismissed.invoke()
                    }
                }
            )
        }
    }
}

class MarkedArrivedSnackBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), ContentViewCallback {

    private val view = View.inflate(context, R.layout.update_customer_snackbar, this)

    init {
        view
        clipToPadding = false
    }

    override fun animateContentIn(delay: Int, duration: Int) {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0f, 1f)
        val animatorSet = AnimatorSet().apply {
            interpolator = OvershootInterpolator()
            setDuration(500)
            playTogether(scaleX, scaleY)
        }
        animatorSet.start()
    }

    override fun animateContentOut(delay: Int, duration: Int) {
    }
}

internal fun View?.findSuitableParent(): ViewGroup? {
    var view = this
    var fallback: ViewGroup? = null
    do {
        if (view is CoordinatorLayout) {
            // We've found a CoordinatorLayout, use it
            return view
        } else if (view is FrameLayout) {
            if (view.id == android.R.id.content) {
                // If we've hit the decor content view, then we didn't find a CoL in the
                // hierarchy, so use it.
                return view
            } else {
                // It's not the content view but we'll use it as our fallback
                fallback = view
            }
        }

        if (view != null) {
            // Else, we will loop and crawl up the view hierarchy and try to find a parent
            val parent = view.parent
            view = if (parent is View) parent else null
        }
    } while (view != null)

    // If we reach here then we didn't find a CoL or a suitable content view so we'll fallback
    return fallback
}
