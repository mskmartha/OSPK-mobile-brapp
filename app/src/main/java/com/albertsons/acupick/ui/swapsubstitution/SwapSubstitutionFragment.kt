package com.albertsons.acupick.ui.swapsubstitution

import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.SwapSubstitutionFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class SwapSubstitutionFragment : BaseFragment<SwapSubstitutionViewModel, SwapSubstitutionFragmentBinding>() {
    override val fragmentViewModel: SwapSubstitutionViewModel by viewModel()

    override fun getLayoutRes() = R.layout.swap_substitution_fragment

    override fun setupBinding(binding: SwapSubstitutionFragmentBinding) {
        super.setupBinding(binding)
        binding.fragmentLifecycleOwner = viewLifecycleOwner
        binding.viewModel = fragmentViewModel
        fragmentViewModel.getSwapSubstitutionDataFromNetwork()
    }
}
