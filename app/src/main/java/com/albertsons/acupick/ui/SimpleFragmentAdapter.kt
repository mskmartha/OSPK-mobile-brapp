package com.albertsons.acupick.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.albertsons.acupick.ui.util.StringIdHelper
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

abstract class SimpleFragmentAdapter : FragmentStateAdapter {
    constructor(fragment: Fragment) : super(fragment)

    constructor(fragment: FragmentManager, lifeCycle: Lifecycle) : super(fragment, lifeCycle)

    constructor(activity: FragmentActivity) : super(activity)

    abstract val pages: Map<Int, Pair<Class<out Fragment>, StringIdHelper>>

    override fun getItemCount() = pages.size

    open fun provideBundle(position: Int): Bundle? = null

    override fun createFragment(position: Int) = pages[position]?.first?.newInstance()?.apply {
        arguments = provideBundle(position)
    } ?: throw IllegalStateException("No fragment defined for position: $position")

    fun menuTabMediatorFactory(tabs: TabLayout, pager: ViewPager2) =
        TabLayoutMediator(
            tabs,
            pager
        ) { tab: TabLayout.Tab, position: Int ->
            tab.text = pages[position]?.second?.getString(pager.context)
                ?: throw IllegalStateException("No title defined for position: $position")
        }
}
