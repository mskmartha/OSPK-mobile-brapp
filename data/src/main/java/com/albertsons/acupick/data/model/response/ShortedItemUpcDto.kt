package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.ShortReasonCode
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to the ShortedItemUpcDto swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ShortedItemUpcDto(
    /** Shorted quantity (due to Out of Stock, etc) */
    @Json(name = "exceptionQty") val exceptionQty: Double? = null,
    @Json(name = "exceptionReasonCode") val exceptionReasonCode: ShortReasonCode? = null,
    @Json(name = "exceptionReasonText") val exceptionReasonText: String? = null,
    @Json(name = "shortedId") val shortedId: Long? = null,
    @Json(name = "shortedTime") val shortedTime: ZonedDateTime? = null,
    @Json(name = "userId") val userId: String? = null
) : Parcelable, Dto
