package com.albertsons.acupick.infrastructure.utils

import java.text.DecimalFormat
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Syntax sugar calling [Double.roundToLong] and using 0L when null */
fun Double?.roundToLongOrZero(): Long = this?.roundToLong() ?: 0L

/** Syntax sugar calling [Double.roundToInt] and using 0 when null */
fun Double?.roundToIntOrZero(): Int = this?.roundToInt() ?: 0

/** Syntax sugar to return 0.0 when receiver is null. Otherwise, original value is used. */
fun Double?.orZero(): Double = this ?: 0.0

fun Double?.toTwoDecimalString(): String = DecimalFormat("#.##").format(orZero())
