package com.albertsons.acupick.ui.substitute

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.albertsons.acupick.R
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.data.toast.Toaster
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.mocks.testApplicationFactory
import com.albertsons.acupick.test.mocks.testItem
import com.albertsons.acupick.test.mocks.testItem2
import com.albertsons.acupick.test.mocks.testNetworkAvailabilityManager
import com.albertsons.acupick.test.mocks.testPickRepository
import com.albertsons.acupick.test.mocks.testUserRepo
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.manualentry.ManualEntryPickParams
import com.albertsons.acupick.ui.manualentry.ManualEntryType
import com.albertsons.acupick.ui.util.StringIdHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock

class SubstituteViewModelTest : BaseTest() {
    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    // @get:Rule
    // val buildVersion27Rule = BuildVersion27Rule()

    @get:Rule
    val koinRule = KoinTestRule(TestModule.generateMockedTestModule())

    // TODO: ACURED_REDESIGN Need to validate this test later
    /*@Test
    fun `WHEN post is called on THEN UI initializes correctly`() {
        // Setup VM
        val vm = substituteViewModelTestFactory()

        vm.handleManualEntryData(
            quantity = 1,
            barcode = BarcodeType.Item.Normal(
                catalogLookupUpc = "123456789123",
                rawBarcode = "123456789123"
            )
        )

        // Setup observer mocks
        val requestedCountMock = vm.requestedCount.mock()
        val substitutedCountMock = vm.substitutedCount.mock()
        val imageUrlMock = vm.imageUrl.mock()
        val descriptionMock = vm.description.mock()
        val upcMock = vm.upc.mock()
        val customerCommentsMock = vm.customerComments.mock()
        val substitutionInfoMock = vm.substitutionInfo.mock()
        val isEachItemMock = vm.isEachItem.mock()
        val quantityMock = vm.quantity.mock()

        // Testing initial UI setup triggered by posting item db activity id
        vm.iaId.postValue(123)

        // Verify UI fields have been updated correctly
        assertThat(requestedCountMock.verifyWithCapture()).isEqualTo(2)
        assertThat(substitutedCountMock.verifyWithCapture()).isEqualTo(0)
        assertThat(imageUrlMock.verifyWithNullableCapture()).isEqualTo("www.google.com/path/to?image?\$ospk-product-itemdetails\$&defaultImage=Not_Available")
        assertThat(descriptionMock.verifyWithCapture()).isEqualTo("Item Description")
        assertThat(upcMock.verifyWithNullableCapture()).isEqualTo("")
        assertThat(customerCommentsMock.verifyWithNullableCapture()).isEqualTo("")
        assertThat(substitutionInfoMock.verifyWithNullableCapture()).isEqualTo("")
        assertThat(isEachItemMock.verifyWithCapture()).isFalse()
        assertThat(quantityMock.verifyWithCapture()).isEqualTo(1)
    }*/

    @Test
    fun `WHEN onManualEntryButtonClicked THEN navigate to Manual Entry`() {
        // Setup VM
        val vm = substituteViewModelTestFactory()

        // Setup observer mocks
        val navigationEventMock = vm.navigationEvent.mock()

        // Initial VM state
        vm.swapSubReason.postValue(null)
        vm.iaIdBeforeSubstitution.postValue(123)
        vm.requestedCount.postValue(0)
        vm.remainingQtyCount.postValue(0)
        vm.item.getOrAwaitValue()

        // Execute code to be tested
        vm.onManualEntryButtonClicked()

        // Verify
        assertThat(navigationEventMock.verifyWithCapture()).isEqualTo(
            NavigationEvent.Directions(
                SubstituteFragmentDirections.actionSubstituteFragmentToManualEntryPickFragment(
                    ManualEntryPickParams(
                        selectedItem = testItem,
                        requestedQty = 0,
                        remainingRequestedQty = 0,
                        stageByTime = null,
                        isSubstitution = true,
                        entryType = ManualEntryType.UPC
                    ),
                    vm.entryType.value ?: ManualEntryType.UPC
                )
            )
        )
    }

