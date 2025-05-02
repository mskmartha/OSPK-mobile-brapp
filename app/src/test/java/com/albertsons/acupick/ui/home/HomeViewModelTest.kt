package com.albertsons.acupick.ui.home

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.albertsons.acupick.R
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.ActivityType
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.CountByFulfillmentTypes
import com.albertsons.acupick.data.model.CustomerArrivalStatus
import com.albertsons.acupick.data.model.EntityReference
import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.HandshakeType
import com.albertsons.acupick.data.model.OrderByCountType
import com.albertsons.acupick.data.model.OrderCountByStoreDto
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.User
import com.albertsons.acupick.data.model.request.UserDto
import com.albertsons.acupick.data.model.response.ActivityAndErDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.AppSummaryResponseDto
import com.albertsons.acupick.data.model.response.PickListBatchingType
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.ServerErrorCodeDto
import com.albertsons.acupick.data.model.response.ServerErrorDto
import com.albertsons.acupick.data.model.response.SiteDto
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.stateFlowOf
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.activityViewModelFactory
import com.albertsons.acupick.test.mocks.testContext
import com.albertsons.acupick.test.mocks.testMfcSiteRepo
import com.albertsons.acupick.test.mocks.testPickRepository
import com.albertsons.acupick.test.mocks.testResources
import com.albertsons.acupick.test.mocks.testSiteRepo
import com.albertsons.acupick.usecase.handoff.CompleteHandoff1PLUseCase
import com.albertsons.acupick.usecase.handoff.CompleteHandoffUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import java.time.LocalDateTime
import java.time.ZonedDateTime

class HomeViewModelTest : BaseTest() {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    @get:Rule
    val koinRule = KoinTestRule(TestModule.generateMockedTestModule())

    private val testActivity: ActivityDto = mock {}

    @Test
    fun `Validate load`() {
        // Init VM
        val vm = homeViewModelFactory()

        vm.load()
        runBlocking { vm.setNextPickTitle(vm.cardData.value, false) }

        // Setup Mocks
        val displayNameMock = vm.displayName.mock()
        val openOrderCountMock = vm.openOrderCount.mock()
        val handOffOrderCountMock = vm.handOffOrderCount.mock()
        val dugCountMock = vm.dugCount.mock()
        val deliveryCountMock = vm.deliveryCount.mock()
        val ambientActiveMock = vm.ambientActive.mock()
        val coldActiveMock = vm.coldActive.mock()
        val frozenActiveMock = vm.frozenActive.mock()
        val nextPickTitleMock = vm.nextPickTitle.mock()
        val pickingTextMock = vm.pickingText.mock()
        val picklistType = vm.pickListType.mock()
        val orderCount = vm.orderCount.mock()

        // Verify UI
        assertThat(displayNameMock.verifyWithCapture()).isEqualTo("t. last")
        assertThat(openOrderCountMock.verifyWithCapture()).isEqualTo("1")
        assertThat(handOffOrderCountMock.verifyWithCapture()).isEqualTo("1")
        assertThat(dugCountMock.verifyWithCapture()).isEqualTo("1")
        assertThat(deliveryCountMock.verifyWithCapture()).isEqualTo("0")
        assertThat(ambientActiveMock.verifyWithCapture(atLeastOnce())).isTrue()
        assertThat(coldActiveMock.verifyWithCapture(atLeastOnce())).isTrue()
        assertThat(frozenActiveMock.verifyWithCapture(atLeastOnce())).isFalse()
        assertThat(nextPickTitleMock.verifyWithCapture(atLeastOnce())).isEqualTo("Hi, wanna pick up where you left off?")
        assertThat(pickingTextMock.verifyWithCapture(atLeastOnce())).isEqualTo("Start staging")
        assertThat(picklistType.verifyWithCapture(atLeastOnce())).isEqualTo(PickListBatchingType.SingleOrder)
        assertThat(orderCount.verifyWithCapture(atLeastOnce())).isEqualTo(2)
    }

