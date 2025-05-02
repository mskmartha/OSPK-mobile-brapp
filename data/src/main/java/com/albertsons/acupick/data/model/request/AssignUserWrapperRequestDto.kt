package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the assignUseWrapperReq (sic) swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class AssignUserWrapperRequestDto(
    @Json(name = "actIds") val actIds: List<Long>? = null,
    @Json(name = "replaceOverride") val replaceOverride: Boolean? = null,
    @Json(name = "resetPickList") val resetPickList: Boolean? = null,
    @Json(name = "user") val user: UserDto? = null,
    @Json(name = "etaArrivalFlag") val etaArrivalFlag: Boolean? = null
) : Parcelable, Dto
