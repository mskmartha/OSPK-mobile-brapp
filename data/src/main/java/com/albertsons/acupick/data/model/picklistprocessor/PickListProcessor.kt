package com.albertsons.acupick.data.model.picklistprocessor

import android.annotation.SuppressLint
import com.albertsons.acupick.data.model.OfflinePickData
import com.albertsons.acupick.data.model.OnlineInMemoryPickData
import com.albertsons.acupick.data.model.RequestResponse
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.request.ActionTimeWrapper
import com.albertsons.acupick.data.model.request.ItemPickRequestDto
import com.albertsons.acupick.data.model.request.ShortPickRequestDto
import com.albertsons.acupick.data.model.request.UndoPickLocalDto
import com.albertsons.acupick.data.model.request.UndoShortRequestDto
import com.albertsons.acupick.data.model.request.pickedTime
import com.albertsons.acupick.data.model.request.shortedTime
import com.albertsons.acupick.data.model.request.splitItems
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ContainerActivityDto
import com.albertsons.acupick.data.model.response.ErItemDto
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.PickItemDto
import com.albertsons.acupick.data.model.response.PickedItemUpcDto
import com.albertsons.acupick.data.model.response.ShortItemDto
import com.albertsons.acupick.data.model.response.ShortedItemUpcDto
import com.albertsons.acupick.data.picklist.PickListOperations
import com.albertsons.acupick.infrastructure.utils.ZONED_DATE_TIME_EPOCH
import com.albertsons.acupick.infrastructure.utils.orZero
import timber.log.Timber
import java.math.BigDecimal

data class PickListProcessorInput(
    val baselineActivityDetails: ActivityDto,
    val onlineInMemoryPickData: OnlineInMemoryPickData,
    val offlinePickData: OfflinePickData,
    val upcToItemIdMap: Map<String, String>,
)

/**
 * Modify the unified pick list creation to:
 * 1) Use the baseline + online request/response data to create a modified baseline (like what you would see from making an activity details api call)
 * 2) Use that modified baseline + offline request data to create a unified baseline used throughout the picking flow
 * Additionally, perform local offline data optimizations every time the offline arrays are updated (record 1 pick and undo 1 pick for the same item cancel each other out)
 */
interface PickListProcessor {
    /**
     * Updates picker display pick list state logic from a combination of:
     * * last activity details source of truth pick list state +
     * * online picking activity (not yet reflected in last activity details source of truth) +
     * * offline picking activity (not yet synced)
     * and returns the resulting unified pick list back.
     */
    fun processUnifiedPickListState(input: PickListProcessorInput): ActivityDto

    /** Performs optimizations on the offline pick data, removing recordPicks/undoPicks and recordShort/undoShort that cancel each other out while offline */
    fun optimizeOfflineData(offlinePickData: OfflinePickData?): OfflinePickData?
}

class PickListProcessorImplementation(private val barcodeMapper: BarcodeMapper, private val pickListOperations: PickListOperations) : PickListProcessor {

    override fun processUnifiedPickListState(input: PickListProcessorInput): ActivityDto {
        val baselineOnlineCombination = deriveBaselineAndOnlineCombination(input)
        val baselineOnlineOfflineCombination = deriveNewBaselineWithOffline(baselineOnlineCombination, input)
        return baselineOnlineOfflineCombination
    }