    @Test
    fun `When onPickListCardCtaClicked and order is in stage THEN navigate to stage`() {
        val viewModel = homeViewModelFactory()
        viewModel.load()
        val mockNavObserver = viewModel.navigationEvent.mock()
        viewModel.onPickListCardCtaClicked()
        // TODO: Fix this test
        // verify(mockNavObserver, after(300)!!.atLeastOnce()).onChanged(
        //     NavigationEvent.Directions(
        //         HomeFragmentDirections.actionHomeFragmentToStagingFragment(
        //             activityId = "1000", isPreviousPrintSuccessful = true
        //         )
        //     )
        // )
    }

    @Test
    fun `When onPickListCardCtaClicked has order already assigned THEN show order assigned dialog`() {
        val viewModel = homeViewModelFactory(
            pickRepository = mock {
                on { pickList } doReturn stateFlowOf(testActivity)
                onBlocking { assignUser(any()) } doReturn ApiResult.Failure.Server(
                    error = ServerErrorDto(
                        debugMessage = null,
                        errorCode = ServerErrorCodeDto(
                            rawValue = ServerErrorCode.NO_USER_TO_ASSIGN_ACTIVITY.value,
                            resolvedType = ServerErrorCode.NO_USER_TO_ASSIGN_ACTIVITY
                        ),
                        message = null,
                        status = null
                    )
                )
                // on { hasActivePickListActivityId() } doReturn false
            },
            apsRepository = apsRepoFactoryNotStagingNotCurrentUser()
        )
        viewModel.load()
        val mockObserver = viewModel.inlineDialogEvent.mock()
        viewModel.onPickListCardCtaClicked()
        // TODO: Fix this test
        // mockObserver.verifyWithCapture()
    }

    @Test
    fun `When onPickListCardCtaClicked has API failure THEN toast is shown`() {
        val viewModel = homeViewModelFactory(
            pickRepository = mock {
                on { pickList } doReturn stateFlowOf(testActivity)
                onBlocking { assignUser(any()) } doReturn ApiResult.Failure.GeneralFailure(message = "")
                on { hasActivePickListActivityId() } doReturn false
            },
            apsRepository = apsRepoFactoryNotStaging()
        )
        viewModel.load()
        val mockObserver = viewModel.navigationEvent.mock()
        viewModel.onPickListCardCtaClicked()
        // verify(mockObserver, never()).onChanged(null)
    }

    @Test
    fun `When onPickListCardCtaClicked has a continue order THEN navigate to pickListItems`() {
        val viewModel = homeViewModelFactory(
            pickRepository = mock {
                on { pickList } doReturn stateFlowOf(testActivity)
                on { hasActivePickListActivityId() } doReturn true
                onBlocking { assignUser(any()) } doReturn ApiResult.Success(ActivityDto())
            },
            apsRepository = apsRepoFactoryNotStaging()
        )
        viewModel.load()
        val mockNavObserver = viewModel.navigationEvent.mock()
        viewModel.onPickListCardCtaClicked()
        // TODO: Fix this test
        // verify(mockNavObserver, after(200)!!.atLeastOnce()).onChanged(
        //     NavigationEvent.Directions(
        //         HomeFragmentDirections.actionToPickListItemsFragment(
        //             activityId = "2000"
        //         )
        //     )
        // )
    }

    @Test
    fun `When no pick THEN copies are start pick`() {
        // Setup VM
        val vm = homeViewModelFactory(
            pickRepository = mock {
                on { pickList } doReturn stateFlowOf(null)
                on { hasActivePickListActivityId() } doReturn false
            },
            apsRepository = apsRepoFactoryNotStagingNotCurrentUser()
        )

        // Setup mocks
        val cardDataMock = vm.cardData.mock()
        val nextPickTitleMock = vm.nextPickTitle.mock()
        val pickingTextMock = vm.pickingText.mock()

        // Load data
        vm.load()
        runBlocking { vm.setNextPickTitle(vm.cardData.value, false) }

        // Verify
        assertThat(cardDataMock.verifyWithNullableCapture(atLeastOnce())).isNotEqualTo(null)
        assertThat(nextPickTitleMock.verifyWithCapture(atLeastOnce())).isEqualTo("Hello again!  Here’s your next pick.")
        assertThat(pickingTextMock.verifyWithCapture(atLeastOnce())).isEqualTo("Start Picking")
    }

