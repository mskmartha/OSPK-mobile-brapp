package com.albertsons.acupick.ui.staging

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.widget.ViewPager2
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.StagingPart2PagerFragmentBinding
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryStagingData
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryStagingViewModel
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryStagingViewModel.Companion.MANUAL_ENTRY_STAGING_REQUEST_KEY
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryStagingViewModel.Companion.MANUAL_ENTRY_STAGING_RESULTS
import com.albertsons.acupick.ui.notification.NotificationViewModel
import com.albertsons.acupick.ui.util.AcupickSnackbar
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class StagingPart2PagerFragment : BaseFragment<StagingPart2PagerViewModel, StagingPart2PagerFragmentBinding>() {
    override val fragmentViewModel: StagingPart2PagerViewModel by navGraphViewModels(R.id.stagingScope)
    override fun getLayoutRes(): Int = R.layout.staging_part2_pager_fragment

    private val notificationViewModel: NotificationViewModel by sharedViewModel()

    // Incoming arguments
    private val args: StagingPart2PagerFragmentArgs by navArgs()
    private var hasSetAdapter: Boolean = false

    override fun setupBinding(binding: StagingPart2PagerFragmentBinding) {
        super.setupBinding(binding)
        // Work with viewModel as receiver
        with(fragmentViewModel) {
            // Pass in input data
            pickingAcitivityId = args.stagingPart2Params.pickingActivityId
            stagingActivityId.value = args.stagingPart2Params.stagingActivityId
            doBagLabelsStillNeedToBePrinted.value = args.stagingPart2Params.isPrintingStillNeeded
            toteList.value = args.stagingPart2Params.toteList
            if (hasNavigatedToAddBags) {
                loadData()
                hasNavigatedToAddBags = false
            }

            if (hasNavigatedToUnAssignTote) {
                loadUnassignTote()
                hasNavigatedToUnAssignTote = false
            }

            // Observe main scan event and relay to view model
            activityViewModel.scannedData.observe(viewLifecycleOwner) {
                onScannerBarcodeReceived(it)
            }

            isBlockingUi.observe(viewLifecycleOwner) {
                activityViewModel.setLoadingState(it, true)
            }

            isStagingCompleted.observe(viewLifecycleOwner) {
                activityViewModel.setToolBarVisibility(!it)
            }

            // TODO - move to binding adapter
            // Notify view model when switching tabs
            binding.staging2ViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    /**
                     * Resets below zone and metric relevant values conditionally
                     *
                     * @param hasNavigatedToStagingOptions A Boolean indicating a new navigation event has triggered from staging to PickListSummary,
                     * BagLabels, etc.,
                     * `true` if it's a new navigation, `false` otherwise (e.g., internal tab switch).
                     */
                    if (!hasNavigatedToStagingOptions) {
                        // The below value needs to be reset whenever the picker changes tabs due to an internal tab switch (manual or automatic)
                        // it should not be cleared when navigating to a new screen.
                        currentZoneBarcode = ""
                        currentZone.value = null
                        scanContainerReasonCode = null
                    } else {
                        hasNavigatedToStagingOptions = false
                    }
                    pageEvent.value = position
                    updateCurrentOrder(tabs.value?.get(position)?.tabArgument?.stagingPart2Params)
                }
            })

            // Tabs are controlled by view model and relayed here to adapter
            tabs.observe(viewLifecycleOwner) { tabs ->
                with(binding.staging2ViewPager) {
                    if (!hasSetAdapter && tabs.isNotNullOrEmpty()) {
                        adapter = StagingPart2PagerAdapter(this@StagingPart2PagerFragment, tabs)
                        (adapter as StagingPart2PagerAdapter).menuTabMediatorFactory(binding.staging2TabLayout, binding.staging2ViewPager).attach()
                        hasSetAdapter = true
                    }
                }
            }

            isCompleteList.observe(viewLifecycleOwner) {
                it?.forEach { orderCompletionState ->
                    (binding.staging2ViewPager.adapter as StagingPart2PagerAdapter).let { stagingAdapter ->
                        val index = stagingAdapter.getIndexOfOrder(orderCompletionState.customerOrderNumber)
                        val tab = binding.staging2TabLayout.getTabAt(index)
                        tab?.icon = if (orderCompletionState.isComplete) {
                            ResourcesCompat.getDrawable(resources, R.drawable.ic_checkmark, null)
                        } else {
                            null
                        }
                    }
                }
            }

            // Observe ViewModel page event and set view pager to appropriate position
            viewLifecycleOwner.lifecycleScope.launch {
                pageEvent.collect {
                    if (binding.staging2ViewPager.currentItem != it) binding.staging2ViewPager.currentItem = it
                }
            }

            notificationViewModel.skipToDestagingAction.observe(viewLifecycleOwner) {
                fragmentViewModel.skipToDestaging()
            }
        }
        @SuppressLint("WrongConstant")
        binding.staging2ViewPager.offscreenPageLimit = OFFSCREEN_PAGE_LIMIT

        setUpFragmentResultListeners()

        fragmentViewModel.acupickSnackEventAnchored.observe(viewLifecycleOwner) {
            viewLifecycleOwner.lifecycleScope.launch {
                AcupickSnackbar.make(this@StagingPart2PagerFragment, it)
                    .setAnchorView(binding.promptBar)
                    .show()
            }
        }
    }

    private fun setUpFragmentResultListeners() {
        requireActivity().supportFragmentManager.setFragmentResultListener(MANUAL_ENTRY_STAGING_REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            // Timber.d("1292 MANUAL_ENTRY_STAGING Recieved")
            // // Any type can be passed via to the bundle
            // val manualEntryResults = bundle.get(MANUAL_ENTRY_STAGING_RESULTS)
            // (manualEntryResults as? ManualEntryStagingData)?.let { stagingData ->
            //     fragmentViewModel.isScanFromManualEntry.value = true
            //     stagingData.zone?.let {
            //         fragmentViewModel.onManualEntryBarcodeReceived(stagingData)
            //     }
            // }
            processManualEntry(bundle)
        }

        setFragmentResultListener(ManualEntryStagingViewModel.MANUAL_ENTRY_STAGING) { _, bundle ->
            processManualEntry(bundle)
        }
    }

    private fun processManualEntry(bundle: Bundle) {
        val manualEntryResults = bundle.get(MANUAL_ENTRY_STAGING_RESULTS)
        (manualEntryResults as? ManualEntryStagingData)?.let { stagingData ->
            fragmentViewModel.isScanFromManualEntry.value = true
            stagingData.zone?.let {
                fragmentViewModel.onManualEntryBarcodeReceived(stagingData)
            }
        }
    }

    //  TODO - is the need for clearing adapter here a symptom of leaking binding?
    override fun onDestroyView(binding: StagingPart2PagerFragmentBinding) {
        hasSetAdapter = false
        binding.apply { staging2ViewPager.adapter = null }
        super.onDestroyView(binding)
    }
}
