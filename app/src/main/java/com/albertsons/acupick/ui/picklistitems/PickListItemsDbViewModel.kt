package com.albertsons.acupick.ui.picklistitems

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.albertsons.acupick.AcuPickConfig
import com.albertsons.acupick.R
import com.albertsons.acupick.data.logic.getCustomerType
import com.albertsons.acupick.data.model.CustomerType
import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.FulfillmentSubType
import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.ImageSizePreset
import com.albertsons.acupick.data.model.PickListActivity
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.itemActivities
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.fulfilledWeight
import com.albertsons.acupick.data.model.response.isIssueScanned
import com.albertsons.acupick.data.model.response.isShorted
import com.albertsons.acupick.data.model.response.isSubstituted
import com.albertsons.acupick.data.model.response.requestedNetWeight
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.orZero
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.infrastructure.utils.roundToLongOrZero
import com.albertsons.acupick.infrastructure.utils.toTwoDecimalString
import com.albertsons.acupick.ui.BaseBindableViewModel
import com.albertsons.acupick.ui.LiveDataHelper
import com.albertsons.acupick.ui.ViewModelItem
import com.albertsons.acupick.ui.bindingadapters.setVisibilityGoneIfTrue
import com.albertsons.acupick.ui.util.CenterZoomLayoutManager
import com.albertsons.acupick.ui.util.asIcon
import com.albertsons.acupick.ui.util.asItemLocation
import com.albertsons.acupick.ui.util.asUpcOrPlu
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.sizedImageUrl
import com.albertsons.acupick.ui.util.asStatusPillString
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class PickListItemHeaderViewModel(
    val header: String,
) : BaseBindableViewModel() {
    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as PickListItemHeaderViewModel, R.layout.item_picklist_item_header) }
    }
}

