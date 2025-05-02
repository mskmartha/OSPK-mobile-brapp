package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class CompleteDropOffRequestDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "dropOffCompTime") val dropOffCompTime: ZonedDateTime? = ZonedDateTime.now(),
    @Json(name = "containerIdList") val containerIdList: List<String>? = null,
    @Json(name = "releaseContainers") val releaseContainers: Boolean = true,
    @Json(name = "validateMissingContainer") val validateMissingContainer: Boolean = true,
) : Parcelable, Dto
