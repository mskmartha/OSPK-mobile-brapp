package com.albertsons.acupick.ui.dialog

import android.content.Context
import android.os.Parcelable
import android.view.View
import androidx.annotation.DrawableRes
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.DomainModel
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.toFormatHelper
import com.albertsons.acupick.ui.util.toIdHelper
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import java.time.ZonedDateTime

/** Common continue order dialog data to support DRY for multiple uses by different screens */
val CONTINUE_ORDER_DIALOG_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.continue_order_title),
        body = StringIdHelper.Id(R.string.continue_order_body),
        positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
        negativeButtonText = StringIdHelper.Id(R.string.cancel),
        cancelOnTouchOutside = false
    )
}

val NO_STORES_ASSIGNED_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        title = StringIdHelper.Id(R.string.no_stores_assigned_title),
        body = StringIdHelper.Id(R.string.no_stores_assigned_body),
        negativeButtonText = StringIdHelper.Id(R.string.cancel),
        cancelOnTouchOutside = true
    )
}

val CANNOT_CHANGE_STORE_WITH_ACTIVE_PICK_LIST_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        title = StringIdHelper.Id(R.string.continue_in_progress_pick_title),
        body = StringIdHelper.Id(R.string.continue_in_progress_pick_message),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
        cancelOnTouchOutside = false
    )
}

val ALREADY_ASSIGNED_PICKLIST_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.staging_server_error),
        body = StringIdHelper.Id(R.string.open_pick_list_assign_error_body),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
        cancelOnTouchOutside = false
    )
}

val REASSIGNED_PICKLIST_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.staging_server_error),
        body = StringIdHelper.Id(R.string.reassigned_error_body),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
        cancelOnTouchOutside = false
    )
}

val HAND_OFF_ALREADY_ASSIGNED_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.hand_off_already_assigned_title),
        body = StringIdHelper.Id(R.string.hand_off_already_assigned_body),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
    )
}

val HAND_OFF_BATCH_ALREADY_ASSIGNED_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.hand_off_already_assigned_title),
        body = StringIdHelper.Id(R.string.hand_off_batch_already_assigned_body),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
    )
}

fun getHandOffAlreadyAssignedWithOrderNumberDialog(orderNumber: String): CustomDialogArgData =
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.hand_off_already_assigned_title),
        body = StringIdHelper.Format(R.string.hand_off_already_assigned_body_with_order_number, orderNumber),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
    )

fun getHandOffBatchAlreadyAssignedWithOrderNumbersDialog(orderNumber: String): CustomDialogArgData =
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.hand_off_already_assigned_title),
        body = StringIdHelper.Format(R.string.hand_off_batch_already_assigned_body_with_oder_number, orderNumber),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
    )

val OF_AGE_ASSOCIATE_VERIFICATION_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        dialogStyle = DialogStyle.OfAgeVerification,
        titleIcon = R.drawable.ic_alert,
        shouldBoldTitle = true,
        title = StringIdHelper.Id(R.string.of_age_verification_title),
        body = StringIdHelper.Id(R.string.of_age_verification_body),
        secondaryBody = StringIdHelper.Id(R.string.of_age_verification_secondary_body),
        negativeButtonText = StringIdHelper.Id(R.string.cancel),
        positiveButtonText = StringIdHelper.Id(R.string.confirm),
        cancelOnTouchOutside = false
    )
}

val DRIVER_ID_VERIFICATION_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        dialogType = DialogType.TitleImageInfo,
        title = StringIdHelper.Id(R.string.age_verification),
        largeImage = R.drawable.ic_id_verification,
        body = StringIdHelper.Id(R.string.id_verification_body),
        positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
        negativeButtonText = StringIdHelper.Id(R.string.id_unavailable),
    )
}

val INVALID_DOB_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        dialogStyle = DialogStyle.OfAgeVerification,
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.invalid_dob_modal_title),
        body = StringIdHelper.Id(R.string.invalid_dob_modal_primary_copy),
        positiveButtonText = StringIdHelper.Id(R.string.invalid_dob_postive_button),
        negativeButtonText = StringIdHelper.Id(R.string.invalid_dob_negative_button),
    )
}

val DRIVER_ID_VERIFICATION_DATA_DUG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        dialogType = DialogType.TitleImageInfo,
        title = StringIdHelper.Id(R.string.age_verification),
        largeImage = R.drawable.ic_id_verification,
        body = StringIdHelper.Id(R.string.id_verification_body_dug),
        positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
        negativeButtonText = StringIdHelper.Id(R.string.id_unavailable),
    )
}

