package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the ItemAttribute swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class FeatureFlagAttributeDto(
    @Json(name = "siteId") val siteId: String? = null,
    @Json(name = "env") val environment: String? = null
) : Parcelable, Dto
