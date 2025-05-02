package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.ContainerActivityStatus
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.EntityReference
import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.FulfillmentSubType
import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

import java.time.ZonedDateTime

/**
 * Corresponds to the ContainerActivityDto swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ContainerActivityDto(
    @Json(name = "attemptToRemove") val attemptToRemove: Boolean? = null,
    @Json(name = "bagCount") val bagCount: Int? = null,
    /** Customer First Name */
    @Json(name = "contactFirstName") val contactFirstName: String? = null,
    /** Customer Last Name */
    @Json(name = "contactLastName") val contactLastName: String? = null,
    @Json(name = "contactPhoneNum") val contactPhoneNumber: String? = null,
    @Json(name = "containerId") val containerId: String? = null,
    @Json(name = "containerItems") val containerItems: List<ErItemDto>? = null,
    @Json(name = "containerType") val containerType: StorageType? = null,
    @Json(name = "customerOrderNumber") val customerOrderNumber: String? = null,
    @Json(name = "erId") val erId: Long? = null,
    @Json(name = "fulfillment") val fulfillment: FulfillmentAttributeDto? = null,
    @Json(name = "id") val id: Long? = null,
    @Json(name = "lastScanTime") val lastScanTime: ZonedDateTime? = null,
    @Json(name = "location") val location: String? = null,
    @Json(name = "nextDestination") val nextDestination: String? = null,
    @Json(name = "reference") val reference: EntityReference? = null,
    @Json(name = "regulated") val regulated: Boolean? = null,
    /** Number of reshopped items in the tote.  >0 value means container is a reshop tote. */
    @Json(name = "reShoppedItems") val reShoppedItems: Double? = null,
    @Json(name = "routeVanNumber") val routeVanNumber: String? = null,
    @Json(name = "shortOrderNumber") val shortOrderNumber: String? = null,
    @Json(name = "status") val status: ContainerActivityStatus? = null,
    @Json(name = "stopNumber") val stopNumber: String? = null,
    @Json(name = "type") val type: ContainerType? = ContainerType.TOTE,
    @Json(name = "looseItemCount") val looseItemCount: Int? = null,
    @Json(name = "slotStartDate") val slotStartDate: ZonedDateTime? = null,
    @Json(name = "slotEndDate") val slotEndDate: ZonedDateTime? = null,
    @Json(name = "isCustBagPreference") val isCustomerBagPreference: Boolean? = null,
) : Parcelable

fun ContainerActivityDto.fullContactName() = "${contactFirstName.orEmpty()} ${contactLastName.orEmpty()}".trim()

fun ContainerActivityDto.asFirstInitialDotLastString(): String {
    val firstInitial = contactFirstName?.take(1)
    return if (firstInitial.isNotNullOrBlank()) {
        "$firstInitial. ${contactLastName.orEmpty()}"
    } else {
        contactLastName.orEmpty()
    }
}

fun ContainerActivityDto.getFulfillmentTypeDescriptions(): String =
    when {
        fulfillment?.type == FulfillmentType.DUG -> "DUG-$shortOrderNumber"
        fulfillment?.subType == FulfillmentSubType.THREEPL -> "3PL-$stopNumber"
        fulfillment?.subType == FulfillmentSubType.ONEPL -> "$routeVanNumber-$stopNumber"
        fulfillment?.type == FulfillmentType.DELIVERY -> "DEL-$stopNumber"
        else -> ""
    }
fun ContainerActivityDto.isReshop(): Boolean = this.reShoppedItems?.toInt() ?: 0 > 0
