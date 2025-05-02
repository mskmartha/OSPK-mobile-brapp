package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class IssuesScanningBag(
    @Json(name = "containerId") val containerId: String?,
    @Json(name = "exceptionReasonCode") val exceptionReasonCode: String?,
) : Parcelable
