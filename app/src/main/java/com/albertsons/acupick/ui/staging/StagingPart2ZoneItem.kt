package com.albertsons.acupick.ui.staging

import android.content.res.ColorStateList
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ItemZoneBagCountBinding
import com.albertsons.acupick.ui.BaseBindableViewModel
import com.albertsons.acupick.ui.ViewModelItem
import com.albertsons.acupick.ui.arrivals.destage.DUMMY_STAGING_LOCATION_SUFFIX
import com.albertsons.acupick.ui.arrivals.destage.OrderCompletionState
import com.albertsons.acupick.ui.arrivals.pharmacy.PrescriptionReturnListUi
import com.albertsons.acupick.ui.models.RxBagUI
import com.albertsons.acupick.ui.models.ZoneBagCountUI
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.getOrZero
import com.albertsons.acupick.ui.util.notZeroOrNull
import com.albertsons.acupick.ui.util.zeroOrNull
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

class StagingPart2ZoneItem(
    private val item: ZoneBagCountUI,
    private val isCustomerPreferBag: Boolean
) : BindableItem<ItemZoneBagCountBinding>() {

    // don't show dummy staging locations like "AMDUMMY" or "CHDUMMY"
    val location = item.zone.takeIf { !it.endsWith(DUMMY_STAGING_LOCATION_SUFFIX) } ?: ""

    override fun initializeViewBinding(view: View) = ItemZoneBagCountBinding.bind(view)
    override fun getLayout() = R.layout.item_zone_bag_count
    override fun bind(viewBinding: ItemZoneBagCountBinding, position: Int) {
        viewBinding.item = item
        viewBinding.isCustomerPreferBag = isCustomerPreferBag
        val charLimit = if (item.isMultiSource == true) LOCATION_CHAR_LIMIT_MFC else LOCATION_CHAR_LIMIT_REGULAR
        viewBinding.location = location.takeLast(charLimit)
    }

    override fun isSameAs(other: Item<*>): Boolean {
        return other is StagingPart2ZoneItem &&
            item.zone == other.item.zone
    }

    override fun hasSameContentAs(other: Item<*>): Boolean {
        return other is StagingPart2ZoneItem &&
            item.isCurrent == other.item.isCurrent &&
            item.scannedBagCount == other.item.scannedBagCount
    }
}

@BindingAdapter("app:zoneBagCounts", "app:isCustomerPreferBag")
fun RecyclerView.setZoneBagCounts(items: List<ZoneBagCountUI>?, isCustomerPreferBag: Boolean?) {
    if (items == null) return

    layoutManager = LinearLayoutManager(context)

    @Suppress("UNCHECKED_CAST")
    // If adapter can be cast to GroupieAdapter, update with new data
    (adapter as? GroupAdapter<GroupieViewHolder>)?.apply {
        // Update adapter with new info.
        clear()
        add(generateSection(items, isCustomerPreferBag ?: true))
    } ?: run {
        //  Create new adapter
        layoutManager = LinearLayoutManager(context)
        adapter = GroupAdapter<GroupieViewHolder>().apply { add(generateSection(items, isCustomerPreferBag ?: true)) }
    }
}

private fun generateSection(items: List<ZoneBagCountUI>, isCustomerPreferBag: Boolean) =
    Section().apply {
        update(
            items.map { StagingPart2ZoneItem(it, isCustomerPreferBag) }
        )
    }