val CANCELED_SINGLE_PICKLIST_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.canceled_title),
        body = StringIdHelper.Id(R.string.picklist_single_canceled_body),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
    )
}

val CANCELED_BATCH_PICKLIST_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.canceled_title),
        body = StringIdHelper.Id(R.string.picklist_batch_canceled_body),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
    )
}

val CANCELED_SINGLE_STAGING_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.canceled_title),
        body = StringIdHelper.Id(R.string.staging_single_canceled_body),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
    )
}

val CANCELED_BATCH_STAGING_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.canceled_title),
        body = StringIdHelper.Id(R.string.staging_batch_canceled_body),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
    )
}

val CANCELED_SINGLE_HANDOFF_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.canceled_title),
        body = StringIdHelper.Id(R.string.handoff_single_canceled_body),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
    )
}

val CANCELED_BATCH_HANDOFF_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.canceled_title),
        body = StringIdHelper.Id(R.string.handoff_batch_canceled_body),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
    )
}

val SENT_TO_PRINTER_TOTES_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.sent_to_printer_totes),
        body = StringIdHelper.Id(R.string.find_your_printer_totes),
        positiveButtonText = StringIdHelper.Id(android.R.string.ok),
        negativeButtonText = null,
        cancelOnTouchOutside = false,
        cancelable = false
    )
}

val SENT_TO_PRINTER_BAGS_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.sent_to_printer),
        body = StringIdHelper.Id(R.string.find_your_printer_bags),
        positiveButtonText = StringIdHelper.Id(android.R.string.ok),
        negativeButtonText = null,
        cancelOnTouchOutside = false,
        cancelable = false
    )
}

val ORDER_ISSUE_SCAN_BAGS_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.scan_bags_dialog_title),
        body = StringIdHelper.Id(R.string.scan_bags_dialog_body),
        positiveButtonText = StringIdHelper.Id(R.string.proceed),
        negativeButtonText = StringIdHelper.Id(R.string.cancel)
    )
}

val ORDER_ISSUE_SCAN_TOTES_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.scan_totes_dialog_title),
        body = StringIdHelper.Id(R.string.scan_totes_dialog_body),
        positiveButtonText = StringIdHelper.Id(R.string.proceed),
        negativeButtonText = StringIdHelper.Id(R.string.cancel)
    )
}

val SHORT_ITEM_REASON_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        dialogType = DialogType.RadioButtons,
        title = StringIdHelper.Id(R.string.short_item_why_label_dialog),
        customData = listOf(
            StringIdHelper.Id(R.string.short_out_of_stock),
            StringIdHelper.Id(R.string.short_tote_full),
            StringIdHelper.Id(R.string.short_prep_not_ready)
        ) as Serializable,
        positiveButtonText = StringIdHelper.Id(R.string.confirm),
        negativeButtonText = StringIdHelper.Id(R.string.close)
    )
}

val SHORT_WINE_ITEM_REASON_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        dialogType = DialogType.RadioButtons,
        title = StringIdHelper.Id(R.string.short_item_why_label_dialog),
        customData = listOf(
            StringIdHelper.Id(R.string.short_out_of_stock),
            StringIdHelper.Id(R.string.short_tote_full),
            StringIdHelper.Id(R.string.short_prep_not_ready)
        ) as Serializable,
        isWineOrder = true,
        positiveButtonText = StringIdHelper.Id(R.string.confirm),
        negativeButtonText = StringIdHelper.Id(R.string.close)
    )
}

val SHORT_ITEM_OOS_WARNING_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        dialogType = DialogType.Informational,
        shouldBoldTitle = true,
        title = StringIdHelper.Id(R.string.shipping_order_warning),
        body = StringIdHelper.Id(R.string.shipping_order_warning_body),
        positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
        negativeButtonText = StringIdHelper.Id(R.string.cancel)
    )
}

val CANCEL_1PL_HANDOFF_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.cancel_1pl_handoff_title),
        body = StringIdHelper.Id(R.string.cancel_1pl_handoff_body),
        positiveButtonText = StringIdHelper.Id(R.string.confirm),
        negativeButtonText = StringIdHelper.Id(R.string.cancel),
        cancelOnTouchOutside = false
    )
}

val ORDER_DETAILS_CANCEL_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.order_details_exit_title),
        body = StringIdHelper.Id(R.string.order_details_exit_body),
        positiveButtonText = StringIdHelper.Id(R.string.confirm),
        negativeButtonText = StringIdHelper.Id(R.string.cancel),
        cancelOnTouchOutside = false
    )
}

