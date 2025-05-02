package com.albertsons.acupick.ui.manualentry.pick

import androidx.fragment.app.Fragment
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.SimpleFragmentAdapter
import com.albertsons.acupick.ui.manualentry.ManualTabUI
import com.albertsons.acupick.ui.manualentry.pick.plu.ManualEntryPluFragment
import com.albertsons.acupick.ui.manualentry.pick.upc.ManualEntryUpcFragment
import com.albertsons.acupick.ui.manualentry.pick.weight.ManualEntryWeightFragment
import com.albertsons.acupick.ui.util.StringIdHelper

class ManualEntryPagerAdapter(fragment: BaseFragment<*, *>, private val tabData: List<ManualTabUI>) : SimpleFragmentAdapter(fragment) {
    override val pages: Map<Int, Pair<Class<out Fragment>, StringIdHelper>> = tabData.mapIndexed { index, manualUi ->
        when (index) {
            0 -> index to Pair(ManualEntryUpcFragment::class.java, StringIdHelper.Raw(manualUi.manualEntryType.name))
            1 -> index to Pair(ManualEntryPluFragment::class.java, StringIdHelper.Raw(manualUi.manualEntryType.name))
            2 -> index to Pair(ManualEntryWeightFragment::class.java, StringIdHelper.Raw(manualUi.manualEntryType.name))
            else -> index to Pair(ManualEntryUpcFragment::class.java, StringIdHelper.Raw(manualUi.manualEntryType.name))
        }
    }.toMap()

    override fun provideBundle(position: Int) = tabData[position].tabArguments.toBundle()
}
