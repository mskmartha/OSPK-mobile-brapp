package com.albertsons.acupick.ui.arrivals

import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.VanStatus
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.BaseBindableViewModel
import com.albertsons.acupick.ui.ViewModelItem
import com.albertsons.acupick.ui.models.CustomerArrivalStatusUI
import com.albertsons.acupick.ui.models.OrderItemUI
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.coroutines.Job

class ArrivalsResultsItemDbViewModel(
    val item: OrderItemUI,
    val isChecked: Boolean,
    val isDisabled: Boolean,
    val inProgress: Boolean,
    val isVisibleEllipsis: Boolean,
    val firstNotificationETATime: Int,
    val secondNotificationETATime: Int,
    val is1pl: Boolean,
    private val itemClickListener: (OrderItemUI) -> Unit,
    private val ellipsisClickListener: (OrderItemUI) -> Unit,
    val onStartTimer: (Job) -> Unit
) : BaseBindableViewModel() {
    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as ArrivalsResultsItemDbViewModel, R.layout.item_arrival_orders) }
    }

    fun onItemClick() {
        itemClickListener(item)
    }

    fun onEllipsisClick() {
        ellipsisClickListener(item)
    }
}

@BindingAdapter(value = ["app:isInProgress", "app:sortedItems", "app:viewModel"], requireAll = false)
fun RecyclerView.sortedItems(
    isInProgress: Boolean?,
    sortedItems: List<OrderItemUI>?,
    viewModel: ArrivalsViewModel?,
) {
    if (sortedItems == null || viewModel == null) return

    val items = if (isInProgress == true)
        sortedItems.filter { it.pickerName.isNotNullOrEmpty() || it.vanStatus == VanStatus.IN_PROGRESS }
    else
        sortedItems.filter { it.pickerName.isNullOrEmpty() && it.vanStatus != VanStatus.IN_PROGRESS }

    @Suppress("UNCHECKED_CAST")
    (adapter as? GroupAdapter<GroupieViewHolder>)?.apply {
        clear()
        add(generateSection(items, viewModel))
    } ?: run {
        layoutManager = LinearLayoutManager(context)
        adapter = GroupAdapter<GroupieViewHolder>().apply { add(generateSection(items, viewModel)) }
    }
}

private fun generateSection(orders: List<OrderItemUI>, viewModel: ArrivalsViewModel) =
    Section().apply {
        update(
            orders.map { order ->
                val inProgress = viewModel.isInProgess(order)
                ArrivalsResultsItemDbViewModel(
                    item = order,
                    isChecked = viewModel.isOrderSelected(order.orderNumber),
                    isDisabled = viewModel.isMaxOrderSelected,
                    inProgress = inProgress,
                    isVisibleEllipsis = order.customerArrivalStatus == CustomerArrivalStatusUI.ARRIVED_NOT_STARTED || order.customerArrivalStatus == CustomerArrivalStatusUI.ARRIVED,
                    firstNotificationETATime = viewModel.firstNotificationETATime,
                    secondNotificationETATime = viewModel.secondNotificationETATime,
                    is1pl = viewModel.is1Pl,
                    itemClickListener = viewModel::onClickItem,
                    ellipsisClickListener = viewModel::onEllipsisClick,
                    onStartTimer = viewModel::onStartTimer
                )
            }
        )
    }