/** Picklists Databinding ViewModel that represents an individual picklist item UI (per item_picklist) */
class PickListItemsDbViewModel(
    context: Context,
    private val isListView: Boolean,
    private val index: Int,
    barcodeMapper: BarcodeMapper,
    private val item: ItemActivityDto,
    val enableButtons: LiveData<Boolean>,
    val subAllowed: Boolean,
    val isTwoWayCommsEnabled: Boolean,
    val isDisplayType3PWEnabled: Boolean,
    val isRepickOriginalItemAllowed: Boolean,
    private val fragmentViewLifecycleOwner: LifecycleOwner,
    private val detailsClickListener: (ItemActivityDto, Boolean) -> Unit,
    private val shortClickListener: (ItemActivityDto) -> Unit,
    private val substituteClickListener: (ItemActivityDto) -> Unit,
    private val completePickListener: (ItemActivityDto) -> Unit,
    private val onCompleteButtonShown: (ItemActivityDto) -> Unit,
    private val currentItemPositionFlow: StateFlow<Int>,
    // In constructor b/c it has to be compared against previous values
    val animateItemComplete: Boolean,
    val listType: PickListType,
    val locationCount: String,
    dispatcherProvider: DispatcherProvider,
) : LiveDataHelper, BaseBindableViewModel() {
    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as PickListItemsDbViewModel, getPickingListUi(isListView), fragmentViewLifecycleOwner) }
    }

    val hasExceptions = item.isFullyPicked() && (item.isShorted || item.isSubstituted)
    val status = item.asStatusPillString(context, listType)

    val itemAddress = item.asItemLocation(context)
    val itemAddressWithoutDept = item.asItemLocation(context, false).takeIf { it.isNotEmpty() } ?: item.locationDetail.orEmpty()
    val itemDepName = item.depName.orEmpty()
    val showMoreInfoButton = listType == PickListType.Todo && item.asItemLocation(context, false).isNotEmpty() && item.locationDetail.isNotNullOrEmpty()
    val showMoreInfoInCardView = item.asItemLocation(context, false).isNotEmpty() && item.locationDetail.isNotNullOrEmpty()
    val processedQty = if (listType == PickListType.Todo || listType == PickListType.Picked) item.processedQty.roundToLongOrZero().toString() else item.exceptionQty.roundToLongOrZero().toString()
    val fulfilledWeight = item.fulfilledWeight
    val orderedWeight = item.orderedWeight
    val totalWeight = (if ((item.requestedNetWeight ?: 0.0) % 1 != 0.0) item.requestedNetWeight ?: 0.0 else item.requestedNetWeight.toInt()).toString()

    // As per figma ACURED_REDESIGN picked item less than < 49% will not falls under the error case
    val showError = item.processedWeight.orZero() > (item.requestedNetWeight + (item.requestedNetWeight * 0.1))
    val showWeightIndicator = item.sellByWeightInd == SellByType.PriceWeighted
    val isCustomerBagPreference = item.isCustomerBagPreference
    val totalQty = item.qty.roundToLongOrZero().toString()
    val itemComplete = item.isFullyPicked() || listType == PickListType.Picked || listType == PickListType.Short
    val showItemCompleteAnimation = MutableLiveData(itemComplete && listType != PickListType.Short && item.sellByWeightInd != SellByType.PriceWeighted)
    val imageUrl = item.sizedImageUrl(ImageSizePreset.PickList)
    val upcOrPlu = item.asUpcOrPlu(context, barcodeMapper)
    val locationToShow = listType == PickListType.Todo && upcOrPlu.isNotNullOrBlank() && itemAddressWithoutDept.isNotNullOrBlank()
    val isPluUnit = item.sellByWeightInd == SellByType.Each || item.sellByWeightInd == SellByType.Weight
    val description = if (isPluUnit && upcOrPlu.isNotEmpty()) {
        item.itemDescription.orEmpty().trim()
    } else item.getItemDescriptionEllipsis()
    val customerInstruction = item.instructionDto?.text?.let { if (it.isNotEmpty()) "\"$it\"" else "" }
    val isVisibleCustomerInstruction = listType == PickListType.Todo && customerInstruction.isNotNullOrBlank()
    val isEbt = item.isSnap.orFalse()
    val customerType = getCustomerType(isEbt, item.isSubscription.orFalse())
    val showCattEbt = AcuPickConfig.cattEnabled.value && (customerType == CustomerType.SNAP || customerType == CustomerType.BOTH)

    private val _isSelected = MutableLiveData<Boolean>()
    val isSelected: LiveData<Boolean> = _isSelected

    val isOrderedByWeight = item.isOrderedByWeight()
    val isDisplayType3PW = isDisplayType3PWEnabled && item.isDisplayType3PW()
    val isSellByTypeWeight = item.sellByWeightInd == SellByType.Weight
    val weightString = if (isDisplayType3PW && listType == PickListType.Todo)
        context.getString(R.string.fit_weight, fulfilledWeight.toTwoDecimalString(), orderedWeight.toTwoDecimalString())
    else item.getWeightAndUom()

    val bottomLinkCta = when (listType) {
        PickListType.Todo -> if (isListView) "" else context.getString(R.string.cant_find_item)
        PickListType.Picked -> when {
            isRepickOriginalItemAllowed && (item.isSubstituted || item.isIssueScanned) -> context.getString(R.string.repick_original_item_cta)
            else -> context.getString(R.string.item_details_unpick)
        }

        PickListType.Short -> context.getString(R.string.move_to_picklist)
    }

    val showUnpick = if (isTwoWayCommsEnabled) {
        when (listType) {
            PickListType.Todo -> true
            PickListType.Picked -> when (isRepickOriginalItemAllowed) {
                true -> !item.isSubstituted || !item.isIssueScanned // To enable the button for substituted/issue reported item if the flag is true
                else -> !item.isSubstituted && !item.isIssueScanned // To disable the button for substituted and issue reported item if the flag is false
            }

            PickListType.Short -> true
        }
    } else {
        true
    }

    val showCompletePickButton = MutableLiveData(showCompleteButton())

    private fun showCompleteButton(): Boolean {
        val showComplete = processedQty == totalQty && listType == PickListType.Todo && item.sellByWeightInd == SellByType.PriceWeighted
        if (showComplete) {
            onCompleteButtonShown(item)
        }
        return showComplete
    }

    init {
        // Use the lifecycle scope from the fragment's view (instead of view model scope) to prevent leaking UI when the fragment is destroyed (at which point the flow is completed)
        fragmentViewLifecycleOwner.lifecycleScope.launch(dispatcherProvider.IO) {
            currentItemPositionFlow.collect { position ->
                _isSelected.postValue(position == index)
                if (_isSelected.value == true) {
                    Timber.v("[currentItemPositionFlow collect] index $position (0-based) selected")
                }
            }
        }
    }

    private fun getPickingListUi(isListView: Boolean) = if (isListView) R.layout.item_picklist_item_listview else R.layout.item_picklist_item

    fun onDetailsClick() {
        Timber.v("[onDetailsClick]")
        detailsClickListener(item, false)
    }

    fun onShortClick() {
        Timber.v("[onShortClick]")
        shortClickListener(item)
    }

    fun onCompletePickClicked() {
        Timber.v("[onCompletePickClicked]")
        completePickListener(item)
        showItemCompleteAnimation.value = true
    }

    fun onSubstituteClick() {
        Timber.v("[onSubstituteClick]")
        substituteClickListener(item)
    }

    fun onLocationClick() {
        Timber.v("[onLocationClick]")
        detailsClickListener(item, true)
    }
}

