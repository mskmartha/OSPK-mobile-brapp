package com.albertsons.acupick.ui.itemdetails

import android.os.Parcelable
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.response.PickedItemUpcDto
import com.albertsons.acupick.data.model.response.ShortedItemUpcDto
import com.albertsons.acupick.databinding.RowUnpickHeaderBinding
import com.albertsons.acupick.ui.BaseBindableViewModel
import com.albertsons.acupick.ui.LiveDataHelper
import com.albertsons.acupick.ui.ViewModelItem
import com.albertsons.acupick.ui.picklistitems.TOTE_UI_COUNT
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.parcelize.Parcelize

/** Wrapper around the different types of an item action to to differentiate between them and the data they represent */

sealed class ItemActionBackingType : Parcelable {
    @Parcelize
    data class Pick(val pickedItemUpcDto: PickedItemUpcDto) : ItemActionBackingType()
    @Parcelize
    data class Substitution(val pickedItemUpcDto: PickedItemUpcDto) : ItemActionBackingType()
    @Parcelize
    data class Short(val shortedItemUpcDto: ShortedItemUpcDto) : ItemActionBackingType()
}

/** Data needed to render a row of the Item Action UI + [backingType] to use for logic when selected and undo cta tapped */
data class ItemAction(
    val qty: String,
    val description: String,
    val containerId: String,
    val isSubstitution: Boolean,
    val isIssueScanned: Boolean = false,
    val backingType: ItemActionBackingType,
    val upcPlu: String,
    val imageUrl: String,
    val sellByType: SellByType,
    val isPWItem: Boolean
)

class ItemActonHeaderItem(
    val toteName: String,
    val isFirstItem: Boolean
) : BindableItem<RowUnpickHeaderBinding?>() {

    val isChecked = MutableLiveData<Boolean>()
    override fun bind(viewBinding: RowUnpickHeaderBinding, position: Int) {
        viewBinding.apply {
            headerItem = this@ItemActonHeaderItem
            executePendingBindings()
        }
    }
    override fun getLayout() = R.layout.row_unpick_header
    override fun initializeViewBinding(view: View) = RowUnpickHeaderBinding.bind(view)
}

/** Supports rendering pick, substitution, and short data (via [itemAction]) using a single unified template, [R.layout.item_details_recycler_item] */
class ItemActionDbViewModel(
    val itemAction: ItemAction,
    val onCheckedChange: () -> Unit,
    var lifecycleOwner: LifecycleOwner? = null
) : LiveDataHelper, BaseBindableViewModel() {
    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as ItemActionDbViewModel, R.layout.row_un_pick, lifecycleOwner) }
    }

    val isChecked = MutableLiveData(itemAction.sellByType == SellByType.Prepped || itemAction.sellByType == SellByType.Weight || itemAction.isPWItem || itemAction.sellByType == SellByType.PriceEach)
    val checkBoxDisable = itemAction.isPWItem || itemAction.sellByType == SellByType.PriceEach
    val qty = itemAction.qty
    val description = itemAction.description
    val imageUrl = itemAction.imageUrl
    val upcPlu = itemAction.upcPlu
    val tote = itemAction.containerId.takeLast(TOTE_UI_COUNT)
    val isSubstitution = itemAction.isSubstitution
    val isIssueScanned = itemAction.isIssueScanned
    val subVisible = if (itemAction.isSubstitution || itemAction.isIssueScanned) View.VISIBLE else View.GONE
    val sellByType = itemAction.sellByType
    val isClickable = MutableLiveData(
        itemAction.sellByType != SellByType.Prepped &&
            itemAction.sellByType != SellByType.Weight &&
            itemAction.sellByType != SellByType.PriceWeighted &&
            itemAction.sellByType != SellByType.PriceEach
    )

    fun toggleCheckBox() {
        isChecked.value = isChecked.value?.not()
        onCheckedChange()
    }
}

@BindingAdapter(value = ["app:pickedItemVms", "app:itemViewModel", "app:fragmentViewLifecycleOwner"])
fun RecyclerView.setItemList(itemActionDbViewModels: List<ItemActionDbViewModel>?, viewModel: ItemDetailsViewModel?, fragmentViewLifecycleOwner: LifecycleOwner? = null) {
    if (itemActionDbViewModels != null && viewModel != null) {

        val groupedList = itemActionDbViewModels.groupBy { it.tote }
        fun generateGroup(adapter: GroupAdapter<GroupieViewHolder>) =
            groupedList.entries.forEachIndexed { index, map ->
                map.key.let {
                    adapter.add(createHeaderSection(ItemActonHeaderItem(it, index == 0)))
                }
                map.value.let {
                    it.forEach { item -> item.lifecycleOwner = fragmentViewLifecycleOwner }
                    adapter.add(createSection(it))
                }
            }

        @Suppress("UNCHECKED_CAST")
        (adapter as? GroupAdapter<GroupieViewHolder>)?.apply {
            // Update adapter with new info.
            clear()
            generateGroup(this)
        } ?: run {
            // Create new adapter
            layoutManager = LinearLayoutManager(context)
            adapter = GroupAdapter<GroupieViewHolder>().apply { generateGroup(this) }
        }
    }
}

@BindingAdapter(value = ["app:setTopMargin"])
fun ConstraintLayout.setTopMargin(dimen: Float) {
    val layoutParams = layoutParams as MarginLayoutParams
    layoutParams.topMargin = dimen.toInt()
}

private fun createHeaderSection(items: ItemActonHeaderItem): Section = Section(items)

private fun createSection(items: List<ItemActionDbViewModel>): Section = Section(items.map { Section(it) })

@BindingAdapter(value = ["isSubstitution", "isIssueScanned"])
fun TextView.setSubLabel(isSubstitution: Boolean, isIssueScanned: Boolean) {
    if (isSubstitution)
        this.text = resources.getString(R.string.substituted)
    else if (isIssueScanned)
        this.text = resources.getString(R.string.issue_reported_label)
}
