package com.albertsons.acupick.data.picklist

import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.barcode.PickingContainer
import com.albertsons.acupick.data.model.response.ContainerActivityDto
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.test.BaseTest
import com.albertsons.acupick.data.test.mocks.testBatchContainerAAA01
import com.albertsons.acupick.data.test.mocks.testBatchContainerAAA02
import com.albertsons.acupick.data.test.mocks.testBatchContainerActivities
import com.albertsons.acupick.data.test.mocks.testBatchContainerBBB01
import com.albertsons.acupick.data.test.mocks.testBatchContainerBBB22
import com.albertsons.acupick.data.test.mocks.testBatchItemAmA
import com.albertsons.acupick.data.test.mocks.testBatchItemAmB
import com.albertsons.acupick.data.test.mocks.testSingleOrderContainerAAA01
import com.albertsons.acupick.data.test.mocks.testSingleOrderContainerAAA02
import com.albertsons.acupick.data.test.mocks.testSingleOrderContainerActivities
import com.albertsons.acupick.data.test.mocks.testSingleOrderContainerBBB01
import com.albertsons.acupick.data.test.mocks.testSingleOrderItemAm
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.mock

@RunWith(Enclosed::class)
class PickListOperationsImplementationTest {

    @RunWith(Parameterized::class)
    class FindExistingValidToteParameterizedTests(
        private val testName: String,
        private val totes: List<ContainerActivityDto>,
        private val item: ItemActivityDto,
        private val output: ContainerActivityDto?,
    ) :
        BaseTest() {

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = listOf(
                arrayOf(
                    "WHEN there are no containers THEN return null",
                    emptyList<ContainerActivityDto>(),
                    testSingleOrderItemAm,
                    null,
                ),
                arrayOf(
                    "WHEN a single order item has a temp zone not in the container list THEN return null",
                    listOf(testSingleOrderContainerBBB01),
                    testSingleOrderItemAm,
                    null,
                ),
                arrayOf(
                    "WHEN a single order item with matching temp zone for container THEN return last matching container",
                    testSingleOrderContainerActivities,
                    testSingleOrderItemAm,
                    testSingleOrderContainerAAA02
                ),
                arrayOf(
                    "WHEN a batch order item has a matching temp zone but mismatched customer order number in the container list THEN return null",
                    listOf(testBatchContainerBBB01),
                    testBatchItemAmA,
                    null,
                ),
                arrayOf(
                    "WHEN a batch order item has a temp zone not in the container list THEN return null",
                    listOf(testBatchContainerBBB22),
                    testBatchItemAmB,
                    null,
                ),
                arrayOf(
                    "WHEN a batch order item with matching temp zone and matching customer order number for container THEN return last matching container",
                    testBatchContainerActivities,
                    testBatchItemAmA,
                    testBatchContainerAAA02,
                ),
            )
        }

        @Test
        fun findExistingValidToteForItem_givenInput_returnsExpectedResult() {
            val sut = PickListOperationsImplementation(mock {})
            val result = sut.findExistingValidToteForItem(totes, item)
            assertThat(result).isEqualTo(output)
        }
    }

    @RunWith(Parameterized::class)
    class IsToteValidForItemParameterizedTests(
        private val testName: String,
        private val item: ItemActivityDto?,
        private val pickingContainers: List<ContainerActivityDto>,
        private val pickingContainerType: PickingContainer,
        private val shouldUseMfcToteLicencePlate: Boolean,
        private val output: Boolean,
    ) : BaseTest() {

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = listOf(
                arrayOf(
                    "WHEN the a new tote is scanned and there are no containers associated with the picklist yet THEN return true since this would be a new tote",
                    testSingleOrderItemAm,
                    emptyList<ContainerActivityDto>(),
                    BarcodeType.Tote("ABC12"),
                    false,
                    true,
                ),
                arrayOf(
                    "WHEN a single order scanned tote is part the picklist but with mismatched item/container temp zones THEN return false",
                    testSingleOrderItemAm,
                    listOf(testSingleOrderContainerBBB01),
                    BarcodeType.Tote("BBB01"),
                    false,
                    false,
                ),
                arrayOf(
                    "WHEN a single order scanned tote is part the picklist with matching item/container temp zones THEN return true",
                    testSingleOrderItemAm,
                    listOf(testSingleOrderContainerAAA01),
                    BarcodeType.Tote("AAA01"),
                    false,
                    true,
                ),
                arrayOf(
                    "WHEN a batch order scanned tote is part the picklist with mismatched customer order number but matching item/container temp zones THEN return false",
                    testBatchItemAmB,
                    listOf(testBatchContainerAAA01),
                    BarcodeType.Tote("AAA01"),
                    false,
                    false,
                ),
                arrayOf(
                    "WHEN a batch order scanned tote is part the picklist with matching customer order number but mismatched item/container temp zones THEN return false",
                    testBatchItemAmB,
                    listOf(testBatchContainerBBB22),
                    BarcodeType.Tote("BBB22"),
                    false,
                    false,
                ),
                arrayOf(
                    "WHEN a batch order scanned tote is part the picklist with matching customer order number and item/container temp zones THEN return true",
                    testBatchItemAmA,
                    listOf(testBatchContainerAAA01),
                    BarcodeType.Tote("AAA01"),
                    false,
                    true,
                ),
                arrayOf(
                    "WHEN an item's storage type matches the supported the storage THEN it should return true",
                    testSingleOrderItemAm,
                    emptyList<ContainerActivityDto>(),
                    BarcodeType.MfcPickingToteLicensePlate("99800417003788", listOf(StorageType.AM)),
                    true,
                    true,
                ),
                arrayOf(
                    "WHEN an items storage type does not match the supported storage type THEN it should return false",
                    testSingleOrderItemAm,
                    emptyList<ContainerActivityDto>(),
                    BarcodeType.Tote("AAA01"),
                    true,
                    false,
                ),
                arrayOf(
                    "WHEN an order should use an MFC tote license plate and the barcode format matches THEN it should return true",
                    testSingleOrderItemAm,
                    emptyList<ContainerActivityDto>(),
                    BarcodeType.MfcPickingToteLicensePlate("99800417003788", listOf(StorageType.AM)),
                    true,
                    true,
                ),
                arrayOf(
                    "WHEN an order should not use an mfc tote license plate and a normal tote is used THEN it should return true",
                    testSingleOrderItemAm,
                    emptyList<ContainerActivityDto>(),
                    BarcodeType.Tote("AAA01"),
                    false,
                    true,
                ),
                arrayOf(
                    "WHEN an order should not use an mfc tote license plate and something other than a normal tote is used THEN it should return false",
                    testSingleOrderItemAm,
                    emptyList<ContainerActivityDto>(),
                    BarcodeType.MfcPickingToteLicensePlate("99800417003788", listOf(StorageType.AM)),
                    false,
                    false,
                ),
            )
        }

        @Test
        fun findExistingValidToteForItem_givenInput_returnsExpectedResult() {
            val sut = PickListOperationsImplementation(mock {})
            val result = sut.isItemIntoPickingContainerValid(item, pickingContainers, pickingContainerType, shouldUseMfcToteLicencePlate)
            assertThat(result).isEqualTo(output)
        }
    }
}