    @Test
    fun `When an order is ready for pickup THEN show the appropriate message`() {
        // Setup VM
        val vm = homeViewModelFactory(
            pickRepository = mock {
                on { pickList } doReturn stateFlowOf(null)
                on { hasActivePickListActivityId() } doReturn false
            },
            apsRepository = apsRepoFactoryHandOff()
        )

        // Setup mocks
        val nextPickTitleMock = vm.nextPickTitle.mock()
        val waitTimeSeconds = vm.waitTimeSeconds.mock()

        // Load data
        vm.load()
        // TODO - fix runBlocking and assert later; likely is due to AcuPickConfig.isCodeUnavailable
        // runBlocking { vm.setNextPickTitle(vm.cardData.value, false) }

        // Verify
        // assertThat(nextPickTitleMock.verifyWithCapture(atLeastOnce())).isEqualTo("Hi there!  A customer is waiting.")
        // assertThat(waitTimeSeconds.verifyWithCapture(atLeastOnce())).isEqualTo(0)
    }

    @Test
    fun `Flash Timer Turns Red`() {
        inlineKoinSingle(override = true) { testSiteRepo }

        val vm = homeViewModelFactory(
            pickRepository = mock {
                on { pickList } doReturn stateFlowOf(null)
                on { hasActivePickListActivityId() } doReturn false
            },
            apsRepository = apsRepoFactoryHandOff()
        )
        vm.countdownDurationMs.value = 0
        val timerColor = vm.timerColor.mock()

        vm.load()

        vm.setTimerColor()

        assertThat(timerColor.verifyWithCapture(atLeastOnce())).isEqualTo(R.color.semiDarkRed)
    }
    // TODO() ACUPICK-2881 Fix broken tests.
    // @Test
    // fun `Flash Timer Turns Orange`() {
    //     inlineKoinSingle(override = true) { testSiteRepo }
    //     val vm = homeViewModelFactory(
    //         pickRepository = mock {
    //             on { pickList } doReturn stateFlowOf(null)
    //             on { hasActivePickListActivityId() } doReturn false
    //         },
    //         apsRepository = apsRepoFactoryHandOff()
    //     )
    //
    //     vm.countdownDurationMs.value = 730000
    //     vm.timeSinceOrderReleasedMs = LocalDateTime.now().minute.toLong() + vm.concernTimeMs
    //     val timerColor = vm.timerColor.mock()
    //
    //     vm.load()
    //
    //     vm.setTimerColor()
    //
    //     assertThat(timerColor.verifyWithCapture(atLeastOnce())).isEqualTo(R.color.darkestOrange)
    // }

    @Test
    fun `Flash Timer Turns Grey`() {
        inlineKoinSingle(override = true) { testSiteRepo }

        val vm = homeViewModelFactory(
            pickRepository = mock {
                on { pickList } doReturn stateFlowOf(null)
                on { hasActivePickListActivityId() } doReturn false
            },
            apsRepository = apsRepoFactoryHandOff()
        )

        vm.countdownDurationMs.value = 730000
        vm.timeSinceOrderReleasedMs = LocalDateTime.now().minute.toLong() - vm.concernTimeMs
        val timerColor = vm.timerColor.mock()

        vm.load()

        vm.setTimerColor()

        assertThat(timerColor.verifyWithCapture(atLeastOnce())).isEqualTo(R.color.grey_700)
    }

    @Test
    fun `When store is MFC and activity is handoff THEN show card`() {
        inlineKoinSingle(override = true) { testMfcSiteRepo }

// Setup VM
        val vm = homeViewModelFactory(
            pickRepository = mock {
                on { pickList } doReturn stateFlowOf(testActivity)
            },
            apsRepository = apsRepoFactoryHandOff()
        )

        // Setup mocks
        val cardDataMock = vm.cardData.mock()

        // Load data
        vm.load()

        // Verify
        assertThat(cardDataMock.verifyWithNullableCapture(atLeastOnce())).isNotEqualTo(null)
    }

    @Test
    fun `When store is not MFC and activity is picking THEN show card`() {
        // Setup VM
        val vm = homeViewModelFactory(
            pickRepository = mock {
                on { pickList } doReturn stateFlowOf(testActivity)
            },
            apsRepository = apsRepoFactoryNotStaging()
        )

        // Setup mocks
        val cardDataMock = vm.cardData.mock()

        // Load data
        vm.load()

        // Verify
        assertThat(cardDataMock.verifyWithNullableCapture(atLeastOnce())).isNotEqualTo(null)
    }