    @SuppressLint("BinaryOperationInTimber")
    override fun optimizeOfflineData(offlinePickData: OfflinePickData?): OfflinePickData? {
        if (offlinePickData == null) return null

        // /////////////////////////////////////////////////////////////////////////
        // Picks
        // /////////////////////////////////////////////////////////////////////////
        val optimizedItemPicks: MutableList<ItemPickRequestDto> = offlinePickData.itemPickRequestDtos.toMutableList()
        val undoItemPickIndicesToRemove: MutableList<Int> = mutableListOf()
        val undoPicks = offlinePickData.undoItemPickRequestDtos.map { it.wrapped }
        undoPicks.forEachIndexed { index, undoPickRequest ->
            // matching record is lineReqDto has same iaID and containerID then the fulfilledQty needs to match qty of the undo
            val matchingRecordPick = optimizedItemPicks.find {
                it.lineReqDto?.firstOrNull()?.iaId == undoPickRequest.undoPickRequestDto.iaId &&
                    it.lineReqDto?.firstOrNull()?.containerId == undoPickRequest.containerId &&
                    it.lineReqDto?.firstOrNull()?.fulfilledQty == undoPickRequest.undoPickRequestDto.qty
            }
            if (matchingRecordPick != null) {
                // undo pick item matches picked item with the same iaId
                // Lookup the index of the picked item match ...
                val matchingRecordPickIndex = optimizedItemPicks.indexOf(matchingRecordPick)
                // and remove it from the new picked items lists
                optimizedItemPicks.removeAt(matchingRecordPickIndex)
                // Store the undo item pick index to filter it later
                undoItemPickIndicesToRemove.add(index)
            } else {
                // no match - nothing to do here
            }
        }

        // Filter out all of the undo picks that were removed above to use in the updated undo item list
        val optimizedUndoItemPicks = offlinePickData.undoItemPickRequestDtos.filterIndexed { index, undoItemPick -> !undoItemPickIndicesToRemove.contains(index) }

        // /////////////////////////////////////////////////////////////////////////
        // Shorts
        // /////////////////////////////////////////////////////////////////////////
        val optimizedShortPicks: MutableList<ShortPickRequestDto> = offlinePickData.shortPickRequestDtos.toMutableList()
        val undoShortIndicesToRemove: MutableList<Int> = mutableListOf()
        val undoShortsSplit = offlinePickData.undoShortRequestDtos.map { it.wrapped }.splitItems()
        // Unwrap undo quantities to 1 qty for undo short, then iterate over the list
        undoShortsSplit.forEachIndexed { index, undoShortRequest ->
            val matchingRecordShort = optimizedShortPicks.firstOrNull { it.shortReqDto?.firstOrNull()?.iaId == undoShortRequest.iaId }
            if (matchingRecordShort != null) {
                // undo short matches request item with the same iaId
                // Lookup the index of the picked item match ...
                val matchingRecordShortIndex = optimizedShortPicks.indexOf(matchingRecordShort)
                // and remove it from the new picked items lists
                optimizedShortPicks.removeAt(matchingRecordShortIndex)
                // Store the undo item pick index to filter it later
                undoShortIndicesToRemove.add(index)
            } else {
                // no match - nothing to do here
            }
        }

        // Filter out all of the undo shorts that were removed above to use in the updated undo short list
        val optimizedUndoShortPicks = offlinePickData.undoShortRequestDtos.filterIndexed { index, undoShort -> !undoShortIndicesToRemove.contains(index) }

        // /////////////////////////////////////////////////////////////////////////
        // Optimized data creation
        // /////////////////////////////////////////////////////////////////////////
        // Create the optimized
        val optimizedOfflinePickData = offlinePickData.copy(
            itemPickRequestDtos = optimizedItemPicks,
            undoItemPickRequestDtos = optimizedUndoItemPicks,
            shortPickRequestDtos = optimizedShortPicks,
            undoShortRequestDtos = optimizedUndoShortPicks
        )
        // Log some info about the changes
        if (optimizedOfflinePickData != offlinePickData) {
            Timber.d("[optimizeOfflineData] optimizations were performed")
            Timber.v("[optimizeOfflineData] total picks - ${optimizedItemPicks.size} (was ${offlinePickData.itemPickRequestDtos.size})")
            Timber.v("[optimizeOfflineData] total undo picks - ${optimizedUndoItemPicks.size} (was ${undoPicks.size})")

            optimizedItemPicks.groupBy { it.lineReqDto?.firstOrNull()?.iaId }.forEach { (iaId, items) ->
                Timber.v(
                    "[optimizeOfflineData] picks detail - iaId=$iaId, count=${items.size}," +
                        "qty=${items.sumByDouble { item -> item.lineReqDto?.sumByDouble { it.fulfilledQty.orZero() }.orZero() }} "
                )
            }
            optimizedUndoItemPicks.groupBy { it.wrapped.undoPickRequestDto.iaId }.forEach { (iaId, items) ->
                Timber.v("[optimizeOfflineData] undo picks detail - iaId=$iaId, count=${items.size}, qty=${items.sumByDouble { it.wrapped.undoPickRequestDto.qty.orZero() }}}")
            }
        } else {
            Timber.v("[optimizeOfflineData] nothing to optimize")
        }
        return optimizedOfflinePickData
    }

    /**
     * The function manipulates the baselineActivityDetails in response to **online requests/responses only** (offline manipulations done in [deriveNewBaselineWithOffline]).
     * The end result is to return a version of the pick list mirroring the response from making the activity details api call.
     * **NOTE: Don't directly call this function from project source! public for quick testing**
     */
    @SuppressLint("BinaryOperationInTimber")
    internal fun deriveBaselineAndOnlineCombination(input: PickListProcessorInput): ActivityDto {
        // bail early if no online in memory data is present, returning baseline data
        if (!input.onlineInMemoryPickData.anyDataPresent()) return input.baselineActivityDetails

        val baselineActivityDetails = input.baselineActivityDetails
        val onlineInMemoryPickData = input.onlineInMemoryPickData
        // No need to use offline data here
        val upcToItemIdMap = input.upcToItemIdMap

        // Pick list items data combinations
        val combinedItemActivities = baselineActivityDetails.itemActivities.orEmpty().map { baselineItemActivity ->
            // Unified values
            // Unified processed/exception values
            val unifiedPickedUpcCodes = baselineItemActivity.resolvePickedItemUpcs(baselineActivityDetails, onlineInMemoryPickData)
            val unifiedShortedItemUpcList = baselineItemActivity.resolveShortedItemUpcs(baselineActivityDetails, onlineInMemoryPickData)

            val unifiedProcessedQty = unifiedPickedUpcCodes.sumByDouble { it.qty.orZero() }
            val unifiedExceptionQty = unifiedShortedItemUpcList.sumByDouble { it.exceptionQty.orZero() }

            // Log transformations
            Timber.v(
                "[deriveBaselineAndOnlineCombination] bpn=${baselineItemActivity.itemId}, customerOrderNumber=${baselineItemActivity.customerOrderNumber}," +
                    " iaId=${baselineItemActivity.id}, unifiedProcessedQty=$unifiedProcessedQty, unifiedExceptionQty=$unifiedExceptionQty," +
                    " qty=${baselineItemActivity.qty}, description=${baselineItemActivity.itemDescription}, unifiedPickedUpcCodes=$unifiedPickedUpcCodes," +
                    " unifiedShortedItemUpcList=$unifiedShortedItemUpcList"
            )
            // FIXME: Determine what other data needs to be added/modified for display purposes from all data sources
            baselineItemActivity.copy(
                processedQty = unifiedProcessedQty,
                exceptionQty = unifiedExceptionQty,
                pickedUpcCodes = unifiedPickedUpcCodes,
                shortedItemUpc = unifiedShortedItemUpcList,
            )
        }
        Timber.v("[deriveBaselineAndOnlineCombination] combined items=$combinedItemActivities")

        // Totes data combinations
        val allDistinctContainerActivityIds = lookupAllContainerActivityIds(baselineActivityDetails, input)
        // Retrieve combined data from baseline + online for containers/totes
        val combinedContainerActivities = allDistinctContainerActivityIds.mapNotNull { containerId ->
            val baselineContainerActivity = baselineActivityDetails.containerActivities.orEmpty().firstOrNull { it.containerId == containerId }
            // Picks - Find all picks for containerId - shorts have no effect on totes
            // To determine items in a container, find all pickedUpcCodes items matching container id from the unified itemActivities list ...
            val itemActivitiesWithItemsInThisTote = combinedItemActivities.flatMap { itemActivity ->
                itemActivity.pickedUpcCodes.orEmpty().filter { it.containerId == containerId }.map { itemActivity }
            }

            // ... then convert those items into the container items, with qty representing the summation of all pickedUpcCodes[].qty for the matching tote
            val containerItems = itemActivitiesWithItemsInThisTote.map { itemActivity ->
                ErItemDto(
                    itemId = itemActivity.itemId,
                    qty = itemActivity.pickedUpcCodes.orEmpty().filter { it.containerId == containerId }.sumByDouble { it.qty.orZero() },
                    itemDesc = itemActivity.itemDescription,
                    imageUrl = itemActivity.imageUrl,
                )
            }

            // Any item that specifies this container should contain the singular storage type for the tote
            val containerType = itemActivitiesWithItemsInThisTote.firstOrNull()?.storageType

            // TODO: Will order types be mismatched between data sources?
            val reference = baselineContainerActivity?.reference ?: baselineActivityDetails.entityReference
            if (containerItems.isNotEmpty()) {
                ContainerActivityDto(
                    containerId = containerId,
                    containerType = containerType,
                    containerItems = containerItems,
                    reference = reference,
                    id = null, // record pick apis don't return this data
                )
            } else {
                null
            }
        }
        Timber.v("[deriveBaselineAndOnlineCombination] combined containers=$combinedContainerActivities")

        val combinedActivityDetails = baselineActivityDetails.copy(
            containerActivities = combinedContainerActivities.takeIf { it.isNotEmpty() }, // leaving pick list in same state as it is coming from the backend when container activities is empty
            itemActivities = combinedItemActivities,
        )
        return combinedActivityDetails
    }

