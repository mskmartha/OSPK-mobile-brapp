package com.albertsons.acupick.ui.staging.winestaging.weight

import android.app.Application
import android.view.View
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.BoxData
import com.albertsons.acupick.data.model.BoxDetails
import com.albertsons.acupick.data.model.BoxDetailsDto
import com.albertsons.acupick.data.model.BoxInfoDto
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.ui.staging.winestaging.WineStagingParams
import com.albertsons.acupick.data.model.response.WineStagingData
import com.albertsons.acupick.data.model.response.WineStagingType
import com.albertsons.acupick.data.model.toDomain
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.WineShippingRepository
import com.albertsons.acupick.data.repository.WineShippingStageStateRepository
import com.albertsons.acupick.databinding.ItemWineBoxWeightBinding
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogStyle
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.models.SnackBarEvent
import com.albertsons.acupick.ui.staging.winestaging.BoxSizeUiHeader
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.staging.winestaging.BoxUiData
import com.albertsons.acupick.ui.staging.winestaging.WineStaging3FragmentDirections
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.lang.Exception

class WineStaging2ViewModel(val app: Application) : BaseViewModel(app) {

    private val dispatcherProvider: DispatcherProvider by inject()
    private val wineRepo: WineShippingRepository by inject()
    private val wineStagingStateRepo: WineShippingStageStateRepository by inject()
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    val customerName = MutableLiveData("")
    val boxUiData = MutableLiveData<BoxUiData?>(null)
    val wineStagingParams = MutableLiveData<WineStagingParams?>(null)
    val pickedUpBox = MutableLiveData(0)
    val boxWeightUiItems: MutableLiveData<List<BoxWeightUiModel>> = MutableLiveData()
    val shouldDisablePrinting: LiveData<Boolean> = boxWeightUiItems.map { weight ->
        val weights = weight.map {
            try {
                it.totalWeight.value?.toDouble()
                true
            } catch (e: Exception) {
                false
            }
        }
        if (weights.isEmpty()) true else weights.contains(false)
    }

    var boxInfoDto: BoxInfoDto? = null

    init {
        registerCloseAction(BOX_SHIPPINGLABELS_PRINTING_DIALOG_TAG) {
            closeActionFactory(
                positive = { navigateToWineStaging3() },
                negative = { navigateToWineStaging3(true) }
            )
        }
    }

    fun fetchData(params: WineStagingParams?) {
        wineStagingParams.value = params
        viewModelScope.launch(dispatcherProvider.IO) {
            val result = isBlockingUi.wrap {
                wineRepo.getBoxDetails(params?.activityId.orEmpty())
            }
            when (result) {
                is ApiResult.Failure -> {
                    // val allBoxes = listOf<BoxWeightUiModel>(
                    //     BoxWeightUiModel(BoxType.MM, "SS-qweqwe-110022", "123123", MutableLiveData<String>(""), ::onItemClicked),
                    //     BoxWeightUiModel(BoxType.LL, "LL-qweqwe-110011", "123123", MutableLiveData<String>(""), ::onItemClicked)
                    // )
                    // boxWeightUiItems.postValue(allBoxes)
                    // pickedUpBox.postValue(2)
                    handleApiError(result)
                }
                is ApiResult.Success -> {
                    val boxes = result.data.boxDetails?.count()
                    boxInfoDto = result.data
                    boxUiData.postValue(result.data.toBoxUiData())
                    boxWeightUiItems.postValue(result.data.boxDetails?.map { it.toBoxWeightUiItems() })
                    pickedUpBox.postValue(boxes)
                }
            }
            wineStagingStateRepo.saveStagingPartOne(
                WineStagingData(
                    activityId = params?.activityId?.toIntOrNull(),
                    shorOrderId = params?.shortOrderNumber,
                    nextActivityId = WineStagingType.WineStaging2,
                    contactName = params?.contactName,
                    boxCount = pickedUpBox.value,
                    customerOrderNumber = params?.customerOrderNumber,
                    entityId = wineStagingParams.value?.entityId.orEmpty(),
                    stageByTime = wineStagingParams.value?.stageByTime.orEmpty(),
                    boxInfo = boxInfoDto?.boxDetails?.map { it.toBoxData() }
                ),
                params?.customerOrderNumber.orEmpty()
            )
        }
    }

