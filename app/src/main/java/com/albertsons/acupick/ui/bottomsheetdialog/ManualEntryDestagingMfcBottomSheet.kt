package com.albertsons.acupick.ui.bottomsheetdialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navGraphViewModels
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ManualEntryDestagingMfcBottomsheetBinding
import com.albertsons.acupick.infrastructure.utils.collectFlow
import com.albertsons.acupick.ui.arrivals.destage.DestageOrderPagerViewModel
import com.albertsons.acupick.ui.manualentry.ManualEntryHandOffUi
import com.albertsons.acupick.ui.manualentry.ManualEntryHandoffParams
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryBaseViewModel
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryHandOffBag
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryHandOffMfcViewModel
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryHandOffMfcViewModel.Companion.MANUAL_ENTRY_TOOL_TIP_TAG_REQUEST_KEY
import com.albertsons.acupick.ui.notification.NotificationViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

/**  Manual Entry picker bottom sheet for destaging */
class ManualEntryDestagingMfcBottomSheet : BaseBottomSheetDialogFragment() {

    private val fragmentVm: ManualEntryHandOffMfcViewModel by viewModel()
    private val notificationViewModel: NotificationViewModel by sharedViewModel()

    private val pagerViewModel: DestageOrderPagerViewModel by navGraphViewModels(R.id.destageOrderScope)

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {

        return DataBindingUtil.inflate<ManualEntryDestagingMfcBottomsheetBinding>(inflater, R.layout.manual_entry_destaging_mfc_bottomsheet, container, false).apply {
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
            collectFlow(fragmentVm.manualEntryTooltipEvent) {
                requireParentFragment().setFragmentResult(
                    MANUAL_ENTRY_TOOL_TIP_TAG_REQUEST_KEY, bundleOf()
                )
            }
            with(fragmentVm) {
                val params = argData.customDataParcel as ManualEntryHandoffParams
                manualEntryHandOffUI.postValue(ManualEntryHandOffUi(params))
                returnMfcToteDataEvent.observe(viewLifecycleOwner) {
                    Timber.d("1292 set bag data in retrun bag data event")
                    handleManualEntryData(it)
                }

                inlineStorageTypeDialogEvent.observe(viewLifecycleOwner) {
                    Timber.v("ManualEntryDestagingBottomSheet")
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
