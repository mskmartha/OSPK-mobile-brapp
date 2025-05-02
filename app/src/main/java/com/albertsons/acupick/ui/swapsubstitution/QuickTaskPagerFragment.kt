package com.albertsons.acupick.ui.swapsubstitution

import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.QuickTaskPagerFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.util.orFalse
import com.google.android.material.tabs.TabLayoutMediator
import org.koin.androidx.viewmodel.ext.android.viewModel

class QuickTaskPagerFragment : BaseFragment<QuickTaskPagerViewModel, QuickTaskPagerFragmentBinding>() {

    val args: QuickTaskPagerFragmentArgs by navArgs()
    override val fragmentViewModel: QuickTaskPagerViewModel by viewModel()
    override fun getLayoutRes(): Int = R.layout.quick_task_pager_fragment

    override fun setupBinding(binding: QuickTaskPagerFragmentBinding) {
        super.setupBinding(binding)
        binding.viewModel = fragmentViewModel
        setAdapter(binding)
        activityViewModel.setToolbarTitle(args.customerName)
    }

    // Used to initially set the adapter and position and to refresh data
    private fun setAdapter(binding: QuickTaskPagerFragmentBinding) {
        binding.apply {
            quickTaskViewPager2.adapter = QuickTaskPagerAdapter(this@QuickTaskPagerFragment, fragmentViewModel.isMasterOrderView.value.orFalse())
            TabLayoutMediator(tabLayout, quickTaskViewPager2) { tab, position ->
                tab.text = when (position) {
                    0 -> getString(R.string.my_items)
                    1 -> getString(R.string.other_shoppers_item)
                    else -> null
                }
            }.attach()
            // To disable swipe gesture
            quickTaskViewPager2.isUserInputEnabled = fragmentViewModel.isMasterOrderView.value.orFalse()
        }
    }

    override fun onDestroyView(binding: QuickTaskPagerFragmentBinding) {
        binding.apply { quickTaskViewPager2.adapter = null }
        super.onDestroyView(binding)
    }
}
