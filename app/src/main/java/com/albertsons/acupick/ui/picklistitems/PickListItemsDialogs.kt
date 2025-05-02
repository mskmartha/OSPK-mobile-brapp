package com.albertsons.acupick.ui.picklistitems

import android.app.Application
import android.content.Context
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.RequestedAmount
import com.albertsons.acupick.data.model.ScannedPickItem
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.ShortReasonCode
import com.albertsons.acupick.data.model.ToteEstimate
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.ItemLocationDto
import com.albertsons.acupick.data.model.response.MAX_ITEM_DESCRIPTION_CHARACTERS_FOR_SMALL_VIEWS
import com.albertsons.acupick.data.model.response.SwapItem
import com.albertsons.acupick.data.model.response.SwapItemParams
import com.albertsons.acupick.data.model.response.netWeight
import com.albertsons.acupick.data.model.response.remainingWeight
import com.albertsons.acupick.data.model.response.requestedNetWeight
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.albertsons.acupick.infrastructure.utils.orZero
import com.albertsons.acupick.ui.bottomsheetdialog.ActionSheetDetails
import com.albertsons.acupick.ui.bottomsheetdialog.ActionSheetOptions
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetArgDataAndTag
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetType
import com.albertsons.acupick.ui.bottomsheetdialog.CustomBottomSheetArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.dialog.OrderedByWeightDialogData
import com.albertsons.acupick.ui.models.QuantityParams
import com.albertsons.acupick.ui.dialog.SENT_TO_PRINTER_TOTES_ARG_DATA
import com.albertsons.acupick.ui.itemdetails.ItemActionDbViewModel
import com.albertsons.acupick.ui.itemdetails.ItemDetailsParams
import com.albertsons.acupick.ui.itemdetails.UnPickParams
import com.albertsons.acupick.ui.itemdetails.UnPickResultParams
// import com.albertsons.acupick.ui.itemdetails.UnPickResultParams
import com.albertsons.acupick.ui.models.AlternateLocationPath
import com.albertsons.acupick.ui.models.AlternativeLocationItem
import com.albertsons.acupick.ui.models.ConfirmAmountUIData
import com.albertsons.acupick.ui.models.OriginalItemParams
import com.albertsons.acupick.ui.substitute.BulkItem
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.asItemLocation
import com.albertsons.acupick.ui.util.getFormattedValue
import com.albertsons.acupick.ui.util.notZeroOrNull
import com.albertsons.acupick.ui.util.toIdHelper
import com.albertsons.acupick.ui.util.toRawHelper
import java.io.Serializable

fun getConfirmItemSameArgDataAndTag(item: ItemActivityDto, isWeightedItemEnabled: Boolean, upcOrPlu: String) =
    CustomDialogArgDataAndTag(
        data = CustomDialogArgData(
            dialogType = DialogType.ConfirmIssueScanItem,
            titleIcon = null,
            title = R.string.confirm_same_item.toIdHelper(),
            imageUrl = item.imageUrl,
            body = R.string.is_the_item_you_are_picking_the_same_customer_requsted.toIdHelper(),
            secondaryBody = item.itemDescription?.toRawHelper(),
            questionBody = upcOrPlu.toRawHelper(),
            remainingOrderedQuantity = (item.qty?.toInt()?.minus(item.processedQty?.toInt() ?: 0)) ?: 0,
            remainingWeight = retrieveRemainingWeightForPw(item, isWeightedItemEnabled),
            positiveButtonText = R.string.confirm.toIdHelper(),
            negativeButtonText = R.string.cancel.toIdHelper(),
        ),
        tag = CONFIRM_ITEM_SAME_DIALOG_TAG
    )

private fun retrieveRemainingWeightForPw(item: ItemActivityDto, isWeightedItemEnabled: Boolean) =
    if (isWeightedItemEnabled && item.isDisplayType3PW()) item.remainingWeight else ""

// TODO ACURED_Redesign Need to remove this method call from un used place
fun getSelectedFlashWarningArgAndTag(selection: Int?, subAllowed: Boolean? = null) =
    when (selection) {
        ShortReasonCode.OUT_OF_STOCK.ordinal -> getFlashWarningOutOfStockArgAndTag(subAllowed)
        ShortReasonCode.PREP_NOT_READY.ordinal -> getFlashWarningPrepNotReadyArgAndTag(subAllowed)
        else -> null
    }

// TODO ACURED_Redesign Need to remove this method call from un used place
fun getPartnerPickWarningArgAndTag(selection: Int?) =
    when (selection) {
        ShortReasonCode.OUT_OF_STOCK.ordinal -> getPartnerPickOutOfStockWarningArgAndTag()
        ShortReasonCode.PREP_NOT_READY.ordinal -> getPartnerPickPrepNotReadyWarningArgAndTag()
        else -> null
    }

