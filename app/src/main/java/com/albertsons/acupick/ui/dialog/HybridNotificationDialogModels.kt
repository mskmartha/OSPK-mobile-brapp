package com.albertsons.acupick.ui.dialog

import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.notification.NotificationData
import com.albertsons.acupick.data.model.notification.NotificationType
import com.albertsons.acupick.data.model.notification.asFirstNameLastInitialDotString
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.UserFeedback
import com.albertsons.acupick.ui.util.toFormatHelper
import com.albertsons.acupick.ui.util.toIdHelper

/*val DRIVER_ARRIVED_DIALOG: CustomDialogArgData by lazy {
    CustomDialogArgData(
        dialogType = DialogType.ModalFiveConfirmation,
        // largeImage = R.drawable.ic_driver_arrived_notification,
        titleIcon = R.drawable.ic_flash_driver_arrived,
        title = R.string.flash_order_driver_arrived_notification_title.toIdHelper(),
        body = R.string.flash_order_driver_arrived_notification_body.toIdHelper(),
        positiveButtonText = R.string.confirm.toIdHelper(),
        negativeButtonText = R.string.cancel_cta.toIdHelper(),
        cancelOnTouchOutside = false
    )
}*/

fun getDriverArrivedDialog(data: NotificationData): CustomDialogArgData =
    CustomDialogArgData(
        dialogType = DialogType.ModalFiveConfirmation,
        titleIcon = R.drawable.ic_flash_driver_arrived,
        title = if (data.notificationType == NotificationType.ARRIVING) R.string.flash_order_driver_arriving_notification_title.toIdHelper() else
            R.string.flash_order_driver_arrived_notification_title.toIdHelper(),
        body = if (data.serviceLevel == OrderType.FLASH) {
            if (data.notificationType == NotificationType.ARRIVING) {
                R.string.flash_order_driver_arriving_notification_body.toIdHelper()
            } else {
                R.string.flash_order_driver_arrived_notification_body.toIdHelper()
            }
        } else {
            if (data.notificationType == NotificationType.ARRIVING) {
                R.string.partnerpick_order_driver_arriving_notification_body.toIdHelper()
            } else {
                R.string.partnerpick_order_driver_arrived_notification_body.toIdHelper()
            }
        },
        positiveButtonText = R.string.confirm.toIdHelper(),
        negativeButtonText = R.string.cancel_cta.toIdHelper(),
        cancelOnTouchOutside = false
    )

fun getEbtWarningDialog(isCattEnabled: Boolean): CustomDialogArgData =
    CustomDialogArgData(
        dialogType = DialogType.EbtWarning,
        largeImage = if (isCattEnabled) R.drawable.ebt_image else R.drawable.ebt_hybrid_notification_image,
        title = R.string.ebt_warning_title.toIdHelper(),
        positiveButtonText = R.string.ok_cta.toIdHelper(),
        body = R.string.ebt_warning_notification_sub_bullet.toIdHelper(),
        secondaryBody = R.string.ebt_warning_notification_weighted_bullet.toIdHelper(),
        cancelOnTouchOutside = false
    )

fun getNoBagsSingleWarningDialog(): CustomDialogArgData =
    CustomDialogArgData(
        dialogType = DialogType.EbtWarning,
        largeImage = R.drawable.no_bags_large_icon,
        title = R.string.no_bags_title.toIdHelper(),
        positiveButtonText = R.string.continue_cta.toIdHelper(),
        cancelOnTouchOutside = false
    )

fun getNoBagsBatchWarningDialog(): CustomDialogArgData =
    CustomDialogArgData(
        dialogType = DialogType.EbtWarning,
        largeImage = R.drawable.no_bags_large_icon,
        title = R.string.no_bags_batch_title.toIdHelper(),
        positiveButtonText = R.string.continue_cta.toIdHelper(),
        body = R.string.no_bags_batch_body.toIdHelper(),
        cancelOnTouchOutside = false
    )

fun getEbtNoBagsSingleWarningDialog(): CustomDialogArgData =
    CustomDialogArgData(
        dialogType = DialogType.EbtNoBagsWarning,
        titleIcon = R.drawable.ebt_image_cbp,
        largeImage = R.drawable.no_bags_small_icon,
        title = R.string.ebt_no_bags_title.toIdHelper(),
        body = R.string.ebt_warning_notification_sub_bullet.toIdHelper(),
        secondaryBody = R.string.ebt_warning_notification_weighted_bullet.toIdHelper(),
        positiveButtonText = R.string.ok_cta.toIdHelper(),
        cancelOnTouchOutside = false
    )

