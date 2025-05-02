package com.albertsons.acupick.ui.staging

import android.os.Parcelable
import androidx.annotation.Keep
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
import com.albertsons.acupick.ui.models.StagingUI
import com.albertsons.acupick.ui.models.ToteUI
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.android.parcel.Parcelize

class AddBagsItemViewModel(
    val item: ToteUI,
    val stagingUI: StagingUI?,
    val isCustomerPreferBag: Boolean,
    private val updateUi: () -> Unit,
    var fragmentViewLifecycleOwner: LifecycleOwner? = null,
) : BaseBindableViewModel() {

    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as AddBagsItemViewModel, R.layout.item_add_bag_count, fragmentViewLifecycleOwner) }
    }

    val toteId = if (stagingUI?.isOrderMultiSource == true) item.toteId?.takeLast(StagingPart2PagerViewModel.MFC_TOTE_ID_UI_LENGTH) else item.toteId
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

@BindingAdapter(value = ["app:setTotesListToAddBag", "app:storageType", "app:fragmentViewLifecycleOwner"], requireAll = true)
fun RecyclerView.setTotesListToAddBag(
    items: List<AddBagsItemViewModel>?,
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
    items: List<AddBagsItemViewModel>,
    storageType: StorageType?,
) = Section().apply {
    update(
        items.filter { it.item.storageType == storageType }
    )
}

@Parcelize
@Keep
data class BagZoneData(
    val zone: String,
    val containerId: String,
) : Parcelable
