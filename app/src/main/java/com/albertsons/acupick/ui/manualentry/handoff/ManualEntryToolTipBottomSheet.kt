package com.albertsons.acupick.ui.manualentry.handoff

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ManualEntryToolTipFragmentBinding
import com.albertsons.acupick.infrastructure.utils.collectFlow
import com.albertsons.acupick.ui.bottomsheetdialog.BaseBottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class ManualEntryToolTipBottomSheet : BaseBottomSheetDialogFragment() {
    private val manualEntryViewModel by viewModel<ManualEntryToolTipViewModel>()
    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<ManualEntryToolTipFragmentBinding>(inflater, R.layout.manual_entry_tool_tip_fragment, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = manualEntryViewModel
            collectFlow(manualEntryViewModel.backButtonEvent) {
                dismiss()
            }
        }
    }
}
