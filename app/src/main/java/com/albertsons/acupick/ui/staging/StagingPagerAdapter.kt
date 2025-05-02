package com.albertsons.acupick.ui.staging

import androidx.fragment.app.Fragment
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.SimpleFragmentAdapter
import com.albertsons.acupick.ui.models.StagingTabUI
import com.albertsons.acupick.ui.util.StringIdHelper

class StagingPagerAdapter(fragment: BaseFragment<*, *>, private val tabData: List<StagingTabUI>) : SimpleFragmentAdapter(fragment) {
    override val pages: Map<Int, Pair<Class<out Fragment>, StringIdHelper>> = tabData.mapIndexed { index, stagingTabUI ->
        index to Pair(StagingFragment::class.java, StringIdHelper.Raw(stagingTabUI.tabLabel ?: ""))
    }.toMap()

    override fun provideBundle(position: Int) = tabData[position].tabArgument?.toBundle()

    fun getIndexOfOrder(customerOrderNumber: String): Int = tabData.indexOfFirst { it.tabArgument?.orderNumber == customerOrderNumber }
}
