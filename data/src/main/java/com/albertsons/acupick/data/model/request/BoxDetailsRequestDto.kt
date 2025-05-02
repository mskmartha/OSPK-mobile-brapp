package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class BoxDetailsRequestDto(
    @Json(name = "boxNumber") val boxNumber: String? = null,
    @Json(name = "orderNumber") val orderNber: String? = null
) : Parcelable, Dto
