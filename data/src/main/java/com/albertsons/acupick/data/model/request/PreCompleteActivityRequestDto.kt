package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.IssuesScanningBag
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to the PreCompleteActReq swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class PreCompleteActivityRequestDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "containerIds") val containerIds: List<String>? = null,
    @Json(name = "preCompTime") val preCompTime: ZonedDateTime = ZonedDateTime.now(),
    @Json(name = "scanAllContainers") val scanAllContainers: Boolean? = null,
    @Json(name = "validateMissingContainer") val validateMissingContainer: Boolean? = null,
    @Json(name = "issuesScanningBags") val issuesScanningBags: List<IssuesScanningBag>? = null
) : Parcelable, Dto
