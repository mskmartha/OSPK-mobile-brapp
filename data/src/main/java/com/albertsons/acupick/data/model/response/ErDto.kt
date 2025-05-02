package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.AmountDto
import com.albertsons.acupick.data.model.CustomerArrivalStatus
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.EntityReference
import com.albertsons.acupick.data.model.ErOrderStatus
import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.ToteEstimateDto
import com.albertsons.acupick.data.model.request.UserDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to the ErDto swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ErDto(
    @Json(name = "additionalActData") val additionalActData: AdditionalActDataDto? = null,
    @Json(name = "assignedTo") val assignedTo: UserDto? = null,
    @Json(name = "bagCount") val bagCount: Int? = null,
    @Json(name = "looseItemCount") val looseItemCount: Int? = null,
    @Json(name = "toteCount") val toteCount: Int? = null,
    @Json(name = "bagCountForHandOff") val bagCountForHandOff: Int? = null,
    @Json(name = "banner") val banner: String? = null,
    @Json(name = "batch") val batch: String? = null,
    @Json(name = "cancelReasonCode") val cancelReasonCode: String? = null,
    @Json(name = "cancelReasonText") val cancelReasonText: String? = null,
    @Json(name = "completionDate")val completionDate: ZonedDateTime? = null,
    @Json(name = "contactPerson") val contactPersonDto: ContactPersonDto? = null,
    @Json(name = "customer") val customerDto: CustomerDto? = null,
    @Json(name = "customerOrderNumber") val customerOrderNumber: String? = null,
    @Json(name = "erActivities") val erActivities: List<OrderPickListDto>? = null,
    @Json(name = "erContainers") val erContainerShorts: List<ErContainerShortDto>? = null,
    @Json(name = "erId") val erId: Long? = null,
    @Json(name = "erLines") val erLines: List<ErLineDto>? = null,
    @Json(name = "erType") val erType: String? = null,
    @Json(name = "expectedCompleteDate") val expectedCompletedDate: ZonedDateTime? = null,
    @Json(name = "ffAddress") val ffAddress: AddressDto? = null,
    @Json(name = "fulfillment") val fulfillment: FulfillmentAttributeDto? = null,
    @Json(name = "fulfillmentSlot") val fulfillmentSlotDto: SlotDto? = null,
    @Json(name = "itemQty") val itemQty: Double? = null,
    /** Time when the customer is arrived/ arriving */
    @Json(name = "nextActExpStartTime") val nextActExpStartTime: ZonedDateTime? = null,
    @Json(name = "orderCount") val orderCount: String?,
    @Json(name = "orderTotal") val orderTotal: AmountDto? = null,
    @Json(name = "orderType") val orderType: OrderType? = null,
    @Json(name = "partnerName") val partnerName: String? = null,
    @Json(name = "pickupActId") val pickupActId: Long? = null,
    @Json(name = "plannedContainerTypes") val plannedContainerTypes: Map<String, String>? = null,
    @Json(name = "reference") val reference: EntityReference? = null,
    @Json(name = "releaseDateTIme") val releaseDateTime: ZonedDateTime? = null,
    @Json(name = "routeVanNumber") val routeVanNumber: String? = null,
    @Json(name = "shiftNo") val shiftNo: String? = null,
    @Json(name = "shortOrderNumber") val shortOrderNumber: String? = null,
    @Json(name = "siteId") val siteId: String? = null,
    @Json(name = "status") val status: ErOrderStatus? = null,
    @Json(name = "stopNumber") val stopNumber: String? = null,
    @Json(name = "subStatus") val subStatus: CustomerArrivalStatus? = null,
    @Json(name = "totalRegulated") val totalRegulated: Int? = null,
    @Json(name = "totalSubstitution") val totalSubstitution: Int? = null,
    @Json(name = "toteEstimate") val toteEstimate: ToteEstimateDto? = null,
    @Json(name = "isYesterday") val isYesterday: Boolean? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "containsRegulatedItem") val containsRegulatedItem: Boolean? = false,
    @Json(name = "isMultiSource") val isMultiSource: Boolean? = null,
    @Json(name = "feScreenStatus") val feScreenStatus: String? = null
) : Parcelable, Dto

fun ContactPersonDto.asFirstInitialDotLastString() =
    "${firstName?.take(1)}. $lastName"
