package com.albertsons.acupick.ui.models

import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.PrePickType
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ContainerActivityDto
import com.albertsons.acupick.data.model.response.asFirstInitialDotLastString
import com.albertsons.acupick.data.model.response.fullContactName
import com.albertsons.acupick.data.model.response.getFulfillmentTypeDescriptions
import com.albertsons.acupick.data.model.response.stageByTime
import java.time.ZonedDateTime

data class StagingUI(
    val activityNo: String?,
    val customerFirstInitialDotLast: String?,
    val customerName: String?,
    // TODO - This change this from DTO to UI model.
    val fulfillment: FulfillmentAttributeDto?,
    val orderNumber: String?,
    val shortOrderId: String?,
    val stageByTime: String?,
    val releasedEventDateTime: ZonedDateTime?,
    val stopNumber: String?,
    val orderType: OrderType?,
    val isOrderMultiSource: Boolean? = null,
    val prepickType: PrePickType? = null,
    val expectedEndDateTime: ZonedDateTime? = null,
    val isCustomerBagPreference: Boolean? = null
) : UIModel {
    constructor(dto: ActivityDto, cad: ContainerActivityDto?) : this(
        activityNo = dto.activityNo,
        customerFirstInitialDotLast = cad?.asFirstInitialDotLastString(),
        customerName = cad?.fullContactName() ?: dto.fullContactName(),
        fulfillment = cad?.fulfillment ?: dto.fulfillment,
        orderNumber = cad?.customerOrderNumber ?: dto.customerOrderNumber,
        shortOrderId = cad?.getFulfillmentTypeDescriptions() ?: dto.getFulfillmentTypeDescriptions(),
        stageByTime = dto.stageByTime(),
        releasedEventDateTime = dto.releasedEventDateTime,
        stopNumber = cad?.stopNumber ?: dto.stopNumber,
        orderType = dto.orderType,
        isOrderMultiSource = dto.isMultiSource,
        prepickType = dto.prePickType,
        expectedEndDateTime = dto.expectedEndTime,
        isCustomerBagPreference = cad?.isCustomerBagPreference
    )
}
