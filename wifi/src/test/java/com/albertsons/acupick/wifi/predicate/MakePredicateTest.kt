package com.albertsons.acupick.wifi.predicate

import com.albertsons.acupick.wifi.model.WiFiDetail
import org.junit.Assert.assertTrue
import org.junit.Test

class MakePredicateTest {

    @Test
    fun testMakePredicateExpectsTruePredicate() {
        // setup
        val wiFiDetail = WiFiDetail()
        val toPredicate: ToPredicate<TestObject> = { truePredicate }
        val filters: Set<TestObject> = TestObject.values().toSet()
        // execute
        val actual: Predicate = makePredicate(TestObject.values(), filters, toPredicate)
        // validate
        assertTrue(actual(wiFiDetail))
    }

    @Test
    fun testMakePredicateExpectsAnyPredicate() {
        // setup
        val wiFiDetail = WiFiDetail()
        val toPredicate: ToPredicate<TestObject> = { truePredicate }
        val filters: Set<TestObject> = setOf(TestObject.VALUE1, TestObject.VALUE3)
        // execute
        val actual: Predicate = makePredicate(TestObject.values(), filters, toPredicate)
        // validate
        assertTrue(actual(wiFiDetail))
    }
}
