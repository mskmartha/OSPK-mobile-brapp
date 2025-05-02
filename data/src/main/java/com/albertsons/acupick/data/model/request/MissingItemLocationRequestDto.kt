package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import timber.log.Timber
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class MissingItemLocationRequestDto(
    @Json(name = "siteId") val siteId: String? = null,
    @Json(name = "activityId") val activityId: Long? = null,
    @Json(name = "itemActivityId") val itemActivityId: Long? = null,
    @Json(name = "itemId") val itemId: String? = null,
    @Json(name = "upcId") val upcId: String? = null,
    @Json(name = "aisle") val aisle: String? = null,
    @Json(name = "section") val section: String? = null,
    @Json(name = "shelf") val shelf: String? = null,
    @Json(name = "comment") val comment: String? = null,
    @Json(name = "locationReasonCode") val locationReasonCode: String? = null,
) : Parcelable, Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class SyncOfflineMissingItemsLocationReqDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "missingLocationReq") val missingItemLocationList: List<MissingItemLocationRequestDto>? = null,
) : Parcelable, Dto

@JsonClass(generateAdapter = true)
data class OfflineMissingItemLocation(
    val actId: Long? = null,
    val missingItemLocationsDto: List<ActionTimeWrapper<MissingItemLocationRequestDto>> = emptyList(),
) : Dto {
    internal fun toSyncOfflineItemRequestDto() = SyncOfflineMissingItemsLocationReqDto(
        actId = actId,
        missingItemLocationList = missingItemLocationsDto.map { it.wrapped }
    )

    internal fun copyWithUnsyncedOfflineActionsCleared(syncStartTimestamp: ZonedDateTime): OfflineMissingItemLocation {
        val missingItemLocations = missingItemLocationsDto.filter { actionTimeWrapper -> actionTimeWrapper.actionTime.isAfter(syncStartTimestamp) }
        return copy(actId, missingItemLocations).also { Timber.v("[copyWithUnsyncedOfflineActionsCleared] data left to sync=${it.toStringOfflineActions()}") }
    }

    internal fun copyWithAllOfflineActionsCleared(): OfflineMissingItemLocation =
        copy(actId = null, missingItemLocationsDto = emptyList())

    internal fun isSyncDataPresent(): Boolean = missingItemLocationsDto.isNotEmpty()

    fun toStringOfflineActions(): String {
        return "missingItemLocationsDto=$missingItemLocationsDto"
    }
}
