package com.albertsons.acupick.ui.bottomsheetdialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ScanItemBottomSheetBinding

/** Bottom sheet for scanning an item */
class ScanItemBottomSheet : BaseBottomSheetDialogFragment() {

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<ScanItemBottomSheetBinding>(inflater, R.layout.scan_item_bottom_sheet, container, false).apply {
            viewData = argData.toViewData(requireContext())
            viewModel = fragmentViewModel
        }
    }
}
