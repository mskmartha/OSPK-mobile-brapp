package com.albertsons.acupick.ui.home

import com.albertsons.acupick.data.model.ActivityType
import com.albertsons.acupick.data.model.DomainModel
import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.PrePickType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.ToteEstimate
import com.albertsons.acupick.data.model.isAdvancePickOrPrePick
import com.albertsons.acupick.data.model.response.ActivityAndErDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.customerNameForHomeCard
import com.albertsons.acupick.data.model.response.fullContactName
import com.albertsons.acupick.data.model.response.isWineOrder
import com.albertsons.acupick.data.model.response.stageByTime
import com.albertsons.acupick.ui.models.CustomerArrivalStatusUI
import com.albertsons.acupick.ui.models.convertArrivalStatus
import com.albertsons.acupick.ui.util.getOrEmpty
import com.albertsons.acupick.ui.util.getOrZero
import java.time.ZonedDateTime

/** Represents common data to render the home card from [ActivityDto] and [ActivityAndErDto] */
data class HomeCardData(
    // Pick List
    val actId: Long?,
    val actType: ActivityType?,
    val activityNo: String?,
    val erId: Long?,
    val expectedEndTime: ZonedDateTime?,
    val fulfillment: FulfillmentAttributeDto?,
    val reProcess: Boolean?,
    val prepNotReady: Boolean? = null, // ActivityDto does not carry isPrepNeeded value only ActivityAndErDto.
    val isAssignedToMe: Boolean,
    val isOrderInStagePhase: Boolean,
    val itemQty: Long?,
    val prevActivityId: Long?,
    val storageTypes: List<StorageType>,
    val isWineOrder: Boolean = false,
    val contactNameForWineShipping: String?, // TODO Need to remove this while re designing wine shipping scenario(Use contactName instead).
    val contactName: String?,
    val shortOrderNumber: String?,
    val stageByTime: String?,
    val entityReference: String?,
    // val pickedBottleCount: String?, // TODO :find how to gather form home screen
    // Order for Hand Off
    val itemCount: Int?,
    val bagCount: String?,
    val looseItemCount: String?,
    val toteCount: String?,
    val isShowBag: Boolean,
    val isShowLoose: Boolean,
    val isShowTote: Boolean,
    val contactFirstName: String?,
    val customerOrderNumber: String?,
    val isOrderReadyToPickUp: Boolean,
    val customerArrivalTime: ZonedDateTime?,
    val customerArrivalStatusUI: CustomerArrivalStatusUI?,
    val countDownTimer: String?,
    val prepickType: PrePickType? = null,
    val isPrePickOrAdvancePick: Boolean,
    val source: String?,
    val is3p: Boolean?,
    // val eta: ZonedDateTime,
    val toteEstimate: ToteEstimate? = null,
    val isMultiSource: Boolean? = null,
    val feScreenStatus: String? = null,
    val rejectedItemsCount: Int? = null,
    val vanNumber: String? = null,
    val vanDepartureTime: ZonedDateTime? = null,
    val is1Pl: Boolean? = null
) : DomainModel {

    constructor(userId: String, dto: ActivityDto) : this(
        actId = dto.actId,
        actType = dto.actType,
        activityNo = dto.activityNo,
        erId = dto.erId,
        expectedEndTime = dto.expectedEndTime,
        fulfillment = dto.fulfillment,
        isAssignedToMe = dto.assignedTo?.userId == userId,
        isOrderInStagePhase = dto.actType == ActivityType.DROP_OFF && dto.assignedTo?.userId == userId,
        itemQty = dto.itemQty?.toLong(),
        prevActivityId = dto.prevActivityId,
        reProcess = dto.reProcess,
        storageTypes = dto.storageTypes.orEmpty(),
        isWineOrder = dto.isWineOrder(),
        contactNameForWineShipping = dto.fullContactName(),
        contactName = dto.customerNameForHomeCard(),
        shortOrderNumber = dto.shortOrderNumber.orEmpty(),
        stageByTime = dto.stageByTime().orEmpty(),
        entityReference = dto.entityReference?.entityId.orEmpty(),
        itemCount = dto.expectedCount ?: 0,
        bagCount = dto.bagCount.getOrEmpty(),
        looseItemCount = dto.looseItemCount.getOrEmpty(),
        toteCount = dto.toteCount.getOrEmpty(),
        isShowBag = dto.bagCount.getOrZero() > 0 && (dto.actType == ActivityType.PICKUP || dto.actType == ActivityType.THREEPL_PICKUP),
        isShowLoose = dto.looseItemCount.getOrZero() > 0 && (dto.actType == ActivityType.PICKUP || dto.actType == ActivityType.THREEPL_PICKUP),
        isShowTote = dto.toteCount.getOrZero() > 0 && (dto.actType == ActivityType.PICKUP || dto.actType == ActivityType.THREEPL_PICKUP),
        contactFirstName = dto.contactFirstName,
        customerOrderNumber = dto.customerOrderNumber,
        isOrderReadyToPickUp = dto.actType == ActivityType.PICKUP || dto.actType == ActivityType.THREEPL_PICKUP,
        customerArrivalTime = dto.nextActExpStartTime,
        customerArrivalStatusUI = convertArrivalStatus(dto.subStatus),
        countDownTimer = dto.stageByTime(),
        prepickType = dto.prePickType,
        isPrePickOrAdvancePick = dto.prePickType.isAdvancePickOrPrePick(),
        source = dto.source,
        is3p = dto.is3p,
        feScreenStatus = dto.feScreenStatus
    )

    constructor(userId: String, dto: ActivityAndErDto) : this(
        actId = dto.actId,
        actType = dto.actType,
        activityNo = dto.activityNo,
        erId = dto.erId,
        expectedEndTime = dto.expectedEndTime,
        fulfillment = dto.fulfillment,
        isAssignedToMe = dto.assignedTo?.userId == userId,
        isOrderInStagePhase = dto.actType == ActivityType.DROP_OFF && dto.assignedTo?.userId == userId,
        itemQty = dto.itemQty?.toLong(),
        prevActivityId = dto.prevActivityId,
        reProcess = dto.reProcess,
        prepNotReady = dto.isPrepNeeded,
        storageTypes = dto.storageTypes.orEmpty(),
        isWineOrder = dto.isWineOrder(),
        contactNameForWineShipping = dto.fullContactName(),
        contactName = dto.customerNameForHomeCard(),
        shortOrderNumber = dto.shortOrderNumber.orEmpty(), // TODO: change this
        stageByTime = dto.stageByTime().orEmpty(),
        entityReference = dto.entityReference?.entityId.orEmpty(),
        itemCount = dto.expectedCount ?: 0,
        bagCount = dto.bagCount.getOrEmpty(),
        looseItemCount = dto.looseItemCount.getOrEmpty(),
        toteCount = dto.toteCount.getOrEmpty(),
        isShowBag = dto.bagCount.getOrZero() > 0 && (dto.actType == ActivityType.PICKUP || dto.actType == ActivityType.THREEPL_PICKUP),
        isShowLoose = dto.looseItemCount.getOrZero() > 0 && (dto.actType == ActivityType.PICKUP || dto.actType == ActivityType.THREEPL_PICKUP),
        isShowTote = dto.toteCount.getOrZero() > 0 && (dto.actType == ActivityType.PICKUP || dto.actType == ActivityType.THREEPL_PICKUP),
        contactFirstName = dto.contactFirstName,
        customerOrderNumber = dto.customerOrderNumber,
        isOrderReadyToPickUp = dto.actType == ActivityType.PICKUP || dto.actType == ActivityType.THREEPL_PICKUP,
        customerArrivalTime = dto.vanDepartureTime ?: dto.nextActExpStartTime,
        customerArrivalStatusUI = convertArrivalStatus(dto.subStatus),
        countDownTimer = dto.stageByTime(),
        prepickType = dto.prePickType,
        isPrePickOrAdvancePick = dto.prePickType.isAdvancePickOrPrePick(),
        source = dto.source,
        is3p = dto.is3p,
        // eta = ZonedDateTime.parse("2023-09-29T08:15:17.000Z"),
        toteEstimate = dto.toteEstimate,
        isMultiSource = dto.isMultiSource,
        feScreenStatus = dto.feScreenStatus,
        rejectedItemsCount = dto.itemQtyToRemove,
        vanNumber = dto.routeVanNumber,
        vanDepartureTime = dto.vanDepartureTime,
        is1Pl = dto.actType == ActivityType.ONEPL_PICKUP
    )
}