fun getEbtNoBagsBatchWarningDialog(): CustomDialogArgData =
    CustomDialogArgData(
        dialogType = DialogType.EbtNoBagsWarning,
        titleIcon = R.drawable.ebt_image_cbp,
        largeImage = R.drawable.no_bags_small_icon,
        title = R.string.ebt_no_bags_title.toIdHelper(),
        body = R.string.ebt_warning_notification_sub_bullet.toIdHelper(),
        secondaryBody = R.string.ebt_warning_notification_weighted_bullet.toIdHelper(),
        questionBody = R.string.ebt_no_bags_body.toIdHelper(),
        positiveButtonText = R.string.ok_cta.toIdHelper(),
        cancelOnTouchOutside = false
    )

fun getFlashAssignedPickNotificationArgData(pickerName: String): CustomDialogArgData =
    CustomDialogArgData(
        dialogType = DialogType.ModalFiveConfirmation,
        titleIcon = R.drawable.ic_flashpicking,
        title = R.string.flash_order_notification_title.toFormatHelper(pickerName),
        body = R.string.flash_order_assigned_pick_notification_body.toIdHelper(),
        positiveButtonText = R.string.flash_order_assigned_pick_notification_begin_button_text.toIdHelper(),
        negativeButtonText = R.string.flash_order_notification_negative_cta_text.toIdHelper(),
        cancelOnTouchOutside = false,
        customData = true,
        cancelable = false,
        soundAndHaptic = UserFeedback.SoundAndHaptic.PickingInterjection.shortName
    )

fun getFlashEndPickNotificationArgData(pickerName: String): CustomDialogArgData =
    CustomDialogArgData(
        dialogType = DialogType.ModalFiveConfirmation,
        // largeImage = R.drawable.flash_end_pick_image,
        titleIcon = R.drawable.ic_flashpicking,
        title = R.string.flash_order_notification_title.toFormatHelper(pickerName),
        body = R.string.flash_order_end_pick_notification_body.toIdHelper(),
        positiveButtonText = R.string.flash_order_end_pick_notification_begin_button_text.toIdHelper(),
        negativeButtonText = R.string.flash_order_notification_negative_cta_text.toIdHelper(),
        cancelOnTouchOutside = false,
        cancelable = false,
        soundAndHaptic = UserFeedback.SoundAndHaptic.PickingInterjection.shortName
    )

fun getFlashEndStagingNotificationArgData(pickerName: String): CustomDialogArgData =
    CustomDialogArgData(
        dialogType = DialogType.ModalFiveConfirmation,
        // largeImage = R.drawable.flash_end_staging_image,
        titleIcon = R.drawable.ic_flashpicking,
        title = R.string.flash_order_notification_title.toFormatHelper(pickerName),
        body = R.string.flash_order_finish_staging_notification_body.toIdHelper(),
        positiveButtonText = R.string.flash_order_finish_staging_notification_begin_button_text.toIdHelper(),
        negativeButtonText = R.string.flash_order_notification_negative_cta_text.toIdHelper(),
        cancelOnTouchOutside = false,
        cancelable = false,
        soundAndHaptic = UserFeedback.SoundAndHaptic.PickingInterjection.shortName
    )

fun getFlashEndHandOffNotificationArgData(pickerName: String): CustomDialogArgData =
    CustomDialogArgData(
        dialogType = DialogType.ModalFiveConfirmation,
        // largeImage = R.drawable.flash_finish_handoff_image,
        titleIcon = R.drawable.ic_flashpicking,
        title = R.string.flash_order_notification_title.toFormatHelper(pickerName),
        body = R.string.flash_order_finish_handoff_notification_body.toIdHelper(),
        positiveButtonText = R.string.flash_order_finish_handoff_notification_begin_button_text.toIdHelper(),
        negativeButtonText = R.string.flash_order_notification_negative_cta_text.toIdHelper(),
        cancelOnTouchOutside = false,
        cancelable = false,
        soundAndHaptic = UserFeedback.SoundAndHaptic.PickingInterjection.shortName
    )

