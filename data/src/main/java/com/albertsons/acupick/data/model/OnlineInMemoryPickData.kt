package com.albertsons.acupick.data.model

import com.albertsons.acupick.data.model.request.ActionTimeWrapper
import com.albertsons.acupick.data.model.request.ItemPickRequestDto
import com.albertsons.acupick.data.model.request.ShortPickRequestDto
import com.albertsons.acupick.data.model.request.UndoPickLocalDto
import com.albertsons.acupick.data.model.request.UndoShortRequestDto
import com.albertsons.acupick.data.model.response.PickItemDto
import com.albertsons.acupick.data.model.response.ShortItemDto

/** Source of truth for in-memory pick data has been successfully transferred to the backend but not yet present in the baseLineActivityDetails cached call.
 * Clear out the instance (or the lists) when fetching the latest activity details */
data class OnlineInMemoryPickData(
    /** List of successful shorts while online but not present in baseline data set */
    val shortPickRequestDtos: List<RequestResponse<ShortPickRequestDto, List<ShortItemDto>>> = emptyList(),
    /** List of undo shorts while online but not present in baseline data set */
    val undoShortRequestDtos: List<RequestResponse<ActionTimeWrapper<UndoShortRequestDto>, List<ShortItemDto>>> = emptyList(),
    /** List of picks while online but not present in baseline data set */
    val itemPickRequestDtos: List<RequestResponse<ItemPickRequestDto, List<PickItemDto>>> = emptyList(),
    /** List of undo picks while online but not present in baseline data set */
    // FIXME: Determine if ActionTimeWrapper is truly needed here to determine how to reconstruct the pickedUpcCodes[] for an item
    /** As part of story "ACIP-278405 undo picks for live order view", we have consolidated multiple undo picks into a list
     * and made as a single api call with list of request objects, Currently OnlineInMemoryPickData is not used anywhere
     * in future if we are planning to use it please update type to List<UndoPickLocalDto> before usage */
    val undoItemPickRequestDtos: List<RequestResponse<ActionTimeWrapper<UndoPickLocalDto>, List<PickItemDto>>> = emptyList(),
) : Dto {
    internal fun anyDataPresent(): Boolean = itemPickRequestDtos.isNotEmpty() || shortPickRequestDtos.isNotEmpty() || undoItemPickRequestDtos.isNotEmpty() || undoShortRequestDtos.isNotEmpty()
}
