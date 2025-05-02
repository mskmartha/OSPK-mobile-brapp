package com.albertsons.acupick.ui.swapsubstitution.otherShoppersItems

import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.QuickTaskFragmentBinding
import com.albertsons.acupick.ui.swapsubstitution.QuickTaskBaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

/** This screen will show other picker's substituted and out of stock items */
class QuickTaskOtherShoppersItemsFragment : QuickTaskBaseFragment<QuickTaskOtherShoppersitemsViewModel, QuickTaskFragmentBinding>() {
    override val fragmentViewModel: QuickTaskOtherShoppersitemsViewModel by viewModel()

    override fun getLayoutRes(): Int = R.layout.quick_task_fragment
}
