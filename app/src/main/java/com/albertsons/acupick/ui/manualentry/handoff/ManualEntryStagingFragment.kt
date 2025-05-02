package com.albertsons.acupick.ui.manualentry.handoff

import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ManualEntryStagingFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.manualentry.ManualEntryStagingUi
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryStagingViewModel.Companion.MANUAL_ENTRY_STAGING
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryStagingViewModel.Companion.MANUAL_ENTRY_STAGING_RESULTS
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ManualEntryStagingFragment : BaseFragment<ManualEntryStagingViewModel, ManualEntryStagingFragmentBinding>() {

    override val fragmentViewModel: ManualEntryStagingViewModel by viewModel()

    val args: ManualEntryStagingFragmentArgs by navArgs()

    override fun getLayoutRes(): Int = R.layout.manual_entry_staging_fragment

    override fun setupBinding(binding: ManualEntryStagingFragmentBinding) {
        super.setupBinding(binding)

        activityViewModel.setToolbarTitle(context?.getString(R.string.toolbar_title_manual_entry) ?: "")

        // fragmentViewModel.manualEntryStagingUI.value = ManualEntryStagingUi(args.manualEntryParams)

        fragmentViewModel.update(ManualEntryStagingUi(args.manualEntryParams))

        fragmentViewModel.returnManualEntryStagingDataEvent.observe(viewLifecycleOwner) {
            Timber.d("1292 Manual Entry Staging fragment setFragmentResult")
            setFragmentResult(MANUAL_ENTRY_STAGING, bundleOf(MANUAL_ENTRY_STAGING_RESULTS to it))
        }
    }
}
