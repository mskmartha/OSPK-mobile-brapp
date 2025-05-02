package com.albertsons.acupick.ui.arrivals.destage.removeitems

import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.RejectedItem
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.ui.BaseBindableViewModel
import com.albertsons.acupick.ui.LiveDataHelper
import com.albertsons.acupick.ui.ViewModelItem
import com.albertsons.acupick.ui.util.orFalse
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

class RejectedItemDbViewModel(
    rejectedItem: RejectedItem?,
    val onCheckedChange: (RejectedItem?, Boolean) -> Unit,
    val misplacedItemClickListener: (RejectedItem?) -> Unit,
) : LiveDataHelper, BaseBindableViewModel() {
    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as RejectedItemDbViewModel, R.layout.item_rejected_item) }
    }

    val item = rejectedItem
    val isChecked = MutableLiveData(false)
    val isMisplaced = MutableLiveData(false)
    val showPill = (rejectedItem?.qty ?: 0) > 0 || rejectedItem?.weight != null
    val isWeighted = rejectedItem?.weight != null && rejectedItem.displayType == 3 && rejectedItem.itemType == SellByType.Weight

    fun onCompleteCheckedChanged() {
        onCheckedChange(item, isChecked.value.orFalse())
    }

    fun onMarkAsMisplacedClicked() {
        misplacedItemClickListener(item)
    }

    fun markItemAsMisplaced() {
        isMisplaced.value = true
    }
}

@BindingAdapter(value = ["app:rejectedItems"])
fun RecyclerView.setRejectedItems(items: List<RejectedItemDbViewModel>?) {
    if (items == null) return
    layoutManager = LinearLayoutManager(context)
    @Suppress("UNCHECKED_CAST")
    // If adapter can be cast to GroupieAdapter, update with new data
    (adapter as? GroupAdapter<GroupieViewHolder>)?.apply {
        // Update adapter with new info.
        clear()
        add(populateRejectedItems(items))
    } ?: run {
        //  Create new adapter
        layoutManager = LinearLayoutManager(context)
        adapter = GroupAdapter<GroupieViewHolder>().apply { add(populateRejectedItems(items)) }
    }
}

private fun populateRejectedItems(itemDbViewModelList: List<RejectedItemDbViewModel>) = Section().apply {
    addAll(itemDbViewModelList.partition { it.isMisplaced.value == false }.toList().flatten())
}

@BindingAdapter("app:markAsMisplacedTextAndColor")
fun TextView.markAsMisplacedTextAndColor(isMarkedMisplaced: Boolean) {
    if (!isMarkedMisplaced) {
        text = context.getString(R.string.mark_as_misplaced)
        setTextColor(context.getColor(R.color.albertsonsBlue))
        isClickable = true
    } else {
        text = context.getString(R.string.item_marked_as_misplaced)
        setTextColor(context.getColor(R.color.darkestOrange))
        isClickable = false
    }
}

@BindingAdapter("app:isWeighted", "app:weight", "app:quantity")
fun TextView.setCount(isWeighted: Boolean, weight: Double, quantity: Int) {
    if (isWeighted) {
        text = context.getString(R.string.remove_weighted_item_unit, weight.toString())
    } else {
        text = quantity.toString()
    }
}

@BindingAdapter("app:setRejectedItemCheckBoxStates")
fun AppCompatImageView.setRejectedItemCheckBoxStates(isChecked: Boolean) {
    if (isChecked) {
        setImageResource(R.drawable.ic_checkbox_checked_state)
    } else {
        setImageResource(R.drawable.ic_checkbox_unchecked_state)
    }
}
