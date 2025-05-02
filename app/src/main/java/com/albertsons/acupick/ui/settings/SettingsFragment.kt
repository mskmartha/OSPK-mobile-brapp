package com.albertsons.acupick.ui.settings

import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.SettingsFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment : BaseFragment<SettingsViewModel, SettingsFragmentBinding>() {
    override val fragmentViewModel: SettingsViewModel by viewModel()
    override fun getLayoutRes(): Int = R.layout.settings_fragment

    override fun setupBinding(binding: SettingsFragmentBinding) {
        super.setupBinding(binding)
        // Add binding/viewmodel logic here
    }
}
