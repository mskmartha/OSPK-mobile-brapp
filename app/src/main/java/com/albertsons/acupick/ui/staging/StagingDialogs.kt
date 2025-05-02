package com.albertsons.acupick.ui.staging

import com.albertsons.acupick.R
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetArgDataAndTag
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetType
import com.albertsons.acupick.ui.bottomsheetdialog.CustomBottomSheetArgData
import com.albertsons.acupick.ui.util.StringIdHelper

fun getCollectToteLabelsArgDataAndTagForBottomSheet(
    toteCount: Int,
): BottomSheetArgDataAndTag {
    return BottomSheetArgDataAndTag(
        data = CustomBottomSheetArgData(
            dialogType = BottomSheetType.CollectToteLaels,
            positiveButtonText = StringIdHelper.Id(R.string.start_bag_count),
            title = StringIdHelper.Plural(R.plurals.collect_n_tote_labels, toteCount),
            body = StringIdHelper.Id(R.string.find_the_printer_to_collect_your_tote_labels),
            largeImage = R.drawable.ic_attach_tote_labels
        ),
        tag = COLLECT_TOTE_LABEL_DIALOG_TAG
    )
}

fun getAttachBagLabelsArgDataAndTagForBottomSheet(): BottomSheetArgDataAndTag {
    return BottomSheetArgDataAndTag(
        data = CustomBottomSheetArgData(
            dialogType = BottomSheetType.CollectToteLaels,
            positiveButtonText = StringIdHelper.Id(R.string.start_staging),
            title = StringIdHelper.Id(R.string.staging_collect_and_attach_bag_labels),
            body = StringIdHelper.Id(R.string.find_the_printer_to_collect_your_bags_and_loose_item_labels),
            largeImage = R.drawable.ic_attach_bag_label
        ),
        tag = ATTACH_BAG_LABEL_BOTTOMSHEET_TAG
    )
}

fun getAttachToteAndLooseLabelsArgDataAndTagForBottomSheet(): BottomSheetArgDataAndTag {
    return BottomSheetArgDataAndTag(
        data = CustomBottomSheetArgData(
            dialogType = BottomSheetType.CollectToteLaels,
            positiveButtonText = StringIdHelper.Id(R.string.start_staging),
            title = StringIdHelper.Id(R.string.staging_collect_and_attach_tote_and_loose_labels),
            body = StringIdHelper.Id(R.string.find_the_printer_to_collect_your_tote_and_loose_item_labels),
            largeImage = R.drawable.ic_attach_bag_label
        ),
        tag = ATTACH_BAG_LABEL_BOTTOMSHEET_TAG
    )
}

fun getReprintToteLabelsArgDataAndTagForBottomSheet(): BottomSheetArgDataAndTag {
    return BottomSheetArgDataAndTag(
        data = CustomBottomSheetArgData(
            dialogType = BottomSheetType.CollectToteLaels,
            positiveButtonText = StringIdHelper.Id(R.string.next),
            title = StringIdHelper.Id(R.string.collect_tote_labels),
            body = StringIdHelper.Id(R.string.find_the_printer_to_collect_your_tote_labels),
            largeImage = R.drawable.ic_collect_tote_labels
        ),
        tag = STAGING_REPRINT_TOTE_BAGS_BOTTOMSHEET_TAG
    )
}

fun getReprintToteLabelsArgDataAndTagForBagPreferredBottomSheet(): BottomSheetArgDataAndTag {
    return BottomSheetArgDataAndTag(
        data = CustomBottomSheetArgData(
            dialogType = BottomSheetType.CollectToteLaels,
            positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
            title = StringIdHelper.Id(R.string.collect_and_attach_tote_labels),
            body = StringIdHelper.Id(R.string.find_the_printer_to_collect_your_tote_labels),
            largeImage = R.drawable.ic_attach_tote_labels
        ),
        tag = STAGING_REPRINT_TOTE_BAGS_PREFERRED_BOTTOMSHEET_TAG
    )
}

const val COLLECT_TOTE_LABEL_DIALOG_TAG = "CollectToteLabelDialogTag"
const val ATTACH_BAG_LABEL_BOTTOMSHEET_TAG = "attachBagLabelBottomSheetTag"
const val STAGING_REPRINT_TOTE_BAGS_BOTTOMSHEET_TAG = "stagingReprintToteBagsBottomSheetTag"
const val STAGING_REPRINT_TOTE_BAGS_PREFERRED_BOTTOMSHEET_TAG = "stagingReprintTotePreferredBagsBottomSheetTag"
