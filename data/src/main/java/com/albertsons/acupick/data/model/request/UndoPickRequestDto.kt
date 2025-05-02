package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class UndoPickLocalDto(
    @Json(name = "containerId") val containerId: String? = null,
    @Json(name = "undoPickRequestDto") val undoPickRequestDto: UndoPickRequestDto
) : Parcelable, Dto

/**
 * Corresponds to the UndoPickReq swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class UndoPickRequestDto(
    /** Pick List id */
    @Json(name = "actId") val actId: Long? = null,
    /** Item activity db id */
    @Json(name = "iaId") val iaId: Long? = null,
    @Json(name = "netWeight") val netWeight: Double? = null,
    /** Upc db id (not barcode) */
    @Json(name = "pickedUpcId") val pickedUpcId: Long? = null,
    @Json(name = "qty") val qty: Double? = null,
    /**
     * Flag rejectionReason to identify that the unpick was performed as part of swap substitution flow
     */
    @Json(name = "rejectionReason") val rejectionReason: SubstitutionRejectedReason? = null,

    @Json(name = "messageSid") val messageSid: String? = null,
) : Parcelable, Dto

/** Give a list of undo Pick Local items, return another list with all identical items combined into single items with updated qty */
fun Iterable<UndoPickLocalDto>?.combineIdenticalUndoPickLocalDtos(): List<UndoPickLocalDto> {
    val combinedList = mutableListOf<UndoPickLocalDto>()
    this?.distinct()?.forEach { item ->
        val qty = this.count { it == item }
        if (qty > 1) {
            val newItem = item.copy(containerId = item.containerId, undoPickRequestDto = item.undoPickRequestDto.copy(qty = qty.toDouble()))
            combinedList.add(newItem)
        } else {
            combinedList.add(item)
        }
    }
    return combinedList
}
