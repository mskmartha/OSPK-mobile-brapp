package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the ErContainerItem swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ErContainerItemDto(
    @Json(name = "erContainer") val erContainerDto: ErContainerDto? = null,
    @Json(name = "id") val id: Long? = null,
    @Json(name = "inventory") val inventoryDto: InventoryAttributeDto? = null,
    @Json(name = "item") val itemDto: ItemAttributeDto? = null,
    @Json(name = "itemId") val itemId: String? = null,
    @Json(name = "lineId") val lineId: String? = null,
    @Json(name = "qty") val qty: Double? = null,
    @Json(name = "regulated") val regulated: Boolean? = null,
    @Json(name = "uom") val uom: String? = null
) : Parcelable, Dto
