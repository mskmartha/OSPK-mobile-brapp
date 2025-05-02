package com.albertsons.acupick.ui.staging.print

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.request.PrintBagLabelRequestDto
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StringIdHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class PrintLabelsViewModel(
    private val app: Application,
) : BaseViewModel(app) {

    private val dispatcherProvider: DispatcherProvider by inject()
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    private val apsRepo: ApsRepository by inject()

    val activityId = MutableLiveData<String>()
    private var isPrintTotes = false

    // Tote count info acts as input event to populate toteCountMap
    /** List of all selected label (and in the future possibly tote) ids */
    val selectedLabelIds = MutableLiveData<MutableList<String?>>(mutableListOf())

    val printButtonEnabled = selectedLabelIds.map { it.isNotEmpty() }

    val printLabelsList: LiveData<List<PrintLabelsHeaderItem>> = MutableLiveData()

    var isCustomerPreferBag = true

    init {
        registerCloseAction(LABELS_PRINTING_DIALOG_TAG) {
            closeActionFactory(positive = { _navigationEvent.postValue(NavigationEvent.Up) })
        }
    }

    fun setupLabelList(labelUiList: List<PrintLabelsHeaderUi>?, isCustomerPreferBag: Boolean) {
        if (labelUiList == null) return

        val groupedLabels = if (labelUiList.groupingBy { it.customerOrderNumber }.eachCount().size == 1) {
            labelUiList.groupBy { it.nameOrType }
        } else {
            labelUiList.groupBy { it.customerOrderNumber }
        }
        val headerItemList = groupedLabels.map { PrintLabelsHeaderItem(it.value.first(), fragmentViewModel = this) }
        isPrintTotes = labelUiList.any { it.isTotes == true }

        this.isCustomerPreferBag = isCustomerPreferBag

        when {
            !isCustomerPreferBag -> R.string.print_bag_labels_title
            isPrintTotes -> R.string.print_tote_labels_title
            else -> R.string.staging_reprint_bag_labels
        }.let { changeToolbarTitleEvent.set(app.getString(it)) }

        printLabelsList.set(headerItemList)
    }

    fun onPrintLabels() {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { onPrintLabels() }
            } else {
                when {
                    !isCustomerPreferBag -> printLabels(apsRepo::rePrintToteAndLooseLabels)
                    isPrintTotes -> apsRepo.printToteLabel(activityId.value.toString())
                    else -> printLabels(apsRepo::printBagLabelsForConts)
                }
            }
        }
    }

    private suspend fun printLabels(onResult: suspend (dto: PrintBagLabelRequestDto) -> ApiResult<Unit>) {
        if (selectedLabelIds.value.isNotNullOrEmpty()) {
            val result = isBlockingUi.wrap {
                onResult.invoke(
                    PrintBagLabelRequestDto(
                        actId = activityId.value?.toLongOrNull() ?: 0L,
                        containerIds = selectedLabelIds.value?.map { it!! }.orEmpty()
                    )
                )
            }
            when (result) {
                is ApiResult.Success -> showLabelSentToPrinterDialog()
                is ApiResult.Failure -> showSnackBar(AcupickSnackEvent(message = StringIdHelper.Id(R.string.error_printing_labels), SnackType.ERROR))
            }
        }
    }

    private fun showLabelSentToPrinterDialog() {
        val dialogBody =
            if (isPrintTotes) {
                StringIdHelper.Id(R.string.find_your_printer_totes)
            } else {
                StringIdHelper.Id(R.string.find_the_printer_to_collect_your_bag_and_loose_labels)
            }
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = CustomDialogArgData(
                    titleIcon = R.drawable.ic_sent_to_printer,
                    title = StringIdHelper.Id(R.string.sent_to_printer),
                    body = dialogBody,
                    positiveButtonText = StringIdHelper.Id(R.string.continue_cta),
                    negativeButtonText = null,
                    cancelOnTouchOutside = false,
                    cancelable = false,
                    dialogType = DialogType.ModalFiveConfirmation
                ),
                tag = LABELS_PRINTING_DIALOG_TAG
            )
        )
    }

    companion object {
        private const val LABELS_PRINTING_DIALOG_TAG = "labelsPrintingDialogTag"
    }
}
