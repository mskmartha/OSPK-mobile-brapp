package com.albertsons.acupick.ui.models

import com.albertsons.acupick.R
import com.albertsons.acupick.data.repository.IdentificationTypePto
import com.albertsons.acupick.ui.util.StringIdHelper

enum class IdentificationType(
    val label: StringIdHelper,
    val isScanable: Boolean,
    val layoutId: Int?,
    val jsonValue: StringIdHelper = label,
) {
    DriversLicense(StringIdHelper.Id(R.string.id_type_drivers_license), true, R.id.radioButton1, StringIdHelper.Raw("Drivers License")),
    StateId(StringIdHelper.Id(R.string.id_type_state_id), false, R.id.radioButton2),
    Passport(StringIdHelper.Id(R.string.id_type_passport), false, R.id.radioButton3),
    MilitaryId(StringIdHelper.Id(R.string.id_type_military_id), false, R.id.radioButton4),
    Other(StringIdHelper.Id(R.string.id_type_other), false, R.id.radioButton5),
}

fun IdentificationType.toIdentificationInfoPto(): IdentificationTypePto = when (this) {
    IdentificationType.DriversLicense -> IdentificationTypePto.DriversLicense
    IdentificationType.StateId -> IdentificationTypePto.StateId
    IdentificationType.Passport -> IdentificationTypePto.Passport
    IdentificationType.MilitaryId -> IdentificationTypePto.MilitaryId
    IdentificationType.Other -> IdentificationTypePto.Other
}
