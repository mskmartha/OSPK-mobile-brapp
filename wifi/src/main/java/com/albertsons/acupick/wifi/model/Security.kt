package com.albertsons.acupick.wifi.model

import java.util.Locale

enum class Security(val additional: String = "") {
    NONE,
    WPS,
    WEP,
    WPA,
    WPA2,
    WPA3;

    companion object {
        private val regex = Regex("[^A-Z0-9]")

        fun findAll(capabilities: String): Set<Security> =
            parse(capabilities).mapNotNull(transform()).toSortedSet().ifEmpty { setOf(NONE) }

        fun findOne(capabilities: String): Security = findAll(capabilities).first()

        private fun transform(): (String) -> Security? = {
            try {
                enumValueOf<Security>(it)
            } catch (e: IllegalArgumentException) {
                enumValues<Security>().find { security -> security.additional == it }
            }
        }

        private fun parse(capabilities: String): List<String> =
            regex.replace(capabilities.uppercase(Locale.getDefault()), "-")
                .split("-")
                .filter { it.isNotBlank() }
    }
}
