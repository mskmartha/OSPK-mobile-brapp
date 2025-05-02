package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the UpdateErContBagReq swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class UpdateErContBagRequestDto(
    @Json(name = "activityId") val activityId: Long,
    @Json(name = "contBagCountReqList") val contBagCountReqList: List<ContBagCountRequestDto?>?,
) : Parcelable, Dto
