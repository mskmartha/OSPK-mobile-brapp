package com.albertsons.acupick.data.model.chat

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class CustomAttributes(
    val messageSentDate: ZonedDateTime? = null,
    val tokenizedLdapId: String? = null,
    val messageSource: MessageSource? = null,
    val messageType: DisplayType? = null,
    val messageSubType: MessageSubType? = null,
    val internalMessageSubType: InternalMessageSubType? = null,
    val pickerInitials: String? = null,
    val orderNumber: String? = null,
    val fulfillmentOrderNumber: String? = null,
    val storeNumber: String? = null,
    val orderedItem: OrderedItem? = null,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class OrderedItem(
    val orderedItemId: String? = null,
    val orderedItemDescription: String? = null,
    val orderedQuantity: Int? = null,
    val orderedItemPrice: Double? = null,
    val oosApprovalStatus: OosApprovalStatus? = null,
    val substitutedItems: List<SubstituteItem>? = null,
    val imageUrl: String? = "",
) : Parcelable {
    val showPrice = orderedItemPrice != null && orderedItemPrice > 0.0
}

@JsonClass(generateAdapter = true)
@Parcelize
data class SubstituteItem(
    val substitutedItemId: String? = null,
    val substitutedItemDescription: String? = null,
    val substitutedQuantity: Int? = null,
    val substitutedItemPrice: Double? = null,
    val substitutedUpcId: String? = null,
    val imageUrl: String? = "",
    val subApprovalStatus: SubApprovalStatus? = null,
) : Parcelable {
    val showPrice = substitutedItemPrice != null && substitutedItemPrice > 0.0
}

@JsonClass(generateAdapter = false)
enum class DisplayType {
    @Json(name = "TEXT")
    TEXT,
    @Json(name = "AISLE_PHOTO")
    AISLE_PHOTO,
    @Json(name = "FORMATTED")
    FORMATTED,
    @Json(name = "GREETING")
    GREETING,
    @Json(name = "INTERNAL")
    INTERNAL
}

@JsonClass(generateAdapter = false)
enum class MessageSubType {
    @Json(name = "SWAP")
    SWAP,
    @Json(name = "SUB")
    SUB,
    @Json(name = "OOS")
    OOS
}

@JsonClass(generateAdapter = false)
enum class InternalMessageSubType {
    @Json(name = "JOINED_CHAT")
    JOINED_CHAT,
    @Json(name = "LEFT_CHAT")
    LEFT_CHAT,
}

@JsonClass(generateAdapter = false)
enum class SubApprovalStatus {
    @Json(name = "REQUESTED")
    REQUESTED,
    @Json(name = "APPROVED")
    APPROVED,
    @Json(name = "REJECTED")
    REJECTED
}

@JsonClass(generateAdapter = false)
enum class OosApprovalStatus {
    @Json(name = "REQUESTED")
    REQUESTED,
    @Json(name = "REJECTED")
    REJECTED,
    @Json(name = "REMOVED")
    REMOVED
}

@JsonClass(generateAdapter = false)
enum class MessageSource {
    @Json(name = "UMA")
    UMA,
    @Json(name = "PICKER")
    PICKER,
    @Json(name = "OSCC")
    OSCC,
    @Json(name = "WEB")
    WEB
}
