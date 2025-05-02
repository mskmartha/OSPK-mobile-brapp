package com.albertsons.acupick.ui.models

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.EntityReference
import com.albertsons.acupick.data.model.RejectedItemsByStorageType
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class RemoveRejectedItemUiData(
    val customerName: String?,
    val customerOrderNumber: String?,
    val shortOrderNumber: String?,
    val zonedBags: List<ZonedBagsScannedData> = emptyList(),
    val rejectedItemCount: List<RejectedItemsByStorageType> = emptyList(),
    val isCustomerBagPreference: Boolean?,
    val entityReference: EntityReference?,
) : Parcelable {
    constructor(uiData: DestageOrderUiData?) : this(
        customerName = uiData?.customerName,
        customerOrderNumber = uiData?.customerOrderNumber,
        shortOrderNumber = uiData?.detailsHeaderUi?.shortOrderNumber,
        zonedBags = uiData?.zonedBags ?: emptyList(),
        rejectedItemCount = uiData?.rejectedItemCount ?: emptyList(),
        isCustomerBagPreference = uiData?.isCustomerBagPreference,
        entityReference = uiData?.entityReference
    )
}
