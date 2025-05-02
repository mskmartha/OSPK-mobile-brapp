package com.albertsons.acupick.ui.substitute

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.response.SwapItem
import com.albertsons.acupick.databinding.ItemRemoveSubstitutionBinding
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

class SwapSubRemoveItemDbViewModel(
    private val swapItem: SwapItem,
) : BindableItem<ItemRemoveSubstitutionBinding?>() {
    override fun bind(viewBinding: ItemRemoveSubstitutionBinding, position: Int) {
        viewBinding.apply { substitutedItem = swapItem }
    }

    override fun getLayout() = R.layout.item_remove_substitution

    override fun initializeViewBinding(view: View) = ItemRemoveSubstitutionBinding.bind(view)
}

@BindingAdapter("app:swapSubstitutionItem")
fun RecyclerView.setSwapSubstitutionItem(items: List<SwapItem>?) {
    if (items.isNotNullOrEmpty()) {
        layoutManager = LinearLayoutManager(context)
        val groupAdapter = GroupAdapter<GroupieViewHolder>()
        items?.map { item ->
            groupAdapter.add(Section(SwapSubRemoveItemDbViewModel(item)))
        }
        adapter = groupAdapter
    }
}
