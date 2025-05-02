package com.albertsons.acupick.ui.staging

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.request.ContBagCountRequestDto
import com.albertsons.acupick.data.model.request.UpdateErContBagRequestDto
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.models.ToteUI
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.combineWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class AddBagsViewModel(
    app: Application,
) : BaseViewModel(app) {

    private val apsRepo: ApsRepository by inject()
    private val dispatcherProvider: DispatcherProvider by inject()

    val stagingActivityId = MutableStateFlow(0L)

    var toteUiList: MutableLiveData<MutableList<ToteUI>> = MutableLiveData(mutableListOf())
    val isCustomerPreferBag = MutableLiveData(true)

    val toteDbVms = toteUiList.combineWith(isCustomerPreferBag) { list, preferBag ->
        list?.map { AddBagsItemViewModel(it.copy(intialBagCount = 0, intialLooseCount = 0), null, preferBag ?: true, ::checkTotalCount) }
    }

    // Flags to control presence of cards in UI
    val hideAmbientCard = toteUiList.map { it -> it.none { it.storageType == StorageType.AM } }
    val hideChilledCard = toteUiList.map { it -> it.none { it.storageType == StorageType.CH } }
    val hideFrozenCard = toteUiList.map { it -> it.none { it.storageType == StorageType.FZ } }
    val hideHotCard = toteUiList.map { it -> it.none { it.storageType == StorageType.HT } }

    val isConfirmCtaEnabled: LiveData<Boolean> = MutableLiveData(false)

    fun onAddLabelsClicked() {
        viewModelScope.launch(dispatcherProvider.IO) {
            addLabels()
        }
    }

    private fun checkTotalCount() {
        isConfirmCtaEnabled.set((toteDbVms.value?.sumOf { (it.bagCount.value ?: 0) + (it.looseCount.value ?: 0) } ?: 0) > 0)
    }

    private fun addLabels() {
        viewModelScope.launch {
            val result = isBlockingUi.wrap {
                apsRepo.addBagCount(
                    UpdateErContBagRequestDto(
                        activityId = stagingActivityId.value,
                        contBagCountReqList = toteDbVms.value?.map {
                            ContBagCountRequestDto(
                                bagCount = it.bagCount.value,
                                containerId = it.item.toteId,
                                looseItemCount = it.looseCount.value,
                            )
                        }
                    )
                )
            }

            when (result) {
                is ApiResult.Failure -> {
                    handleApiError(result, retryAction = { addLabels() })
                }

                is ApiResult.Success -> {
                    showLabelSentToPrinterDialog()
                }
            }
        }
    }

    private fun showLabelSentToPrinterDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_alert,
                    title = StringIdHelper.Id(R.string.labels_printing),
                    body = StringIdHelper.Id(R.string.find_your_additional_labels),
                    positiveButtonText = StringIdHelper.Id(R.string.done),
                    negativeButtonText = null,
                    cancelOnTouchOutside = false,
                    cancelable = false
                ),
                tag = ADD_BAGS_LABEL_SENT_TO_PRINTER_DIALOG_TAG
            )
        )
    }

    init {
        isCustomerPreferBag.observeForever { preferBag ->
            changeToolbarTitleEvent.postValue(
                app.applicationContext.getString(
                    if (preferBag) R.string.add_bag_labels
                    else R.string.add_loose_labels
                )
            )
        }

        registerCloseAction(ADD_BAGS_LABEL_SENT_TO_PRINTER_DIALOG_TAG) {
            closeActionFactory(positive = { _navigationEvent.postValue(NavigationEvent.Up) })
        }
    }

    companion object {
        private const val ADD_BAGS_LABEL_SENT_TO_PRINTER_DIALOG_TAG = "addBagsLabelSentToPrinterDialogTag"
    }
}
