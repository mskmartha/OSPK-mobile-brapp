package com.albertsons.acupick.ui.staging.winestaging

import android.app.Application
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.BoxType
import com.albertsons.acupick.data.repository.WineShippingRepository
import com.albertsons.acupick.databinding.ItemBoxSizeBinding
import com.albertsons.acupick.databinding.ItemBoxSizeHeaderBinding
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.data.model.BoxCountPerOrder
import com.albertsons.acupick.data.model.request.BoxTypeCount
import com.albertsons.acupick.data.model.response.WineStagingData
import com.albertsons.acupick.data.model.response.WineStagingType
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.WineShippingStageStateRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.models.SnackBarEvent
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.transform
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class WineStagingViewModel(val app: Application) : BaseViewModel(app) {
    private val apsRepo: ApsRepository by inject()
    private val dispatcherProvider: DispatcherProvider by inject()
    private val wineRepo: WineShippingRepository by inject()
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    private val wineStagingStateRepo: WineShippingStageStateRepository by inject()
    val boxSizeUiItems: List<BoxSizeUiModel> = boxSizeList()
    private val totalQuantity: Flow<Int> = combine(
        boxSizeUiItems.map { it.totalQuantity.asFlow() }
    ) { ar: Array<Int> ->
        ar.sum()
    }
    val isCtaEnabled = totalQuantity.asLiveData().transform { (it ?: 0) > 0 }
    val shortOrderNumber = MutableLiveData("")
    val longOrderNumber = MutableLiveData("")
    val customerName = MutableLiveData("")
    val stageByTime = MutableLiveData("")
    val pickedUpBottles = MutableLiveData<Int?>(null)
    var entityId = ""
    var wineStagingParams: WineStagingParams? = null
    val actId = MutableLiveData("")
    val nextActivityId = MutableLiveData("")

    fun setupHeader(params: WineStagingParams) {
        wineStagingParams = params
        shortOrderNumber.set(params.shortOrderNumber)
        longOrderNumber.set(params.customerOrderNumber)
        customerName.set(params.contactName)
        stageByTime.set(params.stageByTime)
        actId.set(params.activityId)
        pickedUpBottles.set(params.pickedUpBottleCount.toIntOrNull() ?: 0)
        entityId = params.entityId
        changeToolbarTitleEvent.postValue(app.getString(R.string.toolbar_stage_by_format, params.stageByTime))
        // cache data
        wineStagingStateRepo.saveStagingPartOne(
            WineStagingData(
                activityId = actId.value?.toIntOrNull(),
                shorOrderId = shortOrderNumber.value,
                bottleCount = pickedUpBottles.value,
                nextActivityId = WineStagingType.WineStaging1,
                contactName = customerName.value,
                customerOrderNumber = longOrderNumber.value,
                entityId = entityId,
                stageByTime = stageByTime.value
            ),
            longOrderNumber.value.orEmpty()
        )
    }

    init {
        registerCloseAction(BOX_LABELS_PRINTING_DIALOG_TAG) {
            closeActionFactory(positive = { navigateToWineShippingStage2() })
        }
    }
    fun printBoxLabelsClicked() {
        viewModelScope.launch {
            if (networkAvailabilityManager.isConnected.first()) {
                val boxTypeCount = boxSizeUiItems.map {
                    BoxTypeCount(it.boxSize, it.totalQuantity())
                }.filterNot { it.count == 0 }
                val labelQuantity = boxSizeUiItems.sumOf { it.totalQuantity.value ?: 0 }
                val boxCountPerOrders = listOf(BoxCountPerOrder(entityId, boxTypeCount))
                viewModelScope.launch(dispatcherProvider.IO) {
                    val result = isBlockingUi.wrap {
                        wineRepo.addBoxCount(actId.value.orEmpty(), boxCountPerOrders)
                    }
                    when (result) {
                        is ApiResult.Failure -> {
                            handleApiError(result)
                        }
                        is ApiResult.Success -> {
                            nextActivityId.postValue(result.data.nextActivityId.toString())
                            wineStagingStateRepo.saveStagingPartOne(
                                WineStagingData(
                                    activityId = result.data.nextActivityId, // persist staging activityId
                                    shorOrderId = shortOrderNumber.value,
                                    bottleCount = pickedUpBottles.value,
                                    nextActivityId = WineStagingType.WineStaging2,
                                    contactName = customerName.value,
                                    customerOrderNumber = longOrderNumber.value,
                                    entityId = entityId,
                                    stageByTime = stageByTime.value
                                ),
                                longOrderNumber.value.orEmpty()
                            )
                            attemptToPrintBagLabels(result.data.nextActivityId?.toLong(), labelQuantity) // use staing activityId
                        }
                    }
                }
            } else {
                networkAvailabilityManager.triggerOfflineError { printBoxLabelsClicked() }
            }
        }
    }
    private suspend fun attemptToPrintBagLabels(activityIdString: Long?, labelQuantity: Int) {
        val printedSuccess = (isBlockingUi.wrap { apsRepo.printBagLabels(activityIdString.toString()) }) is ApiResult.Success
        if (printedSuccess.not()) {
            withContext(dispatcherProvider.Main) {
                showSnackBar(SnackBarEvent(prompt = StringIdHelper.Id(R.string.error_printing_labels), null))
            }
        } else {
            showLabelSentToPrinterDialog(labelQuantity)
        }
    }

    private fun showLabelSentToPrinterDialog(labelQuantity: Int) {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.box_label_modal_header),
                    body = StringIdHelper.Plural(R.plurals.box_label_modal_body, labelQuantity),
                    shouldBoldTitle = true,
                    positiveButtonText = StringIdHelper.Id(R.string.ok),
                    negativeButtonText = null,
                    cancelOnTouchOutside = false,
                    cancelable = false
                ),
                tag = BOX_LABELS_PRINTING_DIALOG_TAG
            )
        )
    }

    private fun boxSizeList() = listOf(
        BoxSizeUiModel(BoxType.XS, MutableLiveData<Int>(0)),
        BoxSizeUiModel(BoxType.SS, MutableLiveData<Int>(0)),
        BoxSizeUiModel(BoxType.MM, MutableLiveData<Int>(0)),
        BoxSizeUiModel(BoxType.LL, MutableLiveData<Int>(0)),
        BoxSizeUiModel(BoxType.XL, MutableLiveData<Int>(0))
    )

    private fun navigateToWineShippingStage2() {
        viewModelScope.launch(dispatcherProvider.Main) {
            if (networkAvailabilityManager.isConnected.first()) {
                _navigationEvent.postValue(
                    NavigationEvent.Directions(
                        WineStagingFragmentDirections.actionToWineStaging2Fragment(
                            wineStagingParams = wineStagingParams?.copy(activityId = nextActivityId.value.orEmpty())
                        )
                    )
                )
            } else {
                networkAvailabilityManager.triggerOfflineError { navigateToWineShippingStage2() }
            }
        }
    }
    companion object {
        private const val BOX_LABELS_PRINTING_DIALOG_TAG = "boxLabesPrintingDialogTag"
    }
}

