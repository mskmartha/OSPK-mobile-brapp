package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.VanStatus
import com.albertsons.acupick.data.model.request.UserDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class OnePlDto(
    @Json(name = "activityId") val activityId: Long,
    @Json(name = "vanNumber")val vanNumber: String?,
    @Json(name = "orderCount")val orderCount: Int,
    @Json(name = "plannedDepartureTime") val plannedDepartureTime: ZonedDateTime?,
    @Json(name = "status") val status: VanStatus?,
    @Json(name = "vanArrivalTime") val vanArrivalTime: ZonedDateTime?,
    @Json(name = "assignedTo") val assignedUser: UserDto?,
    @Json(name = "rejectedItemsCount") val rejectedItemsCount: Int?
) : Parcelable, Dto
