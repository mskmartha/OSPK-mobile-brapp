package com.albertsons.acupick.test

import android.os.Build
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Sets BUILD.VERSION.SDK_INT to 27 on test start and 0 on finish. Some androidx compat apis rely on SDK_INT (ex: ContextCompat.getDrawable) */
class BuildVersion27Rule : TestWatcher() {
    override fun starting(description: Description?) = Build.VERSION::class.java.getField("SDK_INT").setFinalStatic(27)
    override fun finished(description: Description?) = Build.VERSION::class.java.getField("SDK_INT").setFinalStatic(0)
}
