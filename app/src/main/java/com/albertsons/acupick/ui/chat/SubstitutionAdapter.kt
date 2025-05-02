package com.albertsons.acupick.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.data.model.chat.SubstituteItem
import com.albertsons.acupick.databinding.ChatMessageSubstitutionItemBinding

class SubstitutionAdapter(private val items: ArrayList<SubstituteItem>) : RecyclerView.Adapter<SubstitutionAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ChatMessageSubstitutionItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val substituteItem = items[position]
        holder.bind(substituteItem)
    }

    override fun getItemCount() = items.size

    class ViewHolder(private val binding: ChatMessageSubstitutionItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(substituteItem: SubstituteItem) {
            binding.subItem = substituteItem
            binding.executePendingBindings()
        }
    }

    fun updateItems(newItems: List<SubstituteItem>) {
        this.items.clear()
        this.items.addAll(newItems)
        notifyDataSetChanged()
    }
}
