package com.albertsons.acupick.ui.arrivals

import com.albertsons.acupick.databinding.ArrivalsFragmentBinding

class ArrivalsInProgressFragment : ArrivalsBaseFragment() {

    override fun setupBinding(binding: ArrivalsFragmentBinding) {
        super.setupBinding(binding)
        binding.isInProgress = true

        pagerVm.tabs.observe(viewLifecycleOwner) {
            fragmentViewModel.results.value = it[1].tabArgument
        }
    }
}
