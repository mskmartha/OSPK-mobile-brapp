package com.albertsons.acupick.ui.staging

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.albertsons.acupick.R
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.response.BagAndLooseItemActivityDto
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.StagingStateRepository
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.mocks.testApplicationFactory
import com.albertsons.acupick.test.mocks.testPickRepository
import com.albertsons.acupick.ui.util.StringIdHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times

class StagingPagerViewModelTest : BaseTest() {
    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    @get:Rule
    val koinRule = KoinTestRule(TestModule.generateMockedTestModule())

    @Test
    fun `WHEN VM is setup and get activity details call fails THEN correct error message is shown`() {
        // DI
        inlineKoinSingle(override = true) {
            testPickRepository().stub {
                onBlocking { getActivityDetails(any(), any()) } doReturn ApiResult.Failure.GeneralFailure("GenericFailure")
            }
        }

        inlineKoinSingle { mock<StagingStateRepository> {} }

        // Setup VM
        val vm = stagingViewPagerModelTestFactory()

        // Setup Observer Mocks
        val errorEventMock = vm.apiErrorEvent.mock()

        // init vm
        vm.activityId.value = "123"
        vm.toteLabelsPrintedSuccessfully.value = true

        // verify
        assertThat(errorEventMock.verifyWithCapture(atLeastOnce())).isEqualTo(
            Pair("", ApiResult.Failure.GeneralFailure(message = "GenericFailure"))
        )
    }

    // /////////////////////////////////////////////////////////////////////////
    // Method tests
    // /////////////////////////////////////////////////////////////////////////

    @Test
    fun `WHEN reprintLabels is called and fails THEN snackbar is displayed`() {
        // DI
        inlineKoinSingle(override = true) {
            testApsRepository.stub {
                onBlocking { printToteLabel(any()) } doReturn ApiResult.Failure.GeneralFailure(".")
            }
        }

        inlineKoinSingle { mock<StagingStateRepository> {} }

        // Setup VM
        val vm = stagingViewPagerModelTestFactory()

        // Setup Observer Mocks
        val acupickSnackBarLiveEventMock = vm.acupickSnackEvent.mock()

        // init vm
        vm.activityId.value = "123"
        vm.toteLabelsPrintedSuccessfully.value = true

        // execute code to be tested
        vm.reprintLabels()

        // verify
        assertThat(acupickSnackBarLiveEventMock.verifyWithNullableCapture(atLeastOnce())?.message).isEqualTo(
            StringIdHelper.Id(R.string.error_printing_labels)
        )
    }

    @Test
    fun `WHEN reprintLabels is called and succeeds THEN no snackbar is displayed`() {

        // DI
        inlineKoinSingle { mock<StagingStateRepository> {} }

        // Setup VM
        val vm = stagingViewPagerModelTestFactory()

        // Setup Observer Mocks
        val snackBarLiveEventMock = vm.snackBarLiveEvent.mock()

        // init vm
        vm.activityId.value = "123"
        vm.toteLabelsPrintedSuccessfully.value = true

        // execute code to be tested
        vm.reprintLabels()

        // verify
        var snackbarNotTriggered = false
        try {
            assertThat(snackBarLiveEventMock.verifyWithNullableCapture(times(0)))
        } catch (e: NoSuchElementException) {
            if (e.localizedMessage == "List is empty.")
                snackbarNotTriggered = true
        }
        assertThat(snackbarNotTriggered).isTrue()
    }

    // /////////////////////////////////////////////////////////////////////////
    // Test Objects and Factories
    // /////////////////////////////////////////////////////////////////////////
    private val testApsRepository: ApsRepository
        get() = mock {
            onBlocking { printToteLabel(any()) } doReturn ApiResult.Success(Unit)
            onBlocking { recordBagCount(any()) } doReturn ApiResult.Success(BagAndLooseItemActivityDto())
        }

    private fun stagingViewPagerModelTestFactory(
        app: Application = testApplicationFactory(),
    ) = StagingPagerViewModel(app)
}
