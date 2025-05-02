package com.albertsons.acupick.test

import android.app.Application
import com.albertsons.acupick.test.mocks.testApplicationFactory
import com.albertsons.acupick.ui.arrivals.complete.HandOffUI
import com.albertsons.acupick.ui.arrivals.complete.HandOffViewModel

fun handOffViewModelTestFactory(
    app: Application = testApplicationFactory(),
    initialUi: HandOffUI,
) = HandOffViewModel(app = app, initialUi = initialUi, activityViewModelFactory(app))
