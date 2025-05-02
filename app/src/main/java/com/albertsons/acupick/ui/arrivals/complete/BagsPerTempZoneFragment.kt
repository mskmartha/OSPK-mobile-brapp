package com.albertsons.acupick.ui.arrivals.complete

import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.BagsPerTempZoneFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class BagsPerTempZoneFragment : BaseFragment<BagsPerTempZoneViewModel, BagsPerTempZoneFragmentBinding>() {
    override val fragmentViewModel: BagsPerTempZoneViewModel by viewModel()
    private val args by navArgs<BagsPerTempZoneFragmentArgs>()
    override fun getLayoutRes(): Int = R.layout.bags_per_temp_zone_fragment

    override fun setupBinding(binding: BagsPerTempZoneFragmentBinding) {
        super.setupBinding(binding)
        fragmentViewModel.loadData(args.bagsPerTempZoneParams)
    }
}