fun getPartnerPickOutOfStockWarningArgAndTag() =
    CustomDialogArgDataAndTag(
        data = CustomDialogArgData(
            titleIcon = R.drawable.ic_alert,
            title = StringIdHelper.Id(R.string.partnerpick_warning_title),
            body = StringIdHelper.Id(R.string.partnerpick_out_of_stock_warning_body),
            positiveButtonText = StringIdHelper.Id(R.string.short_out_of_stock),
            negativeButtonText = StringIdHelper.Id(R.string.cancel)
        ),
        tag = PARTNER_PICK_OUT_OF_STOCK_DIALOG_TAG
    )

fun getPartnerPickPrepNotReadyWarningArgAndTag() =
    CustomDialogArgDataAndTag(
        data = CustomDialogArgData(
            titleIcon = R.drawable.ic_alert,
            title = StringIdHelper.Id(R.string.partnerpick_warning_title),
            body = StringIdHelper.Id(R.string.partnerpick_prep_not_ready_warning_body),
            positiveButtonText = StringIdHelper.Id(R.string.prep_not_ready),
            negativeButtonText = StringIdHelper.Id(R.string.cancel)
        ),
        tag = PARTNER_PICK_PREP_NOT_READY_DIALOG_TAG
    )

fun getFlashWarningOutOfStockArgAndTag(subAllowed: Boolean? = null) =
    CustomDialogArgDataAndTag(
        data = CustomDialogArgData(
            titleIcon = R.drawable.ic_alert,
            title = StringIdHelper.Id(R.string.flash_order_warning_title),
            body = StringIdHelper.Id(
                if (subAllowed == false) {
                    R.string.flash_order_out_of_stock_no_sub_body
                } else {
                    R.string.flash_order_out_of_stock_body
                }
            ),
            positiveButtonText = StringIdHelper.Id(R.string.short_out_of_stock),
            negativeButtonText = StringIdHelper.Id(R.string.cancel),
            cancelOnTouchOutside = false
        ),
        tag = FLASH_WARNING_OOS_DIALOG_TAG
    )

fun getFlashWarningPrepNotReadyArgAndTag(subAllowed: Boolean? = null) =
    CustomDialogArgDataAndTag(
        data = CustomDialogArgData(
            titleIcon = R.drawable.ic_alert,
            title = StringIdHelper.Id(R.string.flash_order_warning_title),
            body = StringIdHelper.Id(
                if (subAllowed == false) {
                    R.string.flash_order_prep_not_ready_no_sub_body
                } else {
                    R.string.flash_order_prep_not_ready_body
                }
            ),
            positiveButtonText = StringIdHelper.Id(R.string.prep_not_ready),
            negativeButtonText = StringIdHelper.Id(R.string.cancel),
            cancelOnTouchOutside = false
        ),
        tag = FLASH_WARNING_PREP_NOT_READY_DIALOG_TAG
    )

fun getContainerReassignmentArgDataAndTag() = CustomDialogArgDataAndTag(
    data = CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.container_reassignment_title),
        body = StringIdHelper.Id(R.string.container_reassignment_body),
        positiveButtonText = StringIdHelper.Id(R.string.yes),
        negativeButtonText = StringIdHelper.Id(R.string.no),
        cancelOnTouchOutside = false
    ),
    tag = CONTAINER_REASSIGNMENT_DIALOG_TAG
)

fun getContainerCannotReassignArgDataAndTag() = CustomDialogArgDataAndTag(
    data = CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.container_cannot_reassign_title),
        body = StringIdHelper.Id(R.string.container_cannot_reassign_body_primary),
        secondaryBody = StringIdHelper.Id(R.string.container_cannot_reassign_body_secondary),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
        cancelOnTouchOutside = true
    ),
    tag = CONTAINER_CANNOT_REASSIGN_DIALOG_TAG
)

fun getEndPickWithExceptionsArgDataAndTag(numItemsNotPicked: Int) =
    CustomDialogArgDataAndTag(
        data = CustomDialogArgData(
            titleIcon = R.drawable.ic_alert,
            title = StringIdHelper.Id(R.string.end_pick_exceptions_title),
            body = StringIdHelper.Plural(R.plurals.end_pick_exceptions_body_plural, numItemsNotPicked),
            secondaryBody = StringIdHelper.Id(R.string.end_pick_body),
            positiveButtonText = StringIdHelper.Id(R.string.end_pick_button),
            negativeButtonText = StringIdHelper.Id(R.string.cancel),
            cancelOnTouchOutside = true
        ),
        tag = END_PICK_WITH_EXCEPTIONS_DIALOG_TAG
    )

