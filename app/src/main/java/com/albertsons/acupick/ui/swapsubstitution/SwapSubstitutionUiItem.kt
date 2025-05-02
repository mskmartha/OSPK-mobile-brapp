package com.albertsons.acupick.ui.swapsubstitution

import android.view.View
import androidx.annotation.StringRes
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.response.SubApprovalStatus
import com.albertsons.acupick.data.model.response.SwapItem
import com.albertsons.acupick.databinding.SwapSubstitutionHeaderItemBinding
import com.albertsons.acupick.databinding.SwapSubstitutionItemBinding
import com.albertsons.acupick.databinding.SwapSubstitutionItemFooterBinding
import com.albertsons.acupick.databinding.SwapSubstitutionSubheaderItemBinding
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

class SwapSubstitutionHeader(
    @StringRes
    val title: Int,
    val count: Int,
) : BindableItem<SwapSubstitutionHeaderItemBinding>() {
    override fun initializeViewBinding(view: View) = SwapSubstitutionHeaderItemBinding.bind(view)
    override fun getLayout() = R.layout.swap_substitution_header_item
    override fun bind(viewBinding: SwapSubstitutionHeaderItemBinding, position: Int) {
        viewBinding.header.text = viewBinding.root.context.getString(title, count)
    }
}

class SwapItemSubHeader(@StringRes val title: Int) : BindableItem<SwapSubstitutionSubheaderItemBinding>() {
    override fun initializeViewBinding(view: View) = SwapSubstitutionSubheaderItemBinding.bind(view)
    override fun getLayout() = R.layout.swap_substitution_subheader_item
    override fun bind(viewBinding: SwapSubstitutionSubheaderItemBinding, position: Int) {
        viewBinding.subheader.text = viewBinding.root.context.getString(title)
    }
}

class SwapSubstitutionItem(val fragmentLifecycleOwner: LifecycleOwner?, private val swapItem: SwapItem) : BindableItem<SwapSubstitutionItemBinding>() {
    override fun bind(viewBinding: SwapSubstitutionItemBinding, position: Int) {
        viewBinding.swapItem = swapItem
    }

    override fun getLayout(): Int = R.layout.swap_substitution_item
    override fun initializeViewBinding(view: View) = SwapSubstitutionItemBinding.bind(view)
}

class SwapSubstitutionItemFooter(
    private val subApprovalStatus: SubApprovalStatus?,
    private val swapItem: SwapItem,
    private val isRepickOriginalItemAllowed: Boolean,
    private val isMasterOrderViewPhase1Enabled: Boolean,
    private val isMasterOrderViewPhase2Enabled: Boolean,
    private val swapSubstitutionClickListener: (SwapItem) -> Unit
) :
    BindableItem<SwapSubstitutionItemFooterBinding>() {
    override fun initializeViewBinding(view: View) = SwapSubstitutionItemFooterBinding.bind(view)
    override fun getLayout() = R.layout.swap_substitution_item_footer

    override fun bind(viewBinding: SwapSubstitutionItemFooterBinding, position: Int) {
        viewBinding.subApprovalStatus = subApprovalStatus
        // Show CTA if [isRepickOriginalItemAllowed] is enabled, otherwise hide it
        viewBinding.isSwapSubstitutionEnabled = when (subApprovalStatus) {
            SubApprovalStatus.APPROVED_SUB -> isRepickOriginalItemAllowed
            else -> true
        }
        // Hide approved text if masterOrderViewPhase2 is enabled, otherwise show it
        viewBinding.approvedViewVisibility = when (subApprovalStatus) {
            SubApprovalStatus.APPROVED_SUB -> isMasterOrderViewPhase2Enabled
            else -> true
        }
        // Hide declined text if masterOrderViewPhase2 is enabled, otherwise show it
        viewBinding.declineViewVisibility = when (subApprovalStatus) {
            SubApprovalStatus.DECLINED_SUB -> isMasterOrderViewPhase2Enabled
            else -> true
        }
        viewBinding.swapConfirmButton.setOnClickListener {
            swapSubstitutionClickListener(swapItem)
        }
    }
}

fun GroupieAdapter.generateSwapSubItems(
    fragmentLifecycleOwner: LifecycleOwner?,
    swapItems: List<SwapItem>,
    viewModel: QuickTaskBaseViewModel,
) {
    val group = Section().apply {
        val statuses = listOf(
            SubApprovalStatus.PENDING_SUB to R.string.pending_substitution,
            SubApprovalStatus.OUT_OF_STOCK to R.string.out_of_stock_items,
            SubApprovalStatus.DECLINED_SUB to R.string.declined_substitutions_swap,
            SubApprovalStatus.APPROVED_SUB to R.string.approved_substitutions_swap
        )

        statuses.forEach { (status, headerRes) ->
            val filteredItems = swapItems.filter { it.subApprovalStatus == status }
            if (filteredItems.isNotEmpty()) {
                add(SwapSubstitutionHeader(headerRes, filteredItems.size))
                filteredItems.forEach { item ->
                    add(SwapItemSubHeader(R.string.item_out_of_stock))
                    add(SwapSubstitutionItem(fragmentLifecycleOwner, item))
                    item.substitutedWith?.takeIf { it.isNotNullOrEmpty() }
                        ?.also { add(SwapItemSubHeader(R.string.substituted_with)) }
                        ?.forEach { subItem ->
                            add(SwapSubstitutionItem(fragmentLifecycleOwner, subItem))
                        }
                    add(
                        SwapSubstitutionItemFooter(
                            subApprovalStatus = item.subApprovalStatus,
                            swapItem = item,
                            isRepickOriginalItemAllowed = viewModel.isRepickOriginalItemAllowed,
                            isMasterOrderViewPhase1Enabled = viewModel.isMasterOrderViewPhase1Enabled,
                            isMasterOrderViewPhase2Enabled = viewModel.isMasterOrderViewPhase2Enabled,
                            swapSubstitutionClickListener = viewModel::onSwapSubstitutionButtonClick
                        )
                    )
                }
            }
        }
    }
    updateAsync(mutableListOf(group))
}

@BindingAdapter("app:setOnRefresh", "app:isRefreshComplete")
fun SwipeRefreshLayout.setOnRefresh(viewModel: QuickTaskBaseViewModel, refreshComplete: Boolean) {
    isRefreshing = refreshComplete
    setOnRefreshListener {
        viewModel.loadData(isRefresh = true)
    }
}

// TODO: Master-order-view will remove old bindings after complete testing this is used in swapsubstitution fragment
@BindingAdapter(value = ["app:swapItems", "app:viewodel", "app:fragmentLifecycleOwner"], requireAll = false)
fun RecyclerView.setSwapItems(
    swapItems: List<SwapItem>?,
    viewModel: SwapSubstitutionViewModel,
    fragmentLifecycleOwner: LifecycleOwner?,
) {
}

@BindingAdapter(value = ["app:swapSubItems", "app:viewodel", "app:fragmentLifecycleOwner"], requireAll = false)
fun RecyclerView.setSwapSubItems(
    swapItems: List<SwapItem>?,
    viewModel: QuickTaskBaseViewModel,
    fragmentLifecycleOwner: LifecycleOwner?,
) {
    layoutManager = LinearLayoutManager(context)
    adapter =
        GroupieAdapter().apply {
            swapItems?.let {
                generateSwapSubItems(
                    fragmentLifecycleOwner,
                    swapItems, viewModel,
                )
            }
        }
}

@BindingAdapter("isUserInputEnabled")
fun ViewPager2.setViewpagerSwipeGesture(isMasterOrderView: Boolean) {
    isUserInputEnabled = isMasterOrderView
}
