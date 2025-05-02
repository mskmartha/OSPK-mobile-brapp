package com.albertsons.acupick.data.picklist

import com.albertsons.acupick.data.model.ItemSearchResult
import com.albertsons.acupick.data.model.toItemSearchResult
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.barcode.PickingContainer
import com.albertsons.acupick.data.model.copyWithCustomerSpecificInformationRemoved
import com.albertsons.acupick.data.model.itemIdOrNull
import com.albertsons.acupick.data.model.response.ContainerActivityDto
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.repository.SiteRepository
import timber.log.Timber

/** Provides apis to retrieve certain information from the pick list. Separated from the repository and processor so that they both can use the operations. */
interface PickListOperations {
    /** Returns the itemId associated with a UPC code, or null if no such item is found */
    fun getItemId(items: List<ItemActivityDto>, upcToItemIdMap: Map<String, String>, itemBarcodeType: BarcodeType.Item?): String?

    /**
     * Returns the item matching the given [itemBarcodeType] with all order/customer specific info nulled out, or null when there is no match for the given [itemBarcodeType]
     * Useful for scenarios where information on the item is needed but not any additional information for a particular customer order.
     */
    fun getItemWithoutOrderOrCustomerDetails(items: List<ItemActivityDto>, upcToItemIdMap: Map<String, String>, itemBarcodeType: BarcodeType.Item?): ItemSearchResult

    /**
     * For non-batch picklists, the item matching the given [itemBarcodeType], or null when there is no match for the given [itemBarcodeType].
     * For batch picklists where identical items from different customer orders are present, the matching item is returned following the below rules (with null for no match):
     *
     * * Use the selected item if the barcode lookup item matches (Note: Since the barcode item that was found would be the first occurrence in the picklist, regardless of customer order number).
     * This functionality allows a picker to scroll to a given item and have scans affect the selected item.
     *
     * * Select the first matching item that doesn't allow substitutions so that those customers will have the in stock items picked first and other customer orders that allow substitutions could
     * receive them if stock is not available for all customers item qty for the picklist.
     * Intended to be used when entering a barcode in the main picking flow to select/highlight/scroll to the item returned from this function.
     */
    fun getNextItemToSelectForScan(items: List<ItemActivityDto>, upcToItemIdMap: Map<String, String>, itemBarcodeType: BarcodeType.Item?, currentSelectedItem: ItemActivityDto?): ItemSearchResult

    /** Returns the item matching the given [itemBarcodeType] or null, filtered by [customerOrderNumber] (when not null) */
    fun getItem(items: List<ItemActivityDto>, upcToItemIdMap: Map<String, String>, itemBarcodeType: BarcodeType.Item?, customerOrderNumber: String?): ItemActivityDto?

    /** Returns the item matching the given [itemIaDbId] or null. */
    fun getItem(items: List<ItemActivityDto>, itemIaDbId: Long?): ItemActivityDto?

    /** Returns the item matching the given [String] or null. */
    fun getItem(items: List<ItemActivityDto>, itemBpnId: String?, customerOrderNumber: String? = null): ItemActivityDto?

    /** Returns the tote matching the given [toteBarcodeType] or null. */
    fun getTote(totes: List<ContainerActivityDto>, toteBarcodeType: PickingContainer): ContainerActivityDto?

    /** Searches for first tote that matches given [item] of the same StorageType. Can be used to provide a hint to the picker indicating which tote to place the scanned item into. */
    fun findExistingValidToteForItem(
        totes: List<ContainerActivityDto>,
        item: ItemActivityDto,
    ): ContainerActivityDto?

    /** True if tote and item
     * 1) order number and
     * 2) storage type match.
     * 3) is MFC barcode or not matches expected value
     * 4) If it is an MFC picking container starts with 999 for frozen and chilled and 998 for others
     * otherwise false
     * */
    fun isItemIntoPickingContainerValid(item: ItemActivityDto?, totes: List<ContainerActivityDto>, toteBarcodeType: PickingContainer, shouldUseMFCTote: Boolean): Boolean
}

