package com.albertsons.acupick.ui.swapsubstitution

import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.ui.BaseFragment
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

abstract class QuickTaskBaseFragment<VM : QuickTaskBaseViewModel, BINDING : ViewDataBinding> : BaseFragment<VM, BINDING>() {
    // DI
    val networkAvailabilityManager: NetworkAvailabilityManager by inject()

    // Constrain fragmentViewModel type to QuickTaskBaseViewModel using generic typing,
    //   then we have direct access to refreshData() and other common VM functions
    abstract override val fragmentViewModel: VM

    // /////////////////////////////////////////////////////////////////////////
    // Common lifecycle and setup functions
    // /////////////////////////////////////////////////////////////////////////
    override fun onResume() {
        super.onResume()
        if (!activityViewModel.getIsAppResumesFromNotification()) {
            setupRefresh()
        }
        activityViewModel.setAppResumesFromNotification(false)
    }

    private fun setupRefresh() {
        view?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                networkAvailabilityManager.isConnected.collect { isConnected ->
                    if (isConnected) {
                        fragmentViewModel.loadData()
                    } else {
                        networkAvailabilityManager.triggerOfflineError { setupRefresh() }
                    }
                }
            }
        }
    }

    override fun setupBinding(binding: BINDING) {
        super.setupBinding(binding)
        fragmentViewModel.isSpinnerShowing.observe(viewLifecycleOwner) {
            activityViewModel.setLoadingState(isLoading = it, blockUi = it)
        }
    }
}
