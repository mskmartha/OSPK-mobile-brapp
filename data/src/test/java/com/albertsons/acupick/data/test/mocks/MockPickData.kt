package com.albertsons.acupick.data.test.mocks

import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.SubstitutionCode
import com.albertsons.acupick.data.model.response.ContainerActivityDto
import com.albertsons.acupick.data.model.response.ItemActivityDto
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

val testSingleOrderItemAm: ItemActivityDto = mock {
    on { itemId } doReturn "112233"
    on { id } doReturn 123
    on { imageUrl } doReturn "www.google.com/path/to?image"
    on { itemAddressDto } doReturn null
    on { contactFirstName } doReturn "ATest"
    on { contactLastName } doReturn "ATester"
    on { customerOrderNumber } doReturn "11223344"
    on { storageType } doReturn StorageType.AM
    on { sellByWeightInd } doReturn SellByType.Prepped
    on { pluCode } doReturn "123"
    on { depName } doReturn "Dept Name"
    on { qty } doReturn 2.0
    on { processedQty } doReturn 0.0
    on { itemDescription } doReturn "Item Description"
    on { exceptionQty } doReturn 0.0
    on { subAllowed } doReturn true
    on { subCode } doReturn SubstitutionCode.SAME_BRAND_DIFF_SIZE
}

val testSingleOrderItemCh: ItemActivityDto = mock {
    on { itemId } doReturn "223344"
    on { id } doReturn 234
    on { imageUrl } doReturn "www.google.com/path/to?image"
    on { itemAddressDto } doReturn null
    on { contactFirstName } doReturn "ATest"
    on { contactLastName } doReturn "ATester"
    on { customerOrderNumber } doReturn "11223344"
    on { storageType } doReturn StorageType.CH
    on { sellByWeightInd } doReturn SellByType.Prepped
    on { pluCode } doReturn "123"
    on { depName } doReturn "Dept Name"
    on { qty } doReturn 2.0
    on { processedQty } doReturn 0.0
    on { itemDescription } doReturn "Item Description"
    on { exceptionQty } doReturn 0.0
    on { subAllowed } doReturn true
    on { subCode } doReturn SubstitutionCode.SAME_BRAND_DIFF_SIZE
}

val testSingleOrderItemFz: ItemActivityDto = mock {
    on { itemId } doReturn "334455"
    on { id } doReturn 345
    on { imageUrl } doReturn "www.google.com/path/to?image"
    on { itemAddressDto } doReturn null
    on { contactFirstName } doReturn "ATest"
    on { contactLastName } doReturn "ATester"
    on { customerOrderNumber } doReturn "11223344"
    on { storageType } doReturn StorageType.FZ
    on { sellByWeightInd } doReturn SellByType.Prepped
    on { pluCode } doReturn "123"
    on { depName } doReturn "Dept Name"
    on { qty } doReturn 2.0
    on { processedQty } doReturn 0.0
    on { itemDescription } doReturn "Item Description"
    on { exceptionQty } doReturn 0.0
    on { subAllowed } doReturn true
    on { subCode } doReturn SubstitutionCode.SAME_BRAND_DIFF_SIZE
}

val testBatchItemAmA: ItemActivityDto = mock {
    on { itemId } doReturn "112233"
    on { id } doReturn 123
    on { imageUrl } doReturn "www.google.com/path/to?image"
    on { itemAddressDto } doReturn null
    on { contactFirstName } doReturn "ATest"
    on { contactLastName } doReturn "Tester"
    on { customerOrderNumber } doReturn "11223344"
    on { storageType } doReturn StorageType.AM
    on { sellByWeightInd } doReturn SellByType.Prepped
    on { pluCode } doReturn "123"
    on { depName } doReturn "Dept Name"
    on { qty } doReturn 2.0
    on { processedQty } doReturn 0.0
    on { itemDescription } doReturn "Item Description"
    on { exceptionQty } doReturn 0.0
    on { subAllowed } doReturn true
    on { subCode } doReturn SubstitutionCode.SAME_BRAND_DIFF_SIZE
}

