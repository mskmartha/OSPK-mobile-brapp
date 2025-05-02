package com.albertsons.acupick.ui.arrivals.destage.removeitems

import android.os.Bundle
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.RemoveRejected1plItemFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class RemoveRejected1PLItemsFragment : BaseFragment<RemoveRejected1PLItemsViewModel, RemoveRejected1plItemFragmentBinding>() {

    private val args: RemoveRejected1PLItemsFragmentArgs by navArgs()
    override val fragmentViewModel: RemoveRejected1PLItemsViewModel by viewModel {
        parametersOf(args.rejectedItemsByZone/*, args.vanId*/)
    }
    val sharedViewModel: RemoveRejected1PLViewModel by navGraphViewModels(R.id.rejected1PLItemScope)

    override fun getLayoutRes() = R.layout.remove_rejected_1pl_item_fragment

    override fun setupBinding(binding: RemoveRejected1plItemFragmentBinding) {
        super.setupBinding(binding)
        binding.fragmentViewLifecycleOwner = viewLifecycleOwner

        activityViewModel.setToolbarTitle(getString(R.string.remove_item_header))
        fragmentViewModel.vanId = args.vanId
        fragmentViewModel.getRejectedItems()

        fragmentViewModel.requestList.observe(viewLifecycleOwner) { list ->
            if (list.isNotEmpty()) {

                setFragmentResult(
                    REMOVED_1PL_ITEMS,
                    bundleOf(REMOVED_1PL_ITEMS_RESULT to list)
                )

                lifecycleScope.launchWhenResumed {
                    fragmentViewModel.navigateUp()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
        }
    }

    companion object {
        const val REMOVED_1PL_ITEMS = "removedItems"
        const val REMOVED_1PL_ITEMS_RESULT = "removedItemsResult"
    }
}
