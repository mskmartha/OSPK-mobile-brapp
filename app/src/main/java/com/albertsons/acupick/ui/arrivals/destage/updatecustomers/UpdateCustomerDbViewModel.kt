package com.albertsons.acupick.ui.arrivals.destage.updatecustomers

import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.BaseBindableViewModel
import com.albertsons.acupick.ui.ViewModelItem
import com.albertsons.acupick.ui.models.OrderItemUI
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

class UpdateCustomerDbViewModel(
    val itemUI: OrderItemUI?,
    val vm: UpdateCustomerBaseViewModel?,
    val isChecked: Boolean,
    val isMaxOrderSelected: Boolean,
    private val itemClickListener: (OrderItemUI?) -> Unit,
) : BaseBindableViewModel() {

    val isDisabled = !isChecked && isMaxOrderSelected

    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as UpdateCustomerDbViewModel, R.layout.item_update_customer) }
    }

    fun onItemClick() {
        itemClickListener(itemUI)
    }
}

@BindingAdapter(value = ["app:viewModel", "app:orderItems"])
fun RecyclerView.setOrderItemUi(viewModel: UpdateCustomerBaseViewModel, orderItems: List<OrderItemUI>?) {
    if (orderItems != null) {
        @Suppress("UNCHECKED_CAST")
        (adapter as? GroupAdapter<GroupieViewHolder>)?.apply {
            clear()
            add(generateSection(orderItems, viewModel))
        } ?: run {
            layoutManager = LinearLayoutManager(context)
            adapter = GroupAdapter<GroupieViewHolder>().apply { add(generateSection(orderItems, viewModel)) }
        }
    }
}

private fun generateSection(orders: List<OrderItemUI>, viewModel: UpdateCustomerBaseViewModel) =
    Section().apply {
        update(
            orders.map { order ->
                UpdateCustomerDbViewModel(
                    itemUI = order,
                    vm = viewModel,
                    isMaxOrderSelected = viewModel.isMaxOrderSelected,
                    isChecked = viewModel.isOrderSelected(order),
                    itemClickListener = viewModel::onClickItem
                )
            }
        )
    }

@BindingAdapter("app:setEmptyString")
fun TextView.setEmptyString(isMarkArrived: Boolean) {
    text = if (isMarkArrived) {
        context.getString(R.string.update_customer_empty_text_marked)
    } else {
        context.getString(R.string.update_customer_empty_text_add)
    }
}

@BindingAdapter("app:fullfillmentTypeNameResource")
fun AppCompatTextView.setFulFillmentTypeResToText(fullfillmentTypeNameResource: Int?) {
    fullfillmentTypeNameResource?.let { text = context.getString(it) }
}
