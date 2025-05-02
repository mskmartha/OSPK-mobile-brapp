package com.albertsons.acupick.ui.chat

import android.app.Application
import android.text.InputFilter
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.EventCategory
import com.albertsons.acupick.EventKey
import com.albertsons.acupick.EventLabel
import com.albertsons.acupick.FirebaseAnalyticsInterface
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.OrderChatDetail
import com.albertsons.acupick.data.model.chat.MessageDataItem
import com.albertsons.acupick.data.model.chat.SendStatus
import com.albertsons.acupick.data.model.fullContactName
import com.albertsons.acupick.data.model.request.NetworkCalls
import com.albertsons.acupick.data.model.request.SubstitutionRejectedReason
import com.albertsons.acupick.data.model.request.UndoPickLocalDto
import com.albertsons.acupick.data.model.request.UndoPickRequestDto
import com.albertsons.acupick.data.model.request.UndoShortRequestDto
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.SubApprovalStatus
import com.albertsons.acupick.data.model.response.SwapItem
import com.albertsons.acupick.data.model.response.fullContactName
import com.albertsons.acupick.data.model.response.isBatchOrder
import com.albertsons.acupick.data.model.response.toSwapItem
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.MessagesRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.PushNotificationsRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.TokenizedLdapRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.picklistitems.REMOVE_SUBSTITUTION_DIALOG_TAG
import com.albertsons.acupick.ui.picklistitems.getRemoveSubstitutionDialogArgDataAndTag
import com.albertsons.acupick.ui.substitute.SubstituteParams
import com.albertsons.acupick.ui.substitute.SubstitutionPath
import com.albertsons.acupick.ui.substitute.SwapSubstitutionReason
import com.albertsons.acupick.ui.util.DrawableIdHelper
import com.albertsons.acupick.ui.util.EventAction
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.getOrZero
import com.albertsons.acupick.ui.util.orFalse
import com.hadilq.liveevent.LiveEvent
import com.twilio.util.TwilioException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import timber.log.Timber
import java.io.InputStream
import java.util.UUID

class ChatViewModel(val conversationSid: String, val orderNumber: String, val fulfillmentOrderId: String, val app: Application) : BaseViewModel(app) {
    val typedMessage = MutableLiveData<String>()
    private val messagesRepository: MessagesRepository by inject()
    private val conversationsRepository: ConversationsRepository by inject()
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    val activityViewModel: MainActivityViewModel by inject()
    private val dispatcherProvider: DispatcherProvider by inject()
    private val pickRepository: PickRepository by inject()
    private val userRepository: UserRepository by inject()
    private val siteRepo: SiteRepository by inject()
    private val tokenizedLdapRepository: TokenizedLdapRepository by inject()
    val isPhotoPreviewMode = MutableLiveData<Boolean>()
    val dismissChatNotication = MutableLiveData<List<String>>()
    val firebaseAnalytics: FirebaseAnalyticsInterface by inject()
    private val pushNotificationsRepository: PushNotificationsRepository by inject()

    val hideNetworkAlert = MutableLiveData(true)
    val isVoiceToText = siteRepo.twoWayCommsFlags.voiceToText
    val isMasterOrderView = siteRepo.twoWayCommsFlags.masterOrderView
    private val orderChatDetail: OrderChatDetail? = pickRepository.pickList.value?.orderChatDetails?.firstOrNull { it.customerOrderNumber == orderNumber }
    val customerName = orderChatDetail?.customerFirstName ?: pickRepository.pickList.value?.contactFirstName.orEmpty()
    private val customerFullName = orderChatDetail?.fullContactName() ?: pickRepository.pickList.value?.fullContactName().orEmpty()
    val isTyping = conversationsRepository.onTyping.asLiveData().map { it[conversationSid] == true }

    init {
        clearToolbarEvent.postValue(Unit)
        changeToolbarTitleEvent.postValue(customerFullName)
        pushNotificationsRepository.setInProgressChatOrderId(orderNumber)
        conversationsRepository.fetchConversation(conversationSid)
        val pushIds = pushNotificationsRepository.getChatPushIds()
        if (pushIds.isNotNullOrEmpty()) {
            dismissChatNotication.value = pushIds
        }
        viewModelScope.launch {
            networkAvailabilityManager.isConnected.collect { connected ->
                hideNetworkAlert.postValue(connected)
                val swapIcon = when (connected) {
                    true -> R.drawable.ic_swap
                    false -> R.drawable.swap
                }
                changeToolbarRightSecondExtraImageEvent.postValue(DrawableIdHelper.Id(swapIcon))
            }
        }

        viewModelScope.launch(dispatcherProvider.IO) {
            toolbarRightSecondImageEvent.asFlow().collect {
                // Quick task screen only will be open if network will be available for swap sub flow
                if (hideNetworkAlert.value == true) {
                    onSwapIconClicked()
                }
            }
        }

        registerCloseAction(REMOVE_SUBSTITUTION_DIALOG_TAG) {
            closeActionFactory(
                positiveWithData = { data ->
                    data?.let {
                        undoPicksSwapSubstitution(data.first, data.second)
                    }
                },
                negative = {},
                dismiss = {},
            )
        }
    }

