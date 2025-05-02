package com.albertsons.acupick.test

import android.app.Application
import com.albertsons.acupick.test.mocks.testApplicationFactory
import com.albertsons.acupick.ui.arrivals.complete.HandOffPagerViewModel

fun handOffPagerViewModelFactory(
    app: Application = testApplicationFactory(),
) = HandOffPagerViewModel(
    app = app,
)
