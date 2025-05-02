package com.albertsons.acupick.ui.arrivals

import com.albertsons.acupick.databinding.ArrivalsFragmentBinding

class ArrivalsAllOrdersFragment : ArrivalsBaseFragment() {

    override fun setupBinding(binding: ArrivalsFragmentBinding) {
        super.setupBinding(binding)
        binding.isInProgress = false

        pagerVm.tabs.observe(viewLifecycleOwner) {
            fragmentViewModel.results.value = it[0].tabArgument
        }
    }
}