val testBatchItemAmB: ItemActivityDto = mock {
    on { itemId } doReturn "223344"
    on { id } doReturn 234
    on { imageUrl } doReturn "www.google.com/path/to?image"
    on { itemAddressDto } doReturn null
    on { contactFirstName } doReturn "BTest"
    on { contactLastName } doReturn "BTester"
    on { customerOrderNumber } doReturn "22334455"
    on { storageType } doReturn StorageType.AM
    on { sellByWeightInd } doReturn SellByType.Prepped
    on { pluCode } doReturn "123"
    on { depName } doReturn "Dept Name"
    on { qty } doReturn 2.0
    on { processedQty } doReturn 0.0
    on { itemDescription } doReturn "Item Description"
    on { exceptionQty } doReturn 0.0
    on { subAllowed } doReturn true
    on { subCode } doReturn SubstitutionCode.SAME_BRAND_DIFF_SIZE
}
val testBatchItemChB: ItemActivityDto = mock {
    on { itemId } doReturn "223344"
    on { id } doReturn 234
    on { imageUrl } doReturn "www.google.com/path/to?image"
    on { itemAddressDto } doReturn null
    on { contactFirstName } doReturn "BTest"
    on { contactLastName } doReturn "BTester"
    on { customerOrderNumber } doReturn "22334455"
    on { storageType } doReturn StorageType.CH
    on { sellByWeightInd } doReturn SellByType.Prepped
    on { pluCode } doReturn "123"
    on { depName } doReturn "Dept Name"
    on { qty } doReturn 2.0
    on { processedQty } doReturn 0.0
    on { itemDescription } doReturn "Item Description"
    on { exceptionQty } doReturn 0.0
    on { subAllowed } doReturn true
    on { subCode } doReturn SubstitutionCode.SAME_BRAND_DIFF_SIZE
}

val testBatchItemAmC: ItemActivityDto = mock {
    on { itemId } doReturn "334455"
    on { id } doReturn 345
    on { imageUrl } doReturn "www.google.com/path/to?image"
    on { itemAddressDto } doReturn null
    on { contactFirstName } doReturn "CTest"
    on { contactLastName } doReturn "CTester"
    on { customerOrderNumber } doReturn "33445566"
    on { storageType } doReturn StorageType.AM
    on { sellByWeightInd } doReturn SellByType.Prepped
    on { pluCode } doReturn "123"
    on { depName } doReturn "Dept Name"
    on { qty } doReturn 2.0
    on { processedQty } doReturn 0.0
    on { itemDescription } doReturn "Item Description"
    on { exceptionQty } doReturn 0.0
    on { subAllowed } doReturn true
    on { subCode } doReturn SubstitutionCode.SAME_BRAND_DIFF_SIZE
}

val testSingleOrderContainerAAA01 =
    ContainerActivityDto(containerId = "AAA01", containerType = StorageType.AM, customerOrderNumber = "11223344", contactFirstName = "ATest", contactLastName = "ATester")
val testSingleOrderContainerAAA02 =
    ContainerActivityDto(containerId = "AAA02", containerType = StorageType.AM, customerOrderNumber = "11223344", contactFirstName = "ATest", contactLastName = "ATester")
val testSingleOrderContainerBBB01 =
    ContainerActivityDto(containerId = "BBB01", containerType = StorageType.CH, customerOrderNumber = "11223344", contactFirstName = "ATest", contactLastName = "ATester")
val testSingleOrderContainerCCC01 =
    ContainerActivityDto(containerId = "CCC01", containerType = StorageType.FZ, customerOrderNumber = "11223344", contactFirstName = "ATest", contactLastName = "ATester")

val testBatchContainerAAA01 = ContainerActivityDto(containerId = "AAA01", containerType = StorageType.AM, customerOrderNumber = "11223344", contactFirstName = "ATest", contactLastName = "ATester")
val testBatchContainerAAA02 = ContainerActivityDto(containerId = "AAA02", containerType = StorageType.AM, customerOrderNumber = "11223344", contactFirstName = "ATest", contactLastName = "ATester")
val testBatchContainerBBB01 = ContainerActivityDto(containerId = "BBB01", containerType = StorageType.AM, customerOrderNumber = "22334455", contactFirstName = "BTest", contactLastName = "BTester")
val testBatchContainerCCC01 = ContainerActivityDto(containerId = "CCC01", containerType = StorageType.AM, customerOrderNumber = "33445566", contactFirstName = "CTest", contactLastName = "CTester")
val testBatchContainerBBB22 = ContainerActivityDto(containerId = "BBB22", containerType = StorageType.CH, customerOrderNumber = "22334455", contactFirstName = "BTest", contactLastName = "BTester")

val testSingleOrderContainerActivities: List<ContainerActivityDto> = listOf(
    testSingleOrderContainerAAA01,
    testSingleOrderContainerAAA02,
    testSingleOrderContainerBBB01,
    testSingleOrderContainerCCC01,
)

val testBatchContainerActivities: List<ContainerActivityDto> = listOf(
    testBatchContainerAAA01,
    testBatchContainerAAA02,
    testBatchContainerBBB01,
    testBatchContainerCCC01,
)