    /**
     * The function manipulates the baselineActivityDetails (already manipulated with online transformations) in response to **offline requests only**
     * (online manipulations done in [deriveBaselineAndOnlineCombination]).
     * The end result is to return a version of the pick list as close as possible to mirroring the response from making the activity details api call.
     * Note that additive calls where db ids are returned in the api call response (ex: upcId) are NOT able to be derived or inferred and will be missing.
     * **NOTE: Don't directly call this function from project source! public for quick testing**
     */
    @SuppressLint("BinaryOperationInTimber")
    internal fun deriveNewBaselineWithOffline(baselineActivityDetails: ActivityDto, input: PickListProcessorInput): ActivityDto {
        // bail early if no offline data is present, returning baseline data from args
        if (!input.offlinePickData.isSyncDataPresent()) return baselineActivityDetails

        // NOTE: Don't use the input.baselineActivityDetails. Use the arg value that already consists of the input baseline + online
        // No need to use online data here
        val offlinePickData = input.offlinePickData
        val upcToItemIdMap = input.upcToItemIdMap

        // Totes data combinations
        val allDistinctContainerActivityIds = lookupAllContainerActivityIds(baselineActivityDetails, input)
        // Retrieve combined data from baseline + offline for containers/totes
        val combinedContainerActivities = allDistinctContainerActivityIds.mapNotNull { containerId ->
            val baselineContainerActivity = baselineActivityDetails.containerActivities.orEmpty().firstOrNull { it.containerId == containerId }
            // Picks - Find all picks for containerId - shorts have no effect on totes
            val matchingOfflinePickItems = offlinePickData.itemPickRequestDtos.flatMap { it.lineReqDto.orEmpty() }.filter { it.containerId == containerId }
            val matchingOfflineUndoPickItems =
                offlinePickData.undoItemPickRequestDtos.map {
                    findPickedItemUpcForUndoPickValues(
                        baselineActivityDetails.itemActivities.orEmpty(),
                        it.wrapped.undoPickRequestDto.iaId, it.wrapped.undoPickRequestDto.pickedUpcId
                    )
                }
                    .filter { it?.pickedItemUpcDto?.containerId == containerId }.filterNotNull()

            // TODO: Will container types be mismatched between data sources?

            // NOTE: While processing offline containers, use the original item db ia id instead of looking up the scanned barcode (which can indicate a different item for substitutions and
            // is also necessary for unique identification of an identical item ordered by multiple customers)
            val representativeOfflinePickItem: ItemActivityDto? = pickListOperations.getItem(baselineActivityDetails.itemActivities!!, matchingOfflinePickItems.firstOrNull()?.iaId)

            val containerType = baselineContainerActivity?.containerType ?: representativeOfflinePickItem?.storageType
            val customerOrderNumber = baselineContainerActivity?.customerOrderNumber ?: representativeOfflinePickItem?.customerOrderNumber
            val contactFirstName = baselineContainerActivity?.contactFirstName ?: representativeOfflinePickItem?.contactFirstName
            val contactLastName = baselineContainerActivity?.contactLastName ?: representativeOfflinePickItem?.contactLastName
            val contactPhoneNumber = baselineContainerActivity?.contactPhoneNumber ?: representativeOfflinePickItem?.contactPhoneNumber
            val fulfillment = baselineContainerActivity?.fulfillment ?: representativeOfflinePickItem?.fulfillment
            val routeVanNumber = baselineContainerActivity?.routeVanNumber ?: representativeOfflinePickItem?.routeVanNumber
            val shortOrderNumber = baselineContainerActivity?.shortOrderNumber ?: representativeOfflinePickItem?.shortOrderNumber
            val stopNumber = baselineContainerActivity?.stopNumber ?: representativeOfflinePickItem?.stopNumber

            val containerItemsList = baselineContainerActivity?.containerItems ?: emptyList()
            // TODO: Determine if full dataset for an item is needed here
            // TODO: Determine if fulfilledQty is appropriate or if upcQty should be used
            val addedItems = matchingOfflinePickItems.map {
                val item = pickListOperations.getItem(baselineActivityDetails.itemActivities.orEmpty(), it.iaId)
                ErItemDto(
                    itemId = item?.itemId,
                    qty = it.fulfilledQty,
                    itemDesc = item?.itemDescription,
                    imageUrl = item?.imageUrl,
                )
            }
            val combinedContainerItemsList = containerItemsList + addedItems
            val removedItems = combinedContainerItemsList.map { erItem ->
                val matchingUndoPick = matchingOfflineUndoPickItems.firstOrNull { it.itemIdBpn == erItem.itemId }
                erItem.copy(qty = erItem.qty.orZero() - matchingUndoPick?.pickedItemUpcDto?.qty.orZero())
            }

            val combinedContainerItemsListWithRemovals = removedItems.filter { it.qty.orZero() > 0.0 }

            // TODO: Will order types be mismatched between data sources?
            val reference = baselineContainerActivity?.reference ?: baselineActivityDetails.entityReference
            // FIXME: Determine what other data needs to be added/modified for display purposes from all data sources
            if (combinedContainerItemsListWithRemovals.isNotEmpty()) {
                ContainerActivityDto(
                    containerId = containerId,
                    containerType = containerType,
                    containerItems = combinedContainerItemsListWithRemovals,
                    reference = reference,
                    customerOrderNumber = customerOrderNumber,
                    contactFirstName = contactFirstName,
                    contactLastName = contactLastName,
                    contactPhoneNumber = contactPhoneNumber,
                    fulfillment = fulfillment,
                    routeVanNumber = routeVanNumber,
                    shortOrderNumber = shortOrderNumber,
                    stopNumber = stopNumber,
                    id = null, // record pick apis don't return this data
                )
            } else {
                null
            }
        }
        Timber.v("[deriveNewBaselineWithOffline] combined containers=$combinedContainerActivities")

        // Pick list items data combinations
        val combinedItemActivities = baselineActivityDetails.itemActivities?.map { baselineItemActivity ->
            // Picks - Find all picks with matching itemactivity db id

            val matchingOfflinePickItems = offlinePickData.itemPickRequestDtos.flatMap { it.lineReqDto.orEmpty() }.filter { it.iaId == baselineItemActivity.id }
            val matchingOfflinePickRequests =
                offlinePickData.itemPickRequestDtos.flatMap { request -> request.lineReqDto.orEmpty().filter { it.iaId == baselineItemActivity.id }.map { request } }
            val matchingOfflineUndoPickItems = offlinePickData.undoItemPickRequestDtos.filter { it.wrapped.undoPickRequestDto.iaId == baselineItemActivity.id }
            // Shorts - Find all shorts with matching itemactivity db id
            val matchingOfflineShortItems = offlinePickData.shortPickRequestDtos.flatMap { it.shortReqDto.orEmpty() }.filter { it.iaId == baselineItemActivity.id }
            val matchingOfflineUndoShortItems = offlinePickData.undoShortRequestDtos.filter { it.wrapped.iaId == baselineItemActivity.id }
            val matchingOfflineShortRequests =
                offlinePickData.shortPickRequestDtos.flatMap { request -> request.shortReqDto.orEmpty().filter { it.iaId == baselineItemActivity.id }.map { request } }
            // Unified processed/exception values
            val unifiedProcessedQty = (
                baselineItemActivity.processedQty.orZero() +
                    matchingOfflinePickItems.sumByDouble { it.fulfilledQty.orZero() } -
                    matchingOfflineUndoPickItems.sumByDouble { it.wrapped.undoPickRequestDto.qty.orZero() }
                ).coerceAtLeast(0.0)

            val unifiedProcessedWeight = (
                if (unifiedProcessedQty == 0.0) BigDecimal.ZERO else
                    baselineItemActivity.processedWeight.orZero().toBigDecimal() +
                        matchingOfflinePickItems.sumOf { it.netWeight.orZero().toBigDecimal() } -
                        matchingOfflineUndoPickItems.sumOf { it.wrapped.undoPickRequestDto.netWeight.orZero().toBigDecimal() }
                ).coerceAtLeast(BigDecimal.ZERO).toDouble()

            val isPickCompleted = if (unifiedProcessedQty == 0.0) false else baselineItemActivity.isPickCompleted || matchingOfflinePickItems.any { it.isPickCompleted == true }

            val unifiedExceptionQty = baselineItemActivity.exceptionQty.orZero() +
                matchingOfflineShortItems.sumByDouble { it.qty.orZero() } -
                matchingOfflineUndoShortItems.sumByDouble { it.wrapped.qty.orZero() }

            // Determine unified pickedUpcCodes
            val convertedPickItemDtoList = matchingOfflinePickRequests.map { it.toPickedItemUpcDto() }
            val combinedPickedUpcCodes = baselineItemActivity.pickedUpcCodes.orEmpty() + convertedPickItemDtoList

            val unifiedPickedUpcCodes = combinedPickedUpcCodes.mapNotNull { pickedItemUpcDto ->
                val amountToUndo = matchingOfflineUndoPickItems.sumByDouble { it.wrapped.undoPickRequestDto.qty.orZero() }
                // val matchingUndoPick = matchingOfflineUndoPickItems.firstOrNull { it.itemIdBpn == erItem.itemId }
                val updateAmount = (pickedItemUpcDto.qty.orZero() - amountToUndo).coerceAtLeast(0.0)
                if (updateAmount >= 0.0) {
                    pickedItemUpcDto.copy(qty = updateAmount)
                } else {
                    null
                }
            }

            // Determine unified shortedItemUpc
            val convertedShortItemDtoList = matchingOfflineShortRequests.map { it.toShortedItemUpcDto() }
            val combinedShortedItemUpcList = baselineItemActivity.shortedItemUpc.orEmpty() + convertedShortItemDtoList

            val unifiedShortedItemUpcList = combinedShortedItemUpcList.mapNotNull { shortedItemUpcDto ->
                val amountToUndo = matchingOfflineUndoShortItems.sumByDouble { it.wrapped.qty.orZero() }
                // val matchingUndoPick = matchingOfflineUndoPickItems.firstOrNull { it.itemIdBpn == erItem.itemId }
                val updateAmount = (shortedItemUpcDto.exceptionQty.orZero() - amountToUndo).coerceAtLeast(0.0)
                if (updateAmount > 0.0) {
                    shortedItemUpcDto.copy(exceptionQty = updateAmount)
                } else {
                    null
                }
            }

            Timber.v(
                "[deriveBaselineAndOnlineCombination] bpn=${baselineItemActivity.itemId}, customerOrderNumber=${baselineItemActivity.customerOrderNumber}," +
                    " iaId=${baselineItemActivity.id}, unifiedProcessedQty=$unifiedProcessedQty, unifiedExceptionQty=$unifiedExceptionQty," +
                    " qty=${baselineItemActivity.qty}, description=${baselineItemActivity.itemDescription}, unifiedPickedUpcCodes=$unifiedPickedUpcCodes," +
                    " unifiedShortedItemUpcList=$unifiedShortedItemUpcList"
            )

            // Log transformations
            Timber.v(
                "[deriveNewBaselineWithOffline]bpn=${baselineItemActivity.itemId}, customerOrderNumber=${baselineItemActivity.customerOrderNumber}," +
                    " iaId=${baselineItemActivity.id}, unifiedProcessedQty=$unifiedProcessedQty, unifiedExceptionQty=$unifiedExceptionQty," +
                    " qty=${baselineItemActivity.qty}, description=${baselineItemActivity.itemDescription}, unifiedPickedUpcCodes=$unifiedPickedUpcCodes," +
                    " unifiedShortedItemUpcList=$unifiedShortedItemUpcList"
            )

            // FIXME: Determine what other data needs to be added/modified for display purposes from all data sources
            baselineItemActivity.copy(
                processedQty = unifiedProcessedQty,
                processedWeight = unifiedProcessedWeight,
                exceptionQty = unifiedExceptionQty,
                isPickCompleted = isPickCompleted,
                pickedUpcCodes = unifiedPickedUpcCodes,
                shortedItemUpc = unifiedShortedItemUpcList,
            )
        }
        Timber.v("[deriveNewBaselineWithOffline] combined items=$combinedItemActivities")

        val combinedActivityDetails = baselineActivityDetails.copy(
            containerActivities = combinedContainerActivities.takeIf { it.isNotEmpty() }, // leaving pick list in same state as it is coming from the backend when container activities is empty
            itemActivities = combinedItemActivities,
        )
        return combinedActivityDetails
    }

