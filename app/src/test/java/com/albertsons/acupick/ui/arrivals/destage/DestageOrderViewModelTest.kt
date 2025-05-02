package com.albertsons.acupick.ui.arrivals.destage

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.asLiveData
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.response.ContainerActivityDto
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.destageOrderViewModelFactory
import com.albertsons.acupick.test.mocks.testActivity
import com.albertsons.acupick.ui.models.BagLabel
import com.albertsons.acupick.ui.models.DestageOrderUiData
import com.albertsons.acupick.ui.models.ZonedBagsScannedData
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.verify

class DestageOrderViewModelTest : BaseTest() {

    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    @get:Rule
    val koinRule = KoinTestRule(TestModule.generateMockedTestModule())

    @Test
    fun `WHEN activity livedata has posted value THEN value changes`() {
        val vm = destageOrderViewModelFactory()
        val activityMock = vm.activity.mock()
        val onStartTimer: (Job) -> Unit = { }
        val activity = DestageOrderUiData(
            actId = 2L,
            activityNo = "blewp",
            erId = 4L,
            customerName = "duderino",
            customerFistNameLastInitial = "duderino b",
            customerOrderNumber = "blurp",
            isDugOrder = false,
            detailsHeaderUi = DetailsHeaderUi(
                activityId = null,
                shortOrderNumber = null,
                contactName = null,
                customerOrderNumber = null,
                expectedCount = 0,
                showRegulatedError = false,
                startTime = null,
                customerArrivalStatusUI = null,
                isCustomerBagPreference = true,
                isGift = false,
                onStartTimer = onStartTimer,
            ),
            zonedBags = listOf(),
            totalCount = 0,
            type = ContainerType.BAG,
            isMultiSource = false,
            entityReference = null,
            hasAddOnPrescription = false,
            rxBags = null,
            customerArrivlaTime = null,
            customerArrivalStatusUI = null,
            fulfillmentTypeUI = null,
            feScreenStatus = null,
            isCustomerBagPreference = false,
            isRxDug = false,
            isGift = false,
            giftMessage = null
        )
        vm.activity.postValue(activity)
        verify(activityMock).onChanged(
            activity
        )
    }

    @Suppress("UNCHECKED_CAST")

    @Test
    fun `WHEN detailsHeaderUi livedata has posted value THEN value changes`() {
        val vm = destageOrderViewModelFactory()
        val detailsHeaderUiMock = vm.detailsHeaderUi.mock()
        val onStartTimer: (Job) -> Unit = { }
        val detailsHeaderUi = DetailsHeaderUi(
            activityId = 10L,
            showRegulatedError = null,
            shortOrderNumber = null,
            contactName = null,
            customerOrderNumber = null,
            expectedCount = null,
            startTime = null,
            customerArrivalStatusUI = null,
            isCustomerBagPreference = true,
            isGift = false,
            onStartTimer = onStartTimer
        )
        vm.detailsHeaderUi.postValue(detailsHeaderUi)
        verify(detailsHeaderUiMock).onChanged(
            detailsHeaderUi
        )
    }

    @Test
    fun `WHEN zonedBagUiData livedata has posted value THEN value changes`() {
        val vm = destageOrderViewModelFactory()
        val zonedBagUiDataMock = vm.zonedBagUiData.mock()
        val zonedBagsScannedData = ZonedBagsScannedData(
            bagLabel = BagLabel(
                containerActivityDto = ContainerActivityDto(),
                testActivity
            )
        )
        vm.zonedBagUiData.postValue(listOf(zonedBagsScannedData))
        verify(zonedBagUiDataMock).onChanged(
            listOf(zonedBagsScannedData)
        )
    }

    @Test
    fun `WHEN currentZone livedata has posted value THEN value changes`() {
        val vm = destageOrderViewModelFactory()
        val currentBagLabelMock = vm.currentBagLabel.asLiveData().mock()
        val bagLabelMock = BagLabel(
            containerActivityDto = ContainerActivityDto(),
            testActivity
        )
        runBlockingTest {
            vm.currentBagLabel.emit(bagLabelMock)
            verify(currentBagLabelMock).onChanged(
                bagLabelMock
            )
        }
    }

