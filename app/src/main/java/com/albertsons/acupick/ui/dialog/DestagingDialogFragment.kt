package com.albertsons.acupick.ui.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.DestagingDialogBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class DestagingDialogFragment : BaseCustomDialogFragment() {
    private val dialogViewmodel: DestagingDialogViewmodel by viewModel()

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<DestagingDialogBinding>(inflater, R.layout.destaging_dialog, container, false).apply {

            viewData = argData.toViewData(requireContext())
            viewModel = dialogViewmodel
            dialogViewmodel.navigation.observe(viewLifecycleOwner) { closeAction ->
                Timber.v("[setupBinding closeActionEvent] closeAction=$closeAction")
                dismiss()
                // Need to invoke this close action *after* the dialog has been dismissed to allow another dialog to be shown from the close action if desired.
                findDialogListener()?.onCloseAction(closeAction.first, closeAction.second)
            }
        }
    }
}
