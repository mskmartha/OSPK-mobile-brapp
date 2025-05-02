package com.albertsons.acupick.ui.manual

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.albertsons.acupick.TestModule
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.KoinTestRule
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.ui.manualentry.ManualEntryPluUi
import com.albertsons.acupick.ui.manualentry.ManualEntryUpcUi
import com.albertsons.acupick.ui.manualentry.ManualEntryWeightUi
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerViewModel
import com.albertsons.acupick.ui.manualentry.pick.ValidationError
import com.albertsons.acupick.ui.manualentry.pick.WeightValidationError
import com.albertsons.acupick.ui.manualentry.pick.plu.ManualEntryPluViewModel
import com.albertsons.acupick.ui.manualentry.pick.upc.ManualEntryUpcViewModel
import com.albertsons.acupick.ui.manualentry.pick.weight.ManualEntryWeightViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class ManualEntryPagerViewModelTest : BaseTest() {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    @get:Rule
    val koinRule = KoinTestRule(TestModule.generateMockedTestModule())

    private val context: Context = mock {
        // Order is important here, most generic match first. Last match is final value.
        on { getString(any(), anyVararg()) } doReturn ""
    }

    private val resources: Resources = mock {}

    private val testItemEach = ItemActivityDto(sellByWeightInd = SellByType.Each, pluCode = "1234", qty = 4.0)
    private val testItemRegular = ItemActivityDto(sellByWeightInd = SellByType.RegularItem, qty = 4.0)

    private val testItemEachInvalid = ItemActivityDto(sellByWeightInd = SellByType.Each, pluCode = "0")

    private fun testApplicationFactory(mockContext: Context = context): Application = mock {
        on { applicationContext } doReturn mockContext
        on { resources } doReturn resources
    }

    private val testWeightedBarcodeType = BarcodeType.Item.Weighted(plu = "1234", catalogLookupUpc = "123454", rawWeight = "1", rawBarcode = "1234")
    private val testEachBarcodeType = BarcodeType.Item.Each(plu = "1234", catalogLookupUpc = "123456798", rawBarcode = "00123", generatedBarcode = "", itemActivityDbId = 0L)
    private val testNormalBarcodeType = BarcodeType.Item.Normal(catalogLookupUpc = "00000123", rawBarcode = "00123")

    private val testBarcodeMapper: BarcodeMapper = mock {
        on { generateWeightedBarcode(any(), any()) } doReturn testWeightedBarcodeType
        on { generateEachBarcode(any(), any()) } doReturn testEachBarcodeType
        on { inferBarcodeType(any(), any()) } doReturn testNormalBarcodeType
    }

    private val testManualInputDefaultValue = "12345678"
    private val testManualEntryUpcUI: ManualEntryUpcUi = mock {
        on { isSubstitution } doReturn true
    }

    private val testManualEntryPluUI: ManualEntryPluUi = mock {
        on { isSubstitution } doReturn true
        on { defaultValue } doReturn testManualInputDefaultValue
    }

    private val testManualEntryWeightUI: ManualEntryWeightUi = mock {
        on { isSubstitution } doReturn true
    }

    @Test
    fun `When manualEntryText, weightEntryText, manualQuantity is valid THEN continue is enabled`() {
        val viewModel = ManualEntryUpcViewModelTestFactory()

        val ui = mock<ManualEntryUpcUi> {
            on { isSubstitution } doReturn true
        }
        viewModel.manualEntryUPCUI.value = ui
        viewModel.upcEntryText.value = "123456789012"

        runBlocking {
            assertThat(viewModel.continueEnabled.getFlowAsLiveDataValue()).isEqualTo(true)
        }
    }

    @Test
    fun `When manualEntryText, weightEntryText, manualQuantity is not valid THEN continue is disabled`() {
        val viewModel = ManualEntryUpcViewModelTestFactory()

        runBlocking {
            assertThat(viewModel.continueEnabled.getFlowAsLiveDataValue()).isEqualTo(false)
        }
    }

    @Test
    fun `When onContinueButtonClicked for each items and no validation error THEN we show Quantity Picker`() {
        inlineKoinSingle { testBarcodeMapper }
        val viewModel = ManualEntryPluViewModelTestFactory()
        val pagerVm = ManualEntryPagerViewModelTestFactory()
        viewModel.pickListItem.value = testItemEach
        viewModel.pluEntryText.value = testManualInputDefaultValue
        viewModel.manualEntryPLUUI.value = testManualEntryPluUI
        pagerVm.onContinueButtonClicked()
        assertThat(viewModel.validationError.value).isEqualTo(ValidationError.NONE)
    }

    @Test
    fun `When onContinueButtonClicked with weight item THEN we show Quantity Picker`() {
        inlineKoinSingle { testBarcodeMapper }
        val viewModel = ManualEntryWeightViewModelTestFactory()
        val pagerVm = ManualEntryPagerViewModelTestFactory()
        viewModel.weightPluEntryText.value = testManualInputDefaultValue
        viewModel.weightEntryText.value = "1"
        viewModel.manualEntryWeightUI.value = testManualEntryWeightUI

        pagerVm.onContinueButtonClicked()

        assertThat(viewModel.validationError.value).isEqualTo(ValidationError.NONE)
    }

    @Test
    fun `When onContinueButtonClicked with prepped or regular item THEN we show Quantity Picker`() {
        inlineKoinSingle { testBarcodeMapper }
        val viewModel = ManualEntryUpcViewModelTestFactory()
        val pagerVm = ManualEntryPagerViewModelTestFactory()
        viewModel.upcEntryText.value = testManualInputDefaultValue
        viewModel.manualEntryUPCUI.value = testManualEntryUpcUI

        pagerVm.onContinueButtonClicked()

        assertThat(viewModel.validationError.value).isEqualTo(ValidationError.NONE)
    }

    @Test
    fun `When valid UPC THEN validationError is NONE`() {
        inlineKoinSingle { testBarcodeMapper }
        val viewModel = ManualEntryUpcViewModelTestFactory()
        viewModel.upcEntryText.value = testManualInputDefaultValue
        viewModel.manualEntryUPCUI.value = testManualEntryUpcUI
        runBlocking {
            viewModel.validateUpcTextEntry()
        }
        assertThat(viewModel.validationError.getOrAwaitValue()).isEqualTo(ValidationError.NONE)
    }

    @Test
    fun `When invalid UPC THEN validationError is NONE`() {
        inlineKoinSingle { testBarcodeMapper }
        val viewModel = ManualEntryUpcViewModelTestFactory()
        viewModel.upcEntryText.value = ""
        runBlocking {
            viewModel.validateUpcTextEntry()
        }
        assertThat(viewModel.validationError.getOrAwaitValue()).isEqualTo(ValidationError.UPC_VALIDATION)
    }

    @Test
    fun `When valid weight THEN weightError is NONE`() {
        val viewModel = ManualEntryWeightViewModelTestFactory()
        viewModel.weightEntryText.value = "5"
        viewModel.validateWeight()
        assertThat(viewModel.weightError.getOrAwaitValue()).isEqualTo(WeightValidationError.NONE)
    }

    @Test
    fun `When invalid weight THEN weightError is WEIGHT_VALIDATION`() {
        val viewModel = ManualEntryWeightViewModelTestFactory()
        viewModel.weightEntryText.value = "150"
        viewModel.validateWeight()
        assertThat(viewModel.weightError.getOrAwaitValue()).isEqualTo(WeightValidationError.WEIGHT_VALIDATION)
    }

    @Test
    fun `When UPC error is cleared THEN validationError is NONE`() {
        inlineKoinSingle { testBarcodeMapper }
        val viewModel = ManualEntryUpcViewModelTestFactory()
        viewModel.upcEntryText.value = ""
        runBlocking {
            viewModel.validateUpcTextEntry()
        }
        assertThat(viewModel.validationError.getOrAwaitValue()).isEqualTo(ValidationError.UPC_VALIDATION)
        viewModel.clearUpcError()
        assertThat(viewModel.validationError.getOrAwaitValue()).isEqualTo(ValidationError.NONE)
    }

    @Test
    fun `When item is each and plu is valid THEN manualEntryText is set`() {
        val viewModel = ManualEntryPluViewModelTestFactory()
        viewModel.pickListItem.value = testItemEach
        viewModel.pluEntryText.value = testManualInputDefaultValue
        viewModel.manualEntryPLUUI.value = testManualEntryPluUI
        assertThat(viewModel.pluEntryText.getOrAwaitValue()).isEqualTo(testManualInputDefaultValue)
    }

    @Test
    fun `When item is each and plu is invalid THEN manualEntryText is not set`() {
        val viewModel = ManualEntryPluViewModelTestFactory()
        viewModel.pickListItem.value = testItemEachInvalid
        viewModel.pickListItem.value?.pluCode
        assertThat(viewModel.pluEntryText.getOrAwaitValue()).isEqualTo("")
    }

    fun ManualEntryUpcViewModelTestFactory(
        app: Application = com.albertsons.acupick.test.mocks.testApplicationFactory(),
    ) = ManualEntryUpcViewModel(
        app = app,
    )

    fun ManualEntryPluViewModelTestFactory(
        app: Application = com.albertsons.acupick.test.mocks.testApplicationFactory(),
    ) = ManualEntryPluViewModel(
        app = app,
    )

    fun ManualEntryWeightViewModelTestFactory(
        app: Application = com.albertsons.acupick.test.mocks.testApplicationFactory(),
    ) = ManualEntryWeightViewModel(
        app = app,
    )

    fun ManualEntryPagerViewModelTestFactory(
        app: Application = com.albertsons.acupick.test.mocks.testApplicationFactory(),
    ) = ManualEntryPagerViewModel(
        app = app
    )
}
