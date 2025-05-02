package com.albertsons.acupick.data.model

import com.albertsons.acupick.data.model.request.ActionTimeWrapper
import com.albertsons.acupick.data.model.request.ItemPickCompleteDto
import com.albertsons.acupick.data.model.request.ItemPickRequestDto
import com.albertsons.acupick.data.model.request.ShortPickRequestDto
import com.albertsons.acupick.data.model.request.SyncOfflinePickingRequestDto
import com.albertsons.acupick.data.model.request.UndoPickLocalDto
import com.albertsons.acupick.data.model.request.UndoShortRequestDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ItemLocationDto
import com.albertsons.acupick.data.model.response.ItemUpcDto
import com.albertsons.acupick.data.model.response.SubstitutionItemDetailsDto
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.squareup.moshi.JsonClass
import timber.log.Timber
import java.time.ZonedDateTime

/** Source of truth for offline pick data that is serialized/deserialized to/from disk */
@JsonClass(generateAdapter = true)
data class OfflinePickData(
    /** Represents the activity details when the picklist was selected to be worked on this session of the app */
    val baselineActivityDetails: ActivityDto = ActivityDto(),
    /** All item upcs to perform offline matching */
    val itemUpcs: List<ItemUpcDto> = emptyList(),
    /** All suggested substitution items */
    val substitutionItemDetails: List<SubstitutionItemDetailsDto> = emptyList(),
    /** All items alternate locations */
    val alternateLocationsDetailsMap: Map<String, List<ItemLocationDto>> = emptyMap(),
    /** List of picks while offline */
    val itemPickRequestDtos: List<ItemPickRequestDto> = emptyList(),
    /** List of undo picks while offline */
    val undoItemPickRequestDtos: List<ActionTimeWrapper<UndoPickLocalDto>> = emptyList(),
    /** List of shorts while offline */
    val shortPickRequestDtos: List<ShortPickRequestDto> = emptyList(),
    /** List of substituted item */
    val itemPickCompleteDto: List<ItemPickCompleteDto> = emptyList(),
    /** List of undo shorts while offline */
    val undoShortRequestDtos: List<ActionTimeWrapper<UndoShortRequestDto>> = emptyList(),

) : Dto {
    internal fun toSyncOfflinePickingRequestDto() = SyncOfflinePickingRequestDto(
        actId = baselineActivityDetails.actId,
        recordPickDto = itemPickRequestDtos.withDisableContainerValidationTrue(),
        recordShortage = shortPickRequestDtos,
        itemPickCompleteDto = itemPickCompleteDto,
        undoPickDto = undoItemPickRequestDtos.map { it.wrapped.undoPickRequestDto },
        undoShortageDto = undoShortRequestDtos.map { it.wrapped },
    )

    /** True if any of the record pick/short undo pick/short arrays is not empty. */
    internal fun isSyncDataPresent(): Boolean = itemPickRequestDtos.isNotEmpty() || shortPickRequestDtos.isNotEmpty() || undoItemPickRequestDtos.isNotEmpty() || undoShortRequestDtos.isNotEmpty() ||
        itemPickCompleteDto.isNotEmpty()

    /** Removes all offline data that has been synced prior to [syncStartTimestamp], leaving any offline data added afterwards to be synced in the future and returns the copy. */
    internal fun copyWithUnsyncedOfflineActionsCleared(syncStartTimestamp: ZonedDateTime): OfflinePickData {
        val itemPickRequestDtos = itemPickRequestDtos.map { itemPickRequestDto ->
            itemPickRequestDto.copy(lineReqDto = itemPickRequestDto.lineReqDto.orEmpty().filter { lineReqDto -> lineReqDto.pickedTime?.isAfter(syncStartTimestamp) == true })
        }.filter { it.lineReqDto.isNotNullOrEmpty() }
        val shortPickRequestDtos = shortPickRequestDtos.map { shortPickRequestDto ->
            shortPickRequestDto.copy(shortReqDto = shortPickRequestDto.shortReqDto.orEmpty().filter { lineReqDto -> lineReqDto.shortedTime?.isAfter(syncStartTimestamp) == true })
        }.filter { it.shortReqDto.isNotNullOrEmpty() }
        val undoItemPickRequestDtos = undoItemPickRequestDtos.filter { actionTimeWrapper -> actionTimeWrapper.actionTime.isAfter(syncStartTimestamp) }
        val undoShortRequestDtos = undoShortRequestDtos.filter { actionTimeWrapper -> actionTimeWrapper.actionTime.isAfter(syncStartTimestamp) }
        val itemPickCompleteDtos = itemPickCompleteDto.map { itemPickCompleteDto ->
            itemPickCompleteDto.copy(substitutedItems = itemPickCompleteDto.substitutedItems.orEmpty().filter { lineReqDto -> lineReqDto.substitutedTime?.isAfter(syncStartTimestamp) == true })
        }.filter { it.substitutedItems.isNotNullOrEmpty() }

        return copy(
            itemPickRequestDtos = itemPickRequestDtos,
            shortPickRequestDtos = shortPickRequestDtos,
            undoItemPickRequestDtos = undoItemPickRequestDtos,
            undoShortRequestDtos = undoShortRequestDtos,
            itemPickCompleteDto = itemPickCompleteDtos
        ).also { Timber.v("[copyWithUnsyncedOfflineActionsCleared] data left to sync=${it.toStringOfflineActions()}") }
    }

    internal fun copyWithAllOfflineActionsCleared(): OfflinePickData =
        copy(itemPickRequestDtos = emptyList(), shortPickRequestDtos = emptyList(), undoItemPickRequestDtos = emptyList(), undoShortRequestDtos = emptyList(), itemPickCompleteDto = emptyList())

    /** Returns an item pick request list with disableContainerValidation set to true for all items. This prevents the backend from failing the sync when a tote is associated with another order. */
    private fun List<ItemPickRequestDto>.withDisableContainerValidationTrue(): List<ItemPickRequestDto> {
        return map { itemPickRequest ->
            itemPickRequest.copy(
                lineReqDto = itemPickRequest.lineReqDto.orEmpty().map { lineReqDto ->
                    lineReqDto.copy(disableContainerValidation = true)
                }
            )
        }
    }

    /** A toString implementation that just logs the list of actions performed while offline. */
    fun toStringOfflineActions(): String {
        return "itemPickRequestDtos=$itemPickRequestDtos, undoItemPickRequestDtos=$undoItemPickRequestDtos, shortPickRequestDtos=$shortPickRequestDtos, " +
            "undoShortRequestDtos=$undoShortRequestDtos"
    }
}