val ORDER_DETAILS_CANCEL_HANDOFF_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        dialogType = DialogType.CustomRadioButtons,
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.order_details_exit_title),
        customData = listOf(
            StringIdHelper.Id(R.string.customer_not_here),
            StringIdHelper.Id(R.string.other),
        ) as Serializable,
        body = StringIdHelper.Id(R.string.order_details_exit_body),
        positiveButtonText = StringIdHelper.Id(R.string.confirm),
        negativeButtonText = StringIdHelper.Id(R.string.cancel),
        cancelOnTouchOutside = false
    )
}

val RX_ORDER_DETAILS_CANCEL_DESTAGE_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.order_details_exit_title),
        shouldBoldTitle = true,
        body = StringIdHelper.Id(R.string.order_details_prescription_abandon_exit_body),
        questionBody = StringIdHelper.Id(R.string.rx_dug_code_warning_question),
        positiveButtonText = StringIdHelper.Id(R.string.confirm),
        negativeButtonText = StringIdHelper.Id(R.string.cancel)
    )
}

val RX_ORDER_DETAILS_CANCEL_HANDOFF_ARG_DATA: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.pharmacy_return_prescription),
        shouldBoldTitle = true,
        body = StringIdHelper.Id(R.string.order_details_prescription_exit_body),
        questionBody = StringIdHelper.Id(R.string.rx_dug_code_warning_question),
        positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
        negativeButtonText = StringIdHelper.Id(R.string.cancel)
    )
}

val HANDOFF_FULL_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.handoff_full_dialog_title),
        body = StringIdHelper.Id(R.string.handoff_full_dialog_body),
        positiveButtonText = StringIdHelper.Id(R.string.ok)
    )
}

fun getRejectedItemDialog(titleId: Int, name: String, quantity: String, storage: String): CustomDialogArgData =
    CustomDialogArgData(
        dialogStyle = DialogStyle.RejectedItems,
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(titleId),
        body = StringIdHelper.FormatWithExtraAdditionalString(R.string.remove_items_body, name, quantity, storage),
        shouldBoldTitle = true,
        positiveButtonText = StringIdHelper.Id(R.string.remove_item_header),
        negativeButtonText = StringIdHelper.Id(R.string.cancel),
        cancelOnTouchOutside = false
    )

val MARK_REJECTED_ITEM_AS_MISPLACED_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = R.string.report_misplaced_item_dialog_title.toIdHelper(),
        body = R.string.report_misplaced_item_dialog_body.toIdHelper(),
        positiveButtonText = R.string.report_misplaced_item_dialog_positive_button.toIdHelper(),
        negativeButtonText = R.string.cancel.toIdHelper(),
        cancelOnTouchOutside = false
    )
}

val MARK_REJECTED_1PL_ITEM_AS_MISPLACED_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = R.string.report_misplaced_item_dialog_title.toIdHelper(),
        body = R.string.report_misplaced_item_dialog_body.toIdHelper(),
        positiveButtonText = R.string.confirm.toIdHelper(),
        negativeButtonText = R.string.cancel.toIdHelper(),
        cancelOnTouchOutside = false
    )
}

val CONFIRM_REMOVAL_OF_ITEMS_THEN_DESTAGE: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.remove_all_rejected_items_title),
        shouldBoldTitle = true,
        body = StringIdHelper.Id(R.string.remove_all_rejected_items_body),
        positiveButtonText = StringIdHelper.Id(R.string.confirm),
        negativeButtonText = StringIdHelper.Id(R.string.cancel)
    )
}

val REMOVE_ITEMS_BACKOUT: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.remove_backout_title),
        body = StringIdHelper.Id(R.string.remove_backout_body),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
        negativeButtonText = StringIdHelper.Id(R.string.cancel)
    )
}

fun getHandOffTakenNotificationDialog(pickerName: String?): CustomDialogArgData =
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.handoff_not_available_title),
        body = if (pickerName.isNullOrBlank()) {
            StringIdHelper.Id(R.string.handoff_not_available_body_generic)
        } else {
            StringIdHelper.Format(R.string.handoff_not_available_body, pickerName)
        },
        positiveButtonText = StringIdHelper.Id(R.string.continue_cta)
    )

val HANDOFF_ASSIGNED_INPROGRESS: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.handoff_inprogress_title),
        body = StringIdHelper.Id(R.string.handoff_inprogress_body),
        positiveButtonText = StringIdHelper.Id(R.string.continue_cta)
    )
}

