package com.albertsons.acupick.ui.picklistitems

import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.NetWeightToolTipFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.ToolTipViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class NetWeightToolTipFragment : BaseFragment<ToolTipViewModel, NetWeightToolTipFragmentBinding>() {

    override val fragmentViewModel: ToolTipViewModel by viewModel()

    override fun getLayoutRes() = R.layout.net_weight_tool_tip_fragment

    override fun setupBinding(binding: NetWeightToolTipFragmentBinding) {
        super.setupBinding(binding)
        activityViewModel.setToolbarTitle(getString(R.string.net_weight_tool_tip_toolbar_title))
    }
}
