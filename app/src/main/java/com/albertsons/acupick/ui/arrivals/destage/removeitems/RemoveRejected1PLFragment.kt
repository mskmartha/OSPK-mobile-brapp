package com.albertsons.acupick.ui.arrivals.destage.removeitems

import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.RemoveRejected1plFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RemoveRejected1PLFragment : BaseFragment<RemoveRejected1PLViewModel, RemoveRejected1plFragmentBinding>() {

    override fun getLayoutRes() = R.layout.remove_rejected_1pl_fragment
    override val fragmentViewModel: RemoveRejected1PLViewModel by navGraphViewModels(R.id.rejected1PLItemScope)
    private val args: RemoveRejected1PLFragmentArgs by navArgs()

    override fun setupBinding(binding: RemoveRejected1plFragmentBinding) {
        super.setupBinding(binding)
        binding.fragmentViewLifecycleOwner = viewLifecycleOwner

        activityViewModel.setToolbarTitle(getString(R.string.remove_item_header))
        fragmentViewModel.rejectedItems.postValue(args.rejectedItems)

        fragmentViewModel.removedItemsByStorageType.observe(viewLifecycleOwner) {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(100)
                fragmentViewModel.isRemoveButtonEnabled()
            }
        }

        setFragmentResultListener(RemoveRejected1PLItemsFragment.REMOVED_1PL_ITEMS) { _, bundle ->
            val requests = bundle.get(RemoveRejected1PLItemsFragment.REMOVED_1PL_ITEMS_RESULT)
            (requests as? List<RejectedItemHeaderViewModel>)?.let {
                with(fragmentViewModel) {
                    val data = this.removedItemsByStorageType.value
                    data?.put(it.first().storageType, it)
                    this.removedItemsByStorageType.postValue(data)
                }
            }
        }
    }
}
