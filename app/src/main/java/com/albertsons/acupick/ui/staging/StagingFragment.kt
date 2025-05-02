package com.albertsons.acupick.ui.staging

import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.StagingFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class StagingFragment : BaseFragment<StagingViewModel, StagingFragmentBinding>() {
    override val fragmentViewModel: StagingViewModel by viewModel()

    override fun getLayoutRes(): Int = R.layout.staging_fragment

    //  TODO - If we need this model to persist across whole app and not just between staging screens
    //     (allowing data persistence for back nav )
    //   Then change to a normal shared provider and it will be scoped globally
    // stagingViewModel will manage data load, and grouping.
    private val stagingPagerViewModel: StagingPagerViewModel by navGraphViewModels(R.id.stagingScope)

    // Reusing nav args from pager fragment to cary info
    private val args: StagingPagerFragmentArgs by navArgs()
    var isMultiSourceOrder = false

    override fun setupBinding(binding: StagingFragmentBinding) {
        super.setupBinding(binding)
        binding.fragmentViewLifecycleOwner = viewLifecycleOwner
        binding.pagerVm = stagingPagerViewModel

        // Use order number to pull UI info from shared VM
        stagingPagerViewModel.getUiForOrder(args.orderNumber)?.let { (stagingUI, totes) ->
            fragmentViewModel.stagingUi.postValue(stagingUI)
            fragmentViewModel.toteUiList.postValue(totes.toMutableList())
            stagingPagerViewModel.currentOrderToteUiList.postValue(totes.toMutableList())
            isMultiSourceOrder = stagingUI.isOrderMultiSource == true
            // stagingPagerViewModel.showCollectToteBottomSheet(totes.size)
        }

        // use fragmentViewModel as receiver
        with(fragmentViewModel) {
            // relay advance event to pager via shared VM
            advanceEvent.observe(viewLifecycleOwner) {
                viewLifecycleOwner.lifecycleScope.launch {
                    stagingPagerViewModel.advanceEvent.emit(Unit)
                }
            }

            // observe count changes and relay info to stagingViewModel
            toteDbVms.observe(viewLifecycleOwner) { toteInfoList ->
                stagingPagerViewModel.toteCountInfo.value = Pair(args.orderNumber, toteInfoList)
            }

            // this (and lots of save noise) can be removed if we are good with saving via
            // fragment onStop() - UI crashes (unexpected) and AS aborts would not save
            requestSave.observe(viewLifecycleOwner) {
                stagingPagerViewModel.updateAndSaveStagingOne()
            }

            // observe order completion changes and relay info to stagingViewModel
            isOrderCompleted.observe(viewLifecycleOwner) {
                viewLifecycleOwner.lifecycleScope.launch {
                    stagingPagerViewModel.orderCompletionStateChangeEvent.emit(Unit)
                }
            }

            orderCompletionState.observe(viewLifecycleOwner) {
                viewLifecycleOwner.lifecycleScope.launch {
                    stagingPagerViewModel.completeOrderFlow.emit(it)
                }
            }
        }
    }
}
