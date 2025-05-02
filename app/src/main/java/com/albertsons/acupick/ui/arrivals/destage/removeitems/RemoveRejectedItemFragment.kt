package com.albertsons.acupick.ui.arrivals.destage.removeitems

import android.os.Bundle
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.databinding.RemoveRejectedItemFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.notification.NotificationViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.core.parameter.parametersOf

class RemoveRejectedItemFragment : BaseFragment<RemoveRejectedItemViewModel, RemoveRejectedItemFragmentBinding>() {

    private val notificationViewModel: NotificationViewModel by sharedViewModel()
    override fun getLayoutRes() = R.layout.remove_rejected_item_fragment
    private val args: RemoveRejectedItemFragmentArgs by navArgs()
    override val fragmentViewModel: RemoveRejectedItemViewModel by viewModel {
        parametersOf(args.ui, StorageType.valueOf(args.storageType))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            handleBackOrUpButton()
        }
    }

    override fun setupBinding(binding: RemoveRejectedItemFragmentBinding) {
        super.setupBinding(binding)
        activityViewModel.setToolbarTitle(getString(R.string.remove_item_header))
        notificationViewModel.notificationMessageSnackEvent.observe(viewLifecycleOwner) { snackBarEvent ->
            snackBarEvent.let { fragmentViewModel.showSnackBar(it) }
        }

        fragmentViewModel.requestList.observe(viewLifecycleOwner) { list ->
            if (list.isNotEmpty()) {
                setFragmentResult(
                    REMOVED_ITEMS,
                    bundleOf(REMOVED_ITEMS_RESULT to list)
                )

                lifecycleScope.launchWhenResumed {
                    fragmentViewModel.navigateUp()
                }
            }
        }
        fragmentViewModel.canceledRejectedItem.observe(viewLifecycleOwner) {
            setFragmentResult(
                CANCELED_REMOVE_ITEMS,
                bundleOf(CANCELED_REMOVE_ITEMS_RESULT to it)
            )
        }
    }

    private fun handleBackOrUpButton() {
        fragmentViewModel.showRemoveItemsBackoutDialog()
    }

    companion object {
        const val REMOVED_ITEMS = "removedItems"
        const val REMOVED_ITEMS_RESULT = "removedItemsResult"
        const val CANCELED_REMOVE_ITEMS = "canceledRemovedItems"
        const val CANCELED_REMOVE_ITEMS_RESULT = "canceledRemovedItemsResult"
    }
}
