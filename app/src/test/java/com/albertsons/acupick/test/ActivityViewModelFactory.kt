package com.albertsons.acupick.test

import android.app.Application
import com.albertsons.acupick.data.buildconfig.BuildConfigProvider
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.network.NetworkAvailabilityController
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.stateFlowOf
import com.albertsons.acupick.test.mocks.testApplicationFactory
import com.albertsons.acupick.test.mocks.testUserRepo
import com.albertsons.acupick.ui.MainActivityViewModel
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

fun activityViewModelFactory(
    app: Application = testApplicationFactory(),
    userRepo: UserRepository = testUserRepo,
    barcodeMapper: BarcodeMapper = mock() { },
    networkAvailabilityManager: NetworkAvailabilityManager = mock {
        onBlocking { isConnected } doReturn stateFlowOf(true)
    },
    networkAvailabilityController: NetworkAvailabilityController = mock {
        on { showOfflineError } doReturn stateFlowOf(Unit)
    },
    buildConfigProvider: BuildConfigProvider = mock {},
    dispatcherProvider: DispatcherProvider = TestDispatcherProvider()

) = MainActivityViewModel(
    app = app,
    userRepo = userRepo,
    barcodeMapper = barcodeMapper,
    networkAvailabilityManager = networkAvailabilityManager,
    networkAvailabilityController = networkAvailabilityController,
    buildConfigProvider = buildConfigProvider,
    dispatcherProvider = dispatcherProvider,
    moshi = mock(),
)
