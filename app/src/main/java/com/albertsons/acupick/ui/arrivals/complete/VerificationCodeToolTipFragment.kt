package com.albertsons.acupick.ui.arrivals.complete

import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.VerificationCodeToolTipFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class VerificationCodeToolTipFragment : BaseFragment<VerificationCodeToolTipViewModel, VerificationCodeToolTipFragmentBinding>() {
    override val fragmentViewModel: VerificationCodeToolTipViewModel by viewModel()

    override fun getLayoutRes(): Int = R.layout.verification_code_tool_tip_fragment

    override fun setupBinding(binding: VerificationCodeToolTipFragmentBinding) {
        super.setupBinding(binding)
    }
}
