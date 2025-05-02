package com.albertsons.acupick.ui.models

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.EntityReference
import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.RejectedItemsByStorageType
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.GiftMessageDto
import com.albertsons.acupick.data.model.response.asFirstInitialDotLastString
import com.albertsons.acupick.data.model.response.asFirstNameLastInitial
import com.albertsons.acupick.data.model.response.getRxBag
import com.albertsons.acupick.data.model.response.hasAddOnPrescription
import com.albertsons.acupick.data.model.response.isRxDug
import com.albertsons.acupick.ui.arrivals.destage.DetailsHeaderUi
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.toFulfillmentTypeUI
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@Parcelize
@Keep
data class DestageOrderUiData(
    val actId: Long?,
    val activityNo: String?,
    val erId: Long?,
    val customerName: String?,
    val customerFistNameLastInitial: String,
    val customerOrderNumber: String?,
    val detailsHeaderUi: DetailsHeaderUi,
    val zonedBags: List<ZonedBagsScannedData>,
    val rejectedItemCount: List<RejectedItemsByStorageType> = emptyList(),
    val totalCount: Int,
    val type: ContainerType,
    val isDugOrder: Boolean,
    val isMultiSource: Boolean?,
    val entityReference: EntityReference?,
    val rxBags: List<RxBagUI>?,
    val hasAddOnPrescription: Boolean?,
    val customerArrivlaTime: ZonedDateTime?,
    val fulfillmentTypeUI: FulfillmentTypeUI?,
    val customerArrivalStatusUI: CustomerArrivalStatusUI?,
    val feScreenStatus: String?,
    val isCustomerBagPreference: Boolean?,
    val isRxDug: Boolean,
    val isGift: Boolean,
    val giftMessage: GiftMessageDto?,
) : Parcelable {
    constructor(dto: ActivityDto, rejectedItemCount: List<RejectedItemsByStorageType>, headerUi: DetailsHeaderUi, zonedBags: List<ZonedBagsScannedData>) : this(
        actId = dto.actId,
        activityNo = dto.activityNo,
        erId = dto.erId,
        customerName = dto.asFirstInitialDotLastString(),
        customerFistNameLastInitial = dto.asFirstNameLastInitial(),
        customerOrderNumber = dto.customerOrderNumber,
        detailsHeaderUi = headerUi,
        zonedBags = zonedBags,
        rejectedItemCount = rejectedItemCount,
        totalCount = dto.containerActivities?.filter {
            it.type == ContainerType.BAG || it.type == ContainerType.LOOSE_ITEM
        }?.size ?: 0,
        type = dto.containerActivities?.firstOrNull()?.type ?: ContainerType.BAG,
        isDugOrder = dto.fulfillment?.type == FulfillmentType.DUG,
        isMultiSource = dto.isMultiSource,
        entityReference = dto.entityReference,
        rxBags = dto.getRxBag().map { bag -> // TODO pass feature flag if needed
            RxBagUI(bag.orderNumber.orEmpty(), bag.bagNumber, null, null)
        },
        hasAddOnPrescription = dto.hasAddOnPrescription(),
        customerArrivlaTime = dto.nextActExpStartTime,
        customerArrivalStatusUI = convertArrivalStatus(dto.subStatus),
        fulfillmentTypeUI = dto.fulfillment?.toFulfillmentTypeUI(),
        feScreenStatus = dto.feScreenStatus,
        isCustomerBagPreference = dto.isCustomerBagPreference,
        isRxDug = dto.isRxDug(),
        isGift = dto.isGift.orFalse(),
        giftMessage = dto.gift?.message
    )
}