val MUST_COMPLETE_HANDOFF_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.notifications_to_complete_handoff_title),
        body = StringIdHelper.Id(R.string.notifications_to_complete_handoff_body),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
        cancelOnTouchOutside = true
    )
}

/*val ORIGINAL_DRIVER_ARRIVED_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        dialogType = DialogType.HybridNotification,
        largeImage = R.drawable.ic_driver_arrived_notification,
        title = R.string.flash_order_driver_arrived_notification_title.toIdHelper(),
        body = R.string.flash_order_driver_arrived_notification_body.toIdHelper(),
        positiveButtonText = R.string.flash_order_continue_to_handoff.toIdHelper(),
        negativeButtonText = R.string.flash_order_continue_staging_button.toIdHelper(),
        cancelOnTouchOutside = false
    )
}*/

val PHARMACY_STAFF_REQUIRED_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        dialogType = DialogType.TitleImageInfo,
        largeImage = R.drawable.ic_rx_staff_member_required,
        title = StringIdHelper.Id(R.string.rx_dug_pharmacy_staff_member_required_dialog_title),
        body = StringIdHelper.Id(R.string.rx_dug_pharmacy_staff_member_required_dialog_body),
        positiveButtonText = StringIdHelper.Id(R.string.continue_cta)
    )
}

fun getPrescriptionAddOn(name: String): CustomDialogArgData {
    return CustomDialogArgData(
        dialogType = DialogType.TitleImageInfo,
        largeImage = R.drawable.ic_pharmacy,
        title = StringIdHelper.Id(R.string.pharmacy_add_on_title),
        body = StringIdHelper.Format(R.string.pharmacy_add_on_body, name),
        positiveButtonText = StringIdHelper.Id(R.string.continue_cta)
    )
}

val DESTAGING_DIALOG_ARGS: CustomDialogArgData by lazy {
    CustomDialogArgData(
        dialogType = DialogType.DestagingDialog,
        title = StringIdHelper.Raw(""),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
        cancelOnTouchOutside = true
    )
}

val ORDER_NOT_AVAILABLE_GENERIC_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = R.string.order_not_available_title.toIdHelper(),
        body = R.string.order_not_available_body_generic.toIdHelper(),
        positiveButtonText = R.string.ok.toIdHelper(),
    )
}

fun getOrderNotAvailableDialog(pickerName: String): CustomDialogArgData =
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = R.string.order_not_available_title.toIdHelper(),
        body = StringIdHelper.Format(R.string.order_not_available_body_format, pickerName),
        positiveButtonText = R.string.ok.toIdHelper(),
    )

fun getFlashOrderNotAvailableDialog(pickerName: String): CustomDialogArgData =
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = R.string.order_not_available_title.toIdHelper(),
        body = StringIdHelper.Format(R.string.flash_order_not_available_body_format, pickerName),
        positiveButtonText = R.string.ok.toIdHelper(),
    )

fun getPartnerPickOrderNotAvailableDialog(pickerName: String): CustomDialogArgData =
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = R.string.order_not_available_title.toIdHelper(),
        body = StringIdHelper.Format(R.string.partnerpick_order_not_available_body_format, pickerName),
        positiveButtonText = R.string.ok.toIdHelper(),
    )

val FLASH_ORDER_FINISH_STAGING_THIS_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = R.string.flash_order_dialog_title.toIdHelper(),
        body = R.string.flash_order_finish_staging_this_dialog_body.toIdHelper(),
        positiveButtonText = R.string.ok.toIdHelper(),
    )
}

val FLASH_ORDER_FINISH_STAGING_YOUR_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = R.string.flash_order_dialog_title.toIdHelper(),
        body = R.string.flash_order_finish_staging_your_dialog_body.toIdHelper(),
        positiveButtonText = R.string.flash_order_continue_staging_button.toIdHelper(),
        negativeButtonText = R.string.cancel.toIdHelper(),
    )
}

val FLASH_ORDER_END_THIS_PICK_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = R.string.flash_order_dialog_title.toIdHelper(),
        body = R.string.flash_order_end_this_pick_dialog_body.toIdHelper(),
        positiveButtonText = R.string.end_pick.toIdHelper(),
        negativeButtonText = R.string.continue_picking.toIdHelper(),
    )
}

