package com.albertsons.acupick.ui.substitute.alternativelocation

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ItemAlternativeLocationBinding
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

class AlternativeLocationsDbViewModel(
    val alternativeLocation: String,
) : BindableItem<ItemAlternativeLocationBinding?>() {
    override fun bind(viewBinding: ItemAlternativeLocationBinding, position: Int) {
        viewBinding.apply { dbViewmodel = this@AlternativeLocationsDbViewModel }
    }

    override fun getLayout() = R.layout.item_alternative_location

    override fun initializeViewBinding(view: View) = ItemAlternativeLocationBinding.bind(view)
}

@BindingAdapter("app:alternativeLocations")
fun RecyclerView.setAlternativeLocations(items: List<String>?) {
    if (items.isNotNullOrEmpty()) {
        layoutManager = LinearLayoutManager(context)
        val groupAdapter = GroupAdapter<GroupieViewHolder>()
        items?.map { location ->
            groupAdapter.add(Section(AlternativeLocationsDbViewModel(location)))
        }
        adapter = groupAdapter
    }
}
