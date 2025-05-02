package com.albertsons.acupick.ui.arrivals.complete

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.albertsons.acupick.R
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.CancelReasonCode
import com.albertsons.acupick.data.model.CompleteHandoffData
import com.albertsons.acupick.data.model.ContainerActivityStatus
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.CustomerArrivalStatus
import com.albertsons.acupick.data.model.EntityReference
import com.albertsons.acupick.data.model.HandOffAction
import com.albertsons.acupick.data.model.HandOffInterstitialParams
import com.albertsons.acupick.data.model.HandOffInterstitialParamsList
import com.albertsons.acupick.data.model.request.ScanContainerRequestDto
import com.albertsons.acupick.data.model.request.ScanContainerWrapperRequestDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ScanContActDto
import com.albertsons.acupick.data.model.response.ScanContDto
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.ServerErrorCodeDto
import com.albertsons.acupick.data.model.response.ServerErrorDto
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.domain.AcuPickLoggerInterface
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.mocks.testApplicationFactory
import com.albertsons.acupick.test.mocks.testNetworkAvailabilityManager
import com.albertsons.acupick.TestModule.acuPickLoggerTestImpl
import com.albertsons.acupick.data.model.OrderSummaryParams
import com.albertsons.acupick.data.model.OrderSummaryParamsList
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.usecase.handoff.CompleteHandoffUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.ZonedDateTime

