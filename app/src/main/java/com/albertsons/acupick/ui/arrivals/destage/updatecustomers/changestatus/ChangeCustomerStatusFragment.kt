package com.albertsons.acupick.ui.arrivals.destage.updatecustomers.changestatus

import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.databinding.UpdateCustomerChangeStatusFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.arrivals.destage.updatecustomers.changestatus.ChangeCustomerStatusViewModel.Companion.UPDATE_CUSTOMER_STATUS_RETURN
import com.albertsons.acupick.ui.arrivals.destage.updatecustomers.changestatus.ChangeCustomerStatusViewModel.Companion.UPDATE_CUSTOMER_STATUS_RETURN_RESULT
import com.albertsons.acupick.ui.notification.NotificationViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ChangeCustomerStatusFragment : BaseFragment<ChangeCustomerStatusViewModel, UpdateCustomerChangeStatusFragmentBinding>() {
    // DI
    private val notificationViewModel: NotificationViewModel by sharedViewModel()

    val networkAvailabilityManager: NetworkAvailabilityManager by inject()

    override val fragmentViewModel: ChangeCustomerStatusViewModel by viewModel()

    override fun getLayoutRes() = R.layout.update_customer_change_status_fragment

    override fun setupBinding(binding: UpdateCustomerChangeStatusFragmentBinding) {
        super.setupBinding(binding)
        binding.customLifecycleOwner = viewLifecycleOwner

        setupRefresh()

        fragmentViewModel.returnOrderToUpdateDataEvent.observe(viewLifecycleOwner) {
            Timber.d("1292 change customer status fragment orderToUpdate listener setFragmentResult")
            setFragmentResult(UPDATE_CUSTOMER_STATUS_RETURN, bundleOf(UPDATE_CUSTOMER_STATUS_RETURN_RESULT to it))
        }
        notificationViewModel.notificationMessageSnackEvent.observe(viewLifecycleOwner) { snackBarEvent ->
            snackBarEvent?.let { it -> fragmentViewModel.showSnackBar(it) }
        }
    }

    private fun setupRefresh() {
        viewLifecycleOwner.lifecycleScope.launch {
            networkAvailabilityManager.isConnected.collect { isConnected ->
                if (isConnected) {
                    fragmentViewModel.loadDetails()
                } else {
                    networkAvailabilityManager.triggerOfflineError { setupRefresh() }
                }
            }
        }
    }
}
