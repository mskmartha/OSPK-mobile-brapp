package com.albertsons.acupick.ui.home

import androidx.lifecycle.lifecycleScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.databinding.HomeFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.chat.ChatIconWithTooltip
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

/** Home screen for the app ([Zeplin](https://zpl.io/25WpJAW)) */
class HomeFragment : BaseFragment<HomeViewModel, HomeFragmentBinding>() {
    // DI
    val networkAvailabilityManager: NetworkAvailabilityManager by inject()

    override val fragmentViewModel: HomeViewModel by viewModel {
        parametersOf(getSharedViewModel<MainActivityViewModel>())
    }

    override fun getLayoutRes(): Int = R.layout.home_fragment

    override fun setupBinding(binding: HomeFragmentBinding) {
        super.setupBinding(binding)
        setupRefresh()
        callBackListener()
        binding.chatButtonView.setContent {
            ChatIconWithTooltip(onChatClicked = { orderNumber ->
                fragmentViewModel.onChatClicked(orderNumber)
            })
        }

        fragmentViewModel.storeTitle.observe(viewLifecycleOwner) { storeNumber ->
            activityViewModel.setToolbarTitle(storeNumber)
        }

        fragmentViewModel.cardData.observe(viewLifecycleOwner) {
            fragmentViewModel.runCardDataActions()
        }


    }

    private fun callBackListener() {
        parentFragmentManager.setFragmentResultListener("dialog_dismissed", viewLifecycleOwner) { _, bundle ->
            Timber.e("dialog_dismissed")

            // Handle dismiss here
        }
    }


    private fun setupRefresh() {
        viewLifecycleOwner.lifecycleScope.launch {
            networkAvailabilityManager.isConnected.collect { isConnected ->
                if (isConnected) {
                    fragmentViewModel.load()
                } else {
                    networkAvailabilityManager.triggerOfflineError { this@HomeFragment.view?.let { setupRefresh() } }
                }
            }
        }
    }
}
