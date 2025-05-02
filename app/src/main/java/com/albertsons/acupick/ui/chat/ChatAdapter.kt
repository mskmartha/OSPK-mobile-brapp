package com.albertsons.acupick.ui.chat

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.data.model.chat.Direction
import com.albertsons.acupick.databinding.ChatGreetingsMessageItemBinding
import com.albertsons.acupick.databinding.ChatIncomingMessageItemBinding
import com.albertsons.acupick.databinding.ChatOosDeclineMessageItemBinding
import com.albertsons.acupick.databinding.ChatOosMessageItemBinding
import com.albertsons.acupick.databinding.ChatOutgoingMediaMessageItemBinding
import com.albertsons.acupick.databinding.ChatOutgoingMessageItemBinding
import com.albertsons.acupick.databinding.ChatPickerJoinedBinding
import com.albertsons.acupick.databinding.ChatPickerLeftBinding
import com.albertsons.acupick.databinding.ChatSubstitutionMessageItemBinding
import com.albertsons.acupick.databinding.ChatSystemCasMessageItemBinding
import com.albertsons.acupick.databinding.ChatSystemMessageItemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class BaseChatViewHolder<T>(binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
    abstract fun bind(value: T, position: Int)
}
class ChatAdapter(private val chatViewModel: ChatViewModel?) : ListAdapter<MessageListViewItem, BaseChatViewHolder<MessageListViewItem>>(ChatDiffUtils) {

    private val INCOMING_MESSAGE = 0
    private val OUTGOING_MESSAGE = 1
    private val OUTGOING__IMAGE_MESSAGE = 2
    private val SYSTEM_MESSAGE = 3
    private val GREETING = 4
    private val SUBSTITUTE_MESSASE = 5
    private val SUBSTITUTE_OOS_MESSASE = 6
    private val SUBSTITUTE_CAS_MESSASE = 7
    private val SUBSTITUTE_OOS_DECLINE = 8
    private val PICKER_JOINED = 9
    private val PICKER_LEFT = 10

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseChatViewHolder<MessageListViewItem> {
        return when (viewType) {
            INCOMING_MESSAGE -> {
                val binding = ChatIncomingMessageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                IncomingMessageViewHolder(binding, chatViewModel)
            }
            OUTGOING_MESSAGE -> {
                val binding = ChatOutgoingMessageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                OutGoingMessageViewHolder(binding, chatViewModel)
            }
            OUTGOING__IMAGE_MESSAGE -> {
                val binding = ChatOutgoingMediaMessageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                OutGoingImageMessageViewHolder(binding, chatViewModel, parent.context)
            }
            GREETING -> {
                val binding = ChatGreetingsMessageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ChatGreetingMessageViewHolder(binding)
            }
            SYSTEM_MESSAGE -> {
                val binding = ChatSystemMessageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SystemMessageViewHolder(binding, chatViewModel)
            }
            SUBSTITUTE_MESSASE -> {
                val binding = ChatSubstitutionMessageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SubstitutionMessageItem(binding, chatViewModel)
            }
            SUBSTITUTE_OOS_MESSASE -> {
                val binding = ChatOosMessageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                OosMessageItem(binding, chatViewModel)
            }
            SUBSTITUTE_CAS_MESSASE -> {
                val binding = ChatSystemCasMessageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SystemCASMessageItem(binding)
            }
            SUBSTITUTE_OOS_DECLINE -> {
                val binding = ChatOosDeclineMessageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ChatSubstitutionOosDeclineItem(binding)
            }
            PICKER_JOINED -> {
                val binding = ChatPickerJoinedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ChatPickerJoinedItem(binding)
            }
            PICKER_LEFT -> {
                val binding = ChatPickerLeftBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ChatPickerLeftItem(binding)
            }
            else -> {
                val binding = ChatOutgoingMessageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                OutGoingMessageViewHolder(binding, chatViewModel)
            }
        }
    }

    override fun onBindViewHolder(holder: BaseChatViewHolder<MessageListViewItem>, position: Int) {
        holder.bind(getItem(position), position)
    }

