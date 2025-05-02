package com.albertsons.acupick.wifi.model

import com.albertsons.acupick.wifi.model.Strength.Companion.calculate
import com.albertsons.acupick.wifi.model.Strength.Companion.reverse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthTest {
    @Test
    fun testStrength() {
        assertEquals(5, Strength.values().size)
    }

    @Test
    fun testWeak() {
        assertTrue(Strength.ZERO.weak())
        assertFalse(Strength.ONE.weak())
        assertFalse(Strength.TWO.weak())
        assertFalse(Strength.THREE.weak())
        assertFalse(Strength.FOUR.weak())
    }

    @Test
    fun testCalculate() {
        assertEquals(Strength.ZERO, calculate(-89))
        assertEquals(Strength.ONE, calculate(-88))
        assertEquals(Strength.ONE, calculate(-78))
        assertEquals(Strength.TWO, calculate(-77))
        assertEquals(Strength.TWO, calculate(-67))
        assertEquals(Strength.THREE, calculate(-66))
        assertEquals(Strength.THREE, calculate(-56))
        assertEquals(Strength.FOUR, calculate(-55))
        assertEquals(Strength.FOUR, calculate(0))
    }

    @Test
    fun testReverse() {
        assertEquals(Strength.FOUR, reverse(Strength.ZERO))
        assertEquals(Strength.THREE, reverse(Strength.ONE))
        assertEquals(Strength.TWO, reverse(Strength.TWO))
        assertEquals(Strength.ONE, reverse(Strength.THREE))
        assertEquals(Strength.ZERO, reverse(Strength.FOUR))
    }
}
