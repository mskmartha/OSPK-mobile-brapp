package com.albertsons.acupick.test.mocks

import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.CategoryStatus
import com.albertsons.acupick.data.model.ContainerActivityStatus
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.CustomerArrivalStatus
import com.albertsons.acupick.data.model.EntityReference
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.SubstitutionCode
import com.albertsons.acupick.data.model.request.AssignUserWrapperRequestDto
import com.albertsons.acupick.data.model.response.ActivityAndErDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ActivityDtoByCategoryDto
import com.albertsons.acupick.data.model.response.ContainerActivityDto
import com.albertsons.acupick.data.model.response.InstructionDto
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.PickedItemUpcDto
import com.albertsons.acupick.data.model.response.ScanContActDto
import com.albertsons.acupick.data.model.response.ScanContDto
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.ServerErrorCodeDto
import com.albertsons.acupick.data.model.response.ServerErrorDto
import com.albertsons.acupick.data.repository.ApsRepository
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.ZonedDateTime

// /////////////////////////////////////////////////////
// APS Repo Test Objects and Factories
// /////////////////////////////////////////////////////

val testCategoryDtoData1: ActivityAndErDto = mock {
    on { status } doReturn ActivityStatus.RELEASED
}

val testCategoryDtoData2: ActivityAndErDto = mock {
    on { status } doReturn ActivityStatus.RELEASED
    on { prevActivityId } doReturn 123654789
    on { actId } doReturn 987456321
}

val testAssignedCategoryDto: ActivityDtoByCategoryDto = mock {
    on { category } doReturn CategoryStatus.ASSIGNED
    on { data } doReturn listOf(testCategoryDtoData1)
}

val searchActivitiesSuccessfulResponse: ApiResult<List<ActivityDtoByCategoryDto>> = ApiResult.Success(
    listOf(
        testAssignedCategoryDto,
        ActivityDtoByCategoryDto(
            category = CategoryStatus.OPEN,
            data = listOf(testCategoryDtoData1)

        ),
        ActivityDtoByCategoryDto(
            category = CategoryStatus.ASSIGNED_TO_ME,
            data = listOf(testCategoryDtoData2)
        )
    )
)

val testApsRepo: ApsRepository = testApsRepoFactory()

val testApsRepoFailures: ApsRepository = testApsRepoFailuresFactory()

val testApsRepoServerFailures: ApsRepository = testApsRepoServerFailuresFactory()

val testApsRepoServerFailuresInvalidUser: ApsRepository = testApsRepoServerFailuresFactory(isUserValid = false)

val testApsRepoAssignUserToHandoffServerFailures: ApsRepository = testApsRepoAssignUserToHandoffServerFailuresFactory()

