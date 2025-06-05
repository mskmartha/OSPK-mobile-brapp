package com.albertsons.acupick.ui.handoff.stepsprogress

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlin.math.roundToInt

/**
 * A custom Android View that displays a progress bar with distinct steps,
 * a movable thumb, and customizable labels for each step.
 *
 * It supports:
 * - Setting total number of steps.
 * - Animating progress changes.
 * - Customizing thumb drawable.
 * - Customizing bar colors and height.
 * - Enabling rounded corners for the bar.
 * - Customizing step labels (text size, color, font, suffix).
 * - Notifying of step changes via a listener.
 */
class StepProgressBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    //region Private View Components
    private val fillView = View(context) // Represents the filled portion of the progress bar
    private val trackView = View(context) // Represents the unfilled (track) portion of the progress bar
    private val thumbView = ImageView(context) // The movable thumb indicator
    private val labelLayout = LinearLayout(context) // Layout for displaying step labels
    //endregion

    //region Public Properties
    /**
     * The total number of steps in the progress bar.
     * When set, it triggers an update of the progress bar (without animation).
     */
    var totalSteps: Int = 5
        set(value) {
            field = value
            updateProgress(animated = false) // Update layout when totalSteps changes
        }

    /**
     * The current step progress. This value can be a float to represent
     * progress between steps.
     * It is set internally and can be observed via [stepChangeListener].
     */
    var currentStep: Float = 0f
        private set // Only settable internally

    /**
     * A listener to be invoked when the [currentStep] changes.
     */
    private var stepChangeListener: ((Float) -> Unit)? = null

    /**
     * Suffix to append to each step label (e.g., "m" for meters).
     * Updating this property redraws the labels.
     */
    var labelSuffix: String = "m"
        set(value) {
            field = value
            updateLabels() // Update labels when suffix changes
        }

    /**
     * Text size of the step labels in SP (scale-independent pixels).
     * Updating this property redraws the labels.
     */
    var labelTextSizeSp: Float = 12f
        set(value) {
            field = value
            updateLabels() // Update labels when text size changes
        }

    /**
     * Text color of the step labels.
     * Updating this property redraws the labels.
     */
    var labelTextColor: Int = Color.BLACK
        set(value) {
            field = value
            updateLabels() // Update labels when text color changes
        }

    /**
     * Typeface for the step labels. If null, [Typeface.DEFAULT] is used.
     * Updating this property redraws the labels.
     */
    var labelTypeface: Typeface? = null
        set(value) {
            field = value
            updateLabels() // Update labels when typeface changes
        }
    //endregion

    init {
        // Initial setup of the view hierarchy when the custom view is created.
        setupLayout()
    }

    /**
     * Sets up the basic layout structure of the progress bar, including the track,
     * fill, thumb, and label container.
     */
    private fun setupLayout() {
        // Clear any existing views to prevent duplicates on re-layout.
        removeAllViews()

        // Main container for the progress bar and labels (vertical orientation).
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        // Container for the progress bar itself (track, fill, thumb).
        val barContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 32.dp) // Fixed height for the bar area
        }

        // Setup the track view (unfilled portion of the bar).
        trackView.layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, 8.dp).apply {
            gravity = Gravity.CENTER_VERTICAL // Center vertically within its parent
        }
        trackView.setBackgroundColor(Color.LTGRAY) // Default track color
        barContainer.addView(trackView)

        // Setup the fill view (filled portion of the bar).
        fillView.layoutParams = FrameLayout.LayoutParams(0, 8.dp).apply { // Initial width is 0
            gravity = Gravity.CENTER_VERTICAL
        }
        fillView.setBackgroundColor(Color.RED) // Default fill color
        barContainer.addView(fillView)

        // Setup the thumb view.
        thumbView.layoutParams = FrameLayout.LayoutParams(24.dp, 24.dp).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL // Initially at the start, centered vertically
        }
        barContainer.addView(thumbView)

        // Add the bar container to the main container.
        container.addView(barContainer)

        // Setup the horizontal LinearLayout for labels.
        labelLayout.orientation = LinearLayout.HORIZONTAL
        labelLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 4.dp // Small margin above labels
        }
        container.addView(labelLayout)

        // Add the main container to this custom view.
        addView(container)

        // Post a runnable to execute after the layout pass, ensuring width is available.
        post {
            updateLabels() // Initialize labels after layout has determined width
            updateProgress(animated = false) // Initialize progress to currentStep (0)
        }
    }

    /**
     * Updates and redraws all step labels based on [totalSteps] and label properties.
     * It calculates the position of each label to align with its corresponding step on the bar.
     */
    private fun updateLabels() {
        labelLayout.removeAllViews() // Clear existing labels
        val barWidth = width.toFloat() // Get the actual width of the view
        if (barWidth == 0f) return // If width is not yet determined, skip

        // Iterate through each step to create and position labels.
        for (i in 0..totalSteps) {
            val label = TextView(context).apply {
                text = "$i$labelSuffix" // Set label text (e.g., "0m", "1m", ...)
                gravity = Gravity.CENTER // Center text within the TextView
                textSize = labelTextSizeSp // Apply configured text size
                setTextColor(labelTextColor) // Apply configured text color
                typeface = labelTypeface ?: Typeface.DEFAULT // Apply configured typeface or default
                layoutParams = if (i == 0 || i == totalSteps) {
                    // For the first and last labels, use WRAP_CONTENT and align to START/END.
                    LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                        weight = 0f // Do not distribute remaining space
                        gravity = if (i == 0) Gravity.START else Gravity.END
                    }
                } else {
                    // For intermediate labels, use weight 1f to distribute evenly.
                    LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                }
            }
            labelLayout.addView(label)

            // Post a runnable to position labels after their width is measured.
            label.post {
                val stepRatio = i.toFloat() / totalSteps // Calculate the ratio for this step
                val labelWidth = label.width.toFloat() // Get the measured width of the label
                // Calculate desired X position (center of label at stepRatio * barWidth)
                val posX = (barWidth * stepRatio) - labelWidth / 2f
                // Clamp X position to prevent labels from going off-screen.
                val clampedX = posX.coerceIn(0f, barWidth - labelWidth)
                label.x = clampedX // Set the actual X position
            }
        }
    }

    /**
     * Updates the progress bar's fill and thumb position.
     *
     * @param progress The target step value (float).
     * @param animated If true, the progress change will be animated.
     */
    private fun updateProgress(progress: Float = currentStep, animated: Boolean = true) {
        // Clamp the progress value to be within valid range [0, totalSteps].
        val clamped = progress.coerceIn(0f, totalSteps.toFloat())
        if (animated && width > 0) {
            // If animation is requested and view width is available, animate the progress.
            animateProgress(currentStep, clamped)
        } else {
            // Otherwise, apply the progress instantly.
            applyProgress(clamped)
        }
    }

    /**
     * Applies the given progress value to the fill view and thumb position instantly.
     *
     * @param progress The target step value.
     */
    private fun applyProgress(progress: Float) {
        val ratio = progress / totalSteps // Calculate the ratio of current progress to total steps
        val widthPx = width // Get the actual width of the progress bar area

        // Update the width of the fill view.
        fillView.layoutParams.width = (widthPx * ratio).toInt()
        fillView.requestLayout() // Request a layout pass to apply the new width

        // Calculate and set the translationX for the thumb to position it correctly.
        // It's offset by half its width to center it on the progress point.
        val thumbOffset = (widthPx * ratio) - thumbView.width / 2f
        thumbView.translationX = thumbOffset.coerceIn(0f, (widthPx - thumbView.width).toFloat()) // Clamp thumb position

        currentStep = progress // Update the internal current step
        stepChangeListener?.invoke(currentStep) // Notify the listener of the step change
    }

    /**
     * Animates the progress change from a starting step to an ending step.
     *
     * @param from The starting step value.
     * @param to The ending step value.
     */
    private fun animateProgress(from: Float, to: Float) {
        ValueAnimator.ofFloat(from, to).apply {
            duration = 300 // Animation duration in milliseconds
            addUpdateListener {
                applyProgress(it.animatedValue as Float) // Apply progress for each animation frame
            }
        }.start() // Start the animation
    }

    //region Public API for controlling the progress bar

    /**
     * Jumps the progress bar to a specific step.
     *
     * @param step The target step value (float).
     * @param animated If true, the jump will be animated.
     */
    fun jumpToStep(step: Float, animated: Boolean = true) {
        updateProgress(step, animated)
    }

    /**
     * Sets a listener to be called when the [currentStep] changes.
     *
     * @param listener A lambda function that receives the current step as a Float.
     */
    fun setOnStepChangeListener(listener: (Float) -> Unit) {
        stepChangeListener = listener
    }

    /**
     * Sets the drawable for the thumb indicator.
     *
     * @param drawable The Drawable to use for the thumb.
     */
    fun setThumbDrawable(drawable: Drawable) {
        thumbView.setImageDrawable(drawable)
    }

    /**
     * Sets the colors for the filled and unfilled parts of the progress bar.
     *
     * @param filledColor The color for the filled portion.
     * @param unfilledColor The color for the unfilled (track) portion.
     */
    fun setBarColors(filledColor: Int, unfilledColor: Int) {
        fillView.setBackgroundColor(filledColor)
        trackView.setBackgroundColor(unfilledColor)
        // If rounded corners are enabled, reapply them to update colors
        setRoundedCorners(true, (trackView.background as? GradientDrawable)?.cornerRadius?.roundToInt()?.dp ?: 4)
    }

    /**
     * Sets the height of the progress bar in DP.
     *
     * @param dp The desired height in density-independent pixels.
     */
    fun setBarHeight(dp: Int) {
        val px = dp.dp // Convert DP to pixels
        trackView.layoutParams.height = px
        fillView.layoutParams.height = px
        trackView.requestLayout() // Request layout to apply new height
        fillView.requestLayout() // Request layout to apply new height
    }

    /**
     * Enables or disables rounded corners for the progress bar track and fill.
     *
     * @param enabled True to enable rounded corners, false for square corners.
     * @param radiusDp The radius of the rounded corners in DP, if enabled.
     */
    fun setRoundedCorners(enabled: Boolean, radiusDp: Int = 4) {
        val radiusPx = radiusDp.dp.toFloat() // Convert DP radius to pixels

        // Create a GradientDrawable for the track view to apply rounded corners.
        val trackShape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE // Rectangular shape
            cornerRadius = if (enabled) radiusPx else 0f // Apply radius if enabled, else 0
            // Get current track color, default to LTGRAY if not a ColorDrawable
            setColor((trackView.background as? ColorDrawable)?.color ?: Color.LTGRAY)
        }
        trackView.background = trackShape // Set the new background

        // Create a GradientDrawable for the fill view to apply rounded corners.
        val fillShape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = if (enabled) radiusPx else 0f
            // Get current fill color, default to RED if not a ColorDrawable
            setColor((fillView.background as? ColorDrawable)?.color ?: Color.RED)
        }
        fillView.background = fillShape // Set the new background
    }
    //endregion

    /**
     * Extension property to convert density-independent pixels (DP) to
     * actual pixels based on the device's screen density.
     */
    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}