    @Test
    fun `When store is not MFC and activity is staging THEN show card`() {
        // Setup VM
        val vm = homeViewModelFactory(
            pickRepository = mock {
                on { pickList } doReturn stateFlowOf(testActivity)
            },
            apsRepository = apsRepoFactoryStaging()
        )

        // Setup mocks
        val cardDataMock = vm.cardData.mock()

        // Load data
        vm.load()

        // Verify
        assertThat(cardDataMock.verifyWithNullableCapture(atLeastOnce())).isNotEqualTo(null)
    }

    @Test
    fun `When store is not MFC and activity is handoff THEN show card`() {
        // Setup VM
        val vm = homeViewModelFactory(
            pickRepository = mock {
                on { pickList } doReturn stateFlowOf(testActivity)
            },
            apsRepository = apsRepoFactoryHandOff()
        )

        // Setup mocks
        val cardDataMock = vm.cardData.mock()

        // Load data
        vm.load()

        // Verify
        assertThat(cardDataMock.verifyWithNullableCapture(atLeastOnce())).isNotEqualTo(null)
    }

    // /////////////////////////////////////////////////////////////////////////
    // Test Objects and Factories
    // /////////////////////////////////////////////////////////////////////////
    fun testApplicationFactory(
        mockContext: Context = testContext,
    ): Application = mock {
        on { applicationContext } doReturn mockContext
        on { resources } doReturn testResources
        // Order is important here, most generic match first. Last match is final value.
        on { getString(ArgumentMatchers.anyInt()) } doReturn ""
        on { getString(ArgumentMatchers.anyInt(), anyVararg()) } doReturn ""
        on { getString(eq(R.string.success_item_scanned_format), ArgumentMatchers.anyString()) } doAnswer { "Bag${it.getArgument<String>(1)} scanned" }
        on { getString(R.string.wrong_item_scanned) } doReturn "Wrong Item Scanned"
        on { getString(R.string.substitute_scan_item) } doReturn "Scan substitution item"
        on { getString(R.string.scan_to_new_tote) } doReturn "Scan a new tote"
        on { getString(eq(R.string.number_short_format), ArgumentMatchers.anyString()) } doAnswer { "${it.getArgument<String>(1)} short" }
        on { getString(R.string.same_brand_diff_size) } doReturn "Same brand, different size"
        on { getString(R.string.item_details_1_item) } doReturn "1 item"
        on { getString(R.string.start_staging) } doReturn "Start staging"
        on { getString(R.string.continue_picking) } doReturn "Continue Picking"
        on { getString(R.string.start_picking) } doReturn "Start Picking"
        on { getString(R.string.hello_continue_pick) } doReturn "Hi, wanna pick up where you left off?"
        on { getString(R.string.hello_again_next_pick, "") } doReturn "Hello again!  Here’s your next pick."
        on { getString(R.string.home_customer_waiting) } doReturn "Hi there!  A customer is waiting."
    }

    private fun userRepoFactory(): UserRepository = mock {
        on { user } doReturn stateFlowOf(
            User(
                userId = "userId",
                firstName = "test",
                lastName = "last",
                sites = listOf(SiteDto("1000", true), SiteDto("2000", false)),
                selectedStoreId = "1000",
            )
        )
    }

    private fun activityAndErDtoFactory() = ActivityAndErDto(
        actId = 2000L,
        prevActivityId = 1000L,
        actType = ActivityType.PICK_PACK,
        activityNo = "123456",
        assignedTo = UserDto(firstName = "test", lastName = "last", userId = "userId"),
        bagCountRequired = true,
        batch = "9999",
        completionTime = null,
        contactFirstName = "person",
        contactLastName = "person",
        entityIds = listOf("1", "2"),
        entityReference = EntityReference(entityId = "9", entityType = "type"),
        erId = 2000L,
        exceptionQty = 0L,
        expectedCount = 10,
        expectedEndTime = null,
        fulfillment = FulfillmentAttributeDto(type = FulfillmentType.DUG),
        handshakeType = HandshakeType.SYSTEM,
        itemQty = 20.0,
        pickUpBay = "thebay",
        processedQty = 0,
        siteId = "1000",
        slotEndDate = null,
        slotStartDate = null,
        status = ActivityStatus.IN_PROGRESS,
        stopNumber = "stop",
        storageTypes = listOf(StorageType.CH, StorageType.AM),
        totalSeqNo = "seq",
        customerOrderNumber = "3048",
        reProcess = false,
        routeVanNumber = "route",
        seqNo = "seqAgain"
    )