    @Test
    fun `WHEN onCompleteButtonClicked THEN show complete confirmation dialog`() {
        // Setup VM
        val vm = substituteViewModelTestFactory()

        // Setup observer mocks
        val showCompleteConfirmationDialogMock = vm.inlineDialogEvent.mock()

        // Initial VM state
        vm.iaIdBeforeSubstitution.postValue(456)

        // Execute code to be tested
        vm.onCompleteButtonClicked()

        // Verify
        assertThat(showCompleteConfirmationDialogMock.verifyWithCapture()).isEqualTo(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.substitute_complete_confirmation_dialog_title),
                    body = StringIdHelper.Id(R.string.substitute_complete_confirmation_dialog_body),
                    positiveButtonText = StringIdHelper.Id(R.string.ok),
                    negativeButtonText = StringIdHelper.Id(R.string.cancel),
                    cancelOnTouchOutside = false
                ),
                tag = "substitutionCompleteConfirmationDialog"
            )
        )
    }

    // /////////////////////////////////////////////////////////////////////////
    // Barcode scanning tests
    // /////////////////////////////////////////////////////////////////////////
    @Test
    fun `WHEN unknown barcode scanned THEN status is updated`() {
        // Setup VM
        val vm = substituteViewModelTestFactory()

        // Setup observer mocks
        val pluOrUpcMock = vm.pluOrUpc.mock()
        val snackBarLiveEventMock = vm.acupickSnackEvent.mock()

        // Initial VM state
        vm.iaIdBeforeSubstitution.postValue(456)
        (vm.entryType as? MutableLiveData)?.postValue(ManualEntryType.UPC)

        // Execute code to be tested
        vm.onScannerBarcodeReceived(BarcodeType.Unknown(rawBarcode = "0000"))

        // Verify
        assertThat(pluOrUpcMock.verifyWithCapture(atLeastOnce())).isEqualTo("")
        assertThat(snackBarLiveEventMock.verifyWithNullableCapture(atLeastOnce())?.message).isEqualTo(
            StringIdHelper.Id(R.string.wrong_substitution_scanned)
        )
    }

    @Test
    fun `WHEN valid barcode scanned THEN plu is updated`() {
        // Setup VM
        val vm = substituteViewModelTestFactory()
        // Setup observer mocks
        val pluOrUpcMock = vm.pluOrUpc.mock()

        // Initial VM state
        vm.iaIdBeforeSubstitution.postValue(123)

        // Execute code to be tested
        vm.onScannerBarcodeReceived(BarcodeType.Item.Normal(rawBarcode = "8675309", catalogLookupUpc = "778899"))

        // Verify
        assertThat(pluOrUpcMock.verifyWithCapture(atLeastOnce())).isEqualTo("8675309")
    }

    @Test
    fun `WHEN subCode is ONLY_USE_SUGGESTED_SUB show showCustomerSubSelectionAlertDialog() dialog`() {
        // Setup VM
        val vm = substituteViewModelTestFactory()

        // Setup observer mocks
        val showDialogMock = vm.inlineDialogEvent.mock()

        // Initial VM state
        vm.iaIdBeforeSubstitution.postValue(456)

        vm.suggestedItem.value.let { MutableLiveData(testItem2) }

        // Verify
        assertThat(vm.getCustomerSubstitutionAlertDialogArgData(testItem)).isEqualTo(
            CustomDialogArgData(
                dialogType = DialogType.SubbedItemAlert,
                title = StringIdHelper.Id(R.string.substitute_customer_selection_alert_title),
                imageUrl = "www.google.com/path/to?image",
                body = StringIdHelper.Id(R.string.substitute_customer_selection_alert_description),
                positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
                cancelOnTouchOutside = true
            )
        )
    }

    // /////////////////////////////////////////////////////////////////////////
    // Test Objects and Factories
    // /////////////////////////////////////////////////////////////////////////
    private fun substituteViewModelTestFactory(
        handle: SavedStateHandle = mock {},
        app: Application = testApplicationFactory(),
        barcodeMapper: BarcodeMapper = mock { },
        pickRepository: PickRepository = testPickRepository(),
        userRepository: UserRepository = testUserRepo,
        dispatcherProvider: DispatcherProvider = TestDispatcherProvider(),
        toaster: Toaster = mock {},
        networkAvailabilityManager: NetworkAvailabilityManager = testNetworkAvailabilityManager,
        activityViewModel: MainActivityViewModel = mock {},
    ) = SubstituteViewModel(
        stateHandle = handle,
        app = app,
        barcodeMapper = barcodeMapper,
        dispatcherProvider = dispatcherProvider,
        pickRepo = pickRepository,
        userRepo = userRepository,
        toaster = toaster,
        networkAvailabilityManager = networkAvailabilityManager,
        activityViewModel = activityViewModel,
    )
}
