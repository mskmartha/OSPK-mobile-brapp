package com.albertsons.acupick.ui.picklists

import android.app.Application
import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.albertsons.acupick.AcuPickConfig
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ActivityType
import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.FulfillmentSubType
import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.isAdvancePickOrPrePick
import com.albertsons.acupick.data.model.response.ActivityAndErDto
import com.albertsons.acupick.data.model.response.PickListBatchingType
import com.albertsons.acupick.data.model.response.associateFirstNameDotLastInitial
import com.albertsons.acupick.data.model.response.customerNameBasedOnAssociate
import com.albertsons.acupick.data.model.response.toDeliveryType
import com.albertsons.acupick.data.model.response.toVanNumber
import com.albertsons.acupick.infrastructure.utils.asApplication
import com.albertsons.acupick.infrastructure.utils.filterIfElseEmpty
import com.albertsons.acupick.infrastructure.utils.toSameZoneInstantLocalDate
import com.albertsons.acupick.ui.BaseBindableViewModel
import com.albertsons.acupick.ui.LiveDataHelper
import com.albertsons.acupick.ui.ViewModelItem
import com.albertsons.acupick.ui.bindingadapters.setVisibilityGoneIfTrue
import com.albertsons.acupick.ui.picklists.open.ChipLabel
import com.albertsons.acupick.ui.picklists.open.OrderCategoryUi
import com.albertsons.acupick.ui.util.StickyGroupieAdapter
import com.albertsons.acupick.ui.util.asFirstInitialDotLastString
import com.albertsons.acupick.ui.util.groupieAdapter
import com.albertsons.acupick.ui.util.orFalse
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.jay.widget.StickyHeadersLinearLayoutManager
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

private const val ACKNOWLEDGED_FLASH_ORDER_STROKE_WIDTH_PX = 4

/** Picklist Databinding ViewModel that represents an individual picklist item UI (per item_picklist) */
class PickListDbViewModel(
    app: Application,
    private val activityAndErDto: ActivityAndErDto,
    private val pickerClickListener: (ActivityAndErDto) -> Unit,
    acknowledgedFlashOrderActId: Long?,
) : LiveDataHelper, BaseBindableViewModel() {
    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as PickListDbViewModel, R.layout.item_picklist) }
    }

    private val reProcess = activityAndErDto.reProcess
    private val prepNotReady = activityAndErDto.isPrepNeeded
    private val isPrePick = activityAndErDto.prePickType.isAdvancePickOrPrePick()
    val redBadgeText = when {
        reProcess == true -> app.getString(R.string.reshop)
        prepNotReady == true -> app.getString(R.string.prep_not_ready)
        isPrePick -> app.getString(R.string.pre_pick)
        else -> null
    }
    val badgeColor = when {
        reProcess == true -> R.color.lightOrange
        prepNotReady == true -> R.color.lightOrange
        isPrePick -> R.color.lightPurple
        else -> R.color.lightOrange
    }
    val fulfillment = when (activityAndErDto.batch?.toDeliveryType()) {
        FulfillmentSubType.THREEPL -> null
        FulfillmentSubType.ONEPL -> {
            FulfillmentAttributeDto(
                subType = activityAndErDto.batch?.toDeliveryType(),
                type = FulfillmentType.DELIVERY
            )
        }
        else -> activityAndErDto.fulfillment
    }
    val activityNo = activityAndErDto.activityNo
    val customerName = activityAndErDto.customerNameBasedOnAssociate()
    val associateName = activityAndErDto.associateFirstNameDotLastInitial()
    val customerOrderNumber = activityAndErDto.customerOrderNumber
    val orderType = activityAndErDto.orderType
    val orderSpeedPillText = activityAndErDto.orderType?.let { app.getString(it.simplify()) } ?: ""
    private val totalItemCount = activityAndErDto.expectedCount ?: 0
    val totalItemCountString = totalItemCount.toString()
    val totalItemsPluralString = app.resources.getQuantityString(R.plurals.items_plural, totalItemCount)
    val isBatch = activityAndErDto.getPickListType() == PickListBatchingType.Batch
    val orderCount = activityAndErDto.getOrderCount()
    val isAcknowledgedFlashOrder = activityAndErDto.actId == acknowledgedFlashOrderActId
    val isPartnerPickOrder = activityAndErDto.is3p == true
    val hideOrderSpeedPill = isPartnerPickOrder || orderType == OrderType.REGULAR || orderType == null
    val isEbt = activityAndErDto.isSnap.orFalse()
    val dueDay = if (activityAndErDto.prePickType.isAdvancePickOrPrePick()) {
        ChronoUnit.DAYS.between(
            ZonedDateTime.now().toLocalDate(), (activityAndErDto.expectedEndTime ?: ZonedDateTime.now()).toSameZoneInstantLocalDate()
        )
    } else {
        0L
    }
    val hideTimer = dueDay != 0L
    // We get a batch even for a single order so this logic for vanNUmber will work for single and batch orders
    val vanNumber = when (activityAndErDto.batch?.toDeliveryType()) {
        FulfillmentSubType.ONEPL -> activityAndErDto.batch.toVanNumber()
        else -> null
    }

    val showEbt = AcuPickConfig.cattEnabled.value && activityAndErDto.isSnap.orFalse()
    val showFreshPass = AcuPickConfig.cattEnabled.value && activityAndErDto.isSubscription.orFalse()
    var ambientActive = activityAndErDto.storageTypes?.any { it == StorageType.AM } ?: false
    var coldActive = activityAndErDto.storageTypes?.any { it == StorageType.CH } ?: false
    var frozenActive = activityAndErDto.storageTypes?.any { it == StorageType.FZ } ?: false
    var hotActive = activityAndErDto.storageTypes?.any { it == StorageType.HT } ?: false
    val endTime = activityAndErDto.expectedEndTime
    val assignedTo = if (activityAndErDto.assignedTo == null) {
        app.getString(R.string.open)
    } else {
        activityAndErDto.assignedTo?.asFirstInitialDotLastString()
    }

    fun onPickerItemClick() {
        pickerClickListener(activityAndErDto)
    }

    val isOrderInStagePhase: Boolean
        get() {
            return activityAndErDto.actType == ActivityType.DROP_OFF
        }
}

