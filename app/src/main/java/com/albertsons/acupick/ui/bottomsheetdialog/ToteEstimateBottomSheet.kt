package com.albertsons.acupick.ui.bottomsheetdialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ToteEstimate
import com.albertsons.acupick.databinding.ToteEstimateBottomSheetBinding

class ToteEstimateBottomSheet : BaseBottomSheetDialogFragment() {

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<ToteEstimateBottomSheetBinding>(inflater, R.layout.tote_estimate_bottom_sheet, container, false).apply {
            viewData = argData.toViewData(requireContext())
            viewModel = fragmentViewModel
            val params = argData.customDataParcel as ToteEstimate
            ambientTote = params.ambient
            chilledTote = params.chilled
        }
    }
}
