package com.albertsons.acupick.test.mocks

import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.ItemSearchResult
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.SubstitutionCode
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.request.AssignUserRequestDto
import com.albertsons.acupick.data.model.request.UserDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ContainerActivityDto
import com.albertsons.acupick.data.model.response.InstructionDto
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.ItemDetailDto
import com.albertsons.acupick.data.model.response.PickedItemUpcDto
import com.albertsons.acupick.data.model.response.ServerErrorDto
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.infrastructure.utils.stateFlowOf
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

val testInstruction: InstructionDto = mock {
    on { text } doReturn "Customer Comment"
}

val testPickedUpcDto: PickedItemUpcDto = mock {
    on { qty } doReturn 1.0
    on { containerId } doReturn "TTC33"
}

val testSubstitutedPickedUpcDto: PickedItemUpcDto = mock {
    on { qty } doReturn 1.0
    on { isSubstitution } doReturn true
    on { containerId } doReturn "TTC22"
}

val testItem: ItemActivityDto = mock {
    on { itemId } doReturn "112233"
    on { id } doReturn 123
    on { imageUrl } doReturn "www.google.com/path/to?image"
    on { itemAddressDto } doReturn null
    on { customerOrderNumber } doReturn "12345678"
    on { sellByWeightInd } doReturn SellByType.Prepped
    on { pluCode } doReturn "123"
    on { depName } doReturn "Dept Name"
    on { qty } doReturn 2.0
    on { processedQty } doReturn 0.0
    on { itemDescription } doReturn "Item Description"
    on { exceptionQty } doReturn 0.0
    on { instructionDto } doReturn testInstruction
    on { subAllowed } doReturn true
    on { subCode } doReturn SubstitutionCode.ONLY_USE_SUGGESTED_SUB
    on { pickedUpcCodes } doReturn listOf(testPickedUpcDto)
}

val testItemWithoutOrderOrCustomerDetails: ItemSearchResult.MatchedItem = mock {
    on { itemActivityDto } doReturn testItem6
}

val testItem2: ItemActivityDto = mock {
    on { qty } doReturn 2.0
    on { processedQty } doReturn 2.0
    on { exceptionQty } doReturn 0.0
}

val testItem3: ItemActivityDto = mock {
    on { qty } doReturn 2.0
    on { processedQty } doReturn 1.0
    on { exceptionQty } doReturn 1.0
}

val testItem4: ItemActivityDto = mock {
    on { qty } doReturn 2.0
    on { processedQty } doReturn 1.0
    on { exceptionQty } doReturn 0.0
    on { pickedUpcCodes } doReturn listOf(testSubstitutedPickedUpcDto)
}

val testItem5: ItemActivityDto = mock {
    on { itemId } doReturn "112233"
    on { id } doReturn 123
    on { imageUrl } doReturn "www.google.com/path/to?image"
    on { itemAddressDto } doReturn null
    on { customerOrderNumber } doReturn "87654321"
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
}

val testItem6: ItemActivityDto = mock {
    on { itemId } doReturn "112233"
    on { id } doReturn 123
    on { imageUrl } doReturn "www.google.com/path/to?image"
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
}

val testContainerActivities: List<ContainerActivityDto> = listOf(
    ContainerActivityDto(containerId = "ttc10"),
    ContainerActivityDto(containerId = "ttc22"),
    ContainerActivityDto(containerId = "ttz99")
)

val testActivity: ActivityDto = mock {
    on { itemActivities } doReturn listOf(testItem, testItem2, testItem3, testItem4, testItem5)
    on { actId } doReturn 0
    on { expectedEndTime } doReturn null
    on { containerActivities } doReturn testContainerActivities
    on { status } doReturn ActivityStatus.PRE_COMPLETED
}

val testActivityWithAssignedTo: ActivityDto = mock {
    on { itemActivities } doReturn listOf(testItem, testItem2, testItem3, testItem4, testItem5)
    on { actId } doReturn 0
    on { expectedEndTime } doReturn null
    on { containerActivities } doReturn testContainerActivities
    on { status } doReturn ActivityStatus.PRE_COMPLETED
    on { assignedTo } doReturn UserDto(userId = "8304636844")
}

val testActivityInProgress: ActivityDto = mock {
    on { itemActivities } doReturn listOf(testItem, testItem2, testItem3, testItem4, testItem5)
    on { actId } doReturn 0
    on { expectedEndTime } doReturn null
    on { containerActivities } doReturn testContainerActivities
    on { status } doReturn ActivityStatus.IN_PROGRESS
}

val testItemDetailDto: ItemDetailDto = mock {
    on { imageUrl } doReturn "image_url"
    on { itemDesc } doReturn "item_desc"
    on { itemId } doReturn "item_id"
}

fun testPickRepository(localTestActivity: ActivityDto = testActivity): PickRepository =
    mock {
        on { pickList } doReturn stateFlowOf(localTestActivity)
        on { iaIdAfterSwapSubstitution } doReturn stateFlowOf(1)
        on { masterPickList } doReturn stateFlowOf(localTestActivity)
        onBlocking { getItem(any<BarcodeType.Item>(), any()) } doReturn testItem
        onBlocking { getItemWithoutOrderOrCustomerDetails(any()) } doReturn testItemWithoutOrderOrCustomerDetails
        onBlocking { getItemDetails(any(), any(), any(), any(), any(), any(), any()) } doReturn ApiResult.Success(testItemDetailDto)
        onBlocking { getSubstitutionItemDetailList(any()) } doReturn ApiResult.Success(emptyList())
        onBlocking { getActivityDetails(any(), any()) } doReturn ApiResult.Success(localTestActivity)
        onBlocking { hasOfflinePickListData() } doReturn false
        onBlocking { getItemUpcList(any(), any(), any()) } doReturn ApiResult.Success(emptyList())
        onBlocking { recordShortage(any()) } doReturn ApiResult.Success(Unit)
        onBlocking { unAssignUser(any(), any(), any(), any()) } doReturn ApiResult.Success(Unit)
        onBlocking { findExistingValidToteForItem(any()) } doReturn null
        on { hasActivePickListActivityId() } doReturn true
        on { getActivePickListActivityId() } doReturn "123456"
        onBlocking {
            assignUser(
                AssignUserRequestDto(
                    actId = 1234L,
                    replaceOverride = false,
                    user = null
                )
            )
        } doReturn ApiResult.Failure.Server(ServerErrorDto())
        onBlocking {
            assignUser(
                AssignUserRequestDto(
                    actId = 3456L,
                    replaceOverride = false,
                    user = null
                )
            )
        } doReturn ApiResult.Success(
            ActivityDto(
                erId = 3456L,
                contactFirstName = "Blerp",
                contactLastName = "Glerp"
            )
        )
    }
