package com.albertsons.acupick.ui.arrivals.complete

import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.OrderSummaryFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class OrderSummaryFragment : BaseFragment<OrderSummaryViewModel, OrderSummaryFragmentBinding>() {

    private val args: OrderSummaryFragmentArgs by navArgs()

    override val fragmentViewModel: OrderSummaryViewModel by viewModel()

    override fun getLayoutRes() = R.layout.order_summary_fragment

    override fun setupBinding(binding: OrderSummaryFragmentBinding) {
        super.setupBinding(binding)
        binding.fragmentLifecycleOwner = viewLifecycleOwner
        binding.viewModel = fragmentViewModel
        fragmentViewModel.setupData(args.orderSummaryArg.orderSummary, args.orderSummaryArg.isCas, args.orderSummaryArg.is3p, args.orderSummaryArg.source)
    }
}
