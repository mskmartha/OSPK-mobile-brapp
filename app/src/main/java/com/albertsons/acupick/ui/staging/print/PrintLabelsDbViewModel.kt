package com.albertsons.acupick.ui.staging.print

import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.databinding.ItemPrintLabelsHeaderBinding
import com.albertsons.acupick.databinding.PrintLabelsExpandedItemBinding
import com.albertsons.acupick.ui.BaseBindableViewModel
import com.albertsons.acupick.ui.LiveDataHelper
import com.albertsons.acupick.ui.ViewModelItem
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.ExpandableItem
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

/** Parent Print Label header Groupie binding. Has 1 [PrintLabelsSubRv] Groupie binding (as [printLabelsSubRv], which contains the list of bag label Groupie items. */
// TODO: Should probably be extending BaseBindableViewModel instead of BindableItem
class PrintLabelsHeaderItem(
    val headerUi: PrintLabelsHeaderUi,
    val fragmentViewModel: PrintLabelsViewModel,
    var lifecycleOwner: LifecycleOwner? = null,
) : BindableItem<ItemPrintLabelsHeaderBinding?>(), ExpandableItem {
    private var expandableGroup: ExpandableGroup? = null

    val printLabelsSubRv = PrintLabelsSubRv(
        subItemUiList = headerUi.subItemUiList,
        fragmentViewModel = fragmentViewModel,
        setChecked = ::setChecked
    )

    val isChecked = MutableLiveData<Boolean>()

    override fun setExpandableGroup(onToggleListener: ExpandableGroup) {
        expandableGroup = onToggleListener
    }

    override fun bind(viewBinding: ItemPrintLabelsHeaderBinding, position: Int) {
        viewBinding.apply {
            headerItem = this@PrintLabelsHeaderItem
            executePendingBindings()
        }
    }

    override fun getLayout() = R.layout.item_print_labels_header
    override fun initializeViewBinding(view: View) = ItemPrintLabelsHeaderBinding.bind(view)

    private fun setChecked(isChecked: Boolean) {
        this.isChecked.value = isChecked
    }
}

/** An Individual bag line item Groupie item. Included in [PrintLabelsSubRv] */
class PrintLabelsSubItemDbViewModel(
    val subItemUi: PrintLabelsSubItemUi,
    val fragmentViewModel: PrintLabelsViewModel,
    var lifecycleOwner: LifecycleOwner? = null,
    private val onCheckedChange: () -> Unit,
) : LiveDataHelper, BaseBindableViewModel() {

    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as PrintLabelsSubItemDbViewModel, R.layout.item_print_labels_item, lifecycleOwner) }
    }

    val isChecked = MutableLiveData<Boolean>(false)
    val bagOrToteNumber = subItemUi.bagOrToteNumber
    val isScanned = subItemUi.isScanned

    fun onSubGroupClicked() {
        isChecked.value = isChecked.value?.not()
        fragmentViewModel.selectedLabelIds.value = fragmentViewModel.selectedLabelIds.value.apply {
            if (isChecked.value == true) {
                this?.add(subItemUi.bagOrToteNumberRawValue)
            } else {
                this?.remove(subItemUi.bagOrToteNumberRawValue)
            }
        }?.distinct()?.toMutableList()
        onCheckedChange()
    }
}

/** The list of all labels ([PrintLabelsSubItemUi]) for a specific order as a Groupie item. A child of [PrintLabelsHeaderItem] */
// TODO: Should probably be extending BaseBindableViewModel instead of BindableItem
class PrintLabelsSubRv(
    private val subItemUiList: List<PrintLabelsSubItemUi>?,
    private val fragmentViewModel: PrintLabelsViewModel,
    private val setChecked: (Boolean) -> Unit,
    var lifecycleOwner: LifecycleOwner? = null,
) : BindableItem<PrintLabelsExpandedItemBinding?>() {

    val subItemList = subItemUiList?.map { label ->
        PrintLabelsSubItemDbViewModel(
            subItemUi = label,
            fragmentViewModel = fragmentViewModel,
            lifecycleOwner = lifecycleOwner,
            onCheckedChange = ::onCheckChanged,
        )
    }?.toMutableList() ?: mutableListOf()

    override fun bind(viewBinding: PrintLabelsExpandedItemBinding, position: Int) {
        viewBinding.lifecycleOwner = lifecycleOwner

        subItemList.forEach {
            it.lifecycleOwner = lifecycleOwner
        }

        if (subItemUiList != null) {
            viewBinding.labelItemsListRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                val groupAdapter = GroupAdapter<GroupieViewHolder>()
                val section = Section()
                section.update(subItemList)
                groupAdapter.add(section)
                adapter = groupAdapter
            }
        }
        viewBinding.executePendingBindings()
    }

    override fun getLayout() = R.layout.print_labels_expanded_item
    override fun initializeViewBinding(view: View) = PrintLabelsExpandedItemBinding.bind(view)

    private fun onCheckChanged() {
        setChecked(subItemList.all { it.isChecked.value == true })
    }
}

@BindingAdapter(value = ["app:printItems", "app:isExpanded", "app:customLifecycleOwner"])
fun RecyclerView.printItems(printItems: List<PrintLabelsHeaderItem>?, isExpanded: Boolean?, lifecycleOwner: LifecycleOwner?) {
    if (printItems != null && isExpanded != null && lifecycleOwner != null) {
        layoutManager = LinearLayoutManager(context)

        fun generateGroup() =
            printItems.map { data ->
                // TODO: Find a better way to pass this than a mutable property
                data.lifecycleOwner = lifecycleOwner
                data.printLabelsSubRv.lifecycleOwner = lifecycleOwner
                ExpandableGroup(data, isExpanded).apply {
                    add(Section(data.printLabelsSubRv))
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

@BindingAdapter(value = ["app:setCheckBoxStates", "app:isCheckBoxEnabled"])
fun AppCompatImageView.setCheckBoxStates(isChecked: Boolean, isCheckBoxEnabled: Boolean = false) {
    if (isChecked) {
        if (isCheckBoxEnabled) {
            setImageResource(R.drawable.ic_checkbox_checked_state)
        } else {
            setImageResource(R.drawable.ic_checkbox_checked_disabled_state)
        }
    } else {
        setImageResource(R.drawable.ic_checkbox_unchecked_state)
    }
}

@BindingAdapter(value = ["app:setUnPickAlertMessage"])
fun AppCompatTextView.setUnPickAlertMessage(sellByType: SellByType) {
    text = when (sellByType) {
        SellByType.Prepped, SellByType.Weight -> context.getString(R.string.all_scans_must_be_unpicked)
        SellByType.PriceWeighted -> context.getString(R.string.unpick_alert_message_pw_item)
        else -> context.getString(R.string.unpick_action_can_not_be_undone)
    }
}
