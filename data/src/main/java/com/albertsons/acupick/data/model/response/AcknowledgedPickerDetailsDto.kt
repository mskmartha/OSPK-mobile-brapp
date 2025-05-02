package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class AcknowledgedPickerDetailsDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "isFlashOrderAcknowledged") val isFlashOrderAcknowledged: Boolean = false,
    @Json(name = "orderNumber") val customerOrderNumber: String? = null,
    @Json(name = "userId") val userId: String? = null
) : Parcelable, Dto
