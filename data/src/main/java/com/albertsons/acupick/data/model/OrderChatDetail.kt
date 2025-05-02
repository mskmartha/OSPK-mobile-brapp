package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

const val MAX_FIRST_NAME_LENGTH_0 = 16
const val MAX_FIRST_NAME_LENGTH_1 = 11
const val MAX_FIRST_NAME_LENGTH_2 = 7
const val MAX_FIRST_NAME_LENGTH_DEFAULT = 5
const val ELLIPSIS = "…"

@JsonClass(generateAdapter = true)
@Parcelize
data class OrderChatDetail(
    @Json(name = "customerOrderNumber") val customerOrderNumber: String? = null,
    @Json(name = "customerFirstName") val customerFirstName: String? = null,
    @Json(name = "customerLastName") val customerLastName: String? = null,
    @Json(name = "conversationSid") val conversationSid: String? = null,
    @Json(name = "referenceEntityId") val referenceEntityId: String? = null,
    @Json(name = "startChatBlockTimer") val startChatBlockTimer: Boolean? = null
) : Parcelable, Dto

data class CustomerChatInfo(
    val customerOrderNumber: String,
    val customerFirstName: String,
    val customerLastName: String,
    val conversationSid: String,
    val referenceEntityId: String,
    val isCustomerTyping: Boolean = false,
    val hasUnreadMessages: Boolean = false,
    val substitutionItemImages: List<String>? = null,
    val lastMessageTime: String = ""
)

fun OrderChatDetail.fullContactName() = "${customerFirstName.orEmpty()} ${customerLastName.orEmpty()}".trim()

fun CustomerChatInfo.asFirstNameLastInitialDotString(size: Int): String {
    val truncatedFirstName = customerFirstName.take(
        when (size) {
            0 -> MAX_FIRST_NAME_LENGTH_0
            1 -> MAX_FIRST_NAME_LENGTH_1
            2 -> MAX_FIRST_NAME_LENGTH_2
            else -> MAX_FIRST_NAME_LENGTH_DEFAULT
        }.let { if (it < customerFirstName.length) it else customerFirstName.length }
    ).let { if (it.length < customerFirstName.length) it + ELLIPSIS else it }
    return "${truncatedFirstName.replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }} ${customerLastName.take(1).uppercase()}."
}