fun testApsRepoFactory(
    searchResult: ApiResult<List<ActivityDtoByCategoryDto>> = searchActivitiesSuccessfulResponse,
): ApsRepository = mock {
    onBlocking {
        searchActivities(
            "2941",
            "8304636844",
            assigned = true,
            assignedToMe = true,
            open = false,
            pickUpReady = false,
            stageByTime = null
        )
    } doReturn searchResult
    onBlocking {
        searchActivities(
            "2941",
            "8304636844",
            assigned = true,
            assignedToMe = true,
            open = true,
            pickUpReady = false,
            stageByTime = null
        )
    } doReturn searchActivitiesSuccessfulResponse
    onBlocking {
        pickUpActivityDetails(id = 1234L, loadCI = true)
    } doReturn ApiResult.Success(
        ActivityDto(
            itemActivities = listOf(
                ItemActivityDto(
                    itemId = "112233",
                    id = 123,
                    imageUrl = "www.google.com/path/to?image",
                    itemAddressDto = null,
                    customerOrderNumber = "12345678",
                    sellByWeightInd = SellByType.Prepped,
                    pluCode = "123",
                    depName = "Dept Name",
                    qty = 2.0,
                    processedQty = 0.0,
                    itemDescription = "Item Description",
                    exceptionQty = 0.0,
                    instructionDto = InstructionDto(text = "Customer Comment"),
                    subAllowed = true,
                    subCode = SubstitutionCode.SAME_BRAND_DIFF_SIZE,
                    pickedUpcCodes = listOf(
                        PickedItemUpcDto(
                            qty = 1.0,
                            containerId = "TTC33",
                        )
                    ),
                ),
                ItemActivityDto(
                    qty = 2.0,
                    processedQty = 2.0,
                    exceptionQty = 0.0,
                ),
                ItemActivityDto(
                    qty = 2.0,
                    processedQty = 1.0,
                    exceptionQty = 1.0,
                ),
                ItemActivityDto(
                    qty = 2.0,
                    processedQty = 1.0,
                    exceptionQty = 0.0,
                    pickedUpcCodes = listOf(
                        PickedItemUpcDto(
                            qty = 1.0,
                            isSubstitution = true,
                            containerId = "TTC22",
                        )
                    )
                ),
                ItemActivityDto(
                    itemId = "112233",
                    id = 123,
                    imageUrl = "www.google.com/path/to?image",
                    itemAddressDto = null,
                    customerOrderNumber = "87654321",
                    sellByWeightInd = SellByType.Prepped,
                    pluCode = "123",
                    depName = "Dept Name",
                    qty = 2.0,
                    processedQty = 0.0,
                    itemDescription = "Item Description",
                    exceptionQty = 0.0,
                    instructionDto = InstructionDto(text = "Customer Comment"),
                    subAllowed = true,
                    subCode = SubstitutionCode.SAME_BRAND_DIFF_SIZE,
                    pickedUpcCodes = listOf(
                        PickedItemUpcDto(
                            qty = 1.0,
                            containerId = "TTC33",
                        )
                    ),
                )
            ),
            actId = 0L,
            expectedEndTime = null,
            containerActivities = listOf(
                ContainerActivityDto(containerId = "ttc10"),
                ContainerActivityDto(containerId = "ttc22"),
                ContainerActivityDto(containerId = "ttz99")
            ),
            status = ActivityStatus.PRE_COMPLETED
        )
    )
    onBlocking {
        pickUpActivityDetails(id = 2345L, loadCI = true)
    } doReturn ApiResult.Failure.Server(ServerErrorDto())
    onBlocking {
        pickUpActivityDetails(id = 3456L, loadCI = true)
    } doReturn ApiResult.Failure.Server(
        ServerErrorDto(
            errorCode =
            ServerErrorCodeDto(
                resolvedType = ServerErrorCode.USER_NOT_VALID,
                rawValue = 1234
            )
        )
    )
    onBlocking {
        pickUpActivityDetails(id = 4567L, loadCI = true)
    } doReturn ApiResult.Failure.GeneralFailure("loading pickup details failed generally")
    onBlocking {
        scanContainers(anyOrNull())
    } doReturn ApiResult.Success(
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
    )
    onBlocking {
        cancelHandoff(any())
    } doReturn ApiResult.Success(Unit)
    onBlocking {
        printToteLabel(any())
    } doReturn ApiResult.Success(Unit)
    onBlocking {
        cancelHandoffs(any())
    } doReturn ApiResult.Success(Unit)
}

fun testApsRepoFailuresFactory(): ApsRepository = mock {
    onBlocking {
        cancelHandoffs(any())
    } doReturn ApiResult.Failure.GeneralFailure("Failed Handoffs")
    onBlocking {
        scanContainers(anyOrNull())
    } doReturn ApiResult.Failure.GeneralFailure("Failed scan containers")
}

fun testApsRepoServerFailuresFactory(isUserValid: Boolean = true): ApsRepository = mock {
    onBlocking {
        scanContainers(anyOrNull())
    } doReturn if (isUserValid) {
        ApiResult.Failure.Server(error = ServerErrorDto(errorCode = ServerErrorCodeDto(rawValue = 1, resolvedType = ServerErrorCode.UNKNOWN_SERVER_ERROR_CODE)))
    } else {
        ApiResult.Failure.Server(error = ServerErrorDto(errorCode = ServerErrorCodeDto(rawValue = 1, resolvedType = ServerErrorCode.USER_NOT_VALID)))
    }
}

fun testApsRepoAssignUserToHandoffServerFailuresFactory(): ApsRepository = mock {
    onBlocking {
        assignUserToHandoffs(
            AssignUserWrapperRequestDto(
                actIds = listOf(1234L),
                replaceOverride = false,
                user = null
            )
        )
    } doReturn ApiResult.Failure.Server(ServerErrorDto())
}
