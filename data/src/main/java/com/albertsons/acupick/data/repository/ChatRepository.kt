package com.albertsons.acupick.data.repository

import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.alsoOnFailure
import com.albertsons.acupick.data.model.alsoOnSuccess
import com.albertsons.acupick.data.model.chat.DisplayType
import com.albertsons.acupick.data.model.chat.InternalMessageSubType
import com.albertsons.acupick.data.model.chat.MessageDataItem
import com.albertsons.acupick.data.model.chat.MessageSource
import com.albertsons.acupick.data.model.chat.ParticipantDataItem
import com.albertsons.acupick.data.model.chat.asMessageDataItems
import com.albertsons.acupick.data.model.chat.asParticipantDataItem
import com.albertsons.acupick.data.model.chat.toMessageDataItem
import com.albertsons.acupick.data.model.request.ChatErrorData
import com.albertsons.acupick.data.model.request.NetworkCalls
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProviderImpl
import com.albertsons.acupick.infrastructure.utils.getMessageCount
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.squareup.moshi.Moshi
import com.twilio.conversations.Conversation
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.Message
import com.twilio.conversations.extensions.ConversationListener
import com.twilio.conversations.extensions.ConversationsClientListener
import com.twilio.conversations.extensions.getLastMessages
import com.twilio.util.TwilioException
import com.twilio.conversations.extensions.getConversation
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.twilio.conversations.extensions.removeParticipantByIdentity
import com.twilio.conversations.extensions.waitForSynchronization
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val PARTICIPANTS_CHECK = 2
interface ConversationsRepository {
    val newMessageNotification: StateFlow<MessageDataItem?>
    val pickerJoinedNotification: StateFlow<MessageDataItem?>
    val pickerLeftNotification: StateFlow<MessageDataItem?>
    val showUnreadMessageDot: StateFlow<HashMap<String, Boolean>>
    val showChatButton: StateFlow<Boolean>
    val onPickerAdded: StateFlow<String?>
    val onTyping: StateFlow<HashMap<String, Boolean>>
    val initializationError: StateFlow<Pair<Boolean, ApiResult.Failure>?>
    val errorData: StateFlow<ApiResult.Failure?>
    val messages: StateFlow<HashMap<String, List<MessageDataItem>?>?>
    fun sendLog(errorCode: ApiResult.Failure, showDialog: Boolean = true)
    fun getMessageByUuid(conversationSid: String, id: String): MessageDataItem?
    fun updateMessageStatus(conversationSid: String, messageUuid: String, sendStatus: Int, errorCode: Int)
    fun updateMessage(message: MessageDataItem)
    fun addMessage(message: MessageDataItem)
    fun fetchConversation(conversationSid: String)
    fun clear()
    fun subscribeToConversationsClientEvents()
    fun unsubscribeFromConversationsClientEvents()
    fun shutDownChat()
    suspend fun initializeChat(userId: String, showDialog: Boolean = true)
    suspend fun fetchParticipants(conversationSid: String, waitForSync: Boolean = true): List<ParticipantDataItem>?
    suspend fun checkIfOnlyPickerPicking(conversationSid: Map<String, Boolean>, waitForSync: Boolean = true): Boolean
    suspend fun insertOrUpdateConversation(conversationSid: String)
    suspend fun removeParticipant(conversationSid: String, userId: String)
    suspend fun getConversationId(orderId: String): String
    suspend fun getOrderIdByConversationSid(conversationSid: String): String
    suspend fun getLastMessageTimestamp(conversationSid: String): String
}

interface ConversationsClientWrapper {
    val isClientCreated: Boolean
    suspend fun getConversationsClient(): ConversationsClient
    suspend fun create(userId: String): ApiResult<String>
    suspend fun shutdown()
}

