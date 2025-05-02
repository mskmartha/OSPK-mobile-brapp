package com.albertsons.acupick.test

import android.app.Application
import com.albertsons.acupick.data.buildconfig.BuildConfigProvider
import com.albertsons.acupick.data.crashreporting.ForceCrashLogic
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.data.toast.Toaster
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.test.mocks.testApplicationFactory
import com.albertsons.acupick.test.mocks.testNetworkAvailabilityManager
import com.albertsons.acupick.test.mocks.testUserRepo
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.auth.LoginViewModel
import org.mockito.kotlin.mock

fun loginViewModelFactory(
    app: Application = testApplicationFactory(),
    userRepository: UserRepository = testUserRepo,
    buildConfigProvider: BuildConfigProvider = mock {},
    forceCrashLogic: ForceCrashLogic = mock {},
    dispatcherProvider: DispatcherProvider = TestDispatcherProvider(),
    activityViewModel: MainActivityViewModel = activityViewModelFactory(app = app),
    networkAvailabilityManager: NetworkAvailabilityManager = testNetworkAvailabilityManager,
    toaster: Toaster = mock {},
) = LoginViewModel(
    app = app,
    userRepo = userRepository,
    buildConfigProvider = buildConfigProvider,
    forceCrashLogic = forceCrashLogic,
    dispatcherProvider = dispatcherProvider,
    activityViewModel = activityViewModel,
    networkAvailabilityManager = networkAvailabilityManager,
    toaster = toaster
)