//  Used to detect changing state,
//  since we're only trying to detect when items (by ID) have transitioned to a complete state this should tolerate switching lists
private var lastPickList: PickListActivity? = null

/** Picklists RecyclerView binding adapter to hookup the vm (ctas), db vm (ui+piping actions to vm), add setup the groupie adapter */
@BindingAdapter(value = ["app:isListView", "app:pickListItems", "app:viewModel", "app:listType", "app:fragmentViewLifecycleOwner"])
fun RecyclerView.setPickListItems(isListView: Boolean, pickList: PickListActivity?, viewModel: PickListItemsViewModel?, listType: PickListType, fragmentViewLifecycleOwner: LifecycleOwner) {
    if (pickList == null || viewModel == null) return

    // Reset last pick list if ID has changed
    if (lastPickList?.actId != pickList.actId) lastPickList = null

    val itemActivitiesMap = pickList.itemActivitiesMap

    layoutManager = if (!isListView) CenterZoomLayoutManager(context, RecyclerView.HORIZONTAL, false)
    else LinearLayoutManager(context)

    // clear previous data on first initialization
    (adapter as? GroupAdapter<*>)?.clear()

    itemActivitiesMap.entries.forEachIndexed { index, entry ->
        val itemActivities = entry.value.orEmpty()
        val header =
            when {
                listType == PickListType.Picked && entry.key == ITEM_SUBSTITUTIONS -> context.getString(R.string.picking_group_header_substitutions)
                listType == PickListType.Picked && entry.key == ITEM_ISSUE_REPORTED -> context.getString(R.string.issue_report_header)
                else -> itemActivities.firstOrNull()?.groupByName
            }

        @Suppress("UNCHECKED_CAST")
        // If adapter already exists, then cast and re-use
        (adapter as? GroupAdapter<GroupieViewHolder>)?.apply {
            // Store recently completed item id
            val completedItemIaId = itemActivities.find { item ->
                item.isFullyPicked() && lastPickList?.itemActivities?.find { prev -> item.id == prev.id }?.isFullyPicked() == false
            }?.id

            // Store recently shorted item id
            val shortedItemIaId = itemActivities.find { item ->
                item.isFullyShorted() && lastPickList?.itemActivities?.find { prev -> item.id == prev.id }?.isFullyShorted() == false
            }?.id
            if (isListView) {
                header?.let {
                    add(generatePickListHeader(it))
                }
            }
            add(
                generatePickListSection(
                    isListView = isListView,
                    itemActivities = itemActivities,
                    fragmentViewLifecycleOwner = fragmentViewLifecycleOwner,
                    viewModel = viewModel,
                    completedItemIaId = completedItemIaId,
                    shortedItemIaId = shortedItemIaId,
                    listType = listType
                )
            )
        } ?: run {
            // Otherwise; create new adapter
            adapter = GroupAdapter<GroupieViewHolder>().apply {
                if (isListView) {
                    header?.let {
                        add(generatePickListHeader(it))
                    }
                }
                add(
                    generatePickListSection(
                        isListView = isListView,
                        itemActivities = itemActivities,
                        fragmentViewLifecycleOwner = fragmentViewLifecycleOwner,
                        viewModel = viewModel,
                        completedItemIaId = null,
                        shortedItemIaId = null,
                        listType = listType
                    )
                )
            }
        }
    }

    // Save pick list state to detect when items are recently completed
    if (listType == PickListType.Todo) lastPickList = pickList.copy()

    if (!isListView) {
        layoutManager?.scrollToPosition(viewModel.getSelectedItemIndex(listType).value)
    }
}

