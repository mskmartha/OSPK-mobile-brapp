package com.albertsons.acupick.ui.arrivals.destage

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.R
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.ContainerActivityStatus
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.CustomerArrivalStatus
import com.albertsons.acupick.data.model.EntityReference
import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.FulfillmentSubType
import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.OrderIssueReasonCode
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.request.ConfirmRxPickupRequestDto
import com.albertsons.acupick.data.model.request.ScanContainerRequestDto
import com.albertsons.acupick.data.model.request.ScanContainerWrapperRequestDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ContainerActivityDto
import com.albertsons.acupick.data.model.response.ScanContActDto
import com.albertsons.acupick.data.model.response.ScanContDto
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.ServerErrorCodeDto
import com.albertsons.acupick.data.model.response.ServerErrorDto
import com.albertsons.acupick.data.model.response.VehicleInfoDto
import com.albertsons.acupick.data.model.response.getRejectedItemsByZone
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.infrastructure.utils.stateFlowOf
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.destageOrderPagerViewModelFactory
import com.albertsons.acupick.test.getPrivateProperty
import com.albertsons.acupick.test.mocks.testActivity
import com.albertsons.acupick.test.mocks.testApsRepo
import com.albertsons.acupick.test.mocks.testApsRepoAssignUserToHandoffServerFailures
import com.albertsons.acupick.test.mocks.testApsRepoFailures
import com.albertsons.acupick.test.mocks.testApsRepoServerFailures
import com.albertsons.acupick.test.mocks.testApsRepoServerFailuresInvalidUser
import com.albertsons.acupick.test.mocks.testOfflineNetworkAvailabilityManager
import com.albertsons.acupick.test.mocks.testPickRepository
import com.albertsons.acupick.test.runPrivateMethod
import com.albertsons.acupick.test.runPrivateMethodWithParams
import com.albertsons.acupick.test.setPrivateProperty
import com.albertsons.acupick.ui.arrivals.complete.HandOffArgData
import com.albertsons.acupick.ui.arrivals.complete.HandOffUI
import com.albertsons.acupick.ui.arrivals.destage.DestageOrderPagerViewModel.Companion.CONFIRM_ORDER_HOT_REMINDER_DIALOG_TAG
import com.albertsons.acupick.ui.arrivals.destage.DestageOrderPagerViewModel.Companion.HANDOFF_USER_NOT_VALID_DIALOG_TAG
import com.albertsons.acupick.ui.arrivals.destage.DestageOrderPagerViewModel.Companion.ORDER_DETAILS_LEAVE_SCREEN_DIALOG_TAG
import com.albertsons.acupick.ui.arrivals.destage.DestageOrderPagerViewModel.Companion.ORDER_ISSUE_COMPLETE_DIALOG_TAG
import com.albertsons.acupick.ui.arrivals.destage.DestageOrderPagerViewModel.Companion.ORDER_ISSUE_DIALOG_TAG
import com.albertsons.acupick.ui.arrivals.destage.DestageOrderPagerViewModel.Companion.ORDER_ISSUE_SCAN_BAGS_DIALOG_TAG
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.dialog.ORDER_DETAILS_CANCEL_ARG_DATA
import com.albertsons.acupick.ui.dialog.ORDER_ISSUE_SCAN_BAGS_ARG_DATA
import com.albertsons.acupick.ui.dialog.getHandOffAlreadyAssignedWithOrderNumberDialog
import com.albertsons.acupick.ui.manualentry.ManualEntryHandoffParams
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryHandOffBag
import com.albertsons.acupick.ui.models.BagLabel
import com.albertsons.acupick.ui.models.DestageOrderUiData
import com.albertsons.acupick.ui.models.SnackBarEvent
import com.albertsons.acupick.ui.models.ZonedBagsScannedData
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.UserFeedback
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.io.Serializable
import java.time.ZonedDateTime

class DestageOrderPagerViewModelTest : BaseTest() {

    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    @get:Rule
    val koinRule = KoinTestRule(TestModule.generateMockedTestModule())

    private val frozenBag = ZonedBagsScannedData(
        bagLabel = BagLabel(
            containerActivityDto = ContainerActivityDto(
                customerOrderNumber = "blerp",
                location = "blurp",
                containerId = "blorp",
                containerType = StorageType.FZ,
                contactLastName = "duderino"
            ),
            testActivity
        )
    )

    private val chilledBag = ZonedBagsScannedData(
        bagLabel = BagLabel(
            containerActivityDto = ContainerActivityDto(
                customerOrderNumber = "blerp",
                location = "blurp",
                containerId = "blorp",
                containerType = StorageType.CH,
                contactLastName = "duderino"
            ),
            testActivity
        )
    )

    private val ambientBag = ZonedBagsScannedData(
        bagLabel = BagLabel(
            containerActivityDto = ContainerActivityDto(
                customerOrderNumber = "blerp",
                location = "blurp",
                containerId = "blorp",
                containerType = StorageType.AM,
                contactLastName = "duderino"
            ),
            testActivity
        )
    )

    private val hotBag = ZonedBagsScannedData(
        bagLabel = BagLabel(
            containerActivityDto = ContainerActivityDto(
                customerOrderNumber = "blerp",
                location = "blurp",
                containerId = "blorp",
                containerType = StorageType.HT,
                contactLastName = "duderino"
            ),
            testActivity
        )
    )

    @Test
    fun `WHEN createOrderIssueCompleteBody with 1 bag frozen`() {
        val vm = destageOrderPagerViewModelFactory()
        var resultString = vm.runPrivateMethod("createOrderIssueCompleteBody")
        assertThat((resultString as StringIdHelper.Raw).rawString).isEmpty()

        vm.setPrivateProperty("currentOrderNumber", "blerp")
        val forceScanTempList = vm.getPrivateProperty("forceScanTempList") as ArrayList<ZonedBagsScannedData>
        forceScanTempList.add(frozenBag)

        resultString = vm.runPrivateMethod("createOrderIssueCompleteBody")
        assertThat((resultString as StringIdHelper.Raw).rawString).isEqualTo("1 bags frozen")
    }

    @Test
    fun `WHEN createOrderIssueCompleteBody with 1 bag frozen and 1 chilled`() {
        val vm = destageOrderPagerViewModelFactory()
        var resultString = vm.runPrivateMethod("createOrderIssueCompleteBody")
        assertThat((resultString as StringIdHelper.Raw).rawString).isEmpty()

        vm.setPrivateProperty("currentOrderNumber", "blerp")
        val forceScanTempList = vm.getPrivateProperty("forceScanTempList") as ArrayList<ZonedBagsScannedData>
        forceScanTempList.add(frozenBag)
        forceScanTempList.add(chilledBag)
        resultString = vm.runPrivateMethod("createOrderIssueCompleteBody")
        assertThat((resultString as StringIdHelper.Raw).rawString).isEqualTo("2 bags frozen and chilled")
    }

    @Test
    fun `WHEN createOrderIssueCompleteBody with 1 bag frozen and 1 chilled and 1 ambient`() {
        val vm = destageOrderPagerViewModelFactory()
        var resultString = vm.runPrivateMethod("createOrderIssueCompleteBody")
        assertThat((resultString as StringIdHelper.Raw).rawString).isEmpty()

        vm.setPrivateProperty("currentOrderNumber", "blerp")
        val forceScanTempList = vm.getPrivateProperty("forceScanTempList") as ArrayList<ZonedBagsScannedData>
        forceScanTempList.add(frozenBag)
        forceScanTempList.add(chilledBag)
        forceScanTempList.add(ambientBag)
        resultString = vm.runPrivateMethod("createOrderIssueCompleteBody")
        assertThat((resultString as StringIdHelper.Raw).rawString).isEqualTo("3 bags frozen, chilled, and ambient")
    }

    @Test
    fun `WHEN createOrderIssueCompleteBody with 1 bag frozen and 1 ambient`() {
        val vm = destageOrderPagerViewModelFactory()
        var resultString = vm.runPrivateMethod("createOrderIssueCompleteBody")
        assertThat((resultString as StringIdHelper.Raw).rawString).isEmpty()

        vm.setPrivateProperty("currentOrderNumber", "blerp")
        val forceScanTempList = vm.getPrivateProperty("forceScanTempList") as ArrayList<ZonedBagsScannedData>
        forceScanTempList.add(frozenBag)
        forceScanTempList.add(ambientBag)
        resultString = vm.runPrivateMethod("createOrderIssueCompleteBody")
        assertThat((resultString as StringIdHelper.Raw).rawString).isEqualTo("2 bags frozen and ambient")
    }

