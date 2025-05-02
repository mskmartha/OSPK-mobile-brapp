package com.albertsons.acupick.ui.substitute

import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.BaseBindableViewModel
import com.albertsons.acupick.ui.LiveDataHelper
import com.albertsons.acupick.ui.ViewModelItem
import com.albertsons.acupick.ui.util.CenterLayoutManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

data class BulkSubstitutionDbViewModel(
    val selected: Boolean,
    private val bulkItemClickListener: (BulkItem) -> Unit,
    val bulkItem: BulkItem,
) : LiveDataHelper, BaseBindableViewModel() {

    val title = bulkItem.itemDes
    val imageUrl = bulkItem.imageUrl
    val itemId = bulkItem.itemId
    val isSystemSuggested = bulkItem.isSystemSuggested
    val isCustomerChoosen = bulkItem.customerChosen
    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as BulkSubstitutionDbViewModel, R.layout.bulk_item_substitution) }
    }

    fun onClick(bulkItems: BulkItem) {
        bulkItemClickListener(bulkItems)
    }
}

@BindingAdapter("app:bulkSubstitutionItems")
fun RecyclerView.setbulkSubstitutionItems(bulkSubstitutionItems: List<BulkSubstitutionDbViewModel>?) {
    if (bulkSubstitutionItems == null) return

    @Suppress("UNCHECKED_CAST")
    // If adapter already exists, then cast and re-use
    (adapter as? GroupAdapter<GroupieViewHolder>)?.apply {
        clear()
        add(generateSubListSection(bulkSubstitutionItems))
    } ?: run {
        // Otherwise; create new adapter
        layoutManager = CenterLayoutManager(context, RecyclerView.VERTICAL, false)
        adapter = GroupAdapter<GroupieViewHolder>().apply {
            add(generateSubListSection(bulkSubstitutionItems))
        }
    }
}

private fun generateSubListSection(subItems: List<BulkSubstitutionDbViewModel>): Section {
    return Section().apply {
        update(
            subItems
        )
    }
}
