package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.CustomerArrivalStatus
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
class FetchOrderStatusResponseDto(
    @Json(name = "erId") val erId: Long? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "subStatus") val subStatus: CustomerArrivalStatus? = null,
    @Json(name = "subStatusTime") val subStatusTime: ZonedDateTime? = null,
    @Json(name = "entityId") val entityId: String? = null,
    @Json(name = "entityType") val entityType: String? = null,
) : Parcelable, Dto
