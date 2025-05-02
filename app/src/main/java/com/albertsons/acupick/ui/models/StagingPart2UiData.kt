package com.albertsons.acupick.ui.models

import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.PrePickType
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ContainerActivityDto
import com.albertsons.acupick.data.model.response.fullContactName
import com.albertsons.acupick.data.model.response.getFulfillmentTypeDescriptions
import com.albertsons.acupick.data.model.response.stageByTime
import java.time.ZonedDateTime

@Suppress("DataClassPrivateConstructor")
data class StagingPart2UiData private constructor(
    val activityNo: String?,
    val customerName: String?,
    val customerOrderNumber: String?,
    val erId: Long?,
    val fulfillment: FulfillmentAttributeDto?,
    val isMultiSource: Boolean?,
    val shortOrderId: String?,
    val stageByTime: String?,
    val releasedEventDateTime: ZonedDateTime?,
    val orderType: OrderType?,
    val totalCount: Int,
    val prepickType: PrePickType? = null,
    val expectedEndDateTime: ZonedDateTime? = null,
    val isCustomerBagPreference: Boolean? = null
) {
    constructor(dto: ActivityDto, cad: ContainerActivityDto?) : this(
        activityNo = dto.activityNo,
        customerName = cad?.fullContactName() ?: dto.fullContactName(),
        customerOrderNumber = cad?.customerOrderNumber ?: dto.customerOrderNumber,
        erId = cad?.erId,
        fulfillment = cad?.fulfillment ?: dto.fulfillment,
        isMultiSource = dto.isMultiSource,
        shortOrderId = cad?.getFulfillmentTypeDescriptions() ?: dto.getFulfillmentTypeDescriptions(),
        stageByTime = dto.stageByTime(),
        releasedEventDateTime = dto.releasedEventDateTime,
        orderType = dto.orderType,
        totalCount = dto.containerActivities?.filter {
            (it.type == ContainerType.BAG || it.type == ContainerType.LOOSE_ITEM || it.type == ContainerType.TOTE) && it.customerOrderNumber == cad?.customerOrderNumber ?: dto.customerOrderNumber
        }?.size ?: 0,
        prepickType = dto.prePickType,
        expectedEndDateTime = dto.expectedEndTime,
        isCustomerBagPreference = cad?.isCustomerBagPreference
    )
}
