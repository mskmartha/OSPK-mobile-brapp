package com.albertsons.acupick.ui.handoff.stepsprogress

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable


/**
 * Data class to hold configuration parameters for the [StepProgressBarView].
 * This provides a structured way to pass various customization options to the view.
 *
 * @property totalSteps The total number of steps in the progress bar.
 * @property labelSuffix A string suffix to append to each step label (e.g., "m", "km"). Defaults to "m".
 * @property labelTextSizeSp The text size of the step labels in scale-independent pixels (sp). Defaults to 12sp.
 * @property labelTextColor The color of the step labels. Defaults to [Color.BLACK].
 * @property labelTypeface The typeface for the step labels. If null, the default typeface is used. Defaults to null.
 * @property barHeightDp The height of the progress bar in density-independent pixels (dp). Defaults to 8dp.
 * @property setRoundedCorners Boolean indicating whether the progress bar should have rounded corners. Defaults to true.
 * @property roundRadius The radius of the rounded corners in dp, if [setRoundedCorners] is true. Defaults to 4dp.
 * @property thumbDrawable The [Drawable] to be used as the thumb indicator. Must not be null.
 * @property barColor A [Pair] of integers representing the filled color (first) and unfilled color (second) of the bar.
 */
data class ProgressViewConfig(
    val totalSteps: Int,
    val labelSuffix: String = "m",
    val labelTextSizeSp: Float = 12f,
    val labelTextColor: Int = Color.BLACK,
    val labelTypeface: Typeface? = null,
    val barHeightDp: Int = 8,
    val setRoundedCorners: Boolean = true,
    val roundRadius: Int = 4,
    val thumbDrawable: Drawable? = null, // This should ideally be non-nullable to ensure a thumb is always provided.
    val barColor: Pair<Int, Int>
)

/**
 * Extension function for [StepProgressBarView] to set up its configuration
 * using a [ProgressViewConfig] object and an optional step change listener.
 * This provides a clean and concise way to initialize the progress bar.
 *
 * @param config The [ProgressViewConfig] object containing all the desired settings.
 * @param onStepChangeListener A lambda function to be invoked when the current step changes.
 * It receives the new current step as a Float.
 * @throws IllegalStateException if any configuration parameter is invalid (e.g., totalSteps <= 0, barHeightDp <= 0, etc.).
 */
fun StepProgressBarView.setUp(config: ProgressViewConfig, onStepChangeListener: (Float) -> Unit) {
    // --- Input Validation ---
    // Ensure totalSteps is a positive value.
    if (config.totalSteps <= 0) {
        throw IllegalStateException("Steps can't be below or equal to 0")
    }

    // If rounded corners are enabled, ensure the radius is non-negative.
    if (config.setRoundedCorners && config.roundRadius < 0) {
        throw IllegalStateException("Corner Radius can't be below 0")
    }

    // Ensure bar height is a positive value.
    if (config.barHeightDp <= 0) {
        throw IllegalStateException("Bar height can't be zero or negative")
    }

    // Ensure a thumb drawable is provided.
    if (config.thumbDrawable == null) {
        throw IllegalStateException("Bar thumb can't be null. Please provide a drawable for the thumb.")
    }
    // --- End Input Validation ---


    // Apply configuration properties to the StepProgressBarView instance.

    // Set the total number of steps.
    totalSteps = config.totalSteps
    // Set the suffix for step labels.
    labelSuffix = config.labelSuffix
    // Set the text size for step labels.
    labelTextSizeSp = config.labelTextSizeSp

    // Set the height of the progress bar.
    setBarHeight(config.barHeightDp)
    // Set the drawable for the thumb indicator.
    setThumbDrawable(config.thumbDrawable)

    // Set the colors for the filled and unfilled parts of the bar.
    setBarColors(filledColor = config.barColor.first, unfilledColor = config.barColor.second)
    // Apply rounded corners if enabled in the configuration.
    if (config.setRoundedCorners) {
        setRoundedCorners(enabled = true, radiusDp = config.roundRadius)
    }

    // Set the text color for step labels.
    labelTextColor = config.labelTextColor

    // Set the typeface for step labels if provided.
    if (config.labelTypeface != null) {
        labelTypeface = config.labelTypeface
    }

    // Set the step change listener, forwarding the callback from the config.
    setOnStepChangeListener {
        onStepChangeListener.invoke(it)
    }
}