fun getEndPickConfirmationArgDataAndTag() = CustomDialogArgDataAndTag(
    data = CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.end_pick_confirmation_title),
        body = StringIdHelper.Id(R.string.end_pick_confirmation_body),
        positiveButtonText = StringIdHelper.Id(R.string.end_pick_confirmation_postive_button),
        negativeButtonText = StringIdHelper.Id(R.string.cancel),
        cancelOnTouchOutside = true
    ),
    tag = END_PICK_DIALOG_TAG
)

fun getLabelSentToPrinterArgDataAndTag() = CustomDialogArgDataAndTag(
    data = SENT_TO_PRINTER_TOTES_ARG_DATA,
    tag = LABEL_SENT_TO_PRINTER_DIALOG_TAG
)

fun getEarlyExitArgDataAndTag() = CustomDialogArgDataAndTag(
    data = CustomDialogArgData(
        titleIcon = R.drawable.ic_confirm,
        title = StringIdHelper.Id(R.string.early_exit_title),
        body = StringIdHelper.Id(R.string.early_exit_body),
        positiveButtonText = StringIdHelper.Id(R.string.early_exit_button),
        cancelOnTouchOutside = false
    ),
    tag = COMPLETE_PICKING_EARLY_EXIT_DIALOG_TAG
)

fun getIssueScanningArgDataAndTag() = CustomDialogArgDataAndTag(
    data = CustomDialogArgData(
        titleIcon = null,
        title = StringIdHelper.Id(R.string.have_trouble_scanning),
        body = StringIdHelper.Id(R.string.would_you_like_to_report_item_scanning_issue),
        positiveButtonText = StringIdHelper.Id(R.string.end_pick_confirmation_postive_button),
        negativeButtonText = StringIdHelper.Id(R.string.cancel),
        cancelOnTouchOutside = true
    ),
    tag = IS_TROUBLE_SCANNING_DIALOG
)

fun getSelectSpecificItemArgDataAndTag() = CustomDialogArgDataAndTag(
    data = CustomDialogArgData(
        titleIcon = null,
        title = StringIdHelper.Id(R.string.select_specific_item_to_report_issue_scan),
        body = StringIdHelper.Id(R.string.select_specific_item_to_report_issue_scan_body),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
        negativeButtonText = null,
        cancelOnTouchOutside = false
    ),
    tag = SELECT_SPECIFIC_ITEM_DIALOG
)

fun getEarlyExitWineArgDataAndTag() = CustomDialogArgDataAndTag(
    data = CustomDialogArgData(
        titleIcon = R.drawable.ic_confirm,
        title = StringIdHelper.Id(R.string.early_exit_title),
        body = StringIdHelper.Id(R.string.early_exit_body),
        positiveButtonText = StringIdHelper.Id(R.string.early_exit_button),
        cancelOnTouchOutside = false
    ),
    tag = COMPLETE_WINE_PICKING_EARLY_EXIT_DIALOG_TAG
)

fun getCompletePickArgDataAndTag() = CustomDialogArgDataAndTag(
    data = CustomDialogArgData(
        titleIcon = R.drawable.ic_confirm,
        title = StringIdHelper.Id(R.string.end_pick_title),
        body = StringIdHelper.Id(R.string.end_pick_body),
        positiveButtonText = StringIdHelper.Id(R.string.end_pick_button),
        negativeButtonText = StringIdHelper.Id(R.string.cancel),
        cancelOnTouchOutside = true
    ),
    tag = COMPLETED_PICK_DIALOG_TAG
)

fun getSuggestSubstitutionArgDataAndTag() = CustomDialogArgDataAndTag(
    data = CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.short_item),
        body = StringIdHelper.Id(R.string.short_sub_body),
        positiveButtonText = StringIdHelper.Id(R.string.short_cta),
        negativeButtonText = StringIdHelper.Id(R.string.cancel),
        cancelOnTouchOutside = false
    ),
    tag = SUGGEST_SUBSTITUTION_DIALOG_TAG
)

fun getCompletePickErrorArgDataAndTag() = CustomDialogArgDataAndTag(
    data = CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.complete_pick_error_title),
        body = StringIdHelper.Id(R.string.complete_pick_error_body),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
        cancelOnTouchOutside = true
    ),
    tag = COMPLETE_PICK_ERROR_DIALOG_TAG
)

