package com.albertsons.acupick.ui.manualentry.handoff

import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ManualEntryHandoffMfcFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.manualentry.ManualEntryHandOffUi
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryBaseViewModel.Companion.MANUAL_ENTRY_HANDOFF
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryBaseViewModel.Companion.MANUAL_ENTRY_HANDOFF_RESULTS
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ManualEntryHandOffMfcFragment : BaseFragment<ManualEntryHandOffMfcViewModel, ManualEntryHandoffMfcFragmentBinding>() {

    override val fragmentViewModel: ManualEntryHandOffMfcViewModel by viewModel()

    val args: ManualEntryHandOffMfcFragmentArgs by navArgs()

    override fun getLayoutRes(): Int = R.layout.manual_entry_handoff_mfc_fragment

    override fun setupBinding(binding: ManualEntryHandoffMfcFragmentBinding) {
        super.setupBinding(binding)
        activityViewModel.setToolbarTitle(context?.getString(R.string.toolbar_title_manual_entry) ?: "")
        activityViewModel.setToolbarNavigationIcon(context?.getDrawable(R.drawable.ic_back_arrow))

        fragmentViewModel.manualEntryHandOffUI.postValue(ManualEntryHandOffUi(args.manualEntryParams))
        fragmentViewModel.returnMfcToteDataEvent.observe(viewLifecycleOwner) {
            Timber.d("1292 manualEntryHandoffMFC fragment setFragmentResult")
            setFragmentResult(MANUAL_ENTRY_HANDOFF, bundleOf(MANUAL_ENTRY_HANDOFF_RESULTS to it))
        }
    }
}
