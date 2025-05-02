package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class SiteDto(
    @Json(name = "siteId") val siteId: String? = null,
    @Json(name = "isDefault")val isDefault: Boolean? = false
) : Parcelable, Dto