// fun getQuantityPickerArgDataAndTag(
//     app: Application,
//     scannedItem: ScannedPickItem,
//     barcodeType: BarcodeType,
//     shouldHideInfo: Boolean
// ): CustomDialogArgDataAndTag {
//     val item = scannedItem.item
//     return CustomDialogArgDataAndTag(
//         data = CustomDialogArgData(
//             dialogType = DialogType.QuantityPicker,
//             title = StringIdHelper.Raw(""),
//             customData = QuantityParams(
//                 barcodeFormatted = barcodeType.getFormattedValue(context = app, hideUnits = true),
//                 isPriced = barcodeType is BarcodeType.Item.Priced && scannedItem.item.isPrepped(),
//                 isWeighted = barcodeType is BarcodeType.Item.Weighted,
//                 isEaches = barcodeType is BarcodeType.Item.Each,
//                 isTotaled = item.isPriceEachTotaled(),
//                 itemId = item.itemId,
//                 description = item.itemDescription,
//                 image = item.imageUrl,
//                 weightEntry = null,
//                 requested = item.qty?.toInt() ?: 0,
//                 entered = item.processedQty?.toInt() ?: 0,
//                 isSubstitution = false,
//                 shouldHideInfo = shouldHideInfo,
//                 storageType = item.storageType,
//                 isRegulated = scannedItem.itemDetails?.isRegulated,
//             ),
//             positiveButtonText = StringIdHelper.Id(R.string.continue_cta)
//         ),
//         tag = QUANTITY_PICKER_PICK_DIALOG_TAG
//     )
// }

fun getAlternativeLocationsArgDataAndTag(
    context: Context,
    lastItemShorted: ItemActivityDto?,
    lastSubstitutedItem: ItemActivityDto?,
    altLocationsList: List<ItemLocationDto>?,
    path: AlternateLocationPath,
): CustomDialogArgDataAndTag {
    val alternativeLocationsList = mutableListOf<String>()
    altLocationsList?.forEach { location ->
        alternativeLocationsList.add(location.itemAddressDto?.asItemLocation(context) ?: "")
    }
    val item = when (path) {
        AlternateLocationPath.Short -> lastItemShorted
        AlternateLocationPath.Substitute -> lastSubstitutedItem
    }.exhaustive

    val positiveButtonText = when (path) {
        AlternateLocationPath.Short -> StringIdHelper.Id(R.string.short_out_of_stock)
        AlternateLocationPath.Substitute -> StringIdHelper.Id(R.string.cant_find_item)
    }.exhaustive

    return CustomDialogArgDataAndTag(
        data = CustomDialogArgData(
            dialogType = DialogType.AlternativeLocations,
            title = StringIdHelper.Id(R.string.alternative_locations_title),
            body = StringIdHelper.Id(R.string.alternate_location_subheading),
            customData = AlternativeLocationItem(
                itemName = item?.getItemDescriptionEllipsis(MAX_ITEM_DESCRIPTION_CHARACTERS_FOR_SMALL_VIEWS),
                upc = item?.primaryUpc,
                imageUrl = item?.imageUrl,
                alternativeLocations = alternativeLocationsList,
                path = path
            ),
            positiveButtonText = positiveButtonText,
            negativeButtonText = StringIdHelper.Id(R.string.item_found_button_text),
            cancelOnTouchOutside = false
        ),
        tag = ALTERNATIVE_LOCATION_DIALOG_TAG
    )
}

fun getOrderedByWeightArgAndData(
    item: ItemActivityDto,
    scannedItemWeight: Double,
    isScannedItemTooLight: Boolean,
    isManualEntry: Boolean,
): CustomDialogArgDataAndTag {
    return CustomDialogArgDataAndTag(
        data = CustomDialogArgData(
            dialogType = DialogType.OrderedByWeight,
            titleIcon = R.drawable.ic_alert,
            title = StringIdHelper.Id(if (isScannedItemTooLight) R.string.ordered_by_weight_too_light else R.string.ordered_by_weight_too_heavy),
            customData = OrderedByWeightDialogData(
                itemDescription = item.itemDescription.orEmpty(),
                plu = item.pluList?.getOrNull(0).orEmpty(),
                imageUrl = item.imageUrl.orEmpty(),
                requestedWeight = item.orderedWeight,
                scannedWeight = scannedItemWeight,
                uom = item.orderedWeightUOM ?: "lb",
            ),
            positiveButtonText = StringIdHelper.Id(R.string.ordered_by_weight_cta_complete),
            negativeButtonText = StringIdHelper.Id(R.string.ordered_by_weight_cta_repick),
            cancelOnTouchOutside = false
        ),
        tag = if (isManualEntry) ORDERED_BY_WEIGHT_MANUAL_ENTRY_DIALOG_TAG else ORDERED_BY_WEIGHT_DIALOG_TAG
    )
}