@BindingAdapter("app:activityNo", "orderCount")
fun TextView.formatOrderString(activityNo: String?, orderCount: Int?) {
    val quantity = orderCount ?: 1
    text = if (quantity > 1) context.getString(R.string.pick_card_format).format(
        activityNo,
        context.resources.getQuantityString(R.plurals.orders_count_plural, quantity, orderCount)
    ) else {
        activityNo
    }
}

@BindingAdapter("app:setOnRefresh", "app:isRefreshComplete")
fun SwipeRefreshLayout.setOnRefresh(viewModel: PickListsBaseViewModel, refreshComplete: Boolean) {
    isRefreshing = refreshComplete
    setOnRefreshListener {
        viewModel.loadData(isRefresh = true)
    }
}

@BindingAdapter(value = ["app:viewModel", "app:picklistCategories"])
fun ChipGroup.setupChipGroup(
    viewModel: PickListsBaseViewModel?,
    picklistCategories: Map<OrderCategoryUi, List<ActivityAndErDto>>?
) {
    removeAllViews()

    val chips = picklistCategories?.map { ChipLabel(it.key, it.value.size) }
    chips?.forEach { (orderCategoryUi, count) -> addView(getChip(context, orderCategoryUi, count, this)) }
    setOnCheckedChangeListener { group, checkedId ->
        val chip: Chip? = group.findViewById(checkedId)
        chip?.let { chipView ->
            val orderCategoryUi = chipView.tag as OrderCategoryUi
            viewModel?.onCategorySelected(orderCategoryUi)
        }
    }
    // set default chip selected
    getChildAt(0)?.let { check(it.id) }
}

private fun getChip(context: Context, orderCategoryUi: OrderCategoryUi, count: Int, chipGroup: ChipGroup): Chip {
    val chip = LayoutInflater.from(context).inflate(R.layout.layout_chip, chipGroup, false) as Chip
    return chip.apply {
        val labelText = context.getString(orderCategoryUi.resourceId)
        text = context.getString(R.string.chip_label, labelText, count)
        tag = orderCategoryUi
    }
}

