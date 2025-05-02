package com.albertsons.acupick.ui.totes

import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import androidx.core.view.updatePadding
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.ToteEstimate
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.databinding.ItemTotesBinding
import com.albertsons.acupick.databinding.ItemTotesExpandedItemsBinding
import com.albertsons.acupick.databinding.TotesExpandedItemBinding
import com.albertsons.acupick.ui.bindingadapters.setVisibilityGoneIfTrue
import com.albertsons.acupick.ui.util.dpToPx
import com.albertsons.acupick.ui.util.notZeroOrNull
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.ExpandableItem
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

class TotesHeaderItemDbViewModel(
    private val totesUiList: List<TotesUi>?,
    private val activityDto: ActivityDto?,
    val totesListSubRv: TotesListSubRv?,
    private val toteEstimate: ToteEstimate?,
) : BindableItem<ItemTotesBinding?>(), ExpandableItem {
    private var expandableGroup: ExpandableGroup? = null

    override fun setExpandableGroup(onToggleListener: ExpandableGroup) {
        expandableGroup = onToggleListener
    }

    override fun bind(viewBinding: ItemTotesBinding, position: Int) {

        viewBinding.apply {
            isExpanded = expandableGroup?.isExpanded
            totesEstimate = toteEstimate
            mfcTotesHeader.isVisible = activityDto?.isMultiSource == true
            mfcTotesEstimate.isVisible = activityDto?.isMultiSource == true
            totesUiList?.map { container ->
                totesUi = container

                val startMargin = customerName.marginStart
                customerName.text = container.getCustomerName(startMargin)

                val isTotesScanned = totesListSubRv?.totes?.isNotEmpty() == true
                totesInUseHeader.isVisible = isTotesScanned
                if (isTotesScanned) {
                    toteHeaderCL.updatePadding(16.dpToPx, 0, 16.dpToPx, 0)
                }
            }
        }
    }

    override fun getLayout() = R.layout.item_totes
    override fun initializeViewBinding(view: View) = ItemTotesBinding.bind(view)
}

class TotesSubItemDbViewModel(
    private val toteUi: TotesSubUi,
) : BindableItem<ItemTotesExpandedItemsBinding?>() {
    override fun bind(viewBinding: ItemTotesExpandedItemsBinding, position: Int) {
        viewBinding.totesSubUI = toteUi
    }

    override fun getLayout() = R.layout.item_totes_expanded_items
    override fun initializeViewBinding(view: View) = ItemTotesExpandedItemsBinding.bind(view)
}

/** recyclerview that contains all totes for specific order */
class TotesListSubRv(
    val totes: List<TotesSubUi>?,
) : BindableItem<TotesExpandedItemBinding?>() {
    override fun bind(viewBinding: TotesExpandedItemBinding, position: Int) {
        if (totes != null) {
            viewBinding.toteItemsListRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                val groupAdapter = GroupAdapter<GroupieViewHolder>()
                val section = Section()
                section.update(
                    totes.map { tote ->
                        TotesSubItemDbViewModel(tote)
                    }
                )
                groupAdapter.add(section)
                adapter = groupAdapter
            }
        }
    }

    override fun getLayout() = R.layout.totes_expanded_item
    override fun initializeViewBinding(view: View) = TotesExpandedItemBinding.bind(view)
}

@BindingAdapter(value = ["app:containerList", "app:isExpanded"])
fun RecyclerView.setTotes(toteItems: List<TotesHeaderItemDbViewModel>?, isExpanded: Boolean?) {
    if (toteItems != null && isExpanded != null) {
        layoutManager = LinearLayoutManager(context)

        fun generateGroup() =
            toteItems.map { data ->
                // val isSubsEmpty = data.totesListSubRv?.totes?.count() == 0
                ExpandableGroup(data, isExpanded).apply {
                    add(Section(data.totesListSubRv))
                }
            }

        @Suppress("UNCHECKED_CAST")
        // If adapter can be cast to GroupieAdapter, update with new data
        (adapter as? GroupAdapter<GroupieViewHolder>)?.apply {
            // Update adapter with new info.
            clear()
            addAll(generateGroup())
        } ?: run {
            //  Create new adapter
            layoutManager = LinearLayoutManager(context)
            adapter = GroupAdapter<GroupieViewHolder>().apply { addAll(generateGroup()) }
        }
    }
}

@BindingAdapter("app:setStorageTypeImg")
fun AppCompatImageView.setStorageTypeImg(storageType: StorageType) {
    setImageDrawable(
        ContextCompat.getDrawable(
            context,
            when (storageType) {
                StorageType.AM -> R.drawable.ic_ambient
                StorageType.CH -> R.drawable.ic_chilled
                StorageType.FZ -> R.drawable.ic_frozen
                StorageType.HT -> R.drawable.ic_hot
            }
        )
    )
}

