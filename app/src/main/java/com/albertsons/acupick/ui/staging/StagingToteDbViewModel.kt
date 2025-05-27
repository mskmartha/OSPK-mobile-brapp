package com.albertsons.acupick.ui.staging

import android.content.res.ColorStateList
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.ui.BaseBindableViewModel
import com.albertsons.acupick.ui.ViewModelItem
import com.albertsons.acupick.ui.arrivals.destage.OrderCompletionState
import com.albertsons.acupick.ui.models.StagingUI
import com.albertsons.acupick.ui.models.ToteUI
import com.albertsons.acupick.ui.staging.StagingPart2PagerViewModel.Companion.MFC_TOTE_ID_UI_LENGTH
import com.albertsons.acupick.ui.util.orFalse
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

class StagingToteDbViewModel(
    val item: ToteUI,
    val stagingUI: StagingUI?,
    val isCustomerPreferBag: Boolean,
    private val updateUi: () -> Unit,
    var fragmentViewLifecycleOwner: LifecycleOwner? = null,
) : BaseBindableViewModel() {

    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as StagingToteDbViewModel, R.layout.item_tote_to_bag, fragmentViewLifecycleOwner) }
    }

    val toteId = if (stagingUI?.isOrderMultiSource == true) item.toteId?.takeLast(MFC_TOTE_ID_UI_LENGTH) else item.toteId
    val bagCount = MutableLiveData(item.intialBagCount)
    val looseCount = MutableLiveData(item.intialLooseCount)
    val total: Int
        get() = (bagCount.value ?: 0) + (looseCount.value ?: 0)

    init {
        bagCount.observeForever {
            updateUi()
        }

        looseCount.observeForever {
            updateUi()
        }
    }
}

@BindingAdapter(value = ["app:totesList", "app:storageType", "app:fragmentViewLifecycleOwner"], requireAll = true)
fun RecyclerView.setTotesList(
    items: List<StagingToteDbViewModel>?,
    storageType: StorageType,
    fragmentViewLifecycleOwner: LifecycleOwner,
) {
    if (items == null) return

    items.forEach {
        it.fragmentViewLifecycleOwner = fragmentViewLifecycleOwner
    }

    layoutManager = LinearLayoutManager(context)

    @Suppress("UNCHECKED_CAST")
    // If adapter can be cast to GroupieAdapter, update with new data
    (adapter as? GroupAdapter<GroupieViewHolder>)?.apply {
        // Update adapter with new info.
        clear()
        add(generateSection(items, storageType))
    }.run {
        // Create new adapter
        layoutManager = LinearLayoutManager(context)
        adapter = GroupAdapter<GroupieViewHolder>().apply { add(generateSection(items, storageType)) }
    }
}

private fun generateSection(
    items: List<StagingToteDbViewModel>,
    storageType: StorageType?,
) = Section().apply {
    update(
        items.filter { it.item.storageType == storageType }
    )
}

@BindingAdapter(value = ["app:orderCompletionState", "app:areAllOrdersCompleted", "app:isLastTab", "app:isMfc", "app:isCustomerPreferBag", "app:isAnyCustomerPreferNoBag"], requireAll = true)
fun AppCompatTextView.setupAddBagCountCta(
    orderCompletionState: OrderCompletionState?,
    areAllOrdersCompleted: Boolean?,
    isLastTab: Boolean?,
    isMfc: Boolean,
    isCustomerPreferBag: Boolean?,
    isAnyCustomerPreferNoBag: Boolean?,
) {
    val isButtonEnabled = if (isMfc || isCustomerPreferBag == false) true else if (isLastTab == true) areAllOrdersCompleted == true else orderCompletionState?.isComplete == true
    val btnColorRes = if (isButtonEnabled) R.color.semiLightBlue else R.color.coffeeLight
    val textColorRes = if (isButtonEnabled) R.color.white else R.color.grey_550
    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, btnColorRes))
    setTextColor(ContextCompat.getColor(context, textColorRes))
    text = context.getString(if (isLastTab == true) if (isAnyCustomerPreferNoBag.orFalse()) R.string.complete_count else R.string.complete_bag_count else R.string.next)
}


