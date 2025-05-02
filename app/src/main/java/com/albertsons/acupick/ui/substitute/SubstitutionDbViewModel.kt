package com.albertsons.acupick.ui.substitute

import android.app.Application
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ImageSizePreset
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.infrastructure.utils.asApplication
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.BaseBindableViewModel
import com.albertsons.acupick.ui.LiveDataHelper
import com.albertsons.acupick.ui.ViewModelItem
import com.albertsons.acupick.ui.bottomsheetdialog.SubstituteConfirmationViewModel
import com.albertsons.acupick.ui.util.CenterLayoutManager
import com.albertsons.acupick.ui.util.getFormattedValue
import com.albertsons.acupick.ui.util.sizedImageUrl
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

class SubstitutionDbViewModel(
    val app: Application,
    private val substitutionItem: SubstitutionLocalItem,
    private val fragmentViewLifecycleOwner: LifecycleOwner,
    val viewModel: SubstituteConfirmationViewModel,
) : LiveDataHelper, BaseBindableViewModel() {
    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as SubstitutionDbViewModel, R.layout.item_substitution, fragmentViewLifecycleOwner) }
    }

    val sellByType = substitutionItem.item.sellByWeightInd

    val isScannedWeightToShow = viewModel.isIssueScanning.value == true && sellByType != null && sellByType != SellByType.RegularItem &&
        sellByType != SellByType.PriceEach && sellByType != SellByType.Each

    val scannedWeight = substitutionItem.itemWeight ?: ""

    val substituteImageUrl = if (substitutionItem.selectedVariant != null) substitutionItem.selectedVariant.imageUrl else substitutionItem.item.sizedImageUrl(ImageSizePreset.ItemDetails)

    val barcodeType = substitutionItem.itemBarcodeType

    val enteredQuantity = when {
        viewModel.isDisplayType3PW.value == true && substitutionItem.orderedWeightWithUom.isNotNullOrEmpty() && !substitutionItem.isIssueScanned -> substitutionItem.orderedWeightWithUom
        substitutionItem.orderedByWeight && !substitutionItem.isIssueScanned -> "${substitutionItem.itemWeight} ${substitutionItem.unitOfMeasure ?: app.getString(R.string.uom_default)}"
        else -> "${substitutionItem.quantity.toInt()}"
    }
    val isCustomerChosenItemAvailable = substitutionItem.isCustomerChosenItemAvailable

    val substituteDescription = if (substitutionItem.selectedVariant != null) substitutionItem.selectedVariant.itemDes else substitutionItem.item.itemDesc.orEmpty()

    val upcOrPlu = if (substitutionItem.orderedByWeight && barcodeType is BarcodeType.Item.Weighted) {
        app.getString(R.string.item_details_plu_weighted_format, barcodeType.plu, unitOfMeasureOrDefault(substitutionItem.unitOfMeasure))
    } else {
        barcodeType?.getFormattedValue(app)
    }

    val isCustomerBagPreference = viewModel.isCustomerBagPreference.value

    fun onDeleteClicked() {
        viewModel.showDeleteSubItemDialog(substitutionItem)
    }

    private fun unitOfMeasureOrDefault(unitOfMeasure: String?) = if (unitOfMeasure.isNotNullOrBlank()) unitOfMeasure else app.getString(R.string.uom_default)
}

/** subList RecyclerView binding adapter to hookup the vm (ctas), db vm (ui+piping actions to vm), add setup the groupie adapter */
@BindingAdapter("app:subListItems", "app:fragmentViewLifecycleOwner", "app:viewModel")
fun RecyclerView.setSubItems(subItems: List<SubstitutionLocalItem>?, fragmentViewLifecycleOwner: LifecycleOwner, viewModel: SubstituteConfirmationViewModel) {
    if (subItems == null) return

    @Suppress("UNCHECKED_CAST")
    // If adapter already exists, then cast and re-use
    (adapter as? GroupAdapter<GroupieViewHolder>)?.apply {
        clear()
        add(generateSubListSection(subItems, fragmentViewLifecycleOwner, viewModel, context.asApplication()))
    } ?: run {
        // Otherwise; create new adapter
        layoutManager = CenterLayoutManager(context, RecyclerView.VERTICAL, false)
        adapter = GroupAdapter<GroupieViewHolder>().apply {
            add(generateSubListSection(subItems, fragmentViewLifecycleOwner, viewModel, context.asApplication()))
        }
    }
}

private fun generateSubListSection(subItems: List<SubstitutionLocalItem>, fragmentViewLifecycleOwner: LifecycleOwner, viewModel: SubstituteConfirmationViewModel, app: Application): Section {
    val section = Section()
    section.update(
        subItems.map { item ->
            SubstitutionDbViewModel(
                app = app,
                substitutionItem = item,
                fragmentViewLifecycleOwner = fragmentViewLifecycleOwner,
                viewModel = viewModel
            )
        }
    )
    return section
}
