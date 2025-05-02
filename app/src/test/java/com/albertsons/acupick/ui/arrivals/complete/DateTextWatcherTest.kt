package com.albertsons.acupick.ui.arrivals.complete

import com.albertsons.acupick.test.BaseTest
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class DateTextWatcherTest : BaseTest() {

    @Test
    fun `WHEN input is empty THEN isEntryPartiallyValid should be true`() {
        assertTrue("".isEntryPartiallyValid())
    }

    @Test
    fun `WHEN has one digit THEN isEntryPartiallyValid should be true`() {
        assertTrue("1".isEntryPartiallyValid())
    }

    @Test
    fun `WHEN input needs its first dash THEN isEntryPartiallyValid should be true`() {
        assertTrue("10".isEntryPartiallyValid())
    }

    @Test
    fun `WHEN input has its first dash THEN isEntryPartiallyValid should be true`() {
        assertTrue("10-".isEntryPartiallyValid())
    }

    @Test
    fun `WHEN input has a digit after the dash THEN isEntryPartiallyValid should be true`() {
        assertTrue("10-1".isEntryPartiallyValid())
    }

    @Test
    fun `WHEN input needs its second dash THEN isEntryPartiallyValid should be true`() {
        assertTrue("10-12".isEntryPartiallyValid())
    }

    @Test
    fun `WHEN input has its second dash THEN isEntryPartiallyValid should be true`() {
        assertTrue("10-12-".isEntryPartiallyValid())
    }

    @Test
    fun `WHEN input has a digit after the second dash THEN isEntryPartiallyValid should be false`() {
        assertFalse("10-12-1".isEntryPartiallyValid())
    }

    @Test
    fun `WHEN input has three digits after the second dash THEN isEntryPartiallyValid should be false`() {
        assertFalse("10-12-198".isEntryPartiallyValid())
    }

    @Test
    fun `WHEN day is above a valid range for a month THEN isEntryPartiallyValid should be false`() {
        assertFalse("10-32".isEntryPartiallyValid())
    }

    @Test
    fun `WHEN month is above a valid range for a year THEN isEntryPartiallyValid should be false`() {
        assertFalse("13".isEntryPartiallyValid())
    }
}
