package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.BoxCountPerOrderDto
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
class AddBoxCountRequestDto(
    @Json(name = "activityId") val activityId: Int? = null,
    @Json(name = "boxCountPerOrder") val boxCountPerOrder: List<BoxCountPerOrderDto>? = null
) : Parcelable, Dto
