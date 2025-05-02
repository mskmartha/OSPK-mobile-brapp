package com.albertsons.acupick.test

import android.app.Application
import com.albertsons.acupick.test.mocks.testApplicationFactory
import com.albertsons.acupick.ui.arrivals.destage.DestageOrderViewModel

fun destageOrderViewModelFactory(
    app: Application = testApplicationFactory(),
) = DestageOrderViewModel(
    app = app,
)
