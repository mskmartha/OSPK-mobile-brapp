package com.albertsons.acupick.test

import android.app.Application
import com.albertsons.acupick.test.mocks.testApplicationFactory
import com.albertsons.acupick.ui.arrivals.ArrivalsPagerViewModel
import com.albertsons.acupick.ui.arrivals.ArrivalsViewModel

fun arrivalsPagerViewModelFactory(
    app: Application = testApplicationFactory()
) = ArrivalsPagerViewModel(app = app, activityViewModel = activityViewModelFactory())

fun arrivalsViewModelFactory(
    app: Application = testApplicationFactory()
) = ArrivalsViewModel(app = app, activityViewModel = activityViewModelFactory())