/** Picklists RecyclerView binding adapter to hookup the vm (ctas), db vm (ui+piping actions to vm), add setup the groupie adapter */
@BindingAdapter(value = ["app:pickLists", "app:viewModel", "app:isFlashOrderEnabled", "app:isWineOrder", "app:acknowledgedFlashOrderActId"])
fun RecyclerView.setPicklists(
    pickLists: List<ActivityAndErDto>?,
    viewModel: PickListsBaseViewModel?,
    isFlashOrderEnabled: Boolean,
    isWineOrder: Boolean,
    acknowledgedFlashOrderActId: Long?,
) {
    if (pickLists != null && viewModel != null) {

        layoutManager = StickyHeadersLinearLayoutManager<StickyGroupieAdapter>(context)

        adapter = (groupieAdapter ?: StickyGroupieAdapter()).apply {
            //  Update data every refresh
            populatePickLists(pickLists, viewModel::onPickClicked, isFlashOrderEnabled, isWineOrder, context.asApplication(), acknowledgedFlashOrderActId)
        }
    }
}

// TODO need to refractor this code and we can remove the stickyHeader and also section grouping
private fun GroupieAdapter.populatePickLists(
    pickLists: List<ActivityAndErDto>?,
    pickerClickListener: (ActivityAndErDto) -> Unit,
    isFlashOrderEnabled: Boolean,
    isWineOrder: Boolean,
    app: Application,
    acknowledgedFlashOrderActId: Long? = null,
) {
    fun createGroupieSection(type: OrderType, filteredOrders: List<ActivityAndErDto>?): Section {
        fun addItemsToGroupieSection(pickList: List<ActivityAndErDto>?, section: Section) {
            // Map pick lists and add to section
            val mappedOpenPickLists = pickList?.map { pickListItem ->
                PickListDbViewModel(
                    app = app,
                    activityAndErDto = pickListItem,
                    pickerClickListener = pickerClickListener,
                    acknowledgedFlashOrderActId = acknowledgedFlashOrderActId,
                )
            }?.toMutableList().orEmpty()
            section.addAll(mappedOpenPickLists)
        }

        val section = Section()
        // section.setHeader(PickListsHeader(type, filteredOrders?.size ?: 0, isWineOrder))
        addItemsToGroupieSection(filteredOrders ?: emptyList(), section)
        return section
    }
    clear()

    val sectionsList = mutableListOf<Section>()

    if (!isWineOrder) {
        sectionsList.add(
            createGroupieSection(
                OrderType.FLASH,
                pickLists?.run {
                    filterIfElseEmpty(isFlashOrderEnabled) { it.orderType == OrderType.FLASH } +
                        filter { it.orderType == OrderType.FLASH3P }
                }
            )
        ) // ERE - Note this change in PR
    }

    sectionsList.add(createGroupieSection(OrderType.REGULAR, pickLists?.filter { it.orderType != OrderType.FLASH && it.orderType != OrderType.FLASH3P }))
    updateAsync(sectionsList)
}

@BindingAdapter(value = ["app:sectionType", "app:openPickListCount", "app:isWineOrder"], requireAll = true)
fun TextView.setTextWithSectionCount(type: OrderType, count: Int, isWineOrder: Boolean) {
    text = when (type) {
        OrderType.FLASH -> context.getString(R.string.flash_header_format, count)
        OrderType.REGULAR -> if (!isWineOrder) context.getString(R.string.express_standard_header, count) else context.getString(R.string.express_standard_header_wine, count)
        else -> ""
    }
}

@BindingAdapter("app:isAcceptedFlashOrder")
fun MaterialCardView.setAcceptedFlashOrder(accepted: Boolean) {
    strokeWidth = if (accepted) ACKNOWLEDGED_FLASH_ORDER_STROKE_WIDTH_PX else 0
}

@BindingAdapter("app:orderTypeIcon")
fun ImageView.setOrderTypeIcon(orderType: OrderType?) {
    when (orderType) {
        OrderType.FLASH -> R.drawable.ic_flash
        OrderType.EXPRESS -> R.drawable.ic_express
        else -> null
    }?.let { setImageResource(it) }
    setVisibilityGoneIfTrue(orderType == OrderType.REGULAR || orderType == null)
}

@BindingAdapter("app:customerNameOrOrderCount", "app:isBatchOrder", "app:orderCount")
fun AppCompatTextView.setcustomerNameOrOrderCount(customerName: String?, isBatchOrder: Boolean?, count: Int?) {
    text = if (isBatchOrder == true) {
        context.resources.getQuantityString(R.plurals.number_of_order_plural, count ?: 1, count ?: 1)
    } else {
        customerName
    }
}

fun OrderType.simplify() = when (this) {
    OrderType.FLASH -> R.string.flash_order
    OrderType.REGULAR -> R.string.standard_order
    OrderType.EXPRESS -> R.string.express_order
    OrderType.FLASH3P -> R.string.partnerpick_order
}
