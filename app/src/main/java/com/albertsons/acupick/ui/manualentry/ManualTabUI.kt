package com.albertsons.acupick.ui.manualentry

import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerFragmentArgs
import com.albertsons.acupick.ui.models.UIModel

data class ManualTabUI(
    val manualEntryType: ManualEntryType,
    val tabArguments: ManualEntryPagerFragmentArgs
) : UIModel
