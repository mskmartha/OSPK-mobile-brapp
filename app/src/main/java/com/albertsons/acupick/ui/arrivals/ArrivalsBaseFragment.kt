package com.albertsons.acupick.ui.arrivals

import androidx.lifecycle.lifecycleScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.databinding.ArrivalsFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.arrivals.destage.ArrivalsOptionsBottomSheetDialogFragment
import com.albertsons.acupick.ui.arrivals.destage.ArrivalsOptionsBottomSheetDialogFragment.Companion.MARK_AS_NOT_HERE_SHEET_TAG
import com.albertsons.acupick.ui.chat.ChatIconWithTooltip
import com.albertsons.acupick.ui.dialog.CloseActionListener
import com.albertsons.acupick.ui.dialog.showWithFragment
import com.albertsons.acupick.ui.notification.NotificationViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

abstract class ArrivalsBaseFragment : BaseFragment<ArrivalsViewModel, ArrivalsFragmentBinding>() {
    // DI
    val networkAvailabilityManager: NetworkAvailabilityManager by inject()

    override val fragmentViewModel: ArrivalsViewModel by viewModel { parametersOf(getSharedViewModel<MainActivityViewModel>()) }
    val pagerVm: ArrivalsPagerViewModel by viewModel { parametersOf(getSharedViewModel<MainActivityViewModel>()) }
    private val notificationViewModel: NotificationViewModel by sharedViewModel()

    override fun getLayoutRes() = R.layout.arrivals_fragment

    // /////////////////////////////////////////////////////////////////////////
    // Common lifecycle and setup functions
    // /////////////////////////////////////////////////////////////////////////
    override fun setupBinding(binding: ArrivalsFragmentBinding) {
        super.setupBinding(binding)

        binding.chatButtonView.setContent {
            ChatIconWithTooltip(onChatClicked = { orderNumber ->
                fragmentViewModel.onChatClicked(orderNumber)
            })
        }

        fragmentViewModel.loadDataEvent.observe(viewLifecycleOwner) { isRefresh ->
            pagerVm.loadResults(isRefresh)
            notificationViewModel.checkForArrivedOrders()
        }

        fragmentViewModel.onEllipsisClickEvent.observe(viewLifecycleOwner) {
            ArrivalsOptionsBottomSheetDialogFragment.newInsance {
                fragmentViewModel.onClickMarkAsNotHere(it)
            }.showWithFragment(this, MARK_AS_NOT_HERE_SHEET_TAG)
        }

        pagerVm.isDataRefreshing.observe(viewLifecycleOwner) {
            fragmentViewModel.isDataRefreshing.value = it
        }

        pagerVm.isDataLoading.observe(viewLifecycleOwner) {
            fragmentViewModel.isDataLoading.value = it
        }
    }

    override fun provide(tag: String?): CloseActionListener? {
        return super.provide(tag) ?: run {
            Timber.e("[provide] unhandled dialog listener")
            null
        }
    }

    override fun onResume() {
        if (!activityViewModel.getIsAppResumesFromNotification()) {
            setupRefresh()
            fragmentViewModel.clearAllPreviousSelections()
        }
        activityViewModel.setAppResumesFromNotification(false)
        super.onResume()
    }

    private fun setupRefresh() {
        view?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                networkAvailabilityManager.isConnected.collect { isConnected ->
                    if (isConnected) {
                        pagerVm.loadResults()
                    } else {
                        networkAvailabilityManager.triggerOfflineError { setupRefresh() }
                    }
                }
            }
        }
    }
}
