package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class StagingSummaryDto(
    @Json(name = "pickList") val picklistId: String? = null,
    @Json(name = "pickingAndStagingDuration") val duration: String? = null,
    @Json(name = "totalItems") val totalItems: Int? = null,
    @Json(name = "pickedItems") val pickedItems: Int? = null,
    @Json(name = "itemsOutOfStock") val itemsOutOfStock: Int? = null,
    @Json(name = "itemsSubstituted") val itemsSubstituted: Int? = null
) : Parcelable, Dto
