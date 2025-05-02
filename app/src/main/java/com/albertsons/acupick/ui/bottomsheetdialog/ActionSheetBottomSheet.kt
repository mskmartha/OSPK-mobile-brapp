package com.albertsons.acupick.ui.bottomsheetdialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ActionSheetBottomSheetBinding
import com.albertsons.acupick.ui.dialog.findDialogListener
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ActionSheetBottomSheet : BaseBottomSheetDialogFragment() {

    private val fragmentVm: ActionSheetBottomSheetViewModel by viewModel()

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<ActionSheetBottomSheetBinding>(inflater, R.layout.action_sheet_bottom_sheet, container, false).apply {
            val actionSheetDetails = argData.customDataParcel as ActionSheetDetails
            fragmentVm.options.addAll(actionSheetDetails.options)
            viewModel = fragmentVm
            fragmentVm.navigation
                .observe(viewLifecycleOwner) { closeAction ->
                    Timber.v("[QuantityPickerBottomSheet setupBinding closeActionEvent] closeAction=$closeAction")
                    dismiss()
                    // Need to invoke this close action *after* the dialog has been dismissed to allow another dialog to be shown from the close action if desired.
                    findDialogListener()?.onCloseAction(closeAction.first, closeAction.second)
                }
        }
    }

    override fun getTheme(): Int {
        return R.style.ThemeOverlay_MaterialComponents_BottomSheetDialog
    }
}
