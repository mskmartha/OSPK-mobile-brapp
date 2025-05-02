package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class ConfigFlag(
    @Json(name = "featureFlagName") val featureFlagName: String? = null,
    @Json(name = "featureFlagValue") val featureFlagValue: Boolean? = null,
    @Json(name = "minVersion") val minVersion: String? = null
) : Parcelable, Dto
