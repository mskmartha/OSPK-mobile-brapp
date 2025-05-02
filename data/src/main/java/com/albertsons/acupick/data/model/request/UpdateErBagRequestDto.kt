package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the UpdateErBagReq swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class UpdateErBagRequestDto(
    @Json(name = "activityId") val activityId: Long? = null,
    @Json(name = "contBagCountReqList") val contContBagCount: List<ContBagCountRequestDto>? = null
) : Parcelable, Dto
