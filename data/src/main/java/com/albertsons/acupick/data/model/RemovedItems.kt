package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
class RemovedItems(
    @Json(name = "itemId") val itemId: String?,
    @Json(name = "itemDesc") val itemDesc: String?,
    @Json(name = "itemReasonCode") val itemReasonCode: ItemReasonCode?,
    @Json(name = "originalItemId") val originalItemId: String?,
    @Json(name = "quantity") val quantity: Int?,
    @Json(name = "regulated") val regulated: Boolean?,
    @Json(name = "scanTimeStamp") val scanTimeStamp: ZonedDateTime?,
    @Json(name = "upc") val upc: String?,
) : Parcelable

@JsonClass(generateAdapter = false)
enum class ItemReasonCode {
    @Json(name = "REMOVED_ITEM") REMOVED_ITEM,
    @Json(name = "MISSING_ITEM") MISSING_ITEM,
    @Json(name = "REMOVED_INEDIT") REMOVED_INEDIT
}
