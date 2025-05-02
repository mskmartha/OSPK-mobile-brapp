package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class Cancel1PLHandoffRequestDto(
    @Json(name = "activityId") val activityId: Long? = null,
    @Json(name = "unassignTime") val unassignTime: ZonedDateTime? = null,
) : Parcelable, Dto
