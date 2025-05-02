package com.albertsons.acupick.ui.arrivals.complete

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.data.model.CancelReasonCode
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.handOffPagerViewModelFactory
import com.albertsons.acupick.test.mocks.mockAps
import com.albertsons.acupick.test.mocks.mockApsFailures
import com.albertsons.acupick.test.mocks.testActivity
import com.albertsons.acupick.test.runPrivateMethod
import com.albertsons.acupick.ui.arrivals.complete.HandOffPagerViewModel.Companion.HANDOFF_LEAVE_SCREEN_DIALOG_TAG
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.ORDER_DETAILS_CANCEL_ARG_DATA
import com.albertsons.acupick.ui.util.StringIdHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyNoInteractions
import java.time.ZonedDateTime

class HandOffPagerViewModelTest : BaseTest() {

    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    @get:Rule
    val koinRule = KoinTestRule(TestModule.generateMockedTestModule())

    @Test
    fun `WHEN vm initialized THEN tabs populated correctly`() {
        val vm = handOffPagerViewModelFactory()
        val tabsMock = vm.tabsLiveData.mock()
        vm.setHandOffOrderUiList(
            listOf(
                HandOffUI(
                    activityDto = testActivity,
                    scanContainerWrapperRequestDto = null,
                    confirmRxPickupRequestDto = null,
                    groceryDestageStartTimestamp = null,
                    groceryDestageCompleteTimestamp = null
                )
            )
        )
        assertThat(tabsMock.verifyWithCapture(atLeastOnce())).isNotEmpty()
    }

    @Test
    fun `WHEN updateHandOffState() called with updated hand-off data THEN pendingActionsMapLiveData updated correctly`() {
        val vm = handOffPagerViewModelFactory()
        val testHandOffUI = HandOffUI(
            activityDto = testActivity,
            scanContainerWrapperRequestDto = null,
            confirmRxPickupRequestDto = null,
            groceryDestageStartTimestamp = null,
            groceryDestageCompleteTimestamp = null
        )
        vm.setHandOffOrderUiList(listOf(testHandOffUI))
        val pendingActionsMapLiveDataMock = vm.pendingActionsMapLiveData.mock()
        vm.updateHandOffState(
            testHandOffUI,
            HandOffResultData(completeOrCancelTime = ZonedDateTime.now(), isIdVerified = false)
        )
        val capturedMap = pendingActionsMapLiveDataMock.verifyWithCapture(atLeastOnce())
        assertThat(!capturedMap.values.toList().first().isCancel).isTrue()
    }

    @Test
    fun `WHEN updateHandOffState() called with restaged order THEN pendingActionsMapLiveData updated correctly`() {
        val vm = handOffPagerViewModelFactory()
        val testHandOffUI = HandOffUI(
            activityDto = testActivity,
            scanContainerWrapperRequestDto = null,
            confirmRxPickupRequestDto = null,
            groceryDestageStartTimestamp = null,
            groceryDestageCompleteTimestamp = null
        )
        vm.setHandOffOrderUiList(listOf(testHandOffUI))

        val pendingActionsMapLiveDataMock = vm.pendingActionsMapLiveData.mock()
        vm.updateHandOffState(
            testHandOffUI,
            HandOffResultData(
                isCancel = true,
                cancelReasonCode = CancelReasonCode.CUSTOMER_NOT_HERE,
                completeOrCancelTime = ZonedDateTime.now(),
                isIdVerified = true
            )
        )
        val capturedMap = pendingActionsMapLiveDataMock.verifyWithCapture(atLeastOnce())
        assertThat(capturedMap.values.toList().first().isCancel).isTrue()
        assertThat(capturedMap.values.toList().first().cancelReasonCode == CancelReasonCode.CUSTOMER_NOT_HERE).isTrue()
    }

    @Test @Ignore
    fun `WHEN handleExitButton THEN live data updated`() {
        val vm = handOffPagerViewModelFactory()
        val dialogEventMock = vm.inlineDialogEvent.mock()
        vm.handleExitButton()
        assertThat(dialogEventMock.verifyWithCapture(atLeastOnce())).isEqualTo(
            CustomDialogArgDataAndTag(
                data = ORDER_DETAILS_CANCEL_ARG_DATA,
                tag = HANDOFF_LEAVE_SCREEN_DIALOG_TAG
            )
        )
    }

    @Test
    fun `WHEN navigateToSearchOrdersPager THEN live data updated`() {
        val vm = handOffPagerViewModelFactory()
        val navigationEventMock = vm.navigationEvent.mock()
        vm.runPrivateMethod("navigateToSearchOrdersPager")
        assertThat(navigationEventMock.verifyWithCapture(atLeastOnce())).isEqualTo(
            NavigationEvent.Directions(
                HandOffPagerFragmentDirections.actionHandOffFragmentToArrivalsOrdersPagerFragment()
            )
        )
    }

    @Test @Ignore
    fun `WHEN exitHandOff with null handoff ui list THEN nothing happens`() {
        val vm = handOffPagerViewModelFactory()
        vm.setHandOffOrderUiList(null)
        val navEventMock = vm.navigationEvent.mock()
        vm.runPrivateMethod("exitHandOff")
        verifyNoInteractions(navEventMock)
    }

    @Test @Ignore
    fun `WHEN exitHandOff with handoff ui list and success THEN no snackbar and nav to search orders`() {
        inlineKoinSingle(override = true) { mockAps }
        val vm = handOffPagerViewModelFactory()
        vm.setHandOffOrderUiList(listOf())
        val snackbarMock = vm.snackBarLiveEvent.mock()
        val navEventMock = vm.navigationEvent.mock()
        vm.runPrivateMethod("exitHandOff")
        var snackbarNotTriggered = false
        try {
            assertThat(snackbarMock.verifyWithNullableCapture(times(0)))
        } catch (e: NoSuchElementException) {
            if (e.localizedMessage == "List is empty.")
                snackbarNotTriggered = true
        }
        assertThat(snackbarNotTriggered).isTrue()
        assertThat(navEventMock.verifyWithCapture(atLeastOnce())).isEqualTo(
            NavigationEvent.Directions(
                HandOffPagerFragmentDirections.actionHandOffFragmentToArrivalsOrdersPagerFragment()
            )
        )
    }

    @Test @Ignore
    fun `WHEN exitHandOff with handoff ui list and fail THEN snackbar and nav to search orders`() {
        inlineKoinSingle(override = true) { mockApsFailures }
        val vm = handOffPagerViewModelFactory()
        val handoffUI = HandOffUI(
            activityDto = testActivity,
            scanContainerWrapperRequestDto = null,
            confirmRxPickupRequestDto = null,
            groceryDestageStartTimestamp = null,
            groceryDestageCompleteTimestamp = null
        )
        vm.setHandOffOrderUiList(listOf(handoffUI))
        val snackbarMock = vm.snackBarLiveEvent.mock()
        val navEventMock = vm.navigationEvent.mock()
        vm.runPrivateMethod("exitHandOff")
        assertThat(snackbarMock.verifyWithNullableCapture(atLeastOnce())?.prompt).isEqualTo(StringIdHelper.Raw("Error cancelling handoff"))
        assertThat(navEventMock.verifyWithCapture(atLeastOnce())).isEqualTo(
            NavigationEvent.Directions(
                HandOffPagerFragmentDirections.actionHandOffFragmentToArrivalsOrdersPagerFragment()
            )
        )
    }
}