    private fun activityAndErDtoFactoryHandOff() = activityAndErDtoFactory().copy(
        actType = ActivityType.PICKUP,
        contactFirstName = "customerFirstName",
        bagCount = 6,
        looseItemCount = 3,
        nextActExpStartTime = ZonedDateTime.now(),
        subStatus = CustomerArrivalStatus.ARRIVED,
    )

    private fun orderCountByStoreFactory() = listOf(
        OrderCountByStoreDto(
            count = 1L,
            countFulfillmentTypes = listOf(CountByFulfillmentTypes(count = 1L, fulfilmentType = FulfillmentType.DUG)),
            type = OrderByCountType.HAND_OFFS
        ),
        OrderCountByStoreDto(
            count = 1L,
            countFulfillmentTypes = listOf(CountByFulfillmentTypes(count = 1L, fulfilmentType = FulfillmentType.DUG)),
            type = OrderByCountType.PENDING_TO_STAGE
        ),
    )

    private fun apsRepoFactoryStaging(): ApsRepository = mock {
        onBlocking { getAppSummary(any(), any(), any()) } doReturn ApiResult.Success(
            AppSummaryResponseDto(
                activity = activityAndErDtoFactory().copy(actType = ActivityType.DROP_OFF),
                orderCountByStore = orderCountByStoreFactory(),
            )
        )
    }

    private fun apsRepoFactoryNotStaging(): ApsRepository = mock {
        onBlocking { getAppSummary(any(), any(), any()) } doReturn ApiResult.Success(
            AppSummaryResponseDto(
                activity = activityAndErDtoFactory(),
                orderCountByStore = orderCountByStoreFactory(),
            )
        )
    }

    private fun apsRepoFactoryNotStagingNotCurrentUser(): ApsRepository = mock {
        onBlocking { getAppSummary(any(), any(), any()) } doReturn ApiResult.Success(
            AppSummaryResponseDto(
                activity = activityAndErDtoFactory().copy(assignedTo = UserDto(firstName = "test", lastName = "last", userId = "notUserId")),
                orderCountByStore = orderCountByStoreFactory(),
            )
        )
    }

    private fun apsRepoFactoryHandOff(): ApsRepository = mock {
        onBlocking { getAppSummary(any(), any(), any()) } doReturn ApiResult.Success(
            AppSummaryResponseDto(
                activity = activityAndErDtoFactoryHandOff(),
                orderCountByStore = orderCountByStoreFactory(),
            )
        )
    }

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    private fun homeViewModelFactory(
        app: Application = testApplicationFactory(),
        pickRepository: PickRepository = testPickRepository(),
        userRepository: UserRepository = userRepoFactory(),
        apsRepository: ApsRepository = apsRepoFactoryStaging(),
        dispatcherProvider: DispatcherProvider = TestDispatcherProvider(),
        networkAvailabilityManager: NetworkAvailabilityManager = mock {
            onBlocking { isConnected } doReturn stateFlowOf(true)
        },
        completeHandoffUseCase: CompleteHandoffUseCase = mock {
            onBlocking { invoke() } doReturn Unit
            onBlocking { handOffReassigned } doReturn stateFlowOf(false)
        },
        completeHandoff1PLUseCase: CompleteHandoff1PLUseCase = mock {
            onBlocking { invoke() } doReturn Unit
            onBlocking { handOffReassigned } doReturn stateFlowOf(false)
        }
    ) = HomeViewModel(
        app = app,
        userRepo = userRepository,
        apsRepo = apsRepository,
        pickRepository = pickRepository,
        dispatcherProvider = dispatcherProvider,
        networkAvailabilityManager = networkAvailabilityManager,
        completeHandoffUseCase = completeHandoffUseCase,
        completeHandoff1PLUseCase = completeHandoff1PLUseCase,
        activityViewModel = activityViewModelFactory(app = app)
    )
}
