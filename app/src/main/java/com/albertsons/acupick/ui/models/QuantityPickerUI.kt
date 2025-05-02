package com.albertsons.acupick.ui.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.android.parcel.Parcelize

@Parcelize
@Keep
data class QuantityPickerUI(
    val itemId: String?,
    val itemImage: String?,
    val itemDescription: String?,
    val upcOrPlu: String?,
    val isSubstitution: Boolean?,
    val requestedCount: Int,
    val enteredQuantity: Int,
    val enteredQuantityVisible: Boolean?,
    val weightEntry: String?,
    val showOriginalItemInfo: Boolean, // TODO ACUPICK_Redesign Should be renamed as isIssueScanning
    val shouldShowQuantityHeader: Boolean,
    val shouldShowDivider: Boolean,
    val shouldShowExampleImage: Boolean,
    val originalItemParams: OriginalItemParams? = null,
    val isCustomerBagPreference: Boolean? = null
) : UIModel, Parcelable {

    constructor(quantityParams: QuantityParams, fixedItemTypesEnabled: Boolean) : this(
        itemId = quantityParams.itemId,
        itemImage = quantityParams.image,
        itemDescription = quantityParams.description,
        upcOrPlu = quantityParams.barcodeFormatted,
        isSubstitution = quantityParams.isSubstitution,
        requestedCount = quantityParams.requested ?: 0,
        enteredQuantity = quantityParams.entered ?: 0,
        enteredQuantityVisible = quantityParams.entered != 0,
        weightEntry = quantityParams.weightEntry,
        showOriginalItemInfo = quantityParams.shouldShowOriginalItemInfo,
        shouldShowDivider = fixedItemTypesEnabled && (quantityParams.isWeighted || quantityParams.isEaches),
        shouldShowQuantityHeader = fixedItemTypesEnabled && (quantityParams.isWeighted || quantityParams.isEaches || quantityParams.isTotaled),
        shouldShowExampleImage = fixedItemTypesEnabled && quantityParams.isTotaled,
        originalItemParams = quantityParams.originalItemParams?.let { originalItemParams ->
            OriginalItemParams(originalItemParams.itemDesc, originalItemParams.itemId, originalItemParams.itemImage, originalItemParams.orderedQty)
        },
        isCustomerBagPreference = quantityParams.isCustomerBagPreference
    )
}