@BindingAdapter("app:setOrderType")
fun AppCompatTextView.setOrderType(orderType: OrderType) {
    text = when (orderType) {
        OrderType.FLASH -> context.getString(R.string.flash_order)
        OrderType.REGULAR -> context.getString(R.string.regular_order)
        OrderType.EXPRESS -> context.getString(R.string.express_order)
        OrderType.FLASH3P -> context.getString(R.string.partnerpick_order)
        else -> context.getString(R.string.regular_order)
    }
}

@BindingAdapter("app:setRadiusByExpanded")
fun ConstraintLayout.setRadiusByExpanded(isExpanded: Boolean?) {
    if (isExpanded == true) {
        setBackgroundResource(R.drawable.drawable_radius_top_4dp)
    } else {
        setBackgroundResource(R.drawable.drawable_radius_4)
        backgroundTintList = context.getColorStateList(R.color.white)
    }
}

@BindingAdapter("app:setDrawableByExpanded")
fun AppCompatImageView.setDrawableByExpanded(isExpanded: Boolean?) {
    setImageResource(
        if (isExpanded == true) {
            R.drawable.ic_collapse
        } else {
            R.drawable.ic_expand
        }
    )
}

@BindingAdapter("app:setVisibilityByExpanded")
fun View.setVisibilityByExpanded(isExpanded: Boolean?) {
    visibility =
        if (isExpanded == true) {
            View.VISIBLE
        } else {
            View.GONE
        }
}

@BindingAdapter(value = ["app:itemTypeCount", "app:storageType"])
fun AppCompatTextView.setItemTypeCountText(totesUi: TotesUi, storageType: StorageType) {
    val itemTypes = totesUi.getStorageItemTypes()?.containsKey(storageType) ?: false
    setVisibilityGoneIfTrue(!itemTypes)
    text = totesUi.getStorageItemTypes()?.get(storageType)?.toInt().toString()
}

@BindingAdapter(value = ["app:itemTypeImage", "app:storageType"])
fun AppCompatImageView.setItemTypeImage(totesUi: TotesUi, storageType: StorageType) {
    val itemTypes = totesUi.getStorageItemTypes()?.containsKey(storageType) ?: false
    setVisibilityGoneIfTrue(!itemTypes)
}

@BindingAdapter("app:totalItems")
fun AppCompatTextView.setTotalItems(totesUi: TotesUi) {
    text = totesUi.getTotalCount(totesUi.orderNumber).toString()
}

@BindingAdapter(value = ["app:isMfcOrder", "app:setToteType", "app:setToteCount"])
fun AppCompatTextView.setToteData(isMfcOrder: Boolean? = null, totesType: List<TotesSubUi>, toteCount: Int) {
    isMfcOrder?.let { mfcOrder ->
        text = when (mfcOrder) {
            true -> totesType.groupingBy { it.storageType }.eachCount().toList()
                .joinToString(", ") { it.first?.name + " (" + it.second + ")" }
            else -> toteCount.toString()
        }
    } ?: toteCount.toString()
}

@BindingAdapter("app:setToteTypeHeaderInfo")
fun AppCompatTextView.setToteTypeHeaderInfo(isMfcOrder: Boolean? = null) {
    isMfcOrder?.let {
        text = when (it) {
            true -> context.resources.getString(R.string.tote_types)
            false -> context.resources.getString(R.string.number_of_totes_header)
        }
    } ?: context.resources.getString(R.string.number_of_totes_header)
}

@BindingAdapter("app:setToteEstimate")
fun AppCompatTextView.setToteEstimate(toteEstimate: ToteEstimate?) {
    toteEstimate?.let { totes ->
        text = if (totes.ambient.notZeroOrNull() && totes.chilled.notZeroOrNull()) {
            context.resources.getString(R.string.tote_type_needed_both_am_and_ch, totes.ambient, totes.chilled)
        } else if (totes.chilled.notZeroOrNull()) {
            context.resources.getString(R.string.tote_types_needed_title_ch, totes.chilled)
        } else if (totes.ambient.notZeroOrNull()) {
            context.resources.getString(R.string.tote_types_needed_title_am, totes.ambient)
        } else ""
    }
}

/*class DividerItemDecorator(private val divider: Drawable?) : RecyclerView.ItemDecoration() {
    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val marginHorizontalInPx = parent.resources.getDimensionPixelOffset(R.dimen.inset_drawable_margin_horizontal)

        val dividerLeft = parent.paddingLeft + marginHorizontalInPx
        val dividerRight = (parent.width - parent.paddingRight) - marginHorizontalInPx
        val childCount = parent.childCount
        for (i in 0..childCount - 2) {
            val child: View = parent.getChildAt(i)
            val params =
                child.layoutParams as RecyclerView.LayoutParams
            val dividerTop: Int = child.bottom + params.bottomMargin
            val dividerBottom = dividerTop + (divider?.intrinsicHeight ?: 0)
            divider?.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom)
            divider?.draw(canvas)
        }
    }
}*/
