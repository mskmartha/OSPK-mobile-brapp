package com.albertsons.acupick.ui.models

import android.os.Parcelable
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.albertsons.acupick.ui.util.orFalse
import kotlinx.parcelize.Parcelize
import java.time.LocalDate

@Parcelize
data class IdentificationInfo(
    val identificationType: IdentificationType,
    val name: String?,
    val dateOfBirth: LocalDate?,
    val identificationNumber: String?,
    var pickupPersonSignature: String?,
) : Parcelable

fun IdentificationInfo?.isComplete(isIdShown: Boolean): Boolean {
    val validIdEntry = if (isIdShown) this?.identificationNumber?.isNotNullOrBlank() == true else true
    return this != null && this.name?.isNotNullOrBlank().orFalse() && (dateOfBirth != null) && validIdEntry
}
