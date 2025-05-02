package com.albertsons.acupick.ui.bindingadapters

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ItemNoWineBoxesScannedBinding
import com.albertsons.acupick.databinding.ItemScannedBoxCardBinding
import com.albertsons.acupick.databinding.ItemScannedWineBoxBinding
import com.albertsons.acupick.ui.models.ZoneBagCountUI
import com.albertsons.acupick.ui.staging.winestaging.BoxSizeUiHeader
import com.albertsons.acupick.ui.util.StringIdHelper
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

@BindingAdapter(value = ["app:zoneBoxScannedItems", "totalScannedItems", "totalBoxCount"], requireAll = true)
fun RecyclerView.setWineStaging(scannedBoxItems: List<ZoneBagCountUI>?, totalScannedItems: Int, totalBoxCount: Int) {
    if (scannedBoxItems == null) return
    layoutManager = LinearLayoutManager(context)

    @Suppress("UNCHECKED_CAST")
    // If adapter can be cast to GroupieAdapter, update with new data
    adapter = GroupieAdapter().apply {
        clear()
        updateAsync(mutableListOf(generateScannedBoxGroup(scannedBoxItems, totalScannedItems, totalBoxCount)))
    }
}

private fun generateScannedBoxGroup(scannedBoxItems: List<ZoneBagCountUI>, totalScannedItems: Int, totalBoxCount: Int) =
    Section().apply {
        add(BoxSizeUiHeader(3))
        add(ScannedBoxCard(scannedBoxItems, totalScannedItems, totalBoxCount))
    }

class BoxScanUiItem(private val item: ZoneBagCountUI) : BindableItem<ItemScannedWineBoxBinding>() {
    override fun initializeViewBinding(view: View): ItemScannedWineBoxBinding = ItemScannedWineBoxBinding.bind(view)
    override fun getLayout(): Int = R.layout.item_scanned_wine_box
    override fun bind(viewBinding: ItemScannedWineBoxBinding, position: Int) {
        viewBinding.location.text = item.zone
        viewBinding.boxCount.text = StringIdHelper.Plural(R.plurals.wine_box_count, item.scannedBagCount).getString(viewBinding.root.context)
    }
}
class NoBoxesScanned : BindableItem<ItemNoWineBoxesScannedBinding>() {
    override fun initializeViewBinding(view: View): ItemNoWineBoxesScannedBinding = ItemNoWineBoxesScannedBinding.bind(view)
    override fun getLayout(): Int = R.layout.item_no_wine_boxes_scanned
    override fun bind(viewBinding: ItemNoWineBoxesScannedBinding, position: Int) {}
}

class ScannedBoxCard(private val scannedBoxItems: List<ZoneBagCountUI>?, private val scannedItems: Int, private val boxCount: Int) : BindableItem<ItemScannedBoxCardBinding>() {
    override fun initializeViewBinding(view: View): ItemScannedBoxCardBinding = ItemScannedBoxCardBinding.bind(view)
    override fun getLayout(): Int = R.layout.item_scanned_box_card
    override fun bind(binding: ItemScannedBoxCardBinding, position: Int) {
        binding.apply {
            totalScannedItems = scannedItems
            totalBoxCount = boxCount
            binding.ambientRecyclerView.apply {
                layoutManager = LinearLayoutManager(binding.root.context)
                adapter = GroupieAdapter().apply {
                    if (scannedBoxItems.isNullOrEmpty()) {
                        addAll(mutableListOf(NoBoxesScanned()))
                    } else {
                        addAll(scannedBoxItems.map { BoxScanUiItem(it) })
                    }
                }
            }
        }
    }
}
