package com.albertsons.acupick.ui.models

import com.albertsons.acupick.ui.util.SnackAction
import com.albertsons.acupick.ui.util.SnackDuration
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StringIdHelper

data class AcupickSnackEvent(
    val message: StringIdHelper,
    val type: SnackType,
    val duration: SnackDuration = SnackDuration.LENGTH_LONG,
    val action: SnackAction? = null,
    val isDismissable: Boolean = false,
    val onDismiss: () -> Unit = {}
)
