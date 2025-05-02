package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class ItemLocationDto(
    @Json(name = "itemAddress") val itemAddressDto: ItemAddressDto? = null,
    @Json(name = "primary") val primary: Boolean? = null,
) : Parcelable, Dto
