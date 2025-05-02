package com.albertsons.acupick.ui.chat

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.chat.CustomAttributes
import com.albertsons.acupick.data.model.chat.Direction
import com.albertsons.acupick.data.model.chat.DisplayType
import com.albertsons.acupick.data.model.chat.DownloadState
import com.albertsons.acupick.data.model.chat.MessageDataItem
import com.albertsons.acupick.data.model.chat.MessageSubType
import com.albertsons.acupick.data.model.chat.SendStatus
import com.albertsons.acupick.infrastructure.utils.SPACED_HOUR_MINUTE_AM_PM_TIME_FORMATTER
import com.albertsons.acupick.infrastructure.utils.ellipse
import com.albertsons.acupick.infrastructure.utils.formattedWith
import com.albertsons.acupick.infrastructure.utils.orZero
import java.time.ZoneId

data class MessageListViewItem(
    val sid: String,
    val uuid: String,
    val index: Long,
    val direction: Direction,
    val author: String,
    val body: String,
    val dateCreated: String,
    val sendStatus: SendStatus,
    val customAttributes: CustomAttributes?,
    val showTime: Boolean,
    val showRetrySpinner: Boolean,
    val pickerInitial: String,
    val formattedTime: String,
    val swapMessage: String,
    val isSwapOrSub: Boolean,
    val mediaSid: String?,
    val mediaFileName: String?,
    val mediaType: String?,
    val mediaSize: Long?,
    val mediaUri: Uri?,
    val mediaDownloadId: Long?,
    val mediaDownloadedBytes: Long?,
    val mediaDownloadState: DownloadState,
    val mediaUploading: Boolean,
    val mediaUploadedBytes: Long?,
    val mediaUploadUri: Uri?,
    val errorCode: Int,
    val isApproved: Boolean = false,
    val senderName: String,
    val senderFirstName: String
)

fun MessageDataItem.toMessageListViewItem(app: Application, userTokenizedLdapId: String, otherPickerTokenizedLdapId: Map<String, String>): MessageListViewItem {

    fun getSwapMessage(): String {
        return if (this.attributes?.messageType == DisplayType.FORMATTED) {
            val item = this.attributes?.orderedItem
            if (this.attributes?.messageSubType == MessageSubType.SWAP || this.attributes?.messageSubType == MessageSubType.SUB) {
                val message = app.getString(
                    R.string.chat_system_message_sub_swap,
                    item?.orderedItemDescription.ellipse(20), item?.orderedQuantity
                ).plus(
                    if (item?.orderedItemPrice.orZero() == 0.0) {
                        app.getString(R.string.chat_system_message_sub_swap_without_price)
                    } else {
                        app.getString(R.string.chat_system_message_sub_swap_with_price, item?.orderedItemPrice.orZero().toString())
                    }
                ).plus("\n")
                    .plus(
                        item?.substitutedItems?.mapIndexed { index, items ->
                            app.getString(
                                R.string.chat_system_message_sub_swap_items,
                                items.substitutedItemDescription.ellipse(20),
                                items.substitutedQuantity,
                            ).plus(
                                if (items.substitutedItemPrice.orZero() != 0.0) {
                                    app.getString(R.string.chat_system_message_sub_swap_items_with_price, items.substitutedItemPrice.orZero().toString())
                                } else {
                                    ""
                                }
                            ).plus(
                                if (index + 1 == item.substitutedItems?.size) {
                                    "."
                                } else {
                                    app.getString(R.string.chat_and).plus("\n")
                                }
                            )
                        }?.joinToString("")
                    )
                message
            } else {
                if (item?.orderedItemPrice.orZero() == 0.0)
                    app.getString(R.string.chat_system_message_oos_without_price, item?.orderedItemDescription.ellipse(20), item?.orderedQuantity)
                else
                    app.getString(R.string.chat_system_message_oos, item?.orderedItemDescription.ellipse(20), item?.orderedQuantity, item?.orderedItemPrice.orZero().toString())
            }
        } else {
            ""
        }
    }

    fun getSenderName(): String {
        return if (userTokenizedLdapId.equals(attributes?.tokenizedLdapId, ignoreCase = true)) {
            app.getString(R.string.chat_sender_you)
        } else {
            otherPickerTokenizedLdapId[attributes?.tokenizedLdapId] ?: ""
        }
    }

    return MessageListViewItem(
        this.sid,
        this.uuid,
        this.index,
        Direction.fromInt(this.direction),
        this.author,
        this.body ?: "",
        this.dateCreated.toString(),
        SendStatus.fromInt(sendStatus),
        this.attributes,
        showTime = this.sendStatus == SendStatus.SENT.value || this.sendStatus == SendStatus.SENDING.value,
        showRetrySpinner = this.sendStatus == SendStatus.SENT_RETRY.value,
        pickerInitial = this.attributes?.pickerInitials ?: "",
        formattedTime = this.attributes?.messageSentDate?.withZoneSameInstant(ZoneId.systemDefault()).formattedWith(SPACED_HOUR_MINUTE_AM_PM_TIME_FORMATTER) ?: "",
        swapMessage = getSwapMessage(),
        isSwapOrSub = this.attributes?.messageSubType == MessageSubType.SWAP || this.attributes?.messageSubType == MessageSubType.SUB,
        this.mediaSid,
        this.mediaFileName,
        this.mediaType,
        this.mediaSize,
        this.mediaUri?.toUri(),
        this.mediaDownloadId,
        this.mediaDownloadedBytes,
        DownloadState.fromInt(this.mediaDownloadState),
        this.mediaUploading,
        this.mediaUploadedBytes,
        this.mediaUploadUri?.toUri(),
        this.errorCode,
        senderName = getSenderName(),
        senderFirstName = otherPickerTokenizedLdapId[attributes?.tokenizedLdapId] ?: ""
    )
}
