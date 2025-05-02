package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the UndoShortReq swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class UndoShortRequestDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "iaId") val iaId: Long? = null,
    @Json(name = "qty") val qty: Double? = null,
    @Json(name = "shortedItemId") val shortedItemId: Long? = null,
    @Json(name = "messageSid") val messageSid: String? = null,
) : Parcelable, Dto

/** Give a list of undo pick items, return another list with all non-1-quantity items split into multiple items */
fun Iterable<UndoShortRequestDto>.splitItems(): List<UndoShortRequestDto> {
    val splitList = mutableListOf<UndoShortRequestDto>()
    this.forEach { item ->
        val qty = item.qty?.toInt() ?: 0
        if (qty > 1) {
            val newItem = item.copy(qty = 1.0)
            repeat(qty) {
                splitList.add(newItem)
            }
        } else {
            splitList.add(item)
        }
    }
    return splitList
}

/** Give a list of undo pick items, return another list with all identical items combined into single items with updated qty */
fun Iterable<UndoShortRequestDto>.combineIdenticalItems(): List<UndoShortRequestDto> {
    val combinedList = mutableListOf<UndoShortRequestDto>()
    this.distinct().forEach { item ->
        val qty = this.count { it == item }
        if (qty > 1) {
            val newItem = item.copy(qty = qty.toDouble())
            combinedList.add(newItem)
        } else {
            combinedList.add(item)
        }
    }
    return combinedList
}
