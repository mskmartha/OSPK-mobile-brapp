package com.albertsons.acupick.data.test

import org.junit.Before
import timber.log.Timber

open class BaseTest {
    @Before
    fun plantTimber() {
        Timber.plant(SystemOutPrintlnTree())
    }
}
