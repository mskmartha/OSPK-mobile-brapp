package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.CancelReasonCode
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class CancelHandoffRequestDto(
    @Json(name = "activityId") val activityId: Long? = null,
    @Json(name = "cancelReasonCode") val cancelReasonCode: CancelReasonCode? = null,
    @Json(name = "cancelReasonText") val cancelReasonText: String? = null,
    @Json(name = "cancelTime") val cancelTime: ZonedDateTime? = ZonedDateTime.now(),
    @Json(name = "entityRefId") val entityRefId: String? = null,
    @Json(name = "erId") val erId: Long? = null,
    @Json(name = "orderNumber") val orderNumber: String? = null,
    @Json(name = "siteId") val siteId: String? = null,
) : Parcelable, Dto
