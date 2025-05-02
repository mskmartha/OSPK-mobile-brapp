package com.albertsons.acupick.ui.substitute

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BulkItem(
    val itemDes: String?,
    val itemId: String?,
    val imageUrl: String?,
    val isSystemSuggested: Boolean,
    val customerChosen: Boolean = false
) : Parcelable
