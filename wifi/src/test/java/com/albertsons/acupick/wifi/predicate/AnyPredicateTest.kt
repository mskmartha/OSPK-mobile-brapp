package com.albertsons.acupick.wifi.predicate

import com.albertsons.acupick.wifi.model.WiFiDetail
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnyPredicateTest {

    @Test
    fun testAnyPredicateIsTrue() {
        // setup
        val wiFiDetail = WiFiDetail()
        val fixture = listOf(falsePredicate, truePredicate, falsePredicate).anyPredicate()
        // execute
        val actual = fixture(wiFiDetail)
        // validate
        assertTrue(actual)
    }

    @Test
    fun testAnyPredicateIsFalse() {
        // setup
        val wiFiDetail = WiFiDetail()
        val fixture = listOf(falsePredicate, falsePredicate, falsePredicate).anyPredicate()
        // execute
        val actual = fixture(wiFiDetail)
        // validate
        assertFalse(actual)
    }
}