    fun getMessageItem(position: Int): MessageListViewItem {
        return getItem(position)
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item.direction) {
            Direction.OUTGOING -> {
                OUTGOING_MESSAGE
            }
            Direction.INCOMING -> {
                INCOMING_MESSAGE
            }
            Direction.OUTGOING_IMAGE -> {
                OUTGOING__IMAGE_MESSAGE
            }
            Direction.GREETING -> {
                GREETING
            }
            Direction.ENRICHDED_SUBSTITUTION -> {
                SUBSTITUTE_MESSASE
            }
            Direction.ENRICHDED_CAS_SUBSTITUTION -> {
                SUBSTITUTE_CAS_MESSASE
            }
            Direction.ENRICHDED_OOS_SUBSTITUTION -> {
                SUBSTITUTE_OOS_MESSASE
            }
            Direction.ENRICHDED_OOS_DECLINE -> {
                SUBSTITUTE_OOS_DECLINE
            }
            Direction.PICKER_JOINED -> {
                PICKER_JOINED
            }
            Direction.PICKER_LEFT -> {
                PICKER_LEFT
            }
            else -> {
                SYSTEM_MESSAGE
            }
        }
    }

    inner class OutGoingMessageViewHolder(
        private val binding: ChatOutgoingMessageItemBinding,
        private val chatViewModel: ChatViewModel?,
    ) : BaseChatViewHolder<MessageListViewItem>(binding) {

        override fun bind(value: MessageListViewItem, position: Int) {
            binding.message = value
            binding.viewModel = chatViewModel
            binding.executePendingBindings()
        }
    }

    inner class OutGoingImageMessageViewHolder(
        private val binding: ChatOutgoingMediaMessageItemBinding,
        private val chatViewModel: ChatViewModel?,
        private val context: Context,
    ) : BaseChatViewHolder<MessageListViewItem>(binding) {

        override fun bind(value: MessageListViewItem, position: Int) {
            Timber.d("bind image, ${value.index}")
            binding.message = value
            binding.viewModel = chatViewModel
            chatViewModel?.conversationSid?.let { conversationSid ->
                chatViewModel.viewModelScope.launch(Dispatchers.IO) {
                    val cursor = context.contentResolver.query(value.mediaUploadUri ?: Uri.EMPTY, null, null, null)
                    if ((cursor?.count ?: 0) > 0) {
                        binding.imageUrl = value.mediaUploadUri.toString()
                    } else {
                        chatViewModel.fetchImageUrl(value.index, conversationSid, {
                            binding.imageUrl = it
                        }, {
                            Timber.d("Fetching chat image url failed for conversation $conversationSid")
                        })
                    }
                    cursor?.close()
                }
            }
            binding.executePendingBindings()
        }
    }

    inner class IncomingMessageViewHolder(
        private val binding: ChatIncomingMessageItemBinding,
        private val chatViewModel: ChatViewModel?,
    ) : BaseChatViewHolder<MessageListViewItem>(binding) {

        override fun bind(value: MessageListViewItem, position: Int) {
            binding.message = value
            binding.viewModel = chatViewModel
            binding.executePendingBindings()
        }
    }

    inner class SystemMessageViewHolder(
        private val binding: ChatSystemMessageItemBinding,
        private val chatViewModel: ChatViewModel?,
    ) : BaseChatViewHolder<MessageListViewItem>(binding) {

        override fun bind(value: MessageListViewItem, position: Int) {
            binding.message = value
            binding.executePendingBindings()
        }
    }

    inner class ChatGreetingMessageViewHolder(
        private val binding: ChatGreetingsMessageItemBinding,
    ) : BaseChatViewHolder<MessageListViewItem>(binding) {

        override fun bind(value: MessageListViewItem, position: Int) {
            binding.message = value
            binding.executePendingBindings()
        }
    }

    inner class SubstitutionMessageItem(
        private val binding: ChatSubstitutionMessageItemBinding,
        private val chatViewModel: ChatViewModel?,
    ) : BaseChatViewHolder<MessageListViewItem>(binding) {

        override fun bind(value: MessageListViewItem, position: Int) {
            binding.message = value
            binding.viewModel = chatViewModel
            binding.executePendingBindings()
        }
    }

    inner class OosMessageItem(
        private val binding: ChatOosMessageItemBinding,
        private val chatViewModel: ChatViewModel?,
    ) : BaseChatViewHolder<MessageListViewItem>(binding) {

        override fun bind(value: MessageListViewItem, position: Int) {
            binding.message = value
            binding.viewModel = chatViewModel
            binding.executePendingBindings()
        }
    }

    inner class SystemCASMessageItem(
        private val binding: ChatSystemCasMessageItemBinding,
    ) : BaseChatViewHolder<MessageListViewItem>(binding) {

        override fun bind(value: MessageListViewItem, position: Int) {
            binding.message = value
            binding.executePendingBindings()
        }
    }

    inner class ChatSubstitutionOosDeclineItem(
        private val binding: ChatOosDeclineMessageItemBinding
    ) : BaseChatViewHolder<MessageListViewItem>(binding) {
        override fun bind(value: MessageListViewItem, position: Int) {
            binding.message = value
            binding.executePendingBindings()
        }
    }

    inner class ChatPickerJoinedItem(
        private val binding: ChatPickerJoinedBinding,
    ) : BaseChatViewHolder<MessageListViewItem>(binding) {
        override fun bind(value: MessageListViewItem, position: Int) {
            binding.message = value
            binding.executePendingBindings()
        }
    }

    inner class ChatPickerLeftItem(
        private val binding: ChatPickerLeftBinding,
    ) : BaseChatViewHolder<MessageListViewItem>(binding) {
        override fun bind(value: MessageListViewItem, position: Int) {
            binding.message = value
            binding.executePendingBindings()
        }
    }
}

object ChatDiffUtils : DiffUtil.ItemCallback<MessageListViewItem>() {

    override fun areItemsTheSame(oldItemPosition: MessageListViewItem, newItemPosition: MessageListViewItem): Boolean {
        return oldItemPosition.uuid == newItemPosition.uuid
    }

    override fun areContentsTheSame(oldItemPosition: MessageListViewItem, newItemPosition: MessageListViewItem): Boolean {
        return oldItemPosition == newItemPosition
    }
}