    fun onPrintShippingLabel() {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first()) {
                val boxWeightDetail = boxWeightUiItems.value?.map {
                    BoxDetails(
                        referenceEntityId = wineStagingParams.value?.entityId,
                        weight = it.totalWeight.value ?: "0.0",
                        label = it.label,
                        type = it.boxType,
                        boxNumber = it.boxNumber
                    )
                }

                val labelQuantity = pickedUpBox.value?.toInt() ?: 0
                val result = isBlockingUi.wrap {
                    wineRepo.updateBoxWeight(wineStagingParams.value?.activityId.orEmpty(), boxWeightDetail)
                }
                when (result) {
                    is ApiResult.Failure -> {
                        handleApiError(result)
                    }
                    is ApiResult.Success -> {
                        wineStagingStateRepo.saveStagingPartOne(
                            WineStagingData(
                                activityId = wineStagingParams.value?.activityId?.toIntOrNull(),
                                shorOrderId = wineStagingParams.value?.shortOrderNumber.orEmpty(),
                                nextActivityId = WineStagingType.WineStaging3,
                                contactName = wineStagingParams.value?.contactName.orEmpty(),
                                boxCount = pickedUpBox.value,
                                customerOrderNumber = wineStagingParams.value?.customerOrderNumber.orEmpty(),
                                entityId = wineStagingParams.value?.entityId,
                                stageByTime = wineStagingParams.value?.stageByTime,
                                boxInfo = boxInfoDto?.boxDetails?.map { it.toBoxData() }
                            ),
                            wineStagingParams.value?.customerOrderNumber.orEmpty()
                        )

                        attemptToPrintBagLabels(labelQuantity)
                    }
                }
            } else {
                networkAvailabilityManager.triggerOfflineError { onPrintShippingLabel() }
            }
        }
    }

    private suspend fun attemptToPrintBagLabels(labelQuantity: Int) {
        showLabelSentToPrinterDialog(labelQuantity)
        val printedSuccess = (
            isBlockingUi.wrap {
                boxInfoDto?.let {
                    wineRepo.printBoxShippingLabels(
                        wineStagingParams.value?.activityId?.toIntOrNull(),
                        wineStagingParams.value?.customerOrderNumber?.toIntOrNull(),
                        wineStagingParams.value?.entityId?.toLongOrNull()
                    )
                }
            }
            ) is ApiResult.Success
        if (printedSuccess.not()) {
            withContext(dispatcherProvider.Main) {
                showSnackBar(SnackBarEvent(prompt = StringIdHelper.Id(R.string.error_printing_labels), null))
            }
        }
    }

    private fun showLabelSentToPrinterDialog(labelQuantity: Int) {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    dialogStyle = DialogStyle.PrintShippingLabel,
                    title = StringIdHelper.Plural(R.plurals.box_shipping_label_modal_header, labelQuantity),
                    body = StringIdHelper.Id(R.string.box_shipping_label_modal_body),
                    shouldBoldTitle = true,
                    positiveButtonText = StringIdHelper.Id(R.string.ok),
                    negativeButtonText = StringIdHelper.Id(R.string.skip),
                    cancelOnTouchOutside = false,
                    cancelable = false
                ),
                tag = BOX_SHIPPINGLABELS_PRINTING_DIALOG_TAG
            )
        )
    }

    fun BoxDetailsDto.toBoxWeightUiItems(): BoxWeightUiModel {
        return BoxWeightUiModel(
            boxNumber = this.boxNumber ?: "",
            boxType = this.type?.toDomain(),
            label = this.label ?: "",
            totalWeight = MutableLiveData<String>(""),
            onClick = ::onItemClicked
        )
    }

    fun onItemClicked(boxWeightUiModel: BoxWeightUiModel) {
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                WineStaging2FragmentDirections.actionToBoxInputWeightFragment(
                    weight = boxWeightUiModel.totalWeight.value?.toString() ?: "",
                    boxLabel = boxWeightUiModel.formattedBoxLabel,
                )
            )
        )
    }

    fun setupHeader(params: WineStagingParams?) {
        wineStagingParams.value = params
        customerName.set(params?.contactName)
        changeToolbarTitleEvent.postValue(app.getString(R.string.toolbar_stage_by_format, wineStagingParams.value?.stageByTime.orEmpty()))
    }

    fun updateWeight(boxWeight: String, boxlabel: String?) {
        boxWeightUiItems.set(
            boxWeightUiItems.value?.map {
                if (boxlabel == it.formattedBoxLabel) {
                    it.totalWeight.set(boxWeight)
                    it
                } else { it }
            }
        )
    }
    private fun navigateToWineStaging3(showReminder: Boolean = false) {
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                WineStaging3FragmentDirections.actionToWineStaging3Fragment(
                    boxUiData = boxUiData.value,
                    wineStagingParams = WineStagingParams(
                        activityId = wineStagingParams.value?.activityId.orEmpty(),
                        entityId = wineStagingParams.value?.entityId.orEmpty(),
                        contactName = wineStagingParams.value?.contactName.orEmpty(),
                        shortOrderNumber = wineStagingParams.value?.shortOrderNumber.orEmpty(),
                        customerOrderNumber = wineStagingParams.value?.customerOrderNumber.orEmpty(),
                        stageByTime = wineStagingParams.value?.stageByTime.orEmpty(),
                        pickedUpBottleCount = wineStagingParams.value?.pickedUpBottleCount.orEmpty()
                    ),
                    shouldShowPrintReminder = showReminder
                )
            )
        )
    }

    companion object {
        private const val BOX_SHIPPINGLABELS_PRINTING_DIALOG_TAG = "boxShippingLabesPrintingDialogTag"
    }
}