    private fun ItemPickRequestDto.toPickedItemUpcDto(): PickedItemUpcDto {
        return lineReqDto!!.first().let {
            PickedItemUpcDto(
                containerId = it.containerId,
                isSubstitution = it.substitution,
                subReasonCode = it.subReasonCode,
                substituteItemDesc = it.substituteItemDesc,
                qty = it.fulfilledQty,
                upcQty = it.upcQty,
                upc = it.upcId,
                // FIXME: This was null but I the time from the network response is simply the request time sent (so using request time now)
                pickedTime = it.pickedTime, // will not have an exact match when offline
                upcId = null, // will not have this when offline as it comes from the actual network response
                userId = it.userId,
                netWeight = it.netWeight
            )
        }
    }

    private fun ShortPickRequestDto.toShortedItemUpcDto(): ShortedItemUpcDto {
        return shortReqDto!!.first().let {
            ShortedItemUpcDto(
                exceptionQty = it.qty,
                exceptionReasonCode = it.shortageReasonCode,
                exceptionReasonText = it.shortageReasonText,
                // FIXME: Should this be null (see ItemPickRequestDto.toPickedItemUpcDto above as they are related in logic)
                shortedTime = it.shortedTime, // will not have an exact match when offline
                shortedId = null, // will not have this when offline as it comes from the actual network response
                userId = it.userId
            )
        }
    }

