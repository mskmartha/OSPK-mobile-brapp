package com.albertsons.acupick.ui.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.ui.models.QuantityParams
import com.albertsons.acupick.databinding.QuantityPickerDialogBinding
import com.albertsons.acupick.ui.models.QuantityPickerUI
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class QuantityPickerDialogFragment : BaseCustomDialogFragment() {

    private val fragmentVm: QuantityPickerDialogViewModel by viewModel()
    private val siteRepo: SiteRepository by inject()

    override val shouldFillScreen
        get() = false

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<QuantityPickerDialogBinding>(inflater, R.layout.quantity_picker_dialog, container, false).apply {
            val quantityParams = argData.customData as QuantityParams
            lifecycleOwner = viewLifecycleOwner
            pickerUI = QuantityPickerUI(quantityParams, siteRepo.fixedItemTypesEnabled)
            viewModel = fragmentVm
            fragmentVm.quantityParams.postValue(quantityParams)

            fragmentVm.navigation.observe(viewLifecycleOwner) { closeAction ->
                Timber.v("[setupBinding closeActionEvent] closeAction=$closeAction")
                dismiss()
                // Need to invoke this close action *after* the dialog has been dismissed to allow another dialog to be shown from the close action if desired.
                findDialogListener()?.onCloseAction(closeAction.first, closeAction.second)
            }
        }
    }
}
