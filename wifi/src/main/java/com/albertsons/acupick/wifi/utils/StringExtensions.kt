package com.albertsons.acupick.wifi.utils

import java.util.Locale

fun String.toCapitalize(locale: Locale): String =
    this.replaceFirstChar { word -> word.uppercase(locale) }