    /**
     * Retrieves a unified list of container ids from all data sources.
     *
     * Since a pick list can have null for containerActivities[] (especially when first starting a pick list with nothing picked),
     * all container ids from all data sources must be combined to determine the current state of containers
     *
     * Don't directly call this function from project source! public for quick testing
     */
    internal fun lookupAllContainerActivityIds(baselineActivityDetails: ActivityDto, input: PickListProcessorInput): List<String> {
        val onlineInMemoryPickData = input.onlineInMemoryPickData
        val offlinePickData = input.offlinePickData

        // Baseline
        val distinctBaselineContainerDetailsContainerIds = baselineActivityDetails.containerActivities.orEmpty().distinctBy { it.containerId }.map { it.containerId }
        // Picks
        val distinctOnlinePickContainerActivityIds = onlineInMemoryPickData.itemPickRequestDtos.flatMap { it.request.lineReqDto.orEmpty() }.distinctBy { it.containerId }.map { it.containerId }
        val distinctOfflinePickContainerActivityIds = offlinePickData.itemPickRequestDtos.flatMap { it.lineReqDto.orEmpty() }.distinctBy { it.containerId }.map { it.containerId }

        val allDistinctContainerActivityIds: List<String> = distinctBaselineContainerDetailsContainerIds.union(distinctOnlinePickContainerActivityIds)
            .union(distinctOfflinePickContainerActivityIds)
            .filterNotNull()
        Timber.v("[lookupAllContainerActivityIds] allDistinctContainerActivityIds=$allDistinctContainerActivityIds")
        return allDistinctContainerActivityIds
    }

