package com.albertsons.acupick.ui.itemdetails

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.SubstitutionCode
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.InstructionDto
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.PickedItemUpcDto
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.DevOptionsRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.data.toast.Toaster
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.stateFlowOf
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.mocks.testPickRepository
import com.albertsons.acupick.ui.MainActivityViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ItemDetailsViewModelTest : BaseTest() {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    // /////////////////////////////////////////////////////////////////////////
    // Tests for onScannerBarcodeReceived
    // /////////////////////////////////////////////////////////////////////////
    @Test
    fun `WHEN an unknown barcode is received THEN barcodeType receives unknown (with empty barcode value) and up navigation is triggered `() {
        // Create VM and mock out dependencies
        val vm = itemDetailsViewModelTestFactory(
            pickRepository = mock {
                on { pickList } doReturn stateFlowOf(testActivity)
                onBlocking { getItem(any<BarcodeType.Item>(), any()) } doReturn null
            }
        )

        // Setup observer mocks
        val mockNavObserver = vm.navigationEvent.mock()
        val mockBarcodeObserver = vm.barcodeType.mock()

        // Execute code to be tested
        vm.onScannerBarcodeReceived(BarcodeType.Unknown("0"))

        // Verify results
        verify(mockBarcodeObserver).onChanged(BarcodeType.Unknown(""))
        verify(mockNavObserver).onChanged(NavigationEvent.Up)
    }

    @Test
    fun `WHEN a barcode is matched to pickRepo THEN barcodeType receives value and up navigation is triggered `() {
        // Setup test data
        val inputBarcode = BarcodeType.Item.Normal(catalogLookupUpc = "123", rawBarcode = "456")

        // Create VM and mock out dependencies
        val vm = itemDetailsViewModelTestFactory(
            pickRepository = mock {
                on { pickList } doReturn stateFlowOf(testActivity)
                onBlocking { getItem(any<BarcodeType.Item>(), any()) } doReturn testItem
            }
        )

        // Init VM State
        vm.iaId.postValue(123)
        vm.setItem(testItem)

        // Setup observer mocks
        val mockNavObserver = vm.navigationEvent.mock()
        val mockBarcodeObserver = vm.barcodeType.mock()

        // Execute code to be tested
        vm.onScannerBarcodeReceived(inputBarcode)

        // Verify results
        verify(mockBarcodeObserver).onChanged(inputBarcode)
        verify(mockNavObserver).onChanged(NavigationEvent.Up)
    }

    @Test
    fun `WHEN a barcode is not matched to pickRepo THEN barcodeType receives unknown (with empty barcode value) and up navigation is triggered `() {
        // Setup test data
        val inputBarcode = BarcodeType.Item.Normal(catalogLookupUpc = "777", rawBarcode = "456")

        // Create VM and mock out dependencies
        val vm = itemDetailsViewModelTestFactory(
            pickRepository = mock {
                on { pickList } doReturn stateFlowOf(testActivity)
                onBlocking { getItem(any<BarcodeType.Item>(), any()) } doReturn null
            }
        )

        // Init VM State
        vm.iaId.postValue(123)
        vm.setItem(testItem)

        // Setup observer mocks
        val mockNavObserver = vm.navigationEvent.mock()
        val mockBarcodeObserver = vm.barcodeType.mock()

        // Execute code to be tested
        vm.onScannerBarcodeReceived(inputBarcode)

        // Verify results
        verify(mockBarcodeObserver).onChanged(BarcodeType.Unknown(""))
        verify(mockNavObserver).onChanged(NavigationEvent.Up)
    }

    // /////////////////////////////////////////////////////////////////////////
    // Click listeners
    // /////////////////////////////////////////////////////////////////////////
    @Test
    fun `WHEN image is clicked THEN event to show dialog is triggered`() {
        // Setup VM using default dependencies
        val vm = itemDetailsViewModelTestFactory()

        // Init VM State
        vm.iaId.postValue(123)
        vm.setItem(testItem)

        // Setup observer mocks
        val mockObserver = vm.showItemPhotoDialog.mock()

        // Execute code to be tested
        vm.onImageClicked()

        // Verify Results
        verify(mockObserver).onChanged("www.google.com/path/toimage?\$ospk-product-itemzoom\$&defaultImage=Not_Available")
    }

    // TODO - Refactor test now that this screen is just a dialog in ItemsDetailsFragment
    /*@Test
    fun `WHEN short is clicked THEN navigation event fires`() {
        // Setup VM using default dependencies
        val vm = itemDetailsViewModelTestFactory()

        // Init VM State
        vm.iaId.postValue(123)
        vm.setItem(noSubsTestItem)

        // Setup observer mocks
        val mockObserver = vm.navigationEvent.mock()

        // Execute code to be tested
        vm.onShortClicked()

        // Verify Results
        verify(mockObserver).onChanged(
            NavigationEvent.Directions(
                ItemDetailsFragmentDirections.actionItemDetailsFragmentToShortItemFragment(
                    item = noSubsTestItem
                )
            )
        )
    }*/

    // /////////////////////////////////////////////////////////////////////////
    // Tests for UI init
    // /////////////////////////////////////////////////////////////////////////
    // TODO: ACURED_REDESIGN Need to validate this test later
   /* @Test
    fun `WHEN setItemDetails is called (1 picked) THEN UI initializes correctly`() {
        // Setup VM using default dependencies
        val vm = itemDetailsViewModelTestFactory()

        // Setup observer mocks
        val itemAddressMock = vm.itemAddress.mock()
        val upcMock = vm.upc.mock()
        val isFullyPickedMock = vm.isFullyPicked.mock()
        val processedQtyMock = vm.processedQty.mock()
        val totalQtyMock = vm.totalQty.mock()
        val imageUrlMock = vm.imageUrl.mock()
        val descriptionMock = vm.description.mock()
        val customerCommentsMock = vm.customerComments.mock()
        val substitutionInfoMock = vm.substitutionInfo.mock()
        val shortQtyMock = vm.shortQty.mock()
        val noScansOrExceptionsMock = vm.noScansOrExceptions.mock()
        val isUnpickButtonEnabledMock = vm.isUnpickButtonEnabled.mock()
        val itemActionListMock = vm.itemActionList.mock()

        // Init VM State
        vm.iaId.postValue(123)

        // Verify results
        assertThat(itemAddressMock.verifyWithCapture(atLeastOnce())).isEqualTo("Dept Name")
        assertThat(upcMock.verifyWithCapture(atLeastOnce())).isEqualTo("")
        assertThat(isFullyPickedMock.verifyWithCapture(atLeastOnce())).isEqualTo(false)
        assertThat(processedQtyMock.verifyWithCapture(atLeastOnce())).isEqualTo("0")
        assertThat(totalQtyMock.verifyWithCapture(atLeastOnce())).isEqualTo("2")
        assertThat(imageUrlMock.verifyWithCapture(atLeastOnce())).isEqualTo("www.google.com/path/to?image?\$ospk-product-itemdetails\$&defaultImage=Not_Available")
        assertThat(descriptionMock.verifyWithCapture(atLeastOnce())).isEqualTo("Item Description")
        assertThat(customerCommentsMock.verifyWithCapture(atLeastOnce())).isEqualTo("")
        assertThat(substitutionInfoMock.verifyWithCapture(atLeastOnce())).isEqualTo("")
        assertThat(shortQtyMock.verifyWithCapture(atLeastOnce())).isEqualTo("0 short")
        assertThat(noScansOrExceptionsMock.verifyWithCapture(atLeastOnce())).isEqualTo(false)
        assertThat(isUnpickButtonEnabledMock.verifyWithCapture(atLeastOnce())).isEqualTo(false)
        itemActionListMock.verifyWithCapture(atLeastOnce()).let { itemList ->
            assertThat(itemList).hasSize(1)
            itemList.first().let {
                assertThat(it.description).isEqualTo("00123")
                assertThat(it.tote).isEqualTo("TTC33")
                assertThat(it.qty).isEqualTo("1 item")
            }
        }
    }*/

    // /////////////////////////////////////////////////////////////////////////
    // Test Objects and Factories
    // /////////////////////////////////////////////////////////////////////////
    private val context: Context = mock {
        // Order is important here, most generic match first. Last match is final value.
        on { getString(anyInt(), anyVararg()) } doReturn ""

        // Make sure string argument matches 123
        on { getString(eq(R.string.item_details_plu_format), anyVararg()) } doReturn "PLU: ???"
        // Use actual argument value with doAnswer and InvocationOnMock.getArgument
        on { getString(eq(R.string.item_details_plu_format), eq("123")) } doAnswer { "PLU: ${it.getArgument<String>(1)}" }
    }

    private val resources: Resources = mock {
        // Order is important here, most generic match first. Last match is final value.
        on { getQuantityString(anyInt(), anyInt(), anyInt()) } doReturn ""
        on { getString(anyInt(), anyVararg()) } doReturn ""
        on { getString(R.string.uom_default) } doReturn "lb"
    }

    private fun testApplicationFactory(mockContext: Context = context): Application = mock {
        on { applicationContext } doReturn mockContext
        on { resources } doReturn resources
        // Order is important here, most generic match first. Last match is final value.
        on { getString(anyInt()) } doReturn ""
        on { getString(anyInt(), anyVararg()) } doReturn ""
        on { getString(eq(R.string.number_short_format), anyVararg()) } doReturn "? short"
        // Use actual argument value with doAnswer and InvocationOnMock.getArgument
        on { getString(eq(R.string.number_short_format), eq("0")) } doAnswer { "${it.getArgument<String>(1)} short" }
        on { getString(R.string.same_brand_diff_size) } doReturn "Same brand, different size"
        on { getString(R.string.item_details_1_item) } doReturn "1 item"
    }

    private val testActivity: ActivityDto = mock {}

    private val testInstruction: InstructionDto = mock {
        on { text } doReturn "Customer Comment"
    }

    private val testPickedUpcDto: PickedItemUpcDto = mock {
        on { qty } doReturn 1.0
        on { containerId } doReturn "TTC33"
    }

    private val testItem: ItemActivityDto = mock {
        on { id } doReturn 123
        on { imageUrl } doReturn "www.google.com/path/toimage"
        on { itemAddressDto } doReturn null
        on { sellByWeightInd } doReturn SellByType.Prepped
        on { pluCode } doReturn "123"
        on { depName } doReturn "Dept Name"
        on { qty } doReturn 2.0
        on { processedQty } doReturn 0.0
        on { itemDescription } doReturn "Item Description"
        on { exceptionQty } doReturn 0.0
        on { instructionDto } doReturn testInstruction
        on { subAllowed } doReturn true
        on { subCode } doReturn SubstitutionCode.SAME_BRAND_DIFF_SIZE
        on { pickedUpcCodes } doReturn listOf(testPickedUpcDto)
        on { customerOrderNumber } doReturn "12345678"
    }

    private val noSubsTestItem: ItemActivityDto = mock {
        on { id } doReturn 123
        on { imageUrl } doReturn "www.google.com/path/toimage"
        on { itemAddressDto } doReturn null
        on { sellByWeightInd } doReturn SellByType.Prepped
        on { pluCode } doReturn "123"
        on { depName } doReturn "Dept Name"
        on { qty } doReturn 2.0
        on { processedQty } doReturn 0.0
        on { itemDescription } doReturn "Item Description"
        on { exceptionQty } doReturn 0.0
        on { instructionDto } doReturn testInstruction
        on { subAllowed } doReturn false
        on { subCode } doReturn SubstitutionCode.SAME_BRAND_DIFF_SIZE
        on { pickedUpcCodes } doReturn listOf(testPickedUpcDto)
    }

    private fun itemDetailsViewModelTestFactory(
        app: Application = testApplicationFactory(),
        pickRepository: PickRepository = testPickRepository(),
        activityViewModel: MainActivityViewModel = mock {},
        dispatcherProvider: DispatcherProvider = TestDispatcherProvider(),
        toaster: Toaster = mock {},
        barcodeMapper: BarcodeMapper = mock {
            on { inferBarcodeType(any(), any()) } doReturn BarcodeType.Item.Normal(catalogLookupUpc = "00000123", rawBarcode = "00123")
        },
        networkAvailabilityManager: NetworkAvailabilityManager = mock {
            onBlocking { isConnected } doReturn stateFlowOf(true)
        },
        devOptionsRepository: DevOptionsRepository = mock {},
        userRepository: UserRepository = mock {},
        siteRepository: SiteRepository = mock {}
    ) = ItemDetailsViewModel(
        app = app,
        pickRepository = pickRepository,
        activityViewModel = activityViewModel,
        dispatcherProvider = dispatcherProvider,
        toaster = toaster,
        barcodeMapper = barcodeMapper,
        networkAvailabilityManager = networkAvailabilityManager,
        devOptionsRepository = devOptionsRepository,
        userRepository = userRepository,
        siteRepository = siteRepository
    )
}
