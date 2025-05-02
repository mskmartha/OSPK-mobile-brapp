package com.albertsons.acupick.ui.swapsubstitution

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.albertsons.acupick.ui.swapsubstitution.myitems.QuickTaskMyItemsFragment
import com.albertsons.acupick.ui.swapsubstitution.otherShoppersItems.QuickTaskOtherShoppersItemsFragment

class QuickTaskPagerAdapter(fragment: Fragment, private val isMasterOrderView: Boolean) : FragmentStateAdapter(fragment) {

    override fun getItemCount() = when (isMasterOrderView) {
        true -> MAX_TAB_COUNT
        else -> MIN_TAB_COUNT
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> QuickTaskMyItemsFragment()
            1 -> QuickTaskOtherShoppersItemsFragment()
            else -> throw Exception("QuickTaskPagerAdapter: createFragment else error")
        }
    }

    companion object {
        const val MIN_TAB_COUNT = 1
        const val MAX_TAB_COUNT = 2
    }
}
