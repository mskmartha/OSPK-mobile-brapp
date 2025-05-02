package com.albertsons.acupick.ui.arrivals.destage.updatecustomers.add

import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.databinding.UpdateCustomerAddCustomerFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.arrivals.destage.updatecustomers.add.AddCustomerViewModel.Companion.ADD_CUSTOMER_RETURN
import com.albertsons.acupick.ui.arrivals.destage.updatecustomers.add.AddCustomerViewModel.Companion.ADD_CUSTOMER_RETURN_RESULT
import com.albertsons.acupick.ui.notification.NotificationViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class AddCustomerFragment : BaseFragment<AddCustomerViewModel, UpdateCustomerAddCustomerFragmentBinding>() {
    // DI
    private val notificationViewModel: NotificationViewModel by sharedViewModel()

    val networkAvailabilityManager: NetworkAvailabilityManager by inject()

    val args: AddCustomerFragmentArgs by navArgs()

    override val fragmentViewModel: AddCustomerViewModel by viewModel()

    override fun getLayoutRes() = R.layout.update_customer_add_customer_fragment

    override fun setupBinding(binding: UpdateCustomerAddCustomerFragmentBinding) {
        super.setupBinding(binding)
        binding.customLifecycleOwner = viewLifecycleOwner

        setupRefresh()

        fragmentViewModel.alreadyAssignedCount.value = args.assignedCount

        fragmentViewModel.returnOrderToAddDataEvent.observe(viewLifecycleOwner) {
            Timber.d("1292 ADD_CUSTOMER_RETURN setFragmentResult")
            setFragmentResult(ADD_CUSTOMER_RETURN, bundleOf(ADD_CUSTOMER_RETURN_RESULT to it))
        }
        notificationViewModel.notificationMessageSnackEvent.observe(viewLifecycleOwner) { snackBarEvent ->
            snackBarEvent?.let { it -> fragmentViewModel.showSnackBar(it) }
        }
    }

    private fun setupRefresh() {
        viewLifecycleOwner.lifecycleScope.launch {
            networkAvailabilityManager.isConnected.collect { isConnected ->
                if (isConnected) {
                    fragmentViewModel.loadDetails(isAddCustomer = true)
                } else {
                    networkAvailabilityManager.triggerOfflineError { setupRefresh() }
                }
            }
        }
    }
}
