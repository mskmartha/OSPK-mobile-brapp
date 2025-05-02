package com.albertsons.acupick.ui.splash

import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.SplashFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class SplashFragment : BaseFragment<SplashViewModel, SplashFragmentBinding>() {
    override val fragmentViewModel: SplashViewModel by viewModel()
    override fun getLayoutRes(): Int = R.layout.splash_fragment

    override fun setupBinding(binding: SplashFragmentBinding) {
        super.setupBinding(binding)
        // Add binding/viewmodel logic here
    }
}
