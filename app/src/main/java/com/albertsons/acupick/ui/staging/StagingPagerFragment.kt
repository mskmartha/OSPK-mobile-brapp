package com.albertsons.acupick.ui.staging

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.addCallback
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.widget.ViewPager2
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.StagingPagerFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.util.hideKeyboard
import com.albertsons.acupick.ui.util.isKeyboardVisible
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

const val OFFSCREEN_PAGE_LIMIT = 5 // This keeps all fragments in the view pager in memory, and fixes some glitches.

class StagingPagerFragment : BaseFragment<StagingPagerViewModel, StagingPagerFragmentBinding>() {
    override val fragmentViewModel: StagingPagerViewModel by navGraphViewModels(R.id.stagingScope)
    override fun getLayoutRes(): Int = R.layout.staging_pager_fragment

    private val stagingArgs by navArgs<StagingPagerFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            fragmentViewModel.navigateHome()
        }
    }

    override fun setupBinding(binding: StagingPagerFragmentBinding) {
        super.setupBinding(binding)

        fragmentViewModel.shouldClearStagingData = stagingArgs.shouldClearData

        // only show confirm CTA when keyboard is not showing
        activityViewModel.keyboardActive.observe(viewLifecycleOwner) { keyboardActive ->
            fragmentViewModel.isKeyboardVisible.value = keyboardActive
        }

        // Work with binding.viewPager2 as receiver
        with(binding.viewPager2) {
            @SuppressLint("WrongConstant")
            offscreenPageLimit = OFFSCREEN_PAGE_LIMIT

            // when switching tabs, notify the destination tab/fragment to give focus to its first EditText
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    fragmentViewModel.onPagerPageSeleceted(position, isKeyboardVisible())
                }
            })
        }

        // Work with viewModel as receiver
        with(fragmentViewModel) {
            // Pass in input data
            activityId.value = stagingArgs.activityId
            toteLabelsPrintedSuccessfully.value = stagingArgs.isPreviousPrintSuccessful

            // Observe Events
            tabs.observe(viewLifecycleOwner) { tabs ->
                binding.viewPager2.adapter = StagingPagerAdapter(this@StagingPagerFragment, tabs)
                (binding.viewPager2.adapter as StagingPagerAdapter).menuTabMediatorFactory(binding.tabLayout, binding.viewPager2).attach()
            }

            // On signal, move view pager back one position.
            viewLifecycleOwner.lifecycleScope.launch {
                backEvent.collect {
                    with(binding) {
                        if (viewPager2.currentItem > 0) {
                            viewPager2.currentItem = viewPager2.currentItem - 1
                        } else {
                            // unless we were already on the first page, then hide keyboard
                            hideKeyboard()
                        }
                    }
                    lastNavEvent = NavDirection.BACK
                }
            }

            // On signal, advance view pager by one position.
            viewLifecycleOwner.lifecycleScope.launch {
                advanceEvent.collect {
                    with(binding) {
                        if ((viewPager2.currentItem + 1) < viewPager2.adapter?.itemCount ?: 0) {
                            viewPager2.currentItem = viewPager2.currentItem + 1
                        } else {
                            // unless we were already on the last page, then hide keyboard
                            hideKeyboard()
                        }
                    }
                    lastNavEvent = NavDirection.NEXT
                }
            }

            isCompleteList.observe(viewLifecycleOwner) {
                it?.forEach { orderCompletionState ->
                    (binding.viewPager2.adapter as StagingPagerAdapter).let { stagingAdapter ->
                        orderCompletionState.customerOrderNumber.let { orderNumber ->
                            val index = stagingAdapter.getIndexOfOrder(orderNumber)
                            val tab = binding.tabLayout.getTabAt(index)
                            val isCustomerNotPreferBag = !(fragmentViewModel.getUiForOrder(orderNumber)?.first?.isCustomerBagPreference ?: true)
                            tab?.icon = if (orderCompletionState.isComplete || isCustomerNotPreferBag) {
                                ResourcesCompat.getDrawable(resources, R.drawable.ic_checkmark, null)
                            } else {
                                null
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView(binding: StagingPagerFragmentBinding) {
        binding.apply { viewPager2.adapter = null }
        super.onDestroyView(binding)
    }
}
