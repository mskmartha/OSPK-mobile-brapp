package com.albertsons.acupick.ui.arrivals.complete

import android.app.Application
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.response.OrderSummary
import com.albertsons.acupick.data.model.response.Title
import com.albertsons.acupick.databinding.OrderHeaderItemBinding
import com.albertsons.acupick.databinding.OrderSubheaderItemBinding
import com.albertsons.acupick.databinding.OrderSummaryDeclinedItemBinding
import com.albertsons.acupick.databinding.OrderSummaryHeaderBinding
import com.albertsons.acupick.databinding.OrderSummaryItemBinding
import com.albertsons.acupick.databinding.OrderSummaryOutOfStockItemBinding
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.BaseViewModel
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

class OrderSummaryViewModel(app: Application) : BaseViewModel(app) {

    val orderSummaryData = MutableLiveData<List<OrderSummary>>()
    val isCas = MutableLiveData<Boolean>()
    val is3p = MutableLiveData<Boolean>()
    val partnerName = MutableLiveData<String>()

    init {
        changeToolbarTitleEvent.postValue(app.getString(R.string.order_summary))
    }

    fun setupData(orderSummary: List<OrderSummary>, isCasOrder: Boolean?, is3p: Boolean?, partnerName: String?) {
        orderSummaryData.postValue(orderSummary)
        isCas.postValue(isCasOrder)
        this.is3p.postValue(is3p)
        this.partnerName.postValue(partnerName)
    }
}

@BindingAdapter(value = ["app:orderItems", "app:viewodel", "app:fragmentLifecycleOwner"], requireAll = false)
fun RecyclerView.setOrderSummary(orderSummaryItems: List<OrderSummary>?, viewModel: OrderSummaryViewModel, fragmentLifecycleOwner: LifecycleOwner?) {
    layoutManager = LinearLayoutManager(context)
    @Suppress("UNCHECKED_CAST")
    adapter = GroupieAdapter().apply {
        orderSummaryItems?.let {
            generateOrderItems(fragmentLifecycleOwner, orderSummaryItems, viewModel)
        }
    }
}

fun GroupieAdapter.generateOrderItems(
    fragmentLifecycleOwner: LifecycleOwner?,
    orderSummaryItem: List<OrderSummary>,
    viewModel: OrderSummaryViewModel,
) {
    val group = Section().apply {

        if (viewModel.is3p.value == true && viewModel.partnerName.value.isNotNullOrEmpty()) {
            add(OrderSummaryHeaderItem(R.string.payment_complete_partner_pick_message, R.drawable.ic_alert, R.color.border_orange, viewModel.partnerName.value))
        } else if (viewModel.isCas.value == null || viewModel.isCas.value == true) {
            add(OrderSummaryHeaderItem(R.string.payment_not_complete_without_sub_message, R.drawable.ic_alert, R.color.border_orange))
        } else {
            add(OrderSummaryHeaderItem(R.string.payment_complete_message, R.drawable.ic_checkmark_summary, R.color.ambientGreen))
        }

        if (orderSummaryItem.any { it.title == Title.OUT_OF_STOCK }) {
            add(OrderHeader(R.string.out_of_Stock))
            orderSummaryItem
                .filter { it.title == Title.OUT_OF_STOCK }
                .map {
                    add(OrderItemOutOfStock(fragmentLifecycleOwner, it))
                }
        }

        if (orderSummaryItem.any { it.title == Title.SUBSTITUTION }) {
            add(OrderHeader(R.string.susbtitutions))
            orderSummaryItem
                .filter { it.title == Title.SUBSTITUTION }
                .map {
                    add(OrderSubHeader(R.string.item_out_of_stock)) // add header
                    add(OrderItemOutOfStock(fragmentLifecycleOwner, it))
                    if (it.substitutedWith.isNotNullOrEmpty()) {
                        add(OrderSubHeader(R.string.substituted_with)) // add header
                        it.substitutedWith?.map {
                            add(OrderItem(fragmentLifecycleOwner, it))
                        }
                    }
                }
        }

        if (orderSummaryItem.any { it.title == Title.APPROVED_SUB }) {
            add(OrderHeader(R.string.approved_substitutions))
            orderSummaryItem
                .filter { it.title == Title.APPROVED_SUB }
                .map {
                    add(OrderSubHeader(R.string.item_out_of_stock)) // add header
                    add(OrderItemOutOfStock(fragmentLifecycleOwner, it))
                    if (it.substitutedWith.isNotNullOrEmpty()) {
                        add(OrderSubHeader(R.string.substituted_with)) // add header
                        it.substitutedWith?.map {
                            add(OrderItem(fragmentLifecycleOwner, it))
                        }
                    }
                }
        }

        if (orderSummaryItem.any { it.title == Title.DECLINED_SUB }) {
            add(OrderHeader(R.string.declied_sub))
            orderSummaryItem
                .filter { it.title == Title.DECLINED_SUB }
                .map {
                    add(OrderSubHeader(R.string.item_out_of_stock)) // add header
                    add(OrderItemOutOfStock(fragmentLifecycleOwner, it))
                    if (it.substitutedWith.isNotNullOrEmpty()) {
                        add(OrderSubHeader(R.string.declined_subtitle)) // add header
                        it.substitutedWith?.map {
                            add(OrderItemDeclined(fragmentLifecycleOwner, it))
                        }
                    }
                }
        }

        if (orderSummaryItem.any { it.title == Title.REST_OF_THE_ITEMS }) {
            add(OrderHeader(R.string.remaining_items))
            orderSummaryItem
                .filter { it.title == Title.REST_OF_THE_ITEMS }
                .map {
                    add(OrderItem(fragmentLifecycleOwner, it))
                }
        }
    }
    updateAsync(mutableListOf(group))
}