    /** Basically a pair of itemDbActivity and associated [PickedItemUpcDto] */
    private data class PickedItemInfo(val itemIdBpn: String?, val pickedItemUpcDto: PickedItemUpcDto)

    /** Given an itemDbActivityId and pickedUpcId, find the matching item and return PickedItemInfo */
    private fun findPickedItemUpcForUndoPickValues(itemActivities: List<ItemActivityDto>, itemDbActivityId: Long?, pickedUpcId: Long?): PickedItemInfo? {
        val matchingItem = itemActivities.firstOrNull { it.id == itemDbActivityId }
        return matchingItem?.pickedUpcCodes.orEmpty().firstOrNull { it.upcId == pickedUpcId }?.let { PickedItemInfo(matchingItem!!.itemId, it) }
    }

    /** Retrieves a unified list of picked upc ids from baseline and online data sources. */
    internal fun lookupBaselineOnlinePickedUpcIds(baselineActivityDetails: ActivityDto, onlineInMemoryPickData: OnlineInMemoryPickData): Set<Long> {
        // Baseline
        val distinctBaselineItemActivitiesPickedUpcIds = baselineActivityDetails.itemActivities.orEmpty()
            .flatMap { it.pickedUpcCodes.orEmpty() }
            .distinctBy { it.upcId }
            .mapNotNull { it.upcId }
        // Picks (don't need to consider undo picks as to undo a pick the upcId must be present in the baseline or in an online pick)
        val distinctOnlinePickUpcIds = onlineInMemoryPickData.itemPickRequestDtos
            .flatMap { it.response }
            .flatMap { it.pickedUpcCodes.orEmpty() }
            .distinctBy { it.upcId }
            .mapNotNull { it.upcId }

        val allDistinctPickedUpcIds: Set<Long> = distinctBaselineItemActivitiesPickedUpcIds.union(distinctOnlinePickUpcIds).toSet()
        Timber.v("[lookupBaselineOnlinePickedUpcIds] allDistinctPickedUpcIds=$allDistinctPickedUpcIds")
        return allDistinctPickedUpcIds
    }

    // Any benefit to optimizing online pick/undo picks (culling stale values per upcId) to maintain a single representation of upcId?
    // If you did that the list sizes for recordPick and undoPick would be limited to only the relevant list items needed with older values thrown out
    // (ex: 100 actions would be more processing but shouldn't necessarily make things easier (possibly easier to debug))

    /**
     * Logic to retrieve pickedUpcCodes[]:
     *
     * 1. Create a list of all known pickedUpcIds by creating a set from the values in the baseline activity details + online record pick
     * 2. For each given pickedUpcId, create list of newest recordPick request/response pairs (by request time) for each unique pickedUpcId (ex: function to retrieve this from online pick data)
     *     * EX: List of request/responses pairs that contains recordPick with request for 1010 and response containing the matching 1010 pickedUpcCodes object (or missing object when all picks have been undone)
     * 3. For each given pickedUpcId, create list newest undoPick request/response (by request time) for each unique pickedUpcId (ex: function to retrieve this from online pick data)
     * 4. For each given pickedUpcId, lookup matching baseline, recordPick, and undoPick responses, choosing the newest of all to use as the source of truth to represent the given pickedUpcId (could be null if it has been unpicked completely)
     * 5. Manually build up the new pickedUpcCodes list (to be empty list when all pickedUpcCode values are null)
     */
    internal fun ItemActivityDto.resolvePickedItemUpcs(baselineActivityDetails: ActivityDto, onlineInMemoryPickData: OnlineInMemoryPickData): List<PickedItemUpcDto> {
        val allPickedUpcIds = lookupBaselineOnlinePickedUpcIds(baselineActivityDetails, onlineInMemoryPickData)
        val unifiedPickedUpcCodes = mutableListOf<PickedItemUpcDto>()
        allPickedUpcIds.forEach { pickedUpcId ->
            val matchingBaselineItemActivityPickedUpcDto = pickedUpcCodes.orEmpty().firstOrNull { it.upcId == pickedUpcId }
            val newestOnlinePickRequestResponse = onlineInMemoryPickData.newestPickRequestResponse(id, pickedUpcId)
            val newestOnlineUndoPickRequestResponse = onlineInMemoryPickData.newestUndoPickRequestResponse(id, pickedUpcId)

            val latestOnlinePickTime = newestOnlinePickRequestResponse?.request?.pickedTime ?: ZONED_DATE_TIME_EPOCH
            val latestOnlineUndoPickTime = newestOnlineUndoPickRequestResponse?.request?.actionTime ?: ZONED_DATE_TIME_EPOCH
            val latestPickedUpcCode = when {
                latestOnlinePickTime.isAfter(latestOnlineUndoPickTime) -> newestOnlinePickRequestResponse?.response?.matchingPickedItemUpcDto(pickedUpcId)
                latestOnlineUndoPickTime.isAfter(latestOnlinePickTime) -> newestOnlineUndoPickRequestResponse?.response?.matchingPickedItemUpcDto(pickedUpcId)
                else -> matchingBaselineItemActivityPickedUpcDto
            }
            if (latestPickedUpcCode != null) {
                unifiedPickedUpcCodes.add(latestPickedUpcCode)
            }
        }
        return unifiedPickedUpcCodes
    }

