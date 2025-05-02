package com.albertsons.acupick.wifi.vendor

import java.util.Locale

internal const val MAX_SIZE = 6
private const val SEPARATOR = ":"

internal fun String.clean(): String =
    orEmpty().replace(SEPARATOR, "").take(MAX_SIZE).uppercase(Locale.getDefault())

internal fun String.toMacAddress(): String =
    when {
        isEmpty() -> ""
        length < MAX_SIZE -> "*$this*"
        else -> substring(0, 2) + SEPARATOR + substring(2, 4) + SEPARATOR + substring(4, 6)
    }
