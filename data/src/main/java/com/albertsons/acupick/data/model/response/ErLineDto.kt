package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.AmountDto
import com.albertsons.acupick.data.model.ContainerActivityStatus
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.ShortReasonCode
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the ERLineDto swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ErLineDto(
    @Json(name = "amount") val amount: AmountDto? = null,
    @Json(name = "attemptToRemove") val attemptToRemove: Boolean? = null,
    @Json(name = "cancelReasonCode") val cancelReasonCode: String? = null,
    @Json(name = "erId") val erId: Long? = null,
    /** Shorted quantity (due to Out of Stock, etc) */
    @Json(name = "exceptionQty") val exceptionQty: Double? = null,
    @Json(name = "exceptionReasonCode") val exceptionReasonCode: ShortReasonCode? = null,
    @Json(name = "exceptionReasonText") val exceptionReasonText: String? = null,
    @Json(name = "id") val id: Long? = null,
    @Json(name = "imageURL") val imageUrl: String? = null,
    @Json(name = "itemAttribute") val itemAttributeDto: ItemAttributeDto? = null,
    @Json(name = "itemId") val itemId: String? = null,
    @Json(name = "itemLocation") val itemLocation: ItemAddressDto? = null,
    @Json(name = "processedQty") val processedQty: Double? = null,
    @Json(name = "qty") val qty: Double? = null,
    @Json(name = "receiptCode") val receiptCode: String? = null,
    @Json(name = "status") val status: ContainerActivityStatus? = null,
    @Json(name = "useBatch") val useBatch: Boolean? = null
) : Parcelable, Dto
