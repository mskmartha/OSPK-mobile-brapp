package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.CustomerArrivalStatus
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class UpdateDugArrivalStatusRequestDto(
    @Json(name = "activityId") val activityId: Long? = null,
    @Json(name = "event") val customerArrivalStatus: CustomerArrivalStatus?,
    @Json(name = "entityRefId") val entityRefId: String? = null,
    @Json(name = "erId") val erId: Long?,
    @Json(name = "eta") val estimateTimeOfArrival: ZonedDateTime? = null,
    @Json(name = "orderNumber") val orderNumber: String?,
    @Json(name = "parkedSpot") val parkedSpot: String? = null,
    @Json(name = "storeNumber") val siteId: Long?,
    @Json(name = "eventTimeStamp") val statusEventTimestamp: ZonedDateTime?,
    @Json(name = "vehicleDetails") val vehicleDetails: String? = null
) : Parcelable, Dto
