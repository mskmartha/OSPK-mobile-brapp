package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.ScanContainerReasonCode
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class ScanContainerRequestDto(
    @Json(name = "containerId") val containerId: String? = null,
    @Json(name = "overrideAttemptToRemove") val overrideAttemptToRemove: Boolean = true,
    @Json(name = "overrideRemoved") val overrideRemoved: Boolean = true,
    @Json(name = "overrideScanUser") val overrideScanUser: Boolean = true,
    @Json(name = "stagingLocation") val stagingLocation: String? = null,
    @Json(name = "startIfNotStarted") val startIfNotStarted: Boolean = true,
    @Json(name = "containerScanTime") val containerScanTime: ZonedDateTime? = null,
    @Json(name = "isLoose") val isLoose: Boolean? = null,
    @Json(name = "reasonCode") val reasonCode: ScanContainerReasonCode? = null
) : Parcelable, Dto
