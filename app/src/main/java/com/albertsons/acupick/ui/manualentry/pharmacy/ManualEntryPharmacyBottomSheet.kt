package com.albertsons.acupick.ui.manualentry.pharmacy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.setFragmentResult
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.BaseComposeViewBottomSheetBinding
import com.albertsons.acupick.ui.bottomsheetdialog.BaseBottomSheetDialogFragment
import com.albertsons.acupick.ui.manualentry.ManualEntryPharmacyParams
import com.albertsons.acupick.ui.manualentry.ManualEntryPharmacyUi
import com.albertsons.acupick.ui.manualentry.pharmacy.ManualEntryPharmacyViewModel.Companion.MANUAL_ENTRY_PHARMACY
import com.albertsons.acupick.ui.manualentry.pharmacy.ManualEntryPharmacyViewModel.Companion.MANUAL_ENTRY_PHARMACY_RESULTS
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ManualEntryPharmacyBottomSheet : BaseBottomSheetDialogFragment() {

    private val manualEntryPharmacyViewModel by viewModel<ManualEntryPharmacyViewModel>()

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        val binding = DataBindingUtil.inflate<BaseComposeViewBottomSheetBinding>(
            inflater, R.layout.base_compose_view_bottom_sheet, container, false
        )

        val param = argData.customDataParcel as ManualEntryPharmacyParams
        setupComposeView(binding, param)
        observeViewModel()

        return binding
    }

    private fun setupComposeView(binding: BaseComposeViewBottomSheetBinding, param: ManualEntryPharmacyParams) {
        binding.baseComposeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ManualEntryPharmacyScreen(viewModel = manualEntryPharmacyViewModel, manualEntryParams = param)
            }
        }
        manualEntryPharmacyViewModel.update(ManualEntryPharmacyUi(param))
    }

    private fun observeViewModel() {
        manualEntryPharmacyViewModel.returnManualEntryDataEvent.observe(viewLifecycleOwner) {
            Timber.d("1292 Manual Entry pharmacy fragment setFragmentResult")
            requireParentFragment().setFragmentResult(MANUAL_ENTRY_PHARMACY, bundleOf(MANUAL_ENTRY_PHARMACY_RESULTS to it))
            dismiss()
        }
    }
}
