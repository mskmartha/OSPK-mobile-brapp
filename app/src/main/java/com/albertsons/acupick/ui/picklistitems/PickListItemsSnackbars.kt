package com.albertsons.acupick.ui.picklistitems

import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.FulfilledQuantityResult
import com.albertsons.acupick.data.model.ScannedPickItem
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.models.SnackBarEvent
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.getOrZero

fun showEndPickSnackBar(action: () -> Unit) =
    SnackBarEvent<Long>(
        prompt = StringIdHelper.Id(R.string.tap_end_pick),
        isIndefinite = true,
        cta = StringIdHelper.Id(R.string.end_pick),
        action = { action.invoke() }
    )

fun showWeightedItemScanbar(selectedItem: ItemActivityDto?, showOrderIssue: Boolean, scanIssueEnabled: Boolean, action: () -> Unit) =
    SnackBarEvent<Long>(
        prompt = StringIdHelper.Plural(
            idRes = R.plurals.scan_weighed_item_plural,
            quantity = selectedItem?.qty?.toInt()?.minus(selectedItem.processedQty?.toInt() ?: 0) ?: 1
        ),
        isIndefinite = true,
        cta = if (showOrderIssue && scanIssueEnabled) StringIdHelper.Id(R.string.item_scanning_issue) else null,
        action = { action.invoke() }
    )

fun showPricedItemSnackbar(scanIssueEnabled: Boolean, showOrderIssue: Boolean, action: () -> Unit) =
    SnackBarEvent<Long>(
        prompt = StringIdHelper.Id(R.string.scan_the_barcode),
        isIndefinite = true,
        cta = if (showOrderIssue && scanIssueEnabled) StringIdHelper.Id(R.string.item_scanning_issue) else null,
        action = { action.invoke() }
    )

fun showPricedWeightedItemSnackbar(scanIssueEnabled: Boolean, showOrderIssue: Boolean, action: () -> Unit) =
    SnackBarEvent<Long>(
        prompt = StringIdHelper.Id(R.string.continue_pick_scan_barcode),
        isIndefinite = true,
        cta = if (showOrderIssue && scanIssueEnabled) StringIdHelper.Id(R.string.item_scanning_issue) else null,
        action = { action.invoke() }
    )

fun showPricedEachItemSnackbar(scanIssueEnabled: Boolean, showOrderIssue: Boolean, action: () -> Unit) =
    SnackBarEvent<Long>(
        prompt = StringIdHelper.Id(R.string.scan_the_barcode),
        isIndefinite = true,
        cta = if (showOrderIssue && scanIssueEnabled) StringIdHelper.Id(R.string.item_scanning_issue) else null,
        action = { action.invoke() }
    )

fun showEachesItemNoPLUSnackbar(action: () -> Unit) =
    SnackBarEvent<Long>(
        prompt = StringIdHelper.Id(R.string.enter_plu_number),
        isIndefinite = true,
        cta = StringIdHelper.Id(R.string.enter),
        action = { action.invoke() }
    )

fun showEachesItemPLUSnackbar(selectedItem: ItemActivityDto, action: () -> Unit) =
    SnackBarEvent<Long>(
        prompt = StringIdHelper.Format(R.string.enter_amount_for_plu_format, selectedItem.pluList?.getOrNull(0).orEmpty()),
        isIndefinite = true,
        cta = StringIdHelper.Id(R.string.enter),
        action = { action.invoke() }
    )

fun showGenericItemSnackbar(scanIssueEnabled: Boolean, showOrderIssue: Boolean, action: () -> Unit) =
    SnackBarEvent<Long>(
        prompt = StringIdHelper.Id(R.string.scan_upc_item),
        isIndefinite = true,
        cta = if (showOrderIssue && scanIssueEnabled) StringIdHelper.Id(R.string.item_scanning_issue) else null,
        action = { action.invoke() }
    )

fun showToteSnackbar(toteId: String?) =
    SnackBarEvent<Long>(
        prompt = if (toteId.isNotNullOrBlank()) {
            StringIdHelper.Format(R.string.scan_specific_tote_format, toteId?.takeLast(TOTE_UI_COUNT).orEmpty())
        } else {
            StringIdHelper.Id(R.string.scan_to_new_tote)
        },
        isIndefinite = true
    )