@BindingAdapter(value = ["app:bagOrToteScannedCount", "app:bagOrToteTotalCount", "app:looseScannedCount", "app:looseTotalCount", "app:isMultiSource", "app:isCustomerPreferBag"])
fun TextView.setOutOfTotalBagOrLooseCount(bagOrToteScannedCount: Int, bagOrToteTotalCount: Int, looseScannedCount: Int, looseTotalCount: Int, isMultiSource: Boolean?, isCustomerPreferBag: Boolean?) {
    text = when {
        isMultiSource == true && bagOrToteTotalCount > 1 -> context.resources.getString(R.string.totesOutOf, bagOrToteScannedCount, bagOrToteTotalCount)
        isMultiSource == true -> context.resources.getString(R.string.toteOutOf, bagOrToteScannedCount, bagOrToteTotalCount)

        isCustomerPreferBag == false && bagOrToteTotalCount > 0 && looseTotalCount > 0 -> context.resources.getString(
            R.string.toteOrLooseOutOf,
            bagOrToteScannedCount,
            bagOrToteTotalCount,
            looseScannedCount,
            looseTotalCount
        )
        isCustomerPreferBag == false && bagOrToteTotalCount > 0 -> context.resources.getString(R.string.toteOutOf, bagOrToteScannedCount, bagOrToteTotalCount)

        bagOrToteTotalCount > 1 && looseTotalCount > 0 -> context.resources.getString(
            R.string.bagsOrLooseOutOf,
            bagOrToteScannedCount,
            bagOrToteTotalCount,
            looseScannedCount,
            looseTotalCount
        )
        bagOrToteTotalCount > 0 && looseTotalCount > 0 -> context.resources.getString(
            R.string.bagOrLooseOutOf,
            bagOrToteScannedCount,
            bagOrToteTotalCount,
            looseScannedCount,
            looseTotalCount
        )
        bagOrToteTotalCount > 1 -> context.resources.getString(R.string.bagsOutOf, bagOrToteScannedCount, bagOrToteTotalCount)
        bagOrToteTotalCount > 0 -> context.resources.getString(R.string.bagOutOf, bagOrToteScannedCount, bagOrToteTotalCount)
        else -> context.resources.getString(R.string.looseOutOf, looseScannedCount, looseTotalCount)
    }
}

@BindingAdapter("app:bagOrToteCount", "app:isCustomerPreferBag")
fun TextView.setBagOrToteCount(item: ZoneBagCountUI?, isCustomerPreferBag: Boolean) {
    val pluralId = if (item?.isMultiSource == true || !isCustomerPreferBag) R.plurals.tote_count_plural_lowercase else R.plurals.bags_count_plural
    text = if (item?.bagOrToteScannedCount.notZeroOrNull())
        context.resources.getQuantityString(pluralId, item?.bagOrToteScannedCount.getOrZero(), item?.bagOrToteScannedCount) else ""
}

@BindingAdapter("app:looseCount")
fun TextView.setLooseCount(item: ZoneBagCountUI?) {
    text = if (item?.looseScannedCount.zeroOrNull() || item?.isMultiSource == true)
        "" else context.resources.getQuantityString(R.plurals.loose_count_plural, item?.looseScannedCount.getOrZero(), item?.looseScannedCount)
}

@BindingAdapter("app:item")
fun View.setSeparatorVisibility(item: ZoneBagCountUI?) {
    visibility =
        if (item?.bagOrToteScannedCount.notZeroOrNull() && item?.looseScannedCount.notZeroOrNull()) View.VISIBLE else View.GONE
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

@BindingAdapter(value = ["app:customerOrderNumber", "app:isCompleteList", "app:lastTabOrderNumber"], requireAll = true)
fun AppCompatTextView.setupButtonState(customerOrderNumber: String?, isCompleteList: List<OrderCompletionState>?, lastTabOrderNumber: String?) {
    val isComplete = isCompleteList?.firstOrNull { it.customerOrderNumber == customerOrderNumber }?.isComplete == true
    val isLastTab = lastTabOrderNumber == customerOrderNumber
    val isAllComplete = isCompleteList?.none { !it.isComplete }
    val isButtonEnabled = if (isLastTab) isAllComplete == true else isComplete
    val btnColorRes = if (isButtonEnabled) R.color.semiLightBlue else R.color.coffeeLight
    val textColorRes = if (isButtonEnabled) R.color.white else R.color.grey_550
    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, btnColorRes))
    setTextColor(ContextCompat.getColor(context, textColorRes))
    text = context.getString(if (isLastTab) R.string.complete_stage else R.string.next_order)
}

@BindingAdapter(value = ["app:unwantedItemsCount"])
fun AppCompatTextView.setUnwantedItemsCount(count: Int?) {
    text = StringIdHelper.Format(R.string.unwanted_items_count, count?.toString().orEmpty()).getString(context)
}