class HandOffInterstitialViewModelTest : BaseTest() {

    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestCoroutineDispatcher())

    @get:Rule
    val koinRule = KoinTestRule(TestModule.generateMockedTestModule())

    // /////////////////////////////////////////////////////////////////////////
    // Initial UI setup tests
    // /////////////////////////////////////////////////////////////////////////
    @Test
    @ExperimentalCoroutinesApi
    fun `WHEN completing handoff successfully THEN navigating to destaging root is triggered`() {
        // Setup VM
        val vm = handOffInterstitialViewModelTestFactory(
            handOffInterstitialParamsList = completeHandoffParamsMock,
            apsRepository = testApsRepositoryPassAll,
            networkAvailabilityManager = testNetworkAvailabilityManager,
            acuPickLoggerInterface = acuPickLoggerTestImpl

        )

        // Setup Mocks
        val navigationEventMock = vm.navigationEvent.mock()

        // Execute code to be tested
        runBlockingTest(dispatcherRule.dispatcher) {
            vm.handleHandoffCompletion(completeHandoffParamsMock, OrderSummaryParamsListMock)
        }
        vm.backToArrivalOrHomeScreen()
        // Verify
        assertThat(navigationEventMock.verifyWithCapture()).isEqualTo(
            NavigationEvent.Back(destinationId = R.id.destageOrderFragment, inclusive = true)
        )
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `WHEN completing handoff and order was reassigned THEN show reassigned dialog`() {
        // Setup VM
        val vm = handOffInterstitialViewModelTestFactory(
            handOffInterstitialParamsList = completeHandoffParamsMock,
            apsRepository = testApsRepositoryReassigned,
            networkAvailabilityManager = testNetworkAvailabilityManager,
            acuPickLoggerInterface = acuPickLoggerTestImpl
        )

        // Setup Mocks
        val showReassignedDialogMock = vm.inlineDialogEvent.mock()

        // Execute code to be tested
        runBlockingTest(dispatcherRule.dispatcher) {
            vm.handleHandoffCompletion(completeHandoffParamsMock, OrderSummaryParamsListMock)
        }

        // Verify
        assertThat(showReassignedDialogMock.verifyWithCapture()).isEqualTo(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.hand_off_already_assigned_title),
                    body = StringIdHelper.Id(R.string.hand_off_already_assigned_body),
                    positiveButtonText = StringIdHelper.Id(R.string.ok),
                ),
                tag = BaseViewModel.SINGLE_ORDER_ERROR_DIALOG_TAG
            )
        )
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `WHEN cancelling handoff successfully THEN navigating to destaging root is triggered`() {
        // Setup VM
        val vm = handOffInterstitialViewModelTestFactory(
            handOffInterstitialParamsList = cancelHandoffParamsMock,
            apsRepository = testApsRepositoryPassAll,
            networkAvailabilityManager = testNetworkAvailabilityManager,
            acuPickLoggerInterface = acuPickLoggerTestImpl
        )

        // Setup Mocks
        val navigationEventMock = vm.navigationEvent.mock()

        // Execute code to be tested
        runBlockingTest(dispatcherRule.dispatcher) {
            vm.handleHandoffCompletion(cancelHandoffParamsMock, OrderSummaryParamsListMock)
        }
        vm.backToArrivalOrHomeScreen()
        // Verify
        assertThat(navigationEventMock.verifyWithCapture()).isEqualTo(
            NavigationEvent.Back(destinationId = R.id.destageOrderFragment, inclusive = true)
        )
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `WHEN cancelling handoff and order was reassigned THEN show reassigned dialog`() {
        // Setup VM
        val vm = handOffInterstitialViewModelTestFactory(
            handOffInterstitialParamsList = cancelHandoffParamsMock,
            apsRepository = testApsRepositoryReassigned,
            networkAvailabilityManager = testNetworkAvailabilityManager,
            acuPickLoggerInterface = acuPickLoggerTestImpl
        )

        // Setup Mocks
        val showReassignedDialogMock = vm.inlineDialogEvent.mock()

        // Execute code to be tested
        runBlockingTest(dispatcherRule.dispatcher) {
            vm.handleHandoffCompletion(cancelHandoffParamsMock, OrderSummaryParamsListMock)
        }

        // Verify
        assertThat(showReassignedDialogMock.verifyWithCapture()).isEqualTo(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.hand_off_already_assigned_title),
                    body = StringIdHelper.Id(R.string.hand_off_already_assigned_body),
                    positiveButtonText = StringIdHelper.Id(R.string.ok),
                ),
                tag = BaseViewModel.SINGLE_ORDER_ERROR_DIALOG_TAG
            )
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
                    startIfNotStarted = true,
                )
            ),
            lastScanTime = ZonedDateTime.now(),
            multipleHandoff = false
        )

    private val scanContActDtoMock: ScanContActDto =
        ScanContActDto(
            id = 4321,
            containerId = "108301",
            location = "AMA05",
            containerType = "AM",
            reference = EntityReference("1212", "Type"),
            status = ContainerActivityStatus.PROCESSED,
            attemptToRemove = false,
            lastScanTime = ZonedDateTime.now(),
            bagCount = 1,
            looseItemCount = 0,
            type = ContainerType.BAG,
            regulated = false,
        )

    private val scanContDtoMock: ScanContDto =
        ScanContDto(
            listOf(
                ScanContActDto(
                    id = 4321,
                    containerId = "108301",
                    location = "AMA05",
                    containerType = "AM",
                    reference = EntityReference("1212", "Type"),
                    status = ContainerActivityStatus.PROCESSED,
                    attemptToRemove = false,
                    lastScanTime = ZonedDateTime.now(),
                    bagCount = 1,
                    looseItemCount = 0,
                    type = ContainerType.BAG,
                    regulated = false,
                )
            ),
            subStatus = CustomerArrivalStatus.ARRIVED,
            nextActExpStartTime = null,
            vehicleInfo = null
        )

    private val completeHandoffParamsMock =
        HandOffInterstitialParamsList(
            listOf(
                HandOffInterstitialParams(
                    activityId = 1234L,
                    cancelReasonCode = null,
                    erId = 123L,
                    handOffAction = HandOffAction.COMPLETE,
                    isIdVerified = true,
                    isPreCompleted = false,
                    confirmOrderTime = ZonedDateTime.now(),
                    completeOrCancelTime = ZonedDateTime.now(),
                    orderNumber = "12345678",
                    scanContainerWrapperRequestDto = scanContainerWrapperRequestDtoMock,
                    siteId = "4321",
                    issuesScanningBag = null,
                    authCodeUnavailableReasonCode = null,
                    authenticatedPin = null,
                    otp = "1234",
                    orderId = "",
                    storeNumber = "",
                    orderStatus = null,
                    cartType = null,
                    customerArrivalTimestamp = null,
                    deliveryCompleteTimestamp = null,
                    groceryDestageStartTimestamp = null,
                    groceryDestageCompleteTimestamp = null,
                    otpCapturedTimestamp = null,
                    otpBypassTimestamp = null,
                    scheduledPickupTimestamp = null,
                    rxOrders = null,
                    confirmRxPickupRequestDto = null,
                    unableToPickOrder = true,
                    pickupUserInfoReq = null,
                    giftLabelConfirmation = null
                )
            )
        )

    private val cancelHandoffParamsMock =
        HandOffInterstitialParamsList(
            listOf(
                HandOffInterstitialParams(
                    activityId = 1234L,
                    cancelReasonCode = CancelReasonCode.CUSTOMER_NOT_HERE,
                    erId = 123L,
                    handOffAction = HandOffAction.CANCEL,
                    isIdVerified = true,
                    isPreCompleted = false,
                    confirmOrderTime = ZonedDateTime.now(),
                    completeOrCancelTime = ZonedDateTime.now(),
                    orderNumber = "12345678",
                    scanContainerWrapperRequestDto = scanContainerWrapperRequestDtoMock,
                    siteId = "4321",
                    issuesScanningBag = null,
                    authCodeUnavailableReasonCode = null,
                    authenticatedPin = null,
                    otp = "1234",
                    orderId = "",
                    storeNumber = "",
                    orderStatus = null,
                    cartType = null,
                    customerArrivalTimestamp = null,
                    deliveryCompleteTimestamp = null,
                    groceryDestageStartTimestamp = null,
                    groceryDestageCompleteTimestamp = null,
                    otpCapturedTimestamp = null,
                    otpBypassTimestamp = null,
                    scheduledPickupTimestamp = null,
                    rxOrders = null,
                    confirmRxPickupRequestDto = null,
                    unableToPickOrder = true,
                    pickupUserInfoReq = null,
                    giftLabelConfirmation = null
                )
            )
        )

    private val OrderSummaryParamsListMock = OrderSummaryParamsList(
        listOf(
            OrderSummaryParams(
                orderNumber = "12345678",
                isCas = true,
                orderSummary = emptyList(),
                is3p = true,
                source = "source"
            )
        )
    )

    private val serverErrorReassigned =
        ApiResult.Failure.Server(
            error = ServerErrorDto(
                errorCode = ServerErrorCodeDto(
                    rawValue = 57,
                    resolvedType = ServerErrorCode.USER_NOT_VALID
                )
            )
        )

    private val testApsRepositoryPassAll: ApsRepository = mock {
        onBlocking { preCompleteActivity(any()) } doReturn ApiResult.Success(ActivityDto())
        onBlocking { pickupComplete(any()) } doReturn ApiResult.Success(ActivityDto())
        onBlocking { scanContainers(any()) } doReturn ApiResult.Success(scanContDtoMock)
        onBlocking { cancelHandoff(any()) } doReturn ApiResult.Success(Unit)
    }
    private val testApsRepositoryReassigned: ApsRepository = mock {
        onBlocking { preCompleteActivity(any()) } doReturn serverErrorReassigned
        onBlocking { pickupComplete(any()) } doReturn serverErrorReassigned
        onBlocking { scanContainers(any()) } doReturn serverErrorReassigned
        onBlocking { cancelHandoff(any()) } doReturn serverErrorReassigned
    }

    private fun handOffInterstitialViewModelTestFactory(
        app: Application = testApplicationFactory(),
        handOffInterstitialParamsList: HandOffInterstitialParamsList,
        apsRepository: ApsRepository,
        networkAvailabilityManager: NetworkAvailabilityManager,
        acuPickLoggerInterface: AcuPickLoggerInterface,
        completeHandoffUseCase: CompleteHandoffUseCase = CompleteHandoffUseCase(
            completeHandoffRepository = mock {
                onBlocking { loadCompleteHandoff() } doReturn CompleteHandoffData(handOffInterstitialParamsList)
            },
            apsRepository = apsRepository,
            networkAvailabilityManager = networkAvailabilityManager,
            acuPickLoggerInterface = acuPickLoggerInterface
        ),
    ) = HandOffInterstitialViewModel(
        app = app,
        completeHandoffUseCase = completeHandoffUseCase
    )
}
