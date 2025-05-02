package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.ActivityType
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the BagAndLoseItemActivityDto swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class BagAndLooseItemActivityDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "actType") val actType: ActivityType? = null,
    @Json(name = "activityNo") val activityNo: String? = null,
    @Json(name = "nextActivityId") val nextActivityId: Long? = null,
    @Json(name = "status") val status: ActivityStatus? = null,
) : Parcelable, Dto
