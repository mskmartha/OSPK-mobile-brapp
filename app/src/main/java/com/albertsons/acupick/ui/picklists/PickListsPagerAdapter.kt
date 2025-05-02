package com.albertsons.acupick.ui.picklists

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.albertsons.acupick.ui.picklists.open.OpenPickListsFragment
import com.albertsons.acupick.ui.picklists.team.TeamPickListsFragment

class PickListsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OpenPickListsFragment()
            1 -> TeamPickListsFragment()
            else -> throw Exception("PickListPagerAdapter: createFragment else error")
        }
    }
}

// keeping this here for now but there are some issues I am facing with using binding adapter due to layoutMediator giving an error saying its been attached too soon
// @BindingAdapter(value = ["viewPagerAdapter"])
// fun ViewPager2.setPagerAdapter(fragment: PickListPagerFragment?) {
//    if (fragment != null)
//        adapter = PickListsPagerAdapter(fragment)
// }