    val messageItems = conversationsRepository.messages
        .onEach { repositoryResult ->
            Timber.d("messageItems: $repositoryResult")
        }
        .asLiveData(viewModelScope.coroutineContext)
        .map { messages ->
            val userToken = userRepository.user.value?.tokenizedLdapId.orEmpty()
            val pickerLdapDetails = tokenizedLdapRepository.pickersLdapDetails.value.orEmpty()
            messages?.get(conversationSid)?.asMessageListViewItems(app, userToken, pickerLdapDetails)
        }

    val showItemPhotoDialog: LiveData<String> = LiveEvent()

    fun retrySendMessage(message: MessageListViewItem) {
        firebaseAnalytics.logEvent(EventCategory.CHAT, EventAction.CLICK, EventLabel.CHAT_RETRY_BTN)
        viewModelScope.launch {
            Timber.d("message")
            try {
                messagesRepository.retrySendTextMessage(conversationSid, message.uuid)
                Timber.d("messageUuid sent: ${message.uuid}")
            } catch (e: TwilioException) {
                Timber.d("messageUuid Text message send error: ${e.errorInfo.status}:${e.errorInfo.code} ${e.errorInfo.message}")
                conversationsRepository.updateMessageStatus(conversationSid, message.uuid, SendStatus.ERROR.value, e.errorInfo.code)
            }
        }
    }

    fun retrySendMediaMessage(message: MessageListViewItem) {
        viewModelScope.launch {
            Timber.d("retrySendMediaMessage")
            try {
                messagesRepository.retrySendMediaMessage(conversationSid, message.uuid)
                Timber.d("messageUuid sent: ${message.uuid}")
            } catch (e: TwilioException) {
                Timber.d("messageUuid Text message send error: ${e.errorInfo.status}:${e.errorInfo.code} ${e.errorInfo.message}")
                conversationsRepository.updateMessageStatus(conversationSid, message.uuid, SendStatus.ERROR.value, e.errorInfo.code)
            }
            firebaseAnalytics.logEvent(EventCategory.CHAT, EventAction.CLICK, EventLabel.CHAT_RETRY_BTN)
        }
    }

