package com.albertsons.acupick.ui.chat

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

import android.app.Application
import com.albertsons.acupick.data.model.CustomerChatInfo
import com.albertsons.acupick.data.model.OrderChatDetail
import com.albertsons.acupick.data.model.chat.Direction
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.PushNotificationsRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.util.isPickingFlowExcludeSubstitution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import org.koin.core.component.inject

class ChatButtonViewModel(val app: Application) : BaseViewModel(app) {

    private val pickRepository: PickRepository by inject()
    private val conversationsRepository: ConversationsRepository by inject()
    private val pushNotificationsRepo: PushNotificationsRepository by inject()
    private val dispatcherProvider: DispatcherProvider by inject()
    private val _isTooltipVisible = MutableStateFlow(false)
    private val _notificationCount = MutableStateFlow(0)
    val isTooltipVisible: StateFlow<Boolean> get() = _isTooltipVisible
    val notificationCount: StateFlow<Int> get() = _notificationCount
    val showUnreadMessages = MutableStateFlow(false)
    val showNotificationCount = MutableStateFlow(false)
    val isPickingScreen = MutableStateFlow(pushNotificationsRepo.getCurrentDestination().isPickingFlowExcludeSubstitution())

    val customerChatInfoList: MutableStateFlow<List<CustomerChatInfo>> = MutableStateFlow(emptyList())
    val showChatButton = MutableStateFlow(false)
    private val openItemTypes = listOf(Direction.ENRICHDED_SUBSTITUTION, Direction.ENRICHDED_OOS_SUBSTITUTION)

    init {
        viewModelScope.launch {
            conversationsRepository.showUnreadMessageDot.collectLatest { unreadMessageList ->
                showUnreadMessages.value = unreadMessageList.values.any { it }
                _notificationCount.value = unreadMessageList.count { it.value }
                showNotificationCount.value = unreadMessageList.size > 1
            }
        }

        viewModelScope.launch(dispatcherProvider.IO) {
            conversationsRepository.showChatButton.collectLatest {
                showChatButton.value = it
            }
        }
    }

    fun onFloatingActionButtonClick(onChatClicked: (orderNumber: String) -> Unit) {
        viewModelScope.launch {
            val orderChatDetails = pickRepository.pickList.value?.orderChatDetails
                ?.filter {
                    it.conversationSid.isNotNullOrEmpty() ||
                        conversationsRepository.getConversationId(it.customerOrderNumber.orEmpty()).isNotNullOrEmpty()
                }.orEmpty()

            when (orderChatDetails.size) {
                1 -> {
                    _isTooltipVisible.value = false
                    val orderChatDetail = orderChatDetails.first()
                    onChatClicked(orderChatDetail.customerOrderNumber.orEmpty())
                }
                else -> {
                    val customerChatInfoList = orderChatDetails.map { orderChatDetail ->
                        val conversationSid = orderChatDetail.conversationSid
                            ?: conversationsRepository.getConversationId(orderChatDetail.customerOrderNumber.orEmpty())
                        val lastTimeStamp = conversationsRepository.getLastMessageTimestamp(conversationSid)
                        orderChatDetail.toCustomerChatInfo(conversationsRepository, openItemTypes, conversationSid, lastTimeStamp)
                    }.sortedByDescending { it.lastMessageTime }
                    this@ChatButtonViewModel.customerChatInfoList.value = customerChatInfoList
                    _isTooltipVisible.value = !_isTooltipVisible.value
                }
            }
        }
    }

    fun setTooltipVisibility(isVisible: Boolean) {
        _isTooltipVisible.value = isVisible
    }

    private fun OrderChatDetail.toCustomerChatInfo(
        conversationsRepository: ConversationsRepository,
        openItemTypes: List<Direction>,
        conversationSid: String,
        lastTimeStamp: String,
    ): CustomerChatInfo {
        return CustomerChatInfo(
            customerOrderNumber = customerOrderNumber.orEmpty(),
            customerFirstName = customerFirstName.orEmpty(),
            customerLastName = customerLastName.orEmpty(),
            conversationSid = conversationSid,
            hasUnreadMessages = conversationsRepository.showUnreadMessageDot.value[conversationSid] ?: false,
            referenceEntityId = referenceEntityId.orEmpty(),
            isCustomerTyping = conversationsRepository.onTyping.value[this.conversationSid] ?: false,
            substitutionItemImages = conversationsRepository.messages.value?.get(conversationSid)
                ?.filter { Direction.fromInt(it.direction) in openItemTypes }
                ?.mapNotNull { it.attributes?.orderedItem?.imageUrl },
            lastMessageTime = lastTimeStamp
        )
    }
}
