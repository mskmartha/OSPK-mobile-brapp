package com.albertsons.acupick.wifi.predicate

import com.albertsons.acupick.wifi.model.WiFiDetail
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AllPredicateTest {

    @Test
    fun testAllPredicateIsTrue() {
        // setup
        val wiFiDetail = WiFiDetail()
        val fixture = listOf(truePredicate, truePredicate, truePredicate).allPredicate()
        // execute
        val actual = fixture(wiFiDetail)
        // validate
        assertTrue(actual)
    }

    @Test
    fun testAllPredicateIsFalse() {
        // setup
        val wiFiDetail = WiFiDetail()
        val fixture = listOf(falsePredicate, truePredicate, falsePredicate).allPredicate()
        // execute
        val actual = fixture(wiFiDetail)
        // validate
        assertFalse(actual)
    }
}