    private fun undoPicksSwapSubstitution(itemId: String, messageSid: String) {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { undoPicksSwapSubstitution(itemId, messageSid) }
            } else {
                val item = getItem(itemId)
                item?.toSwapItem()?.substitutedWith?.let { substitutionList ->
                    if (substitutionList.isEmpty()) return@launch
                    val substitutionRemovedQty = substitutionList.sumBy { it.qty?.toInt().getOrZero() }
                    val requests =
                        substitutionList.map {
                            UndoPickLocalDto(
                                containerId = it.containerId,
                                undoPickRequestDto = UndoPickRequestDto(
                                    actId = pickRepository.pickList.first()?.actId ?: 0,
                                    iaId = item.id,
                                    netWeight = it.netWeight,
                                    pickedUpcId = it.upcId,
                                    qty = it.qty,
                                    rejectionReason = SubstitutionRejectedReason.SWAP,
                                    messageSid = messageSid
                                )
                            )
                        }
                    val results =
                        isBlockingUi.wrap {
                            pickRepository.undoPicks(requests)
                        }
                    if (results is ApiResult.Failure) {
                        Timber.d("undoPicksSwapSubstitution failed")
                    } else {
                        navigateToSubstitution(item.toSwapItem(), substitutionRemovedQty = substitutionRemovedQty)
                    }
                }
            }
        }
    }

    fun onSwapSubstitutionClicked(subItem: MessageListViewItem) {
        val subItemId = subItem.customAttributes?.orderedItem?.orderedItemId ?: ""
        if (canPerformSubstitutionOrOosSwap(subItemId)) {
            removeSubstitution(subItem)
        } else if (siteRepo.twoWayCommsFlags.masterOrderView == true) {
            validateOtherPickerState(subItem, false)
        } else {
            showCantSwapError()
        }
    }

    fun handleCameraAccessError(cameraPermissionFeatureDisabled: NetworkCalls) {
        conversationsRepository.sendLog(ApiResult.Failure.GeneralFailure("unable to access camera", networkCallName = cameraPermissionFeatureDisabled.value), false)
        showCameraPermissionIssue()
    }

    private fun removeSubstitution(subItem: MessageListViewItem) {
        subItem.customAttributes?.orderedItem?.orderedItemId?.let { itemId ->
            getItem(itemId)?.let { substitutedItemList ->
                inlineDialogEvent.postValue(getRemoveSubstitutionDialogArgDataAndTag(substitutedItemList.toSwapItem().substitutedWith.orEmpty(), Pair(itemId, subItem.sid)))
            }
        }
    }

    private fun validateOtherPickerState(substitutionItem: MessageListViewItem, isOutOfStock: Boolean) {
        val subItemId = substitutionItem.customAttributes?.orderedItem?.orderedItemId ?: ""
        val customerOrderNumber = orderNumber.takeIf { pickRepository.pickList.value?.isBatchOrder().orFalse() }
        viewModelScope.launch(dispatcherProvider.IO) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> {
                    val actId = pickRepository.pickList.first()?.actId.toString()
                    val result = isBlockingUi.wrap { pickRepository.isOtherPickerActive(id = actId, itemId = subItemId, orderNumber = customerOrderNumber) }
                    when (result) {
                        is ApiResult.Success -> {
                            val masterPicklist = result.data.masterView?.firstOrNull()
                            val swapItem = masterPicklist?.itemActivities?.firstOrNull()?.toSwapItem(isMasterOrderView = true, isOutOfStock = isOutOfStock)
                            navigateToSubstitution(swapItem, substitutionItem.sid, itemId = subItemId)
                        }

                        is ApiResult.Failure -> {
                            if (result is ApiResult.Failure.Server) {
                                when (result.error?.errorCode?.resolvedType) {
                                    ServerErrorCode.PICKING_IN_PROGRESS -> showSwapSubstitutioErrorDialog(substitutionItem.senderFirstName)
                                    else -> handleApiError(errorType = result)
                                }
                            } else {
                                handleApiError(errorType = result)
                            }
                        }
                    }.exhaustive
                }

                false -> {
                    networkAvailabilityManager.triggerOfflineError { validateOtherPickerState(substitutionItem, isOutOfStock) }
                }
            }
        }
    }

    private fun showSwapSubstitutioErrorDialog(senderName: String) {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Format(R.string.swap_substitution_error_dialog_title, senderName),
                    body = StringIdHelper.Id(R.string.swap_substitution_error_dialog_description),
                    positiveButtonText = StringIdHelper.Id(R.string.ok_cta),
                    cancelOnTouchOutside = true
                ),
                tag = SWAP_SUBSTITUTION_ERROR_DIALOG_TAG
            )
        )
    }

    private fun showCantSwapError() {
        val args = CustomDialogArgData(
            title = StringIdHelper.Id(R.string.chat_other_shoppers_item),
            body = StringIdHelper.Id(R.string.chat_other_shoppers_item_body),
            positiveButtonText = StringIdHelper.Id(R.string.ok_cta),
            cancelOnTouchOutside = true
        )

        val dialog = CustomDialogArgDataAndTag(
            data = args,
            tag = CANT_SWAP_DIALOG_TAG
        )
        inlineDialogEvent.postValue(dialog)
    }

    private fun showCameraPermissionIssue() {
        val args = CustomDialogArgData(
            title = StringIdHelper.Id(R.string.chat_camera_error),
            body = StringIdHelper.Id(R.string.chat_camera_feature_disabled),
            positiveButtonText = StringIdHelper.Id(R.string.ok_cta),
            cancelOnTouchOutside = true
        )

        val dialog = CustomDialogArgDataAndTag(
            data = args,
            tag = CHAT_CAMERA_ACCESS_DIALOG_TAG
        )
        inlineDialogEvent.postValue(dialog)
    }

    fun onOosSwapClicked(oosItem: MessageListViewItem) {
        val oosItemId = oosItem.customAttributes?.orderedItem?.orderedItemId ?: ""
        if (canPerformSubstitutionOrOosSwap(oosItemId)) {
            undoShorts(oosItemId, oosItem.sid)
        } else if (siteRepo.twoWayCommsFlags.masterOrderView == true) {
            validateOtherPickerState(oosItem, true)
        } else {
            showCantSwapError()
        }
    }

    private fun canPerformSubstitutionOrOosSwap(subOrOosItemId: String): Boolean {
        return pickRepository.pickList.value?.itemActivities?.any { it.itemId == subOrOosItemId } == true
    }

    private fun getItem(itemId: String): ItemActivityDto? {
        return pickRepository.pickList.value?.itemActivities?.find { it.itemId == itemId }
    }

    private fun getIaId(itemId: String): Long? {
        return pickRepository.pickList.value?.itemActivities?.find { it.itemId == itemId }?.id
    }

    private fun undoShorts(oosItemId: String, messageSid: String) {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { undoShorts(oosItemId, messageSid) }
            } else {
                val item = pickRepository.pickList.value?.itemActivities?.find { it.itemId == oosItemId }
                val requests = item?.shortedItemUpc?.map {
                    UndoShortRequestDto(
                        actId = pickRepository.pickList.first()?.actId ?: 0,
                        iaId = getIaId(oosItemId),
                        shortedItemId = it.shortedId,
                        qty = it.exceptionQty,
                        messageSid = messageSid
                    )
                }
                val results = requests?.let {
                    isBlockingUi.wrap {
                        pickRepository.undoShortages(it)
                    }
                }
                results?.let {
                    navigateToSubstitution(item.toSwapItem(isOutOfStock = true), messageSid)
                }
            }
        }
    }

    // Navigate to substitution screen
    // send messageSid in recordPickComplete request for OOS swap and
    // send messageSid in undoPick request for Substitution swap
    private fun navigateToSubstitution(swapItem: SwapItem? = null, messageSid: String? = null, substitutionRemovedQty: Int? = null, itemId: String? = null) {
        viewModelScope.launch {
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    ChatFragmentDirections.actionChatFragmentToSubstituteFragment(
                        SubstituteParams(
                            iaId = swapItem?.id,
                            pickListId = pickRepository.pickList.first()?.actId.toString(),
                            path = SubstitutionPath.SWAPSUBSTITUTION,
                            substitutionRemovedQty = substitutionRemovedQty,
                            messageSid = messageSid,
                            swapSubstitutionReason = getSwapSubstitutionReason(swapItem, itemId)
                        )
                    )
                )
            )
        }
    }

    private fun onSwapIconClicked() {
        firebaseAnalytics.logEvent(EventCategory.CHAT, EventAction.CLICK, EventLabel.CHAT_SWAP_BTN)
        _navigationEvent.postValue(NavigationEvent.Directions(ChatFragmentDirections.actionChatFragmentToQuickTaskFragment(customerName = customerFullName)))
    }

    fun fetchImageUrl(index: Long, sid: String, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            try {
                val sourceUriResult = runCatching {
                    messagesRepository.getMediaContentTemporaryUrl(index, sid)
                }
                Timber.d("fetch image url success ${sourceUriResult.getOrElse { it.message }}")
                val source = sourceUriResult.getOrElse { "" }
                Timber.d("source is :: $source")
                withContext(Dispatchers.Main) {
                    onSuccess(source)
                }
            } catch (exception: Exception) {
                Timber.d("fetch Image Url failed for sid $sid with exception ${exception.message}")
                onFailure()
            }
        }
    }

    fun sendMessage() {
        firebaseAnalytics.logEvent(EventCategory.CHAT, EventAction.CLICK, EventLabel.CHAT_SEND_BTN, listOf(Pair(EventKey.EVENT_CATEGORY, "text_message")))
        typedMessage.value?.toString()?.trim().takeIf { it.isNotNullOrEmpty() }?.let { message ->
            Timber.d("Sending message: $message")
            sendTextMessage(conversationSid, message)
            typedMessage.set("")
        }
    }

    fun setPhotoPreviewBoolean(isVisibile: Boolean, text: CharSequence) {
        isPhotoPreviewMode.postValue(isVisibile)
        typedMessage.postValue(text.toString())
    }

    fun sendTextMessage(
        conversationId: String,
        message: String,
    ) = viewModelScope.launch {
        val messageUuid = UUID.randomUUID().toString()
        Timber.d("messageUuid: $messageUuid")
        try {
            userRepository.user.value?.let {
                messagesRepository.sendTextMessage(conversationId, orderNumber, fulfillmentOrderId, message, messageUuid, it)
                Timber.d("messageUuid sent: $messageUuid")
            }
        } catch (e: TwilioException) {
            Timber.d("messageUuid Text message send error: ${e.errorInfo.status}:${e.errorInfo.code} ${e.errorInfo.message}")
            conversationsRepository.updateMessageStatus(conversationId, messageUuid, SendStatus.ERROR.value, e.errorInfo.code)
        }
    }

    fun handleChatImageClick(imageurl: String) {
        firebaseAnalytics.logEvent(EventCategory.CHAT, EventAction.CLICK, EventLabel.CHAT_IMAGE_CLICK)
        viewModelScope.launch {
            showItemPhotoDialog.postValue(
                imageurl
            )
        }
    }

    fun sendMediaMessage(
        uri: String,
        inputStream: InputStream,
        fileName: String?,
        mimeType: String?,
    ) {
        firebaseAnalytics.logEvent(EventCategory.CHAT, EventAction.CLICK, EventLabel.CHAT_SEND_BTN, listOf(Pair(EventKey.EVENT_CATEGORY, "media_message")))
        val text = typedMessage.value
        typedMessage.set("")
        viewModelScope.launch {
            val messageUuid = UUID.randomUUID().toString()
            try {
                userRepository.user.value?.let {
                    messagesRepository.sendMediaMessage(conversationSid, orderNumber, fulfillmentOrderId, uri, inputStream, fileName, mimeType, messageUuid, it, text)
                    Timber.d("Media message sent: $messageUuid")
                }
            } catch (e: TwilioException) {
                Timber.d("Media message send error: ${e.errorInfo.status}:${e.errorInfo.code} ${e.errorInfo.message}")
            }
        }
    }

    private fun getSwapSubstitutionReason(swapItem: SwapItem?, itemId: String? = null): SwapSubstitutionReason? {
        return swapItem?.let {
            when (it.subApprovalStatus) {
                SubApprovalStatus.OUT_OF_STOCK ->
                    if (canPerformSubstitutionOrOosSwap(itemId ?: it.itemId ?: "")) {
                        if (isMasterOrderView == true) SwapSubstitutionReason.SWAP_OOS else SwapSubstitutionReason.SWAP
                    } else {
                        SwapSubstitutionReason.SWAP_OOS_OTHER_PICKLIST
                    }
                else -> if (canPerformSubstitutionOrOosSwap(itemId ?: it.itemId ?: "")) {
                    SwapSubstitutionReason.SWAP
                } else {
                    SwapSubstitutionReason.SWAP_OTHER_PICKLIST
                }
            }
        }
    }

    fun handleMessageDisplayed(messageIndex: Long) =
        viewModelScope.launch {
            try {
                Timber.d("handleMessageDisplayed: $messageIndex")
                messagesRepository.notifyMessageRead(conversationSid, messageIndex)
            } catch (e: TwilioException) {
                // Ignored
                Timber.d("handleMessageDisplayed: $e")
            }
        }

    fun onNetworkAlertCloseCTAClick() {
        hideNetworkAlert.postValue(true)
    }

    fun clearNotification() {
        pushNotificationsRepository.clearChatIds()
    }

    companion object {
        const val CANT_SWAP_DIALOG_TAG = "cantSwapDialogTag"
        private const val SWAP_SUBSTITUTION_ERROR_DIALOG_TAG = "swapSubstitutionErrorDialogTag"
        private const val CHAT_CAMERA_ACCESS_DIALOG_TAG = "cameraAccessDialogTag"
    }
}

