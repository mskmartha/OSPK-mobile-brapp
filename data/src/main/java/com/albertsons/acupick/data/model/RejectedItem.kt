package com.albertsons.acupick.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@Parcelize
@Keep
data class RejectedItem(
    val displayType: Int?,
    val imageUrl: String?,
    val itemDesc: String?,
    val itemId: String?,
    val itemType: SellByType? = null,
    var misplacedQty: Int? = 0,
    val originalItemId: String?,
    val upcOrPlu: String? = null,
    val qty: Int? = 1,
    val weight: Double? = null,
    val regulated: Boolean? = null,
    val splitId: Int? = null,
    val containerId: String? = null,
    val isRemovedInEdit: Boolean? = null
) : Parcelable

fun RejectedItem.toRemoveItems(isMisplaced: Boolean = false, splitCount: Int? = null, scanTimestamp: ZonedDateTime) =
    RemovedItems(
        itemId = itemId,
        itemDesc = itemDesc,
        originalItemId = originalItemId,
        upc = upcOrPlu,
        quantity = splitCount ?: qty,
        regulated = regulated,
        scanTimeStamp = scanTimestamp,
        itemReasonCode = when {
            isMisplaced -> ItemReasonCode.MISSING_ITEM
            isRemovedInEdit == true -> ItemReasonCode.REMOVED_INEDIT
            else -> ItemReasonCode.REMOVED_ITEM
        }
    )
