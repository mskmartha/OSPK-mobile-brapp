package com.albertsons.acupick.ui.staging

import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.BaseBindableViewModel
import com.albertsons.acupick.ui.ViewModelItem
import com.albertsons.acupick.ui.models.ToteUI
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

class UnAssignToteZoneItem(
    val item: ToteUI,
    val fragmentViewModel: UnAssignTotesViewModel,
    val fragmentViewLifecycleOwner: LifecycleOwner? = null
) : BaseBindableViewModel() {

    val isChecked = MutableLiveData(item.isChecked)
    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as UnAssignToteZoneItem, R.layout.unassign_tote, fragmentViewLifecycleOwner) }
    }

    fun toggleCheckBox() {
        isChecked.value = isChecked.value?.not()
        fragmentViewModel.updateDiscardedToteList(item.toteId!!, isChecked.value == true)
    }
}

@BindingAdapter("app:zoneTotes", "app:viewModel", "app:fragmentViewLifecycleOwner")
fun RecyclerView.setZoneTotes(items: List<ToteUI>?, viewModel: UnAssignTotesViewModel, fragmentViewLifecycleOwner: LifecycleOwner? = null) {
    if (items == null) return

    layoutManager = LinearLayoutManager(context)

    @Suppress("UNCHECKED_CAST")
    // If adapter can be cast to GroupieAdapter, update with new data
    (adapter as? GroupAdapter<GroupieViewHolder>)?.apply {
        // Update adapter with new info.
        clear()
        add(generateSection(items, viewModel, fragmentViewLifecycleOwner))
    } ?: run {
        //  Create new adapter
        layoutManager = LinearLayoutManager(context)
        adapter = GroupAdapter<GroupieViewHolder>().apply { add(generateSection(items, viewModel, fragmentViewLifecycleOwner)) }
    }
}

private fun generateSection(items: List<ToteUI>, viewModel: UnAssignTotesViewModel, fragmentViewLifecycleOwner: LifecycleOwner? = null) =
    Section().apply {
        update(
            items.map { UnAssignToteZoneItem(it, viewModel, fragmentViewLifecycleOwner) }
        )
    }

@BindingAdapter("app:setToteStates")
fun AppCompatImageView.setToteCheckBoxStates(isChecked: Boolean) {
    if (isChecked) {
        setImageResource(R.drawable.ic_checkbox_checked_state)
    } else {
        setImageResource(R.drawable.ic_checkbox_unchecked_state)
    }
}