class ConversationsRepositoryImpl(
    private val conversationsClientWrapper: ConversationsClientWrapper,
    private val dispatchers: DispatcherProvider = DispatcherProviderImpl(),
    private val siteRepository: SiteRepository,
    private val moshi: Moshi,
) : ConversationsRepository {
    private val mutex = Mutex()
    private val currentConversationSid: MutableSet<String> = mutableSetOf()
    private var retryCount = 0
    private val repositoryScope = CoroutineScope(dispatchers.IO + SupervisorJob())
    private val _messages: MutableStateFlow<HashMap<String, List<MessageDataItem>?>?> = MutableStateFlow(null)
    private val _newMessageNotification: MutableStateFlow<MessageDataItem?> = MutableStateFlow(null)
    private val _pickerJoinedNotification: MutableStateFlow<MessageDataItem?> = MutableStateFlow(null)
    private val _pickerLeftNotification: MutableStateFlow<MessageDataItem?> = MutableStateFlow(null)
    private val _showUnreadMessageDot: MutableStateFlow<HashMap<String, Boolean>> = MutableStateFlow(hashMapOf())
    private val _showChatButton: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _onTyping: MutableStateFlow<HashMap<String, Boolean>> = MutableStateFlow(hashMapOf())
    private val _initializationError: MutableStateFlow<Pair<Boolean, ApiResult.Failure>?> = MutableStateFlow(null)
    private val _errorData: MutableStateFlow<ApiResult.Failure?> = MutableStateFlow(null)
    private val _onPickerAdded: MutableStateFlow<String?> = MutableStateFlow(null)

    override val messages: StateFlow<HashMap<String, List<MessageDataItem>?>?>
        get() = _messages

    override val newMessageNotification: StateFlow<MessageDataItem?>
        get() = _newMessageNotification
    override val pickerJoinedNotification: StateFlow<MessageDataItem?>
        get() = _pickerJoinedNotification
    override val pickerLeftNotification: StateFlow<MessageDataItem?>
        get() = _pickerLeftNotification
    override val showUnreadMessageDot: StateFlow<HashMap<String, Boolean>>
        get() = _showUnreadMessageDot

    override val showChatButton: StateFlow<Boolean>
        get() = _showChatButton

    override val onTyping: StateFlow<HashMap<String, Boolean>>
        get() = _onTyping

    override val initializationError: StateFlow<Pair<Boolean, ApiResult.Failure>?>
        get() = _initializationError.asStateFlow()

    override val errorData: StateFlow<ApiResult.Failure?>
        get() = _errorData.asStateFlow()

    override val onPickerAdded: StateFlow<String?>
        get() = _onPickerAdded.asStateFlow()

    init {
        launch {
            showChatButton.collectLatest {
                if (it) {
                    val errorData = ChatErrorData(conversationSids = listOf(currentConversationSid.joinToString(", "))).toJsonString(moshi)
                    sendLog(ApiResult.Failure.GeneralFailure(errorData, networkCallName = NetworkCalls.CHAT_BUTTON_DISPLAYED.value), false)
                }
            }
        }
    }

    private val clientListener = ConversationsClientListener(
        onConversationDeleted = { conversation ->
            Timber.d("Conversation onConversationDeleted $conversation")
            if (currentConversationSid.isNotNullOrEmpty() && currentConversationSid.contains(conversation.sid)) {
                Timber.d("Conversation clear $conversation")
                clear()
            }
        },
        onConversationUpdated = { conversation, _ ->
            Timber.d("Conversation onConversationUpdated $conversation")
            if (currentConversationSid.isNotNullOrEmpty() && currentConversationSid.contains(conversation.sid)) {
                setUnreadMessageCount(conversation)
            }
        },
        onConversationAdded = { conversation ->
            launch {
                Timber.d("Conversation Conversation $conversation")
                currentConversationSid.add(conversation.sid)
                insertOrUpdateConversation(conversation.sid)
                delay(5000)
                _onPickerAdded.value = conversation.sid
            }
            val errorData = ChatErrorData(conversationSids = listOf(conversation.sid)).toJsonString(moshi)
            sendLog(ApiResult.Failure.GeneralFailure(errorData, networkCallName = NetworkCalls.NEW_PICKER_ADDED.value), false)
        },
        onConversationSynchronizationChange = { conversation ->
            Timber.d("Conversation onConversationSynchronizationChange $conversation")
        },
    )

    private val conversationListener = ConversationListener(
        onTypingStarted = { conversation, participant ->
            _onTyping.value = HashMap(_onTyping.value).apply { put(conversation.sid, true) }
            Timber.d("${participant.identity} started typing in ${conversation.friendlyName}")
        },
        onTypingEnded = { conversation, participant ->
            _onTyping.value = HashMap(_onTyping.value).apply { put(conversation.sid, false) }
            Timber.d("${participant.identity} stopped typing in ${conversation.friendlyName}")
        },
        onParticipantAdded = { participant ->
            Timber.d("${participant.identity} added in ${participant.conversation.sid}")
        },
        onParticipantUpdated = { participant, reason ->
            Timber.d("${participant.identity} updated in ${participant.conversation.sid}, reason: $reason")
        },
        onParticipantDeleted = { participant ->
            Timber.d("${participant.identity} deleted in ${participant.conversation.sid}")
        },
        onMessageDeleted = { message ->
        },
        onMessageUpdated = { message, reason ->
            Timber.d("webhook onMessageUpdated $message $reason")
            if (currentConversationSid.isNotNullOrEmpty() && currentConversationSid.contains(message.conversationSid)) {
                repositoryScope.launch {
                    val messageData = message.toMessageDataItem(shouldEnrichedChatFlag = siteRepository.twoWayCommsFlags.enrichedChat)
                    if (reason == Message.UpdateReason.ATTRIBUTES) {
                        updateMessageBySid(messageData)
                    }
                }
            }
        },
        onMessageAdded = { message ->
            Timber.d("webhook onMessageAdded $message ${currentConversationSid.size}")

            if (currentConversationSid.isNotNullOrEmpty() && currentConversationSid.contains(message.conversationSid)) {
                repositoryScope.launch {
                    val messageData = message.toMessageDataItem(shouldEnrichedChatFlag = siteRepository.twoWayCommsFlags.enrichedChat)
                    if (messageData.author != conversationsClientWrapper.getConversationsClient().myIdentity) {
                        addMessage(message)
                        if (messageData.attributes?.messageSource != MessageSource.PICKER && messageData.attributes?.messageType == DisplayType.TEXT) {
                            _newMessageNotification.emit(messageData)
                        }
                        if (messageData.attributes?.messageType == DisplayType.INTERNAL && messageData.attributes?.internalMessageSubType == InternalMessageSubType.JOINED_CHAT) {
                            _pickerJoinedNotification.emit(messageData)
                        } else if (messageData.attributes?.messageType == DisplayType.INTERNAL && messageData.attributes?.internalMessageSubType == InternalMessageSubType.LEFT_CHAT) {
                            _pickerLeftNotification.emit(messageData)
                        }
                    }
                }
            }
        }
    )

    private fun setUnreadMessageCount(conversation: Conversation) {
        launch {
            try {
                val readIndex = conversation.lastReadMessageIndex ?: 0L
                val lastMessageIndex = conversation.lastMessageIndex ?: 0L
                val count = (lastMessageIndex - readIndex).coerceAtLeast(0L)
                if (count > 0L) {
                    val messageCount = conversation.getMessageCount()
                    val messages = conversation.getLastMessages(count.toInt() ?: 0).map { it.toMessageDataItem(shouldEnrichedChatFlag = siteRepository.twoWayCommsFlags.enrichedChat) }
                    val unreadMessages = messages.count {
                        it.attributes?.messageType != DisplayType.FORMATTED && it.attributes?.messageType != DisplayType.GREETING &&
                            it.attributes?.messageSource != MessageSource.PICKER && it.attributes?.messageSource != MessageSource.OSCC
                    }
                    Timber.d("setUnreadMessageCount $count $unreadMessages, $messages")
                    _showUnreadMessageDot.value = HashMap(_showUnreadMessageDot.value).apply {
                        put(conversation.sid, unreadMessages > 0L)
                    }
                } else {
                    _showUnreadMessageDot.value = HashMap(_showUnreadMessageDot.value).apply {
                        put(conversation.sid, false)
                    }
                }
            } catch (e: Exception) {
                Timber.d("setUnreadMessageCount error ${e.message}")
            }
        }
    }

    private fun launch(block: suspend CoroutineScope.() -> Unit) = repositoryScope.launch(
        context = CoroutineExceptionHandler { _, e -> Timber.e(e, "Coroutine failed ${e.localizedMessage}") },
        block = block
    )

    override suspend fun initializeChat(userId: String, showDialog: Boolean) {
        Timber.d("initializeChat")
        if (siteRepository.twoWayCommsFlags.chatBeta == true) {
            retryCount++
            conversationsClientWrapper.create(userId)
                .alsoOnSuccess {
                    subscribeToConversationsClientEvents()
                    val errorData = ChatErrorData(errorMessage = "Retry count: $retryCount, user: $userId").toJsonString(moshi)
                    sendLog(ApiResult.Failure.GeneralFailure(errorData, networkCallName = NetworkCalls.SUCCESS_SUBSCRIPTION_TO_CHAT.value), false)
                    retryCount = 0
                    _errorData.value = null
                }
                .alsoOnFailure {
                    when (it) {
                        is ApiResult.Failure -> {
                            _errorData.value = it
                            sendLog(it, showDialog)
                        }
                        else -> {}
                    }
                }
        }
    }

    override fun sendLog(errorCode: ApiResult.Failure, showDialog: Boolean) {
        _initializationError.value = Pair(showDialog, errorCode)
    }

    // override suspend fun getStatus(): List<User> {
    //     val users = conversationsClientWrapper.getConversationsClient().subscribedUsers
    //     val self = conversationsClientWrapper.getConversationsClient().getAndSubscribeUser("user03") {
    //         Timber.d("chatstatus ${it.isOnline}")
    //     }
    //     // Timber.d("chatstatus ${self}")
    //     return users ?: emptyList()
    // }

    override fun getMessageByUuid(conversationSid: String, id: String): MessageDataItem? {
        return _messages.value?.get(conversationSid)?.find { it.uuid == id }
    }

    override fun clear() {
        // this logic will not have impact on batch scenario.
        // current flow removed all pickers at once hence no need to change logic.
        // revisit to handle for specific converdation in future if needed.
        launch {
            _showChatButton.value = false
            _showUnreadMessageDot.value = hashMapOf()
            _messages.value = null
            currentConversationSid.clear()
            _newMessageNotification.value = null
            _pickerLeftNotification.value = null
            _pickerLeftNotification.value = null
            _onTyping.value.clear()
        }
    }

    private fun fetchMessages(conversation: Conversation, fetch: suspend Conversation.() -> List<Message>) = flow<List<MessageDataItem>> {
        Timber.d("fetchMessages: $conversation")
        try {
            val messages = conversation.fetch()
            Timber.d("fetchMessages: $messages.to")

            val newlist = ArrayList(_messages.value?.get(conversation.sid).orEmpty()).apply {
                clear()
                addAll(messages.asMessageDataItems(siteRepository.twoWayCommsFlags.enrichedChat))
            }
            _messages.value = _messages.value?.let {
                HashMap(it).apply {
                    put(conversation.sid, newlist)
                }
            } ?: hashMapOf(conversation.sid to newlist)
        } catch (e: TwilioException) {
            val errorData = ChatErrorData(conversationSids = listOf(conversation.sid), errorMessage = e.localizedMessage.orEmpty()).toJsonString(moshi)
            sendLog(
                ApiResult.Failure.GeneralFailure(errorData, networkCallName = NetworkCalls.FETCH_MESSAGES_2_FAILURE.value),
                false
            )
            Timber.d("fetchMessages error: ${e.errorInfo.message}")
        }
    }

    override suspend fun checkIfOnlyPickerPicking(conversations: Map<String, Boolean>, waitForSync: Boolean): Boolean {
        return if (siteRepository.twoWayCommsFlags.chatBeta == true) {
            conversations.any {
                val participants = fetchParticipants(it.key, waitForSync)
                participants.isNotNullOrEmpty() && (participants?.size ?: 0) <= PARTICIPANTS_CHECK
            }
        } else {
            false
        }
    }

    override suspend fun fetchParticipants(conversationSid: String, waitForSync: Boolean): List<ParticipantDataItem>? {
        return if (siteRepository.twoWayCommsFlags.chatBeta == true) {
            try {
                if (conversationsClientWrapper.isClientCreated) {
                    Timber.d("fetchParticipants $conversationSid")
                    val conversation = conversationsClientWrapper.getConversationsClient().getConversation(conversationSid)
                    if (waitForSync) {
                        conversation.waitForSynchronization()
                    }
                    conversation.participantsList.map { it.asParticipantDataItem() }
                } else {
                    emptyList()
                }
            } catch (e: TwilioException) {
                Timber.d("fetchParticipants error: ${e.errorInfo.message}")
                null
            }
        } else {
            emptyList()
        }
    }

    override fun subscribeToConversationsClientEvents() {
        launch {
            Timber.d("Client listener added")
            conversationsClientWrapper.getConversationsClient().addListener(clientListener)
        }
    }

    override fun unsubscribeFromConversationsClientEvents() {
        launch {
            Timber.d("Client listener removed")
            conversationsClientWrapper.getConversationsClient().removeListener(clientListener)
        }
    }

    /**
     * Source of truth for the active chats, returns a conversation id for a particular order number,
     * Empty id represents no active chats
     */
    override suspend fun getConversationId(orderId: String): String {
        Timber.d("getConversationId start $orderId ${siteRepository.twoWayCommsFlags.chatBeta == true}")
        return if (siteRepository.twoWayCommsFlags.chatBeta == true) {
            try {
                val conversation = conversationsClientWrapper.getConversationsClient().myConversations?.firstOrNull {
                    Timber.d("getConversationId ${it?.sid}, ${it.uniqueName}, ${it.state}")
                    it.uniqueName == orderId
                }
                conversation?.sid?.let {
                    currentConversationSid.add(it)
                }
                val errorData = ChatErrorData(orderNumbers = listOf(orderId), conversationSids = listOf(conversation?.sid ?: "")).toJsonString(moshi)
                sendLog(
                    ApiResult.Failure.GeneralFailure(errorData, networkCallName = NetworkCalls.GET_CUSTOMERID_SUCCESS.value),
                    false
                )
                conversation?.sid ?: ""
            } catch (e: Exception) {
                if (conversationsClientWrapper.isClientCreated) {
                    val errorData = ChatErrorData(orderNumbers = listOf(orderId), errorMessage = e.localizedMessage.orEmpty()).toJsonString(moshi)
                    sendLog(ApiResult.Failure.GeneralFailure(errorData, networkCallName = NetworkCalls.GET_CUSTOMERID_FAILURE.value), false)
                    Timber.d("getConversationId ${e.message}")
                    ""
                } else {
                    ""
                }
            }
        } else {
            ""
        }
    }

    override suspend fun getOrderIdByConversationSid(conversationSid: String): String {
        Timber.d("getOrderIdByConversationSid start $conversationSid ${siteRepository.twoWayCommsFlags.chatBeta == true}")
        return if (siteRepository.twoWayCommsFlags.chatBeta == true) {
            try {
                val conversation = conversationsClientWrapper.getConversationsClient().getConversation(conversationSid)
                conversation.uniqueName
            } catch (e: Exception) {
                Timber.d("getOrderIdByConversationSid ${e.message}")
                ""
            }
        } else {
            ""
        }
    }

    override suspend fun insertOrUpdateConversation(conversationSid: String) {
        if (siteRepository.twoWayCommsFlags.chatBeta == true) {
            try {
                val conversation = conversationsClientWrapper.getConversationsClient().getConversation(conversationSid)
                Timber.d("repo updating dataItem in db... ${conversation.uniqueName} ${conversation.sid}")
                conversation.addListener(conversationListener)
                _showChatButton.value = true
                _newMessageNotification.value = null
                setUnreadMessageCount(conversation)
                fetchConversation(conversationSid)
                currentConversationSid.add(conversationSid)
            } catch (e: Exception) {
                val errorData = ChatErrorData(conversationSids = listOf(conversationSid), errorMessage = e.localizedMessage.orEmpty()).toJsonString(moshi)
                sendLog(ApiResult.Failure.GeneralFailure(errorData, networkCallName = NetworkCalls.INSERT_AND_UPDATE_FAILURE.value), false)
                Timber.d("insertOrUpdateConversation ${e.message}")
            }
        }
    }

    override fun fetchConversation(conversationSid: String) {
        launch {
            mutex.withLock {
                fetchMessages(conversationSid)
            }
        }
    }

    override suspend fun removeParticipant(conversationSid: String, userId: String) {
        Timber.d("removeParticipant")
        try {
            if (conversationSid.isNotNullOrEmpty()) {
                val conversation = conversationsClientWrapper.getConversationsClient().getConversation(conversationSid)
                conversation.removeParticipantByIdentity(userId)
                conversation.removeListener(conversationListener)
            }
            _showChatButton.value = false
        } catch (e: Exception) {
            val errorData = ChatErrorData(conversationSids = listOf(conversationSid), errorMessage = e.localizedMessage.orEmpty()).toJsonString(moshi)
            sendLog(ApiResult.Failure.GeneralFailure(errorData, networkCallName = NetworkCalls.PICKER_REMOVED_FAILED.value), false)
            _showChatButton.value = false
            Timber.d("removeParticipant failed")
        }
        _onPickerAdded.value = null
        clear()
    }

    private suspend fun fetchMessages(conversationSid: String) {
        Timber.d("fetchMessages:")
        try {
            val conversation = conversationsClientWrapper
                .getConversationsClient()
                .getConversation(conversationSid)
            Timber.d("fetchMessages count: ${conversation.sid} -- ${conversation.getMessageCount().toInt()}")

            fetchMessages(conversation) { getLastMessages(conversation.getMessageCount().toInt()) }.collect()
        } catch (e: Exception) {
            val errorData = ChatErrorData(conversationSids = listOf(conversationSid), errorMessage = e.localizedMessage.orEmpty()).toJsonString(moshi)
            sendLog(
                ApiResult.Failure.GeneralFailure(errorData, networkCallName = NetworkCalls.FETCH_MESSAGES_1_FAILURE.value),
                false
            )
            Timber.d("fetchMessages error: $e")
        }
    }

    override fun updateMessageStatus(conversationSid: String, messageUuid: String, sendStatus: Int, errorCode: Int) {
        launch {
            mutex.withLock {
                Timber.d("updateMessageStatus: $messageUuid")
                val messages = _messages.value?.get(conversationSid).orEmpty()
                val existingMessage = messages.find { it.uuid == messageUuid }
                if (existingMessage != null) {
                    Timber.d("existingMessage}")
                    val newlst = ArrayList(messages).map { if (it.uuid == messageUuid) it.copy(sendStatus = sendStatus) else it }
                    _messages.value = HashMap(_messages.value.orEmpty()).apply {
                        put(conversationSid, newlst)
                    }
                } else {
                    Timber.d("message not found for update")
                }
            }
        }
    }

    override fun updateMessage(message: MessageDataItem) {
        launch {
            mutex.withLock {
                Timber.d("updateMessage sent: $message")
                val messages = _messages.value?.get(message.conversationSid).orEmpty()
                val existingMessage = messages.find { it.uuid == message.uuid }
                if (existingMessage != null) {
                    val newlst = messages.map {
                        if (it.uuid == message.uuid) message else it
                    }
                    _messages.value = HashMap(_messages.value.orEmpty()).apply {
                        put(message.conversationSid, newlst)
                    }
                } else {
                    Timber.d("updateMessage failed}")
                }
            }
        }
    }

    private fun updateMessageBySid(message: MessageDataItem) {
        launch {
            mutex.withLock {
                Timber.d("updateMessageBySid sent: $message")
                val messages = _messages.value?.get(message.conversationSid).orEmpty()
                val newlst = messages.map {
                    if (it.sid == message.sid) message else it
                }
                _messages.value = HashMap(_messages.value.orEmpty()).apply {
                    put(message.conversationSid, newlst)
                }
            }
        }
    }

    override fun shutDownChat() {
        launch {
            if (conversationsClientWrapper.isClientCreated && siteRepository.twoWayCommsFlags.chatBeta == true) {
                Timber.d("shutDownChat")
                unsubscribeFromConversationsClientEvents()
                clear()
                runCatching { conversationsClientWrapper.shutdown() }
            }
        }
    }

    override fun addMessage(message: MessageDataItem) {
        launch {
            mutex.withLock {
                Timber.d("Message added: $message")
                val messages = _messages.value?.get(message.conversationSid).orEmpty()
                val newlst = ArrayList(messages).apply {
                    add(message)
                }
                _messages.value = HashMap(_messages.value.orEmpty()).apply {
                    put(message.conversationSid, newlst)
                }
            }
        }
    }

    private fun addMessage(message: Message) {
        launch {
            mutex.withLock {
                val messages = _messages.value?.get(message.conversationSid).orEmpty()
                val existingMessage = messages.find { it.sid == message.sid }
                if (existingMessage != null) {
                    Timber.d("existingMessage do nothing} ")
                } else {
                    addMessage(message.toMessageDataItem(shouldEnrichedChatFlag = siteRepository.twoWayCommsFlags.enrichedChat))
                }
            }
        }
    }

    override suspend fun getLastMessageTimestamp(conversationId: String): String {
        return try {
            val conversation = conversationsClientWrapper.getConversationsClient().getConversation(conversationId)
            val lastMessage = conversation.getLastMessages(1).firstOrNull()
            lastMessage?.dateCreated.orEmpty()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get last message timestamp for conversation: $conversationId")
            ""
        }
    }
}
