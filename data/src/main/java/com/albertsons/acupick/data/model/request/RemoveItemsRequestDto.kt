package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.EntityReference
import com.albertsons.acupick.data.model.RemovedItems
import com.albertsons.acupick.data.model.RemovedItemsAnalytics
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class RemoveItems1PLRequestDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "siteId") val siteId: String? = null,
    @Json(name = "vanNumber") val vanNumber: String? = null,
    @Json(name = "giftLabelPrintConfirmation") val giftLabelPrintConfirmation: Boolean? = null,
    @Json(name = "removeItemsReqs") val removeItemsReqs: List<RemoveItemsRequestDto>? = null,
) : Parcelable, Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class RemoveItemsRequestDto(
    @Json(name = "orderNo") val orderNo: String? = null,
    @Json(name = "siteId") val siteId: String? = null,
    @Json(name = "entityReference") val entityReference: EntityReference? = null,
    @Json(name = "timestamp") val timestamp: ZonedDateTime? = null,
    @Json(name = "reasonCode") val reasonCode: RemoveItemsReasonCode? = null,
    @Json(name = "removedItems") val removedItems: List<RemovedItems?>? = null,
    @Json(name = "analytics") val analytics: RemovedItemsAnalytics? = null,
) : Parcelable, Dto

@JsonClass(generateAdapter = false)
enum class RemoveItemsReasonCode {
    @Json(name = "CUSTOMER_NOT_LEGAL_AGE")
    CUSTOMER_NOT_LEGAL_AGE,
    @Json(name = "CUSTOMER_NOT_HAVE_VALID_ID")
    CUSTOMER_NOT_HAVE_VALID_ID,
    @Json(name = "DRIVER_NOT_LEGAL_AGE")
    DRIVER_NOT_LEGAL_AGE,
    @Json(name = "DRIVER_NOT_HAVE_VALID_ID")
    DRIVER_NOT_HAVE_VALID_ID,
    @Json(name = "REMOVED_ITEM")
    REMOVED_ITEM,
}
