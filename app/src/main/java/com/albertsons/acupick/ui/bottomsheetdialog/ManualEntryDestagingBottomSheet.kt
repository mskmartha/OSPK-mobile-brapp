package com.albertsons.acupick.ui.bottomsheetdialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navGraphViewModels
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ManualEntryDestagingBottomsheetBinding
import com.albertsons.acupick.ui.arrivals.destage.DestageOrderPagerViewModel
import com.albertsons.acupick.ui.manualentry.ManualEntryHandOffUi
import com.albertsons.acupick.ui.manualentry.ManualEntryHandoffParams
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryBaseViewModel
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryHandOffBag
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryHandOffViewModel
import com.albertsons.acupick.ui.notification.NotificationViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**  Manual Entry picker bottom sheet for destaging */
class ManualEntryDestagingBottomSheet : BaseBottomSheetDialogFragment() {

    private val fragmentVm: ManualEntryHandOffViewModel by viewModel()
    private val notificationViewModel: NotificationViewModel by sharedViewModel()

    private val pagerViewModel: DestageOrderPagerViewModel by navGraphViewModels(R.id.destageOrderScope)

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {

        return DataBindingUtil.inflate<ManualEntryDestagingBottomsheetBinding>(inflater, R.layout.manual_entry_destaging_bottomsheet, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = fragmentVm
            notificationViewModel.notificationMessageSnackEvent.observe(viewLifecycleOwner) { snackBarEvent ->
                snackBarEvent?.let { it -> fragmentViewModel.showSnackBar(it) }
            }

            sharedViewModel.bottomSheetRecordPickArgData.observe(viewLifecycleOwner) {
                if (it.exit) {
                    dismiss()
                }
            }

            with(fragmentVm) {
                val params = argData.customDataParcel as ManualEntryHandoffParams
                manualEntryHandOffUI.postValue(ManualEntryHandOffUi(params))
                returnBagDataEvent.observe(viewLifecycleOwner) {
                    handleManualEntryData(it)
                }
                returnNonMfcToteDataEvent.observe(viewLifecycleOwner) {
                    handleManualEntryData(it)
                }
                inlineStorageTypeDialogEvent.observe(viewLifecycleOwner) {
                    pagerViewModel.storageTypeEvent.postValue(it)
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    pagerViewModel.selectedStorageType.collect {
                        fragmentVm.sendSelection(it)
                    }
                }
            }
        }
    }
    private fun handleManualEntryData(manualEntredData: ManualEntryHandOffBag) {
        requireActivity().supportFragmentManager.setFragmentResult(
            ManualEntryBaseViewModel.MANUAL_ENTRY_HANDOFF,
            bundleOf(ManualEntryBaseViewModel.MANUAL_ENTRY_HANDOFF_RESULTS to manualEntredData)
        )
        dismiss()
    }
}