@BindingAdapter("app:scannedZoneLocations")
fun TextView.setScannedLocationsCount(item: List<String>?) {
    text = if (item?.isEmpty() == true)
        "" else context.resources.getString(R.string.existing_scanned_location, item?.joinToString(separator = ", "))
}

class RxPrescriptionsItemsDbViewModel(
    val item: String,
    val isScanned: Boolean
) : BaseBindableViewModel() {
    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as RxPrescriptionsItemsDbViewModel, R.layout.item_rx_prescription_number) }
    }
}

class RxPrescriptionReturnItemsDbViewModel(
    val item: PrescriptionReturnListUi
) : BaseBindableViewModel() {
    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as RxPrescriptionReturnItemsDbViewModel, R.layout.item_rx_prescription_return) }
    }
}

@BindingAdapter("app:rxPrescriptions", "app:scannedRxBags", requireAll = true)
fun RecyclerView.setRxPrescriptions(rxPrescriptions: List<Pair<String, String>>?, scannedRxBags: List<RxBagUI>?) {
    if (rxPrescriptions == null) return

    layoutManager = LinearLayoutManager(context)

    @Suppress("UNCHECKED_CAST")
    (adapter as? GroupAdapter<GroupieViewHolder>)?.apply {
        clear()
        add(generateRxPrescriptionsSection(rxPrescriptions, scannedRxBags.orEmpty()))
    } ?: run {
        adapter = GroupAdapter<GroupieViewHolder>().apply {
            add(generateRxPrescriptionsSection(rxPrescriptions, scannedRxBags.orEmpty()))
        }
    }

    isNestedScrollingEnabled = true
    setHasFixedSize(true)

    layoutParams.apply {
        height = if (rxPrescriptions.size > ITEMS_LIMIT_PRESCRIPTION_LIST) {
            context.resources.getDimensionPixelSize(R.dimen.rx_prescription_container_max_height)
        } else {
            LinearLayout.LayoutParams.WRAP_CONTENT
        }
    }
}

private fun generateRxPrescriptionsSection(items: List<Pair<String, String>>, scannedRxBags: List<RxBagUI>) = Section().apply {
    update(
        items.map { item ->
            RxPrescriptionsItemsDbViewModel(item.second, scannedRxBags.any { it.bagNumber == item.first })
        }
    )
}

@BindingAdapter("app:prescriptionListUi")
fun RecyclerView.setRxPrescriptionReturn(rxPrescriptions: List<PrescriptionReturnListUi>?) {
    if (rxPrescriptions == null) return

    layoutManager = LinearLayoutManager(context)

    @Suppress("UNCHECKED_CAST")
    (adapter as? GroupAdapter<GroupieViewHolder>)?.apply {
        clear()
        add(generateRxPrescriptionReturnSection(rxPrescriptions))
    } ?: run {
        adapter = GroupAdapter<GroupieViewHolder>().apply {
            add(generateRxPrescriptionReturnSection(rxPrescriptions))
        }
    }

    isNestedScrollingEnabled = true
    setHasFixedSize(true)

    layoutParams.apply {
        height = if (rxPrescriptions.size > ITEMS_LIMIT_RETURN_PRESCRIPTION_LIST) {
            context.resources.getDimensionPixelSize(R.dimen.rx_prescription_return_container_max_height)
        } else {
            LinearLayout.LayoutParams.WRAP_CONTENT
        }
    }
}

private fun generateRxPrescriptionReturnSection(items: List<PrescriptionReturnListUi>) = Section().apply {
    update(
        items.map { item ->
            RxPrescriptionReturnItemsDbViewModel(item)
        }
    )
}

const val LOCATION_CHAR_LIMIT_REGULAR = 5
const val LOCATION_CHAR_LIMIT_MFC = 3
const val ITEMS_LIMIT_PRESCRIPTION_LIST = 8
const val ITEMS_LIMIT_RETURN_PRESCRIPTION_LIST = 5
