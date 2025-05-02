package com.albertsons.acupick.ui.manualentry.handoff

import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ManualEntryHandoffFragmentBinding
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.manualentry.ManualEntryHandOffUi
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryBaseViewModel.Companion.MANUAL_ENTRY_HANDOFF
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryBaseViewModel.Companion.MANUAL_ENTRY_HANDOFF_RESULTS
import com.albertsons.acupick.ui.notification.NotificationViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ManualEntryHandOffFragment : BaseFragment<ManualEntryHandOffViewModel, ManualEntryHandoffFragmentBinding>() {

    override val fragmentViewModel: ManualEntryHandOffViewModel by viewModel()
    private val notificationViewModel: NotificationViewModel by sharedViewModel()

    val args: ManualEntryHandOffFragmentArgs by navArgs()

    override fun getLayoutRes(): Int = R.layout.manual_entry_handoff_fragment

    override fun setupBinding(binding: ManualEntryHandoffFragmentBinding) {
        super.setupBinding(binding)
        activityViewModel.setToolbarTitle(context?.getString(R.string.toolbar_title_manual_entry) ?: "")
        notificationViewModel.notificationMessageSnackEvent.observe(viewLifecycleOwner) { snackBarEvent ->
            snackBarEvent?.let { it -> fragmentViewModel.showSnackBar(it) }
        }

        with(fragmentViewModel) {
            manualEntryHandOffUI.postValue(ManualEntryHandOffUi(args.manualEntryParams))
            returnBagDataEvent.observe(viewLifecycleOwner) {
                Timber.d("1292 set bag data in retrun bag data event")
                setFragmentResult(MANUAL_ENTRY_HANDOFF, bundleOf(MANUAL_ENTRY_HANDOFF_RESULTS to it))
            }
            returnNonMfcToteDataEvent.observe(viewLifecycleOwner) {
                Timber.d("ManualEntryHandoffNonMfcTote fragment setFragmentResult")
                setFragmentResult(MANUAL_ENTRY_HANDOFF, bundleOf(MANUAL_ENTRY_HANDOFF_RESULTS to it))
            }
            toteIdEntryText.observe(viewLifecycleOwner) {
                if (it.isNotNullOrBlank()) {
                    clearToteIdError()
                    clearBagError()
                    bagsEntryText.postValue("")
                }
            }
            bagsEntryText.observe(viewLifecycleOwner) {
                if (it.isNotNullOrBlank()) {
                    clearBagError()
                    clearToteIdError()
                    toteIdCapText.postValue("")
                }
            }
        }
    }
}
