package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.CategoryStatus
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the ActivityDtoByCategory swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ActivityDtoByCategoryDto(
    @Json(name = "category") val category: CategoryStatus? = null,
    @Json(name = "data") val data: List<ActivityAndErDto>? = null
) : Parcelable, Dto
