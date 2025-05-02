package com.albertsons.acupick.ui.util

import android.view.animation.Interpolator
import kotlin.math.cos
import kotlin.math.pow

class BounceInterpolator(private val amplitude: Double, private val frequency: Double) : Interpolator {
    override fun getInterpolation(time: Float): Float {
        return (
            -1 * Math.E.pow(-time / amplitude) *
                cos(frequency * time) + 1
            ).toFloat()
    }
}
