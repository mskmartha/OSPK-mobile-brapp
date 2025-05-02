package com.albertsons.acupick.ui.arrivals.destage.removeitems

import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.Remove1plHandoffFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class Handoff1PLFragment() : BaseFragment<HandOff1PLViewModel, Remove1plHandoffFragmentBinding>() {

    override val fragmentViewModel: HandOff1PLViewModel by viewModel()
    private val args: Handoff1PLFragmentArgs by navArgs()
    override fun getLayoutRes() = R.layout.remove_1pl_handoff_fragment

    override fun setupBinding(binding: Remove1plHandoffFragmentBinding) {
        super.setupBinding(binding)
        binding.orderCount = args.totalOrderCount
        binding.removedCount = args.removedItemsCount
        binding.misplacedCount = args.misplacedItemsCount
        fragmentViewModel.handleHandoffCompletion(args.handOffInterstitialParamsList)
    }
}
