package com.albertsons.acupick.wifi.utils

import android.annotation.SuppressLint
import java.util.Locale
import java.util.SortedMap

private object SyncAvoid {
    @SuppressLint("ConstantLocale")
    val defaultLocale: Locale = Locale.getDefault()
    val countryCodes: Set<String> = Locale.getISOCountries().toSet()
    val availableLocales: List<Locale> = Locale.getAvailableLocales().filter { countryCodes.contains(it.country) }

    @SuppressLint("ConstantLocale")
    val countriesLocales: SortedMap<String, Locale> =
        availableLocales.map { it.country.toCapitalize(Locale.getDefault()) to it }.toMap()
            .toSortedMap()
    val supportedLocales: List<Locale> = setOf(
        Locale.ENGLISH,
        defaultLocale
    ).toList()
}

fun findByCountryCode(countryCode: String): Locale =
    SyncAvoid.availableLocales
        .find { countryCode.toCapitalize(Locale.getDefault()) == it.country }
        ?: SyncAvoid.defaultLocale

fun allCountries(): List<Locale> = SyncAvoid.countriesLocales.values.toList()

fun supportedLanguages(): List<Locale> = SyncAvoid.supportedLocales
