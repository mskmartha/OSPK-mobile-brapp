package com.albertsons.acupick.ui.manualentry.handoff

import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ManualEntryStagingMfcFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.manualentry.ManualEntryStagingUi
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryStagingViewModel.Companion.MANUAL_ENTRY_STAGING
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryStagingViewModel.Companion.MANUAL_ENTRY_STAGING_RESULTS
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ManualEntryStagingMfcFragment : BaseFragment<ManualEntryStagingMfcViewModel, ManualEntryStagingMfcFragmentBinding>() {

    override val fragmentViewModel: ManualEntryStagingMfcViewModel by viewModel()

    val args: ManualEntryStagingMfcFragmentArgs by navArgs()

    override fun getLayoutRes(): Int = R.layout.manual_entry_staging_mfc_fragment

    override fun setupBinding(binding: ManualEntryStagingMfcFragmentBinding) {
        super.setupBinding(binding)
        activityViewModel.setToolbarTitle(context?.getString(R.string.toolbar_title_manual_entry) ?: "")

        fragmentViewModel.manualEntryStagingUI.postValue(ManualEntryStagingUi(args.manualEntryParams))
        fragmentViewModel.returnMfcToteDataEvent.observe(viewLifecycleOwner) {
            Timber.d("1292 manualEntryHandoffMFC fragment setFragmentResult")
            setFragmentResult(MANUAL_ENTRY_STAGING, bundleOf(MANUAL_ENTRY_STAGING_RESULTS to it))
        }
    }
}
