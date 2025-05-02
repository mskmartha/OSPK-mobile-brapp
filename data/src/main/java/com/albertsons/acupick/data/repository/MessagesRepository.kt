package com.albertsons.acupick.data.repository

import com.albertsons.acupick.data.model.User
import com.albertsons.acupick.data.model.chat.CustomAttributes
import com.albertsons.acupick.data.model.chat.CustomAttributesJsonAdapter
import com.albertsons.acupick.data.model.chat.Direction
import com.albertsons.acupick.data.model.chat.DisplayType
import com.albertsons.acupick.data.model.chat.InternalMessageSubType
import com.albertsons.acupick.data.model.chat.MessageDataItem
import com.albertsons.acupick.data.model.chat.MessageSource
import com.albertsons.acupick.data.model.chat.MessageSubType
import com.albertsons.acupick.data.model.chat.SendStatus
import com.albertsons.acupick.data.model.chat.toMessageDataItem
import com.albertsons.acupick.data.network.ChatZonedDateTimeJsonAdapter
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProviderImpl
import com.albertsons.acupick.infrastructure.utils.firstMedia
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import com.twilio.conversations.Attributes
import com.twilio.conversations.MediaUploadListener
import com.twilio.conversations.extensions.advanceLastReadMessageIndex
import com.twilio.conversations.extensions.getConversation
import com.twilio.conversations.extensions.getMessageByIndex
import com.twilio.conversations.extensions.getTemporaryContentUrl
import com.twilio.conversations.extensions.sendMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.io.InputStream
import java.time.ZonedDateTime
import java.util.Date
import java.util.UUID

interface MessagesRepository {
    suspend fun sendTextMessage(
        conversationSid: String,
        orderNumber: String,
        fulfillmentOrderNumber: String,
        text: String,
        uuid: String,
        user: User,
    )

    suspend fun retrySendTextMessage(
        conversationSid: String,
        messageUuid: String,
    )

    suspend fun sendMediaMessage(
        conversationSid: String,
        orderNumber: String,
        fulfillmentOrderNumber: String,
        uri: String,
        inputStream: InputStream,
        fileName: String?,
        mimeType: String?,
        messageUuid: String,
        user: User,
        text: String?
    )
    suspend fun sendPickerJoinedMessage(conversationSid: String, orderNumber: String, user: String, tokenizedLdap: String)
    suspend fun sendPickerLeftMessage(conversationSid: String, orderNumber: String, user: String, tokenizedLdap: String)

    suspend fun retrySendMediaMessage(conversationSid: String, messageUuid: String)
    suspend fun updateMessageStatus(messageUuid: String, sendStatus: SendStatus, errorCode: Int = 0, conversationSid: String,)
    suspend fun notifyMessageRead(conversationSid: String, index: Long)
    suspend fun getMediaContentTemporaryUrl(index: Long, conversationSid: String): String
}

