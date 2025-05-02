package com.albertsons.acupick.ui.staging.winestaging

import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.WineStagingFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class WineStagingFragment : BaseFragment<WineStagingViewModel, WineStagingFragmentBinding>() {
    private val args: WineStagingFragmentArgs by navArgs()
    override val fragmentViewModel: WineStagingViewModel by viewModel() {
        parametersOf(args.wineStagingParams)
    }
    override fun getLayoutRes() = R.layout.wine_staging_fragment

    override fun setupBinding(binding: WineStagingFragmentBinding) {
        super.setupBinding(binding)
        binding.apply {
            viewModel = fragmentViewModel
            lifecycleOwner = viewLifecycleOwner
        }
        fragmentViewModel.setupHeader(args.wineStagingParams)
    }
}
