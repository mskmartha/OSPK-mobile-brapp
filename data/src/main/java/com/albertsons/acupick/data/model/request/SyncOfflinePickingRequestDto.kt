package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the SyncOfflinePickingReq swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class SyncOfflinePickingRequestDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "completeBackroomPick") val completeBackroomPickDto: PickCompleteRequestDto? = null,
    @Json(name = "completeDropOffAndScanReq") val completeDropOffAndScanReqDto: CompleteDropOffAndScanRequestDto? = null,
    @Json(name = "completeDropOffReq") val completeDropOffReqDto: CompleteDropOffRequestDto? = null,
    @Json(name = "dropOffRemoveContainerReqs") val dropOffRemoveContainerReqDtos: List<RemoveContainerRequestDto>? = null,
    @Json(name = "dropOffScanRequestList") val dropOffScanRequestDtoList: List<ScanRequestDto>? = null,
    @Json(name = "pickupCompleteReq") val pickupCompleteReqDto: PickupCompleteRequestDto? = null,
    @Json(name = "pickupRemoveContainerReqs") val pickupRemoveContainerReqDtos: List<RemoveContainerRequestDto>? = null,
    @Json(name = "pickupScanRequestList") val pickupScanRequestDtoList: List<ScanRequestDto>? = null,
    @Json(name = "preCompleteActReq") val preCompleteActivityReqDto: PreCompleteActivityRequestDto? = null,
    @Json(name = "recordPick") val recordPickDto: List<ItemPickRequestDto>? = null,
    @Json(name = "recordShortage") val recordShortage: List<ShortPickRequestDto>? = null,
    @Json(name = "itemPickCompleteDto") val itemPickCompleteDto: List<ItemPickCompleteDto>? = null,
    @Json(name = "undoPick") val undoPickDto: List<UndoPickRequestDto>? = null,
    @Json(name = "undoShortage") val undoShortageDto: List<UndoShortRequestDto>? = null
) : Parcelable, Dto
