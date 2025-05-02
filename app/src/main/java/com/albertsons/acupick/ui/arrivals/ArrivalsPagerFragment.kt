package com.albertsons.acupick.ui.arrivals

import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ArrivalsPagerFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.MainActivityViewModel
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ArrivalsPagerFragment : BaseFragment<ArrivalsPagerViewModel, ArrivalsPagerFragmentBinding>() {

    override val fragmentViewModel: ArrivalsPagerViewModel by viewModel {
        parametersOf(getSharedViewModel<MainActivityViewModel>())
    }
    override fun getLayoutRes(): Int = R.layout.arrivals_pager_fragment

    override fun setupBinding(binding: ArrivalsPagerFragmentBinding) {
        super.setupBinding(binding)

        with(binding) {
            fragmentViewModel.tabs.observe(viewLifecycleOwner) { tabData ->
                arrivalsViewPager.adapter = ArrivalsPagerAdapter(this@ArrivalsPagerFragment, tabData)
                (arrivalsViewPager.adapter as ArrivalsPagerAdapter).menuTabMediatorFactory(tabLayout, arrivalsViewPager).attach()
            }
        }
        activityViewModel.setToolbarTitle(getString(R.string.arrivals))
    }

    override fun onDestroyView(binding: ArrivalsPagerFragmentBinding) {
        binding.apply { arrivalsViewPager.adapter = null }
        super.onDestroyView(binding)
    }
}