val FLASH_ORDER_END_YOUR_PICK_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = R.string.flash_order_dialog_title.toIdHelper(),
        body = R.string.flash_order_end_your_pick_dialog_body.toIdHelper(),
        positiveButtonText = R.string.end_pick.toIdHelper(),
        negativeButtonText = R.string.cancel.toIdHelper(),
    )
}

val PARNTERPICK_ORDER_FINISH_STAGING_THIS_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = R.string.partnerpick_order_title.toIdHelper(),
        body = R.string.partnerpick_order_finish_staging_this_dialog_body.toIdHelper(),
        positiveButtonText = R.string.ok.toIdHelper(),
    )
}

/*val PARTNERPICK_ORDER_FINISH_STAGING_YOUR_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = R.string.partnerpick_order_title.toIdHelper(),
        body = R.string.partnerpick_order_finish_staging_your_dialog_body.toIdHelper(),
        positiveButtonText = R.string.flash_order_continue_staging_button.toIdHelper(),
        negativeButtonText = R.string.cancel.toIdHelper(),
    )
}*/

val PARTNERPICK_END_THIS_PICK_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = R.string.partnerpick_order_title.toIdHelper(),
        body = R.string.partnerpick_order_end_this_pick_dialog_body.toIdHelper(),
        positiveButtonText = R.string.end_pick.toIdHelper(),
        negativeButtonText = R.string.continue_picking.toIdHelper(),
    )
}

val PARTNERPICK_ORDER_END_YOUR_PICK_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = R.string.partnerpick_order_title.toIdHelper(),
        body = R.string.partnerpick_order_end_your_pick_dialog_body.toIdHelper(),
        positiveButtonText = R.string.end_pick.toIdHelper(),
        negativeButtonText = R.string.cancel.toIdHelper(),
    )
}

val RELOAD_DILAOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        title = R.string.something_went_wrong.toIdHelper(),
        body = R.string.something_wrong_body.toIdHelper(),
        positiveButtonText = R.string.try_again.toIdHelper(),
        negativeButtonText = R.string.close.toIdHelper(),
        cancelOnTouchOutside = false
    )
}

val ONE_TIME_LAUNCH_DIALOG : CustomDialogArgData by lazy {
    CustomDialogArgData (
        title = StringIdHelper.Raw(""),
        positiveButtonText = StringIdHelper.Id(R.string.ok),
        cancelOnTouchOutside = false,
        dialogType = DialogType.FirstLaunchDialogFragment
    )
}
fun getObtainSignatureDialogDialog(isDugOrder: Boolean): CustomDialogArgData {
    return CustomDialogArgData(
        dialogType = DialogType.TitleImageInfo,
        title = R.string.obtain_signature_modal_header.toIdHelper(),
        largeImage = R.drawable.ic_signature_obtain,
        body = when (isDugOrder) {
            true -> R.string.obtain_signature_modal_body_dug
            false -> R.string.obtain_signature_modal_body_3pl
        }.toIdHelper(),
        positiveButtonText = R.string.confirm.toIdHelper(),
        negativeButtonText = R.string.cancel_cta.toIdHelper()
    )
}

fun getDestageOrderHotReminderArgData(hotDialogActivityBagData: StringIdHelper): CustomDialogArgData =
    CustomDialogArgData(
        titleIcon = R.drawable.ic_alert,
        title = StringIdHelper.Id(R.string.hot_item_reminder_title),
        body = hotDialogActivityBagData,
        positiveButtonText = StringIdHelper.Id(android.R.string.ok),
        negativeButtonText = null,
        cancelOnTouchOutside = false,
        cancelable = false
    )

fun getNoBagsWithCustomerNameDialog(customerName: String): CustomDialogArgData =
    CustomDialogArgData(
        dialogType = DialogType.EbtWarning,
        largeImage = R.drawable.no_bags_large_icon,
        title = R.string.no_bags_title_with_customer_name.toFormatHelper(customerName),
        body = R.string.no_bags_body_with_customer_name.toFormatHelper(customerName),
        positiveButtonText = R.string.continue_cta.toIdHelper(),
        cancelOnTouchOutside = false
    )

fun getConfirmAllBagsRemovedDialog(customerName: String): CustomDialogArgData =
    CustomDialogArgData(
        title = R.string.all_bags_removed_title.toIdHelper(),
        body = R.string.confirm_removal_of_bags.toFormatHelper(customerName),
        positiveButtonText = R.string.confirm.toIdHelper(),
        negativeButtonText = StringIdHelper.Id(R.string.cancel),
        cancelOnTouchOutside = false
    )

