package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class ValidatePalletRequestDto(
    val siteId: String,
    val timeZone: String,
    val pallet: String,
) : Parcelable, Dto
