package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.ActivityType
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the OrderPickListDto swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class OrderPickListDto(
    @Json(name = "actType") val actType: ActivityType? = null,
    @Json(name = "activityNo") val activityNo: String? = null,
    @Json(name = "firstName") val firstName: String? = null,
    @Json(name = "lastName") val lastName: String? = null,
    @Json(name = "userId") val userId: String? = null,
) : Parcelable, Dto