class PickListOperationsImplementation(
    private val siteRepo: SiteRepository
) : PickListOperations {
    override fun getItemId(items: List<ItemActivityDto>, upcToItemIdMap: Map<String, String>, itemBarcodeType: BarcodeType.Item?): String? = when (itemBarcodeType) {
        is BarcodeType.Item.Each -> getItemWithoutOrderOrCustomerDetails(items, upcToItemIdMap, itemBarcodeType).itemIdOrNull()
        null -> null
        else -> upcToItemIdMap[itemBarcodeType.catalogLookupUpc]
    }

    override fun getItemWithoutOrderOrCustomerDetails(items: List<ItemActivityDto>, upcToItemIdMap: Map<String, String>, itemBarcodeType: BarcodeType.Item?): ItemSearchResult {
        return when (itemBarcodeType) {
            is BarcodeType.Item.Each -> items.find { it.id == itemBarcodeType.itemActivityDbId }.toItemSearchResult(siteRepo.fixedItemTypesEnabled)
            else -> items.find { it.itemId == getItemId(items, upcToItemIdMap, itemBarcodeType) }.toItemSearchResult(siteRepo.fixedItemTypesEnabled)
        }.copyWithCustomerSpecificInformationRemoved(siteRepo.fixedItemTypesEnabled)
    }

    override fun getNextItemToSelectForScan(
        items: List<ItemActivityDto>,
        upcToItemIdMap: Map<String, String>,
        itemBarcodeType: BarcodeType.Item?,
        currentSelectedItem: ItemActivityDto?,
    ): ItemSearchResult {
        return when (itemBarcodeType) {
            is BarcodeType.Item.Each -> {
                items.find { it.id == itemBarcodeType.itemActivityDbId }.toItemSearchResult(siteRepo.fixedItemTypesEnabled)
            }
            else -> {
                val matchingItemsFromBarcode = run {
                    val allMatchingItemsFromBarcode = items.filter { it.itemId == getItemId(items, upcToItemIdMap, itemBarcodeType) }
                    val matchingItemsFromBarcodeNotFullyPicked = allMatchingItemsFromBarcode.filter { !it.isFullyPicked() }
                    matchingItemsFromBarcodeNotFullyPicked.takeIf { it.isNotEmpty() } ?: allMatchingItemsFromBarcode
                }

                val firstMatchingItem = matchingItemsFromBarcode.firstOrNull()
                val scannedItemMatchesSelectedItem = currentSelectedItem?.itemId == firstMatchingItem?.itemId
                val firstMatchingItemWithNoSubstitutionsAllowed = matchingItemsFromBarcode.firstOrNull { it.subAllowed == false }
                when {
                    // For batch picklists, there can be multiple identical items (from multiple customer orders). Use the selected item if the barcode lookup item matches
                    // (Note: Since the barcode item that was found would be the first occurrence in the picklist, regardless of customer order number).
                    // This functionality allows a picker to scroll to a given item and have scans affect the selected item.
                    scannedItemMatchesSelectedItem -> currentSelectedItem.toItemSearchResult(siteRepo.fixedItemTypesEnabled, itemBarcodeType)
                    // For batch picklists, there can be multiple identical items (from multiple customer orders). Select the first matching item that doesn't allow substitutions so that
                    // those customers will have the in stock items picked first and other customer orders that allow substitutions could receive them if stock is not available
                    // for all customer's item qty for the picklist.
                    firstMatchingItemWithNoSubstitutionsAllowed != null -> firstMatchingItemWithNoSubstitutionsAllowed.toItemSearchResult(siteRepo.fixedItemTypesEnabled, itemBarcodeType)
                    // For all other cases, fall back to the first matching item (pre-batch behavior)
                    else -> firstMatchingItem.toItemSearchResult(siteRepo.fixedItemTypesEnabled, itemBarcodeType)
                }
            }
        }
    }

    override fun getItem(items: List<ItemActivityDto>, upcToItemIdMap: Map<String, String>, itemBarcodeType: BarcodeType.Item?, customerOrderNumber: String?): ItemActivityDto? {
        val filteredItems = items.filter { if (customerOrderNumber != null) it.customerOrderNumber == customerOrderNumber else true }
        return when (itemBarcodeType) {
            is BarcodeType.Item.Each -> filteredItems.find { it.id == itemBarcodeType.itemActivityDbId }
            else -> filteredItems.find { it.itemId == getItemId(filteredItems, upcToItemIdMap, itemBarcodeType) }
        }
    }

    override fun getItem(items: List<ItemActivityDto>, itemIaDbId: Long?): ItemActivityDto? {
        return items.firstOrNull { it.id == itemIaDbId }
    }

    override fun getItem(items: List<ItemActivityDto>, itemBpnId: String?, customerOrderNumber: String?): ItemActivityDto? {
        val filteredItems = items.filter { if (customerOrderNumber != null) it.customerOrderNumber == customerOrderNumber else true }
        return filteredItems.firstOrNull { it.itemId == itemBpnId }
    }

    override fun getTote(totes: List<ContainerActivityDto>, toteBarcodeType: PickingContainer): ContainerActivityDto? {
        return totes.find { it.containerId == toteBarcodeType.rawBarcode }
    }

    override fun findExistingValidToteForItem(
        totes: List<ContainerActivityDto>,
        item: ItemActivityDto,
    ): ContainerActivityDto? {
        return totes.lastOrNull { tote ->
            itemsMatchPreviouslyScannedItems(item, tote)
        }
    }

    override fun isItemIntoPickingContainerValid(item: ItemActivityDto?, totes: List<ContainerActivityDto>, toteBarcodeType: PickingContainer, shouldUseMFCToteLicensePlate: Boolean): Boolean {
        if (item == null) {
            return false
        }
        val orderTypeValidatoin = if (shouldUseMFCToteLicensePlate) {
            // MFC reshop totes are not valid for picking on MFC totes so only MFC tote license plates are accepted
            toteBarcodeType is BarcodeType.MfcPickingToteLicensePlate &&
                isMfcToteFormatValidForItem(item, toteBarcodeType)
        } else {
            toteBarcodeType is BarcodeType.Tote
        }

        return orderTypeValidatoin && itemMatchesPreviousItemsInTote(item, totes, toteBarcodeType)
    }

    private fun itemMatchesPreviousItemsInTote(item: ItemActivityDto, totes: List<ContainerActivityDto>, toteBarcodeType: PickingContainer): Boolean {
        val matchingTote = getTote(totes, toteBarcodeType)
        return (itemsMatchPreviouslyScannedItems(item, matchingTote, toteBarcodeType, enableLogging = true))
    }

    private fun isMfcToteFormatValidForItem(item: ItemActivityDto, toteBarcodeType: BarcodeType.MfcPickingToteLicensePlate): Boolean {
        return toteBarcodeType.mfcStorageTypes?.contains(item.storageType) == true
    }

    /** True if item/tote aren't null and they have the same storage type and customerOrderNumber. */
    private fun itemsMatchPreviouslyScannedItems(
        item: ItemActivityDto?,
        tote: ContainerActivityDto?,
        toteBarcodeType: PickingContainer? = null,
        enableLogging: Boolean = false,
    ): Boolean {
        // TODO: Possibly change from Boolean to more information enum/sealed classed type for different failure
        return (item == null || tote == null || (item.storageType == tote.containerType && item.customerOrderNumber == tote.customerOrderNumber)).also {
            if (enableLogging) {
                when {
                    tote == null -> Timber.d("[isValidItemToteCombination] no matching tote found for $toteBarcodeType - new tote can contain any storage type")
                    item != null && item.storageType != tote.containerType -> {
                        Timber.w("[isValidItemToteCombination] item storage type (${item.storageType}) incompatible with tote storage type (${tote.containerType})")
                    }
                    item != null && item.customerOrderNumber != tote.customerOrderNumber -> {
                        Timber.w("[isValidItemToteCombination] item customer order number (${item.customerOrderNumber}) mismatch with tote customer order number (${tote.customerOrderNumber})")
                    }
                }
            }
        }
    }
}
