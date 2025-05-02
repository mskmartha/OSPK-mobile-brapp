package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.ShortReasonCode
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to the ShortReq swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ShortRequestDto(
    @Json(name = "iaId") val iaId: Long? = null,
    @Json(name = "itemId") val itemId: String? = null,
    @Json(name = "lineId") val lineId: String? = null,
    @Json(name = "qty") val qty: Double? = null,
    @Json(name = "shortageReasonCode") val shortageReasonCode: ShortReasonCode? = null,
    @Json(name = "shortageReasonText") val shortageReasonText: String? = null,
    @Json(name = "shortedTime") val shortedTime: ZonedDateTime? = null,
    @Json(name = "userId") val userId: String? = null
) : Parcelable, Dto
