package com.albertsons.acupick.ui.devoptions

import android.os.Bundle
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.albertsons.acupick.R
import com.albertsons.acupick.data.buildconfig.BuildConfigProvider
import com.albertsons.acupick.databinding.DevOptionsFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Screen that manages dev options (only accessible from internal or debug builds (aka non release production builds)
 */
class DevOptionsFragment : BaseFragment<DevOptionsViewModel, DevOptionsFragmentBinding>() {
    override val fragmentViewModel: DevOptionsViewModel by viewModel()
    override fun getLayoutRes(): Int = R.layout.dev_options_fragment

    private val buildConfigProvider by inject<BuildConfigProvider>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Only debug/internal builds allowed to show this screen. Immediately close if somehow launched on prod release build.
        if (buildConfigProvider.isProductionReleaseBuild) {
            // NOTE: Special case usage of findNavController
            findNavController().popBackStack()
            return
        }
    }

    override fun setupBinding(binding: DevOptionsFragmentBinding) {
        super.setupBinding(binding)
        fragmentViewModel.messageToUser.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        fragmentViewModel.apsEnvironmentDropdownDismissed.observe(viewLifecycleOwner) {
            // Clear focus on the environment switcher to prevent the dropdown from showing unintentionally on orientation change (related to focus still being on the environment switcher)
            binding.apsEnvironmentSwitcherInputLayout.clearFocus()
        }
        fragmentViewModel.authEnvironmentDropdownDismissed.observe(viewLifecycleOwner) {
            // Clear focus on the environment switcher to prevent the dropdown from showing unintentionally on orientation change (related to focus still being on the environment switcher)
            binding.authEnvironmentSwitcherInputLayout.clearFocus()
        }
        fragmentViewModel.configEnvironmentDropdownDismissed.observe(viewLifecycleOwner) {
            // Clear focus on the environment switcher to prevent the dropdown from showing unintentionally on orientation change (related to focus still being on the environment switcher)
            binding.configEnvironmentSwitcherInputLayout.clearFocus()
        }
        fragmentViewModel.osccEnvironmentDropdownDismissed.observe(viewLifecycleOwner) {
            // Clear focus on the environment switcher to prevent the dropdown from showing unintentionally on orientation change (related to focus still being on the environment switcher)
            binding.osccEnvironmentSwitcherInputLayout.clearFocus()
        }
        fragmentViewModel.itemProcessorEnvironmentDropdownDismissed.observe(viewLifecycleOwner) {
            // Clear focus on the environment switcher to prevent the dropdown from showing unintentionally on orientation change (related to focus still being on the environment switcher)
            binding.itemProcessorEnvironmentSwitcherInputLayout.clearFocus()
        }
        fragmentViewModel.autoLogoutDropdownDismissed.observe(viewLifecycleOwner) {
            binding.autoLogoutTimeSwitcher.clearFocus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Could also check on back/up nav press to trigger restart
        fragmentViewModel.triggerRestartIfNecessary()
    }
}
