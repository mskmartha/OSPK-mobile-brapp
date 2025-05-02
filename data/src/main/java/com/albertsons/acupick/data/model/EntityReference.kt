package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the ActivityDto.EntityReference swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class EntityReference(
    @Json(name = "entityId") val entityId: String? = null,
    @Json(name = "entityType") val entityType: String? = null
) : Parcelable