fun getCustomerArrivedNotificationData(driverName: String): CustomDialogArgData =
    CustomDialogArgData(
        dialogType = DialogType.ModalFiveConfirmation,
        // largeImage = R.drawable.customer_arrived_image,
        titleIcon = R.drawable.ic_flash_driver_arrived,
        title = R.string.customer_arrived_notification_title.toIdHelper(),
        body = R.string.customer_arrived_notification_body.toFormatHelper(driverName),
        positiveButtonText = R.string.confirm.toIdHelper(),
        negativeButtonText = R.string.cancel_cta.toIdHelper(),
        cancelable = false
    )

fun getDriverArrivedNotificationData(partnerCompany: String, driverName: String, customerName: String): CustomDialogArgData =
    CustomDialogArgData(
        dialogType = DialogType.ModalFiveConfirmation,
        // largeImage = R.drawable.urgent_3pl_icon,
        titleIcon = R.drawable.ic_flash_driver_arrived,
        title = R.string.dasher_arrived_notification_title.toFormatHelper(partnerCompany),
        body = R.string.dasher_arrived_notification_body.toFormatHelper(driverName, customerName),
        positiveButtonText = R.string.confirm.toIdHelper(),
        negativeButtonText = R.string.cancel_cta.toIdHelper(),
        cancelable = false
    )

fun getDugInterjectionAssignedHandoffNotificationArgData(data: NotificationData, associateName: String?): CustomDialogArgData =
    CustomDialogArgData(
        dialogType = DialogType.ModalFiveConfirmation,
        titleIcon = R.drawable.ic_dug_interjection,
        title = R.string.dug_interjection_notification_title.toFormatHelper(data.asFirstNameLastInitialDotString()),
        body = R.string.dug_interjection_assigned_handoff_notification_body.toFormatHelper(associateName ?: ""),
        cutomerArrivalTime = data.customerArrivedTime,
        positiveButtonText = R.string.begin_handoff_now.toIdHelper(),
        cancelOnTouchOutside = false,
        customData = false,
        cancelable = false,
        soundAndHaptic = UserFeedback.SoundAndHaptic.ArrivalInterjection.shortName
    )

fun getDugInterjectionSkipPickingNotificationArgData(data: NotificationData, associateName: String?): CustomDialogArgData =
    CustomDialogArgData(
        dialogType = DialogType.ModalFiveConfirmation,
        titleIcon = R.drawable.ic_dug_interjection,
        title = R.string.dug_interjection_notification_title.toFormatHelper(data.asFirstNameLastInitialDotString()),
        body = R.string.dug_interjection_skip_picking_notification_body.toFormatHelper(associateName ?: ""),
        cutomerArrivalTime = data.customerArrivedTime,
        positiveButtonText = R.string.begin_handoff_now.toIdHelper(),
        cancelOnTouchOutside = false,
        customData = true,
        cancelable = false,
        soundAndHaptic = UserFeedback.SoundAndHaptic.ArrivalInterjection.shortName
    )

fun getDugInterjectionSkipStagingNotificationArgData(data: NotificationData, associateName: String?): CustomDialogArgData =
    CustomDialogArgData(
        dialogType = DialogType.ModalFiveConfirmation,
        titleIcon = R.drawable.ic_dug_interjection,
        title = R.string.dug_interjection_notification_title.toFormatHelper(data.asFirstNameLastInitialDotString()),
        body = R.string.dug_interjection_skip_staging_notification_body.toFormatHelper(associateName ?: ""),
        cutomerArrivalTime = data.customerArrivedTime,
        positiveButtonText = R.string.flash_order_continue_staging_button.toIdHelper(),
        negativeButtonText = R.string.home_begin_handoff.toIdHelper(),
        cancelOnTouchOutside = false,
        customData = true,
        cancelable = false,
        soundAndHaptic = UserFeedback.SoundAndHaptic.ArrivalInterjection.shortName
    )

fun getInterjectionDailogForAllUsers(data: NotificationData, skipInterjectionAndContinueStaging: StringIdHelper? = null): CustomDialogArgData =
    CustomDialogArgData(
        dialogType = DialogType.InterjectionForALLUsers,
        title = R.string.begin_handoff_now.toIdHelper(),
        body = R.string.dug_interjection_notification_title.toFormatHelper(data.asFirstNameLastInitialDotString()),
        cutomerArrivalTime = data.customerArrivedTime,
        positiveButtonText = R.string.home_begin_handoff.toIdHelper(),
        cancelOnTouchOutside = false,
        customData = true,
        cancelable = false,
        negativeButtonText = skipInterjectionAndContinueStaging,
        soundAndHaptic = UserFeedback.SoundAndHaptic.ArrivalInterjection.shortName
    )