@BindingAdapter(value = ["app:boxSizeItems", "app:fragmentLifecycleOwner"], requireAll = false)
fun RecyclerView.setBoxSizeItems(boxSizeUiItems: List<BoxSizeUiModel>, fragmentLifecycleOwner: LifecycleOwner?) {
    layoutManager = LinearLayoutManager(context)
    @Suppress("UNCHECKED_CAST")
    adapter = GroupieAdapter().apply { generateBoxSizeGroup(fragmentLifecycleOwner, boxSizeUiItems) }
}

fun GroupieAdapter.generateBoxSizeGroup(
    fragmentLifecycleOwner: LifecycleOwner?,
    boxSizeUiItems: List<BoxSizeUiModel>
) {
    val group = Section().apply {
        add(BoxSizeUiHeader(1))
        boxSizeUiItems.map { add(BoxSizeUi(fragmentLifecycleOwner, it)) }
    }
    updateAsync(mutableListOf(group))
}

class BoxSizeUiHeader(val step: Int) : BindableItem<ItemBoxSizeHeaderBinding>() {
    override fun initializeViewBinding(view: View) = ItemBoxSizeHeaderBinding.bind(view)
    override fun getLayout() = R.layout.item_box_size_header
    override fun bind(viewBinding: ItemBoxSizeHeaderBinding, position: Int) {
        viewBinding.stepCount = step
        viewBinding.instruction = when (step) {
            1 -> viewBinding.root.context.getString(R.string.box_size_subheader)
            2 -> viewBinding.root.context.getString(R.string.box_weight_subheader)
            else -> { viewBinding.root.context.getString(R.string.box_size_subheader) }
        }
    }
}
class BoxSizeUi(val fragmentLifecycleOwner: LifecycleOwner?, private val boxSizeUiItem: BoxSizeUiModel) : BindableItem<ItemBoxSizeBinding>() {
    private val MAX_QUANTITY = 99
    override fun initializeViewBinding(view: View) = ItemBoxSizeBinding.bind(view)
    override fun getLayout() = R.layout.item_box_size
    override fun bind(viewBinding: ItemBoxSizeBinding, position: Int) {
        viewBinding.viewModel = boxSizeUiItem
        viewBinding.boxCapacityQuantity.text = "0"

        viewBinding.fragmentLifecycleOwner = fragmentLifecycleOwner
        viewBinding.capacityQuantityMinus.setImageDrawable(ContextCompat.getDrawable(viewBinding.root.context, R.drawable.ic_minus_disabled))
        viewBinding.capacityQuantityMinus.setOnClickListener {
            val quantity = viewBinding.boxCapacityQuantity.text?.toString()?.toInt() ?: 0
            viewBinding.boxCapacityQuantity.text = viewBinding.viewModel?.totalQuantity.decrementQuantity(quantity, 0)
            updateQuantityButton(viewBinding)
        }
        viewBinding.capacityQuantityPlus.setOnClickListener {
            val quantity = viewBinding.boxCapacityQuantity.text?.toString()?.toInt() ?: 0
            viewBinding.boxCapacityQuantity.text = viewBinding.viewModel?.totalQuantity.incrementQuantity(quantity, MAX_QUANTITY)
            updateQuantityButton(viewBinding)
        }
    }
    private fun updateQuantityButton(viewBinding: ItemBoxSizeBinding) {
        viewBinding.capacityQuantityMinus.setImageDrawable(ContextCompat.getDrawable(viewBinding.root.context, setNegativeDrawable(viewBinding.boxCapacityQuantity.text?.toString()?.toInt() ?: 0)))
        viewBinding.capacityQuantityPlus.setImageDrawable(
            ContextCompat.getDrawable(
                viewBinding.root.context,
                setPositiveDrawable(
                    viewBinding.boxCapacityQuantity.text?.toString()?.toInt() ?: 0, MAX_QUANTITY
                )
            )
        )
    }

    private fun MutableLiveData<Int>?.incrementQuantity(quantity: Int, maxQuantity: Int): String {
        this?.value = quantity.plus(1).coerceAtMost(maxQuantity)
        return this?.value?.toString().orEmpty()
    }
    private fun MutableLiveData<Int>?.decrementQuantity(quantity: Int, minValue: Int): String {
        this?.value = quantity.minus(1).coerceAtLeast(minValue)
        return this?.value?.toString().orEmpty()
    }
    private fun setPositiveDrawable(quantity: Int, maxQuantity: Int) = if (quantity == maxQuantity) R.drawable.ic_plus_disabled else R.drawable.ic_plus_enabled
    private fun setNegativeDrawable(quantity: Int) = if (quantity > 0) R.drawable.ic_minus_enabled else R.drawable.ic_minus_disabled
}
