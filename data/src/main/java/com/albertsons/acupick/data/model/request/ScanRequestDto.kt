package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to the ScanRequest swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ScanRequestDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "batchId") val batchId: Long? = null,
    @Json(name = "containerId") val containerId: String? = null,
    @Json(name = "lastScanTime") val lastScanTime: ZonedDateTime? = ZonedDateTime.now(),
    @Json(name = "overrideAttemptToRemove") val overrideAttemptToRemove: Boolean? = null,
    @Json(name = "overrideRemoved") val overrideRemoved: Boolean? = null,
    @Json(name = "overrideScanUser") val overrideScanUser: Boolean? = null,
    @Json(name = "stagingLocation") val stagingLocation: String? = null,
    @Json(name = "startIfNotStarted") val startIfNotStarted: Boolean? = null
) : Parcelable, Dto
