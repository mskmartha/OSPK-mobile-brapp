package com.albertsons.acupick.ui.storelist

import androidx.navigation.fragment.findNavController
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.StoresFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.notification.NotificationViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.stateViewModel

class StoreSelectionFragment : BaseFragment<StoresViewModel, StoresFragmentBinding>() {
    override val fragmentViewModel: StoresViewModel by stateViewModel()

    private val notificationViewModel: NotificationViewModel by sharedViewModel()

    override fun getLayoutRes() = R.layout.stores_fragment

    override fun setupBinding(binding: StoresFragmentBinding) {
        super.setupBinding(binding)
        activityViewModel.setToolbarTitle(getString(R.string.select_store_title))
        fragmentViewModel.storeSelectionCompleteAction.observe(viewLifecycleOwner) {
            if (notificationViewModel.wasNotificationClicked) {
                notificationViewModel.handleNotificationAfterLogin()
            } else {
                findNavController().navigate(R.id.action_storeSelectionFragment_to_homeFragment)
            }
        }
    }
}
