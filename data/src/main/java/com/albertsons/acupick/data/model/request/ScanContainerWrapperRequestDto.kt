package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class ScanContainerWrapperRequestDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "containerReqs") val containerReqs: List<ScanContainerRequestDto>? = null,
    @Json(name = "lastScanTime") val lastScanTime: ZonedDateTime? = ZonedDateTime.now(),
    @Json(name = "multipleHandoff") val multipleHandoff: Boolean,
    @Json(name = "isDarkStoreEnabled") val isDarkStore: Boolean? = null,
    @Json(name = "isWineFulfillment") val isWineFulfillment: Boolean? = null,
) : Parcelable, Dto
