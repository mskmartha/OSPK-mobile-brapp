package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the /api/createUpdateDeviceInfo
 */
// https://confluence.safeway.com/pages/viewpage.action?pageId=124563391
@JsonClass(generateAdapter = true)
@Parcelize
data class CreateUpdateDeviceInfoRequestDto(
    @Json(name = "deviceId") val deviceId: String? = null,
    @Json(name = "deviceToken") val deviceToken: String? = null,
    @Json(name = "siteId") val siteId: String? = null,
) : Parcelable, Dto
