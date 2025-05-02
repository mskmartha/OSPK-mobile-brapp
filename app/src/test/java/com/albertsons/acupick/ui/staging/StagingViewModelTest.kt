package com.albertsons.acupick.ui.staging

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.mocks.testApplicationFactory
import org.junit.Rule
import org.junit.rules.TestRule

class StagingViewModelTest : BaseTest() {
    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    @get:Rule
    val koinRule = KoinTestRule(TestModule.generateMockedTestModule())

    // /////////////////////////////////////////////////////////////////////////
    // Initial UI setup tests
    // /////////////////////////////////////////////////////////////////////////
    // @Test
    // fun `WHEN VM is setup is called on THEN UI initializes correctly`() {
    //     // Setup VM
    //     val mainActivityViewModel = activityViewModelFactory()
    //     val vm = stagingViewModelTestFactory(
    //         activityViewModel = mainActivityViewModel
    //     )
    //
    //     inlineKoinSingle(override = true) {
    //         testPickRepository
    //     }
    //
    //     // Setup observer mocks
    //     val showReloadDialogMock = mainActivityViewModel.showErrorDialog.mock()
    //     val activityDtoMock = vm.activityDto.mock()
    //     val isStageButtonEnabledMock = vm.isStageButtonEnabled.mock()
    //
    //     // init vm
    //     vm.setupStaging("123", true)
    //
    //     // verify
    //     verify(showReloadDialogMock, never()).onChanged(
    //         DialogInfo(
    //             tag = "errorDialogTag",
    //             data = CustomDialogArgData(
    //                 title = StringIdHelper.Raw(""),
    //                 body = StringIdHelper.Raw("")
    //             )
    //         )
    //     )
    //     assertThat(activityDtoMock.verifyWithCapture()).isEqualTo(testActivity)
    //     assertThat(isStageButtonEnabledMock.verifyWithCapture(atLeastOnce())).isTrue()
    // }

    // @Test
    // fun `WHEN VM is setup and networkOffline then UI displays correct errors`() {
    //     // Setup VM
    //
    //     val vm = stagingViewModelTestFactory()
    //
    //     // Setup Observer Mocks
    //     val showReloadDialogMock = vm.inlineDialogEvent.mock()
    //
    //     // init vm
    //     vm.setupStaging("123", true)
    //
    //     // verify
    //     assertThat(showReloadDialogMock.verifyWithCapture(atLeastOnce())).isEqualTo(
    //         CustomDialogArgDataAndTag(
    //             tag = "stagingLoadDataRetryDialogTag",
    //             data = CustomDialogArgData(
    //                 title = StringIdHelper.Id(R.string.wifi_error_title),
    //                 body = StringIdHelper.Id(R.string.wifi_error_body),
    //                 positiveButtonText = StringIdHelper.Id(R.string.refresh),
    //                 cancelOnTouchOutside = false
    //             )
    //         )
    //     )
    // }

    private fun stagingViewModelTestFactory(
        app: Application = testApplicationFactory(),
    ) = StagingViewModel(app = app)
}
