package com.albertsons.acupick.ui.swapsubstitution.myitems

import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.QuickTaskFragmentBinding
import com.albertsons.acupick.ui.swapsubstitution.QuickTaskBaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

/** This screen will show logged in picker substituted and out of stock items */
class QuickTaskMyItemsFragment : QuickTaskBaseFragment<QuickTaskMyItemsViewModel, QuickTaskFragmentBinding>() {
    override val fragmentViewModel: QuickTaskMyItemsViewModel by viewModel()

    override fun getLayoutRes(): Int = R.layout.quick_task_fragment
}