/** All values needed to show the custom dialog */
data class CustomDialogArgData(
    val dialogType: DialogType = DialogType.Informational,
    val dialogStyle: DialogStyle? = null,
    @DrawableRes val largeImage: Int? = null,
    @DrawableRes val titleIcon: Int? = null,
    val title: StringIdHelper,
    val body: StringIdHelper? = null,
    val bodyWithBold: String? = null,
    val shouldBoldTitle: Boolean = false,
    val secondaryBody: StringIdHelper? = null,
    val questionBody: StringIdHelper? = null,
    val imageUrl: String? = null,
    val remainingOrderedQuantity: Int = 0,
    val remainingWeight: String = "",
    val isOrderedByWeight: Boolean = false,
    val positiveButtonText: StringIdHelper? = null,
    val negativeButtonText: StringIdHelper? = null,
    val cancelable: Boolean = true,
    val cancelOnTouchOutside: Boolean = true,
    val customData: Serializable? = null,
    val isWineOrder: Boolean = false,
    val cutomerArrivalTime: ZonedDateTime? = null,
    val orderedWeightOrRemainingQty: String? = null,
    val retryAction: Unit? = null,
    val closeIconVisibility: Boolean = false,
    val soundAndHaptic: String? = null,
    val boldWord: StringIdHelper? = null
) : DomainModel, Serializable

data class CustomDialogArgDataAndTag(val data: CustomDialogArgData, val tag: String) : DomainModel, Serializable

/** Internal view state representation used by databinding on the layout */
data class CustomDialogViewData(
    val dialogStyle: DialogStyle?,
    @DrawableRes val largeImage: Int?,
    @DrawableRes val titleIcon: Int?,
    val titleIconVisibility: Int,
    val shouldBoldTitle: Boolean = false,
    val title: String,
    val titleVisibility: Int,
    val body: String?,
    val bodyVisibility: Int,
    val bodyWithBold: String?,
    val secondaryBody: String?,
    val secondaryBodyVisibility: Int,
    val questionBody: String?,
    val questionBodyVisibility: Int,
    val positiveButtonText: String?,
    val imageUrl: String?,
    val remainingOrderedQuantity: Int,
    val remainingWeight: String,
    val isOrderedByWeight: Boolean = false,
    val orderedWeightOrRemainingQty: String?,
    val positiveButtonVisibility: Int,
    val negativeButtonText: String?,
    val negativeButtonVisibility: Int,
    val closeIconVisibility: Int,
    val boldWord: String?
)

fun CustomDialogArgData.toViewData(context: Context): CustomDialogViewData {
    return CustomDialogViewData(
        dialogStyle = dialogStyle,
        largeImage = largeImage,
        titleIcon = titleIcon,
        titleIconVisibility = if (titleIcon == null) View.INVISIBLE else View.VISIBLE,
        title = title.getString(context),
        titleVisibility = if (title.getString(context).isBlank()) View.GONE else View.VISIBLE,
        body = body?.getString(context),
        bodyVisibility = if (body == null) View.GONE else View.VISIBLE,
        bodyWithBold = bodyWithBold,
        secondaryBody = secondaryBody?.getString(context),
        secondaryBodyVisibility = if (secondaryBody == null) View.GONE else View.VISIBLE,
        questionBody = questionBody?.getString(context),
        questionBodyVisibility = if (questionBody == null) View.GONE else View.VISIBLE,
        imageUrl = imageUrl,
        remainingOrderedQuantity = remainingOrderedQuantity,
        remainingWeight = remainingWeight,
        isOrderedByWeight = isOrderedByWeight,
        orderedWeightOrRemainingQty = orderedWeightOrRemainingQty,
        positiveButtonText = positiveButtonText?.getString(context),
        negativeButtonText = negativeButtonText?.getString(context),
        positiveButtonVisibility = if (positiveButtonText == null) View.GONE else View.VISIBLE,
        negativeButtonVisibility = if (negativeButtonText == null) View.GONE else View.VISIBLE,
        shouldBoldTitle = shouldBoldTitle,
        closeIconVisibility = if (closeIconVisibility) View.VISIBLE else View.GONE,
        boldWord = boldWord?.getString(context)
    )
}

/** Action user took to close the dialog */
@Parcelize
enum class CloseAction : Parcelable {
    /** Positive action was taken by the user */
    Positive,

    /** Negative action was taken by the user */
    Negative,

    /** User dismissed dialog (system back press or tap outside bounds) */
    Dismiss
}
