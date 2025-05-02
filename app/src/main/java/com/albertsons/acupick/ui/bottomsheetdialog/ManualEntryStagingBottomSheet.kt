package com.albertsons.acupick.ui.bottomsheetdialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.setFragmentResult
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ManualEntryStagingBottomsheetBinding
import com.albertsons.acupick.ui.manualentry.ManualEntryStagingParams
import com.albertsons.acupick.ui.manualentry.ManualEntryStagingUi
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryStagingViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**  Manual Entry picker bottom sheet for bag scanning */
class ManualEntryStagingBottomSheet : BaseBottomSheetDialogFragment() {

    private val fragmentVm: ManualEntryStagingViewModel by viewModel()

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {

        return DataBindingUtil.inflate<ManualEntryStagingBottomsheetBinding>(inflater, R.layout.manual_entry_staging_bottomsheet, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = fragmentVm

            val params = argData.customDataParcel as ManualEntryStagingParams
            fragmentVm.manualEntryStagingUI.value = ManualEntryStagingUi(params)

            fragmentVm.returnManualEntryStagingDataEvent.observe(viewLifecycleOwner) {
                // TODO_ACURED Need to refactor or remove this setFragment Result when we are handling the MFC and WineShipping
                setFragmentResult(ManualEntryStagingViewModel.MANUAL_ENTRY_STAGING, bundleOf(ManualEntryStagingViewModel.MANUAL_ENTRY_STAGING_RESULTS to it))
                requireActivity().supportFragmentManager.setFragmentResult(
                    ManualEntryStagingViewModel.MANUAL_ENTRY_STAGING_REQUEST_KEY,
                    bundleOf(ManualEntryStagingViewModel.MANUAL_ENTRY_STAGING_RESULTS to it)
                )
                dismissBottomSheet()
            }
        }
    }
}