fun getItemDetailsArgDataAndTagForBottomSheet(
    iaId: Long,
    actId: Long,
    activityNo: String,
    altItemLocations: List<ItemLocationDto>?,
    item: ItemActivityDto?,
    pickListType: PickListType = PickListType.Todo,
    isFromSubstitutionFlow: Boolean = false,
    isMoveToLocation: Boolean = false
): BottomSheetArgDataAndTag {

    val title = if (pickListType == PickListType.Short || pickListType == PickListType.Picked) {
        StringIdHelper.Id(R.string.title_item_details)
    } else {
        if (item?.sellByWeightInd == SellByType.Weight) {
            StringIdHelper.Id(R.string.scan_weighed_item_title)
        } else if (item?.sellByWeightInd == SellByType.Each) {
            StringIdHelper.Id(R.string.title_item_details)
        } else {
            StringIdHelper.Id(R.string.scan_item)
        }
    }
    // Open full screen bottom sheet
    val isFullScreenOpened = isMoveToLocation || (pickListType == PickListType.Todo && (item?.sellByWeightInd == SellByType.PriceWeighted || item?.sellByWeightInd == SellByType.Each))

    return BottomSheetArgDataAndTag(
        data = CustomBottomSheetArgData(
            dialogType = BottomSheetType.ItemDetail,
            title = title,
            customDataParcel = ItemDetailsParams(
                iaId = iaId,
                actId = actId,
                activityNo = activityNo,
                altItemLocations = altItemLocations,
                pickListType = pickListType,
                isFromSubstitutionFlow = isFromSubstitutionFlow,
                isMoveToLocation = isMoveToLocation,
            ),
            isFullScreen = isFullScreenOpened
        ),
        tag = ITEM_DETAIL_BOTTOMSHEET_TAG
    )
}

fun getUnPickArgDataAndTagForBottomSheet(
    iaId: Long,
    actId: Long,
    activityNo: String,
    item: ItemActivityDto?,
    pickListType: PickListType = PickListType.Todo,
): BottomSheetArgDataAndTag {

    val title = StringIdHelper.Id(R.string.select_unpick_items)

    return BottomSheetArgDataAndTag(
        data = CustomBottomSheetArgData(
            dialogType = BottomSheetType.UnPick,
            title = title,
            customDataParcel = UnPickParams(
                iaId = iaId,
                actId = actId,
                activityNo = activityNo,
                pickListType = pickListType
            ),
            positiveButtonText = StringIdHelper.Id(R.string.un_pick),
            peekHeight = R.dimen.expanded_bottomsheet_peek_height
        ),
        tag = UN_PICK_BOTTOMSHEET_TAG
    )
}

fun getQuantityPickerArgDataAndTagForBottomSheet(
    app: Application,
    scannedItem: ScannedPickItem,
    barcodeType: BarcodeType,
    isIssueScanning: Boolean,
): BottomSheetArgDataAndTag {
    val item = scannedItem.item
    return BottomSheetArgDataAndTag(
        data = CustomBottomSheetArgData(
            dialogType = BottomSheetType.QuantityPicker,
            title = StringIdHelper.Raw(""),
            customDataParcel = QuantityParams(
                barcodeFormatted = barcodeType.getFormattedValue(context = app, hideUnits = true),
                isPriced = barcodeType is BarcodeType.Item.Priced && scannedItem.item.isPrepped(),
                isWeighted = barcodeType is BarcodeType.Item.Weighted,
                isEaches = barcodeType is BarcodeType.Item.Each,
                isTotaled = item.isPriceEachTotaled(),
                itemId = if (isIssueScanning) scannedItem.itemDetails?.itemId else item.itemId,
                description = if (isIssueScanning) scannedItem.itemDetails?.itemDesc else item.itemDescription,
                image = if (isIssueScanning) scannedItem.itemDetails?.imageUrl else item.imageUrl,
                weightEntry = null,
                requested = item.qty?.toInt() ?: 0,
                entered = item.processedQty?.toInt() ?: 0,
                isSubstitution = false,
                isIssueScanning = isIssueScanning,
                storageType = if (isIssueScanning) scannedItem.itemDetails?.storageType else item.storageType,
                isRegulated = scannedItem.itemDetails?.isRegulated,
                shouldShowOriginalItemInfo = isIssueScanning,
                originalItemParams = if (isIssueScanning) OriginalItemParams(
                    item.itemDescription, item.itemId, item.imageUrl,
                    (item.qty?.toInt()?.minus(item.processedQty?.toInt() ?: 0)) ?: 0
                )
                else null,
                isCustomerBagPreference = scannedItem.item.isCustomerBagPreference
            ),
            positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
            peekHeight = if (isIssueScanning) R.dimen.expanded_bottomsheet_peek_height else R.dimen.default_bottomsheet_peek_height
        ),
        tag = QUANTITY_PICKER_PICK_DIALOG_TAG
    )
}