private fun RecyclerView.generatePickListSection(
    isListView: Boolean,
    itemActivities: List<ItemActivityDto>,
    fragmentViewLifecycleOwner: LifecycleOwner,
    viewModel: PickListItemsViewModel,
    completedItemIaId: Long? = null,
    shortedItemIaId: Long? = null,
    listType: PickListType,
): Section {
    val section = Section()
    section.update(
        itemActivities.mapIndexed { index, pickListItem ->
            PickListItemsDbViewModel(
                isListView = isListView,
                context = context,
                index = index,
                barcodeMapper = viewModel.barcodeMapper,
                item = pickListItem,
                enableButtons = viewModel.pickListEnabled,
                subAllowed = viewModel.isSubstitutionAllowed(pickListItem),
                fragmentViewLifecycleOwner = fragmentViewLifecycleOwner,
                detailsClickListener = viewModel::onDetailsCtaClicked,
                shortClickListener = viewModel::onShortCtaClicked,
                substituteClickListener = viewModel::onSubstituteCtaClicked,
                completePickListener = viewModel::onCompletePickClicked,
                onCompleteButtonShown = viewModel::onCompleteButtonShown,
                currentItemPositionFlow = viewModel.getSelectedItemIndex(listType),
                animateItemComplete = if (pickListItem.sellByWeightInd == SellByType.PriceWeighted) true else pickListItem.id == completedItemIaId || pickListItem.id == shortedItemIaId,
                listType = listType,
                locationCount = viewModel.getLocationCount(pickListItem.itemId),
                dispatcherProvider = viewModel.dispatcherProvider,
                isTwoWayCommsEnabled = viewModel.isTwoWayCommsEnabled,
                isDisplayType3PWEnabled = viewModel.isDisplayType3PWEnabled,
                isRepickOriginalItemAllowed = viewModel.isRepickOriginalItemAllowed
            )
        }
    )
    return section
}

private fun RecyclerView.generatePickListHeader(groupHeader: String) = PickListItemHeaderViewModel(groupHeader)

@BindingAdapter("app:start_lottie_animation")
fun LottieAnimationView.startLottieAnimation(start: Boolean) {
    progress = if (start) 0f else 1f
    if (start) playAnimation()
}

@BindingAdapter(value = ["app:setButtonStyle", "app:setButtonEnabled"], requireAll = false)
fun MaterialButton.setButtonStyle(subAllowed: Boolean, enabled: Boolean) {
    backgroundTintList = if (!subAllowed || !enabled) context.getColorStateList(R.color.infoBlue3pct) else context.getColorStateList(R.color.infoBlue10pct)
    setTextColor(if (!subAllowed || !enabled) context.getColorStateList(R.color.lightButtonDisabledTextBlue) else context.getColorStateList(R.color.darkBlue))
}

@BindingAdapter(value = ["app:setCompleteButtonEnabled"], requireAll = false)
fun MaterialButton.setButtonStyle(enabled: Boolean) {
    backgroundTintList = if (!enabled) context.getColorStateList(R.color.disabledBlue) else context.getColorStateList(R.color.darkBlue)
    setTextColor(context.getColorStateList(R.color.white))
}

@BindingAdapter("app:isEbt")
fun View.setEbt(isEbt: Boolean) {
    setPadding(paddingLeft, paddingTop, if (isEbt) 96 else 16, paddingBottom)
}

@BindingAdapter("app:paddingRecyclerview", "app:pickingItems", "app:selectedItemIndex")
fun RecyclerView.setRecyclerViewPadding(isListView: Boolean, activity: PickListActivity?, selectedItemIndex: Int) {
    val topPadding = if (isListView) 8 else 16
    val bottomPadding = if (isListView) 41 else 16
    val horizontalPadding = if (isListView) 0 else 52
    setPadding(horizontalPadding, topPadding, horizontalPadding, bottomPadding)
}

