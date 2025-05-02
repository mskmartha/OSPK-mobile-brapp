package com.albertsons.acupick.ui.arrivals.complete

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.RemovedItems
import kotlinx.parcelize.Parcelize

/** Contains regulated item data for display on the pick up complete screen */
@Parcelize
@Keep
data class HandOffRegulatedItem(
    val description: String,
    val totalQty: Double,
    val itemId: String?,
    val originalItemId: String?,
    val upc: String?,
    val imageUrl: String?,
) : Parcelable

fun HandOffRegulatedItem.toRemovedItem(isRegulated: Boolean) =
    RemovedItems(
        itemId = itemId,
        itemDesc = description,
        originalItemId = originalItemId,
        quantity = totalQty.toInt(),
        regulated = isRegulated,
        upc = upc,
        itemReasonCode = null,
        scanTimeStamp = null,
    )