fun getQuantityPickerArgDataAndTagForBulkBottomSheet(
    app: Application,
    scannedItem: ScannedPickItem,
    barcodeType: BarcodeType,
    selectedVariant: BulkItem,
    isIssueScanning: Boolean,
): BottomSheetArgDataAndTag {
    val item = scannedItem.item
    return BottomSheetArgDataAndTag(
        data = CustomBottomSheetArgData(
            dialogType = BottomSheetType.QuantityPicker,
            title = StringIdHelper.Raw(""),
            customDataParcel = QuantityParams(
                barcodeFormatted = barcodeType.getFormattedValue(context = app, hideUnits = true),
                isPriced = barcodeType is BarcodeType.Item.Priced && scannedItem.item.isPrepped(),
                isWeighted = barcodeType is BarcodeType.Item.Weighted,
                isEaches = barcodeType is BarcodeType.Item.Each,
                isTotaled = item.isPriceEachTotaled(),
                itemId = selectedVariant.itemId,
                description = selectedVariant.itemDes,
                image = selectedVariant.imageUrl,
                weightEntry = null,
                requested = item.qty?.toInt() ?: 0,
                entered = item.processedQty?.toInt() ?: 0,
                isSubstitution = false,
                isIssueScanning = isIssueScanning,
                storageType = if (isIssueScanning) scannedItem.itemDetails?.storageType else item.storageType,
                isRegulated = scannedItem.itemDetails?.isRegulated,
                shouldShowOriginalItemInfo = isIssueScanning,
                originalItemParams = if (isIssueScanning) OriginalItemParams(
                    item.itemDescription, item.itemId, item.imageUrl,
                    (item.qty?.toInt()?.minus(item.processedQty?.toInt() ?: 0)) ?: 0
                )
                else null
            ),
            positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
            peekHeight = if (isIssueScanning) R.dimen.expanded_bottomsheet_peek_height else R.dimen.default_bottomsheet_peek_height
        ),
        tag = QUANTITY_PICKER_PICK_DIALOG_TAG
    )
}

fun getToteScanArgDataAndTagForBottomSheet(suggestedToteId: String?, isCustomerBagPreference: Boolean?): BottomSheetArgDataAndTag {
    var titleIcon: Int? = null
    var titleDescription: StringIdHelper? = null
    val title = when {
        isCustomerBagPreference == false -> {
            titleIcon = R.drawable.ic_no_bag_tote
            titleDescription = StringIdHelper.Id(R.string.scan_tote_no_bags_description)
            if (suggestedToteId.isNotNullOrBlank()) {
                StringIdHelper.Format(R.string.scan_specific_tote_format, suggestedToteId?.takeLast(TOTE_UI_COUNT).orEmpty())
            } else {
                StringIdHelper.Id(R.string.scan_to_new_tote)
            }
        }

        suggestedToteId.isNotNullOrBlank() -> {
            titleIcon = R.drawable.ic_tote_suggestion
            titleDescription = StringIdHelper.Id(R.string.scan_a_new_tote_description)
            StringIdHelper.Format(R.string.scan_specific_tote_format, suggestedToteId?.takeLast(TOTE_UI_COUNT).orEmpty())
        }
        else -> {
            titleIcon = R.drawable.ic_tote_new
            titleDescription = StringIdHelper.Id(R.string.scan_a_new_tote_description)
            StringIdHelper.Id(R.string.scan_to_new_tote)
        }
    }
    return BottomSheetArgDataAndTag(
        data = CustomBottomSheetArgData(
            dialogType = BottomSheetType.ToteScan,
            titleIcon = titleIcon,
            title = title,
            textToBeHighlightedInTitle = StringIdHelper.Raw(suggestedToteId.orEmpty()),
            body = titleDescription,
            positiveButtonText = StringIdHelper.Id(R.string.ok),
            shouldBoldTitle = isCustomerBagPreference != false,
        ),
        tag = TOTE_SCAN_BOTTOMSHEET_TAG
    )
}

fun getDismissItemDetailBottomSheetArgDataAndTag(): CustomBottomSheetArgData {
    return CustomBottomSheetArgData(
        title = StringIdHelper.Id(R.string.title_item_details),
        exit = true
    )
}

fun getDismissUnPickBottomSheetArgDataAndTag(itemActionList: List<ItemActionDbViewModel>): CustomBottomSheetArgData {
    val checkedItems = itemActionList.filter { it.isChecked.value == true }.map { it.itemAction.backingType }
    return CustomBottomSheetArgData(
        title = StringIdHelper.Id(R.string.un_pick),
        exit = true,
        customDataParcel = UnPickResultParams(checkedItems = checkedItems),
        dialogType = BottomSheetType.UnPick
    )
}

