package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the ErContainer swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ErContainerDto(
    @Json(name = "bagCount") val bagCount: Int? = null,
    @Json(name = "containerId") val containerId: String? = null,
    @Json(name = "containerType") val containerType: String? = null,
    @Json(name = "currentLocation") val currentLocation: String? = null,
    @Json(name = "destination") val destination: String? = null,
    @Json(name = "dimension") val dimensionDto: DimensionDto? = null,
    @Json(name = "erContainerItem") val erContainerItem: List<ErItemDto>? = null,
    @Json(name = "erId") val erId: Long? = null,
    @Json(name = "id") val id: Long? = null,
    @Json(name = "isRegulated") val isRegulated: Boolean? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "stagingLocation") val stagingLocation: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "tagged") val tagged: Boolean? = null,
) : Parcelable, Dto
