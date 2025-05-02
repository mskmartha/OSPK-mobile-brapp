package com.albertsons.acupick.data.model.chat

import com.albertsons.acupick.data.network.ChatZonedDateTimeJsonAdapter
import com.albertsons.acupick.infrastructure.utils.firstMedia
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import com.twilio.conversations.Message
import timber.log.Timber
import java.io.InputStream
import java.time.ZonedDateTime

data class MessageDataItem(
    val sid: String,
    val conversationSid: String,
    val author: String,
    val dateCreated: Long,
    val body: String?,
    val index: Long,
    val attributes: CustomAttributes?,
    val direction: Int,
    val sendStatus: Int,
    val uuid: String,
    val mediaSid: String? = null,
    val mediaFileName: String? = null,
    val mediaType: String? = null,
    val mediaSize: Long? = null,
    val mediaUri: String? = null,
    val mediaDownloadId: Long? = null,
    val mediaDownloadedBytes: Long? = null,
    val mediaDownloadState: Int = 0,
    val mediaUploading: Boolean = false,
    val mediaUploadedBytes: Long? = null,
    val mediaUploadUri: String? = null,
    val inputStream: InputStream? = null,
    val errorCode: Int = 0,
)

private val moshi: Moshi = Moshi.Builder()
    .add(ZonedDateTime::class.java, ChatZonedDateTimeJsonAdapter().nullSafe())
    .add(DisplayType::class.java, EnumJsonAdapter.create(DisplayType::class.java).withUnknownFallback(null).nullSafe())
    .add(MessageSource::class.java, EnumJsonAdapter.create(MessageSource::class.java).withUnknownFallback(null).nullSafe())
    .add(MessageSubType::class.java, EnumJsonAdapter.create(MessageSubType::class.java).withUnknownFallback(null).nullSafe())
    .build()

fun Message.toMessageDataItem(
    uuid: String = "",
    sendStatus: Int? = null,
    inputStream: InputStream? = null,
    shouldEnrichedChatFlag: Boolean?
): MessageDataItem {
    // Timber.d("Conversation Message ${this.body} ${this.author} ${this.author == currentUserIdentity} ")
    val customAttributes = CustomAttributesJsonAdapter(moshi)
    val customdata =
        try {
            Timber.d("customdata -- Body - $body--${attributes.jsonObject}")
            customAttributes.fromJson(attributes.jsonObject.toString())
        } catch (e: Exception) {
            Timber.d("toMessageDataItem error $e, ${attributes.jsonObject} , ${attributes.type}")
            CustomAttributes()
        }
    val media = firstMedia // @todo: support multiple media

    fun getDirection(): Direction {
        return if (customdata?.messageSource == MessageSource.PICKER && customdata.messageType == DisplayType.TEXT) {
            Direction.OUTGOING
        } else if (customdata?.messageSource == MessageSource.PICKER && customdata.messageType == DisplayType.AISLE_PHOTO) {
            Direction.OUTGOING_IMAGE
        } else if (customdata?.messageSource != MessageSource.PICKER && customdata?.messageType == DisplayType.FORMATTED) {
            if (shouldEnrichedChatFlag == true) {
                if (customdata.messageSubType == MessageSubType.SUB || customdata.messageSubType == MessageSubType.SWAP) {
                    if (customdata.orderedItem?.substitutedItems?.any { it.subApprovalStatus != null } == true) {
                        if (customdata.orderedItem.substitutedItems.any { it.subApprovalStatus == SubApprovalStatus.REQUESTED }) {
                            Direction.ENRICHDED_SUBSTITUTION
                        } else {
                            Timber.d(" getDirection ENRICHDED_CAS_SUBSTITUTION")
                            Direction.ENRICHDED_CAS_SUBSTITUTION
                        }
                    } else {
                        Timber.d(" getDirection Direction.SYSTEM END SWAP")
                        Direction.SYSTEM
                    }
                } else {
                    if (customdata.orderedItem?.oosApprovalStatus != null) {
                        if (customdata.orderedItem.oosApprovalStatus == OosApprovalStatus.REQUESTED || customdata.orderedItem.oosApprovalStatus == OosApprovalStatus.REJECTED) {
                            Direction.ENRICHDED_OOS_SUBSTITUTION
                        } else {
                            Timber.d(" getDirection ENRICHDED_OOS_DECLINE")
                            Direction.ENRICHDED_OOS_DECLINE
                        }
                    } else {
                        Timber.d(" getDirection Direction.SYSTEM END OSS")
                        Direction.SYSTEM
                    }
                }
            } else {
                Timber.d(" getDirection Direction.SYSTEM END")
                Direction.SYSTEM
            }
        } else if (customdata?.messageSource != MessageSource.PICKER && customdata?.messageType == DisplayType.GREETING) {
            Direction.GREETING
        } else if (customdata?.messageType == DisplayType.INTERNAL) {
            if (customdata.internalMessageSubType == InternalMessageSubType.JOINED_CHAT) {
                Direction.PICKER_JOINED
            } else {
                Direction.PICKER_LEFT
            }
        } else {
            Direction.INCOMING
        }
    }

    return MessageDataItem(
        this.sid,
        this.conversationSid,
        this.author,
        this.dateCreatedAsDate.time,
        this.body ?: "",
        this.messageIndex,
        customdata,
        getDirection().value,
        sendStatus ?: SendStatus.SENT.value,
        uuid,
        media?.sid,
        media?.filename,
        media?.contentType,
        media?.size,
        inputStream = inputStream
    )
}

enum class SendStatus(val value: Int) {
    UNDEFINED(0),
    SENDING(1),
    SENT(2),
    SENT_RETRY(3),
    ERROR(4);

    companion object {
        private val valuesMap = values().associateBy { it.value }
        fun fromInt(value: Int) = valuesMap[value] ?: error("Invalid value $value for SendStatus")
    }
}

enum class Direction(val value: Int) {
    INCOMING(0),
    OUTGOING(1),
    OUTGOING_IMAGE(2),
    SYSTEM(3),
    GREETING(4),
    ENRICHDED_SUBSTITUTION(5),
    ENRICHDED_CAS_SUBSTITUTION(6),
    ENRICHDED_OOS_SUBSTITUTION(7),
    ENRICHDED_OOS_DECLINE(8),
    PICKER_JOINED(9),
    PICKER_LEFT(10);

    companion object {
        private val valuesMap = values().associateBy { it.value }
        fun fromInt(value: Int) = valuesMap[value] ?: error("Invalid value $value for Direction")
    }
}

enum class DownloadState(val value: Int) {
    NOT_STARTED(0),
    DOWNLOADING(1),
    COMPLETED(2),
    ERROR(3);

    companion object {
        private val valuesMap = values().associateBy { it.value }
        fun fromInt(value: Int) = valuesMap[value] ?: error("Invalid value $value for DownloadState")
    }
}

fun List<Message>.asMessageDataItems(shouldEnrichedChatFlag: Boolean?) = map { it.toMessageDataItem(shouldEnrichedChatFlag = shouldEnrichedChatFlag) }
