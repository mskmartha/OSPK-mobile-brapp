package com.albertsons.acupick.ui.dialog.first_launch

import android.app.Activity
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.albertsons.acupick.R

class CustomShowcaseView private constructor(
    private val activity: Activity,
    private val targetView: View,
    @LayoutRes private val tooltipLayoutRes: Int,
    private val tooltipGravity: Gravity,
    private val onDismiss: () -> Unit
) {
    enum class Gravity { TOP, BOTTOM, CENTER }

    private lateinit var overlayView: FrameLayout

    fun show() {
        val root = activity.window.decorView as ViewGroup
        overlayView = LayoutInflater.from(activity)
            .inflate(R.layout.empty_layout, root, false) as FrameLayout

        val tooltipView = LayoutInflater.from(activity).inflate(tooltipLayoutRes, overlayView, false)

        // Initial alpha for fade-in
        tooltipView.alpha = 0f

        // Add tooltip to overlay
        overlayView.addView(tooltipView)
        root.addView(overlayView)

        // Fade-in animation AFTER layout and position
        overlayView.post {
            positionTooltip(tooltipView)

            // 🔥 FADE IN
            tooltipView.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }

        // 🔥 FADE OUT on button click
        tooltipView.findViewById<View>(R.id.btnGotIt)?.setOnClickListener {
            tooltipView.animate()
                .alpha(0f)
                .setDuration(250)
                .withEndAction {
                    dismiss()
                }
                .start()
        }
    }


    private fun positionTooltip(tooltipView: View) {
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val targetX = location[0]
        val targetY = location[1]
        val targetW = targetView.width
        val targetH = targetView.height

        val screenW = Resources.getSystem().displayMetrics.widthPixels
        val tooltipW = tooltipView.measuredWidth
        val tooltipH = tooltipView.measuredHeight

        val x = targetX + targetW / 2 - tooltipW / 2
        val y = when (tooltipGravity) {
            Gravity.TOP -> targetY - tooltipH - 16
            Gravity.BOTTOM -> targetY + targetH + 16
            Gravity.CENTER -> targetY + targetH / 2 - tooltipH / 2
        }

        tooltipView.translationX = x.toFloat().coerceAtLeast(16f)
        tooltipView.translationY = y.toFloat().coerceAtLeast(16f)
    }

    fun dismiss() {
        (activity.window.decorView as ViewGroup).removeView(overlayView)
        onDismiss()
    }

    class Builder(private val activity: Activity) {
        private lateinit var targetView: View
        private var tooltipLayoutRes: Int = -1
        private var tooltipGravity = Gravity.BOTTOM
        private var onDismiss: () -> Unit = {}

        fun setTargetView(view: View) = apply { this.targetView = view }
        fun setCustomTooltip(@LayoutRes layoutRes: Int) = apply { this.tooltipLayoutRes = layoutRes }
        fun setTooltipGravity(gravity: Gravity) = apply { this.tooltipGravity = gravity }
        fun onDismiss(callback: () -> Unit) = apply { this.onDismiss = callback }

        fun build(): CustomShowcaseView {
            return CustomShowcaseView(activity, targetView, tooltipLayoutRes, tooltipGravity, onDismiss)
        }
    }
}

sealed class ShowCaseEvents{
  object ShowTimerShowCase :  ShowCaseEvents()
  object ShowBottomNavShowCase :  ShowCaseEvents()
}

fun AppCompatActivity.showTimerShowCase(targetView:View,callback: () -> Unit){
    CustomShowcaseView.Builder(this@showTimerShowCase)
        .setTargetView(targetView)
        .setCustomTooltip(R.layout.layout_showcase_overlay)
        .setTooltipGravity(CustomShowcaseView.Gravity.BOTTOM)
        .onDismiss {
            callback.invoke()
        }
        .build()
        .show()
}

fun AppCompatActivity.showBottomNavigationShowCase(targetView:View,callback: () -> Unit){
    CustomShowcaseView.Builder(this@showBottomNavigationShowCase)
        .setTargetView(targetView)
        .setCustomTooltip(R.layout.bottom_nav_tool_top)
        .setTooltipGravity(CustomShowcaseView.Gravity.TOP)
        .onDismiss {
            callback.invoke()
        }
        .build()
        .show()
}