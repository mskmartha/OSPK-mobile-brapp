package com.albertsons.acupick.ui.profile

import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ProfileFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class ProfileFragment : BaseFragment<ProfileViewModel, ProfileFragmentBinding>() {
    override val fragmentViewModel: ProfileViewModel by viewModel()
    override fun getLayoutRes(): Int = R.layout.profile_fragment

    override fun setupBinding(binding: ProfileFragmentBinding) {
        super.setupBinding(binding)
        activityViewModel.setToolbarTitle(getString(R.string.toolbar_title_profile))
        // Add binding/viewmodel logic here
    }
}
