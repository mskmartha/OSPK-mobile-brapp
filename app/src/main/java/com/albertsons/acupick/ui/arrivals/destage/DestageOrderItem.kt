package com.albertsons.acupick.ui.arrivals.destage

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ItemOrderDetailsTotesBinding
import com.albertsons.acupick.ui.models.ZonedBagsScannedData
import com.albertsons.acupick.ui.util.getOrZero
import com.albertsons.acupick.ui.util.notZeroOrNull
import com.albertsons.acupick.ui.util.orTrue
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

class DestageOrderItem(
    private val zone: ZonedBagsScannedData?,
    var fragmentLifecycleOwner: LifecycleOwner?,
) : BindableItem<ItemOrderDetailsTotesBinding>() {

    // don't show dummy staging locations like "AMDUMMY" or "CHDUMMY"
    val location = zone?.bagData?.zoneId?.takeIf { !it.endsWith(DUMMY_STAGING_LOCATION_SUFFIX) } ?: ""

    override fun initializeViewBinding(view: View) = ItemOrderDetailsTotesBinding.bind(view)
    override fun getLayout() = R.layout.item_order_details_totes
    override fun bind(viewBinding: ItemOrderDetailsTotesBinding, position: Int) {
        viewBinding.zone = zone
        viewBinding.location = location.takeLast(5)
        viewBinding.lifecycleOwner = fragmentLifecycleOwner
    }

    override fun isSameAs(other: Item<*>): Boolean {
        return other is DestageOrderItem &&
            zone?.bagData?.zoneId == other.zone?.bagData?.zoneId
    }

    override fun hasSameContentAs(other: Item<*>): Boolean {
        return other is DestageOrderItem &&
            zone?.isActive == other.zone?.isActive &&
            zone?.currentBagsScanned == other.zone?.currentBagsScanned
    }
}

@BindingAdapter(value = ["app:zoneBagCounts", "app:fragmentLifecycleOwner"])
fun RecyclerView.setZoneBagCounts(items: List<ZonedBagsScannedData>?, fragmentLifecycleOwner: LifecycleOwner?) {
    if (items == null) return

    layoutManager = LinearLayoutManager(context)

    @Suppress("UNCHECKED_CAST")
    // If adapter can be cast to GroupieAdapter, update with new data
    (adapter as? GroupAdapter<GroupieViewHolder>)?.apply {
        // Update adapter with new info.
        clear()
        add(generateSection(items, fragmentLifecycleOwner))
    } ?: run {
        //  Create new adapter
        layoutManager = LinearLayoutManager(context)
        adapter = GroupAdapter<GroupieViewHolder>().apply { add(generateSection(items, fragmentLifecycleOwner)) }
    }
}

private fun generateSection(items: List<ZonedBagsScannedData>, fragmentLifecycleOwner: LifecycleOwner?) =
    Section().apply {
        update(
            items.map { DestageOrderItem(it, fragmentLifecycleOwner) }
        )
    }

@BindingAdapter("destageItem")
fun TextView.setBagCount(item: ZonedBagsScannedData?) {
    val pluralDisplay = when {
        item?.bagData?.isMultiSourceOrder == true -> {
            val pluralId = if (item.bagData.isReshop) R.plurals.reshop_tote_count_plural_lowercase else R.plurals.tote_count_plural_lowercase
            context.resources.getQuantityString(pluralId, item.totalBagsForZone ?: 0, item.totalBagsForZone)
        }

        !item?.bagData?.isCustomerBagPreference.orTrue() -> when {
            item?.looseItemCount.getOrZero() > 0 -> context.resources.getQuantityString(
                R.plurals.destaging_tote_loose_count_plural_uppercase,
                (item?.totalBagsForZone.getOrZero() - item?.looseItemCount.getOrZero()),
                item?.totalBagsForZone.getOrZero()
            )

            else -> context.resources.getQuantityString(R.plurals.tote_count_plural, item?.totalBagsForZone.getOrZero(), item?.totalBagsForZone.getOrZero())
        }

        else -> context.resources.getQuantityString(R.plurals.bag_count_plural, item?.totalBagsForZone ?: 0, item?.totalBagsForZone)
    }
    text = context.getString(R.string.bag_count_multiple_bags_format, item?.currentBagsScanned, pluralDisplay)
}

