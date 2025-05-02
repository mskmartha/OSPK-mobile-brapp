package com.albertsons.acupick.wifi.utils

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Locale

class LocaleUtilsTest {
    @Test
    fun testAllCountries() {
        // execute
        val actual = allCountries()
        // validate
        assertTrue(actual.size >= 2)
        assertTrue(actual[0].country < actual[actual.size - 1].country)
    }

    @Test
    fun testFindByCountryCode() {
        // setup
        val expected = allCountries()[0]
        // execute
        val actual = findByCountryCode(expected.country)
        // validate
        assertEquals(expected, actual)
        assertEquals(expected.country, actual.country)
        assertEquals(expected.displayCountry, actual.displayCountry)
        assertNotEquals(expected.country, expected.displayCountry)
        assertNotEquals(actual.country, actual.displayCountry)
    }

    @Test
    fun testFindByCountryCodeWithUnknownCode() {
        // execute
        val actual = findByCountryCode("WW")
        // validate
        assertEquals(Locale.getDefault(), actual)
    }

    @Test
    fun testSupportedLanguages() {
        // setup
        val expected: Set<Locale> = setOf(
            Locale.ENGLISH,
            Locale.getDefault()
        )
        // execute
        val actual = supportedLanguages()
        // validate
        assertEquals(expected.size, actual.size)
        for (locale in expected) {
            assertTrue(actual.contains(locale))
        }
    }
}