@BindingAdapter(value = ["app:boxWeightItems", "app:fragmentLifecycleOwner"], requireAll = false)
fun RecyclerView.setBoxSizeItems(boxWeightUiItems: List<BoxWeightUiModel>?, fragmentLifecycleOwner: LifecycleOwner?) {
    layoutManager = LinearLayoutManager(context)
    @Suppress("UNCHECKED_CAST")
    adapter = GroupieAdapter().apply { generateBoxWeightGroup(fragmentLifecycleOwner, boxWeightUiItems) }
}

private fun GroupieAdapter.generateBoxWeightGroup(
    fragmentLifecycleOwner: LifecycleOwner?,
    boxWeightUiItems: List<BoxWeightUiModel>?,
) {
    val group = Section().apply {
        add(BoxSizeUiHeader(2))
        boxWeightUiItems?.map { add(BoxWeightUi(fragmentLifecycleOwner, it)) }
    }
    updateAsync(mutableListOf(group))
}

class BoxWeightUi(val fragmentLifecycleOwner: LifecycleOwner?, private val boxWeightUiModel: BoxWeightUiModel) : BindableItem<ItemWineBoxWeightBinding>() {
    override fun initializeViewBinding(view: View): ItemWineBoxWeightBinding = ItemWineBoxWeightBinding.bind(view)
    override fun getLayout(): Int = R.layout.item_wine_box_weight
    override fun bind(viewBinding: ItemWineBoxWeightBinding, position: Int) {
        viewBinding.viewModel = boxWeightUiModel
        viewBinding.fragmentLifecycleOwner = fragmentLifecycleOwner
    }
}

fun BoxInfoDto.toBoxUiData(): BoxUiData = BoxUiData(
    this.boxDetails?.map {
        BoxData(
            referenceEntityId = it.referenceEntityId.orEmpty(),
            type = it.type?.name.orEmpty(),
            orderNumber = it.orderNumber.orEmpty(),
            boxNumber = it.boxNumber.orEmpty(),
            label = it.label.orEmpty(),
            zoneType = StorageType.AM
        )
    }.orEmpty()
)

fun BoxDetailsDto.toBoxData() = BoxData(
    referenceEntityId = this.referenceEntityId.orEmpty(),
    type = this.type?.name.orEmpty(),
    orderNumber = this.orderNumber.orEmpty(),
    boxNumber = this.boxNumber.orEmpty(),
    label = this.label.orEmpty(),
    zoneType = StorageType.AM
)