    @Test
    fun `WHEN createOrderIssueCompleteBody with 1 bag chilled`() {
        val vm = destageOrderPagerViewModelFactory()
        var resultString = vm.runPrivateMethod("createOrderIssueCompleteBody")
        assertThat((resultString as StringIdHelper.Raw).rawString).isEmpty()

        vm.setPrivateProperty("currentOrderNumber", "blerp")
        val forceScanTempList = vm.getPrivateProperty("forceScanTempList") as ArrayList<ZonedBagsScannedData>
        forceScanTempList.clear()
        forceScanTempList.add(chilledBag)
        resultString = vm.runPrivateMethod("createOrderIssueCompleteBody")
        assertThat((resultString as StringIdHelper.Raw).rawString).isEqualTo("1 bags chilled")
    }

    @Test
    fun `WHEN createOrderIssueCompleteBody with 1 bag chilled and 1 bag ambient`() {
        val vm = destageOrderPagerViewModelFactory()
        var resultString = vm.runPrivateMethod("createOrderIssueCompleteBody")
        assertThat((resultString as StringIdHelper.Raw).rawString).isEmpty()

        vm.setPrivateProperty("currentOrderNumber", "blerp")
        val forceScanTempList = vm.getPrivateProperty("forceScanTempList") as ArrayList<ZonedBagsScannedData>
        forceScanTempList.add(chilledBag)
        forceScanTempList.add(ambientBag)
        resultString = vm.runPrivateMethod("createOrderIssueCompleteBody")
        assertThat((resultString as StringIdHelper.Raw).rawString).isEqualTo("2 bags chilled and ambient")
    }

    @Test
    fun `WHEN createOrderIssueCompleteBody is activated with 1 hot bag, verify resultString is 1 bags hot`() {
        val vm = destageOrderPagerViewModelFactory()
        var resultString = vm.runPrivateMethod("createOrderIssueCompleteBody")
        assertThat((resultString as StringIdHelper.Raw).rawString).isEmpty()

        vm.setPrivateProperty("currentOrderNumber", "blerp")
        val forceScanTempList = vm.getPrivateProperty("forceScanTempList") as ArrayList<ZonedBagsScannedData>
        forceScanTempList.add(hotBag)
        resultString = vm.runPrivateMethod("createOrderIssueCompleteBody")
        assertThat((resultString as StringIdHelper.Raw).rawString).isEqualTo("1 bags hot")
    }