    /** Returns list of all item pick values that for [itemActivityId]] */
    internal fun OnlineInMemoryPickData.matchingOnlinePickRequestResponses(itemActivityId: Long?): List<RequestResponse<ItemPickRequestDto, List<PickItemDto>>> {
        return itemPickRequestDtos.flatMap { requestResponse ->
            requestResponse.request.lineReqDto.orEmpty().filter { it.iaId == itemActivityId }.map { requestResponse }
        }
    }

    /** Returns last item in online pick array for [itemActivityId] and [pickedUpcId] or null if it doesn't exist */
    internal fun OnlineInMemoryPickData.newestPickRequestResponse(itemActivityId: Long?, pickedUpcId: Long): RequestResponse<ItemPickRequestDto, List<PickItemDto>>? {
        return matchingOnlinePickRequestResponses(itemActivityId).flatMap { requestResponse ->
            requestResponse.response.flatMap { it.pickedUpcCodes.orEmpty() }.filter { it.upcId == pickedUpcId }.map { requestResponse }
        }.lastOrNull()
    }

    /** Returns list of all item undo pick values that for [itemActivityId]] */
    /** Currently OnlineInMemoryPickData and its methods are not used in debug/production build.
     * As per Jira story "ACIP-278405 undo picks for live order view", we have consolidated undo picks api call for live order view,
     * in future if we are planning to use the OnlineInMemoryPickData please update this method like below commented code
     * as the Request type is changed.
     */
    /**
     * internal fun OnlineInMemoryPickData.matchingOnlineUndoPickRequestResponses(itemActivityId: Long?): List<RequestResponse<ActionTimeWrapper<List<UndoPickLocalDto>>, List<PickItemDto>>> {
     return undoItemPickRequestDtos.filter { requestResponse ->
     requestResponse.request.wrapped.first().undoPickRequestDto.iaId == itemActivityId
     }
     }
     */
    internal fun OnlineInMemoryPickData.matchingOnlineUndoPickRequestResponses(itemActivityId: Long?): List<RequestResponse<ActionTimeWrapper<UndoPickLocalDto>, List<PickItemDto>>> {
        return undoItemPickRequestDtos.filter { requestResponse ->
            requestResponse.request.wrapped.undoPickRequestDto.iaId == itemActivityId
        }
    }

    /** Returns last item in online undo pick array for [itemActivityId] and [pickedUpcId] or null if it doesn't exist */
    /** Currently OnlineInMemoryPickData and its methods are not used in debug/production build.
     * As per Jira story "ACIP-278405 undo picks for live order view", we have consolidated undo picks api call for live order view,
     * in future if we are planning to use the OnlineInMemoryPickData please update this method like below commented code
     * as the Request type is changed.
     */
    /**
     * internal fun OnlineInMemoryPickData.newestUndoPickRequestResponse(itemActivityId: Long?, pickedUpcId: Long): RequestResponse<ActionTimeWrapper<List<UndoPickLocalDto>>, List<PickItemDto>>? {
     return matchingOnlineUndoPickRequestResponses(itemActivityId).lastOrNull { it.request.wrapped.first().undoPickRequestDto.pickedUpcId == pickedUpcId }
     }
     */
    internal fun OnlineInMemoryPickData.newestUndoPickRequestResponse(itemActivityId: Long?, pickedUpcId: Long): RequestResponse<ActionTimeWrapper<UndoPickLocalDto>, List<PickItemDto>>? {
        return matchingOnlineUndoPickRequestResponses(itemActivityId).lastOrNull { it.request.wrapped.undoPickRequestDto.pickedUpcId == pickedUpcId }
    }

    internal fun List<PickItemDto>.matchingPickedItemUpcDto(pickedUpcId: Long): PickedItemUpcDto? {
        return flatMap { pickItemDto -> pickItemDto.pickedUpcCodes.orEmpty().filter { it.upcId == pickedUpcId } }.firstOrNull()
    }

    /** Retrieves a unified list of shorted ids from baseline and online data sources. */
    internal fun lookupBaselineOnlineShortedIds(baselineActivityDetails: ActivityDto, onlineInMemoryPickData: OnlineInMemoryPickData): Set<Long> {
        // Baseline
        val distinctBaselineItemActivitiesShortedUpcIds = baselineActivityDetails.itemActivities.orEmpty()
            .flatMap { it.shortedItemUpc.orEmpty() }
            .distinctBy { it.shortedId }
            .mapNotNull { it.shortedId }
        // Shorts (don't need to consider undo shorts as to undo a short the shortedId must be present in the baseline or in an online short)
        val distinctOnlineShortedIds = onlineInMemoryPickData.shortPickRequestDtos
            .flatMap { it.response }
            .flatMap { it.shortageReasonCodes.orEmpty() }
            .distinctBy { it.shortedId }
            .mapNotNull { it.shortedId }

        val allDistinctShortedIds: Set<Long> = distinctBaselineItemActivitiesShortedUpcIds.union(distinctOnlineShortedIds).toSet()
        Timber.v("[lookupBaselineOnlineShortedIds] allDistinctShortedIds=$allDistinctShortedIds")
        return allDistinctShortedIds
    }

