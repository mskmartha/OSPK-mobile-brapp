package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class StorageLocation(
    @Json(name = "customerOrderNumber") val customerOrderNumber: String,
    @Json(name = "storageTypes") val storageTypes: List<StorageLocationType>? = null,
) : Parcelable, Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class StorageLocationType(
    @Json(name = "containerType") val containerType: StorageType,
    @Json(name = "locations") val locations: List<String>,
) : Parcelable, Dto
