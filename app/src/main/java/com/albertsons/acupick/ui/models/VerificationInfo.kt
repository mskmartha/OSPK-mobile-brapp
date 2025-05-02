package com.albertsons.acupick.ui.models

data class VerificationInfo(
    val isDugOrder: Boolean,
    val minimumAgeRequired: Int,
    var identificationInfo: IdentificationInfo? = null,
)
