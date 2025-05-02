package com.albertsons.acupick.ui.models

import android.os.Parcelable
import com.albertsons.acupick.data.model.StorageType
import kotlinx.parcelize.Parcelize

@Parcelize
data class QuantityParams(
    val barcodeFormatted: String,
    val isPriced: Boolean,
    val isWeighted: Boolean,
    val isEaches: Boolean,
    val isTotaled: Boolean,
    val itemId: String? = null,
    val image: String? = null,
    val description: String? = null,
    val weightEntry: String?,
    val requested: Int?,
    val entered: Int?,
    val isSubstitution: Boolean?,
    val isIssueScanning: Boolean,
    val shouldShowOriginalItemInfo: Boolean,
    val storageType: StorageType?,
    val isRegulated: Boolean?,
    val isSameItem: Boolean = false,
    val originalItemParams: OriginalItemParams? = null,
    val isCustomerBagPreference: Boolean? = null,
) : Parcelable

@Parcelize
data class OriginalItemParams(
    val itemDesc: String?,
    val itemId: String?,
    val itemImage: String?,
    val orderedQty: Int?
) : Parcelable
