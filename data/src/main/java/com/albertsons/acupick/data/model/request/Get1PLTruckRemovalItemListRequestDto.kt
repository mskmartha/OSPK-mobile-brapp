package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class Get1PLTruckRemovalItemListRequestDto(
    @Json(name = "vanId") val vanId: String? = null,
    @Json(name = "eventTimeStamp") val eventTimeStamp: ZonedDateTime? = null,
    @Json(name = "activityId") val activityId: Long? = null,
    @Json(name = "siteId") val siteId: String? = null,
    @Json(name = "defaultPickListSelected") val defaultPickListSelected: Boolean? = true,
    @Json(name = "replaceOverride") val replaceOverride: Boolean? = null,
    @Json(name = "resetPickList") val resetPickList: Boolean? = null,
    @Json(name = "user") val user: UserDto? = null,
) : Parcelable, Dto