    @Test
    fun `WHEN zonedBagUiData has posted value THEN ambient chilled and frozen counts change`() {
        val vm = destageOrderViewModelFactory()
        val ambientCountMock = vm.amZoneCounts.mock()
        val chilledCountMock = vm.chZoneCounts.mock()
        val frozenCountMock = vm.fzZoneCounts.mock()
        val zonedBagsScannedDataAmbient = ZonedBagsScannedData(
            bagLabel = BagLabel(
                containerActivityDto = ContainerActivityDto(
                    containerType = StorageType.AM
                ),
                testActivity
            )
        )
        val zonedBagsScannedDataChilled = ZonedBagsScannedData(
            bagLabel = BagLabel(
                containerActivityDto = ContainerActivityDto(
                    containerType = StorageType.CH
                ),
                testActivity
            )
        )
        val zonedBagsScannedDataFrozen = ZonedBagsScannedData(
            bagLabel = BagLabel(
                containerActivityDto = ContainerActivityDto(
                    containerType = StorageType.FZ
                ),
                testActivity
            )
        )
        vm.zonedBagUiData.postValue(listOf(zonedBagsScannedDataAmbient, zonedBagsScannedDataChilled, zonedBagsScannedDataFrozen))
        verify(ambientCountMock).onChanged(1)
        verify(chilledCountMock).onChanged(1)
        verify(frozenCountMock).onChanged(1)
        vm.zonedBagUiData.postValue(listOf())
        verify(ambientCountMock).onChanged(0)
        verify(chilledCountMock).onChanged(0)
        verify(frozenCountMock).onChanged(0)
    }

    @Test
    fun `Bag Bypass Count has posted value with forced scan THEN forceScannedCount value changes`() {
        val vm = destageOrderViewModelFactory()
        val onStartTimer: (Job) -> Unit = { }
        val detailsHeaderUi = DetailsHeaderUi(
            activityId = 10L,
            showRegulatedError = null,
            shortOrderNumber = null,
            contactName = null,
            customerOrderNumber = "1",
            expectedCount = null,
            startTime = null,
            customerArrivalStatusUI = null,
            isCustomerBagPreference = true,
            isGift = false,
            onStartTimer = onStartTimer
        )
        val forceScannedCountMock = vm.forceScannedCount.mock()
        val bagBypass = mutableListOf<ZonedBagsScannedData>()
        val zonedBagsScannedData = ZonedBagsScannedData(
            bagData = BagLabel(
                containerActivityDto = ContainerActivityDto(
                    customerOrderNumber = "1"

                ),
                testActivity
            )
        )
        vm.detailsHeaderUi.postValue(detailsHeaderUi)
        bagBypass.add(zonedBagsScannedData)

        vm.updateBagBypass(bagBypass)
        verify(forceScannedCountMock).onChanged(1)
    }

    @Test
    fun `WHEN zonedBagUiData has posted value with currentBagsScanned THEN isComplete value changes`() {
        val vm = destageOrderViewModelFactory()
        val isCompleteMock = vm.isComplete.mock()
        val zonedBagsScannedData = ZonedBagsScannedData(
            bagLabel = BagLabel(
                containerActivityDto = ContainerActivityDto(
                    customerOrderNumber = "blerp"
                ),
                testActivity
            )
        )
        val onStartTimer: (Job) -> Unit = { }
        val detailsHeaderUi = DetailsHeaderUi(
            activityId = null,
            shortOrderNumber = null,
            contactName = null,
            customerOrderNumber = "blerp",
            expectedCount = 0,
            showRegulatedError = false,
            startTime = null,
            customerArrivalStatusUI = null,
            isCustomerBagPreference = true,
            isGift = false,
            onStartTimer = onStartTimer
        )
        vm.zonedBagUiData.postValue(listOf(zonedBagsScannedData))
        vm.detailsHeaderUi.postValue(detailsHeaderUi)
        verify(isCompleteMock).onChanged(false)

        zonedBagsScannedData.currentBagsScanned = 1
        vm.zonedBagUiData.postValue(listOf(zonedBagsScannedData))
        verify(isCompleteMock).onChanged(true)
    }

    @Test
    fun `WHEN currentZone zonedBagUiData has posted value with currentBagsScanned with non-MFC order THEN isComplete value changes`() {
        val vm = destageOrderViewModelFactory()
        vm.isMfcOrder.postValue(false)
        val orderIssuesButtonEventMock = vm.orderIssuesButtonEvent.mock()
        vm.onOrderIssuesButtonPressed()
        verify(orderIssuesButtonEventMock).onChanged(
            false
        )
    }

    @Test
    fun `WHEN currentZone zonedBagUiData has posted value with currentBagsScanned with MFC order THEN isComplete value changes`() {
        val vm = destageOrderViewModelFactory()
        vm.isMfcOrder.postValue(true)
        val orderIssuesButtonEventMock = vm.orderIssuesButtonEvent.mock()
        vm.onOrderIssuesButtonPressed()
        verify(orderIssuesButtonEventMock).onChanged(
            true
        )
    }
}
