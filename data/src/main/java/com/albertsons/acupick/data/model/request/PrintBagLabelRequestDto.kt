package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the PrintBagLableReq [sic] swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class PrintBagLabelRequestDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "containerIds") val containerIds: List<String>,
) : Parcelable, Dto