fun List<MessageDataItem>.asMessageListViewItems(app: Application, userLDapToken: String, otherPickerLdapIds: Map<String, String>) = mapIndexed { index, item ->
    item.toMessageListViewItem(
        app,
        userLDapToken, otherPickerLdapIds
    )
}

@BindingAdapter("onActionSend")
fun EditText.setoOnActionSend(function: (() -> Unit)?) {
    if (function == null) {
        setOnEditorActionListener(null)
    } else {
        setOnEditorActionListener { _, actionId, event ->

            val imeAction =
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE,
                    EditorInfo.IME_ACTION_SEND,
                    EditorInfo.IME_ACTION_GO,
                    -> true
                    else -> false
                }

            val actionDown = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN

            if (imeAction || actionDown) {
                true.also {
                    function.invoke()
                }
            } else {
                false
            }
        }
    }
}

@BindingAdapter("app:voiceToText")
fun EditText.setVoiceToText(isVoiceToText: Boolean) {
    val filter = InputFilter { source, start, end, dest, dstart, dend ->
        for (i in start until end) {
            val type = Character.getType(source[i])
            if (type.toByte() == Character.SURROGATE || type.toByte() == Character.OTHER_SYMBOL) {
                return@InputFilter ""
            }
        }
        null
    }

    if (isVoiceToText) {
        filters = arrayOf(filter)
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
    } else {
        filters = arrayOf()
        inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        View.IMPORTANT_FOR_AUTOFILL_NO
    }
}
