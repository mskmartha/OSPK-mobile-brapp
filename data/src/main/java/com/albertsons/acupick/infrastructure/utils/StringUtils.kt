package com.albertsons.acupick.infrastructure.utils

import com.albertsons.acupick.domain.AcuPickLoggerInterface
import java.util.Locale

/**
 * Extension function wrapper that provides the not version of [isNullOrEmpty].
 *
 * Created to add the opposite (borrowed the idea from the existence of [isEmpty] and [isNotEmpty] stblib extensions)
 */
fun String?.isNotNullOrEmpty() = !this.isNullOrEmpty()

fun String?.isNotNullOrBlank() = !this.isNullOrBlank()

fun String.specialTrim(): String =
    this.trim { it <= ' ' }.replace(" +".toRegex(), " ")

fun String.toCapitalize(locale: Locale): String =
    this.replaceFirstChar { word -> word.uppercase(locale) }

fun String?.ellipse(length: Int): String = if ((this?.length ?: 0) > length) "${this?.substring(0, length)?.trim()}..." else this ?: ""

fun String?.logError(value: String, acuPickLogger: AcuPickLoggerInterface) {
    if (this.isNullOrEmpty() || this.equals("null", true) || this.equals("0", true)) acuPickLogger.e(value)
}

fun String?.isValidActivityId(): Boolean {
    return !(this.isNullOrEmpty() || this.equals("null", true) || this.equals("0", true))
}
