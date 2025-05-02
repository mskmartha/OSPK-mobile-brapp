package com.albertsons.acupick.ui.storelist

import android.os.Bundle
import androidx.activity.addCallback
import androidx.navigation.fragment.findNavController
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ChangeStoreFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.util.hideKeyboard
import org.koin.androidx.viewmodel.ext.android.stateViewModel

class ChangeStoreFragment : BaseFragment<StoresViewModel, ChangeStoreFragmentBinding>() {
    override val fragmentViewModel: StoresViewModel by stateViewModel()
    override fun getLayoutRes() = R.layout.change_store_fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            handleBackOrUpButton()
        }
        activityViewModel.setToolbarTitle(getString(R.string.select_store_title))
    }

    override fun setupBinding(binding: ChangeStoreFragmentBinding) {
        super.setupBinding(binding)
        fragmentViewModel.storeClickAction.observe(viewLifecycleOwner) { hideKeyboard(true) }

        with(binding) {
            activityViewModel.keyboardActive.observe(viewLifecycleOwner) { keyboardActive ->
                if (!keyboardActive) {
                    searchTextInputEditText.clearFocus()
                }
            }

            fragmentViewModel.storeSelectionCompleteAction.observe(viewLifecycleOwner) {
                findNavController().navigate(R.id.action_changeStoreFragment_to_homeFragment)
            }
        }
    }

    private fun handleBackOrUpButton() {
        findNavController().navigateUp()
    }
}