    // /////////////////////////////////////////////////////////////////////////
    // Dialogs
    // /////////////////////////////////////////////////////////////////////////
    @Test
    fun `WHEN showMaxAssignedDialog verify inlineDialogEvent`() {
        val vm = destageOrderPagerViewModelFactory()
        val mockInlineDialogEvent = vm.inlineDialogEvent.mock()
        vm.showMaxAssignedDialog()
        verify(mockInlineDialogEvent).onChanged(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.max_assigned_title),
                    body = StringIdHelper.Id(R.string.max_assigned_body),
                    positiveButtonText = StringIdHelper.Id(R.string.ok)
                ),
                tag = DestageOrderPagerViewModel.MAX_ORDERS_ASSIGNED
            )
        )
    }

    @Test
    fun `WHEN showOrderIssueCompleteDialog verify inlineDialogEvent`() {
        val vm = destageOrderPagerViewModelFactory()
        val inlineDialogEventMock = vm.inlineDialogEvent.mock()
        vm.runPrivateMethod("showOrderIssueCompleteDialog")
        verify(inlineDialogEventMock).onChanged(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.complete_order_issue_dialog_title),
                    body = StringIdHelper.Raw(""),
                    positiveButtonText = StringIdHelper.Id(R.string.complete),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel)
                ),
                tag = ORDER_ISSUE_COMPLETE_DIALOG_TAG
            )
        )
    }

    @Test
    fun `WHEN showHandOffAlreadyAssignedDialog verify inlineDialogEvent`() {
        val vm = destageOrderPagerViewModelFactory()
        val inlineDialogEventMock = vm.inlineDialogEvent.mock()
        // vm.runPrivateMethod("showHandOffAlreadyAssignedDialog")
        vm.runPrivateMethodWithParams("showHandOffAlreadyAssignedDialog", listOf("123456"))
        verify(inlineDialogEventMock).onChanged(
            CustomDialogArgDataAndTag(
                data = getHandOffAlreadyAssignedWithOrderNumberDialog("[123456]"),
                tag = HANDOFF_USER_NOT_VALID_DIALOG_TAG,
            )
        )
    }

    @Test
    fun `WHEN showOrderIssueScanBagDialog verify inlineDialogEvent`() {
        val vm = destageOrderPagerViewModelFactory()
        val inlineDialogEventMock = vm.inlineDialogEvent.mock()
        vm.runPrivateMethod("showOrderIssueScanBagDialog")
        verify(inlineDialogEventMock).onChanged(
            CustomDialogArgDataAndTag(
                data = ORDER_ISSUE_SCAN_BAGS_ARG_DATA,
                tag = ORDER_ISSUE_SCAN_BAGS_DIALOG_TAG
            )
        )
    }

    @Test
    fun `WHEN showExitDialog verify inlineDialogEvent`() {
        val vm = destageOrderPagerViewModelFactory()
        val inlineDialogEventMock = vm.inlineDialogEvent.mock()
        vm.handleExitButton()
        verify(inlineDialogEventMock).onChanged(
            CustomDialogArgDataAndTag(
                data = ORDER_DETAILS_CANCEL_ARG_DATA,
                tag = ORDER_DETAILS_LEAVE_SCREEN_DIALOG_TAG
            )
        )
    }

    // /////////////////////////////////////////////////////////////////////////
    // Navigation
    // /////////////////////////////////////////////////////////////////////////
    @Test
    fun `WHEN navigateToUpdateCustomerAddFragment verify navigationEvent`() {
        val vm = destageOrderPagerViewModelFactory()
        val navigationEventMock = vm.navigationEvent.mock()
        vm.runPrivateMethodWithParams("navigateToUpdateCustomerAddFragment", 1)
        runBlocking {
            delay(550)
            verify(navigationEventMock).onChanged(
                NavigationEvent.Directions(
                    NavGraphDirections.actionToUpdateCustomerAddFragment(1)
                )
            )
        }
    }

    @Ignore
    @Test
    fun `WHEN navigateToManualEntry verify navigationEvent`() {
        val vm = destageOrderPagerViewModelFactory()
        val navigationEventMock = vm.navigationEvent.mock()
        vm.runPrivateMethod("navigateToManualEntry")
        verify(navigationEventMock).onChanged(
            NavigationEvent.Directions(
                NavGraphDirections.actionToManualEntryHandOffFragment(
                    ManualEntryHandoffParams(
                        bagLabels = arrayListOf(),
                        customerOrderNumber = "",
                        activityId = "",
                        isMutliSource = false,
                        shortOrderId = null,
                        customerName = null
                    )
                )
            )
        )
    }

    @Test
    fun `WHEN navigateToHandOff verify navigationEvent`() {
        val vm = destageOrderPagerViewModelFactory()
        vm.isRxComplete.value = true
        val navigationEventMock = vm.navigationEvent.mock()
        val failedActivityList = listOf<ActivityDto>()
        val data = ConfirmRxPickupRequestDto()
        val vehicleInfo: HashMap<Long?, ScanContDto> = hashMapOf()
        vm.runPrivateMethodWithParams("navigateToHandOff", failedActivityList, data)
        verify(navigationEventMock).onChanged(
            NavigationEvent.Directions(
                NavGraphDirections.actionToHandOffFragment(
                    HandOffArgData(vm.runPrivateMethodWithParams("createHandOffUiList", failedActivityList, data, vehicleInfo) as List<HandOffUI>)
                )
            )
        )
    }

    @Test
    fun `WHEN exitHandOff with cancelHandoffs success verify navigationEvent`() {
        inlineKoinSingle(override = true) {
            testApsRepo
        }
        val vm = destageOrderPagerViewModelFactory()
        val navigationEventMock = vm.navigationEvent.mock()
        val activityDtoArray = vm.getPrivateProperty("activityDtoArray") as ArrayList<ActivityDto>
        activityDtoArray.add(ActivityDto())
        vm.setPrivateProperty("activityDtoArray", activityDtoArray)
        vm.runPrivateMethod("exitHandOff")
        verify(navigationEventMock).onChanged(
            NavigationEvent.Up
        )
    }

    @Test
    fun `WHEN exitHandOff with cancelHandoffs failure verify navigationEvent and snackBarEvent`() {
        inlineKoinSingle(override = true) {
            testApsRepoFailures
        }
        val vm = destageOrderPagerViewModelFactory()
        val navigationEventMock = vm.navigationEvent.mock()
        val snackbarMock = vm.acupickSnackEvent.mock()
        val activityDtoArray = vm.getPrivateProperty("activityDtoArray") as ArrayList<ActivityDto>
        activityDtoArray.add(ActivityDto())
        vm.setPrivateProperty("activityDtoArray", activityDtoArray)
        vm.runPrivateMethod("exitHandOff")
        verify(navigationEventMock).onChanged(
            NavigationEvent.Up
        )
        assertThat(snackbarMock.verifyWithNullableCapture(atLeastOnce()))
    }

    @Test
    fun `WHEN assignToMe changes to failure value verify server error failure`() {
        inlineKoinSingle(override = true) { testApsRepoAssignUserToHandoffServerFailures }
        val vm = destageOrderPagerViewModelFactory()
        val apiErrorEventMock = vm.apiErrorEvent.mock()
        val failureApiResult: ApiResult.Failure = ApiResult.Failure.Server(error = ServerErrorDto())
        vm.assignToMe(1234L)
        assertThat(apiErrorEventMock.verifyWithCapture(atLeastOnce())).isEqualTo(Pair("", failureApiResult))
    }

    // DUG interjection using new snackbar directly here so that livedata value would not be getting change
    @Ignore
    @Test
    fun `WHEN assignToMe changes to success value verify snackbar text sent`() {
        inlineKoinSingle(override = true) { testPickRepository() }
        val vm = destageOrderPagerViewModelFactory()
        val markedArrivedsnackBarLiveEventMock = vm.markedArrivedSnackBarEvent.mock()
        vm.assignToMe(3456L)
        verify(markedArrivedsnackBarLiveEventMock).onChanged(
            "B. Glerp has been added to your handoff"
        )
    }

    @Test
    fun `WHEN assignBagsToOrder without activityDto verify empty tabs and completelist`() {
        val vm = destageOrderPagerViewModelFactory()
        val tabsMock = vm.tabs.mock()
        val completeListMock = vm.isCompleteList.mock()
        val testDtoList: MutableList<ActivityDto> = mutableListOf()
        vm.runPrivateMethodWithParams("assignBagsToOrder", testDtoList)
        assertThat(tabsMock.verifyWithCapture(atLeastOnce()).isEmpty()).isTrue()
        assertThat(completeListMock.verifyWithCapture(atLeastOnce()).isEmpty()).isTrue()
    }

    @Test
    fun `WHEN assignBagsToOrder with activityDto and non-complete bag verify non-empty tabs and empty completelist`() {
        val vm = destageOrderPagerViewModelFactory()
        val zonedBagsScannedData = vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>
        val testDtoList: MutableList<ActivityDto> = mutableListOf()
        val completeListMock = vm.isCompleteList.mock()
        val tabsMock = vm.tabs.mock()
        testDtoList.add(
            ActivityDto(
                customerOrderNumber = "OrderNum1",
                contactFirstName = "Blerp",
                contactLastName = "Glerp"
            )
        )
        zonedBagsScannedData.add(
            ZonedBagsScannedData(
                bagLabel = BagLabel(
                    containerActivityDto = ContainerActivityDto(
                        customerOrderNumber = "OrderNum1"
                    ),
                    testActivity
                )
            )
        )
        vm.runPrivateMethodWithParams("assignBagsToOrder", testDtoList)
        assertThat(tabsMock.verifyWithCapture(atLeastOnce()).isNotEmpty()).isTrue()
        verify(completeListMock).onChanged(
            listOf(OrderCompletionState(customerOrderNumber = "OrderNum1", isComplete = false))
        )
    }

    @Test
    fun `WHEN assignBagsToOrder with 2 activityDto and complete bags verify size 2 tabs and non-empty completelist`() {
        val vm = destageOrderPagerViewModelFactory()
        val zonedBagsScannedData = vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>
        val testDtoList: MutableList<ActivityDto> = mutableListOf()
        val completeListMock = vm.isCompleteList.mock()
        val tabsMock = vm.tabs.mock()
        testDtoList.add(
            ActivityDto(
                customerOrderNumber = "OrderNum2",
                contactFirstName = "Blerps",
                contactLastName = "Glerps"
            )
        )
        testDtoList.add(
            ActivityDto(
                customerOrderNumber = "OrderNum1",
                contactFirstName = "Blerp",
                contactLastName = "Glerp"
            )
        )
        zonedBagsScannedData.add(
            ZonedBagsScannedData(
                bagLabel = BagLabel(
                    containerActivityDto = ContainerActivityDto(
                        customerOrderNumber = "OrderNum1"
                    ),
                    testActivity
                )
            )
        )
        val completeBag = ZonedBagsScannedData(
            bagLabel = BagLabel(
                containerActivityDto = ContainerActivityDto(
                    customerOrderNumber = "OrderNum2"
                ),
                testActivity
            )
        )
        completeBag.currentBagsScanned = 1
        zonedBagsScannedData.first().currentBagsScanned = 1
        zonedBagsScannedData.add(completeBag)
        vm.runPrivateMethodWithParams("assignBagsToOrder", testDtoList)
        assertThat(tabsMock.verifyWithCapture(atLeastOnce()).size == 2).isTrue()
        assertThat(completeListMock.verifyWithCapture(atLeastOnce()).size == 2).isTrue()
    }

    @Test
    fun `WHEN loadDetails online verify DTO size is 1`() {
        inlineKoinSingle(override = true) { testApsRepo }
        val vm = destageOrderPagerViewModelFactory()
        val activityDtoArray = vm.getPrivateProperty("activityDtoArray") as ArrayList<ActivityDto>
        vm.loadDetails(listOf(1234L))
        assertThat(activityDtoArray.size == 1).isTrue()
    }

    @Test
    fun `WHEN loadDetails offline verify DTO size is empty and error triggered`() {
        val networkAvailabilityManagerMock: NetworkAvailabilityManager = mock {
            onBlocking { isConnected } doReturn stateFlowOf(false)
            onBlocking {
                triggerOfflineError { }
            }.doAnswer {
            }
        }
        inlineKoinSingle(override = true) { networkAvailabilityManagerMock }
        val vm = destageOrderPagerViewModelFactory()
        val activityDtoArray = vm.getPrivateProperty("activityDtoArray") as ArrayList<ActivityDto>
        vm.loadDetails(listOf(1234L))
        runBlocking {
            verify(networkAvailabilityManagerMock, times(1)).triggerOfflineError(any())
        }
        assertThat(activityDtoArray.isEmpty()).isTrue()
    }

    @Test
    fun `WHEN handleExitButton with complete bags verify dialog`() {
        val vm = destageOrderPagerViewModelFactory()
        val inlineDialogEventMock = vm.inlineDialogEvent.mock()
        val zonedBagsScannedData = vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>
        val testDtoList: MutableList<ActivityDto> = mutableListOf()
        testDtoList.add(
            ActivityDto(
                customerOrderNumber = "OrderNum1",
                contactFirstName = "Blerp",
                contactLastName = "Glerp"
            )
        )
        val completeBag = ZonedBagsScannedData(
            bagLabel = BagLabel(
                containerActivityDto = ContainerActivityDto(
                    customerOrderNumber = "OrderNum1"
                ),
                testActivity
            )
        )
        completeBag.currentBagsScanned = 1
        zonedBagsScannedData.add(completeBag)
        vm.handleExitButton()
        assertThat(inlineDialogEventMock.verifyWithCapture(atLeastOnce())).isEqualTo(
            CustomDialogArgDataAndTag(
                data = ORDER_DETAILS_CANCEL_ARG_DATA,
                tag = ORDER_DETAILS_LEAVE_SCREEN_DIALOG_TAG
            )
        )
    }

    @Ignore
    @Test
    fun `WHEN onManualCtaClicked verify navigationEvent`() {
        val vm = destageOrderPagerViewModelFactory()
        val navigationEventMock = vm.navigationEvent.mock()
        vm.onManualCtaClicked()
        verify((navigationEventMock)).onChanged(
            NavigationEvent.Directions(
                NavGraphDirections.actionToManualEntryHandOffFragment(
                    ManualEntryHandoffParams(
                        bagLabels = arrayListOf(),
                        customerOrderNumber = "",
                        activityId = "",
                        isMutliSource = false,
                        shortOrderId = null,
                        customerName = null
                    )
                )
            )
        )
    }

    @Test
    fun destageOrderPagerViewModelTest_createScanContainerWrapperRequestDto() {
        val vm = destageOrderPagerViewModelFactory()
        val zonedBagsScannedData = vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>
        zonedBagsScannedData.add(
            ZonedBagsScannedData(
                bagLabel = BagLabel(
                    containerActivityDto = ContainerActivityDto(
                        customerOrderNumber = "TestOrder1",
                        containerId = "ContainerId1"
                    ),
                    testActivity
                )
            )
        )
        val activityDtoArray = vm.getPrivateProperty("activityDtoArray") as ArrayList<ActivityDto>
        activityDtoArray.add(
            ActivityDto(
                customerOrderNumber = "TestOrder1",
                actId = 1234L
            )
        )
        val wrapperRequestDto = vm.runPrivateMethodWithParams("createScanContainerWrapperRequestDto", "TestOrder1") as ScanContainerWrapperRequestDto
        assertThat(wrapperRequestDto.actId).isEqualTo(1234L)
        assertThat(wrapperRequestDto.containerReqs?.isNotEmpty()).isTrue()
        assertThat(wrapperRequestDto.containerReqs?.first()).isEqualTo(
            ScanContainerRequestDto(
                containerId = "ContainerId1",
                overrideAttemptToRemove = true,
                overrideRemoved = true,
                overrideScanUser = true,
                stagingLocation = null,
                startIfNotStarted = true,
                isLoose = false
            )
        )
    }

    @Test
    fun `WHEN createHandOffUiList run empty verify handOffUi empty`() {
        val vm = destageOrderPagerViewModelFactory()
        val activityList: MutableList<ActivityDto> = mutableListOf()
        val data = ConfirmRxPickupRequestDto()
        val vehicleInfo: HashMap<Long?, ScanContDto> = hashMapOf()
        val handoffUiList = vm.runPrivateMethodWithParams("createHandOffUiList", activityList, data, vehicleInfo) as List<HandOffUI>
        assertThat(handoffUiList.isEmpty())
    }

    @Test
    fun `WHEN createHandOffUiList run non-empty verify handOffUi non-empty`() {
        val vm = destageOrderPagerViewModelFactory()
        val activityList: MutableList<ActivityDto> = mutableListOf()
        activityList.add(
            ActivityDto(
                customerOrderNumber = "TestOrder1",
                actId = 1234L
            )
        )
        val activityDtoArray = vm.getPrivateProperty("activityDtoArray") as ArrayList<ActivityDto>
        activityDtoArray.add(
            ActivityDto(
                customerOrderNumber = "TestOrder1",
                contactFirstName = "Blerp",
                contactLastName = "Glerp",
                actId = 1234L
            )
        )
        activityDtoArray.add(
            ActivityDto(
                customerOrderNumber = "TestOrder2",
                contactFirstName = "Klerp",
                contactLastName = "Flerp",
                actId = 2345L
            )
        )
        val vehicleInfo: HashMap<Long?, ScanContDto> = hashMapOf()
        vehicleInfo.put(
            1234L,
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
                vehicleInfo = VehicleInfoDto(vehicleDetail = "Red color", parkedSpot = "A123")
            )
        )
        val data = ConfirmRxPickupRequestDto()
        val handoffUiList = vm.runPrivateMethodWithParams("createHandOffUiList", activityList, data, vehicleInfo) as List<HandOffUI>
        assertThat(handoffUiList.isNotEmpty())
        assertThat(handoffUiList.first().orderNumber).isEqualTo("TestOrder1")
        assertThat(handoffUiList.first().tabLabel).isEqualTo("B. Glerp")
        assertThat(handoffUiList.first().confirmOrderText).isEqualTo("der1")
        assertThat(handoffUiList.first().scanContainerWrapperRequestDto).isNotNull()

        assertThat(handoffUiList[1].orderNumber).isEqualTo("TestOrder2")
        assertThat(handoffUiList[1].tabLabel).isEqualTo("K. Flerp")
        assertThat(handoffUiList[1].confirmOrderText).isEqualTo("der2")
        assertThat(handoffUiList[1].scanContainerWrapperRequestDto).isNull()

        assertThat(handoffUiList.first().vehicleInformation).isEqualTo("Red color")
        assertThat(handoffUiList.first().spotNumber).isEqualTo("A123")
    }

    @Test
    fun `WHEN onConfirmCtaClick success verify navigationEvent`() {
        inlineKoinSingle(override = true) { testApsRepo }
        var vm = destageOrderPagerViewModelFactory()
        vm.isRxComplete.value = true
        var navigationEventMock = vm.navigationEvent.mock()
        var activityDtoArray = vm.getPrivateProperty("activityDtoArray") as ArrayList<ActivityDto>
        val activityDto = ActivityDto(
            customerOrderNumber = "TestOrder1",
            contactFirstName = "Blerp",
            contactLastName = "Glerp",
            actId = 1234L,
            status = ActivityStatus.IN_PROGRESS
        )
        activityDtoArray.add(activityDto)
        vm.onConfirmCtaClick()
        navigationEventMock.verifyWithCapture(atLeastOnce())
    }

    @Test
    fun `WHEN onConfirmCtaClick failure verify navigationEvent`() {
        inlineKoinSingle(override = true) { testApsRepoFailures }
        val vm = destageOrderPagerViewModelFactory()
        val navigationEventMock = vm.navigationEvent.mock()
        vm.isRxComplete.value = true
        val activityDtoArray = vm.getPrivateProperty("activityDtoArray") as ArrayList<ActivityDto>
        val activityDto = ActivityDto(
            customerOrderNumber = "TestOrder1",
            contactFirstName = "Blerp",
            contactLastName = "Glerp",
            actId = 1234L,
            status = ActivityStatus.IN_PROGRESS
        )
        activityDtoArray.add(activityDto)
        vm.onConfirmCtaClick()
        navigationEventMock.verifyWithCapture(atLeastOnce())
    }

    @Test
    fun `WHEN onConfirmCtaClick failure invalid user verify navigationEvent`() {
        // val activityVMMock: MainActivityViewModel = activityViewModelFactory()
        // inlineKoinSingle(override = true) { activityVMMock }
        inlineKoinSingle(override = true) { testApsRepoServerFailuresInvalidUser }
        val vm = destageOrderPagerViewModelFactory()
        vm.isRxComplete.value = true
        val navigationEventMock = vm.navigationEvent.mock()
        val inlineDialogEventMock = vm.inlineDialogEvent.mock()
        val activityDtoArray = vm.getPrivateProperty("activityDtoArray") as ArrayList<ActivityDto>
        val activityDto = ActivityDto(
            customerOrderNumber = "TestOrder1",
            contactFirstName = "Blerp",
            contactLastName = "Glerp",
            status = ActivityStatus.IN_PROGRESS
        )
        activityDtoArray.add(activityDto)
        vm.onConfirmCtaClick()
        navigationEventMock.verifyWithCapture(atLeastOnce())
    }

    @Test
    fun `WHEN onConfirmCtaClick failure invalid user verify dialog`() {
        inlineKoinSingle(override = true) { testApsRepoServerFailuresInvalidUser }
        val vm = destageOrderPagerViewModelFactory()
        vm.isRxComplete.value = true
        val navigationEventMock = vm.navigationEvent.mock()
        val inlineDialogEventMock = vm.inlineDialogEvent.mock()
        val activityDtoArray = vm.getPrivateProperty("activityDtoArray") as ArrayList<ActivityDto>
        val activityDto = ActivityDto(
            customerOrderNumber = "123456",
            contactFirstName = "Blerp",
            contactLastName = "Glerp",
            actId = 1234L,
            status = ActivityStatus.IN_PROGRESS
        )
        activityDtoArray.add(activityDto)
        vm.onConfirmCtaClick()
        verify(inlineDialogEventMock).onChanged(
            CustomDialogArgDataAndTag(
                data = getHandOffAlreadyAssignedWithOrderNumberDialog("[123456]"),
                tag = HANDOFF_USER_NOT_VALID_DIALOG_TAG,
            )
        )
    }

    @Test
    fun `WHEN onConfirmCtaClick failure server verify navigationEvent and server error`() {
        inlineKoinSingle(override = true) { testApsRepoServerFailures }
        val vm = destageOrderPagerViewModelFactory()
        vm.isRxComplete.value = true
        val navigationEventMock = vm.navigationEvent.mock()
        val apiErrorEventMock = vm.apiErrorEvent.mock()
        val failureApiResult: ApiResult.Failure = ApiResult.Failure.Server(
            error =
            ServerErrorDto(errorCode = ServerErrorCodeDto(rawValue = 1, resolvedType = ServerErrorCode.UNKNOWN_SERVER_ERROR_CODE))
        )
        val activityDtoArray = vm.getPrivateProperty("activityDtoArray") as ArrayList<ActivityDto>
        val activityDto = ActivityDto(
            customerOrderNumber = "TestOrder1",
            contactFirstName = "Blerp",
            contactLastName = "Glerp",
            actId = 1234L,
            status = ActivityStatus.IN_PROGRESS
        )
        activityDtoArray.add(activityDto)
        vm.onConfirmCtaClick()
        navigationEventMock.verifyWithCapture(atLeastOnce())
        assertThat(apiErrorEventMock.verifyWithCapture(atLeastOnce())).isEqualTo(Pair("", failureApiResult))
    }

    @Test
    fun `WHEN onConfirmCtaClick failure offline verify navigationEvent`() {
        inlineKoinSingle(override = true) { testOfflineNetworkAvailabilityManager }
        val vm = destageOrderPagerViewModelFactory()
        vm.isRxComplete.value = true
        val navigationEventMock = vm.navigationEvent.mock()

        vm.onConfirmCtaClick()
        verify(navigationEventMock).onChanged(
            NavigationEvent.Directions(
                NavGraphDirections.actionToHandOffFragment(
                    HandOffArgData(listOf())
                )
            )
        )
    }

    @Test
    fun `WHEN cancelOrderIssue confirm forceScanTemp and orderIssueReasonCodeTemp are empty and null`() {
        val vm = destageOrderPagerViewModelFactory()
        var forceScanTempList = vm.getPrivateProperty("forceScanTempList") as ArrayList<ZonedBagsScannedData>
        vm.setPrivateProperty("orderIssueReasonCodeTemp", OrderIssueReasonCode.BAGS_MISSING)
        forceScanTempList.add(ZonedBagsScannedData(bagLabel = BagLabel(ContainerActivityDto(), testActivity)))

        vm.runPrivateMethod("cancelOrderIssue")
        forceScanTempList = vm.getPrivateProperty("forceScanTempList") as ArrayList<ZonedBagsScannedData>
        assertThat(forceScanTempList.isEmpty()).isTrue()
        assertThat(vm.getPrivateProperty("orderIssueReasonCodeTemp") as OrderIssueReasonCode?).isNull()
    }

    @Test
    fun `WHEN completeOrderIssue verify live data calls and zoneBagsScannedData updated`() {
        inlineKoinSingle(override = true) { mock<UserFeedback> {} }
        val vm = destageOrderPagerViewModelFactory()
        val zonedBagsScannedData = vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>
        val clearOrderIssueMock = vm.clearOrderIssue.mock()
        val forceScanTempList = vm.getPrivateProperty("forceScanTempList") as ArrayList<ZonedBagsScannedData>
        val forceScanList = vm.getPrivateProperty("forceScanList") as ArrayList<ZonedBagsScannedData>

        val bagData = ZonedBagsScannedData(bagLabel = BagLabel(containerActivityDto = ContainerActivityDto(customerOrderNumber = "TestOrder1", containerId = "ContainerId1"), testActivity))
        zonedBagsScannedData.add(bagData)
        forceScanList.add(bagData)
        forceScanTempList.add(bagData)

        vm.completeOrderIssue()
        assertThat((vm.getPrivateProperty("orderIssueReasonMap") as? MutableMap<String, OrderIssueReasonCode?>)?.isNotEmpty()).isTrue()
        clearOrderIssueMock.verifyWithNullableCapture(atLeastOnce())
        assertThat((vm.getPrivateProperty("orderIssueReasonCodeTemp") as OrderIssueReasonCode?)).isNull()
        assertThat(zonedBagsScannedData.first().bagsForcedScanned == 1).isTrue()
    }

    @Test
    fun `WHEN extractBagsForForceScan verify live data updates and dialog`() {
        val vm = destageOrderPagerViewModelFactory()
        val inlineDialogEventMock = vm.inlineDialogEvent.mock()
        val zonedBagsScannedData = vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>
        val forceScanTempList = vm.getPrivateProperty("forceScanTempList") as ArrayList<ZonedBagsScannedData>

        val bagData = ZonedBagsScannedData(
            bagLabel = BagLabel(
                containerActivityDto = ContainerActivityDto(
                    customerOrderNumber = "TestOrder1",
                    containerId = "ContainerId1"
                ),
                testActivity
            )
        )
        zonedBagsScannedData.add(bagData)
        vm.currentOrderNumber = "TestOrder1"
        vm.runPrivateMethod("extractBagsForForceScan")
        assertThat(forceScanTempList.isNotEmpty()).isTrue()
        verify(inlineDialogEventMock).onChanged(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.complete_order_issue_dialog_title),
                    body = StringIdHelper.Raw(""),
                    positiveButtonText = StringIdHelper.Id(R.string.complete),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel)
                ),
                tag = ORDER_ISSUE_COMPLETE_DIALOG_TAG
            )
        )
    }

    @Test
    fun `WHEN setOrderIssue verify live data updated`() {
        val vm = destageOrderPagerViewModelFactory()
        vm.setOrderIssue(OrderIssueReasonCode.BAG_LABELS)
        assertThat(vm.getPrivateProperty("orderIssueReasonCodeTemp") as OrderIssueReasonCode?).isEqualTo(OrderIssueReasonCode.BAG_LABELS)
    }

    @Test
    fun `WHEN showBagPrompt empty verify scan_bags_prompt`() {
        val vm = destageOrderPagerViewModelFactory()
        val snackbarEventMock = vm.snackbarEvent.mock()
        vm.showIndefinitePrompt()
        assertThat(snackbarEventMock.verifyWithNullableCapture(atLeastOnce())?.prompt).isEqualTo(StringIdHelper.Id(R.string.scan_bags_prompt))
    }

    @Test
    fun `WHEN showBagPrompt has bag verify scan_a_new_bag`() {
        val vm = destageOrderPagerViewModelFactory()
        val snackbarEventMock = vm.snackbarEvent.mock()
        val zonedBagsScannedData = vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>
        val bagData = ZonedBagsScannedData(
            bagLabel = BagLabel(
                containerActivityDto = ContainerActivityDto(
                    customerOrderNumber = "TestOrder1",
                    containerId = "ContainerId1"
                ),
                testActivity
            )
        )
        bagData.currentBagsScanned = 1
        zonedBagsScannedData.add(bagData)
        vm.showIndefinitePrompt()
        assertThat(snackbarEventMock.verifyWithNullableCapture(atLeastOnce())?.prompt).isEqualTo(StringIdHelper.Id(R.string.scan_a_new_bag))
    }

    @Test @Ignore
    fun `WHEN showBagPrompt complete verify clears snackbar`() {
        val vm = destageOrderPagerViewModelFactory()
        val zonedBagsScannedData = vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>
        val bagData = ZonedBagsScannedData(
            bagLabel = BagLabel(
                containerActivityDto = ContainerActivityDto(
                    customerOrderNumber = "TestOrder1",
                    containerId = "ContainerId1"
                ),
                testActivity
            )
        )
        bagData.currentBagsScanned = 1
        zonedBagsScannedData.add(bagData)
        vm.currentOrderNumber = "TestOrder1"
        vm.isComplete.postValue(true)
        vm.isRxComplete.postValue(true)
        val snackBarEvent: SnackBarEvent<Long> = SnackBarEvent(prompt = StringIdHelper.Raw("blewp"))
        val dismissMock = snackBarEvent.dismissLiveEvent?.mock()
        vm.snackBarEventList.add(snackBarEvent)
        vm.showIndefinitePrompt()
        verify(dismissMock)?.onChanged(Unit)
    }

    @Test
    fun `WHEN showWrongScan verify snackbar string matches`() {
        var hitSoundFeedback = false
        val userFeedbackMock: UserFeedback = mock {
            onBlocking {
                setFailureScannedSoundAndHaptic()
            } doAnswer {
                hitSoundFeedback = true
            }
        }
        inlineKoinSingle(override = true) { userFeedbackMock }
        val vm = destageOrderPagerViewModelFactory()
        val snackBarLiveEventMock = vm.acupickSnackEvent.mock()
        vm.runPrivateMethodWithParams("showWrongScan", "testString")
        assertThat(hitSoundFeedback).isTrue()
        assertThat((snackBarLiveEventMock.verifyWithNullableCapture(atLeastOnce())?.message as StringIdHelper.Raw).rawString == "testString")
    }

    @Test @Ignore
    fun confirmOrderPagerViewModelTest_showScanSuccess() {
        var hitSoundFeedback = false
        val userFeedbackMock: UserFeedback = mock {
            onBlocking {
                setSuccessScannedSoundAndHaptic()
            } doAnswer {
                hitSoundFeedback = true
            }
        }
        inlineKoinSingle(override = true) { userFeedbackMock }
        val vm = destageOrderPagerViewModelFactory()
        val snackBarLiveEventMock = vm.snackBarLiveEvent.mock()
        val bag = BarcodeType.Bag(rawBarcode = "blerp", bagOrToteId = "klerp", customerOrderNumber = "flerp", displayToteId = "plerp")
        val bagLabel = BagLabel(ContainerActivityDto(customerOrderNumber = "flerp", location = "vlerp"), testActivity)
        (vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>).add(
            ZonedBagsScannedData(
                bagLabel = bagLabel
            )
        )
        vm.runPrivateMethodWithParams("handleScannedBagOrMfcTote", bag, bagLabel)
        assertThat(hitSoundFeedback).isTrue()
        assertThat((snackBarLiveEventMock.verifyWithNullableCapture(atLeastOnce())?.prompt as StringIdHelper.Raw).rawString == "Bag rp scanned out of Zone vlerp")
    }

    @Test
    fun `WHEN handleScannedBagOrMfcTote with mfcTote already scanned failure toast message and sound`() {
        var hitBadSoundFeedback = false
        val userFeedbackMock: UserFeedback = mock {
            onBlocking {
                setFailureScannedSoundAndHaptic()
            } doAnswer {
                hitBadSoundFeedback = true
            }
        }
        inlineKoinSingle(override = true) { userFeedbackMock }
        val vm = destageOrderPagerViewModelFactory()
        val snackBarLiveEventMock = vm.acupickSnackEvent.mock()
        val bagLabel = BagLabel(
            containerActivityDto = ContainerActivityDto(
                customerOrderNumber = "TestOrder1",
                location = "LocationPlace",
                containerId = "ContainerId1"
            ),
            testActivity
        )
        val bag = BarcodeType.MfcTote(
            bagOrToteId = "ContainerId1",
            customerOrderNumber = "TestOrder1",
            rawBarcode = "klerp",
            displayToteId = "kerpblerp",
        )
        bagLabel.isScanned = true
        val bagLabelSourceOfTruth = vm.getPrivateProperty("bagLabelSourceOfTruth") as ArrayList<BagLabel>
        bagLabelSourceOfTruth.add(bagLabel)
        vm.runPrivateMethodWithParams("handleScannedBagOrMfcTote", bag, bagLabel)
        assertThat((snackBarLiveEventMock.verifyWithNullableCapture(atLeastOnce())?.message as StringIdHelper.Raw).rawString == "Tote d1 already scanned")
        assertThat(hitBadSoundFeedback).isTrue()
    }

    @Test
    fun `WHEN handleScannedBagOrMfcTote with mfcTote generic failure toast message and sound`() {
        val bag = BarcodeType.MfcTote(
            bagOrToteId = "flerp",
            customerOrderNumber = "glerp",
            rawBarcode = "klerp",
            displayToteId = "kerpblerp",
        )
        var hitSoundFeedback = false
        val userFeedbackMock: UserFeedback = mock {
            onBlocking {
                setFailureScannedSoundAndHaptic()
            } doAnswer {
                hitSoundFeedback = true
            }
        }
        inlineKoinSingle(override = true) { userFeedbackMock }
        val vm = destageOrderPagerViewModelFactory()
        val snackBarLiveEventMock = vm.acupickSnackEvent.mock()
        val bagScannedData = ZonedBagsScannedData(
            bagLabel = BagLabel(
                containerActivityDto = ContainerActivityDto(
                    customerOrderNumber = "TestOrder1",
                    containerId = "ContainerId1"
                ),
                testActivity
            )
        )
        val zonedBagsScannedData = vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>
        zonedBagsScannedData.add(bagScannedData)
        vm.onScannerBarcodeReceived(bag)
        assertThat(hitSoundFeedback).isTrue()
        assertThat((snackBarLiveEventMock.verifyWithNullableCapture(atLeastOnce())?.message as StringIdHelper.Raw).rawString == "Tote scanned not part of order")
    }

    @Test
    fun `WHEN checkIfScannedBagHasBeenForceScanned verify hasBagBeenForceScanned false`() {
        val vm = destageOrderPagerViewModelFactory()
        val bag = BarcodeType.Bag(
            bagOrToteId = "flerp",
            customerOrderNumber = "glerp",
            rawBarcode = "klerp",
            displayToteId = "vlerp"
        )
        val hasBagBeenForceScanned = vm.runPrivateMethodWithParams("checkIfScannedBagHasBeenForceScanned", bag.bagOrToteId) as Boolean
        assertThat(hasBagBeenForceScanned).isFalse()
    }

    @Test
    fun `WHEN checkIfScannedBagHasBeenForceScanned verify hasBagBeenForceScanned true`() {
        val vm = destageOrderPagerViewModelFactory()
        val bag = BarcodeType.Bag(
            bagOrToteId = "flerp",
            customerOrderNumber = "glerp",
            rawBarcode = "klerp",
            displayToteId = "vlerp"
        )
        (vm.getPrivateProperty("forceScanList") as ArrayList<ZonedBagsScannedData>).add(
            ZonedBagsScannedData(
                bagLabel = BagLabel(
                    containerActivityDto = ContainerActivityDto(containerId = "flerp"),
                    testActivity
                )
            )
        )
        val hasBagBeenForceScanned = vm.runPrivateMethodWithParams("checkIfScannedBagHasBeenForceScanned", bag.bagOrToteId) as Boolean
        assertThat(hasBagBeenForceScanned).isTrue()
    }

    @Test
    fun `WHEN isValidScan with invalid verify method returns false`() {
        val vm = destageOrderPagerViewModelFactory()
        val validBagData = BagLabel(
            containerActivityDto = ContainerActivityDto(
                customerOrderNumber = "TestOrder1",
                containerId = "ContainerId1"
            ),
            testActivity
        )
        val zoneScannedBagData = ZonedBagsScannedData(bagLabel = validBagData)
        val invalidBagData = BagLabel(
            containerActivityDto = ContainerActivityDto(
                customerOrderNumber = "TestOrder2",
                containerId = "ContainerId2"
            ),
            testActivity
        )
        assertThat(vm.isValidScan(invalidBagData, zoneScannedBagData)).isFalse()
    }

    @Test
    fun `WHEN isValidScan with valid verify method returns true`() {
        val vm = destageOrderPagerViewModelFactory()
        val validBagData = BagLabel(
            containerActivityDto = ContainerActivityDto(
                customerOrderNumber = "TestOrder1",
                containerId = "ContainerId1"
            ),
            testActivity
        )
        val zoneScannedBagData = ZonedBagsScannedData(bagLabel = validBagData)
        assertThat(vm.isValidScan(validBagData, zoneScannedBagData)).isTrue()
    }

    @Test
    fun `WHEN isValidScan with valid with zonedBagsScannedData verify method returns true`() {
        val vm = destageOrderPagerViewModelFactory()
        val validBagData = BagLabel(
            containerActivityDto = ContainerActivityDto(
                customerOrderNumber = "TestOrder1",
                containerId = "ContainerId1"
            ),
            testActivity
        )
        val zoneScannedBagData = ZonedBagsScannedData(bagLabel = validBagData)
        val zonedBagsScannedData = vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>
        zonedBagsScannedData.add(zoneScannedBagData)
        assertThat(vm.isValidScan(validBagData, zoneScannedBagData)).isTrue()
    }

    @Test
    fun `WHEN onScannerBarcodeReceived currentBagsScanned 1 verify failure`() {
        val bag = BarcodeType.Bag(
            bagOrToteId = "flerp",
            customerOrderNumber = "glerp",
            rawBarcode = "klerp",
            displayToteId = "vlerp"
        )
        var hitSoundFeedback = false
        val userFeedbackMock: UserFeedback = mock {
            onBlocking {
                setFailureScannedSoundAndHaptic()
            } doAnswer {
                hitSoundFeedback = true
            }
        }
        inlineKoinSingle(override = true) { userFeedbackMock }
        val vm = destageOrderPagerViewModelFactory()
        val snackBarLiveEventMock = vm.acupickSnackEvent.mock()
        val zonedBagsScannedData = vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>
        val bagScannedData = ZonedBagsScannedData(
            bagLabel = BagLabel(
                containerActivityDto = ContainerActivityDto(
                    customerOrderNumber = "TestOrder1",
                    containerId = "ContainerId1"
                ),
                testActivity
            )
        )
        bagScannedData.currentBagsScanned = 1
        zonedBagsScannedData.add(bagScannedData)

        vm.onScannerBarcodeReceived(bag)
        assertThat((snackBarLiveEventMock.verifyWithNullableCapture(atLeastOnce())?.message as StringIdHelper.Raw).rawString == "Wrong item scanned - Please scan a bag")
        assertThat(hitSoundFeedback).isTrue()
    }

    @Test
    fun `WHEN onScannerBarcodeReceived currentBagsScanned 0 verify failure`() {
        var hitSoundFeedback = false
        val userFeedbackMock: UserFeedback = mock {
            onBlocking {
                setFailureScannedSoundAndHaptic()
            } doAnswer {
                hitSoundFeedback = true
            }
        }
        inlineKoinSingle(override = true) { userFeedbackMock }
        val vm = destageOrderPagerViewModelFactory()
        val snackBarLiveEventMock = vm.acupickSnackEvent.mock()
        val bagScannedData = ZonedBagsScannedData(
            bagLabel = BagLabel(
                containerActivityDto = ContainerActivityDto(
                    customerOrderNumber = "TestOrder1",
                    containerId = "ContainerId1"
                ),
                testActivity
            )
        )
        val zonedBagsScannedData = vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>
        bagScannedData.currentBagsScanned = 0
        zonedBagsScannedData.add(bagScannedData)
        vm.onScannerBarcodeReceived(BarcodeType.Zone(rawBarcode = "flerp", zoneType = StorageType.CH))
        assertThat((snackBarLiveEventMock.verifyWithNullableCapture(atLeastOnce())?.message as StringIdHelper.Raw).rawString == "Wrong item scanned - Please scan a bag")
        assertThat(hitSoundFeedback).isTrue()
    }

    @Test
    fun `WHEN onScannerBarcodeReceived failure verify snack success`() {
        val bag = BarcodeType.Bag(
            bagOrToteId = "flerp",
            customerOrderNumber = "glerp",
            rawBarcode = "klerp",
            displayToteId = "vlerp"
        )
        var hitSoundFeedback = false
        val userFeedbackMock: UserFeedback = mock {
            onBlocking {
                setFailureScannedSoundAndHaptic()
            } doAnswer {
                hitSoundFeedback = true
            }
        }
        inlineKoinSingle(override = true) { userFeedbackMock }
        val vm = destageOrderPagerViewModelFactory()
        val snackBarLiveEventMock = vm.acupickSnackEvent.mock()
        val bagScannedData = ZonedBagsScannedData(
            bagLabel = BagLabel(
                containerActivityDto = ContainerActivityDto(
                    customerOrderNumber = "TestOrder1",
                    containerId = "ContainerId1"
                ),
                testActivity
            )
        )
        val zonedBagsScannedData = vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>
        zonedBagsScannedData.add(bagScannedData)
        vm.onScannerBarcodeReceived(bag)
        assertThat(hitSoundFeedback).isTrue()
        assertThat((snackBarLiveEventMock.verifyWithNullableCapture(atLeastOnce())?.message as StringIdHelper.Raw).rawString == "Bag scanned not part of order")
    }

    @Test
    fun `WHEN handleManualEntryBag with null bag verify no sound feedback`() {
        var hitSoundFeedback = false
        val userFeedbackMock: UserFeedback = mock {
            onBlocking {
                setFailureScannedSoundAndHaptic()
            } doAnswer {
                hitSoundFeedback = true
            }
        }
        inlineKoinSingle(override = true) { userFeedbackMock }
        val vm = destageOrderPagerViewModelFactory()
        vm.handleManualEntryBag(
            handOffBag = ManualEntryHandOffBag()
        )
        assertThat(hitSoundFeedback).isFalse()
    }

    @Test
    fun `WHEN handleManualEntryBag with bag verify sound feedback`() {
        var hitSoundFeedback = false
        val userFeedbackMock: UserFeedback = mock {
            onBlocking {
                setFailureScannedSoundAndHaptic()
            } doAnswer {
                hitSoundFeedback = true
            }
        }
        inlineKoinSingle(override = true) { userFeedbackMock }
        val vm = destageOrderPagerViewModelFactory()
        val zonedBagsScannedData = vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>
        val bagScannedData = ZonedBagsScannedData(
            bagLabel = BagLabel(
                containerActivityDto = ContainerActivityDto(
                    customerOrderNumber = "TestOrder1",
                    containerId = "ContainerId1"
                ),
                testActivity
            )
        )
        val bag = BarcodeType.Bag(
            bagOrToteId = "flerp",
            customerOrderNumber = "glerp",
            rawBarcode = "klerp",
            displayToteId = "vlerp"
        )
        bagScannedData.currentBagsScanned = 1
        zonedBagsScannedData.add(bagScannedData)
        vm.handleManualEntryBag(
            handOffBag = ManualEntryHandOffBag(bag = bag)
        )
        assertThat(hitSoundFeedback).isTrue()
    }

    @Test
    fun `WHEN handleScannedBag on already scanned verify toast and bad sound`() {
        var hitBadSoundFeedback = false
        val userFeedbackMock: UserFeedback = mock {
            onBlocking {
                setFailureScannedSoundAndHaptic()
            } doAnswer {
                hitBadSoundFeedback = true
            }
        }
        inlineKoinSingle(override = true) { userFeedbackMock }
        val vm = destageOrderPagerViewModelFactory()
        val snackBarLiveEventMock = vm.acupickSnackEvent.mock()
        val bagLabel = BagLabel(
            containerActivityDto = ContainerActivityDto(
                customerOrderNumber = "TestOrder1",
                location = "LocationPlace",
                containerId = "ContainerId1"
            ),
            testActivity
        )
        val bag = BarcodeType.Bag(
            bagOrToteId = "ContainerId1",
            customerOrderNumber = "TestOrder1",
            rawBarcode = "klerp",
            displayToteId = "vlerp"
        )
        bagLabel.isScanned = true
        val bagLabelSourceOfTruth = vm.getPrivateProperty("bagLabelSourceOfTruth") as ArrayList<BagLabel>
        bagLabelSourceOfTruth.add(bagLabel)
        vm.runPrivateMethodWithParams("handleScannedBagOrMfcTote", bag, bagLabel)
        assertThat((snackBarLiveEventMock.verifyWithNullableCapture(atLeastOnce())?.message as StringIdHelper.Raw).rawString == "Bag d1 already scanned")
        assertThat(hitBadSoundFeedback).isTrue()
    }

    @Test @Ignore
    fun `WHEN handleScannedBag on valid scan verify toast and good sound`() {
        var hitGoodSoundFeedback = false
        val userFeedbackMock: UserFeedback = mock {
            onBlocking {
                setSuccessScannedSoundAndHaptic()
            } doAnswer {
                hitGoodSoundFeedback = true
            }
        }
        inlineKoinSingle(override = true) { userFeedbackMock }
        val vm = destageOrderPagerViewModelFactory()
        val snackBarLiveEventMock = vm.snackBarLiveEvent.mock()
        val zonedBagsScannedData = vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>
        val bagLabel = BagLabel(
            containerActivityDto = ContainerActivityDto(
                customerOrderNumber = "TestOrder1",
                location = "LocationPlace",
                containerId = "ContainerId1"
            ),
            activityDto = testActivity
        )
        val bag = BarcodeType.Bag(
            bagOrToteId = "ContainerId1",
            customerOrderNumber = "TestOrder1",
            rawBarcode = "klerp",
            displayToteId = "vlerp"
        )
        zonedBagsScannedData.add(
            ZonedBagsScannedData(
                bagLabel = bagLabel
            )
        )
        bagLabel.isScanned = false
        val bagLabelSourceOfTruth = vm.getPrivateProperty("bagLabelSourceOfTruth") as ArrayList<BagLabel>
        bagLabelSourceOfTruth.add(bagLabel)
        vm.runPrivateMethodWithParams("handleScannedBagOrMfcTote", bag, bagLabel)
        assertThat((snackBarLiveEventMock.verifyWithNullableCapture(atLeastOnce())?.prompt as StringIdHelper.Raw).rawString == "Bag d1 scanned out of Zone vlerp")
        assertThat(hitGoodSoundFeedback).isTrue()
    }

    @Test
    fun `WHEN getUiDataForOrder with no resultsUiList and no order number verify null`() {
        val vm = destageOrderPagerViewModelFactory()
        var uiDataForOrder = vm.getUiDataForOrder("")
        assertThat(uiDataForOrder).isNull()
        val onStartTimer: (Job) -> Unit = { }
        (vm.resultsUiList as MutableLiveData<List<DestageOrderUiData>>).postValue(
            listOf(DestageOrderUiData(ActivityDto(), ActivityDto().getRejectedItemsByZone(), DetailsHeaderUi(ActivityDto(), onStartTimer), listOf()))
        )
        uiDataForOrder = vm.getUiDataForOrder("")
        assertThat(uiDataForOrder).isNull()
    }

    @Test
    fun `WHEN getUiDataForOrder with resultsUiList and order number verify not null`() {
        val vm = destageOrderPagerViewModelFactory()
        val actDto = ActivityDto(
            customerOrderNumber = "blewp"
        )
        val onStartTimer: (Job) -> Unit = { }
        (vm.resultsUiList as MutableLiveData<List<DestageOrderUiData>>).postValue(
            listOf(DestageOrderUiData(actDto, actDto.getRejectedItemsByZone(), DetailsHeaderUi(actDto, onStartTimer), listOf()))
        )
        val uiDataForOrder = vm.getUiDataForOrder("blewp")
        assertThat(uiDataForOrder).isNotNull()
    }

    @Test
    fun `WHEN getValidBagFromScannedBagOrMfcTote with mfc tote bag verify valid output`() {
        val vm = destageOrderPagerViewModelFactory()
        val mfcTote = BarcodeType.MfcTote(rawBarcode = "blewp", bagOrToteId = "blurp", customerOrderNumber = "blip", displayToteId = "kerpblerp")
        val bagLabelSourceOfTruth = vm.getPrivateProperty("bagLabelSourceOfTruth") as ArrayList<BagLabel>
        var validBag = vm.runPrivateMethodWithParams("getValidBagFromScannedBagOrMfcTote", mfcTote)
        assertThat(validBag).isNull()

        bagLabelSourceOfTruth.add(BagLabel(ContainerActivityDto(customerOrderNumber = "blip", containerId = "blurp"), testActivity))
        validBag = vm.runPrivateMethodWithParams("getValidBagFromScannedBagOrMfcTote", mfcTote)
        assertThat(validBag).isNotNull()
    }

    @Test
    fun `WHEN showBagOrMfcToteScanSuccess with mfcTote verify success and snackbar`() {
        var hitGoodSoundFeedback = false
        val userFeedbackMock: UserFeedback = mock {
            onBlocking {
                setSuccessScannedSoundAndHaptic()
            } doAnswer {
                hitGoodSoundFeedback = true
            }
        }
        inlineKoinSingle(override = true) { userFeedbackMock }
        val vm = destageOrderPagerViewModelFactory()
        val snackBarLiveEventMock = vm.acupickSnackEvent.mock()
        val zonedBagsScannedData = vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>
        val bagLabel = BagLabel(
            containerActivityDto = ContainerActivityDto(
                customerOrderNumber = "TestOrder1",
                location = "LocationPlace",
                containerId = "ContainerId1"
            ),
            testActivity
        )
        val bag = BarcodeType.MfcTote(
            bagOrToteId = "ContainerId1",
            customerOrderNumber = "TestOrder1",
            rawBarcode = "klerp",
            displayToteId = "kerpblerp",
        )
        zonedBagsScannedData.add(
            ZonedBagsScannedData(
                bagLabel = bagLabel
            )
        )
        bagLabel.isScanned = false
        val bagLabelSourceOfTruth = vm.getPrivateProperty("bagLabelSourceOfTruth") as ArrayList<BagLabel>
        bagLabelSourceOfTruth.add(bagLabel)
        vm.runPrivateMethodWithParams("showBagOrMfcToteScanSuccess", bag, bagLabel)
        assertThat((snackBarLiveEventMock.verifyWithNullableCapture(atLeastOnce())?.message as StringIdHelper.Raw).rawString == "Bag d1 scanned out of Zone vlerp")
        assertThat(hitGoodSoundFeedback).isTrue()
    }

    @Test
    fun `WHEN showBagOrMfcToteScanSuccess with invalid barcode type verify success and snackbar`() {
        var hitGoodSoundFeedback = false
        val userFeedbackMock: UserFeedback = mock {
            onBlocking {
                setSuccessScannedSoundAndHaptic()
            } doAnswer {
                hitGoodSoundFeedback = true
            }
        }
        inlineKoinSingle(override = true) { userFeedbackMock }
        val vm = destageOrderPagerViewModelFactory()
        val snackBarLiveEventMock = vm.acupickSnackEvent.mock()
        val zonedBagsScannedData = vm.getPrivateProperty("zonedBagsScannedData") as ArrayList<ZonedBagsScannedData>
        val bagLabel = BagLabel(
            containerActivityDto = ContainerActivityDto(
                customerOrderNumber = "TestOrder1",
                location = "LocationPlace",
                containerId = "ContainerId1"
            ),
            testActivity
        )
        val bag = BarcodeType.Bag(
            rawBarcode = "klerp",
            bagOrToteId = "glerp",
            customerOrderNumber = "flerp",
            displayToteId = "null"
        )
        zonedBagsScannedData.add(
            ZonedBagsScannedData(
                bagLabel = bagLabel
            )
        )
        bagLabel.isScanned = false
        val bagLabelSourceOfTruth = vm.getPrivateProperty("bagLabelSourceOfTruth") as ArrayList<BagLabel>
        bagLabelSourceOfTruth.add(bagLabel)
        vm.runPrivateMethodWithParams("showBagOrMfcToteScanSuccess", bag, bagLabel)
        assertThat((snackBarLiveEventMock.verifyWithNullableCapture(atLeastOnce())?.message as StringIdHelper.Raw).rawString == "null")
        assertThat(hitGoodSoundFeedback).isTrue()
    }

    @Test
    fun `WHEN checkComplete with complete and hot items verify hot dialog shown`() {
        val vm = destageOrderPagerViewModelFactory()
        val inlineDialogEventMock = vm.inlineDialogEvent.mock()
        (vm.getPrivateProperty("activityDtoArray") as MutableList<ActivityDto>).add(
            ActivityDto(
                containerActivities = listOf(
                    ContainerActivityDto(
                        containerType = StorageType.HT
                    )
                )
            )
        )
        vm.runPrivateMethod("checkComplete")
        assertThat(inlineDialogEventMock.verifyWithCapture(atLeastOnce())).isEqualTo(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.Informational,
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(idRes = R.string.hot_item_reminder_title),
                    body = StringIdHelper.Format(idRes = R.string.hot_item_reminder_body, rawString = "", additionalString = null),
                    secondaryBody = null,
                    customData = null,
                    imageUrl = null,
                    positiveButtonText = StringIdHelper.Id(idRes = android.R.string.ok),
                    negativeButtonText = null,
                    cancelable = false,
                    cancelOnTouchOutside = false
                ),
                tag = CONFIRM_ORDER_HOT_REMINDER_DIALOG_TAG
            )
        )
    }

    @Test
    fun `WHEN getHotDialogActivityBagData with 2 or 3 bags verify strings`() {
        val vm = destageOrderPagerViewModelFactory()
        (vm.getPrivateProperty("activityDtoArray") as MutableList<ActivityDto>).addAll(
            listOf(
                ActivityDto(
                    containerActivities = listOf(
                        ContainerActivityDto(
                            containerType = StorageType.HT
                        )
                    ),
                    fulfillment = FulfillmentAttributeDto(
                        type = FulfillmentType.DUG,
                    ),
                    shortOrderNumber = "blerp"
                ),
                ActivityDto(
                    containerActivities = listOf(
                        ContainerActivityDto(
                            containerType = StorageType.HT
                        )
                    ),
                    fulfillment = FulfillmentAttributeDto(
                        subType = FulfillmentSubType.THREEPL,
                    ),
                    stopNumber = "glerp"
                )
            )
        )
        var dialogData = vm.runPrivateMethod("getHotDialogActivityBagData")
        assertThat(dialogData).isEqualTo(
            StringIdHelper.Format(idRes = R.string.hot_item_reminder_body, rawString = "0 bags for DUG-blerp and 0 bags for 3PL-blerp", additionalString = null)
        )
        (vm.getPrivateProperty("activityDtoArray") as MutableList<ActivityDto>).add(
            ActivityDto(
                containerActivities = listOf(
                    ContainerActivityDto(
                        containerType = StorageType.HT
                    )
                ),
                fulfillment = FulfillmentAttributeDto(
                    subType = FulfillmentSubType.ONEPL,
                ),
                routeVanNumber = "glerp",
                stopNumber = "dlerp"
            )
        )
        dialogData = vm.runPrivateMethod("getHotDialogActivityBagData")
        assertThat(dialogData).isEqualTo(
            StringIdHelper.Format(idRes = R.string.hot_item_reminder_body, rawString = "0 bags for DUG-blerp, 0 bags for 3PL-blerp, and 0 bags for glerp-dlerp", additionalString = null)
        )
    }

    @Test
    fun `WHEN showOrderIssueDialog verify livedata`() {
        val vm = destageOrderPagerViewModelFactory()
        val inlineDialogEventMock = vm.inlineDialogEvent.mock()
        vm.showOrderIssueDialog()
        assertThat(inlineDialogEventMock.verifyWithCapture(atLeastOnce())).isEqualTo(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    dialogType = DialogType.RadioButtons,
                    title = StringIdHelper.Id(R.string.order_issue_dialog_title),
                    customData = listOf(
                        StringIdHelper.Id(R.string.bag_labels_missing),
                        StringIdHelper.Id(R.string.bags_missing),
                    ) as Serializable,
                    positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                ),
                tag = ORDER_ISSUE_DIALOG_TAG
            )
        )
    }
}