@BindingAdapter("app:zonedBagOrToteData")
fun TextView.setZonedBagOrToteData(item: ZonedBagsScannedData?) {
    text = when {
        item?.totalBagsPerLocation.getOrZero() == 0 -> ""
        item?.bagData?.isMultiSourceOrder == true || !item?.bagData?.isCustomerBagPreference.orTrue() -> {
            if ((item?.totalBagsPerLocation ?: 0) > 1) {
                context.getString(R.string.totesOutOf, item?.bagsScanned.getOrZero(), item?.totalBagsPerLocation.getOrZero())
            } else {
                context.getString(R.string.toteOutOf, item?.bagsScanned.getOrZero(), item?.totalBagsPerLocation.getOrZero())
            }
        }

        else -> {
            if ((item?.totalBagsPerLocation ?: 0) > 1) {
                context.getString(R.string.bagsOutOf, item?.bagsScanned.getOrZero(), item?.totalBagsPerLocation.getOrZero())
            } else {
                context.getString(R.string.bagOutOf, item?.bagsScanned.getOrZero(), item?.totalBagsPerLocation.getOrZero())
            }
        }
    }
}

@BindingAdapter("app:zonedLooseData")
fun TextView.setZonedLooseData(item: ZonedBagsScannedData?) {
    text = if (item?.bagData?.isMultiSourceOrder == false && item.totalLoosePerLocation > 0) {
        context.getString(R.string.loose_out_of_count, item.looseScanned, item.totalLoosePerLocation)
    } else {
        ""
    }
}

@BindingAdapter("app:zonedData")
fun View.setSeparatorVisibility(item: ZonedBagsScannedData?) {
    visibility =
        if (item?.totalBagsPerLocation.notZeroOrNull() && item?.totalLoosePerLocation.notZeroOrNull()) View.VISIBLE else View.GONE
}

@BindingAdapter("isCurrentZone")
fun TextView.setCurrentZone(isCurrentZone: Boolean) {
    setTextAppearance(
        if (isCurrentZone) R.style.NunitoSansBold16_darkBlue else R.style.NunitoSansRegular16
    )
}

@BindingAdapter("app:isMultiSource")
fun TextView.isMultiSource(isMultiSource: Boolean) {
    text = context.getString(if (isMultiSource) R.string.no_totes_scanned else R.string.no_zones_scanned)
}

@BindingAdapter(
    value = [
        "app:selectedOrderNumber",
        "app:lastOrderNumber",
        "app:currentOrderZonedBagList",
        "app:isAllComplete"
    ],
    requireAll = true
)
fun AppCompatTextView.setupDestageButtonState(
    selectedOrderNumber: String?,
    lastOrderNumber: String?,
    currentOrderZonedBagList: List<ZonedBagsScannedData>,
    isAllComplete: Boolean?,
) {
    val isComplete = currentOrderZonedBagList.all { it.isComplete() } && currentOrderZonedBagList.isNotEmpty()
    val isLastTab = lastOrderNumber == selectedOrderNumber
    val isButtonEnabled = when {
        isLastTab -> isAllComplete == true
        else -> isComplete
    }
    val btnColorRes = if (isButtonEnabled) R.color.semiLightBlue else R.color.coffeeLight
    val textColorRes = if (isButtonEnabled) R.color.white else R.color.grey_550
    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, btnColorRes))
    setTextColor(ContextCompat.getColor(context, textColorRes))
    text = context.getString(
        if (isLastTab) R.string.complete_destaging
        else R.string.next
    )
}
