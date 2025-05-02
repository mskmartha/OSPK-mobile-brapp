package com.albertsons.acupick.ui.arrivals.destage.removeitems

import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.EntityReference
import com.albertsons.acupick.data.model.RejectedItem
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.ui.BaseBindableViewModel
import com.albertsons.acupick.ui.LiveDataHelper
import com.albertsons.acupick.ui.ViewModelItem
import com.albertsons.acupick.ui.util.orFalse
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

class RejectedItemHeaderViewModel(
    val shortOrderNumber: String,
    val orderNumber: String,
    val storageType: StorageType = StorageType.AM,
    val entityReference: EntityReference? = null,
    val childItems: List<Rejected1PLItemDbViewModel>,
) : BaseBindableViewModel() {
    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as RejectedItemHeaderViewModel, R.layout.row_1pl_rejected_item_header) }
    }
}

class Rejected1PLItemDbViewModel(
    var fragmentViewLifecycleOwner: LifecycleOwner? = null,
    rejectedItem: RejectedItem,
    val onCheckedChange: (RejectedItem?, Boolean) -> Unit,
    val misplacedItemClickListener: (RejectedItem?) -> Unit,
) : LiveDataHelper, BaseBindableViewModel() {
    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as Rejected1PLItemDbViewModel, R.layout.row_1pl_rejected_item, fragmentViewLifecycleOwner) }
    }

    val item = rejectedItem
    val isChecked = MutableLiveData(false)
    val isMisplaced = MutableLiveData(false)
    val showPill = (rejectedItem.qty ?: 0) > 0 || rejectedItem.weight != null
    val isWeighted = rejectedItem.weight != null && rejectedItem.displayType == 3 && rejectedItem.itemType == SellByType.Weight

    fun onCheckChanged() {
        onCheckedChange(item, isChecked.value?.not() ?: false)
    }

    fun onMarkAsMisplacedClicked() {
        if (!isMisplaced.value.orFalse()) {
            misplacedItemClickListener(item)
        }
    }

    fun markItemAsMisplaced() {
        isMisplaced.value = true
    }
}

@BindingAdapter(value = ["app:listItems", "app:viewModel", "app:fragmentViewLifecycleOwner"])
fun RecyclerView.setListItems(listItems: List<RejectedItemHeaderViewModel>?, viewModel: RemoveRejected1PLItemsViewModel?, fragmentViewLifecycleOwner: LifecycleOwner) {
    if (listItems == null || viewModel == null) return

    layoutManager = LinearLayoutManager(context)

    // clear previous data on first initialization
    (adapter as? GroupAdapter<*>)?.clear()

    listItems.forEachIndexed { index, entry ->
        val itemActivities = entry.childItems
        itemActivities.forEach {
            it.fragmentViewLifecycleOwner = fragmentViewLifecycleOwner
        }
        val shortOrderNumber = entry.shortOrderNumber
        val orderNumber = entry.orderNumber
        val storegeType = entry.storageType
        val entityRef = entry.entityReference

        @Suppress("UNCHECKED_CAST")
        // If adapter already exists, then cast and re-use
        (adapter as? GroupAdapter<GroupieViewHolder>)?.apply {
            add(RejectedItemHeaderViewModel(shortOrderNumber, orderNumber, storegeType, entityRef, itemActivities))
            add(
                generateListSection(
                    itemActivities = itemActivities,
                    viewModel = viewModel,
                )
            )
        } ?: run {
            // Otherwise; create new adapter
            adapter = GroupAdapter<GroupieViewHolder>().apply {
                add(RejectedItemHeaderViewModel(shortOrderNumber, orderNumber, storegeType, entityRef, itemActivities))
                add(
                    generateListSection(
                        itemActivities = itemActivities,
                        viewModel = viewModel,
                    )
                )
            }
        }
    }
}

private fun RecyclerView.generateListSection(
    itemActivities: List<Rejected1PLItemDbViewModel>,
    viewModel: RemoveRejected1PLItemsViewModel,
): Section {
    val section = Section()
    section.update(itemActivities)
    return section
}
