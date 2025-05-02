package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class GiftDto(
    @Json(name = "message") val message: GiftMessageDto? = null
) : Parcelable, Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class GiftMessageDto(
    @Json(name = "from") val giftFrom: String? = null,
    @Json(name = "to") val giftTo: String? = null,
    @Json(name = "text") val giftText: String? = null,
) : Parcelable, Dto
