package com.albertsons.acupick.ui.arrivals.complete

import androidx.fragment.app.Fragment
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.SimpleFragmentAdapter
import com.albertsons.acupick.ui.util.StringIdHelper

class HandOffPagerAdapter(fragment: BaseFragment<*, *>, private val tabData: List<HandOffTabUI>) : SimpleFragmentAdapter(fragment) {
    override val pages: Map<Int, Pair<Class<out Fragment>, StringIdHelper>> = tabData.mapIndexed { index, tabUI ->
        index to Pair(HandOffFragment::class.java, StringIdHelper.Raw(tabUI.tabLabel ?: ""))
    }.toMap()

    override fun provideBundle(position: Int) = tabData[position].tabArgument.toBundle()

    fun getIndexOfOrder(customerOrderNumber: String): Int = tabData.indexOfFirst { it.tabArgument.handOffArgData.currentHandOffUI?.orderNumber == customerOrderNumber }
}
