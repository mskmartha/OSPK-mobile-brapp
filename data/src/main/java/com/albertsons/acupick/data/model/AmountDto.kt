package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the Amount swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class AmountDto(
    @Json(name = "amount") val amount: Double? = null,
    @Json(name = "currency") val currency: String? = null,
    @Json(name = "netPromotionAmount") val netPromotionAmount: Double? = null,
) : Parcelable, Dto