fun getConfirmAmountArgDataAndTagForBottomSheet(
    item: ItemActivityDto,
): BottomSheetArgDataAndTag {
    val totalRequestedNetWeight: Double
    val currentNetWeight: Double
    if (item.sellByWeightInd == SellByType.PriceEachTotal) {
        totalRequestedNetWeight = item.qty ?: 0.0
        currentNetWeight = item.processedQty ?: 0.0
    } else {
        totalRequestedNetWeight = item.requestedNetWeight
        currentNetWeight = item.netWeight
    }
    return BottomSheetArgDataAndTag(
        data = CustomBottomSheetArgData(
            dialogType = BottomSheetType.ConfirmAmount,
            title = StringIdHelper.Raw(""),
            peekHeight = R.dimen.expanded_bottomsheet_peek_height,
            customDataParcel = ConfirmAmountUIData(
                pageTitle = StringIdHelper.Id(R.string.confirm_amount_toolbar_title),
                itemDescription = item.itemDescription.orEmpty(),
                requestedAmount = RequestedAmount(
                    baseWeight = item.itemWeight?.toDoubleOrNull().orZero(),
                    totalRequestedNetWeight = totalRequestedNetWeight,
                    currentNetWeight = currentNetWeight,
                    weightUOM = item.itemWeightUom.orEmpty()
                ),
                itemType = item.sellByWeightInd ?: SellByType.PriceScaled
            ),
        ),
        tag = CONFIRM_AMOUNT_BOTTOMSHEET_TAG
    )
}

fun getShortItemReasonArgDataForActionSheet(substituteOptions: MutableList<ActionSheetOptions>): CustomBottomSheetArgData {
    return CustomBottomSheetArgData(
        dialogType = BottomSheetType.ActionSheet,
        draggable = false,
        title = StringIdHelper.Raw(""),
        peekHeight = R.dimen.actionsheet_peek_height,
        customDataParcel = ActionSheetDetails(substituteOptions)
    )
}

fun getPickLaterDialogArgData(isPrePickIssueScanning: Boolean = false): CustomDialogArgDataAndTag {
    return CustomDialogArgDataAndTag(
        data = CustomDialogArgData(
            dialogType = DialogType.ModalFiveConfirmation,
            title = StringIdHelper.Id(R.string.save_this_item_for_later),
            titleIcon = R.drawable.ic_pick_later,
            body = StringIdHelper.Id(R.string.save_this_item_for_later_description),
            positiveButtonText = StringIdHelper.Id(R.string.save_cta),
            negativeButtonText = StringIdHelper.Id(R.string.cancel),
            cancelOnTouchOutside = false
        ),
        tag = if (isPrePickIssueScanning) PICK_LATER_ISSUE_SCANNING_DIALOG else PICK_LATER_DIALOG
    )
}

fun getShortItemConfirmationDialogArgData(title: Int, bodyText: Int? = null): CustomDialogArgData {
    return CustomDialogArgData(
        title = StringIdHelper.Id(title),
        body = bodyText?.let { StringIdHelper.Id(it) },
        positiveButtonText = StringIdHelper.Id(R.string.confirm),
        negativeButtonText = StringIdHelper.Id(R.string.cancel),
        cancelOnTouchOutside = false
    )
}

// pass itemId to get it back in the callback for positive click
fun getRemoveSubstitutionDialogArgDataAndTag(swapSubListLocal: List<SwapItem>, itemIdAndMessageSid: Pair<String, String>? = null): CustomDialogArgDataAndTag {
    val dialogBody = StringIdHelper.Plural(R.plurals.remove_substitution_body, swapSubListLocal.size)
    return CustomDialogArgDataAndTag(
        data = CustomDialogArgData(
            dialogType = DialogType.RemoveSubstitution,
            title = StringIdHelper.Id(R.string.remove_substituted_item),
            body = dialogBody,
            customData = SwapItemParams(itemIdAndMessageSid, swapSubListLocal),
            positiveButtonText = StringIdHelper.Id(R.string.confirm),
            negativeButtonText = StringIdHelper.Id(R.string.cancel_cta),
            cancelOnTouchOutside = false
        ),
        tag = REMOVE_SUBSTITUTION_DIALOG_TAG
    )
}

fun getTotesNeededArgDataAndTagForBottomSheet(toteEstimate: ToteEstimate?): BottomSheetArgDataAndTag {
    val title = if (toteEstimate?.ambient.notZeroOrNull() && toteEstimate?.chilled.notZeroOrNull()) {
        StringIdHelper.Format(R.string.totes_needed_title_both_am_and_ch, toteEstimate?.ambient.toString(), toteEstimate?.chilled.toString())
    } else if (toteEstimate?.chilled.notZeroOrNull()) {
        StringIdHelper.Plural(R.plurals.totes_needed_title_ch, toteEstimate?.chilled ?: 1)
    } else {
        StringIdHelper.Plural(R.plurals.totes_needed_title_am, toteEstimate?.ambient ?: 1)
    }
    return BottomSheetArgDataAndTag(
        data = CustomBottomSheetArgData(
            dialogType = BottomSheetType.ToteEstimate,
            positiveButtonText = StringIdHelper.Id(R.string.got_it),
            title = title,
            body = StringIdHelper.Id(R.string.totes_needed_body),
            largeImage = R.drawable.ic_attach_tote_labels,
            customDataParcel = toteEstimate
        ),
        tag = TOTE_ESTIMATE_DIALOG
    )
}

