package com.albertsons.acupick.ui.metrics

import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.MetricsFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class MetricsFragment : BaseFragment<MetricsViewModel, MetricsFragmentBinding>() {
    override val fragmentViewModel: MetricsViewModel by viewModel()
    override fun getLayoutRes(): Int = R.layout.metrics_fragment

    override fun setupBinding(binding: MetricsFragmentBinding) {
        super.setupBinding(binding)
        // Add binding/viewmodel logic here
    }
}
