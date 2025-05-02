package com.albertsons.acupick.ui.auth

import androidx.navigation.fragment.findNavController
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.LoginFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.notification.NotificationViewModel
import com.albertsons.acupick.ui.util.hideKeyboard
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class LoginFragment : BaseFragment<LoginViewModel, LoginFragmentBinding>() {
    override val fragmentViewModel: LoginViewModel by viewModel {
        parametersOf(getSharedViewModel<MainActivityViewModel>())
    }

    private val notificationViewModel: NotificationViewModel by sharedViewModel()

    override fun getLayoutRes(): Int = R.layout.login_fragment

    override fun setupBinding(binding: LoginFragmentBinding) {
        super.setupBinding(binding)
        fragmentViewModel.isBlockingUi.observe(viewLifecycleOwner) { isBlockingUi ->
            activityViewModel.setLoadingState(isLoading = isBlockingUi, blockUi = true)
            if (isBlockingUi) {
                binding.idTextInputLayout.clearEditTextFocus()
                binding.passwordTextInputLayout.clearEditTextFocus()
            }
        }

        fragmentViewModel.hideKeyboard.observe(viewLifecycleOwner) {
            hideKeyboard()
        }

        fragmentViewModel.enterStoreAction.observe(viewLifecycleOwner) {
            if (notificationViewModel.wasNotificationClicked) {
                notificationViewModel.handleNotificationAfterLogin()
            } else {
                if (findNavController().currentDestination?.id == R.id.loginFragment) {
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
            }
        }
    }
}
