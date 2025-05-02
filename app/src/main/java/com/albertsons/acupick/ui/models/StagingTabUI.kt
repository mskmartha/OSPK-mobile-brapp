package com.albertsons.acupick.ui.models

import com.albertsons.acupick.ui.staging.StagingPagerFragmentArgs

data class StagingTabUI(
    val tabLabel: String?,
    val tabArgument: StagingPagerFragmentArgs?
) : UIModel
