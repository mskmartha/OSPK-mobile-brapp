package com.albertsons.acupick.ui.staging

import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.databinding.StagingPart2FragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.arrivals.destage.OrderCompletionState
import com.albertsons.acupick.ui.dialog.CloseActionListenerProvider
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class StagingPart2Fragment : BaseFragment<StagingPart2ViewModel, StagingPart2FragmentBinding>(), CloseActionListenerProvider {
    override val fragmentViewModel: StagingPart2ViewModel by viewModel()
    override fun getLayoutRes(): Int = R.layout.staging_part2_fragment

    // Arguments passed thru from pager adapter
    private val args: StagingPart2PagerFragmentArgs by navArgs()

    // Pager VM for observing data updates.
    private val pagerVM: StagingPart2PagerViewModel by navGraphViewModels(R.id.stagingScope)

    override fun setupBinding(binding: StagingPart2FragmentBinding) {
        super.setupBinding(binding)
        binding.fragmentViewLifecycleOwner = viewLifecycleOwner
        binding.pagerVm = pagerVM

        // Get customer order number from incoming arguments
        val customerOrderNumber = args.stagingPart2Params.customerOrderNumber ?: ""

        // Relay Activity UI info
        viewLifecycleOwner.lifecycleScope.launch {
            pagerVM.getActivityByOrder(customerOrderNumber).collect {
                fragmentViewModel.activity.postValue(it)
                pagerVM.currentOrderShortId = it?.shortOrderId.orEmpty()
                pagerVM.currentCustomerName = it?.customerName.orEmpty()
                fragmentViewModel.isMultiSource.value = it?.isMultiSource
            }
        }

        // Initialize bag list
        pagerVM.bagsByOrder(customerOrderNumber).observe(viewLifecycleOwner) {
            fragmentViewModel.bagList.postValue(it.toMutableList())
        }

        // Update scanned bags list from pager VM to page VM; only pull related to this order.
        viewLifecycleOwner.lifecycleScope.launch {
            pagerVM.scannedBagsByOrder(customerOrderNumber).collect {
                fragmentViewModel.scannedBags.value = it
            }
        }

        // Update current zone from pager VM
        viewLifecycleOwner.lifecycleScope.launch {
            pagerVM.currentZone.collect {
                fragmentViewModel.currentZone.value = it
            }
        }

        // Send the order number of the most recently scanned bag,
        // this keeps the zones on batch orders only show as active on the correct order
        viewLifecycleOwner.lifecycleScope.launch {
            pagerVM.scannedOrderNumber.collect {
                fragmentViewModel.scannedOrderNumber.value = it
            }
        }

        // Update existing scanned zone location list from pageVM
        viewLifecycleOwner.lifecycleScope.launch {
            pagerVM.existingScannedZoneLocationsByOrder(customerOrderNumber).collect {
                fragmentViewModel.existingScannedZoneLocations.value = it
            }
        }

        // Update combined scanned zone location list (existing, scanned bags) from pageVM
        viewLifecycleOwner.lifecycleScope.launch {
            pagerVM.mergedZoneLocationsByOrder(customerOrderNumber).collect {
                fragmentViewModel.mergedZoneLocations.value = it
            }
        }

        // TODO - add HT to this when on the staging ticket
        fragmentViewModel.scrollToZone.observe(viewLifecycleOwner) { zoneType ->
            val scrollTo = when (zoneType) {
                StorageType.AM -> binding.ambientBanner.top
                StorageType.CH -> binding.chilledBanner.top
                StorageType.FZ -> binding.frozenBanner.top
                StorageType.HT -> binding.hotBanner.top
                else -> binding.ambientBanner.top
            } - binding.orderInfo.height

            binding.scrollView.post {
                binding.scrollView.smoothScrollTo(0, scrollTo)
            }
        }

        fragmentViewModel.isComplete.observe(viewLifecycleOwner) { isComplete ->
            viewLifecycleOwner.lifecycleScope.launch {
                pagerVM.orderCompletionUpdateEvent.emit(
                    OrderCompletionState(
                        customerOrderNumber = args.stagingPart2Params.customerOrderNumber.orEmpty(),
                        isComplete = isComplete,
                    )
                )
            }
        }
    }
}
