package com.albertsons.acupick.ui.arrivals.destage

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.albertsons.acupick.ui.SimpleFragmentAdapter
import com.albertsons.acupick.ui.models.DestageOrderTabUI
import com.albertsons.acupick.ui.util.StringIdHelper

class DestageOrderPagerAdapter(
    fragment: FragmentManager,
    lifecycle: Lifecycle,
    private val tabData: List<DestageOrderTabUI>,
) : SimpleFragmentAdapter(fragment, lifecycle) {

    override val pages: Map<Int, Pair<Class<out Fragment>, StringIdHelper>> = tabData.mapIndexed { index, searchOrdersResultDetailsTabUI ->
        index to Pair(
            DestageOrderFragment::class.java,
            StringIdHelper.Raw(searchOrdersResultDetailsTabUI.tabLabel)
        )
    }.toMap()

    override fun provideBundle(position: Int) = tabData[position].tabArgument?.toBundle()

    fun getIndexOfOrder(customerOrderNumber: String): Int = tabData.indexOfFirst { it.tabArgument?.orderNumber == customerOrderNumber }
}
