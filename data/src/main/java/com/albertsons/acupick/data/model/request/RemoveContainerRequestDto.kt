package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.EntityReference
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the RemoveContainerReq swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class RemoveContainerRequestDto(
    @Json(name = "activityId") val activityId: Long? = null,
    @Json(name = "clearStagLoc") val clearStagLoc: Boolean? = null,
    @Json(name = "containerId") val containerId: String? = null,
    @Json(name = "enRef") val enRef: EntityReference? = null
) : Parcelable, Dto
