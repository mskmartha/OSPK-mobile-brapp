package com.albertsons.acupick.ui.staging

import androidx.fragment.app.Fragment
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.SimpleFragmentAdapter
import com.albertsons.acupick.ui.models.StagingPart2TabUI
import com.albertsons.acupick.ui.util.StringIdHelper

class StagingPart2PagerAdapter(fragment: BaseFragment<*, *>, private val tabData: List<StagingPart2TabUI>) : SimpleFragmentAdapter(fragment) {
    override val pages: Map<Int, Pair<Class<out Fragment>, StringIdHelper>> = tabData.mapIndexed { index, stagingTabUI ->
        index to Pair(StagingPart2Fragment::class.java, StringIdHelper.Raw(stagingTabUI.tabLabel ?: ""))
    }.toMap()

    override fun provideBundle(position: Int) = tabData[position].tabArgument?.toBundle()

    fun getIndexOfOrder(customerOrderNumber: String): Int = tabData.indexOfFirst { it.tabArgument?.stagingPart2Params?.customerOrderNumber == customerOrderNumber }
}
