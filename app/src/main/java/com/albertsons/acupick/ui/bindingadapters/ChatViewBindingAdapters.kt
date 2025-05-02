package com.albertsons.acupick.ui.bindingadapters

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.chat.SubApprovalStatus
import com.albertsons.acupick.data.model.chat.SubstituteItem
import com.albertsons.acupick.ui.chat.ChatAdapter
import com.albertsons.acupick.ui.chat.ChatCasSubItemsAdapter
import com.albertsons.acupick.ui.chat.ChatViewModel
import com.albertsons.acupick.ui.chat.MessageListViewItem
import com.albertsons.acupick.ui.chat.SubstitutionAdapter
import timber.log.Timber

@BindingAdapter(value = ["app:setMessages", "app:chatViewModel"], requireAll = true)
fun RecyclerView.setMessages(
    messages: List<MessageListViewItem>?,
    chatViewModel: ChatViewModel?,
) {
    if (messages == null) return
    Timber.d("handleMessageDisplayed: ${messages.size}")

    adapter?.apply {
        // Update adapter with new info.
        val itemCountBefore = itemCount
        (adapter as ChatAdapter).submitList(messages)
        if (messages.size > itemCountBefore) smoothScrollToPosition(this.itemCount ?: 0)
        Timber.d("handleMessageDisplayed adapter : ${messages.size}")
    } ?: run {
        layoutManager =
            LinearLayoutManager(context).apply {
                orientation = RecyclerView.VERTICAL
                stackFromEnd = true
            }
        adapter = ChatAdapter(chatViewModel)
        (adapter as ChatAdapter).submitList(messages)
        Timber.d("handleMessageDisplayed adapter null: ${messages.size}")
    }

    this.addOnScrollListener(
        object : RecyclerView.OnScrollListener() {
            override fun onScrolled(
                recyclerView: RecyclerView,
                dx: Int,
                dy: Int,
            ) {
                Timber.d("handleMessageDisplayed: onScrolled")

                val index = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                Timber.d("handleMessageDisplayed index: $index")

                if (index == RecyclerView.NO_POSITION) return

                // val message = (recyclerView.adapter as GroupAdapter).getItem(index) as? ChatItem
                val message = (recyclerView.adapter as ChatAdapter).getMessageItem(index)
                message?.let {
                    Timber.d("handleMessageDisplayed index: ${it.index} ${it.body}")

                    chatViewModel?.handleMessageDisplayed(it.index)
                }
            }
        },
    )
}

@BindingAdapter(value = ["app:setSubItems"], requireAll = true)
fun RecyclerView.setSubstitutionItems(
    substitionItems: List<SubstituteItem>? = null,
) {
    substitionItems?.let {
        adapter?.apply {
            (adapter as SubstitutionAdapter).updateItems(it)
        } ?: run {
            layoutManager =
                LinearLayoutManager(context).apply {
                    orientation = RecyclerView.VERTICAL
                }
            adapter = SubstitutionAdapter(ArrayList(it))
        }
    }
}

@BindingAdapter(value = ["app:setCasSubItems"], requireAll = true)
fun RecyclerView.setCasSubstitutionItems(
    substitionItems: List<SubstituteItem>? = null,
) {
    substitionItems?.let {
        adapter?.apply {
            (adapter as ChatCasSubItemsAdapter).updateItems(it)
        } ?: run {
            layoutManager =
                LinearLayoutManager(context).apply {
                    orientation = RecyclerView.VERTICAL
                }
            adapter = ChatCasSubItemsAdapter(ArrayList(it))
        }
    }
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

@BindingAdapter("app:statusTextColor")
fun TextView.setStatusTextColor(messageItem: MessageListViewItem) {
    val isApproved = messageItem.customAttributes?.orderedItem?.substitutedItems?.any { it.subApprovalStatus == SubApprovalStatus.APPROVED } ?: false
    text = if (isApproved) {
        setTextColor(context.getColor(R.color.approved_green))
        context.getString(R.string.chat_substitution_approved)
    } else {
        setTextColor(context.getColor(R.color.chat_coffee))
        context.getString(R.string.chat_substitution_declined)
    }
}

@BindingAdapter("senderNameStyle")
fun TextView.setSenderNameStyle(senderName: String) {
    if (senderName == context.getString(R.string.chat_sender_you)) {
        this.setTextAppearance(R.style.NunitoSansRegular12_White)
        this.setBackgroundResource(R.drawable.bg_rounded_corner_name_box)
    } else {
        this.setTextAppearance(R.style.NunitoSansRegular12_Grey700)
        this.setBackgroundResource(R.color.transparent)
    }
}