@BindingAdapter(value = ["picklistItemsBottomPrompt", "isListView", "isShowingEmptyState"], requireAll = false)
fun AppCompatTextView.setPickListItemBottomPrompt(bottomPrompt: PickListItemsBottomPrompt, isListView: Boolean = false, isShowingEmptyState: Boolean = false) {
    val prompt = bottomPrompt.prompt?.let { prompt -> context.getString(prompt) }
    text = prompt
    val textColor = if (bottomPrompt.onClickPrompt != null) R.color.semiLightBlue else R.color.grey_700
    setTextColor(ContextCompat.getColor(context, textColor))
    setOnClickListener { bottomPrompt.onClickPrompt?.invoke() }
    setVisibilityGoneIfTrue(prompt.isNullOrEmpty() || isListView || isShowingEmptyState)
}

@BindingAdapter(value = ["picklistItemsBottomPrompt", "isListView", "isShowingEmptyState"], requireAll = false)
fun ImageView.setPickListItemBottomPrompt(bottomPrompt: PickListItemsBottomPrompt, isListView: Boolean = false, isShowingEmptyState: Boolean = false) {
    setOnClickListener { bottomPrompt.onClickKeyboard?.invoke() }
    setVisibilityGoneIfTrue(bottomPrompt.onClickKeyboard == null || isListView || isShowingEmptyState)
}

@BindingAdapter("app:bottomPrompt", "app:isListView", "app:isShowingEmptyState")
fun View.showScanBorder(bottomPrompt: PickListItemsBottomPrompt, isListView: Boolean, isShowingEmptyState: Boolean) {
    val prompt = bottomPrompt.prompt?.let { prompt -> context.getString(prompt) }
    setVisibilityGoneIfTrue(prompt.isNullOrEmpty() || isListView || isShowingEmptyState)
}

@BindingAdapter("app:isListView")
fun ConstraintLayout.setupBackground(isListView: Boolean) {
    setBackgroundColor(if (isListView) Color.parseColor("#FFFFFF") else Color.parseColor("#FAF9F8"))
}

@BindingAdapter("app:isListView")
fun TabLayout.setupBackground(isListView: Boolean) {
    background = (if (isListView) context.getDrawable(R.drawable.tab_background_selector) else null)
    if (!isListView) setBackgroundColor(Color.parseColor("#FFFFFF"))
}

@BindingAdapter("app:isAcceptedFlashOrder")
fun ConstraintLayout.setAcceptedFlashOrder(accepted: Boolean) {
    background = if (accepted) context.getDrawable(R.drawable.bordered_flash_acknowledge) else null
    if (!accepted) setBackgroundColor(Color.parseColor("#FFFFFF"))
}

@BindingAdapter(value = ["app:isBatchOrder", "app:onePLvanNumber", "app:updateFulfillment"])
fun AppCompatTextView.updateFulfillment(isBatchOrder: Boolean?, vanNumber: String?, fullFillmentAttr: FulfillmentAttributeDto?) {
    if (fullFillmentAttr?.type == FulfillmentType.DUG)
        text = context.resources.getString(R.string.dug)
    else if (fullFillmentAttr?.type == FulfillmentType.DELIVERY && fullFillmentAttr.subType == FulfillmentSubType.THREEPL)
        text = context.resources.getString(R.string.threepl)
    else if (fullFillmentAttr?.type == FulfillmentType.DELIVERY && fullFillmentAttr.subType == FulfillmentSubType.ONEPL)
        text = context.getString(R.string.one_pl_van_number, vanNumber)
    else
        text = null

    val fullFillmentTypeImage = when {
        fullFillmentAttr != null -> {
            ContextCompat.getDrawable(context, fullFillmentAttr.asIcon()) as Drawable
        }

        else -> {
            ContextCompat.getDrawable(context, android.R.color.transparent) as Drawable
        }
    }

    setCompoundDrawablesWithIntrinsicBounds(null, null, fullFillmentTypeImage, null)
}

@BindingAdapter("app:isEmptyStateAndToDoTab")
fun RecyclerView.setupBackground(isEmptyStateAndToDoTab: Boolean) {
    background = (if (isEmptyStateAndToDoTab) ContextCompat.getDrawable(context, R.color.backgroundGrey) else null)
}

const val ITEM_SUBSTITUTIONS = "item_substituitions"
const val ITEM_ISSUE_REPORTED = "item_issue_reported"
