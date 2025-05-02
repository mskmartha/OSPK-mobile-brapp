package com.albertsons.acupick.data.model.response

import androidx.annotation.Keep
import com.albertsons.acupick.data.model.request.UserDto
import com.squareup.moshi.Json
import java.io.Serializable

@Keep
data class SwapItem(
    val subApprovalStatus: SubApprovalStatus? = null,
    val id: Long? = null,
    val itemId: String? = null,
    val imageUrl: String? = null,
    val itemDesc: String? = null,
    val containerId: String? = null,
    val netWeight: Double? = null,
    val qty: Double? = 0.0,
    val price: Double? = null,
    val upcId: Long? = null,
    val substitutedWith: List<SwapItem>? = null,
    val shortedItemUpc: List<ShortedItemUpcDto>? = null,
    val assignedTo: UserDto? = null,
    val customerOrderNumber: String? = null
) : Serializable {

    val quantity = qty?.toInt().toString()
    val showPrice = price != null
}

data class SwapItemParams(
    val itemIdAndMessageSid: Pair<String, String>? = null,
    val substituteItem: List<SwapItem>,
) : Serializable

@Keep
data class SwapSubstitutionArg(
    val id: String,
    val name: String,
) : Serializable

enum class SubApprovalStatus {
    @Json(name = "APPROVED_SUB")
    APPROVED_SUB,

    @Json(name = "DECLINED_SUB")
    DECLINED_SUB,

    @Json(name = "PENDING_SUB")
    PENDING_SUB,

    @Json(name = "OUT_OF_STOCK")
    OUT_OF_STOCK
}
