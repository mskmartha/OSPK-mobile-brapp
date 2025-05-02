package com.albertsons.acupick.ui.arrivals.complete

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.request.ScanContainerRequestDto
import com.albertsons.acupick.data.model.request.ScanContainerWrapperRequestDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.handOffViewModelTestFactory
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.ZonedDateTime

class HandOffViewModelTest : BaseTest() {

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
    // fun `WHEN vm is setup with no regulated items THEN UI initializes correctly`() {
    //     // Setup VM
    //     val vm = handOffViewModelTestFactory()
    //
    //     // Setup mocks
    //     val itemsMock = vm.handOffUI.map { it.items }.mock()
    //     val hasRegulatedItemsMock = vm.hasRegulatedItems.mock()
    //     val isCompleteEnabledMock = vm.isCompleteEnabled.mock()
    //
    //     // Init VM
    //     vm.handOffUI.postValue(handOffUIMock)
    //
    //     // Verify UI mocks
    //     assertThat(itemsMock.verifyWithCapture(atLeast(1))).hasSize(0)
    //     // Commenting out pending merge of in progress refactor
    //     // assertThat(totalRegulatedItemsMock.verifyWithCapture()).isEqualTo("0 Restricted Items")
    //     assertThat(hasRegulatedItemsMock.verifyWithCapture(atLeastOnce())).isFalse()
    //     assertThat(isCompleteEnabledMock.verifyWithCapture(atLeastOnce())).isFalse()
    // }

// /////////////////////////////////////////////////////////////////////////
    // LiveData tests
    // /////////////////////////////////////////////////////////////////////////
    @Test
    fun `WHEN onValidIdClicked THEN livedata set correctly`() {
        val vm = handOffViewModelTestFactory(initialUi = handOffUIMock)
        vm.isValidIdClicked.postValue(false)
        vm.isInvalidIdClicked.postValue(true)
        val isValidIdClickedMock = vm.isValidIdClicked.mock()
        val isInvalidIdClickedMock = vm.isInvalidIdClicked.mock()
        vm.onValidIdClicked()
        verify(isValidIdClickedMock).onChanged(
            true
        )
        verify(isInvalidIdClickedMock).onChanged(
            false
        )
    }

    @Test
    fun `WHEN onInvalidIdClicked THEN livedata set correctly`() {
        val vm = handOffViewModelTestFactory(initialUi = handOffUIMock)
        vm.isValidIdClicked.postValue(true)
        vm.isInvalidIdClicked.postValue(false)
        val isValidIdClickedMock = vm.isValidIdClicked.mock()
        val isInvalidIdClickedMock = vm.isInvalidIdClicked.mock()
        vm.onInvalidIdClicked()
        verify(isValidIdClickedMock).onChanged(
            false
        )
        verify(isInvalidIdClickedMock).onChanged(
            true
        )
    }

    // /////////////////////////////////////////////////////////////////////////
    // Test Objects and Factories
    // /////////////////////////////////////////////////////////////////////////

    private val scanContainerWrapperRequestDtoMock =
        ScanContainerWrapperRequestDto(
            actId = 1234,
            containerReqs = listOf(
                ScanContainerRequestDto(
                    containerId = "108301",
                    overrideAttemptToRemove = true,
                    overrideRemoved = true,
                    overrideScanUser = true,
                    stagingLocation = "AMA05",
                    startIfNotStarted = true
                )
            ),
            lastScanTime = ZonedDateTime.now(),
            multipleHandoff = false
        )

    private val handOffUIMock: HandOffUI = mock {
        on { name } doReturn "First Last"
        on { scanContainerWrapperRequestDto } doReturn scanContainerWrapperRequestDtoMock
        on { source } doReturn "Instacart"
        on { provider } doReturn "Uber"
    }

    private val testDugActivity: ActivityDto = mock {
        on { fulfillment } doReturn FulfillmentAttributeDto(type = FulfillmentType.DUG)
    }

    private val testDeliveryActivity: ActivityDto = mock {
        on { fulfillment } doReturn FulfillmentAttributeDto(type = FulfillmentType.DELIVERY)
    }
}
