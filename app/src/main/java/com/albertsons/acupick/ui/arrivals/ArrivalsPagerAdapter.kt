package com.albertsons.acupick.ui.arrivals

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.SimpleFragmentAdapter
import com.albertsons.acupick.ui.models.ArrivalOrdersTabUI
import com.albertsons.acupick.ui.util.StringIdHelper

class ArrivalsPagerAdapter(
    fragment: BaseFragment<*, *>,
    private val tabData: List<ArrivalOrdersTabUI>
) : SimpleFragmentAdapter(fragment) {

    override val pages: Map<Int, Pair<Class<out Fragment>, StringIdHelper>> = tabData.mapIndexed { index, searchTabUI ->
        when (index) {
            0 -> index to Pair(ArrivalsAllOrdersFragment::class.java, StringIdHelper.Raw(searchTabUI.tabLabel ?: ""))
            else -> index to Pair(ArrivalsInProgressFragment::class.java, StringIdHelper.Raw(searchTabUI.tabLabel ?: ""))
        }
    }.toMap()

    override fun provideBundle(position: Int) = bundleOf(Pair("searchOrdersPager", tabData[position].tabArgument))
}
