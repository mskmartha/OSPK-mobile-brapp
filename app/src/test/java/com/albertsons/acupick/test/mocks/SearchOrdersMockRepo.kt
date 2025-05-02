package com.albertsons.acupick.test.mocks

import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.ErOrderStatus
import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.FulfillmentSubType
import com.albertsons.acupick.data.model.FulfillmentType.DUG
import com.albertsons.acupick.data.model.User
import com.albertsons.acupick.data.model.request.UserDto
import com.albertsons.acupick.data.model.response.ContactPersonDto
import com.albertsons.acupick.data.model.response.ErDto
import com.albertsons.acupick.data.model.response.SiteDto
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.infrastructure.utils.stateFlowOf
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

val uniquePickupReadyCustomerOrderNumberResponseDto = ErDto(
    contactPersonDto = ContactPersonDto(emailId = "", firstName = "", id = "", lastName = "Johnson", phoneNumber = ""),
    customerOrderNumber = "86428642",
    fulfillment = FulfillmentAttributeDto(type = DUG),
    bagCount = 0,
    orderCount = "0",
    status = ErOrderStatus.DROPPED_OFF,
)
val uniqueNonPickupReadyCustomerOrderNumberResponseDto = ErDto(
    contactPersonDto = ContactPersonDto(emailId = "", firstName = "Jack", id = "", lastName = "Black", phoneNumber = ""),
    customerOrderNumber = "86428640",
    fulfillment = FulfillmentAttributeDto(type = DUG),
    bagCount = 0,
    orderCount = "0",
    status = ErOrderStatus.RELEASED,
)
val order12345678ResponseDto01 = ErDto(
    contactPersonDto = ContactPersonDto(emailId = "", firstName = "Jane", id = "", lastName = "Doe", phoneNumber = ""),
    customerOrderNumber = "12345678",
    fulfillment = FulfillmentAttributeDto(type = DUG),
    bagCount = 0,
    orderCount = "0",
)
val order12345678ResponseDto02 = ErDto(
    contactPersonDto = ContactPersonDto(emailId = "", firstName = "Jane", id = "", lastName = "Doe", phoneNumber = ""),
    customerOrderNumber = "12345678",
    fulfillment = FulfillmentAttributeDto(type = DUG),
    bagCount = 0,
    orderCount = "0",
)

val data =
    listOf(
        order12345678ResponseDto01,
        order12345678ResponseDto02,
        uniqueNonPickupReadyCustomerOrderNumberResponseDto,
        uniquePickupReadyCustomerOrderNumberResponseDto,
        ErDto(
            contactPersonDto = ContactPersonDto(emailId = "", firstName = "", id = "", lastName = "Williams", phoneNumber = ""),
            customerOrderNumber = "13579135",
            fulfillment = FulfillmentAttributeDto(type = DUG),
            bagCount = 0,
            orderCount = "0",
        ),
        ErDto(
            contactPersonDto = ContactPersonDto(emailId = "", firstName = "Drew", id = "", lastName = "", phoneNumber = ""),
            customerOrderNumber = "97531975",
            fulfillment = FulfillmentAttributeDto(type = DUG),
            assignedTo = UserDto(userId = "8304636844"),
            bagCount = 0,
            orderCount = "0",
        ),
    )

val baseData1 = ErDto(
    contactPersonDto = ContactPersonDto(emailId = "", firstName = "", id = "", lastName = "Williams", phoneNumber = ""),
    customerOrderNumber = "13579135",
    fulfillment = FulfillmentAttributeDto(type = DUG),
    bagCount = 0,
    orderCount = "0",
    erId = 1234,
    siteId = "2345"
)

val baseData2 = ErDto(
    contactPersonDto = ContactPersonDto(emailId = "", firstName = "Drew", id = "", lastName = "", phoneNumber = ""),
    customerOrderNumber = "97531975",
    assignedTo = UserDto(userId = "blerp", firstName = "Blerp", lastName = "Glerp"),
    fulfillment = FulfillmentAttributeDto(type = DUG),
    bagCount = 0,
    orderCount = "0",
    erId = 2345,
    siteId = "1234"
)

val searchOrderReadyData1 = ErDto(
    contactPersonDto = ContactPersonDto(emailId = "", firstName = "Drew", id = "", lastName = "", phoneNumber = ""),
    customerOrderNumber = "97531975",
    assignedTo = UserDto(userId = "blerp", firstName = "Blerp", lastName = "Glerp"),
    fulfillment = FulfillmentAttributeDto(type = DUG, subType = FulfillmentSubType.ONEPL),
    status = ErOrderStatus.DROPPED_OFF,
    bagCount = 0,
    orderCount = "0",
    erId = 2345,
    siteId = "1234"
)

val mockAps = mock<ApsRepository> {
    onBlocking {
        searchCustomerPickupOrders(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
    } doReturn ApiResult.Success(data)
    onBlocking {
        searchCustomerPickupOrders(
            firstName = "",
            lastName = "",
            orderNumber = null,
            siteId = "2941",
            onlyPickupReady = true,
        )
    } doReturn ApiResult.Success(listOf(searchOrderReadyData1))
    onBlocking {
        searchCustomerPickupOrders(
            firstName = "",
            lastName = "",
            orderNumber = null,
            siteId = "glerp",
            onlyPickupReady = true,
        )
    } doReturn ApiResult.Failure.GeneralFailure("search failed!")
    onBlocking { cancelHandoffs(listOf()) } doReturn ApiResult.Success(Unit)
}

val mockApsFailures = mock<ApsRepository> {
    onBlocking { cancelHandoffs(any()) } doReturn ApiResult.Failure.GeneralFailure("fail")
}

fun userRepoFactory(): UserRepository = mock {
    on { user } doReturn stateFlowOf(
        User(
            userId = "userId",
            firstName = "test",
            lastName = "last",
            sites = listOf(SiteDto("1000", true), SiteDto("2000", false)),
            selectedStoreId = "1000"
        )
    )
}
