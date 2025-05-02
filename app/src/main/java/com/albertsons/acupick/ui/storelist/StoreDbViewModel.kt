package com.albertsons.acupick.ui.storelist

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.BaseBindableViewModel
import com.albertsons.acupick.ui.LiveDataHelper
import com.albertsons.acupick.ui.ViewModelItem
import com.google.android.material.textview.MaterialTextView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

class StoreDbViewModel(
    val storeNo: String,
    val selected: Boolean,
    private val storeClickListener: (String) -> Unit
) : LiveDataHelper, BaseBindableViewModel() {
    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> =
        { vm -> ViewModelItem(vm as StoreDbViewModel, R.layout.item_store) }

    fun onStoreClicked() = storeClickListener(storeNo)
}

@BindingAdapter(value = ["app:stores", "app:viewModel"])
fun RecyclerView.setStores(stores: List<String>?, viewModel: StoresViewModel?) {
    // Exit if not all information provided yet.
    if (stores == null || viewModel == null) return

    @Suppress("UNCHECKED_CAST")
    // If adapter can be cast to GroupieAdapter, update with new data
    (adapter as? GroupAdapter<GroupieViewHolder>)?.apply {
        // Update adapter with new info.
        clear()
        add(generateSection(stores, viewModel))
    } ?: run {
        //  Create new adapter
        layoutManager = LinearLayoutManager(context)
        adapter = GroupAdapter<GroupieViewHolder>().apply { add(generateSection(stores, viewModel)) }
    }
}

private fun generateSection(stores: List<String>, viewModel: StoresViewModel) =
    Section().apply {
        update(
            stores.map { storeNo ->
                StoreDbViewModel(
                    storeNo = storeNo,
                    selected = storeNo == viewModel.store,
                    storeClickListener = viewModel::onStoreClicked
                )
            }
        )
    }

@BindingAdapter("isSelected")
fun View.setSelectedBinding(selected: Boolean) {
    isSelected = selected
}

@BindingAdapter("selectedTypefaceToggle")
fun MaterialTextView.selectedTypefaceToggle(selected: Boolean) {
    typeface = resources.getFont(if (selected) R.font.nunito_sans_bold else R.font.nunito_sans_semibold)
}