class OrderHeader(@StringRes val title: Int) : BindableItem<OrderHeaderItemBinding>() {
    override fun initializeViewBinding(view: View) = OrderHeaderItemBinding.bind(view)
    override fun getLayout() = R.layout.order_header_item
    override fun bind(viewBinding: OrderHeaderItemBinding, position: Int) {
        viewBinding.header.text = viewBinding.root.context.getString(title)
    }
}

class OrderSubHeader(@StringRes val title: Int) : BindableItem<OrderSubheaderItemBinding>() {
    override fun initializeViewBinding(view: View) = OrderSubheaderItemBinding.bind(view)
    override fun getLayout() = R.layout.order_subheader_item
    override fun bind(viewBinding: OrderSubheaderItemBinding, position: Int) {
        viewBinding.subheader.text = viewBinding.root.context.getString(title)
    }
}

class OrderItem(val fragmentLifecycleOwner: LifecycleOwner?, private val orderItem: OrderSummary) : BindableItem<OrderSummaryItemBinding>() {
    override fun bind(viewBinding: OrderSummaryItemBinding, position: Int) {
        viewBinding.orderSummary = orderItem
    }

    override fun getLayout(): Int = R.layout.order_summary_item
    override fun initializeViewBinding(view: View) = OrderSummaryItemBinding.bind(view)
}

class OrderItemOutOfStock(val fragmentLifecycleOwner: LifecycleOwner?, private val orderItem: OrderSummary) : BindableItem<OrderSummaryOutOfStockItemBinding>() {
    override fun bind(viewBinding: OrderSummaryOutOfStockItemBinding, position: Int) {
        viewBinding.orderSummary = orderItem
    }

    override fun getLayout(): Int = R.layout.order_summary_out_of_stock_item
    override fun initializeViewBinding(view: View) = OrderSummaryOutOfStockItemBinding.bind(view)
}

class OrderItemDeclined(val fragmentLifecycleOwner: LifecycleOwner?, private val orderItem: OrderSummary) : BindableItem<OrderSummaryDeclinedItemBinding>() {
    override fun bind(viewBinding: OrderSummaryDeclinedItemBinding, position: Int) {
        viewBinding.orderSummary = orderItem
    }

    override fun getLayout(): Int = R.layout.order_summary_declined_item
    override fun initializeViewBinding(view: View) = OrderSummaryDeclinedItemBinding.bind(view)
}

class OrderSummaryHeaderItem(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    @ColorRes val strokeColor: Int,
    private val partnerName: String? = null,
) : BindableItem<OrderSummaryHeaderBinding>() {
    override fun bind(viewBinding: OrderSummaryHeaderBinding, position: Int) {
        viewBinding.message.text = viewBinding.root.context.getString(title, partnerName)
        viewBinding.image.setImageResource(icon)
        viewBinding.materialCardView.strokeColor = ContextCompat.getColor(viewBinding.root.context, strokeColor)
    }

    override fun getLayout(): Int = R.layout.order_summary_header
    override fun initializeViewBinding(view: View) = OrderSummaryHeaderBinding.bind(view)
}
