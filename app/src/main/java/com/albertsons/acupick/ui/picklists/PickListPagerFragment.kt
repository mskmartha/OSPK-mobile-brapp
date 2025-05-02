package com.albertsons.acupick.ui.picklists

import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.PickListPagerFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.chat.ChatIconWithTooltip
import com.google.android.material.tabs.TabLayoutMediator
import org.koin.androidx.viewmodel.ext.android.viewModel

class PickListPagerFragment : BaseFragment<PickListPagerViewModel, PickListPagerFragmentBinding>() {
    override val fragmentViewModel: PickListPagerViewModel by viewModel()

    var getTabViewModelByPosition: Int = 0

    lateinit var binder: PickListPagerFragmentBinding
    override fun getLayoutRes(): Int = R.layout.pick_list_pager_fragment

    override fun setupBinding(binding: PickListPagerFragmentBinding) {
        super.setupBinding(binding)
        binding.viewModel = fragmentViewModel
        binding.chatButtonView.setContent {
            ChatIconWithTooltip(onChatClicked = { orderNumber ->
                fragmentViewModel.onChatClicked(orderNumber)
            })
        }
        setAdapter(binding)
        activityViewModel.setToolbarTitle(getString(R.string.pick_list_toolbar_title))
    }

    // Used to initially set the adapter and position and to refresh data
    private fun setAdapter(binding: PickListPagerFragmentBinding) {
        binding.apply {
            pickListsViewPager2.adapter = PickListsPagerAdapter(this@PickListPagerFragment)
            TabLayoutMediator(tabLayout, pickListsViewPager2) { tab, position ->
                tab.text = when (position) {
                    0 -> getString(R.string.tab_open)
                    1 -> getString(R.string.tab_in_progress)
                    else -> null
                }
            }.attach()
            pickListsViewPager2.currentItem = getTabViewModelByPosition
        }
    }

    override fun onDestroyView(binding: PickListPagerFragmentBinding) {
        binding.apply { pickListsViewPager2.adapter = null }
        super.onDestroyView(binding)
    }
}
