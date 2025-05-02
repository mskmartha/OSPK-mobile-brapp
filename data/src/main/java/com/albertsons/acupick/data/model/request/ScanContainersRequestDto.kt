package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

// TODO: The class name may change
@JsonClass(generateAdapter = true)
@Parcelize
data class ScanContainersRequestDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "containerReqs") val containerReqs: List<ScanContainerRequestDto>? = null,
    @Json(name = "scanContainerTIme") val scanContainerTime: ZonedDateTime? = null,
) : Parcelable, Dto
