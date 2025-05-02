package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.ContainerActivityStatus
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.EntityReference
import com.albertsons.acupick.data.model.ScanContainerReasonCode
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to the ScanContActDto swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ScanContActDto(
    @Json(name = "attemptToRemove") val attemptToRemove: Boolean?,
    @Json(name = "bagCount") val bagCount: Int?,
    @Json(name = "containerId") val containerId: String?,
    @Json(name = "containerType") val containerType: String?,
    @Json(name = "id") val id: Long?,
    @Json(name = "lastScanTime") val lastScanTime: ZonedDateTime?,
    @Json(name = "location") val location: String?,
    @Json(name = "looseItemCount") val looseItemCount: Int?,
    @Json(name = "reference") val reference: EntityReference?,
    @Json(name = "regulated") val regulated: Boolean?,
    @Json(name = "status") val status: ContainerActivityStatus?,
    @Json(name = "type") val type: ContainerType?,
    @Json(name = "reasonCode") val reasonCode: ScanContainerReasonCode? = null
) : Parcelable, Dto