    /**
     * Logic to retrieve shortedItemUpc[]:
     *
     * 1. Create a list of all known shortedIds by creating a set from the values in the baseline activity details + online record short
     * 2. For each given shortedId, create list of newest recordShort request/response pairs (by request time) for each unique shortedId (ex: function to retrieve this from online short data)
     *     * EX: List of request/responses pairs that contains recordShort with request for 1010 and response containing the matching 1010 shortedItemUpc object (or missing object when all shorts have been undone)
     * 3. For each given shortedId, create list newest undoShort request/response (by request time) for each unique shortedId (ex: function to retrieve this from online short data)
     * 4. For each given shortedId, lookup matching baseline, recordShort, and undoShort responses, choosing the newest of all to use as the source of truth to represent the given shortedId (could be null if it has been undo shorted completely)
     * 5. Manually build up the new shortedItemUpc list (to be empty list when all shortedItemUpc values are null)
     */
    internal fun ItemActivityDto.resolveShortedItemUpcs(baselineActivityDetails: ActivityDto, onlineInMemoryPickData: OnlineInMemoryPickData): List<ShortedItemUpcDto> {
        val allShortedIds = lookupBaselineOnlineShortedIds(baselineActivityDetails, onlineInMemoryPickData)
        val unifiedShortedItemUpcs = mutableListOf<ShortedItemUpcDto>()
        allShortedIds.forEach { shortedId ->
            val matchingBaselineItemActivityShortedItemUpcDto = shortedItemUpc.orEmpty().firstOrNull { it.shortedId == shortedId }
            val newestOnlinePickRequestResponse = onlineInMemoryPickData.newestShortRequestResponse(id, shortedId)
            val newestOnlineUndoPickRequestResponse = onlineInMemoryPickData.newestUndoShortRequestResponse(id, shortedId)

            val latestOnlineShortTime = newestOnlinePickRequestResponse?.request?.shortedTime ?: ZONED_DATE_TIME_EPOCH
            val latestOnlineUndoShortTime = newestOnlineUndoPickRequestResponse?.request?.actionTime ?: ZONED_DATE_TIME_EPOCH
            val latestShortedItemUpcDto = when {
                latestOnlineShortTime.isAfter(latestOnlineUndoShortTime) -> newestOnlinePickRequestResponse?.response?.matchingShortedItemUpcDto(shortedId)
                latestOnlineUndoShortTime.isAfter(latestOnlineShortTime) -> newestOnlineUndoPickRequestResponse?.response?.matchingShortedItemUpcDto(shortedId)
                else -> matchingBaselineItemActivityShortedItemUpcDto
            }
            if (latestShortedItemUpcDto != null) {
                unifiedShortedItemUpcs.add(latestShortedItemUpcDto)
            }
        }
        return unifiedShortedItemUpcs
    }

    /** Returns list of all item pick values that for [itemActivityId]] */
    internal fun OnlineInMemoryPickData.matchingOnlineShortRequestResponses(itemActivityId: Long?): List<RequestResponse<ShortPickRequestDto, List<ShortItemDto>>> {
        return shortPickRequestDtos.flatMap { requestResponse ->
            requestResponse.request.shortReqDto.orEmpty().filter { it.iaId == itemActivityId }.map { requestResponse }
        }
    }

    /** Returns last item in online pick array for [itemActivityId] and [shortedId] or null if it doesn't exist */
    internal fun OnlineInMemoryPickData.newestShortRequestResponse(itemActivityId: Long?, shortedId: Long): RequestResponse<ShortPickRequestDto, List<ShortItemDto>>? {
        return matchingOnlineShortRequestResponses(itemActivityId).flatMap { requestResponse ->
            requestResponse.response.flatMap { it.shortageReasonCodes.orEmpty() }.filter { it.shortedId == shortedId }.map { requestResponse }
        }.lastOrNull()
    }

    /** Returns list of all item undo pick values that for [itemActivityId]] */
    internal fun OnlineInMemoryPickData.matchingOnlineUndoShortRequestResponses(itemActivityId: Long?): List<RequestResponse<ActionTimeWrapper<UndoShortRequestDto>, List<ShortItemDto>>> {
        return undoShortRequestDtos.filter { requestResponse ->
            requestResponse.request.wrapped.iaId == itemActivityId
        }
    }

    /** Returns last item in online undo pick array for [itemActivityId] and [shortedId] or null if it doesn't exist */
    internal fun OnlineInMemoryPickData.newestUndoShortRequestResponse(itemActivityId: Long?, shortedId: Long): RequestResponse<ActionTimeWrapper<UndoShortRequestDto>, List<ShortItemDto>>? {
        return matchingOnlineUndoShortRequestResponses(itemActivityId).lastOrNull { it.request.wrapped.shortedItemId == shortedId }
    }

    internal fun List<ShortItemDto>.matchingShortedItemUpcDto(shortedId: Long): ShortedItemUpcDto? {
        return flatMap { shortedItemDto -> shortedItemDto.shortageReasonCodes.orEmpty().filter { it.shortedId == shortedId } }.firstOrNull()
    }
}
