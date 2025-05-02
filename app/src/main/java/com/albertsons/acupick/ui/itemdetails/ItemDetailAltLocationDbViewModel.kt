package com.albertsons.acupick.ui.itemdetails

import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.response.ItemLocationDto
import com.albertsons.acupick.databinding.ItemAlternativeLocationItemDetailBinding
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.util.asItemLocation
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

class ItemDetailAltLocationDbViewModel(
    val alternativeLocation: String,
) : BindableItem<ItemAlternativeLocationItemDetailBinding?>() {
    override fun bind(viewBinding: ItemAlternativeLocationItemDetailBinding, position: Int) {
        viewBinding.apply { detailDbViewmodel = this@ItemDetailAltLocationDbViewModel }
    }

    override fun getLayout() = R.layout.item_alternative_location_item_detail

    override fun initializeViewBinding(view: View) = ItemAlternativeLocationItemDetailBinding.bind(view)
}

@BindingAdapter("app:altLocations")
fun RecyclerView.setAltLocations(items: List<ItemLocationDto>?) {
    layoutManager = LinearLayoutManager(context)
    val groupAdapter = GroupAdapter<GroupieViewHolder>()
    if (items.isNotNullOrEmpty()) {
        items?.map { location ->
            groupAdapter.add(Section(ItemDetailAltLocationDbViewModel(location.itemAddressDto?.asItemLocation(context) ?: context.getString(R.string.alt_location_not_available))))
        }
    } else {
        groupAdapter.add(Section(ItemDetailAltLocationDbViewModel(context.getString(R.string.alt_location_not_available))))
    }
    adapter = groupAdapter
}

@BindingAdapter("setAlternateLocations")
fun AppCompatTextView.setAlternateLocations(items: List<ItemLocationDto>?) {
    text = if (items.isNotNullOrEmpty()) {
        val locationOne = items?.getOrNull(0)?.itemAddressDto?.asItemLocation(context) ?: ""
        val locationTwo = items?.getOrNull(1)?.itemAddressDto?.asItemLocation(context) ?: ""
        context.getString(R.string.alternateLocations, locationOne, locationTwo)
    } else {
        context.getString(R.string.alt_location_not_available)
    }
}