class MessagesRepositoryImpl(
    private val conversationsClient: ConversationsClientWrapper,
    private val conversationsRepository: ConversationsRepository,
    private val networkAvailabilityManager: NetworkAvailabilityManager,
    private val siteRepository: SiteRepository,
    private val dispatchers: DispatcherProvider = DispatcherProviderImpl(),
) : MessagesRepository {

    private val moshi: Moshi = Moshi.Builder()
        .add(ZonedDateTime::class.java, ChatZonedDateTimeJsonAdapter().nullSafe())
        .add(DisplayType::class.java, EnumJsonAdapter.create(DisplayType::class.java).withUnknownFallback(null).nullSafe())
        .add(MessageSource::class.java, EnumJsonAdapter.create(MessageSource::class.java).withUnknownFallback(null).nullSafe())
        .add(MessageSubType::class.java, EnumJsonAdapter.create(MessageSubType::class.java).withUnknownFallback(null).nullSafe())
        .build()

    val customAttributesJsonAdapter = CustomAttributesJsonAdapter(moshi)
    private val repositoryScope = CoroutineScope(dispatchers.IO + SupervisorJob())

    override suspend fun sendTextMessage(conversationSid: String, orderNumber: String, fulfillmentOrderNumber: String, text: String, uuid: String, user: User) {

        val attributes = CustomAttributes(
            messageSentDate = ZonedDateTime.now(),
            tokenizedLdapId = user.tokenizedLdapId,
            messageSource = MessageSource.PICKER,
            messageType = DisplayType.TEXT,
            fulfillmentOrderNumber = fulfillmentOrderNumber,
            storeNumber = siteRepository.siteDetails.value?.siteId,
            pickerInitials = user.firstInitialLastName(),
            orderNumber = orderNumber
        )

        val message = MessageDataItem(
            "",
            conversationSid,
            author = user.userId,
            Date().time,
            text,
            -1,
            attributes,
            Direction.OUTGOING.value,
            SendStatus.SENDING.value,
            uuid
        )
        conversationsRepository.addMessage(message)

        if (networkAvailabilityManager.isConnected.value) {
            try {
                val conversation = conversationsClient.getConversationsClient().getConversation(conversationSid)
                val sentMessage = conversation.sendMessage {
                    Timber.d("sendTextMessage $attributes ----- ${Attributes(JSONObject(customAttributesJsonAdapter.toJson(attributes)))}")
                    this.attributes = Attributes(JSONObject(customAttributesJsonAdapter.toJson(attributes)))
                    this.body = text
                }.toMessageDataItem(uuid, SendStatus.SENT.value, shouldEnrichedChatFlag = siteRepository.twoWayCommsFlags.enrichedChat)

                conversationsRepository.updateMessage(sentMessage)
                Timber.d("retrySendTextMessage 3")
            } catch (e: Exception) {
                Timber.d("sendTextMessage error ${e.message}")
                conversationsRepository.updateMessageStatus(conversationSid, uuid, SendStatus.ERROR.value, 0)
            }
        } else {
            conversationsRepository.updateMessageStatus(conversationSid, uuid, SendStatus.ERROR.value, 0)
        }
    }

    override suspend fun sendPickerJoinedMessage(conversationSid: String, orderNumber: String, user: String, tokenizedLdap: String) {
        if (siteRepository.twoWayCommsFlags.masterOrderView2 == true) {
            val messageUuid = UUID.randomUUID().toString()
            val attributes = CustomAttributes(
                messageSentDate = ZonedDateTime.now(),
                messageSource = MessageSource.PICKER,
                messageType = DisplayType.INTERNAL,
                tokenizedLdapId = tokenizedLdap,
                internalMessageSubType = InternalMessageSubType.JOINED_CHAT,
                storeNumber = siteRepository.siteDetails.value?.siteId,
                orderNumber = orderNumber
            )

            val message = MessageDataItem(
                "",
                conversationSid,
                author = user,
                Date().time,
                "",
                -1,
                attributes,
                Direction.PICKER_JOINED.value,
                SendStatus.SENDING.value,
                messageUuid
            )
            sendJoinedLeftMessage(conversationSid, attributes, messageUuid, message)
        }
    }

    override suspend fun sendPickerLeftMessage(conversationSid: String, orderNumber: String, user: String, tokenizedLdap: String) {
        if (siteRepository.twoWayCommsFlags.masterOrderView2 == true) {
            val messageUuid = UUID.randomUUID().toString()
            val attributes = CustomAttributes(
                messageSentDate = ZonedDateTime.now(),
                messageSource = MessageSource.PICKER,
                messageType = DisplayType.INTERNAL,
                tokenizedLdapId = tokenizedLdap,
                internalMessageSubType = InternalMessageSubType.LEFT_CHAT,
                storeNumber = siteRepository.siteDetails.value?.siteId,
                orderNumber = orderNumber
            )

            val message = MessageDataItem(
                "",
                conversationSid,
                author = user,
                Date().time,
                "",
                -1,
                attributes,
                Direction.PICKER_LEFT.value,
                SendStatus.SENDING.value,
                messageUuid
            )
            sendJoinedLeftMessage(conversationSid, attributes, messageUuid, message)
        }
    }

    private suspend fun sendJoinedLeftMessage(conversationSid: String, attributes: CustomAttributes, messageUuid: String, message: MessageDataItem) {
        conversationsRepository.addMessage(message)
        if (networkAvailabilityManager.isConnected.value) {
            try {
                val conversation = conversationsClient.getConversationsClient().getConversation(conversationSid)
                val sentMessage = conversation.sendMessage {
                    Timber.d("sendTextMessage $attributes ----- ${Attributes(JSONObject(customAttributesJsonAdapter.toJson(attributes)))}")
                    this.attributes = Attributes(JSONObject(customAttributesJsonAdapter.toJson(attributes)))
                    this.body = ""
                }.toMessageDataItem(messageUuid, SendStatus.SENT.value, shouldEnrichedChatFlag = siteRepository.twoWayCommsFlags.enrichedChat)

                conversationsRepository.updateMessage(sentMessage)
                Timber.d("retrySendTextMessage 3")
            } catch (e: Exception) {
                Timber.d("sendTextMessage error ${e.message}")
                conversationsRepository.updateMessageStatus(conversationSid, messageUuid, SendStatus.ERROR.value, 0)
            }
        } else {
            conversationsRepository.updateMessageStatus(conversationSid, messageUuid, SendStatus.ERROR.value, 0)
        }
    }

    override suspend fun retrySendTextMessage(conversationSid: String, messageUuid: String) {
        val message = conversationsRepository.getMessageByUuid(conversationSid, messageUuid) ?: return
        if (message.sendStatus == SendStatus.SENDING.value) return
        Timber.d("retrySendTextMessage $message")

        conversationsRepository.updateMessageStatus(conversationSid, messageUuid, SendStatus.SENT_RETRY.value, 0)

        if (networkAvailabilityManager.isConnected.value) {
            try {
                val conversation = conversationsClient.getConversationsClient().getConversation(conversationSid)
                val sentMessage =
                    conversation.sendMessage {
                        this.attributes = Attributes(JSONObject(customAttributesJsonAdapter.toJson(message.attributes)))
                        this.body = message.body
                    }.toMessageDataItem(messageUuid, SendStatus.SENT.value, shouldEnrichedChatFlag = siteRepository.twoWayCommsFlags.enrichedChat)

                conversationsRepository.updateMessage(sentMessage)
            } catch (e: Exception) {
                Timber.d("retrySendTextMessage error ${e.message}")
                conversationsRepository.updateMessageStatus(conversationSid, messageUuid, SendStatus.ERROR.value, 0)
            }
        } else {
            Timber.d("retrySendTextMessage 2")
            conversationsRepository.updateMessageStatus(conversationSid, messageUuid, SendStatus.ERROR.value, 0)
        }
    }

    override suspend fun sendMediaMessage(
        conversationSid: String,
        orderNumber: String,
        fulfillmentOrderNumber: String,
        uri: String,
        inputStream: InputStream,
        fileName: String?,
        mimeType: String?,
        messageUuid: String,
        user: User,
        text: String?
    ) {

        val attributes = CustomAttributes(
            messageSentDate = ZonedDateTime.now(),
            tokenizedLdapId = user.tokenizedLdapId,
            messageSource = MessageSource.PICKER,
            messageType = DisplayType.AISLE_PHOTO,
            pickerInitials = user.firstInitialLastName(),
            fulfillmentOrderNumber = fulfillmentOrderNumber,
            storeNumber = siteRepository.siteDetails.value?.siteId,
            orderNumber = orderNumber
        )

        val message = MessageDataItem(
            "",
            conversationSid,
            author = user.userId,
            Date().time,
            text,
            -1,
            attributes,
            Direction.OUTGOING_IMAGE.value,
            SendStatus.SENDING.value,
            messageUuid,
            mediaFileName = fileName,
            mediaUploadUri = uri,
            mediaType = mimeType,
            inputStream = inputStream
        )

        conversationsRepository.addMessage(message)

        if (networkAvailabilityManager.isConnected.value) {
            try {
                val conversation = conversationsClient.getConversationsClient().getConversation(conversationSid)
                repositoryScope.launch {
                    val sentMessage = conversation.sendMessage {
                        this.attributes = Attributes(JSONObject(customAttributesJsonAdapter.toJson(attributes)))
                        this.body = text
                        addMedia(
                            inputStream,
                            mimeType ?: "",
                            fileName,
                            createMediaUploadListener(uri, messageUuid, conversationSid)
                        )
                    }.toMessageDataItem(messageUuid, SendStatus.SENDING.value, shouldEnrichedChatFlag = siteRepository.twoWayCommsFlags.enrichedChat)

                    conversationsRepository.updateMessage(sentMessage)
                }
            } catch (e: Exception) {
                Timber.d("sendMediaMessage error ${e.message}")
                conversationsRepository.updateMessageStatus(conversationSid, messageUuid, SendStatus.ERROR.value, 0)
            }
        } else {
            conversationsRepository.updateMessageStatus(conversationSid, messageUuid, SendStatus.ERROR.value, 0)
        }
    }

    private fun createMediaUploadListener(
        uri: String,
        messageUuid: String,
        conversationSid: String
    ): MediaUploadListener {
        return object : MediaUploadListener {
            override fun onStarted() {
                Timber.d("Upload started for $uri")
            }

            override fun onProgress(uploadedBytes: kotlin.Long) {
                Timber.d("Upload progress for $uri: $uploadedBytes bytes")
            }

            override fun onCompleted(mediaSid: kotlin.String) {
                Timber.d("onBindViewHolder $uri complete: $mediaSid")
                conversationsRepository.updateMessageStatus(conversationSid, messageUuid, SendStatus.SENT.value, 0)
            }

            override fun onFailed(errorInfo: com.twilio.util.ErrorInfo) {
                super.onFailed(errorInfo)
                Timber.d("onFailed $uri onFailed: $errorInfo")
                conversationsRepository.updateMessageStatus(conversationSid, messageUuid, SendStatus.ERROR.value, 0)
            }
        }
    }

    override suspend fun retrySendMediaMessage(conversationSid: String, messageUuid: String) {
        val message = conversationsRepository.getMessageByUuid(conversationSid, messageUuid) ?: return
        if (message.sendStatus == SendStatus.SENDING.value) return
        Timber.d("retrySendTextMessage $message")
        if (message.mediaUploadUri == null) {
            Timber.d("Missing mediaUploadUri in retrySendMediaMessage: $message")
            return
        }
        conversationsRepository.updateMessageStatus(conversationSid, messageUuid, SendStatus.SENT_RETRY.value, 0)

        if (networkAvailabilityManager.isConnected.value) {
            try {
                val conversation = conversationsClient.getConversationsClient().getConversation(conversationSid)
                message.inputStream?.let {
                    Timber.d("message.inputStream $message.inputStream")
                    val sentMessage =
                        conversation.sendMessage {
                            this.attributes = Attributes(JSONObject(customAttributesJsonAdapter.toJson(message.attributes)))
                            this.body = message.body
                            addMedia(
                                it,
                                message.mediaType ?: "",
                                message.mediaFileName,
                                createMediaUploadListener(message.mediaUploadUri, messageUuid, conversationSid)
                            )
                        }.toMessageDataItem(messageUuid, SendStatus.SENT.value, shouldEnrichedChatFlag = siteRepository.twoWayCommsFlags.enrichedChat)

                    conversationsRepository.updateMessage(sentMessage)
                } ?: run {
                    conversationsRepository.updateMessageStatus(conversationSid, messageUuid, SendStatus.ERROR.value, 0)
                }
            } catch (e: Exception) {
                Timber.d("retrySendMediaMessage error ${e.message}")
                conversationsRepository.updateMessageStatus(conversationSid, messageUuid, SendStatus.ERROR.value, 0)
            }
        } else {
            Timber.d("retrySendTextMessage 2")
            conversationsRepository.updateMessageStatus(conversationSid, messageUuid, SendStatus.ERROR.value, 0)
        }
    }

    override suspend fun updateMessageStatus(
        messageUuid: String,
        sendStatus: SendStatus,
        errorCode: Int,
        conversationSid: String
    ) {
        conversationsRepository.updateMessageStatus(conversationSid, messageUuid, sendStatus.value, errorCode)
    }

    override suspend fun notifyMessageRead(
        conversationSid: String,
        index: Long,
    ) {
        try {
            val messages = conversationsClient.getConversationsClient().getConversation(conversationSid)
            Timber.d("notifyMessageRead ${messages?.lastReadMessageIndex}: ($index index), $conversationSid")
            if (index > (messages.lastReadMessageIndex ?: -1)) {
                messages.advanceLastReadMessageIndex(index)
            }
        } catch (e: Exception) {
            Timber.d("notifyMessageRead error ${e.message}")
        }
    }

    override suspend fun getMediaContentTemporaryUrl(
        index: Long,
        conversationSid: String,
    ): String {
        return try {
            val messages = conversationsClient.getConversationsClient().getConversation(conversationSid)
            val message = messages.getMessageByIndex(index)

            message.firstMedia?.getTemporaryContentUrl()!!
        } catch (e: Exception) {
            Timber.d("getMediaContentTemporaryUrl error ${e.message}")
            ""
        }
    }
}
