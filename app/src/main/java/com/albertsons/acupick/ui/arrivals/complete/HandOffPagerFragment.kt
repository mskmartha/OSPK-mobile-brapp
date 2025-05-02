package com.albertsons.acupick.ui.arrivals.complete

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.addCallback
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.widget.ViewPager2
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.HandOffPagerFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.staging.OFFSCREEN_PAGE_LIMIT
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class HandOffPagerFragment : BaseFragment<HandOffPagerViewModel, HandOffPagerFragmentBinding>() {
    private val handOffArgs by navArgs<HandOffPagerFragmentArgs>()

    override fun getLayoutRes(): Int = R.layout.hand_off_pager_fragment

    override val fragmentViewModel: HandOffPagerViewModel by navGraphViewModels(R.id.handOffScope)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            fragmentViewModel.handleBackButton()
        }
    }

    override fun setupBinding(binding: HandOffPagerFragmentBinding) {
        super.setupBinding(binding)

        with(fragmentViewModel) {
            setHandOffOrderUiList(handOffArgs.handOffArgData.handOffUIList)
            isFromNotification.postValue(handOffArgs.isFromNotification)
            setPartialPrescriptionInfo(handOffArgs.isFromPartialPrescriptionReturn)
            setPickedBagNumbers(handOffArgs.pickedBagNumbers)

            // TODO - Move to binding adapter
            // Keep all tabs in memory
            @SuppressLint("WrongConstant")
            binding.viewPager2.offscreenPageLimit = OFFSCREEN_PAGE_LIMIT

            // Notify view model when switching tabs
            viewLifecycleOwner.lifecycleScope.launch {
                getPageEventFlow(binding.viewPager2)
                    .collect { position ->
                        pageEvent.value = position
                    }
            }

            // Observe ViewModel page event and set view pager to appropriate position
            viewLifecycleOwner.lifecycleScope.launch {
                pageEvent.collect {
                    if (binding.viewPager2.currentItem != it) binding.viewPager2.currentItem = it
                }
            }

            // Tabs are controlled by view model and relayed here to adapter
            tabsLiveData.observe(viewLifecycleOwner) { tabs ->
                binding.viewPager2.adapter = HandOffPagerAdapter(this@HandOffPagerFragment, tabs)
                (binding.viewPager2.adapter as HandOffPagerAdapter).menuTabMediatorFactory(binding.tabLayout, binding.viewPager2).attach()

                binding.tabLayout.tabMode = when (tabs.size) {
                    2 -> TabLayout.MODE_FIXED
                    else -> TabLayout.MODE_AUTO
                }
            }

            isCompleteList.observe(viewLifecycleOwner) {
                it.forEach { orderCompletionState ->
                    (binding.viewPager2.adapter as HandOffPagerAdapter?)?.let { adapter ->
                        val index = adapter.getIndexOfOrder(orderCompletionState.customerOrderNumber)
                        val tab = binding.tabLayout.getTabAt(index)
                        tab?.setIcon(
                            if (orderCompletionState.isComplete) {
                                val iconResId = if (tab.isSelected) R.drawable.ic_check_dark_blue else R.drawable.ic_check_grey
                                ResourcesCompat.getDrawable(resources, iconResId, null)
                            } else {
                                null
                            }
                        )
                    }
                }
            }

            isBlockingUi.observe(viewLifecycleOwner) {
                activityViewModel.setLoadingState(it, true)
            }
        }

        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                fragmentViewModel.setActiveOderNumber(handOffArgs.handOffArgData.handOffUIList[position].orderNumber)
            }
        })
    }

    override fun onDestroyView(binding: HandOffPagerFragmentBinding) {
        binding.apply { viewPager2.adapter = null }
        super.onDestroyView(binding)
    }
}
