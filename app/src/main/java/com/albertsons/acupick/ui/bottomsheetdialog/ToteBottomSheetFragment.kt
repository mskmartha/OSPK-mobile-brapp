package com.albertsons.acupick.ui.bottomsheetdialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ItemPickBottomSheetBinding

/** Bottom sheet for scanning a tote */
class ToteBottomSheetFragment : BaseBottomSheetDialogFragment() {

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<ItemPickBottomSheetBinding>(inflater, R.layout.item_pick_bottom_sheet, container, false).apply {
            fragmentViewLifecycleOwner = viewLifecycleOwner
            viewData = argData.toViewData(requireContext())
            viewModel = fragmentViewModel

            // TODO: ACURED_REDESIGN Refactored once item complete bottomsheet removed from all the flow
            sharedViewModel.bottomSheetRecordPickArgData.observe(viewLifecycleOwner) {
                if (it.dialogType == BottomSheetType.ToteScan) {
                   /* it?.let { argData ->
                        viewData = argData.toViewData(requireContext())
                    }*/
                    fragmentViewModel.dismissBottomSheet()
                }
            }
        }
    }
}
