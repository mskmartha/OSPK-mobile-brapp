package com.albertsons.acupick.ui.arrivals.destage

import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.albertsons.acupick.EventCategory
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.databinding.DestageOrderFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.arrivals.destage.reportmissingbag.ReportMissingBagBottomSheetDialogFragment
import com.albertsons.acupick.ui.arrivals.destage.reportmissingbag.ReportMissingBagBottomSheetDialogFragment.Companion.REPORT_MISSING_BAG_SHEET_TAG
import com.albertsons.acupick.ui.dialog.showWithFragment
import com.albertsons.acupick.ui.arrivals.destage.DestageOrderViewModel.Companion.DESTAGE_BOTTOM_SHEET_DIALOG_TAG
import com.albertsons.acupick.ui.util.EventAction
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.orTrue
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class DestageOrderFragment : BaseFragment<DestageOrderViewModel, DestageOrderFragmentBinding>() {
    override val fragmentViewModel: DestageOrderViewModel by viewModel()
    override fun getLayoutRes(): Int = R.layout.destage_order_fragment

    private val pagerViewModel: DestageOrderPagerViewModel by navGraphViewModels(R.id.destageOrderScope)

    private val args: DestageOrderPagerFragmentArgs by navArgs()

    override fun setupBinding(binding: DestageOrderFragmentBinding) {
        super.setupBinding(binding)
        binding.pagerVm = pagerViewModel

        binding.fragmentViewLifecycleOwner = viewLifecycleOwner

        // View Pager -> Fragment communication

        // Get static UI data from pager VM to display
        pagerViewModel.getUiDataForOrder(args.orderNumber).let {
            fragmentViewModel.detailsHeaderUi.value = it?.detailsHeaderUi
            fragmentViewModel.activity.value = it
            fragmentViewModel.rxBagList.value = it?.rxBags
            fragmentViewModel.hasAddOnPrescription.value = it?.hasAddOnPrescription
            pagerViewModel.apiRxBagList.value = it?.rxBags
            fragmentViewModel.isMfcOrder.value = it?.isMultiSource == true && it.type == ContainerType.TOTE
            fragmentViewModel.isCustomerBagPreference.value = it?.isCustomerBagPreference
            fragmentViewModel.populateRxOrderIds()
            fragmentViewModel.updateRejectedCount()
            pagerViewModel.firebaseAnalytics.logEvent(EventCategory.DE_STAGING, EventAction.SCREEN_VIEW, args.orderNumber)
        }
        pagerViewModel.showPerscriptionPickup.observe(viewLifecycleOwner) {
            fragmentViewModel.apply {
                isRxDug.value = it.orFalse()
                if (isRxDug.value != false) {
                    showRxScannedCount()
                }
            }
        }

        pagerViewModel.storageTypeEvent.observe(viewLifecycleOwner) {
            // for a batch order the event is observed in multiple fragments due to which the storage type dialog is called multiple times
            pagerViewModel.activeOrderNumber.value?.let { orderNumber ->
                if (args.orderNumber == orderNumber)
                    fragmentViewModel.getStorageType(it)
            }
        }

        pagerViewModel.arrivalLabel.observe(viewLifecycleOwner) {
            fragmentViewModel.arrivalLabel.value = it
        }

        pagerViewModel.completedRejectedItems.observe(viewLifecycleOwner) {
            fragmentViewModel.updateRejectedItemsVisibility(it)
        }

        pagerViewModel.currentBagLabel.observe(viewLifecycleOwner) {
            fragmentViewModel.currentBagLabel.value = it
        }

        fragmentViewModel.scannedOrderNumber.value = pagerViewModel.currentOrderNumber

        viewLifecycleOwner.lifecycleScope.launch {
            pagerViewModel.currentOrderHasLoosItem.collect {
                fragmentViewModel.hasLooseItem.value = it
            }
        }

        // Observe dynamic UI data from pager VM to update display
        viewLifecycleOwner.lifecycleScope.launch {
            pagerViewModel.zonedBagUiData.collect {
                fragmentViewModel.updateZonedBagUiData(it)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            pagerViewModel.incomingScannedRxBagsSharedFlow.collect {
                fragmentViewModel.updateRxCount(it)
            }
        }

        // Fragment -> View Pager communication
        fragmentViewModel.orderIssuesButtonEvent.observe(viewLifecycleOwner) { isMfc ->
            if (pagerViewModel.lockTab.value == false) {
                if (isMfc) pagerViewModel.showOrderIssueScanToteDialog() else pagerViewModel.showOrderIssueScanBagDialog()
            }
        }
        fragmentViewModel.pharmacySheetEvent.observe(viewLifecycleOwner) { isPartialRx ->
            val listener = object : DestageBottomSheetDialogFragment.DestageDialogListener {
                override fun reportIssue() {
                    fragmentViewModel.showPharmacyIssueModal()
                }

                override fun abandonPartialPrescriptionPickup() {
                    pagerViewModel.showLeavingRxScreenDialog(true)
                }
            }
            DestageBottomSheetDialogFragment.newInstance(isPartialRx, listener).showWithFragment(this, DESTAGE_BOTTOM_SHEET_DIALOG_TAG)
        }

        fragmentViewModel.bagBypassClickEvent.observe(viewLifecycleOwner) {
            val listener = object : ReportMissingBagBottomSheetDialogFragment.DestageBagBypassDialogListener {
                override fun missingLooseItemClicked() {
                    pagerViewModel.navigateToReportMissingLooseItemFragment(it)
                }

                override fun missingLooseItemLabelClicked() {
                    pagerViewModel.navigateToReportMissingLooseItemLabelFragment(it)
                }
                override fun missingBagLabelClicked() {
                    pagerViewModel.navigateToReportMissingBagOrToteLabelFragment(it)
                }

                override fun missingBagClicked() {
                    pagerViewModel.navigateToReportMissingBagOrToteFragment(it)
                }

                override fun cancelClicked() {
                    pagerViewModel.cancelOrderIssue()
                }
            }
            ReportMissingBagBottomSheetDialogFragment.newInstance(
                listener, fragmentViewModel.isMfcOrder.value ?: false,
                fragmentViewModel.hasLooseItem.value ?: false,
                fragmentViewModel.isCustomerBagPreference.value.orTrue()
            ).showWithFragment(
                this,
                REPORT_MISSING_BAG_SHEET_TAG
            )
        }

        fragmentViewModel.rxDeliveryFailureReason.observe(viewLifecycleOwner) {
            pagerViewModel.rxDeliveryFailureReason.value = it.orEmpty()
        }

        fragmentViewModel.rxDeliveryFailureReasonLiveEvent.observe(viewLifecycleOwner) {
            pagerViewModel.continueToHandoff()
        }

        fragmentViewModel.isRxComplete.observe(viewLifecycleOwner) { isRxComplete ->
            pagerViewModel.isRxComplete.value = isRxComplete.orFalse()
        }

        pagerViewModel.bagBypass.observe(viewLifecycleOwner) { bagBypass ->
            fragmentViewModel.updateBagBypass(bagBypass)
        }

        fragmentViewModel.isComplete.observe(viewLifecycleOwner) { isComplete ->
            viewLifecycleOwner.lifecycleScope.launch {
                pagerViewModel.orderCompletionUpdateEvent.emit(
                    OrderCompletionState(
                        customerOrderNumber = args.orderNumber,
                        isComplete = isComplete,
                    )
                )
            }
        }
        fragmentViewModel.reprintGiftNote.observe(viewLifecycleOwner) {
            pagerViewModel.launchGiftingDailog()
        }
        activityViewModel.navigationButtonIntercept.observe(viewLifecycleOwner) {
            if (fragmentViewModel.isRxDug.value.orFalse()) {
                // TODO: Add navigation parts here for the next screen. Not implemented yet.
            } else {
                findNavController().navigateUp()
            }
        }

        pagerViewModel.activeOrderNumber.observe(viewLifecycleOwner) {
            fragmentViewModel.onChangeActiveOrder(it, pagerViewModel.getUiDataForOrder(args.orderNumber))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            fragmentViewModel.selectedStorageType.collectLatest {
                // Send the data over the Shared Viemodel which is observed in the Destaging Manual entry fragment
                pagerViewModel.selectedStorageType.emit(it)
            }
        }
    }
}
