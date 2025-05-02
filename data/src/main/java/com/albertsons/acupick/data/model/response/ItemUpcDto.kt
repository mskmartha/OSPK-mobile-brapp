package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

/**
 * Corresponds to the ItemUpcDto swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ItemUpcDto(
    @Json(name = "itemId") val itemId: String? = null,
    @Json(name = "upc") val upcList: List<String>? = null,
) : Parcelable, Dto
