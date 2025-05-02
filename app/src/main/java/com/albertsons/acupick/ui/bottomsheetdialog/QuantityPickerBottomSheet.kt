package com.albertsons.acupick.ui.bottomsheetdialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.databinding.QuantityPickerBottomsheetBinding
import com.albertsons.acupick.ui.dialog.QuantityPickerDialogViewModel
import com.albertsons.acupick.ui.dialog.findDialogListener
import com.albertsons.acupick.ui.models.QuantityParams
import com.albertsons.acupick.ui.models.QuantityPickerUI
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

/**  Quantity picker bottom sheet for a scanned item/product */
class QuantityPickerBottomSheet : BaseBottomSheetDialogFragment() {

    private val fragmentVm: QuantityPickerDialogViewModel by viewModel()
    private val siteRepo: SiteRepository by inject()

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<QuantityPickerBottomsheetBinding>(inflater, R.layout.quantity_picker_bottomsheet, container, false).apply {
            val quantityParams = argData.customDataParcel as QuantityParams
            lifecycleOwner = viewLifecycleOwner
            pickerUI = QuantityPickerUI(quantityParams, siteRepo.fixedItemTypesEnabled)
            viewModel = fragmentVm
            fragmentVm.quantityParams.postValue(quantityParams)
            fragmentVm.navigation
                .observe(viewLifecycleOwner) { closeAction ->
                    Timber.v("[QuantityPickerBottomSheet setupBinding closeActionEvent] closeAction=$closeAction")
                    dismissBottomSheet()
                    // Need to invoke this close action *after* the dialog has been dismissed to allow another dialog to be shown from the close action if desired.
                    findDialogListener()?.onCloseAction(closeAction.first, closeAction.second)
                }
            fragmentVm.maxQuantity.observe(viewLifecycleOwner) {
                if (!quantityParams.isIssueScanning) {
                    quantityPicker.setMax(it)
                }
            }
        }
    }
}