fun getEndPickReasonConfirmationDialogArgData(): CustomDialogArgDataAndTag {
    return CustomDialogArgDataAndTag(
        data = CustomDialogArgData(
            dialogType = DialogType.CustomRadioButtons,
            title = StringIdHelper.Id(R.string.end_pick_reason_dialog_title),
            customData = listOf(
                StringIdHelper.Id(R.string.picking_another_order),
                StringIdHelper.Id(R.string.handoff_customer),
                StringIdHelper.Id(R.string.short_tote_full),
                StringIdHelper.Id(R.string.other),
            ) as Serializable,
            positiveButtonText = StringIdHelper.Id(R.string.confirm),
            cancelOnTouchOutside = false
        ),
        tag = END_PICK_REASON_DIALOG_TAG
    )
}

const val COMPLETE_PICKING_EARLY_EXIT_DIALOG_TAG = "completePickingEarlyExitDialogTag"
const val COMPLETE_WINE_PICKING_EARLY_EXIT_DIALOG_TAG = "completeWinePickingEarlyExitDialogTag"
const val END_PICK_WITH_EXCEPTIONS_DIALOG_TAG = "endPickWithExceptionsDialogTag"
const val END_PICK_DIALOG_TAG = "endPickDialogTag"
const val LABEL_SENT_TO_PRINTER_DIALOG_TAG = "labelSentToPrinterDialogTag"
const val COMPLETED_PICK_DIALOG_TAG = "completePickDialogTag"
const val SUGGEST_SUBSTITUTION_DIALOG_TAG = "suggestSubstitutionDialogTag"
const val COMPLETE_PICK_ERROR_DIALOG_TAG = "completePickErrorDialogTag"
const val OVERRIDE_SUBSTITUTION_DIALOG_TAG = "overrideSubstitutionDialogTag"
const val CONTAINER_REASSIGNMENT_DIALOG_TAG = "containerReassignmentDialogTag"
const val CONTAINER_CANNOT_REASSIGN_DIALOG_TAG = "containerCannotReassignDialog"
const val CONFIRM_ITEM_SAME_DIALOG_TAG = "confirmSameItemDialogTag"
const val QUANTITY_PICKER_PICK_DIALOG_TAG = "quanitityPickerPickDialogTag"
const val ALTERNATIVE_LOCATION_DIALOG_TAG = "alternativeLocationDialogTag"
const val FLASH_WARNING_DIALOG_TAG = "flashWarningDialogTag"
const val FLASH_WARNING_OOS_DIALOG_TAG = "flashWarningOOSDialogTag"
const val FLASH_WARNING_PREP_NOT_READY_DIALOG_TAG = "flashWarningPrepNotReadyDialogTag"
const val ORDERED_BY_WEIGHT_DIALOG_TAG = "orderedByWeightDialogTag"
const val ORDERED_BY_WEIGHT_MANUAL_ENTRY_DIALOG_TAG = "orderedByWeightManualEntryDialogTag"
const val PARTNER_PICK_OUT_OF_STOCK_DIALOG_TAG = "partnerPickOutOfStockDialogTag"
const val PARTNER_PICK_PREP_NOT_READY_DIALOG_TAG = "partnerPickPrepNotReadyDialogTag"
const val ITEM_DETAIL_BOTTOMSHEET_TAG = "ItemDetailBottomSheetTag"
const val TOTE_SCAN_BOTTOMSHEET_TAG = "ToteScanBottomSheetTag"
const val MANUAL_ENTRY_STAGING_BOTTOMSHEET_TAG = "ManualEntryStagingBottomSheetTag"
const val UN_PICK_BOTTOMSHEET_TAG = "UnPickBottomSheetTag"
const val CONFIRM_AMOUNT_BOTTOMSHEET_TAG = "ConfirmAmountBottomSheetTag"
const val IS_TROUBLE_SCANNING_DIALOG = "IsTroubleScanningDialog"
const val SELECT_SPECIFIC_ITEM_DIALOG = "SelectSpecificItemDialog"
const val PICK_LATER_DIALOG = "PickLaterDialog"
const val PICK_LATER_ISSUE_SCANNING_DIALOG = "PickLaterIssueScanningDialog"
const val TOTE_ESTIMATE_DIALOG = "ToteEstimateDialog"
const val REMOVE_SUBSTITUTION_DIALOG_TAG = "removeSubstitutionDialogTag"
const val MISSING_ITEM_LOCATION_DIALOG_TAG = "missingItemLocationDialogTag"
const val WHERE_TO_FIND_LOCATION_DIALOG_TAG = "wherToFindLocationDialogTag"
const val END_PICK_REASON_DIALOG_TAG = "endPickReasonDialogTag"
