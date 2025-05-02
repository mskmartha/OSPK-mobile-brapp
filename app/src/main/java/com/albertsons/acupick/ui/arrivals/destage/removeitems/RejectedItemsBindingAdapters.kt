package com.albertsons.acupick.ui.arrivals.destage.removeitems

import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.ui.util.StringIdHelper

@BindingAdapter("app:storageType")
fun ImageView.setStorageIcon(storageType: StorageType?) {
    setImageDrawable(
        AppCompatResources.getDrawable(
            context,
            when (storageType ?: StorageType.AM) {
                StorageType.AM -> R.drawable.ic_ambient_on
                StorageType.CH -> R.drawable.ic_chilled_on
                StorageType.FZ -> R.drawable.ic_frozen_on
                StorageType.HT -> R.drawable.ic_hot_on
            }
        )
    )
}

@BindingAdapter("app:storageTypeIcon")
fun ImageView.setStorageTypeIcon(storageType: StorageType?) {
    storageType?.let {
        setImageDrawable(
            AppCompatResources.getDrawable(
                context,
                when (it) {
                    StorageType.AM -> R.drawable.ic_ambient
                    StorageType.CH -> R.drawable.ic_chilled
                    StorageType.FZ -> R.drawable.ic_frozen
                    StorageType.HT -> R.drawable.ic_hot
                }
            )
        )
    }
}

@BindingAdapter("app:storageTypeLabel")
fun TextView.setStorageTypeLabel(storageType: StorageType?) {
    storageType?.let {
        text = when (it) {
            StorageType.FZ -> context.getString(R.string.storage_type_frozen)
            StorageType.CH -> context.getString(R.string.storage_type_chilled)
            StorageType.AM -> context.getString(R.string.storage_type_ambient)
            StorageType.HT -> context.getString(R.string.storage_type_hot)
            else -> context.getString(R.string.storage_type_ambient)
        }
    }
}

@BindingAdapter(value = ["app:isRemoved", "app:unWantedItemsCount"])
fun TextView.setUnwantedItems(isRemoved: Boolean, unWantedItemsCount: Int) {
    if (isRemoved) {
        text = context.getString(R.string.items_removed)
        setTextColor(ContextCompat.getColor(context, R.color.grey_700))
        isClickable = false
    } else {
        text = StringIdHelper.Plural(R.plurals.rejected_1pl_items_plural, unWantedItemsCount).getString(context)
        setTextColor(ContextCompat.getColor(context, R.color.semiLightBlue))
        isClickable = true
    }
}

@BindingAdapter(value = ["app:vanId"])
fun TextView.setDriverLabel(vanId: String?) {
    vanId?.let {
        text = context.getString(R.string.van_label, vanId)
    }
}

@BindingAdapter(value = ["app:itemType", "app:displayType", "app:upcOrPlu"])
fun TextView.formatUpcOrPlu(type: SellByType? = SellByType.RegularItem, displayType: Int, upcOrPlu: String?) {
    if (type != null && upcOrPlu != null) {
        val convertedupcOrPlu = convertUPCSentFromendToDisplayedPLUOrUpc(upcOrPlu, displayType, type)
        text = when (type) {
            SellByType.Each ->
                context.getString(R.string.item_details_plu_format, convertedupcOrPlu)
            SellByType.Weight ->
                if (displayType == 3) {
                    context.getString(R.string.item_details_plu_weighted_format, convertedupcOrPlu, context.getString(R.string.uom_default))
                } else {
                    context.getString(R.string.item_details_plu_format, convertedupcOrPlu)
                }
            SellByType.RegularItem, SellByType.PriceEachUnique, SellByType.PriceScaled, SellByType.PriceEachTotal, SellByType.Prepped ->
                context.getString(R.string.item_details_upc_format, convertedupcOrPlu)
            else -> ""
        }
    } else {
        text = ""
    }
}

// https://confluence.safeway.com/pages/viewpage.action?spaceKey=AcuPick&title=PLU+flow
fun convertUPCSentFromendToDisplayedPLUOrUpc(scannedUpc: String?, displayType: Int, sellByType: SellByType): String? {
    scannedUpc?.let {
        return when (sellByType) {
            SellByType.Each -> {
                scannedUpc.substring(7..11)
            }
            SellByType.Weight -> {
                scannedUpc.substring(2..6)
            }
            else -> {
                scannedUpc
            }
        }
    }
    return scannedUpc
}