fun displayItemScanSuccessfulSnackbar(sellByType: SellByType, rawBarcode: String, isFromManualEntry: Boolean, weight: Double) =
    SnackBarEvent<Long>(
        isSuccess = true,
        prompt = when (sellByType) {
            SellByType.Weight -> {
                when (isFromManualEntry) {
                    true -> StringIdHelper.Format(R.string.item_entered_weight_format, rawBarcode)
                    else -> StringIdHelper.Format(R.string.item_scanned_weight_format, rawBarcode)
                }
            }
            SellByType.Each -> {
                StringIdHelper.Format(R.string.item_entered_plu_format, rawBarcode)
            }
            SellByType.PriceWeighted -> {
                StringIdHelper.Format(R.string.item_scanned_price_weighted_format, weight.toString())
            }
            else -> {
                // The item is being picked by UPC or is a Priced Item
                when (isFromManualEntry) {
                    true -> StringIdHelper.Format(R.string.item_entered_upc_format, rawBarcode)
                    else -> StringIdHelper.Format(R.string.item_scanned_upc_format, rawBarcode)
                }
            }
        }

    )

fun displayItemScanFailureSnackbar(isAnItem: Boolean) =
    AcupickSnackEvent(
        message = if (isAnItem) {
            StringIdHelper.Id(R.string.wrong_item_scanned)
        } else {
            StringIdHelper.Id(R.string.no_item_scanned)
        },
        type = SnackType.ERROR
    )

fun displayToteScanFailureSnackbar(isPickingContainer: Boolean) =
    AcupickSnackEvent(
        message = if (isPickingContainer) {
            StringIdHelper.Id(R.string.wrong_tote_scanned)
        } else {
            StringIdHelper.Id(R.string.no_tote_scanned)
        },
        type = SnackType.ERROR
    )

fun displayScanHeavyFailureSnackbar() =
    AcupickSnackEvent(
        message = StringIdHelper.Id(R.string.heavy_pw_item_scan),
        type = SnackType.ERROR
    )

fun displayScanHeavyDsiplayType3FailureSnackbar(maxWeight: Double) =
    AcupickSnackEvent(
        message = StringIdHelper.Format(R.string.heavy_pw_display_type_3_item_scan, maxWeight.toString()),
        type = SnackType.ERROR
    )

fun displayErrorInQuanntityFailureSnackbar() =
    AcupickSnackEvent(
        message = StringIdHelper.Id(R.string.error_in_quantity_item_scan),
        type = SnackType.ERROR
    )

fun displayQuanntityExceedsSnackbar() =
    AcupickSnackEvent(
        message = StringIdHelper.Id(R.string.scan_quantity_exceeded),
        type = SnackType.ERROR
    )

fun displayToteScanSuccessfulSnackbar(toteId: String) =
    SnackBarEvent<Long>(
        isSuccess = true,
        prompt = StringIdHelper.Format(R.string.tote_scanned_format, toteId.takeLast(TOTE_UI_COUNT))
    )

fun displayToteScanSuccess(toteId: String) =
    AcupickSnackEvent(
        message = StringIdHelper.Format(R.string.tote_scanned_format, toteId.takeLast(TOTE_UI_COUNT)),
        type = SnackType.SUCCESS
    )

// Display success message after tote scan with scanned qty/weight
fun displayItemPickedSnackbar(scannedItem: ScannedPickItem, fullFilledQuantity: FulfilledQuantityResult, isLocationUpdated: Boolean) =
    AcupickSnackEvent(
        message = when {
            scannedItem.item.sellByWeightInd == SellByType.PriceWeighted -> {
                val weight = fullFilledQuantity.toWeight().toString()
                when (isLocationUpdated) {
                    true -> StringIdHelper.Format(R.string.item_scanned_price_weighted_format_location_updated, weight)
                    false -> StringIdHelper.Format(R.string.item_scanned_price_weighted_format, weight)
                }
            }
            scannedItem.item.isOrderedByWeight() -> {
                val weight = (scannedItem.barcodeType as BarcodeType.Item.Weighted).weight.toString()
                when (isLocationUpdated) {
                    true -> StringIdHelper.Format(R.string.item_scanned_price_weighted_format_location_updated, weight)
                    false -> StringIdHelper.Format(R.string.item_scanned_price_weighted_format, weight)
                }
            }
            else -> {
                val pickedQuantity = fullFilledQuantity.toQuantity() + scannedItem.item.processedQty?.toInt().getOrZero()
                when {
                    pickedQuantity >= scannedItem.item.qty?.toInt().getOrZero() -> {
                        when (isLocationUpdated) {
                            true -> StringIdHelper.Id(R.string.item_complete_location_updated)
                            false -> StringIdHelper.Id(R.string.item_complete_picked)
                        }
                    }
                    else -> {
                        when (isLocationUpdated) {
                            true -> StringIdHelper.Plural(R.plurals.number_of_items_picked_location_updated, fullFilledQuantity.toQuantity())
                            false -> StringIdHelper.Plural(R.plurals.number_of_items_picked, fullFilledQuantity.toQuantity())
                        }
                    }
                }
            }
        },
        type = SnackType.SUCCESS
    )
