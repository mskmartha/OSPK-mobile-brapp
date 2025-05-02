package com.albertsons.acupick.ui.bottomsheetdialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.CollectToteLabelsBottomSheetBinding

class CollectToteLabelBottomSheet : BaseBottomSheetDialogFragment() {

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<CollectToteLabelsBottomSheetBinding>(inflater, R.layout.collect_tote_labels_bottom_sheet, container, false).apply {
            viewData = argData.toViewData(requireContext())
            viewModel = fragmentViewModel
        }
    }
}
